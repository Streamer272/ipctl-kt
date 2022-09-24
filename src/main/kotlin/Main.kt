import kotlinx.cli.*
import kotlinx.coroutines.*

// TODO: add logging

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("ipctl")
    val callback by parser.option(
        ArgType.String,
        shortName = "c",
        fullName = "callback",
        description = "Callback to execute"
    ).required()
    val interval by parser.option(
        ArgType.Int,
        shortName = "i",
        fullName = "interval",
        description = "Interval to execute callback"
    )

    class Ip : Subcommand("ip", "Get IP address") {
        override fun execute() {
            val listener = Listener(callback, (interval ?: 60000).toLong())
            GlobalScope.launch {
                println(listener.fetchCurrentIp())
            }
        }
    }

    class Update : Subcommand("update", "Update IP") {
        override fun execute() {
            val listener = Listener(callback, (interval ?: 60000).toLong())
            GlobalScope.launch {
                listener.updateIp(listener.fetchCurrentIp())
            }
        }
    }

    class Listen : Subcommand("listen", "Listen for IP change") {
        override fun execute() {
            val listener = Listener(callback, (interval ?: 60000).toLong())
            GlobalScope.launch {
                listener.listen()
            }
        }
    }

    class Service : Subcommand("service", "Setup a service") {
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
ExecStart=ipctl --callback '$callback' listen

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

    println("before")
    parser.parse(args)
    println("after")
}