import kotlinx.cli.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.system.exitProcess

val logger = KotlinLogging.logger {}

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("ipctl")

    class Ip : Subcommand("ip", "Get IP address") {
        override fun execute() {
            val listener = Listener()
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
        ).required()

        override fun execute() {
            val listener = Listener(callback)
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
        ).required()
        val interval by option(
            ArgType.Int,
            shortName = "i",
            fullName = "interval",
            description = "Interval to execute callback"
        )

        override fun execute() {
            val listener = Listener(callback, (interval ?: 60000).toLong())
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
        ).required()
        val interval by option(
            ArgType.Int,
            shortName = "i",
            fullName = "interval",
            description = "Interval to execute callback"
        )

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
ExecStart=ipctl listen --callback '$callback' --interval $interval

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
