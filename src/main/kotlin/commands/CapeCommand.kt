package commands

import Colors
import Command
import MojangName
import MojangUtils
import MojangUtils.cachedName
import PermissionTypes.AUTHORIZE_CAPES
import Send.error
import Send.normal
import Send.success
import User
import arg
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import doesLater
import doesLaterIfHas
import greedyString
import helpers.MathHelper.round
import helpers.StringHelper.fixedUUID
import helpers.StringHelper.toHumanReadable
import helpers.StringHelper.toUserID
import literal
import net.ayataka.kordis.entity.message.Message
import string
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList

// TODO: this is pretty server specific. Will be removed in the future and changed to a command
object CapeCommand : Command("cape") {
    init {
        literal("create") {
            greedyString("id") {
                doesLaterIfHas(AUTHORIZE_CAPES) { context ->
                    val id: String = context arg "id"

                    val args = id.split(" ") // we have to do this because it's a greedy string, and <> aren't parsed as a single string
                    val finalID: String

                    if (args.size != 3) {
                        message.error(findError) // TODO TEMPORARY
                        return@doesLaterIfHas
                    }

                    try {
                        finalID = args[0].toUserID().toString()
                    } catch (e: NumberFormatException) {
                        message.error(findError)
                        return@doesLaterIfHas
                    }

                    /** find [CapeType] from user's args */
                    val type = CapeType.values().find { capeType ->
                        capeType.realName.equals(args[2], ignoreCase = true)
                    }

                    if (type == null) {
                        message.error("Couldn't find Cape type \"${args[2].toHumanReadable()}\"!")
                        return@doesLaterIfHas
                    }

                    // make sure the user actually exists, so they can activate their cape
                    val user = (if (finalID.isEmpty()) null else server?.members?.find(finalID.toLong())) ?: run {
                        message.error(findError)
                        return@doesLaterIfHas
                    }

                    val newCape = Cape(null, type = type)
                    val newCapes = arrayListOf(newCape)
                    val existingCapeUser = user.id.getUser(null)
                    val updatedCapeUser = existingCapeUser?.editCapes(newCapes) ?: CapeUser(user.id, newCapes, type == CapeType.DONOR)

                    updateCapeUser(existingCapeUser, updatedCapeUser)

                    message.channel.send {
                        embed {
                            title = "Cape created!"
                            field("User", user.mention)
                            field("Type", type.realName)
                            field("Cape UUID", newCape.capeUUID)
                            color = Colors.success
                        }
                    }
                }
            }
        }

        literal("list") {
            doesLater {
                val userCapes = message.getCapes() ?: return@doesLater

                message.channel.send {
                    embed {
                        userCapes.forEach {
                            field("Cape UUID ${it.capeUUID}", "Player Name: ${cachedName(it.playerUUID) ?: "Not attached"}\nCape Type: ${it.type.realName}")
                        }
                        color = Colors.primary
                    }
                }
            }
        }

        literal("attach") {
            doesLater {
                message.error("You must provide a Cape UUID to attach to a player!")
            }

            string("uuid") {
                doesLater {
                    message.error("You must provide a player name to attach to!")
                }

                greedyString("username") {
                    doesLater { context ->
                        val username: String = context arg "username"
                        val capeUUID: String = context arg "uuid"


                        var capeUser = message.author?.id.getUser(message) ?: return@doesLater
                        val capes = message.getCapes() ?: return@doesLater

                        val cape = capes.find { it.capeUUID == capeUUID } ?: run {
                            message.error("Couldn't find a Cape with a UUID of `$capeUUID`. Make sure you're entering the short UUID as the Cape UUID, not your player UUID")
                            return@doesLater
                        }

                        var user: User? = null
                        var msg: Message? = null

                        username.fixedUUID()?.let {
                            msg = message.normal("Found UUID to attach to Cape `$capeUUID` - verifying")

                            user = MojangUtils.getFromUUID(it) ?: run {
                                msg?.edit {
                                    title = "Error"
                                    description = "Couldn't find an account with the UUID `$capeUUID`!\n" +
                                            "Make sure you have a real Mojang account, contact a moderator if this keeps happening."
                                    color = Colors.error
                                }
                                return@doesLater
                            }

                        } ?: run {
                            msg = message.normal("Found name to attach to Cape `$capeUUID` - looking up UUID!")

                            user = MojangUtils.getFromName(username) ?: run {
                                msg?.edit {
                                    title = "Error"
                                    description = "Couldn't find an account with the name `$username`!\n" +
                                            "Make sure it is spelled correctly and it is a real Mojang account, contact a moderator if this keeps happening."
                                    color = Colors.error
                                }
                                return@doesLater
                            }

                        }

                        user ?: run { return@doesLater }

                        val alreadyAttached = capes.find { it.playerUUID == user!!.uuid }
                        if (alreadyAttached != null) { // TODO: weird bug with this. If someone tries attaching to an already used name and then changing to unused, it will skip timeout
                            msg?.edit {
                                description = "You already have ${user!!.currentMojangName.name} attached to Cape `${alreadyAttached.capeUUID}`!"
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        // this is after everything because we don't care about Mojang requests that much, but we don't want to commit every 5 minutes or whatever
                        changeTimeOut(capeUUID)?.let {
                            msg?.edit {
                                description = "Cape `${capeUUID}` was changed recently, you must wait $it more minutes before you can change it again!"
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        cape.playerUUID = user!!.uuid
                        capeUser = capeUser.editCapes(arrayListOf(cape))
                        updateCapeUser(capeUser, capeUser)

                        msg?.edit {
                            description = "Successfully attached Cape `${cape.capeUUID}` to user `${user!!.currentMojangName.name}`"
                            color = Colors.success
                        }
                    }
                }
            }
        }

        literal("detach") {
            greedyString("uuid") {
                doesLater { context ->
                    val capeUUID: String = context arg "uuid"

                    var capeUser = message.author?.id.getUser(message) ?: return@doesLater
                    val capes = message.getCapes() ?: return@doesLater

                    val cape = capes.find { it.capeUUID == capeUUID } ?: run {
                        message.error("Couldn't find a Cape with a UUID of `$capeUUID`. Make sure you're entering the short UUID as the Cape UUID, not your player UUID")
                        return@doesLater
                    }

                    changeTimeOut(capeUUID)?.let {
                        message.error("Cape `${capeUUID}` was changed recently, you must wait $it more minutes before you can change it again!")
                        return@doesLater
                    }

                    val playerUUID = cape.playerUUID
                    cape.playerUUID = null
                    capeUser = capeUser.editCapes(arrayListOf(cape))
                    updateCapeUser(capeUser, capeUser)

                    message.success("Successfully removed ${cachedName(playerUUID)} from Cape `${cape.capeUUID}`!")
                }
            }
        }

        literal("save") {
            doesLater {
                save()
                message.success("Saved!")
            }
        }

        literal("load") {
            doesLater {
                load()
                message.success("Loaded!")
            }
        }

        load()
    }


    fun load() {
        if (!File("config/capes.json").exists()) return
        try {
            Files.newBufferedReader(Paths.get("config/capes.json")).use {
                val readCapeUsers = GsonBuilder().setPrettyPrinting().create().fromJson<ArrayList<CapeUser>?>(it, object : TypeToken<List<CapeUser>>() {}.type)
                readCapeUsers?.let { read ->
                    capeUsers = read
                } ?: run {
                    println("=".repeat(20))
                    println("Error reading capes!!")
                    println("=".repeat(20))
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        capeUsers?.let { capes ->
            Files.newBufferedWriter(Paths.get("config/capes.json")).use {
                val json = gson.toJson(capeUsers, object : TypeToken<List<CapeUser>>() {}.type)
                println("==========\n$json\n===========")
                it.write(json)
            }
        }
    }

    private fun updateCapeUser(oldUser: CapeUser?, newUser: CapeUser) {
        capeUsers?.removeAll { it.id == oldUser?.id }

        // add if capeUsers isn't null, otherwise make a new list
        capeUsers?.add(newUser) ?: run {
            capeUsers = arrayListOf(newUser)
        }
    }

    private fun CapeUser.editCapes(capes: ArrayList<Cape>): CapeUser {
        if (capes == this.capes) {
            return this
        }

        val user = this
        val oldCapes = this.capes
        user.capes = capes

        oldCapes.forEach { oldCape -> // copy unmodified capes over to new user
            val found = user.capes.find { newCape -> oldCape.capeUUID == newCape.capeUUID }
            if (found != null) return@forEach
            user.capes.add(oldCape)
        }

        if (!user.isPremium && capes.find { it.type == CapeType.DONOR } != null) {
            user.isPremium = true
        }

        return user
    }

    private suspend fun Message.getCapes(): ArrayList<Cape>? {
        capeUsers ?: run {
            this.error("No capes are loaded, contact a developer about this!")
            return null
        }

        return this.author?.id.getCapes(this)
    }

    private suspend fun Long?.getCapes(message: Message?): ArrayList<Cape>? {
        return this.getUser(message)?.capes
    }

    private suspend fun Long?.getUser(message: Message?): CapeUser? {
        return capeUsers?.find { user ->
            user.id == this
        } ?: run {
            message?.error("User <@$this> does not have any capes!")
            null
        }
    }

    private fun changeTimeOut(capeUUID: String): Double? {
        val time = changeTimes[capeUUID] ?: run {
            changeTimes[capeUUID] = System.currentTimeMillis()
            return null
        }

        val difference = System.currentTimeMillis() - time

        if (difference > 900000) { // waited more than 15 minutes, reset the timer
            changeTimes[capeUUID] = System.currentTimeMillis()
            return null
        }

        return round((900000 - difference) / 60000.0, 2) // / convert ms to minutes, with 2 decimal places
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val resetTime get() = System.currentTimeMillis() - 910000
    private val changeTimes = hashMapOf<String, Long>()
    private var capeUsers: ArrayList<CapeUser>? = null
    private const val findError = "Username is improperly formatted, try pinging or using the users ID, and make sure the user exists in this server!"
}

data class CapeUser(
    val id: Long,
    var capes: ArrayList<Cape>,
    @SerializedName("is_premium")
    var isPremium: Boolean = false
)

data class Cape(
    @SerializedName("player_uuid")
    var playerUUID: String?,
    @SerializedName("cape_uuid")
    val capeUUID: String = UUID.randomUUID().toString().substring(0, 5),
    val type: CapeType
)

enum class CapeType(val realName: String, val imageKey: String) {
    BOOSTER("Booster", "booster"),
    CONTEST("Contest", "contest"),
    CONTRIBUTOR("Contributor", "github1"),
    DONOR("Donor", "donator2"),
    INVITER("Inviter", "inviter")
}
