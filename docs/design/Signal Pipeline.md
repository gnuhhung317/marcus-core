# RFC-001: Kiến trúc xử lý tín hiệu (Signal Ingestion & Fan-out Pipeline)

**Author:** Hùng
**Status:** DRAFT
**Date:** 2026-03-11

## 1. Context & Problem
Hệ thống Marcus đóng vai trò là Hub trung chuyển tín hiệu từ Bot của Developer tới các Local Executor (Node) của người dùng (Subscriber). Quá trình này đối mặt với 3 thách thức kỹ thuật chí mạng:
1. **Idempotency (Tính luỹ đẳng):** Mạng chập chờn, Bot tự động retry gửi lệnh `OPEN_LONG` nhiều lần. Nếu không chặn, user sẽ vào nhiều lệnh trùng lặp dẫn đến cháy tài khoản.
2. **Latency (Độ trễ):** Trong Crypto, trễ 1 giây có thể trượt giá (Slippage) nặng. Luồng xử lý phải đạt mức milliseconds.
3. **Fan-out Bottleneck:** 1 Bot có thể có hàng chục ngàn user. Khi có 1 tín hiệu vào, hệ thống phải broadcast ra ngần ấy Websocket sessions cùng một lúc mà không làm nghẽn CPU/RAM của server.

## 2. Proposed Solution

### 2.1. API Contract
Developer phải tuân thủ payload này khi gọi `POST /api/v1/signals`. Bắt buộc phải có `signal_id` (UUID) do Bot tự sinh ra để làm key chống lặp.

```json
{
  "signal_id": "123e4567-e89b-12d3-a456-426614174000",
  "bot_id": "bot-trend-v1",
  "exchange_slug": "binance",
  "symbol": "BTCUSDT",
  "action": "OPEN_LONG",
  "price": 65000.5,
  "metadata": {
    "ml_confidence": 0.92,
    "recommended_leverage": 10
  },
  "timestamp": 1741167605000
}