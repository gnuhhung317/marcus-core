# ARCHITECTURE_BACKEND.md: Onboarding for Senior Developers

Chào mừng bạn đến với đội ngũ Engineering của **Marcus**. Với tư cách là Principal Architect, tôi trình bày bản đồ kiến trúc hệ thống của dự án **signal-core-backend**. Tài liệu này giúp bạn nắm bắt "Mental Model" của hệ thống trước khi bắt đầu commit code.

---

## 1. Clean Architecture Boundaries (Ranh giới Kiến trúc)

Hệ thống tuân thủ nghiêm ngặt mô hình **Hexagonal Architecture** (Ports & Adapters). Ranh giới giữa các module được phân định rõ ràng:

*   **marcus-domain**: Trái tim của hệ thống.
    *   **Purity**: Thuần khiết 100%. Không phụ thuộc Spring, JPA hay bất kỳ library ngoài nào (ngoại trừ Lombok).
    *   **Logic**: Chứa **Signal**, **Bot**, **Subscription** (Domain Models) và các **Value Objects**.
    *   **Ports**: Định nghĩa các Interface như **SignalRepository**, **SignalPublisherPort**, **BotRepository**. Đây là "hợp đồng" mà tầng Domain yêu cầu thế giới bên ngoài thực hiện.
*   **marcus-application**: Tầng điều phối (Orchestration).
    *   Chứa các **UseCases** (ví dụ: **CaptureSignalUseCase**, **SubscribeBotUseCase**).
    *   Chỉ phụ thuộc vào **marcus-domain**. Nó nhận Input từ API, gọi Domain Logic và ra lệnh cho các Ports.
*   **marcus-infrastructure**: Tầng thực thi (Adapters).
    *   Hiện thực hóa các Ports. Ví dụ: **JpaSignalRepositoryImpl** (PostgreSQL), **KafkaSignalProducer** (Kafka).
*   **marcus-api**: Cổng giao tiếp (Entry Points).
    *   Chứa **SignalController** (REST) và **ExecutorWebSocketHandler** (WebSocket).

**Chứng minh nguyên tắc Dependency**:
Mũi tên dependency luôn hướng vào trong: `api -> application -> domain` và `infrastructure -> domain`.
*Trích dẫn*: **CaptureSignalUseCase** sử dụng **SignalRepository** (Interface trong domain) để lưu dữ liệu mà không hề biết đó là PostgreSQL hay MongoDB.

---

## 2. The Nervous System (Dòng chảy Dữ liệu & Sự kiện)

Luồng đi của một **Signal** từ khi bot gửi đến khi tới tay người dùng (Executor):

1.  **Ingestion**: **SignalController** (POST `/api/v1/signals`) nhận request.
2.  **Validation & Idempotency**:
    *   **BotSignatureInterceptor** kiểm tra chữ ký HMAC và dùng Redis `setIfAbsent` để chặn Duplicate Signature (Idempotency Window 60s).
    *   **CaptureSignalUseCase** gọi `signalRepository.existsBySignalId` để chặn trùng lặp logic vĩnh viễn trong DB.
3.  **Persistence**: Lưu vào PostgreSQL thông qua **SignalRepository**.
4.  **Global Event**: Gọi `signalPublisherPort.publish(signal)` đẩy vào Kafka topic `trading-signals`.
5.  **Instance Fanout**:
    *   **SignalDispatchKafkaConsumer** trên mỗi pod dùng group id riêng và nhận mọi signal từ topic `trading-signals`.
6.  **WebSocket Dispatch**:
    *   **ExecutorSessionRegistry** giữ **WebSocketSession** trong memory và broadcast frame JSON đến mọi executor đã đăng ký với bot đó.

---

## 3. Critique & Optimization (Phản biện & Tối ưu hóa)

### Concurrency & Idempotency
*   **Idempotency**: Rất tốt ở tầng API (Redis) và DB (Unique Constraint `signal_id`).
*   **Race Condition**: Có rủi ro nhỏ ở tầng Database khi checking `existsBySignalId` và `save` không nằm trong một atomic transaction mang tính lock. Tuy nhiên, Unique Constraint ở DB sẽ là chốt chặn cuối cùng (ném ra `DataIntegrityViolationException`).

### Coupling Leakage (Điểm mù)
*   **Leakage**: Tầng **marcus-application** đang sử dụng trực tiếp các DTO từ tầng API (ví dụ: **CaptureSignalRequest**). Đây là sự rò rỉ cấu trúc ngoại vi vào tầng logic.
    *   *Khắc phục*: Nên map sang Command object trước khi truyền vào UseCase.
*   **Domain Model**: `BaseModel` trong domain chứa `createdAt`, `updatedAt` - thường là các trường của persistence layer. Dù không vi phạm nghiêm trọng nhưng nó cho thấy sự ảnh hưởng của tư duy database lên domain model.

### Khả năng mở rộng (Scalability)
*   **WebSocket Registry**: Hiện tại **ExecutorSessionRegistry** sử dụng **ConcurrentHashMap** lưu trong memory của từng Pod.
    *   **Rủi ro**: Khi hệ thống scale lên 10-20 pods, việc quản lý "ai đang ở đâu" phụ thuộc vào việc định tuyến Kafka key. Nếu Kafka key bị phân phối không đều, một số pod sẽ bị quá tải trong khi pod khác nhàn rỗi.
    *   **Redis Pub/Sub**: Việc broadcast qua Redis Pub/Sub cho mọi Pod khi có routing message là một giải pháp tạm thời. Khi số lượng Pod và Tín hiệu tăng cao, traffic nội bộ giữa các Pod sẽ bùng nổ (N*M).
    *   *Feedback*: Cần chuyển sang cơ chế **Distributed Session Registry** dùng Redis Hash để tra cứu PodID trực tiếp thay vì broadcast.

### Technical Debt
*   **Blocking I/O**: Trong **ExecutorSessionRegistry.broadcastToBot**, lệnh `session.sendMessage(message)` là blocking. Nếu một kết nối mạng của client bị chậm, nó có thể làm nghẽn luồng xử lý của các client khác trong cùng vòng lặp broadcast.
    *   *Khắc phục*: Cần triển khai per-session outbound queue (Ring Buffer).

---
*Tài liệu này được soạn bởi Principal Architect. Mọi thắc mắc vui lòng thảo luận tại RFC channel.*
