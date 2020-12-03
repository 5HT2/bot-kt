@file:Suppress("UNUSED")

package org.kamiblue.botkt.command

import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.Permissions.missingPermissions
import org.kamiblue.botkt.command.arguments.DiscordChannelArgumentType
import org.kamiblue.botkt.command.arguments.DiscordEmojiArgumentType
import org.kamiblue.botkt.command.arguments.DiscordUserArgumentType
import org.kamiblue.botkt.command.arguments.UserPromise
import org.kamiblue.botkt.utils.AnimatableEmoji

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class BrigadierDsl

/**
 * Appends a new [literal](LiteralArgumentBuilder) to `this` [ArgumentBuilder].
 *
 * @param name the name of the literal argument
 * @param block the receiver function for further construction of the literal argument
 */
fun <T> ArgumentBuilder<T, *>.literal(name: String, block: (@BrigadierDsl LiteralArgumentBuilder<T>).() -> Unit): ArgumentBuilder<*, *> =
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
    block: (@BrigadierDsl RequiredArgumentBuilder<S, R>).() -> Unit,
) =
    then(RequiredArgumentBuilder.argument<S, R>(name, argument).also(block))

/**
 * A shorthand for appending a boolean required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.bool(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Boolean>).() -> Unit,
) =
    argument(name, BoolArgumentType.bool(), block)

/**
 * A shorthand for appending a double required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.double(
    name: String,
    block: RequiredArgumentBuilder<S, Double>.() -> Unit,
): T =
    argument(name, DoubleArgumentType.doubleArg(), block)

/**
 * A shorthand for appending a float required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.float(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Float>).() -> Unit,
): T =
    argument(name, FloatArgumentType.floatArg(), block)

/**
 * A shorthand for appending a integer required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.integer(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Int>).() -> Unit,
): T =
    argument(name, IntegerArgumentType.integer(), block)

/**
 * A shorthand for appending a `long` required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.long(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Long>).() -> Unit,
): T =
    argument(name, LongArgumentType.longArg(), block)

/**
 * A shorthand for appending a string required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.string(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, String>).() -> Unit,
): T =
    argument(name, StringArgumentType.string(), block)

/**
 * A shorthand for appending a greedy string required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.greedyString(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, String>).() -> Unit,
): T =
    argument(name, StringArgumentType.greedyString(), block)

/**
 * A shorthand for appending a discord ping required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.ping(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, UserPromise>).() -> Unit,
): T =
    argument(name, DiscordUserArgumentType, block)

/**
 * A shorthand for appending a discord emoji required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.emoji(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, AnimatableEmoji>).() -> Unit,
): T =
    argument(name, DiscordEmojiArgumentType, block)

/**
 * A shorthand for appending a discord channel required argument to `this` [ArgumentBuilder]
 *
 * @see argument
 */
fun <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.channel(
    name: String,
    block: (@BrigadierDsl RequiredArgumentBuilder<S, Long>).() -> Unit,
): T =
    argument(name, DiscordChannelArgumentType, block)

/**
 * Sets the executes callback for `this` [ArgumentBuilder]
 *
 * @param command the callback
 */
infix fun <S> ArgumentBuilder<S, *>.does(command: (@BrigadierDsl CommandContext<S>) -> Int): ArgumentBuilder<*, *> = executes(command)

/**
 * Shorthand for [doesLater] with an empty [does] handler that always returns `0`.
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
fun ArgumentBuilder<Cmd, *>.doesLaterIfHas(permission: PermissionTypes, later: suspend MessageReceiveEvent.(CommandContext<Cmd>) -> Unit): ArgumentBuilder<*, *> =
    does { context ->
        context.source later {
            if (this.message.author!!.id.hasPermission(permission))
                later(this, context)
            else
                this.message.missingPermissions(permission)
        }
        0
    }

/**
 * Gets the value of a (required) argument in the command hierarchy
 *
 * @see CommandContext.getArgument
 */
inline infix fun <reified R, S> CommandContext<S>.arg(name: String): R = getArgument(name, R::class.java)

suspend inline infix fun <S> CommandContext<S>.userArg(name: String) = (this.arg<UserPromise, S>(name))()

fun <S> CommandContext<S>.channelArg(name: String, server: Server?) = server?.channels?.find(this.arg(name))
