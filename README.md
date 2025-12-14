# LocalChat P2P
## Đồ án môn học Các hệ thống phân tán PTITHCM
Ứng dụng desktop cho phép nhắn tin trực tiếp và nhắn tin nhóm trong mạng nội bộ (LAN) theo mô hình Peer-to-Peer (P2P)
phi tập trung, không cần máy chủ trung gian, không cần lưu dữ liệu chat(dữ liệu được đồng bộ khi online lại, nếu toàn
bộ náy trong mạng offline thì xem như mất hết dữ liệu).


## Các tính năng chính
- Kết nối phi tập trung (P2P): Các máy trạm tự động tìm thấy nhau trong mạng LAN mà không cần Server trung tâm
(Thông qua UDP multicast và unicast).
- Các máy được định danh bằng chữ ký số. Tin nhắn hội thoại được lưu trong phiên hoạt động và đồng bộ khi online lại
  (Nếu các máy khác cũng offline hết thì xem như mất tin nhắn).
- Hỗ trợ nhắn tin 1-1 và nhắn tin theo nhóm.
- Bảo mật tin nhắn với E2E Encryption:
  - Trao đổi khóa phiên (Session Key) bảo mật bằng RSA-2048.
  - Mã hóa nội dung tin nhắn bằng AES-256.
  - Xác thực gói tin bằng Chữ ký số.
- Đồng bộ dữ liệu:
  - Sử dụng Lamport Clock để sắp xếp thứ tự tin nhắn logic.
  - Cơ chế Sync Metadata & Fetch Message để đồng bộ lịch sử chat khi online trở lại.

    
# Hướng dẫn tải và cài đặt

## Cấu hình yêu cầu

- Đồ án này được code dựa trên ngôn ngữ java
- Phiên bản JDK: 25
- Các phiên bản JDK cũ hơn có thể gây lỗi cú pháp(các bản JDK cũ hơn không hỗ trợ các cú pháp mới như import module; 
IO.println(); IO.readln() có thể gây lỗi)
- Build: dùng các IDE (intelliJ, vscode , ...) hoặc tối thiểu cần cài maven để build và chạy bằng command terminal

## Tải và chạy chương trình

### Kiểm tra phiên bản (yêu cầu java 25 trở lên)

```commandline
java --version
```

### Clone project & Build & Run

```commandline
git clone https://github.com/TuLe142857/LocalChat.git
cd LocalChat
mvn clean package
mvn javafx:run
```
