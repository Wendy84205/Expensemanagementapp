package com.example.financeapp.viewmodel.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Language(val code: String, val name: String)

class LanguageViewModel : ViewModel() {
    private val _currentLanguage = MutableStateFlow(Language("vi", "Tiếng Việt"))
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _languageCode = MutableStateFlow("vi")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    // Translations dictionary
    private val translations = mapOf(
        "vi" to mapOf(
            // Basic app and navigation
            "language_settings" to "Cài đặt ngôn ngữ",
            "choose_language" to "Chọn ngôn ngữ",
            "current_language" to "Ngôn ngữ hiện tại",
            "language_saved" to "Đã lưu ngôn ngữ",
            "language_reset" to "Đã khôi phục ngôn ngữ",
            "save" to "Lưu",
            "reset" to "Khôi phục",
            "language" to "Ngôn ngữ",
            "app_title" to "Quản lý chi tiêu",
            "cancel" to "Hủy",
            "back" to "Quay lại",
            "next" to "Tiếp theo",
            "loading" to "Đang tải",
            "error" to "Lỗi",
            "success" to "Thành công",
            "warning" to "Cảnh báo",
            "info" to "Thông tin",
            "add" to "Thêm",
            "edit" to "Sửa",
            "delete" to "Xóa",
            "confirm" to "Xác nhận",
            "extensions" to "Tiện ích mở rộng",
            "extra_tools_like_ai_calendar_scan" to "Các công cụ bổ sung như Danh Mục, Ngân Sách, Chi Tiêu Định Kỳ",

            // ========== MỚI: THÊM TỪ HOME SCREEN ==========
            // HomeScreen specific
            "greeting" to "Xin chào",
            "user" to "Người dùng",
            "monthly_spending_title" to "Số tiền bạn đã chi trong tháng",
            "view_details" to "Xem chi tiết",
            "spent_this_month" to "Số tiền đã chi tiêu trong tháng này",
            "classification_by_type" to "Chi theo phân loại",
            "recent_transactions" to "Giao dịch gần đây",
            "view_all" to "Xem tất cả",
            "income" to "Thu",
            "expense" to "Chi",
            "no_recent_transactions" to "Bạn chưa có giao dịch gần đây",
            "create_transaction" to "Tạo giao dịch",
            "overview" to "Tổng quan",
            "this_month" to "Tháng này",
            "spending_limit" to "Hạn mức chi tiêu",
            "create_or_select_fund_for_limit" to "Tạo hoặc lựa chọn quỹ tiết kiệm",
            "limit_description" to "để chúng tôi tính toán hạn mức chi tiêu",
            "select_or_create_fund" to "Lựa chọn / Tạo quỹ tiết kiệm",
            "no_chart_data" to "Chưa có dữ liệu biểu đồ",
            "spending_by_category" to "Chi theo phân loại", // Mới
            "daily_spending" to "Chi tiêu hằng ngày", // Mới
            "savings_fund" to "Tiền sâu TH", // Mới
            "needs" to "Cần thiết", // Mới
            "training" to "Đào tạo", // Mới
            "entertainment" to "Hoan hỉ", // Mới
            "savings" to "Tiết kiệm", // Mới
            "self_care" to "Tự thẩm", // Mới
            "free_spending" to "Tự do", // Mới
            "limit" to "Hạn mức", // Mới
            "spent" to "Đã tiêu", // Mới
            "see_more" to "Xem thêm", // Mới
            "monthly_spending_total" to "Số tiền đã chi tiêu trong tháng", // Mới
            "transactions" to "Giao dịch", // Mới cho bottom nav
            "profile" to "Cá nhân", // Mới cho bottom nav
            // =============================================

            // Main navigation
            "home" to "Trang chủ",
            "statistics" to "Thống kê",
            "transactions" to "Giao dịch",
            "categories" to "Danh mục",
            "reports" to "Báo cáo",
            "settings" to "Cài đặt",
            "account" to "Tài khoản",

            // HomeScreen (phần cũ - giữ lại cho tương thích)
            "greeting" to "Xin chào 👋",
            "total_balance" to "Tổng số dư",
            "need_attention" to "Cần chú ý",
            "stable" to "Ổn định",
            "status" to "Tình trạng",
            "financial_trend" to "Tình hình thu chi",
            "month" to "Tháng",
            "spending" to "Chi tiêu",
            "income" to "Thu nhập",
            "total" to "Tổng",
            "line_chart" to "Biểu đồ đường",
            "column_chart" to "Biểu đồ cột",
            "pie_chart" to "Biểu đồ tròn",
            "no_data" to "Chưa có dữ liệu để hiển thị",
            "no_spending_data" to "Chưa có dữ liệu chi tiêu để hiển thị",
            "no_transactions" to "Chưa có giao dịch nào",
            "reset_password" to "Thay đổi mật khẩu",
            "time_range" to "Khoảng thời gian",
            "data_type" to "Loại dữ liệu",

            // TransactionScreen
            "transaction_book" to "Sổ giao dịch",
            "transactions_count" to "giao dịch",
            "financial_overview" to "Tổng quan tài chính",
            "monthly_income" to "Thu tháng",
            "monthly_expense" to "Chi tháng",
            "transaction_count" to "Số giao dịch",
            "transaction_history" to "Lịch sử giao dịch",
            "no_note" to "Không có ghi chú",
            "delete_transaction" to "Xóa giao dịch",
            "delete_confirmation" to "Bạn có chắc chắn muốn xóa giao dịch",
            "confirm_delete" to "Xác nhận xóa",
            "no_transactions_description" to "Bắt đầu bằng cách thêm giao dịch đầu tiên của bạn",

            // AddTransactionScreen
            "record_transaction" to "Ghi lại giao dịch",
            "manual_input" to "Nhập thủ công",
            "image_input" to "Nhập từ ảnh",
            "select_category" to "Chọn danh mục",
            "transaction_date" to "Ngày giao dịch",
            "today" to "Hôm nay",
            "repeat_frequency" to "Tần suất lặp lại",
            "no_repeat" to "Không lặp lại",
            "daily" to "Hàng ngày",
            "weekly" to "Hàng tuần",
            "monthly" to "Hàng tháng",
            "note" to "Ghi chú",
            "enter_transaction_description" to "Nhập mô tả giao dịch",
            "update_transaction" to "Cập nhật giao dịch",
            "add_income_transaction" to "Thêm thu nhập",
            "add_expense_transaction" to "Thêm chi tiêu",
            "delete_transaction_dialog" to "Xóa giao dịch",
            "delete_transaction_description" to "Bạn có chắc chắn muốn xóa giao dịch này?",
            "delete_action" to "Xóa",

            // Categories
            "food_drink" to "Ăn uống",
            "shopping" to "Mua sắm",
            "family" to "Gia đình",
            "other" to "Khác",

            // Calendar Screen
            "calendar" to "Lịch",
            "difference" to "Chênh lệch",
            "transaction_list" to "Danh sách giao dịch",
            "select_other_day" to "Chọn ngày khác để xem giao dịch",
            "previous_month" to "Tháng trước",
            "next_month" to "Tháng sau",

            // Days of week (short)
            "monday_short" to "T2",
            "tuesday_short" to "T3",
            "wednesday_short" to "T4",
            "thursday_short" to "T5",
            "friday_short" to "T6",
            "saturday_short" to "T7",
            "sunday_short" to "CN",

            // Days of week (full)
            "monday" to "Thứ Hai",
            "tuesday" to "Thứ Ba",
            "wednesday" to "Thứ Tư",
            "thursday" to "Thứ Năm",
            "friday" to "Thứ Sáu",
            "saturday" to "Thứ Bảy",
            "sunday" to "Chủ Nhật",

            // Months
            "january" to "Tháng 1",
            "february" to "Tháng 2",
            "march" to "Tháng 3",
            "april" to "Tháng 4",
            "may" to "Tháng 5",
            "june" to "Tháng 6",
            "july" to "Tháng 7",
            "august" to "Tháng 8",
            "september" to "Tháng 9",
            "october" to "Tháng 10",
            "november" to "Tháng 11",
            "december" to "Tháng 12",
            "yearly_comparison" to "Biến động",

            // Help & Support
            "help_support" to "Trợ giúp & Hỗ trợ",
            "faq" to "Câu hỏi thường gặp",
            "faq_add_transaction" to "Làm sao để thêm giao dịch?",
            "faq_add_transaction_answer" to "Vào Trang chủ → bấm nút \"+\" màu xanh ở góc phải dưới màn hình",
            "faq_view_statistics" to "Làm sao để xem thống kê?",
            "faq_view_statistics_answer" to "Vào mục Thống kê → chọn khoảng thời gian và loại dữ liệu muốn xem",
            "faq_logout" to "Làm sao để đăng xuất?",
            "faq_logout_answer" to "Vào Cài đặt → cuộn xuống → chọn \"Đăng xuất\"",
            "faq_change_theme" to "Làm sao để thay đổi theme?",
            "faq_change_theme_answer" to "Vào Cài đặt → bật/tắt \"Chế độ giao diện\"",
            "contact_support" to "Liên hệ hỗ trợ",
            "contact_description" to "Nếu bạn cần hỗ trợ thêm hoặc gặp sự cố kỹ thuật, vui lòng liên hệ:",
            "email" to "Email",
            "website" to "Website",
            "working_hours" to "Giờ làm việc",
            "response_time" to "Phản hồi trong 24h",
            "detailed_guide" to "Hướng dẫn chi tiết",
            "weekdays" to "Thứ 2 - Thứ 6",
            "working_time" to "8:00 - 17:00",
            "usage_tips" to "Mẹo sử dụng",
            "tip_categories" to "Sử dụng danh mục để phân loại chi tiêu rõ ràng",
            "tip_savings" to "Đặt mục tiêu tiết kiệm để theo dõi tiến độ",
            "tip_statistics" to "Xem thống kê hàng tháng để điều chỉnh chi tiêu",
            "tip_reminders" to "Sử dụng tính năng nhắc nhở cho hóa đơn định kỳ",

            // Common transaction fields
            "amount" to "Số tiền",
            "description" to "Mô tả",
            "date" to "Ngày",
            "category" to "Danh mục",
            "add_expense" to "Thêm chi tiêu",
            "expense_list" to "Danh sách chi tiêu",
            "add_transaction" to "Thêm giao dịch",
            "total_income" to "Tổng thu",
            "total_expense" to "Tổng chi",
            "this_week" to "Tuần này",
            "this_year" to "Năm nay",
            "trend_analysis" to "Phân tích xu hướng",
            "category_analysis" to "Phân tích danh mục",

            // Notification settings
            "notification_settings" to "Cài đặt thông báo",
            "customize_notifications" to "Tùy chỉnh cách bạn nhận thông báo",
            "notification_types" to "Loại thông báo",
            "push_notifications" to "Thông báo đẩy (Push)",
            "email_notifications" to "Thông báo qua Email",
            "sms_notifications" to "Thông báo qua SMS",
            "financial_alerts" to "Cảnh báo tài chính",
            "low_balance_alert" to "Cảnh báo số dư thấp",
            "large_transaction_alert" to "Cảnh báo giao dịch lớn",
            "monthly_report" to "Báo cáo tháng",
            "restore" to "Khôi phục",
            "save_settings" to "Lưu cài đặt",
            "settings_restored" to "Đã khôi phục cài đặt",
            "settings_saved" to "Đã lưu cài đặt thông báo",

            // Settings and account
            "manage_personal_info" to "Quản lý thông tin cá nhân",
            "notifications" to "Thông báo",
            "enable_disable_notifications" to "Bật / tắt thông báo",
            "theme_mode" to "Chế độ giao diện",
            "about_app" to "Về ứng dụng",
            "sign_out" to "Đăng xuất",
            "logout_account" to "Thoát khỏi tài khoản của bạn",

            // StatisticsScreen
            "financial_fluctuations" to "Biến động thu chi",
            "time_range_weekly" to "Theo tuần",
            "time_range_monthly" to "Theo tháng",
            "time_range_yearly" to "Theo năm",
            "data_type_income" to "Thu nhập",
            "data_type_expense" to "Chi tiêu",
            "data_type_difference" to "Chênh lệch",
            "fluctuations" to "Biến động",
            "compared_to_same_period" to "So với cùng kỳ",
            "same_period_as" to "Bằng cùng kỳ",
            "sub_category" to "Danh mục con",
            "parent_category" to "Danh mục cha",
            "no_transactions_time_period" to "Bạn không có giao dịch nào tại thời gian này",
            "millions" to "(Triệu)",
            "thousands" to "(Nghìn)",
            "last_week" to "tuần trước",
            "last_month" to "tháng trước",
            "last_year" to "năm trước",

            "balance" to "Số dư",
            "current_balance" to "Số dư hiện tại",
            "total_balance" to "Tổng số dư",
            "balance_overview" to "Tổng quan số dư",
            "available_balance" to "Số dư khả dụng",
            "remaining_balance" to "Số dư còn lại",

            // Account info
            "account_info" to "Thông tin tài khoản",
            "update_personal_info" to "Cập nhật thông tin cá nhân",
            "full_name" to "Họ và tên",
            "phone_number" to "Số điện thoại",
            "read_only" to "Chỉ đọc",
            "update_success" to "Cập nhật thành công",
            "saving" to "Đang lưu...",
            "save_changes" to "Lưu thay đổi",
            "system_info" to "Thông tin hệ thống",
            "user_id" to "ID người dùng",
            "not_available" to "Không có sẵn",
            "provider" to "Nhà cung cấp",
            "email_verification" to "Xác thực email",
            "verified" to "Đã xác thực",
            "not_verified" to "Chưa xác thực",
            "created_at" to "Tạo lúc",
            "last_login" to "Đăng nhập lần cuối",
            "unknown" to "Không xác định",
            "email_password" to "Email/Mật khẩu",
            "google" to "Google",
            "facebook" to "Facebook",

            // Category Management
            "create_category" to "Tạo danh mục",
            "click_to_change_icon" to "Chạm để đổi biểu tượng",
            "category_name" to "Tên danh mục",
            "category_name_example" to "Ví dụ: Ăn uống, Mua sắm...",
            "parent_category" to "Danh mục cha",
            "select_parent_category" to "Chọn danh mục cha",
            "select_category" to "Chọn danh mục",
            "icon" to "Biểu tượng",
            "ready_to_create_category" to "Sẵn sàng tạo danh mục",
            "select_icon" to "Chọn biểu tượng",
            "select_group_for_new_category" to "Chọn nhóm cho danh mục mới",
            "search_categories" to "Tìm kiếm danh mục...",
            "found" to "Tìm thấy",
            "categories" to "danh mục",
            "no_categories_found" to "Không tìm thấy danh mục",
            "try_different_keywords" to "Thử từ khóa khác",
            "main_category" to "Danh mục chính",
            "sub_category" to "Danh mục con",
            "category_management" to "Quản lý danh mục",
            "add_new_category" to "Thêm danh mục mới",
            "has" to "Đã có",
            "add_sub_category" to "Thêm danh mục con",
            "no_sub_categories" to "Chưa có danh mục con",
            "sub_categories" to "danh mục con"
        ),
        "en" to mapOf(
            // Basic app and navigation
            "language_settings" to "Language Settings",
            "choose_language" to "Choose Language",
            "current_language" to "Current Language",
            "language_saved" to "Language saved",
            "language_reset" to "Language reset",
            "save" to "Save",
            "reset" to "Reset",
            "language" to "Language",
            "app_title" to "Expense Manager",
            "cancel" to "Cancel",
            "back" to "Back",
            "next" to "Next",
            "loading" to "Loading",
            "error" to "Error",
            "success" to "Success",
            "warning" to "Warning",
            "info" to "Info",
            "add" to "Add",
            "edit" to "Edit",
            "delete" to "Delete",
            "confirm" to "Confirm",
            "current_balance" to "Current balance",
            "balance" to "Balance",
            "current_balance" to "Current Balance",
            "total_balance" to "Total Balance",
            "balance_overview" to "Balance Overview",
            "available_balance" to "Available Balance",
            "remaining_balance" to "Remaining Balance",
            "calendar" to "Calendar",
            "difference" to "Difference",
            "transaction_list" to "Transaction List",
            "select_other_day" to "Select another day to view transactions",
            "previous_month" to "Previous Month",
            "next_month" to "Next Month",
            "extensions" to "Extensions",
            "extra_tools_like_ai_calendar_scan" to "Extra tools like Category, Budget, Any Spend",

            // ========== MỚI: THÊM TỪ HOME SCREEN ==========
            // HomeScreen specific
            "greeting" to "Hello",
            "user" to "User",
            "monthly_spending_title" to "Your monthly spending",
            "view_details" to "View details",
            "spent_this_month" to "Amount spent this month",
            "classification_by_type" to "Spending by category",
            "recent_transactions" to "Recent transactions",
            "view_all" to "View all",
            "income" to "Income",
            "expense" to "Expense",
            "no_recent_transactions" to "No recent transactions",
            "create_transaction" to "Create transaction",
            "overview" to "Overview",
            "this_month" to "This month",
            "spending_limit" to "Spending limit",
            "create_or_select_fund_for_limit" to "Create or select savings fund",
            "limit_description" to "to help calculate spending limit",
            "select_or_create_fund" to "Select / Create fund",
            "no_chart_data" to "No chart data",
            "spending_by_category" to "Spending by category", // Mới
            "daily_spending" to "Daily spending", // Mới
            "savings_fund" to "Savings fund", // Mới
            "needs" to "Necessary", // Mới
            "training" to "Training", // Mới
            "entertainment" to "Entertainment", // Mới
            "savings" to "Savings", // Mới
            "self_care" to "Self-care", // Mới
            "free_spending" to "Free spending", // Mới
            "limit" to "Limit", // Mới
            "spent" to "Spent", // Mới
            "see_more" to "See more", // Mới
            "monthly_spending_total" to "Monthly spending total", // Mới
            "transactions" to "Transactions", // Mới cho bottom nav
            "profile" to "Profile", // Mới cho bottom nav
            // =============================================

            // Days of week (short)
            "monday_short" to "Mon",
            "tuesday_short" to "Tue",
            "wednesday_short" to "Wed",
            "thursday_short" to "Thu",
            "friday_short" to "Fri",
            "saturday_short" to "Sat",
            "sunday_short" to "Sun",

            // Days of week (full)
            "monday" to "Monday",
            "tuesday" to "Tuesday",
            "wednesday" to "Wednesday",
            "thursday" to "Thursday",
            "friday" to "Friday",
            "saturday" to "Saturday",
            "sunday" to "Sunday",

            // Months
            "january" to "January",
            "february" to "February",
            "march" to "March",
            "april" to "April",
            "may" to "May",
            "june" to "June",
            "july" to "July",
            "august" to "August",
            "september" to "September",
            "october" to "October",
            "november" to "November",
            "december" to "December",

            // Help & Support
            "help_support" to "Help & Support",
            "faq" to "Frequently Asked Questions",
            "faq_add_transaction" to "How to add a transaction?",
            "faq_add_transaction_answer" to "Go to Home → tap the blue \"+\" button at the bottom right",
            "faq_view_statistics" to "How to view statistics?",
            "faq_view_statistics_answer" to "Go to Statistics → select time range and data type to view",
            "faq_logout" to "How to logout?",
            "faq_logout_answer" to "Go to Settings → scroll down → select \"Sign Out\"",
            "faq_change_theme" to "How to change theme?",
            "faq_change_theme_answer" to "Go to Settings → toggle \"Theme Mode\"",
            "contact_support" to "Contact Support",
            "contact_description" to "If you need additional support or encounter technical issues, please contact:",
            "email" to "Email",
            "website" to "Website",
            "working_hours" to "Working Hours",
            "response_time" to "Response within 24h",
            "detailed_guide" to "Detailed guides",
            "weekdays" to "Monday - Friday",
            "working_time" to "8:00 - 17:00",
            "usage_tips" to "Usage Tips",
            "tip_categories" to "Use categories to clearly classify expenses",
            "tip_savings" to "Set savings goals to track progress",
            "tip_statistics" to "View monthly statistics to adjust spending",
            "tip_reminders" to "Use reminder feature for recurring bills",

            // Notification settings
            "notification_settings" to "Notification Settings",
            "customize_notifications" to "Customize how you receive notifications",
            "notification_types" to "Notification Types",
            "push_notifications" to "Push Notifications",
            "email_notifications" to "Email Notifications",
            "sms_notifications" to "SMS Notifications",
            "financial_alerts" to "Financial Alerts",
            "low_balance_alert" to "Low Balance Alert",
            "large_transaction_alert" to "Large Transaction Alert",
            "monthly_report" to "Monthly Report",
            "restore" to "Restore",
            "save_settings" to "Save Settings",
            "settings_restored" to "Settings restored",
            "settings_saved" to "Notification settings saved",

            // Main navigation
            "home" to "Home",
            "statistics" to "Statistics",
            "transactions" to "Transactions",
            "categories" to "Categories",
            "reports" to "Reports",
            "settings" to "Settings",
            "account" to "Account",

            // HomeScreen (phần cũ - giữ lại cho tương thích)
            "greeting" to "Hello 👋",
            "total_balance" to "Total balance",
            "need_attention" to "Need attention",
            "stable" to "Stable",
            "status" to "Status",
            "financial_trend" to "Financial trend",
            "month" to "Month",
            "spending" to "Spending",
            "income" to "Income",
            "total" to "Total",
            "line_chart" to "Line chart",
            "column_chart" to "Column chart",
            "pie_chart" to "Pie chart",
            "no_data" to "No data to display",
            "no_spending_data" to "No spending data to display",
            "no_transactions" to "No transactions",

            // TransactionScreen
            "transaction_book" to "Transaction Book",
            "transactions_count" to "transactions",
            "financial_overview" to "Financial Overview",
            "monthly_income" to "Monthly Income",
            "monthly_expense" to "Monthly Expense",
            "transaction_count" to "Transaction Count",
            "transaction_history" to "Transaction History",
            "no_note" to "No note",
            "delete_transaction" to "Delete Transaction",
            "delete_confirmation" to "Are you sure you want to delete the transaction",
            "confirm_delete" to "Confirm Delete",
            "no_transactions_description" to "Start by adding your first transaction",

            // AddTransactionScreen
            "record_transaction" to "Record Transaction",
            "manual_input" to "Manual Input",
            "image_input" to "Image Input",
            "select_category" to "Select Category",
            "transaction_date" to "Transaction Date",
            "today" to "Today",
            "repeat_frequency" to "Repeat Frequency",
            "no_repeat" to "No Repeat",
            "daily" to "Daily",
            "weekly" to "Weekly",
            "monthly" to "Monthly",
            "note" to "Note",
            "enter_transaction_description" to "Enter transaction description",
            "update_transaction" to "Update Transaction",
            "add_income_transaction" to "Add Income Transaction",
            "add_expense_transaction" to "Add Expense Transaction",
            "delete_transaction_dialog" to "Delete Transaction",
            "delete_transaction_description" to "Are you sure you want to delete this transaction?",
            "delete_action" to "Delete",

            // Categories
            "food_drink" to "Food & Drink",
            "shopping" to "Shopping",
            "family" to "Family",
            "other" to "Other",

            // Account info
            "account_info" to "Account Information",
            "update_personal_info" to "Update Personal Information",
            "full_name" to "Full Name",
            "email" to "Email",
            "phone_number" to "Phone Number",
            "read_only" to "Read Only",
            "update_success" to "Update Successful",
            "saving" to "Saving...",
            "save_changes" to "Save Changes",
            "system_info" to "System Information",
            "user_id" to "User ID",
            "not_available" to "Not Available",
            "provider" to "Provider",
            "email_verification" to "Email Verification",
            "verified" to "Verified",
            "not_verified" to "Not Verified",
            "created_at" to "Created At",
            "last_login" to "Last Login",
            "unknown" to "Unknown",
            "email_password" to "Email/Password",
            "google" to "Google",
            "facebook" to "Facebook",
            "phone" to "Phone",

            // Common transaction fields
            "amount" to "Amount",
            "description" to "Description",
            "date" to "Date",
            "category" to "Category",
            "add_expense" to "Add Expense",
            "expense_list" to "Expense List",
            "add_transaction" to "Add transaction",
            "total_income" to "Total income",
            "total_expense" to "Total expense",
            "this_week" to "This week",
            "this_year" to "This year",
            "trend_analysis" to "Trend analysis",
            "category_analysis" to "Category analysis",

            // Settings and account
            "manage_personal_info" to "Manage personal information",
            "notifications" to "Notifications",
            "enable_disable_notifications" to "Enable / disable notifications",
            "theme_mode" to "Theme mode",
            "about_app" to "About app",
            "sign_out" to "Sign out",
            "logout_account" to "Log out of your account",

            // StatisticsScreen
            "financial_fluctuations" to "Financial Fluctuations",
            "time_range_weekly" to "Weekly",
            "time_range_monthly" to "Monthly",
            "time_range_yearly" to "Yearly",
            "data_type_income" to "Income",
            "data_type_expense" to "Expense",
            "data_type_difference" to "Difference",
            "fluctuations" to "Fluctuations",
            "category_analysis" to "Category Analysis",
            "compared_to_same_period" to "Compared to same period",
            "same_period_as" to "Same period as",
            "sub_category" to "Sub Category",
            "parent_category" to "Parent Category",
            "no_transactions_time_period" to "You have no transactions in this time period",
            "millions" to "(Millions)",
            "thousands" to "(Thousands)",
            "last_week" to "last week",
            "last_month" to "last month",
            "last_year" to "last year",
            "extensions" to "Extensions",
            "manage_tools" to "Manage Tools",
            "expense_categories" to "Expense Categories",
            "customize_spending_categories" to "Customize Spending Categories",
            "budgets" to "Budgets",
            "set_and_track_monthly_budget" to "Set and track monthly budget",

            // Category Management
            "create_category" to "Create Category",
            "click_to_change_icon" to "Tap to change icon",
            "category_name" to "Category Name",
            "category_name_example" to "Example: Food, Shopping...",
            "parent_category" to "Parent Category",
            "select_parent_category" to "Select Parent Category",
            "select_category" to "Select Category",
            "icon" to "Icon",
            "ready_to_create_category" to "Ready to create category",
            "select_icon" to "Select Icon",
            "select_group_for_new_category" to "Select group for new category",
            "search_categories" to "Search categories...",
            "found" to "Found",
            "categories" to "categories",
            "no_categories_found" to "No categories found",
            "try_different_keywords" to "Try different keywords",
            "main_category" to "Main Category",
            "sub_category" to "Sub Category",
            "category_management" to "Category Management",
            "add_new_category" to "Add New Category",
            "has" to "Has",
            "add_sub_category" to "Add Sub Category",
            "no_sub_categories" to "No sub categories",
            "sub_categories" to "sub categories"
        )
    )

