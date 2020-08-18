import io.ktor.util.date.GMTDate
import io.ktor.util.date.toGMTDate
import java.sql.Timestamp
import java.util.*

object Converter {
    fun epochToFormattedDate(epoch: Long): String {
        return formatDate(epochToDate(epoch).toInstant().toGMTDate())
    }

    fun epochToDate(epoch: Long): Date {
        val stamp = Timestamp(epoch)
        return Date(stamp.time)
    }

    fun formatDate(gmtDate: GMTDate): String {
        return "${gmtDate.year}/${gmtDate.month}/${gmtDate.dayOfMonth} ${gmtDate.hours}:${gmtDate.minutes}:${gmtDate.seconds}"
    }
}