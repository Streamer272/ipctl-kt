import com.jaredrummler.ktsh.Shell
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*

class Listener(private val callback: String = "", private val interval: Long = 60000) {
    private val client = HttpClient(CIO)
    private val shell = Shell("sh")
    var currentIp = ""

    suspend fun fetchCurrentIp(): String {
        val response = client.get("http://api.ipify.org")
        return response.body()
    }

    fun updateIp(newIp: String) {
        Logger.info("Updating IP")
        val result = shell.run(callback.replace("#IP", newIp))
        if (result.isSuccess)
            currentIp = newIp

        Logger.info("Changed IP to $newIp")
        println(result.stdout)
    }

    suspend fun listen() {
        while (true) {
            Logger.info("Fetching IP")
            val newIp = fetchCurrentIp()
            Logger.info("Old IP: $currentIp, New IP: $newIp")

            if (newIp != currentIp && newIp.isNotEmpty())
                updateIp(newIp)
            else
                Logger.info("IP not changed")

            Thread.sleep(interval)
        }
    }
}
