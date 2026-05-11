# Hệ Thống Signal Core Backend - Kiến Trúc Tổng Quan

## 1. Tổng Quan (Overview)
Hệ thống `signal-core-backend` là trái tim của nền tảng Marcus Trading, chịu trách nhiệm tiếp nhận, lưu trữ và phân phối các tín hiệu giao dịch (trading signals) từ các Bot tới các Local Executor theo thời gian thực. Hệ thống được xây dựng theo kiến trúc **Hexagonal (Clean Architecture)** nhằm đảm bảo tính linh hoạt, dễ kiểm thử và tách biệt rõ ràng giữa logic nghiệp vụ và hạ tầng kỹ thuật.

## 2. Bản Đồ Module (Module Map)

### marcus-domain
- **File**: `marcus-domain/`
- **Responsibility**: Chứa các business model (`Signal`, `Bot`), domain service và interfaces (`ports`) cho infrastructure. Đây là lõi của ứng dụng, hoàn toàn độc lập với các framework bên ngoài.
- **Depends on**: None
- **Called by**: `marcus-application`, `marcus-infrastructure`
- **Key decision**: Định nghĩa các Ports để giao tiếp với DB và Message Broker, cho phép thay đổi hạ tầng mà không ảnh hưởng đến nghiệp vụ.

### marcus-application
- **File**: `marcus-application/`
- **Responsibility**: Triển khai các Use Case (nghiệp vụ chính) như `CaptureSignalUseCase`, `ResolveBotRoutingTargetsUseCase`. Nó điều phối luồng dữ liệu giữa Domain và các adapters.
- **Depends on**: `marcus-domain`
- **Called by**: `marcus-api`
- **Key decision**: Mỗi Use Case được đóng gói trong một class riêng biệt (Command Pattern), giúp code dễ đọc và bảo trì.

### marcus-infrastructure
- **File**: `marcus-infrastructure/`
- **Responsibility**: Triển khai các `adapters` cho các `ports` đã định nghĩa ở Domain. Bao gồm: Persistence (PostgreSQL), Messaging (Kafka), Caching (Redis), và Security (JWT, API Key).
- **Depends on**: `marcus-domain`
- **Called by**: `marcus-application`, Spring Framework.
- **Key decision**: Sử dụng Kafka để decoupled quá trình nhận tín hiệu và xử lý tín hiệu. Có cơ chế lưu trữ hàng loạt (Batch Save) để tối ưu hiệu năng DB.

### marcus-api
- **File**: `marcus-api/`
- **Responsibility**: Cung cấp các REST API endpoints cho Bot gửi tín hiệu và WebSocket cho các Local Executor nhận tín hiệu thời gian thực.
- **Depends on**: `marcus-application`, `marcus-domain`
- **Called by**: External Clients (Bots, Mobile/Web UI, Local Executors).
- **Key decision**: Sử dụng WebSocket với cơ chế `SessionRegistry` để quản lý các kết nối từ Executor và dispatch tín hiệu chính xác theo `bot_id`.

## 3. Luồng Dữ Liệu (Data Flow - Signal Pipeline)

1. **Ingestion**: Bot gửi tín hiệu qua `POST /api/v1/signals` (Xác thực bằng chữ ký bot).
2. **Validation & Persist**: `CaptureSignalUseCase` kiểm tra tính hợp lệ, lưu vào PostgreSQL (đồng bộ) để đảm bảo phản hồi nhanh cho Bot.
3. **Queueing**: Tín hiệu được đẩy vào Kafka topic `trading-signals`.
4. **Dispatching**:
   - `SignalDispatchKafkaConsumer` (trong `marcus-api`) lắng nghe topic Kafka.
   - Dựa trên `bot_id`, tìm các session WebSocket đang hoạt động của các Executor tương ứng.
   - Broadcast tín hiệu qua WebSocket.
5. **Async Storage**: `KafkaSignalStorageConsumerAdapter` (trong `marcus-infrastructure`) cũng lắng nghe topic Kafka để thực hiện lưu trữ/cập nhật DB theo batch (tối ưu hóa).

