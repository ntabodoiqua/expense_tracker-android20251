# BÁO CÁO ĐỒ ÁN

# ỨNG DỤNG QUẢN LÝ CHI TIÊU - SMARTWALLET

**Môn học:** Phát triển ứng dụng cho thiết bị di động  
**Ngày báo cáo:** 15/01/2026

---

## MỤC LỤC

1. [Giới thiệu dự án](#1-giới-thiệu-dự-án)
2. [Mục tiêu và phạm vi](#2-mục-tiêu-và-phạm-vi)
3. [Công nghệ và kiến trúc](#3-công-nghệ-và-kiến-trúc)
4. [Tính năng chính](#4-tính-năng-chính)
5. [Thiết kế và giao diện](#5-thiết-kế-và-giao-diện)
6. [Cơ sở dữ liệu](#6-cơ-sở-dữ-liệu)
7. [Kiến trúc ứng dụng](#7-kiến-trúc-ứng-dụng)
8. [Quy trình phát triển](#8-quy-trình-phát-triển)
9. [Testing và đảm bảo chất lượng](#9-testing-và-đảm-bảo-chất-lượng)
10. [Thách thức và giải pháp](#10-thách-thức-và-giải-pháp)
11. [Kết quả đạt được](#11-kết-quả-đạt-được)
12. [Hướng phát triển tương lai](#12-hướng-phát-triển-tương-lai)
13. [Kết luận](#13-kết-luận)
14. [Tài liệu tham khảo](#14-tài-liệu-tham-khảo)

---

## 1. GIỚI THIỆU DỰ ÁN

### 1.1. Tên ứng dụng

**SmartWallet - Ứng dụng quản lý chi tiêu thông minh**

### 1.2. Đối tượng người dùng

- Sinh viên, người đi làm cần quản lý tài chính cá nhân
- Người muốn theo dõi thu chi hàng ngày/tháng/năm
- Người có nhu cầu phân tích thói quen chi tiêu

### 1.3. Vấn đề giải quyết

- Quản lý thu chi cá nhân một cách hiệu quả
- Theo dõi các khoản chi tiêu theo danh mục
- Phân tích thói quen chi tiêu qua biểu đồ trực quan
- Cảnh báo chi tiêu vượt mức
- Lưu trữ lịch sử giao dịch

### 1.4. Ý nghĩa của dự án

Ứng dụng giúp người dùng kiểm soát tài chính tốt hơn, xây dựng thói quen chi tiêu hợp lý, và đưa ra quyết định tài chính thông minh dựa trên dữ liệu phân tích.

---

## 2. MỤC TIÊU VÀ PHẠM VI

### 2.1. Mục tiêu

- **Mục tiêu chính:** Xây dựng ứng dụng quản lý chi tiêu hoàn chỉnh trên nền tảng Android
- **Mục tiêu kỹ thuật:**
  - Áp dụng kiến trúc MVVM (Model-View-ViewModel)
  - Sử dụng Room Database để lưu trữ dữ liệu local
  - Tích hợp thư viện biểu đồ MPAndroidChart
  - Áp dụng Material Design Guidelines
  - Hỗ trợ đa ngôn ngữ (Tiếng Việt, Tiếng Anh)

### 2.2. Phạm vi

- **Nền tảng:** Android (API 29+)
- **Ngôn ngữ lập trình:** Kotlin
- **Lưu trữ:** Cơ sở dữ liệu local (Room)
- **Phạm vi chức năng:**
  - ✅ Quản lý giao dịch thu/chi
  - ✅ Phân loại theo danh mục
  - ✅ Thống kê và biểu đồ
  - ✅ Tìm kiếm và lọc giao dịch
  - ✅ Cài đặt thông tin người dùng
  - ✅ Thông báo và nhắc nhở

### 2.3. Các tính năng ngoài phạm vi (Future Work)

- Đồng bộ hóa dữ liệu trên cloud
- Xuất báo cáo PDF
- Quản lý ngân sách
- Tích hợp với ngân hàng

---

## 3. CÔNG NGHỆ VÀ KIẾN TRÚC

### 3.1. Nền tảng và ngôn ngữ

- **Platform:** Android
- **Language:** Kotlin 100%
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 36 (Android 14+)
- **Build System:** Gradle (Kotlin DSL)

### 3.2. Thư viện và Framework chính

#### 3.2.1. Android Jetpack Components

```kotlin
// Core Libraries
androidx.core:core-ktx
androidx.appcompat:appcompat
androidx.activity:activity
androidx.constraintlayout

// UI Components
com.google.android.material:material  // Material Design
androidx.cardview:cardview
androidx.recyclerview:recyclerview
```

#### 3.2.2. Room Database (Persistence)

```kotlin
// Room - SQLite Database Wrapper
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1
kapt("androidx.room:room-compiler:2.6.1")
```

#### 3.2.3. Architecture Components (MVVM)

```kotlin
// ViewModel & LiveData
androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
androidx.lifecycle:lifecycle-livedata-ktx:2.7.0
```

#### 3.2.4. Thư viện đồ họa và UI

```kotlin
// Biểu đồ
com.github.PhilJay:MPAndroidChart:v3.1.0

// Load và hiển thị hình ảnh
com.github.bumptech.glide:glide:4.16.0
```

### 3.3. Kiến trúc ứng dụng: MVVM

```
┌─────────────────────────────────────────────────────┐
│                      VIEW LAYER                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │  Activities  │  │  Fragments   │  │  Adapters│  │
│  └──────────────┘  └──────────────┘  └──────────┘  │
└─────────────────────────────────────────────────────┘
                        ▲  │
                        │  ▼
┌─────────────────────────────────────────────────────┐
│                   VIEWMODEL LAYER                   │
│              ┌────────────────────┐                 │
│              │ TransactionViewModel│                 │
│              │  - LiveData        │                 │
│              │  - Business Logic  │                 │
│              └────────────────────┘                 │
└─────────────────────────────────────────────────────┘
                        ▲  │
                        │  ▼
┌─────────────────────────────────────────────────────┐
│                     MODEL LAYER                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │  Repository  │  │     DAO      │  │  Entity  │  │
│  └──────────────┘  └──────────────┘  └──────────┘  │
│                  ┌────────────────┐                 │
│                  │  Room Database │                 │
│                  └────────────────┘                 │
└─────────────────────────────────────────────────────┘
```

---

## 4. TÍNH NĂNG CHÍNH

### 4.1. Màn hình Onboarding (Chào mừng)

**File:** `OnboardingActivity.kt`

**Chức năng:**

- Chào mừng người dùng lần đầu sử dụng app
- Cho phép nhập tên và chọn ảnh đại diện
- Lưu thông tin vào SharedPreferences
- Skip option: cho phép bỏ qua và thiết lập sau

**Công nghệ:**

- SharedPreferences (UserPreferences)
- Glide library (load ảnh)
- ActivityResultContracts (chọn ảnh từ Gallery)

### 4.2. Màn hình Home (Trang chủ)

**File:** `HomeFragment.kt`

**Chức năng:**

- Hiển thị tổng quan thu chi
  - Tổng thu nhập
  - Tổng chi tiêu
  - Chi tiêu hôm nay
  - Chi tiêu tháng này
- Danh sách 5 giao dịch gần nhất
- Nút FAB để thêm giao dịch mới
- Swipe to delete (vuốt để xóa giao dịch)
- Click để chỉnh sửa giao dịch
- Hiển thị badge thông báo

**Công nghệ:**

- RecyclerView với custom adapter
- ItemTouchHelper (swipe gesture)
- LiveData Observer
- Bottom Sheet Dialog

### 4.3. Màn hình Search (Tìm kiếm)

**File:** `SearchFragment.kt`

**Chức năng:**

- Tìm kiếm giao dịch theo tên, ghi chú
- Lọc theo loại (Thu/Chi)
- Lọc theo danh mục
- Lọc theo khoảng thời gian
- Hiển thị kết quả real-time

**Công nghệ:**

- SearchView
- Spinner (dropdown)
- DatePicker Dialog
- Filter logic

### 4.4. Màn hình Chart (Thống kê)

**File:** `ChartFragment.kt`

**Chức năng:**

- Chọn khoảng thời gian: Tuần/Tháng/Năm/Tất cả
- Thống kê tổng quan:
  - Tổng thu nhập
  - Tổng chi tiêu
  - Số dư ròng
  - Số lượng giao dịch
- Biểu đồ tròn (Pie Chart):
  - Phân bố chi tiêu theo danh mục
  - Phân bố thu nhập theo danh mục
- Biểu đồ cột (Bar Chart):
  - Xu hướng thu chi theo thời gian
- Danh sách top giao dịch lớn nhất

**Công nghệ:**

- MPAndroidChart library
- Pie Chart với custom legend
- Bar Chart với animations
- Custom ValueFormatter

### 4.5. Màn hình Setting (Cài đặt)

**File:** `SettingFragment.kt`

**Chức năng:**

- Hiển thị và chỉnh sửa thông tin cá nhân
- Thay đổi ảnh đại diện
- Đổi tên người dùng
- Chọn ngôn ngữ (Tiếng Việt/English)
- Xóa toàn bộ dữ liệu
- Thông tin ứng dụng

**Công nghệ:**

- SharedPreferences
- AlertDialog
- Locale configuration

### 4.6. Màn hình Notification (Thông báo)

**File:** `NotificationFragment.kt`

**Chức năng:**

- Hiển thị các thông báo về:
  - Chi tiêu vượt mức trong ngày (>200,000đ)
  - Chi tiêu vượt mức trong tháng (>5,000,000đ)
  - Tóm tắt tình hình tài chính
- Sắp xếp theo thời gian mới nhất
- Badge đếm số thông báo chưa đọc

**Công nghệ:**

- RecyclerView với custom adapter
- Calendar API
- Badge notification

### 4.7. Add/Edit Transaction (Thêm/Sửa giao dịch)

**File:** `AddTransactionFragment.kt`

**Chức năng:**

- Bottom Sheet Dialog
- Nhập số tiền
- Chọn loại: Thu nhập/Chi tiêu
- Chọn danh mục từ dropdown
- Nhập ghi chú
- Chọn ngày giờ
- Validation dữ liệu

**Các danh mục:**

- **Chi tiêu:** Ăn uống, Giải trí, Mua sắm, Y tế, Giáo dục, Di chuyển, Hóa đơn, Khác
- **Thu nhập:** Lương, Thưởng, Đầu tư, Quà tặng, Bán hàng, Khác

**Công nghệ:**

- BottomSheetDialogFragment
- Spinner
- DatePicker, TimePicker
- Data validation

---

## 5. THIẾT KẾ VÀ GIAO DIỆN

### 5.1. Nguyên tắc thiết kế

- **Material Design 3:** Tuân thủ Material Design Guidelines của Google
- **Color Scheme:**
  - Primary: #6200EA (Purple)
  - Accent: #03DAC5 (Teal)
  - Background: #FFFFFF (Light), #121212 (Dark)
- **Typography:** Roboto font family
- **Dark Mode Support:** Hỗ trợ chế độ tối

### 5.2. Layout Structure

#### MainActivity

```
┌────────────────────────────────┐
│      Fragment Container        │
│   (Home/Search/Chart/Setting)  │
│                                │
└────────────────────────────────┘
┌────────────────────────────────┐
│    Bottom Navigation View      │
│  [Home] [Search] [Chart] [⚙️]  │
└────────────────────────────────┘
```

#### HomeFragment

```
┌────────────────────────────────┐
│  Avatar  |  Chào mừng, User 🔔│
├────────────────────────────────┤
│  ┌───────┐ ┌───────┐ ┌───────┐│
│  │  Thu  │ │  Chi  │ │ H.Nay ││
│  └───────┘ └───────┘ └───────┘│
├────────────────────────────────┤
│  Giao dịch gần đây    [Xem >] │
│  ┌────────────────────────────┐│
│  │ 🍜 Ăn sáng      -50,000đ   ││
│  │ 💰 Lương     +5,000,000đ   ││
│  └────────────────────────────┘│
└────────────────────────────────┘
                            [ + ]
```

### 5.3. Màu sắc và Icons

- **Icons:** Material Icons
- **Expense color:** Red (#F44336)
- **Income color:** Green (#4CAF50)
- **Charts:** Colorful palette

---

## 6. CƠ SỞ DỮ LIỆU

### 6.1. Database Schema

#### Transaction Entity

```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,        // Tiêu đề giao dịch
    val amount: Double,       // Số tiền
    val type: Int,            // 0: Chi tiêu, 1: Thu nhập
    val category: String,     // Danh mục
    val note: String = "",    // Ghi chú
    val date: Long            // Timestamp (milliseconds)
)
```

### 6.2. DAO (Data Access Object)

```kotlin
@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
```

### 6.3. Database Instance (Singleton Pattern)

```kotlin
@Database(entities = [Transaction::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db"
                ).build()
            }
        }
    }
}
```

---

## 7. KIẾN TRÚC ỨNG DỤNG

### 7.1. MVVM Pattern

#### Model

- **Transaction.kt:** Entity class
- **TransactionDao.kt:** Database operations
- **AppDatabase.kt:** Database instance
- **UserPreferences.kt:** SharedPreferences wrapper

#### View

- **Activities:** OnboardingActivity, MainActivity
- **Fragments:** HomeFragment, SearchFragment, ChartFragment, SettingFragment, NotificationFragment
- **Adapters:** TransactionAdapter

#### ViewModel

- **TransactionViewModel.kt:**
  - Quản lý LiveData
  - Business logic
  - Giao tiếp với Repository/DAO

### 7.2. Data Flow

```
User Action (View)
    ↓
ViewModel (Business Logic)
    ↓
Repository/DAO (Data Layer)
    ↓
Room Database
    ↓
LiveData/Flow (Observe)
    ↓
ViewModel
    ↓
View (Update UI)
```

### 7.3. Package Structure

```
com.example.expensetracker/
├── data/
│   ├── AppDatabase.kt
│   ├── Transaction.kt
│   ├── TransactionDao.kt
│   └── UserPreferences.kt
├── viewmodel/
│   └── TransactionViewModel.kt
├── ui/
│   ├── TransactionAdapter.kt
│   └── AddTransactionFragment.kt
├── Activities (root)
│   ├── MainActivity.kt
│   └── OnboardingActivity.kt
└── Fragments (root)
    ├── HomeFragment.kt
    ├── SearchFragment.kt
    ├── ChartFragment.kt
    ├── SettingFragment.kt
    └── NotificationFragment.kt
```

---

## 8. QUY TRÌNH PHÁT TRIỂN

### 8.1. Phương pháp phát triển

- **Agile/Scrum methodology**
- Phát triển theo sprint (1-2 tuần/sprint)

### 8.2. Các giai đoạn

#### Giai đoạn 1: Lập kế hoạch và Thiết kế (1 tuần)

- Phân tích yêu cầu
- Thiết kế mockup/wireframe
- Thiết kế database schema
- Chọn công nghệ và thư viện

#### Giai đoạn 2: Xây dựng cơ sở (2 tuần)

- Setup project Android
- Cấu hình Gradle dependencies
- Tạo database với Room
- Implement MVVM architecture
- Tạo UI cơ bản với Material Design

#### Giai đoạn 3: Phát triển tính năng (3 tuần)

- **Week 1:** Onboarding + Home + Add Transaction
- **Week 2:** Search + Notification
- **Week 3:** Chart + Statistics + Settings

#### Giai đoạn 4: Testing và Tối ưu (1 tuần)

- Unit testing
- UI testing
- Fix bugs
- Performance optimization
- Code refactoring

#### Giai đoạn 5: Hoàn thiện (1 tuần)

- Đa ngôn ngữ
- Dark mode
- Polish UI/UX
- Viết documentation

### 8.3. Version Control

- **Git:** Quản lý source code
- **Branching strategy:**
  - `main`: Production-ready code
  - `develop`: Development branch
  - `feature/*`: Feature branches

---

## 9. TESTING VÀ ĐẢM BẢO CHẤT LƯỢNG

### 9.1. Unit Testing

```kotlin
// Example: TransactionViewModel Test
@Test
fun testAddTransaction() {
    val transaction = Transaction(
        title = "Test",
        amount = 100.0,
        type = 0,
        category = "Food"
    )
    viewModel.addTransaction(transaction)
    // Assert...
}
```

### 9.2. UI Testing

- Sử dụng Espresso framework
- Test các user flow chính

### 9.3. Manual Testing

- Test trên nhiều thiết bị khác nhau
- Test các Android version khác nhau
- Test dark mode
- Test đa ngôn ngữ

### 9.4. Performance

- Không memory leak
- Database query tối ưu
- Smooth animations (60 FPS)
- App size tối ưu

---

## 10. THÁCH THỨC VÀ GIẢI PHÁP

### 10.1. Thách thức 1: Quản lý State với LiveData

**Vấn đề:** Cập nhật UI real-time khi data thay đổi

**Giải pháp:**

- Sử dụng LiveData và Flow từ Room
- Observer pattern trong Fragment
- ViewModel để retain data qua configuration changes

### 10.2. Thách thức 2: Complex Chart Rendering

**Vấn đề:** Vẽ biểu đồ phức tạp với nhiều dữ liệu

**Giải pháp:**

- Sử dụng MPAndroidChart library
- Custom ValueFormatter
- Lazy loading và pagination
- Cache data đã xử lý

### 10.3. Thách thức 3: Date/Time Handling

**Vấn đề:** Xử lý múi giờ, format ngày tháng

**Giải pháp:**

- Lưu timestamp (Long) trong database
- Sử dụng Calendar và SimpleDateFormat
- Consistent timezone handling

### 10.4. Thách thức 4: Image Persistence

**Vấn đề:** Lưu trữ và load ảnh đại diện

**Giải pháp:**

- Sử dụng URI với persistable permission
- Fallback về ảnh mặc định nếu lỗi
- Glide library để cache và load hiệu quả

### 10.5. Thách thức 5: Đa ngôn ngữ

**Vấn đề:** Hỗ trợ nhiều ngôn ngữ

**Giải pháp:**

- Resource strings trong values/values-vi
- Configuration change handling
- Restart activity khi đổi ngôn ngữ

---

## 11. KẾT QUẢ ĐẠT ĐƯỢC

### 11.1. Các tính năng đã hoàn thành

✅ Onboarding cho người dùng mới  
✅ Quản lý giao dịch CRUD đầy đủ  
✅ Phân loại theo danh mục  
✅ Tìm kiếm và lọc nâng cao  
✅ Thống kê đa dạng (Pie Chart, Bar Chart)  
✅ Thông báo thông minh  
✅ Cài đặt cá nhân hóa  
✅ Hỗ trợ đa ngôn ngữ  
✅ Material Design 3  
✅ Dark Mode

### 11.2. Metrics

- **Lines of Code:** ~3,500+ lines
- **Files:** 16 Kotlin files + 11 XML layouts
- **App Size:** ~8-10 MB
- **Min Android Version:** Android 10 (API 29)
- **Target Devices:** Smartphone và Tablet

### 11.3. Screenshots

_(Có thể chụp màn hình ứng dụng và thêm vào đây)_

---

## 12. HƯỚNG PHÁT TRIỂN TƯƠNG LAI

### 12.1. Tính năng mới

- 📊 **Quản lý ngân sách:** Đặt giới hạn chi tiêu cho từng danh mục
- ☁️ **Cloud Sync:** Đồng bộ dữ liệu qua Firebase
- 📱 **Multi-platform:** Phát triển phiên bản iOS
- 💳 **Bank Integration:** Liên kết với tài khoản ngân hàng
- 📄 **Export PDF:** Xuất báo cáo chi tiết
- 🔔 **Push Notification:** Nhắc nhở định kỳ
- 👥 **Multi-user:** Chia sẻ sổ thu chi gia đình

### 12.2. Cải thiện kỹ thuật

- Migration sang Jetpack Compose
- Implement Repository Pattern
- Dependency Injection với Hilt
- CI/CD pipeline
- Automated testing coverage > 80%

### 12.3. UX Improvements

- Onboarding tutorial
- Tutorial tooltips
- Gesture navigation
- Widget cho home screen
- Shortcuts

---

## 13. KẾT LUẬN

### 13.1. Đánh giá chung

Dự án **SmartWallet** đã hoàn thành đầy đủ các yêu cầu của môn học Phát triển ứng dụng di động. Ứng dụng được xây dựng với kiến trúc MVVM chuẩn, sử dụng các công nghệ Android hiện đại như Room Database, LiveData, và Material Design 3.

### 13.2. Kiến thức đã học

- **Android Development:** Activity, Fragment lifecycle, Navigation
- **Kotlin Programming:** Coroutines, Extension functions, Data classes
- **Architecture Pattern:** MVVM, Repository pattern
- **Database:** Room ORM, SQLite
- **UI/UX:** Material Design, Custom Views, Animations
- **Libraries:** Glide, MPAndroidChart
- **Tools:** Android Studio, Gradle, Git

### 13.3. Kỹ năng đạt được

- Phân tích và thiết kế ứng dụng
- Áp dụng design pattern
- Quản lý state và lifecycle
- Xử lý dữ liệu bất đồng bộ
- Tối ưu performance
- Testing và debugging
- Version control với Git

### 13.4. Lời cảm ơn

Em xin chân thành cảm ơn Thầy/Cô đã hướng dẫn và tạo điều kiện cho em hoàn thành đồ án này. Qua quá trình thực hiện, em đã học hỏi được rất nhiều kiến thức và kỹ năng quý báu về phát triển ứng dụng di động.

---

## 14. TÀI LIỆU THAM KHẢO

### 14.1. Official Documentation

1. **Android Developers:** https://developer.android.com/
2. **Kotlin Documentation:** https://kotlinlang.org/docs/home.html
3. **Room Database Guide:** https://developer.android.com/training/data-storage/room
4. **Material Design Guidelines:** https://m3.material.io/

### 14.2. Libraries Documentation

5. **MPAndroidChart:** https://github.com/PhilJay/MPAndroidChart
6. **Glide:** https://github.com/bumptech/glide
7. **Lifecycle & ViewModel:** https://developer.android.com/topic/libraries/architecture

### 14.3. Learning Resources

8. **Udacity Android Courses**
9. **Google Codelabs**
10. **Stack Overflow**

### 14.4. Tools

11. **Android Studio:** https://developer.android.com/studio
12. **Git:** https://git-scm.com/
13. **Figma (Design):** https://www.figma.com/

---

**Sinh viên thực hiện:** [Tên của bạn]  
**MSSV:** [Mã số sinh viên]  
**Lớp:** [Lớp học]  
**Email:** [Email của bạn]

**Ngày hoàn thành:** 15/01/2026
