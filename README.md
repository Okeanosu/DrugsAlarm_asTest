**DrugsAlarm**

DrugsAlarm là một ứng dụng Android giúp bạn quản lý việc uống thuốc đúng giờ, theo dõi kho thuốc và thống kê tiến độ điều trị.

----------------

**Tính năng nổi bật**
**Nhắc nhở thông minh**
- **Hệ thống báo thức:** Sử dụng chuông báo thức hệ thống và rung để đảm bảo bạn không bỏ lỡ liều thuốc nào.
- **Tần suất linh hoạt:** Hỗ trợ nhiều chế độ lặp lại:
    - Theo giờ: Mỗi 1 giờ, 2 giờ, 4 giờ, 8 giờ, 12 giờ.
    - Hàng ngày, hàng tuần, mỗi 2 tuần, hàng tháng.
    - Uống một lần duy nhất.
- **Thông báo nhanh:** Cho phép xác nhận "Đã uống" ngay từ thanh thông báo mà không cần mở ứng dụng.

----------------

### Dashboard thống kê
- **Biểu đồ hàng tuần:** Theo dõi tỉ lệ uống thuốc (Đã uống và Bỏ lỡ) trong 7 ngày gần nhất qua biểu đồ cột sinh động.
- **Tương tác trực quan:** Nhấn vào từng cột trên biểu đồ để xem chi tiết danh sách thuốc và thời gian cụ thể của ngày đó.
- **Uống bù:** Cho phép xác nhận uống bù trong mục **Lịch sử** và tự động update trên bảng thống kê

### Quản lý thuốc
- **Theo dõi số lượng:** Tự động trừ số lượng thuốc sau mỗi lần xác nhận uống.
- **Cảnh báo hết thuốc:** Hiển thị màu sắc cảnh báo và nút **"Nạp lại"** nhanh khi số lượng thuốc trong kho xuống thấp (<=5).

### Lịch sử chi tiết
- Lưu trữ toàn bộ quá trình uống thuốc.
- Hỗ trợ chỉnh sửa trạng thái (Đã uống, Bỏ lỡ, Tạm dừng) hoặc thời gian trong quá khứ.
- Chức năng xóa từng mục hoặc xóa toàn bộ lịch sử.
  
----------------

## Công nghệ sử dụng

- **Ngôn ngữ:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Cơ sở dữ liệu:** Room Persistence Library
- **Xử lý nền:** AlarmManager & BroadcastReceivers
- **Kiến trúc:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** ViewModelProvider

----------------

## Cấu trúc dự án

- `com.example.drugsalarm.data`: Chứa các Entity (Medicine, IntakeLog), DAO và Database logic.
- `com.example.drugsalarm.receiver`: Xử lý các sự kiện hệ thống (Báo thức, Khởi động máy, Hành động từ thông báo).
- `com.example.drugsalarm.ui`: Chứa các Composable functions cấu thành giao diện người dùng.

----------------

## Màn hình danh sách thuốc

  <img width="351" height="777" alt="image" src="https://github.com/user-attachments/assets/001b350a-ae89-4c43-b8e0-ad65ca6e5bce" />
  
## Biểu đồ thống kê

  <img width="350" height="779" alt="image" src="https://github.com/user-attachments/assets/6f18535d-0e52-47d1-8f59-56b5b269ad01" />
  
  <img width="355" height="779" alt="image" src="https://github.com/user-attachments/assets/5d4782ff-1f2a-44f2-8f2f-bd82b14d6ba8" />
  
  <img width="349" height="774" alt="image" src="https://github.com/user-attachments/assets/9b539aae-8055-4404-9a0c-1715631f48c3" />
  
## Thông báo nhắc nhở

  <img width="349" height="781" alt="image" src="https://github.com/user-attachments/assets/cbd2c4f8-7f85-4fde-87b0-a2cc2899fc64" />

----------------

## Cài đặt

1. Clone repository.
2. Mở bằng **Android Studio)**.
3. Requirement: JDK 11.
