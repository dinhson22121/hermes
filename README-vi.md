# Hermes

Hermes là một dự án quản lý balance và freeze balance của các tài khoản (account) với mục tiêu đảm bảo tính đồng bộ và nhất quán trong môi trường phân tán. Dự án được thiết kế để xử lý các thay đổi balance của người dùng một cách an toàn, tránh các tình trạng race condition.

## Tính năng chính

- **Quản lý balance và freeze balance**: Cung cấp các thao tác cho balance và freeze balance của tài khoản.
- **Đảm bảo đồng bộ trong môi trường phân tán**: Sử dụng Redis lock và event sourcing để đảm bảo các thay đổi balance của mỗi người dùng chỉ được thực thi bởi một luồng duy nhất tại bất kỳ thời điểm nào.
- **Xử lý operation từ Kafka**: Hermes nhận các yêu cầu (operation) từ Kafka để thực thi các thao tác trên tài khoản.

## Kiến trúc và triển khai

- **Redis Lock**: Đảm bảo tính đồng bộ trong môi trường đa luồng, ngăn ngừa tình trạng race condition.
- **Event Sourcing**: Lưu trữ các sự kiện thay đổi nhằm duy trì lịch sử và trạng thái của mỗi tài khoản.
- **Kafka**: Là nguồn tiếp nhận và phân phối các operation đến các luồng xử lý trong hệ thống.

## Performance Benchmarks

- **Environment**:
   - **Hardware**: MacBook M1 Pro with 32GB RAM
   - **Setup**:
      - 1 node Kafka
      - 1 node Redis
      - 1 node MongoDB
      - 1 node Hermes

- **Load Details**:
   - **Total Accounts**: 100,000
   - **Operations per Account**: 10
   - **Total Operations**: 1,000,000

- **Performance Metrics**:
   - **Average Response Time**: 1 second
   - **Response Time Distribution**: 70% of requests complete in under 300 milliseconds, ensuring high responsiveness under load.

## Hướng dẫn cài đặt

### Yêu cầu hệ thống

- Java 21
- Redis
- Kafka
- MongoDB

### Cài đặt

1. **Clone dự án**:
   ```bash
   git clone https://github.com/username/hermes.git
   cd hermes
   ```

2. **Cấu hình**: Điều chỉnh file cấu hình để kết nối với Kafka, Redis, và MongoDB.

3. **Chạy dự án**:
   ```bash
   ./mvnw spring-boot:run
   ```

## Sử dụng

Hermes sẽ tự động lắng nghe các operation từ Kafka và xử lý các thao tác trên tài khoản. Mọi thay đổi trên balance của người dùng đều được quản lý chặt chẽ và đảm bảo an toàn trong môi trường đa luồng.

## Đóng góp

Các đóng góp về cải tiến hiệu suất và tính năng của dự án đều được hoan nghênh. Hãy mở một pull request hoặc issue để bắt đầu.

---

Hy vọng nội dung này đáp ứng nhu cầu của bạn.
