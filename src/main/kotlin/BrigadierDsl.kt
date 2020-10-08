import Permissions.hasPermission
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.ayataka.kordis.event.events.message.MessageReceiveEvent

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class BrigadierDsl

/**
 * Appends a new [literal](LiteralArgumentBuilder) to `this` [ArgumentBuilder].
 *
 * @param name the name of the literal argument
 * @param block the receiver function for further construction of the literal argument
 */
fun <T> ArgumentBuilder<T, *>.literal(name: String, block: (@BrigadierDsl LiteralArgumentBuilder<T>).() -> Unit) =
    then(LiteralArgumentBuilder.literal<T>(name).also(block))

/**
 * Appends a new required argument to `this` [ArgumentBuilder].
 *
 * @param name the name of the required argument
 * @param argument the type of required argument, for example [IntegerArgumentType]
 * @param block the receiver function for further construction of the required argument
 */
fun <S, T : ArgumentBuilder<S, T>, R> ArgumentBuilder<S, T>.argument(
    name: String,
    argument: ArgumentType<R>,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, R>).() -> Unit
) =
    then(RequiredArgumentBuilder.argument<S, R>(name, argument).also(block))

/**
 * A shorthand for appending a boolean required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.bool(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Boolean>).() -> Unit
) =
    argument(name, BoolArgumentType.bool(), block)

/**
 * A shorthand for appending a double required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.double(
    name: String,
    block: RequiredArgumentBuilder<S, Double>.() -> Unit
) =
    argument(name, DoubleArgumentType.doubleArg(), block)

/**
 * A shorthand for appending a float required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.float(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Float>).() -> Unit
) =
    argument(name, FloatArgumentType.floatArg(), block)

/**
 * A shorthand for appending a integer required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.integer(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Int>).() -> Unit
) =
    argument(name, IntegerArgumentType.integer(), block)

/**
 * A shorthand for appending a `long` required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.long(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Long>).() -> Unit
) =
    argument(name, LongArgumentType.longArg(), block)

/**
 * A shorthand for appending a string required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.string(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, String>).() -> Unit
) =
    argument(name, StringArgumentType.string(), block)

/**
 * A shorthand for appending a greedy string required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.greedyString(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, String>).() -> Unit
) =
    argument(name, StringArgumentType.greedyString(), block)

/**
 * Sets the executes callback for `this` [ArgumentBuilder]
 *
 * @param command the callback
 */
infix fun <S> ArgumentBuilder<S, *>.does(command: (@BrigadierDsl CommandContext<S>) -> Int) = executes(command)

/**
 * Shorthand for [doesLater] with an empty [now] handler that always returns `0`.
 */
infix fun ArgumentBuilder<Cmd, *>.doesLater(later: suspend MessageReceiveEvent.(CommandContext<Cmd>) -> Unit) =
    does { context ->
        context.source later {
            later(this, context)
        }
        0
    }

/**
 * The same as [doesLater], but with a permission check.
 */
fun ArgumentBuilder<Cmd, *>.doesLaterIfHas(permission: PermissionTypes, later: suspend MessageReceiveEvent.(CommandContext<Cmd>) -> Unit) =
        does { context ->
            context.source later {
                fun String.toHumanReadable() = this.toLowerCase().replace('_', ' ').capitalize()

                if (this.message.hasPermission(permission))
                    later(this, context)
                else
                    this.message.channel.send {
                        embed {
                            title = "Missing permission"
                            description = "Sorry, but you're missing the '${permission.name.toHumanReadable()}' permission, which is required to run this command."
                            color = Colors.error
                        }
                    }
            }
            0
        }

/**
 * Gets the value of a (required) argument in the command hierarchy
 *
 * @see CommandContext.getArgument
 */
inline infix fun <reified R, S> CommandContext<S>.arg(name: String) = getArgument(name, R::class.java)
