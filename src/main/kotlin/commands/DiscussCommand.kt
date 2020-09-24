package commands

import Colors
import Command
import PermissionTypes
import Permissions.hasPermission
import arg
import doesLater
import greedyString
import literal
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import string

// TODO: make this not hardcoded
object DiscussCommand : Command("discuss") {
    init {
        literal("addon") {
            greedyString("idea") {
                doesLater { context ->
                    if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                        return@doesLater
                    }

                    val idea: String = context arg "idea"
                    val name = "t-" + server!!.channels.find(message.channel.id)!!.name.substring(2)
                    val discussionTopic = server!!.textChannels.findByName(name)!!

                    discussionTopic.send {
                        if (!message.attachments.isEmpty()) {
                            embed {
                                imageUrl = message.attachments.stream().findFirst().get().url
                                color = Colors.primary
                            }
                        } else { // needs to be else because you cannot embed images in the same message with content
                            embed {
                                author(name = message.author!!.name)
                                field("Added:", idea, false)
                                color = Colors.primary
                            }
                        }
                    }

                }
            }
        }
        string("topic") {
            greedyString("description") {
                doesLater { context ->
                    if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                        return@doesLater
                    }

                    val upperCouncil = server!!.roles.findByName("upper council")!!
                    val lowerCouncil = server!!.roles.findByName("lower council")!!

                    val topic: String = context arg "topic"
                    val description: String = context arg "description"
                    val discussionCategory = server!!.channelCategories.findByName("discussions")

                    val allow = PermissionSet(1024) // allow reading
                    val deny = PermissionSet(2048) // disallow speaking

                    val discussionTopic = server!!.createTextChannel {
                        this.category = discussionCategory
                        this.name = "t-$topic"
                    }

                    discussionTopic.send {
                        embed {
                            author(name = "${message.author!!.name} created $topic")
                            field("Topic", description, false)
                            color = Colors.primary
                        }
                    }

                    discussionTopic.edit {
                        this.rolePermissionOverwrites.add(RolePermissionOverwrite(upperCouncil, allow, deny))
                        this.rolePermissionOverwrites.add(RolePermissionOverwrite(lowerCouncil, allow, deny))
                    }

                    server!!.createTextChannel {
                        this.category = discussionCategory
                        this.name = "d-$topic"
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Discuss any topic asynchronously with the council.\n\n" +
                "Create a new discussion:\n" +
                "`;$name <topic-name> <Some detailed description of the topic>`\n\n" +
                "Add to the current discussion, while inside a `#d-` channel:\n" +
                "`;$name addon <Some point to add on. Put a . here if you're attaching an image>`"
    }
}