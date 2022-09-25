import io.github.cdimascio.dotenv.dotenv
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("ipctl")

    fun createListener(callback: String? = null, interval: Int? = null): Listener {
        var usedCallback: String? = null
        var usedInterval: Int? = null

        val configFile = File("${System.getProperty("user.home")}/.ipctlrc")
        if (configFile.exists()) {
            val config = dotenv {
                directory = System.getProperty("user.home")
                filename = ".ipctlrc"
                ignoreIfMalformed = true
                ignoreIfMissing = true
            }
            if (config["callback"].isNotEmpty())
                usedCallback = config["callback"]
            if (config["interval"].isNotEmpty() && config["interval"].toIntOrNull() != null)
                usedInterval = config["interval"].toInt()
        }

        if (!callback.isNullOrEmpty())
            usedCallback = callback
        if (interval != null)
            usedInterval = interval

        if (usedCallback == null)
            usedCallback = "echo Hello World!"
        if (usedInterval == null)
            usedInterval = 60_000

        return Listener(usedCallback, usedInterval.toLong())
    }

    class Ip : Subcommand("ip", "Get IP address") {
        override fun execute() {
            val listener = createListener()
            GlobalScope.launch {
                println(listener.fetchCurrentIp())
                exitProcess(0)
            }
        }
    }

    class Update : Subcommand("update", "Update IP") {
        val callback by option(
            ArgType.String,
            shortName = "c",
            fullName = "callback",
            description = "Callback to execute"
        )

        override fun execute() {
            val listener = createListener(callback)
            GlobalScope.launch {
                listener.updateIp(listener.fetchCurrentIp())
                exitProcess(0)
            }
        }
    }

    class Listen : Subcommand("listen", "Listen for IP change") {
        val callback by option(
            ArgType.String,
            shortName = "c",
            fullName = "callback",
            description = "Callback to execute"
        )
        val interval by option(
            ArgType.Int,
            shortName = "i",
            fullName = "interval",
            description = "Interval to execute callback"
        )

        override fun execute() {
            val listener = createListener(callback, interval)
            GlobalScope.launch {
                listener.listen()
                exitProcess(0)
            }
        }
    }

    class Service : Subcommand("service", "Setup a service") {
        val callback by option(
            ArgType.String,
            shortName = "c",
            fullName = "callback",
            description = "Callback to execute"
        )
        val interval by option(
            ArgType.Int,
            shortName = "i",
            fullName = "interval",
            description = "Interval to execute callback"
        )
        val intervalText = interval?.let { "--interval $interval" } ?: ""

        override fun execute() {
            println(
                """echo "[Unit]
Description=Listen to IP change
After=network.target
StartLimitIntervalSec=0

[Service]
Type=simple
Restart=always
RestartSec=1
User=`whoami`
ExecStart=ipctl listen --callback '$callback' $intervalText

[Install]
WantedBy=multi-currentUser.target" > /lib/systemd/system/ipctl.service""".trimMargin()
            )
        }
    }

    val ip = Ip()
    val update = Update()
    val listen = Listen()
    val service = Service()
    parser.subcommands(ip, update, listen, service)

    parser.parse(args)
}
