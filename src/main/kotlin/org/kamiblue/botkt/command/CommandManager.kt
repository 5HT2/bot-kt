package org.kamiblue.botkt.command

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigManager
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.command.commands.system.ExceptionCommand
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils
import org.kamiblue.botkt.utils.StringUtils.flat
import org.kamiblue.command.AbstractCommandManager
import org.kamiblue.command.utils.CommandNotFoundException
import org.kamiblue.command.utils.SubCommandNotFoundException
import org.kamiblue.commons.utils.ClassUtils
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.jar.JarFile


object CommandManager : AbstractCommandManager<MessageExecuteEvent>() {

    private val executeQueue = ConcurrentLinkedQueue<suspend () -> Unit>()

    fun init() {
        val commandClasses = ClassUtils.findClasses("org.kamiblue.botkt.command.commands", BotCommand::class.java)

        val jarFile = File("plugin.jar")
        importPlugin(jarFile)
        val classes: List<Class<*>> = findJarClasses(jarFile)

        MessageSendUtils.log("Registering commands...")

        for (clazz in commandClasses) {
            val botCommand = ClassUtils.getInstance(clazz)
            botCommand.category.commands.add(register(botCommand))
        }

        MessageSendUtils.log("Registered ${getCommands().size} commands!")
    }

    suspend fun submit(event: MessageReceiveEvent, string: String) {
        executeQueue.add {
            runCommand(event, string)
        }
    }

    suspend fun runQueued() = coroutineScope {
        if (executeQueue.isNotEmpty()) {
            executeQueue.map {
                async {
                    it()
                }
            }.awaitAll()
            executeQueue.clear()
        }
    }

    private suspend fun runCommand(event: MessageReceiveEvent, string: String) {
        val args = tryParseArgument(event, string) ?: return

        try {
            try {
                invoke(MessageExecuteEvent(args, event))
            } catch (e: CommandNotFoundException) {
                handleCommandNotFoundException(event, e)
            } catch (e: SubCommandNotFoundException) {
                handleSubCommandNotFoundException(event, string, args, e)
            }
        } catch (e: Exception) {
            handleExceptions(event, args, e)
        }
    }

    private suspend fun tryParseArgument(event: MessageReceiveEvent, string: String) = try {
        parseArguments(string)
    } catch (e: IllegalArgumentException) {
        ExceptionCommand.addException(e)
        event.message.channel.send {
            embed {
                title = "Invalid input: ${Main.prefix}$string"
                description = "${e.message}\n" +
                    "Use the `${Main.prefix}exception` command to view the full stacktrace."
                color = Colors.ERROR.color
            }
        }
        null
    }

    private suspend fun handleCommandNotFoundException(
            event: MessageReceiveEvent,
            e: CommandNotFoundException
    ) {
        if (ConfigManager.readConfigSafe<UserConfig>(
                        ConfigType.USER,
                        false
                )?.unknownCommandError == true
        ) {
            event.message.channel.send {
                embed {
                    title = "Unknown Command"
                    description = "Command not found: `${e.command}`\n" +
                        "Use `${Main.prefix}help` to get a list of available commands."
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun handleSubCommandNotFoundException(
            event: MessageReceiveEvent,
            string: String,
            args: Array<String>,
            e: SubCommandNotFoundException
    ) {
        val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }

        val syntax = e.command.printArgHelp()
            .lines()
            .joinToString("\n") {
                if (it.isNotBlank() && !it.startsWith("- ")) {
                    "`${Main.prefix}$it`"
                } else {
                    it
                }
            }

        event.message.channel.send {
            embed {
                title = "Invalid Syntax: `${Main.prefix}${string}`"
                if (bestCommand != null) {
                    val prediction = "`${Main.prefix}${bestCommand.printArgHelp()}`"
                    field("Did you mean?", prediction)
                }
                field("Available arguments:", syntax.flat(1024))
                color = Colors.ERROR.color
            }
        }
    }

    private suspend fun handleExceptions(
            event: MessageReceiveEvent,
            args: Array<String>,
            e: Exception
    ) {
        ExceptionCommand.addException(e)
        event.message.channel.send {
            embed {
                title = "Command Exception Occurred"
                description = "The command `${args.first()}` threw an exception.\n" +
                    "Use the `${Main.prefix}exception` command to view the full stacktrace."
                field("Exception Message", e.message.toString())
                color = Colors.ERROR.color
            }
        }
    }

    private fun importPlugin(file: File){
        val addURL = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
        addURL.isAccessible = true
        addURL.invoke(URLClassLoader.getSystemClassLoader(), file.toURI().toURL())
    }

    private fun findJarClasses(file: File): List<Class<*>> {
        val classes = ArrayList<Class<*>>()
        val entries = JarFile(file).entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            var name = entry.name

            if (name.endsWith(".class")) {
                name = name.substring(0, name.lastIndexOf('.'))
                classes.add(Class.forName(name))
            }
        }
        return classes
    }
}

