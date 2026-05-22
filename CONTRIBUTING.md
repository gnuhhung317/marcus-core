# 🏗️ Cẩm nang Kiến trúc Marcus (Clean Modular Monolith)

Tài liệu này quy định các nguyên tắc thiết kế mã nguồn cho dự án Marcus. Mọi Pull Request (PR) vi phạm các rule này sẽ bị reject.

## 🎯 1. Nguyên tắc cốt lõi (The Golden Rule)
* **Quy tắc hướng phụ thuộc:** Mũi tên dependency luôn trỏ từ ngoài vào trong: `API` -> `Application` -> `Domain` <- `Infrastructure`.
* **Tuyệt đối:** `Domain` và `Application` không được phép phụ thuộc vào `Infrastructure` hoặc `API`.

## 📦 2. Quy định cho từng Module

### 🧠 marcus-domain (Trái tim hệ thống)
* **Role:** Định nghĩa nghiệp vụ, thực thể, và các "hợp đồng" (Interface).
* **Tech Stack:** 100% Java thuần (POJO, Record, Enum) + Lombok.
* **❌ KHÔNG ĐƯỢC PHÉP:** * Chứa bất kỳ Annotation nào của Spring Framework (như `@Component`, `@Service`, `@Autowired`).
    * Chứa các thư viện công nghệ (JPA `@Entity`, Kafka, Redis, Jackson).
* **Rule Thiết kế:** * Dùng **ID** (`String`, `UUID`) để liên kết các Entity lớn (ví dụ: `botId`, `userId`), không nhúng nguyên một Object lớn vào Entity khác để tránh N+1 Query và ràng buộc logic.
    * Mọi giao tiếp ra bên ngoài DB, Queue phải được định nghĩa bằng **Interface (Port)** (VD: `SignalRepository`).

### 👨‍🍳 marcus-application (Bếp trưởng điều phối)
* **Role:** Chứa các Use Case (Kịch bản sử dụng) và DTOs.
* **Tech Stack:** Spring Core (`@Service`), Java.
* **Quy tắc Use Case:** * Áp dụng Single Responsibility Principle: Mỗi Use Case là một class duy nhất, thực hiện đúng 1 chức năng (VD: `ProcessIncomingSignalUseCase`, không dùng `SignalService` chung chung).
    * Use Case nhận DTO từ API, gọi các quy tắc nghiệp vụ trong Domain, và ra lệnh cho các Interface của Infrastructure.

### ⚙️ marcus-infrastructure (Phòng máy - Đồ nghề)
* **Role:** Triển khai (Implement) các Interface từ Domain để giao tiếp với DB, Cache, Message Queue.
* **Tech Stack:** Spring Data JPA, Redis, Kafka, PostgreSQL, MapStruct.
* **Quy tắc Adapter:**
    * **Bắt buộc:** Phải dùng **MapStruct** (hoặc tự map tay) để chuyển đổi giữa `Domain Object` (POJO) và `Database Entity` (`@Entity`).
    * Tuyệt đối không trả DB Entity ra ngoài khỏi module này.
    * Không chứa logic nghiệp vụ trading tại đây. Nơi đây chỉ làm nhiệm vụ: Lưu, Xóa, Gửi, Nhận.

### 🚪 marcus-api (Cửa ngõ ra vào)
* **Role:** Giao tiếp với Client (User, Bot Node) qua REST API, WebSocket.
* **Tech Stack:** Spring Web, Spring Security, Swagger.
* **Quy tắc Controller:**
    * Controller phải cực "mỏng" (Thin Controller).
    * Chỉ làm nhiệm vụ: Nhận Request -> Validate data format cơ bản -> Gọi Use Case ở module `application` -> Trả về Response.
    * Không viết câu `if/else` liên quan đến logic nghiệp vụ tại Controller.

## 🚀 3. Quy trình tư duy khi làm một tính năng mới (Feature Workflow)
Đừng nhảy vào code Controller hay Database ngay. Hãy đi theo luồng **Domain-First**:
1. **Domain:** Tính năng này cần Entity nào? Dữ liệu gốc là gì? (Viết POJO). Cần lấy/lưu data gì? (Viết Interface).
2. **Application:** Logic điều phối diễn ra thế nào? (Viết Use Case).
3. **Infrastructure:** Làm sao để lưu vào Postgres? (Viết Entity, Mapper, Adapter).
4. **API:** Mở cổng nào cho Client gọi? (Viết Controller, Route).