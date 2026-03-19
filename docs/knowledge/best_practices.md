# Hệ Thống Kiến Thức & Bài Học Kinh Nghiệm (DATN)

## 1. Tư Duy Kiến Trúc (Clean Architecture)
*   **Dependency Rule**: Tầng Application chỉ được phụ thuộc vào Domain. Tầng Infrastructure phụ thuộc vào Domain để triển khai (Implement) các Interface (Ports). Luôn tuân thủ quy tắc "Inside-Out".
*   **Module Aggregation**: Module `marcus-api` đóng vai trò là "người lắp ráp" (Aggregator), nó phụ thuộc vào tất cả các module khác để kích hoạt ứng dụng và cấu hình Beans.
*   **Use Case Pattern**: Mỗi nghiệp vụ là một Class duy nhất với một nhiệm vụ duy nhất (SRP), thường thông qua hàm `execute()`. Tránh việc dồn quá nhiều logic vào một Service phình to.

## 2. Bảo mật hệ thống (Security & Crypto)
*   **Hashing vs Encryption**:
    *   **BCrypt (Hashing)**: Dùng cho mật khẩu người dùng (One-way, không cần và không thể dịch ngược).
    *   **AES (Encryption)**: Dùng cho `Secret Key` của Bot vì hệ thống cần giải mã ra để tính toán chữ ký HMAC khi xác thực Request.
*   **HMAC Validation**:
    *   Phải dùng **Timing-safe comparison** (`MessageDigest.isEqual`) để chống lại tấn công Timing Attack.
    *   Sử dụng `HexFormat` (Java 17+) để xử lý chuỗi Hex thay vì code thủ công để tránh lỗi encode/decode.
*   **Master Key**: Sử dụng một chìa khóa vạn năng (Master Key) quản lý qua biến môi trường (Environment Variable) để mã hóa toàn bộ dữ liệu nhạy cảm trong DB.

## 3. Quản lý Dữ liệu & Spring Data JPA
*   **Technical ID Strategy**: Ưu tiên sử dụng `String` (UUID) cho technical primary keys để tăng tính bảo mật (không đoán được ID tiếp theo) và hỗ trợ hệ thống phân tán.
*   **Optional Mapping**: Sử dụng `.map(mapper::toDomain)` để chuyển đổi từ Entity sang Domain Model một cách an toàn (Null-safe).
*   **Query Methods**: Tận dụng đặt tên hàm chuẩn của Spring Data (như `existsByIdAndRole`) thay vì viết `@Query` thủ công cho các logic đơn giản để tận dụng tối đa sức mạnh của framework.
*   **Transaction**: Đặt `@Transactional` ở tầng Application (Use Case) để đảm bảo tính nhất quán dữ liệu khi một nghiệp vụ thực hiện nhiều thao tác ghi.

## 4. Kỹ thuật Multi-module Maven
*   **Classpath vs File System**: Tuyệt đối không dùng đường dẫn tương đối kiểu `../../` để tìm file cấu hình. Sử dụng `classpath:` và khai báo dependency đúng giữa các module.
*   **Dependency Scope**: Hiểu rõ khi nào dùng `<scope>provided</scope>` (thư viện có sẵn ở môi trường chạy như Tomcat) và tránh lạm dụng nó cho các thư viện core.
*   **Transitive Dependency**: Module nào dùng code của thư viện nào thì phải khai báo trực tiếp ở module đó (Direct Dependency), không dựa dẫm hoàn toàn vào việc thừa hưởng (Transitive Dependency).

## 5. Danh sách các "Điểm mù" (Common Gotchas)
*   **IDE nhận nhưng Runtime lỗi**: Thường do thiếu dependency trong file đóng gói hoặc sai cấu hình classpath trong module `marcus-api`.
*   **Filter Order**: `AuthenticationFilter` là class chung chung, cần dùng một mốc cụ thể trong Spring Security (như `SecurityContextHolderFilter`) để đăng ký thứ tự chạy chính xác.
*   **Authentication vs Authorization**: Phân biệt rõ việc nhận diện Bot (qua API Key) và xác thực tính toàn vẹn dữ liệu/danh tính (qua HMAC Signature).
