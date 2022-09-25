import java.time.LocalDateTime

class Logger {
    companion object {
        fun info(message: String) {
            val time = LocalDateTime.now()
            println("[${time.hour}:${time.minute}:${time.second}.${time.nano}] $message")
        }
    }
}