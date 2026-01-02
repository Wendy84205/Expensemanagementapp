package com.example.financeapp.data.models

import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.*

data class RecurringExpense(
    @PropertyName("id") val id: String = "",
    @PropertyName("title") val title: String = "",
    @PropertyName("amount") val amount: Double = 0.0,
    @PropertyName("category") val category: String = "",
    @PropertyName("categoryIcon") val categoryIcon: String = "",
    @PropertyName("categoryColor") val categoryColor: String = "",
    @PropertyName("wallet") val wallet: String = "",
    @PropertyName("description") val description: String? = null,

    // Cấu hình định kỳ
    @PropertyName("frequency") val frequency: String = "", // Lưu dạng string
    @PropertyName("startDate") val startDate: String = "",
    @PropertyName("endDate") val endDate: String? = null,
    @PropertyName("nextOccurrence") val nextOccurrence: String = "",

    // Trạng thái
    @PropertyName("isActive") val isActive: Boolean = true,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("userId") val userId: String = "", // 🔥 QUAN TRỌNG: Phân biệt user

    // Thống kê
    @PropertyName("totalGenerated") val totalGenerated: Int = 0,
    @PropertyName("lastGenerated") val lastGenerated: String? = null
) {
    // Helper functions để convert enum
    fun getFrequencyEnum(): RecurringFrequency {
        return try {
            RecurringFrequency.valueOf(frequency)
        } catch (e: Exception) {
            RecurringFrequency.MONTHLY
        }
    }

    // Kiểm tra xem có cần tạo giao dịch mới không (PHIÊN BẢN ĐÃ FIX)
    fun shouldGenerateToday(currentDate: String = getCurrentDate()): Boolean {
        if (!isActive) return false

        // Kiểm tra đã qua endDate chưa
        if (endDate != null && isDateBefore(currentDate, endDate)) {
            return false // Đã quá hạn
        }

        // Kiểm tra đã đến ngày bắt đầu chưa
        if (isDateBefore(currentDate, startDate)) {
            return false // Chưa đến ngày bắt đầu
        }

        // Kiểm tra ngày hiện tại so với nextOccurrence
        return !isDateBefore(currentDate, nextOccurrence)
    }

    // Tính ngày kế tiếp
    fun calculateNextOccurrence(): String {
        return calculateNextDate(nextOccurrence.ifEmpty { startDate }, getFrequencyEnum())
    }

    // Kiểm tra xem đã hết hạn chưa
    fun isExpired(currentDate: String = getCurrentDate()): Boolean {
        return endDate != null && isDateBefore(currentDate, endDate)
    }

    // Clone với các thay đổi
    fun copyWithNextOccurrence(): RecurringExpense {
        return this.copy(
            nextOccurrence = calculateNextOccurrence(),
            totalGenerated = totalGenerated + 1,
            lastGenerated = getCurrentDate()
        )
    }

    // Kiểm tra xem có hợp lệ không
    fun isValid(): Boolean {
        return title.isNotBlank() &&
                amount > 0 &&
                category.isNotBlank() &&
                startDate.isNotBlank() &&
                frequency.isNotBlank() &&
                userId.isNotBlank()
    }

    // Helper functions để so sánh ngày
    private fun isDateBefore(date1: String, date2: String): Boolean {
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)
            d1 != null && d2 != null && d1.before(d2)
        } catch (e: Exception) {
            false
        }
    }

    private fun isDateAfter(date1: String, date2: String): Boolean {
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)
            d1 != null && d2 != null && d1.after(d2)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        // Định nghĩa format duy nhất
        private const val DATE_FORMAT = "yyyy-MM-dd"

        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(Date())
        }

        // Helper để format từ UI date (dd/MM/yyyy) sang internal format
        fun formatDateFromUI(uiDate: String): String {
            return try {
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                val date = inputFormat.parse(uiDate)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                getCurrentDate()
            }
        }

        // Helper để format từ internal format sang UI date
        fun formatDateForUI(internalDate: String): String {
            return try {
                val inputFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(internalDate)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                internalDate
            }
        }

        private fun calculateNextDate(currentDate: String, frequency: RecurringFrequency): String {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val date = sdf.parse(currentDate) ?: return currentDate

            val calendar = Calendar.getInstance()
            calendar.time = date

            when (frequency) {
                RecurringFrequency.DAILY -> calendar.add(Calendar.DATE, 1)
                RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.MONTHLY -> {
                    // Xử lý đặc biệt cho tháng
                    calendar.add(Calendar.MONTH, 1)
                    // Đảm bảo không vượt quá ngày cuối tháng
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                    if (currentDay > maxDay) {
                        calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                    }
                }
                RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
                RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
            }

            return sdf.format(calendar.time)
        }

        fun fromEnum(
            id: String = "",
            title: String = "",
            amount: Double = 0.0,
            category: String = "",
            categoryIcon: String = "",
            categoryColor: String = "",
            wallet: String = "",
            description: String? = null,
            frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
            startDate: String = "",
            endDate: String? = null,
            nextOccurrence: String = "",
            isActive: Boolean = true,
            userId: String = "",
            totalGenerated: Int = 0,
            lastGenerated: String? = null
        ): RecurringExpense {
            return RecurringExpense(
                id = id,
                title = title,
                amount = amount,
                category = category,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                wallet = wallet,
                description = description,
                frequency = frequency.name,
                startDate = startDate,
                endDate = endDate,
                nextOccurrence = nextOccurrence.ifEmpty { calculateNextDate(startDate, frequency) },
                isActive = isActive,
                userId = userId,
                totalGenerated = totalGenerated,
                lastGenerated = lastGenerated
            )
        }
    }
}