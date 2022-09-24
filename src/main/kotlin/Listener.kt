import com.jaredrummler.ktsh.Shell
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*

class Listener(private val callback: String, private val interval: Long = 60000) {
    private val client = HttpClient(CIO)
    private val shell = Shell("sh")
    var currentIp = ""

    suspend fun fetchCurrentIp(): String {
        val response = client.get("https://api.ipify.org")
        return response.body()
    }

    fun updateIp(newIp: String) {
        val result = shell.run(callback.replace("\$IP\$", newIp))
        if (result.isSuccess)
            currentIp = newIp

        println(result.stdout)
    }

    suspend fun listen() {
        while (true) {
            val newIp = fetchCurrentIp()

            if (newIp != currentIp && newIp.isNotEmpty()) {
                updateIp(newIp)
            }

            Thread.sleep(interval)
        }
    }
}
