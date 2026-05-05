# Manual E2E: subscribe bot and dispatch a signal

Run this flow when you want to verify the full path manually:

1. A trader subscribes to the bot and gets a `wsToken`.
2. A websocket client connects with that `wsToken` and waits for frames.
3. The bot sends a signed signal through `POST /api/v1/signals`.
4. The websocket client receives the `ack` frame and then the `signal` frame.

## Given credentials

```text
botId    = bot_f25c000d5e90488c9c1c58fd5cd11843
botName  = Momentum Alpha
apiKey   = ak_2839eb9cdfad4b9abc4ee4e3865b2635
rawSecret = sk_b02a6a6baaf34ae885216de07256a7c1
wsToken  = ws_a0f3e543ed5448a98b266e1b6ef1e96d
```

## Preconditions

- Backend is up, for example with `docker compose up -d` from `signal-core-backend`.
- Python 3.10+ is available.
- The Python environment used for the websocket client has the `websockets` package installed.
- If you want to create a fresh subscription by curl, you also need a valid trader JWT as `USER_JWT`.

Set the shared values first:

```powershell
$BaseUrl = $env:MARCUS_API_BASE_URL
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = 'http://localhost:8080' }

$WsUrl = $env:MARCUS_WS_URL
if ([string]::IsNullOrWhiteSpace($WsUrl)) { $WsUrl = 'ws://localhost:8080/ws/executor' }

$BotId = 'bot_f25c000d5e90488c9c1c58fd5cd11843'
$BotApiKey = 'ak_2839eb9cdfad4b9abc4ee4b3865b2635'
$BotSecret = 'sk_b02a6a6baaf34ae885216de07256a7c1'
$WsToken = 'ws_a0f3e543ed5448a98b266e1b6ef1e96d'
```

## Terminal A: websocket client

Start a lightweight websocket listener that authenticates with the runtime token, sends the subscribe handshake, and prints every frame it receives.

```powershell
$env:MARCUS_WS_URL = $WsUrl
$env:MARCUS_WS_TOKEN = $WsToken
$env:MARCUS_BOT_ID = $BotId

@'
import asyncio
import json
import os

import websockets


async def main() -> None:
    ws_url = os.environ["MARCUS_WS_URL"]
    ws_token = os.environ["MARCUS_WS_TOKEN"]
    bot_id = os.environ["MARCUS_BOT_ID"]

    async with websockets.connect(
        ws_url,
        extra_headers={"Authorization": f"Bearer {ws_token}"},
        ping_interval=None,
        ping_timeout=None,
    ) as websocket:
        handshake = {
            "type": "subscribe",
            "payload": {
                "bot_id": bot_id,
                "protocol_version": "1.0",
                "stream": "signal_execution",
                "mode": "consume_only",
            },
        }
        await websocket.send(json.dumps(handshake, separators=(",", ":")))
        print("subscribe frame sent")

        while True:
            message = await websocket.recv()
            print(message)


asyncio.run(main())
'@ | python -
```

Expected output:

- an `ack` frame for the subscribe handshake
- later, a `signal` frame with the trading payload

## Terminal B: create subscription for the user

If you want to exercise the user-facing subscribe endpoint, call it with a valid trader JWT.
If the subscription already exists, you can skip this step and keep the provided `wsToken`.

```powershell
$UserJwt = $env:USER_JWT

curl.exe -i -X POST "$BaseUrl/api/v1/subscriptions/$BotId" `
  -H "Authorization: Bearer $UserJwt" `
  -H "Content-Type: application/json"
```

Expected result:

- HTTP `201 Created`
- response body contains the subscription result and runtime `wsToken`

## Terminal C: send a signed signal

This request uses the bot API key and raw secret you provided. The signature payload is:

`timestamp + "\n" + raw JSON body`

```powershell
$Timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$SignalBody = @'
{
  "signalId": "sig_manual_e2e_001",
  "botId": "bot_f25c000d5e90488c9c1c58fd5cd11843",
  "symbol": "BTC/USDT",
  "action": "OPEN_LONG",
  "entry": 68420.5,
  "stopLoss": 67800.0,
  "takeProfit": 69250.0,
  "status": "RECEIVED",
  "generatedTimestamp": "2026-05-04T00:00:00",
  "metadata": {
    "strategy": "Momentum Alpha",
    "source": "manual-e2e"
  }
}
'@

$SignalFile = Join-Path $env:TEMP 'marcus-signal-manual-e2e.json'
Set-Content -Path $SignalFile -Value $SignalBody -NoNewline -Encoding utf8

$Payload = "$Timestamp`n$SignalBody"
$Hmac = New-Object System.Security.Cryptography.HMACSHA256 ([Text.Encoding]::UTF8.GetBytes($BotSecret))
$SignatureBytes = $Hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($Payload))
$Signature = [Convert]::ToHexString($SignatureBytes).ToLowerInvariant()

curl.exe -i -X POST "$BaseUrl/api/v1/signals" `
  -H "X-Timestamp: $Timestamp" `
  -H "X-Bot-Api-Key: $BotApiKey" `
  -H "X-Signature: $Signature" `
  -H "Content-Type: application/json" `
  --data-binary "@$SignalFile"
```

Expected result:

- HTTP `200 OK`
- the websocket client from Terminal A prints the `signal` frame
- the frame contains at least `signalId`, `botId`, `symbol`, and `action`

## Full manual sequence

1. Start Terminal A and keep it open.
2. If needed, run Terminal B to create the subscription.
3. Run Terminal C to send the signed signal.
4. Verify Terminal A prints `ack` first, then `signal`.

## Troubleshooting

- `401 Unauthorized` from `/api/v1/signals`: check `X-Timestamp`, `X-Bot-Api-Key`, `X-Signature`, and the exact raw JSON body used for the HMAC.
- `400 Bad Request` from the signature filter: timestamp is too far from server time or malformed.
- No websocket frames after subscribe: confirm the websocket URL and that the `wsToken` matches the active subscription.
- `409 Conflict` on subscribe: the user is already subscribed to that bot.