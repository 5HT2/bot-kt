package org.kamiblue.botkt.event

import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.*
import net.ayataka.kordis.event.events.server.ServerReadyEvent
import net.ayataka.kordis.event.events.server.ServerShutdownEvent
import net.ayataka.kordis.event.events.server.ServerUpdateEvent
import net.ayataka.kordis.event.events.server.channel.ChannelCreateEvent
import net.ayataka.kordis.event.events.server.channel.ChannelDeleteEvent
import net.ayataka.kordis.event.events.server.channel.ChannelUpdateEvent
import net.ayataka.kordis.event.events.server.emoji.EmojiUpdateEvent
import net.ayataka.kordis.event.events.server.role.RoleCreateEvent
import net.ayataka.kordis.event.events.server.role.RoleDeleteEvent
import net.ayataka.kordis.event.events.server.role.RoleUpdateEvent
import net.ayataka.kordis.event.events.server.user.*

@Suppress("UNUSED")
internal object KordisEventProcessor {

    /* Message */
    @EventHandler
    fun messageDeleteEventListener(event: MessageDeleteEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun messageEditEventListener(event: MessageEditEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun messageReceiveEventListener(event: MessageReceiveEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun reactionAddEventListener(event: ReactionAddEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun reactionRemoveEventListener(event: ReactionRemoveEvent) {
        BotEventBus.post(event)
    }


    /* Server */
    @EventHandler
    fun serverReadyEventListener(event: ServerReadyEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun serverShutdownEventListener(event: ServerShutdownEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun serverUpdateEventListener(event: ServerUpdateEvent) {
        BotEventBus.post(event)
    }


    /* Channel */
    @EventHandler
    fun channelCreateEventListener(event: ChannelCreateEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun channelDeleteEventListener(event: ChannelDeleteEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun channelUpdateEventListener(event: ChannelUpdateEvent) {
        BotEventBus.post(event)
    }


    /* Emoji */
    @EventHandler
    fun emojiUpdateEventListener(event: EmojiUpdateEvent) {
        BotEventBus.post(event)
    }


    /* Role */
    @EventHandler
    fun roleCreateEventListener(event: RoleCreateEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun roleDeleteEventListener(event: RoleDeleteEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun roleUpdateEventListener(event: RoleUpdateEvent) {
        BotEventBus.post(event)
    }


    /* User */
    @EventHandler
    fun userBanEventListener(event: UserBanEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun userJoinEventListener(event: UserJoinEvent) {
        BotEventBus.post(event)
    }


    @EventHandler
    fun userLeaveEventListener(event: UserLeaveEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun userRoleUpdateEventListener(event: UserRoleUpdateEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun userUnbanEventListener(event: UserUnbanEvent) {
        BotEventBus.post(event)
    }

    @EventHandler
    fun userUpdateEventListener(event: UserUpdateEvent) {
        BotEventBus.post(event)
    }

}