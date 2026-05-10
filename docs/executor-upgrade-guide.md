# Executor Client Upgrade Guide: Signed WebSocket Handshake

## Overview
The Marcus backend now requires executor clients to use a **signed HMAC-SHA256 handshake** for WebSocket connections. This replaces the legacy `subscribe` frame with a cryptographically secure protocol.

## Why This Change?
1. **Security**: Prevents unauthorized executor impersonation (was missing in legacy subscribe frame)
2. **Idempotency**: Enables exactly-once semantics for signal ingest (critical for decision audit)
3. **Replay Protection**: Timestamp validation prevents old/replayed handshakes

## Changes Required

### Old Protocol (DEPRECATED ⚠️)
```python
# Old: NO handshake signature
ws.send(json.dumps({
    "type": "subscribe",
    "payload": {
        "bot_id": "bot-123",
        "ws_token": "ws_abc123...",
    }
}))
```

### New Protocol (REQUIRED ✅)
```python
import json
import hmac
import hashlib
import base64
import uuid
from datetime import datetime

# New: Signed handshake with HMAC-SHA256
def sign_handshake(bot_id, ws_token):
    nonce = str(uuid.uuid4())
    version = "1.0"
    timestamp = datetime.utcnow().isoformat() + "Z"
    
    # Build payload
    payload = {
        "nonce": nonce,
        "version": version
    }
    
    # Signature covers: botId|timestamp|base64(payload)
    payload_json = json.dumps(payload, separators=(',', ':'))
    payload_base64 = base64.b64encode(payload_json.encode()).decode()
    message = f"{bot_id}|{timestamp}|{payload_base64}"
    
    # Compute HMAC-SHA256 with wsToken as key
    signature = base64.b64encode(
        hmac.new(
            ws_token.encode(),
            message.encode(),
            hashlib.sha256
        ).digest()
    ).decode()
    
    return {
        "type": "handshake",
        "botId": bot_id,
        "timestamp": timestamp,
        "payload": payload,
        "signature": signature
    }

# Connect and handshake
async def connect_with_signed_handshake():
    async with websockets.connect("wss://marcus-backend/ws/executor", extra_headers=[
        ("Authorization", f"Bearer {ws_token}")
    ]) as ws:
        # Send signed handshake
        handshake_frame = sign_handshake("bot-123", "ws_abc123...")
        await ws.send(json.dumps(handshake_frame))
        
        # Expect handshake-ack response
        ack = json.loads(await ws.recv())
        assert ack["type"] == "ack"
        assert ack["payload"]["ack_type"] == "handshake"
        assert ack["payload"]["status"] == "ok"
        assert ack["payload"]["bot_id"] == "bot-123"
        
        print("✅ Connected with signed handshake!")
        
        # Now ready to send execution events
        await ws.send(json.dumps({
            "type": "execution_event",
            "eventId": "evt-123",
            "idempotencyKey": "idempotency-key-123",
            "correlationId": "correlation-id-123",
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "payload": {
                "signal": "BUY",
                "quantity": 10,
                ...
            }
        }))
```

## Implementation Checklist

- [ ] **Update `ws_client.py`** (or equivalent WebSocket client):
  - Add HMAC-SHA256 signing function
  - Compute signature as `base64(HMAC-SHA256(botId|timestamp|base64(payload), wsToken))`
  - Send signed `handshake` frame instead of legacy `subscribe`
  - Validate handshake-ack response with correct `ack_type`

- [ ] **Update `engine.py`** (or connection initialization):
  - Pass `ws_token` (not `bot_secret`) to WS client
  - Use signed handshake before sending execution events

- [ ] **Remove `bot_secret` from executor config**:
  - `bot_secret` is no longer needed (use `ws_token` instead)
  - Clean up all references to `bot_secret`

- [ ] **Update tests**:
  - Test signed handshake with valid HMAC ✅
  - Test handshake rejection on invalid signature ✅
  - Test handshake rejection on expired timestamp ✅
  - Ensure existing execution_event tests still pass ✅

- [ ] **Manual Testing**:
  - [ ] Connect with signed handshake to dev/staging backend
  - [ ] Verify handshake-ack received
  - [ ] Send execution_event and verify ack
  - [ ] Check backend logs for "handshake accepted" debug message
  - [ ] Load test with multiple concurrent connections

## Error Handling

### Handshake Rejection Codes
| Error | Cause | Fix |
|-------|-------|-----|
| **401 POLICY_VIOLATION** | Invalid signature or expired timestamp (> 5 min old) | Recalculate signature, use fresh timestamp |
| **400 BAD_DATA** | Missing botId, timestamp, signature, or payload | Ensure all fields present in handshake frame |
| **406 NOT_ACCEPTABLE** | No active subscription matches (botId, wsToken) combo | Verify bot is subscribed and wsToken is correct |
| **1002 PROTOCOL_ERROR** | Unsupported frame type after handshake | Only send heartbeat, execution_event, or other supported types |

### Connection Lifecycle
```
1. Connect to wss://marcus-backend/ws/executor
   ↓
2. Send signed handshake frame
   ↓
3a. ✅ Receive handshake-ack → ready to send events
   OR
3b. ❌ Connection closes with status 401/400/406 → retry or investigate
   ↓
4. Send execution_event frames (after successful handshake)
   ↓
5. Send heartbeat every 30s to keep connection alive
```

## Timestamps & Clock Skew

**Important**: The backend validates that the handshake timestamp is within **±5 minutes** of server time.

- **Minimum requirement**: Executor and backend clocks must be synchronized within 5 minutes
- **Recommendation**: Use NTP or similar to keep executor system clock accurate to ±10 seconds
- **Error recovery**: If you receive "expired_handshake" errors:
  1. Check system clock on executor: `date` (Unix/Linux) or `Get-Date` (Windows)
  2. Sync with NTP pool: `ntpdate pool.ntp.org` or similar
  3. Retry connection with fresh timestamp

## FAQ

**Q: Can I reuse the same handshake frame multiple times?**
A: No. Each connection requires a new signed handshake with a fresh timestamp. The signature is only valid for 5 minutes.

**Q: What if I'm still using bot_secret?**
A: You must remove it. The new protocol uses `ws_token` (already provided in subscription API) instead of bot_secret.

**Q: How often should I reconnect?**
A: Keep the connection open and send heartbeats every 30s. Only reconnect if the connection is closed (network failure, timeout, server restart).

**Q: Is this backward compatible?**
A: No. The backend no longer accepts the old `subscribe` frame. You must upgrade to the signed handshake protocol.

## Support

For issues or questions:
1. Check the executor-upgrade-guide.md (this file)
2. Review backend logs for `ExecutorWebSocketHandler` messages
3. Verify signature calculation matches the backend expectation
4. Test locally against dev/staging before production

---

**Deployment Timeline**:
- Executor team: Implement and test signed handshake locally
- Backend team: Deploy backend with signed handshake requirement
- Executor team: Deploy updated executor with signed handshake
- Coordinate both deployments to avoid downtime