    // Danh sách ngôn ngữ hỗ trợ
    private val availableLanguages = listOf(
        Language("vi", "Tiếng Việt"),
        Language("en", "English")
    )

    // Get available languages
    fun getAvailableLanguages(): List<Language> {
        return availableLanguages
    }

    // Change language - updated to work with Language object
    fun changeLanguage(languageCode: String) {
        val language = availableLanguages.find { it.code == languageCode }
        if (language != null) {
            _currentLanguage.value = language
            _languageCode.value = languageCode
        }
    }

    // New method to set language directly with Language object
    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        _languageCode.value = language.code
    }

    // Get translation for a key
    fun getTranslation(key: String, fallbackLanguage: String = "en"): String {
        val currentLangCode = _languageCode.value
        return translations[currentLangCode]?.get(key) ?:
        translations[fallbackLanguage]?.get(key) ?: key
    }

    // Get current language name
    fun getCurrentLanguageName(): String {
        return _currentLanguage.value.name
    }

    // Get language from code
    fun getLanguageFromCode(code: String): Language {
        return availableLanguages.find { it.code == code } ?: availableLanguages[0]
    }

    // Set language from code (for initialization)
    fun setLanguageFromCode(code: String) {
        val language = getLanguageFromCode(code)
        _currentLanguage.value = language
        _languageCode.value = code
    }

    // Get current language code
    fun getCurrentLanguageCode(): String {
        return _languageCode.value
    }

    // Initialize with saved language
    fun initializeWithSavedLanguage(savedCode: String) {
        setLanguageFromCode(savedCode)
    }
}