## 4. Giao Tiếp Executor-Backend (Executor-Backend Communication)

Luồng giao tiếp giữa Backend và Executor được thực hiện qua WebSocket bền vững (Persistent WebSocket) với các đặc điểm sau:

### 4.1. Vòng đời kết nối (Connection Lifecycle)
- **Handshake & Auth**: Executor kết nối bằng cách gửi mã `wsToken` (Bearer Token) trong header. Sau khi kết nối, Executor phải gửi một frame `handshake` chứa chữ ký HMAC-SHA256 (được tính từ `botId`, `timestamp`, `payload` và `wsToken`) để xác thực quyền sở hữu bot.
- **Session Management**: `ExecutorSessionRegistry` quản lý các session trong bộ nhớ. Hệ thống áp dụng chính sách **Single Session Per Token**: nếu một Executor mới kết nối với cùng một token, kết nối cũ sẽ bị ngắt.
- **Heartbeat**: Executor gửi frame `heartbeat` định kỳ để duy trì kết nối. Backend phản hồi bằng frame `ack`.

### 4.2. Giao tiếp Hai chiều (Bidirectional Messaging)
- **Luồng Xuống (Downward - Backend to Executor)**:
  - Tín hiệu (`signal`): Được gửi tự động khi có tín hiệu mới từ Bot (thông qua Kafka -> WebSocket).
  - Phản hồi hệ thống (`ack`, `system`): Thông báo kết quả xử lý hoặc lỗi hệ thống.
- **Luồng Lên (Upward - Executor to Backend)**:
  - `execution_event`: Executor gửi cập nhật trạng thái lệnh (vào lệnh, đóng lệnh, khớp từng phần). Backend phản hồi bằng `execution_ack`.
  - `audit-push`: Gửi các dữ liệu kiểm toán và trạng thái tài khoản (`balance_snapshot`). Backend sử dụng dữ liệu này để đồng bộ hóa số dư (`BalanceSyncUseCase`).

### 4.3. Phối hợp Cluster (Cluster Coordination)
- Hệ thống sử dụng **Kafka** (topic `trading-signals-routing`) để điều hướng tín hiệu giữa các instance backend khác nhau. Khi một tín hiệu cần gửi tới một Executor không nằm trên instance hiện tại, nó sẽ được đẩy qua Kafka để instance đang giữ kết nối WebSocket đó thực hiện gửi.
- **Redis** đóng vai trò là registry tập trung để theo dõi instance nào đang giữ kết nối của Executor nào (thông qua `SignalServerDispatchPort`).

## 5. Phụ Thuộc Bên Ngoài (External Dependencies)

- **PostgreSQL**: Lưu trữ dữ liệu quan hệ (Bots, Signals, Users, Portfolios). Sử dụng kiểu dữ liệu `jsonb` cho metadata.
- **Apache Kafka**: Message Broker đóng vai trò xương sống cho việc phân phối tín hiệu bất đồng bộ.
- **Redis**: Lưu trữ session, cache thông tin routing và hỗ trợ phân phối tín hiệu trong môi trường cluster (thông qua `signalServerDispatchPort`).
- **Spring Boot**: Framework chính để phát triển ứng dụng Java.

## 5. Các Vùng Rủi Ro & Nợ Kỹ Thuật (Risk Areas & Tech Debt)

- **Sự chồng chéo persistence**: Hiện tại Signal đang được lưu cả ở UseCase (đồng bộ) và Kafka Consumer (bất đồng bộ). Cần làm rõ mục đích của việc này hoặc hợp nhất để tránh xung đột dữ liệu.
- **Error Handling trong WebSocket**: Việc dispatch tín hiệu nếu thất bại chưa có cơ chế retry rõ ràng (hiện tại dựa vào tính bền vững của Kafka).
- **Scalability**: Khi số lượng kết nối WebSocket tăng cao, cần cơ chế Redis Pub/Sub để dispatch tín hiệu giữa các instance backend khác nhau một cách hiệu quả hơn.
