package commands

import Cape
import CapeColor
import CapeUser
import Colors
import Command
import ConfigManager.readConfigSafe
import ConfigType
import PermissionTypes.AUTHORIZE_CAPES
import Send.error
import Send.normal
import Send.success
import User
import UserConfig
import arg
import cachedName
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import doesLater
import doesLaterIfHas
import fixedUUID
import getFromName
import getFromUUID
import greedyString
import helpers.MathHelper.round
import helpers.ShellHelper.bash
import helpers.ShellHelper.systemBash
import helpers.StringHelper.toHumanReadable
import helpers.StringHelper.toUserID
import kotlinx.coroutines.delay
import literal
import net.ayataka.kordis.entity.message.Message
import string
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

// TODO: this is pretty server specific. Will be removed in the future and changed to a plugin
object CapeCommand : Command("cape") {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        literal("create") {
            greedyString("id") {
                doesLaterIfHas(AUTHORIZE_CAPES) { context ->
                    val id: String = context arg "id"

                    val args = id.split(" ") // we have to do this because it's a greedy string, and <> aren't parsed as a single string
                    val finalID: Long

                    if (args.size != 2) {
                        message.error(findError) // TODO TEMPORARY
                        return@doesLaterIfHas
                    }

                    try {
                        finalID = args[0].toUserID()
                    } catch (e: NumberFormatException) {
                        message.error(findError)
                        return@doesLaterIfHas
                    }

                    /** find CapeType from user's args */
                    val type = CapeType.values().find { capeType ->
                        capeType.realName.equals(args[1], ignoreCase = true)
                    }

                    if (type == null) {
                        message.error("Couldn't find Cape type \"${args[1].toHumanReadable()}\"!")
                        return@doesLaterIfHas
                    }

                    // make sure the user actually exists, so they can activate their cape
                    val user = server?.members?.find(finalID) ?: run {
                        message.error(findError)
                        return@doesLaterIfHas
                    }

                    val newCape = Cape(null, type = type)
                    val newCapes = arrayListOf(newCape)
                    val existingCapeUser = user.id.getUser(null)
                    val updatedCapeUser = existingCapeUser?.editCapes(newCapes) ?: CapeUser(user.id, newCapes, type == CapeType.DONOR)

                    updateCapeUser(updatedCapeUser, existingCapeUser?.id)

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

        literal("delete") {
            string("uuid") {
                greedyString("user") {
                    doesLaterIfHas(AUTHORIZE_CAPES) { context ->
                        val capeUUID: String = context arg "uuid"
                        val userID: String = context arg "user"
                        val finalID: Long

                        try {
                            finalID = userID.toUserID()
                        } catch (e: NumberFormatException) {
                            message.error(findError)
                            return@doesLaterIfHas
                        }

                        val user = capeUsers?.find { it.id == finalID } ?: run {
                            message.error("Couldn't find a Cape User with the ID `$finalID`!")
                            return@doesLaterIfHas
                        }

                        val cape = user.capes.find { it.capeUUID.equals(capeUUID, true) } ?: run {
                            message.error(capeError(capeUUID))
                            return@doesLaterIfHas
                        }

                        val editedUser = user.deleteCape(cape.capeUUID)
                        updateCapeUser(editedUser)

                        message.success("Removed Cape `$capeUUID` from Cape User `$finalID`!")
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
            string("uuid") {
                greedyString("username") {
                    doesLater { context ->
                        val username: String = context arg "username"
                        val capeUUID: String = context arg "uuid"


                        var capeUser = message.author?.id.getUser(message) ?: return@doesLater
                        val capes = message.getCapes() ?: return@doesLater

                        val cape = capes.find { it.capeUUID == capeUUID } ?: run {
                            message.error(capeError(capeUUID))
                            return@doesLater
                        }

                        var user: User? = null
                        var msg: Message? = null

                        username.fixedUUID()?.let {
                            msg = message.normal("Found UUID to attach to Cape `$capeUUID` - verifying")

                            user = getFromUUID(it) ?: run {
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

                            user = getFromName(username) ?: run {
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
                        if (alreadyAttached != null) {
                            msg?.edit {
                                description = "You already have ${user!!.currentMojangName.name} attached to Cape `${alreadyAttached.capeUUID}`!"
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        // this is after everything because we don't care about Mojang requests that much, but we don't want to commit every 5 minutes or whatever
                        changeTimeOut(capeUUID)?.let {
                            msg?.edit {
                                description = changeError(capeUUID, it)
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        cape.playerUUID = user!!.uuid
                        capeUser = capeUser.editCapes(arrayListOf(cape))
                        updateCapeUser(capeUser)

                        msg?.edit {
                            description = "Successfully attached Cape `${cape.capeUUID}` to user `${user!!.currentMojangName.name}`"
                            color = Colors.success
                        }
                    }
                }
            }
        }

        literal("detach") {
            string("uuid") {
                doesLater { context ->
                    val capeUUID: String = context arg "uuid"

                    var capeUser = message.author?.id.getUser(message) ?: return@doesLater
                    val capes = message.getCapes() ?: return@doesLater

                    val cape = capes.find { it.capeUUID == capeUUID } ?: run {
                        message.error(capeError(capeUUID))
                        return@doesLater
                    }

                    changeTimeOut(capeUUID)?.let {
                        message.error(changeError(capeUUID, it))
                        return@doesLater
                    }

                    val playerUUID = cape.playerUUID
                    cape.playerUUID = null
                    capeUser = capeUser.editCapes(arrayListOf(cape))
                    updateCapeUser(capeUser)

                    message.success("Successfully removed ${cachedName(playerUUID)} from Cape `${cape.capeUUID}`!")
                }
            }
        }

        literal("color") {
            string("uuid") {
                string("colorPrimary") {
                    string("colorBorder") {
                        doesLater { context ->
                            val capeUUID: String = context arg "uuid"
                            val colorPrimary: String = context arg "colorPrimary"
                            val colorBorder: String = context arg "colorBorder"

                            var capeUser = message.author?.id.getUser(message) ?: return@doesLater
                            val capes = message.getCapes() ?: return@doesLater

                            val cape = capes.find { it.capeUUID == capeUUID } ?: run {
                                message.error(capeError(capeUUID))
                                return@doesLater
                            }

                            changeTimeOut(capeUUID)?.let {
                                message.error(changeError(capeUUID, it))
                                return@doesLater
                            }

                            if (cape.type != CapeType.CONTEST) {
                                message.error("You're only able to change the colors of Contest Capes, `${capeUUID} is a `${cape.type.realName} Cape!")
                            }

                            if (!hexRegex.matches(colorPrimary) || !hexRegex.matches(colorBorder)) {
                                message.error("You must enter both colors in 6-long hex format, eg `9b90ff` or `8778ff`.")
                                return@doesLater
                            }

                            val oldColor = cape.color
                            cape.color = CapeColor(colorPrimary.toLowerCase(), colorBorder.toLowerCase())
                            capeUser = capeUser.editCapes(arrayListOf(cape))
                            updateCapeUser(capeUser)

                            message.success("Successfully changed Cape `${cape.capeUUID}` colors from $oldColor to ${cape.color}!")
                        }
                    }
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


    private fun load() {
        if (!File(capesFile).exists()) return
        try {
            Files.newBufferedReader(Paths.get(capesFile)).use {
                val readCapeUsers = GsonBuilder().setPrettyPrinting().create().fromJson<ArrayList<CapeUser>?>(it, object : TypeToken<List<CapeUser>>() {}.type)
                readCapeUsers?.let { read ->
                    capeUsers = read
                } ?: run {
                    println("=".repeat(20))
                    println("Error reading capes!!")
                    println("=".repeat(20))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        capeUsers?.let { capes ->
            Files.newBufferedWriter(Paths.get(capesFile)).use {
                it.write(gson.toJson(capes, object : TypeToken<List<CapeUser>>() {}.type))
            }
        }
    }

    suspend fun commit() { // TODO: hardcoded and a hack. I'm embarrassed to push this, but this is waiting for me to add plugin support
        readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId?.let {
            if (it != 573954110454366214) return
        } ?: run { return }

        val assets = "/home/mika/projects/cape-api"
        val time = "date".bash()

        "git checkout capes".systemBash(assets)
        delay(50)
        "git reset --hard origin/capes".systemBash(assets)
        delay(200)
        "git pull".systemBash(assets)
        delay(1000) // yea bash commands don't wait to finish so you get an error with the lockfile
        "cp config/capes.json $assets/test.json".systemBash()
        delay(50)
        "git add test.json".systemBash(assets)
        delay(100)
        "git commit -am \"Updated capes $time\"".systemBash(assets)
        delay(1000)
        "git push".systemBash(assets)
    }

    private fun updateCapeUser(newUser: CapeUser, oldUser: Long? = newUser.id) {
        capeUsers?.removeAll { it.id == oldUser }

        // add if capeUsers isn't null, otherwise make a new list
        capeUsers?.add(newUser) ?: run {
            capeUsers = arrayListOf(newUser)
        }
    }

    private fun CapeUser.deleteCape(capeUUID: String): CapeUser {
        val user = this
        user.capes.removeIf { it.capeUUID == capeUUID }
        return user
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
        val time = changedTimeouts[capeUUID] ?: run {
            changedTimeouts[capeUUID] = System.currentTimeMillis()
            return null
        }

        val difference = System.currentTimeMillis() - time

        if (difference > 900000) { // waited more than 15 minutes, reset the timer
            changedTimeouts[capeUUID] = System.currentTimeMillis()
            return null
        }

        return round((900000 - difference) / 60000.0, 2) // / convert ms to minutes, with 2 decimal places
    }

    private val hexRegex = Regex("^[A-Fa-f0-9]{6}\$")
    private val changedTimeouts = hashMapOf<String, Long>()
    private var capeUsers: ArrayList<CapeUser>? = null

    private fun capeError(capeUUID: String) = "Couldn't find a Cape with a UUID of `$capeUUID`. Make sure you're entering the short UUID as the Cape UUID, not your player UUID"
    private fun changeError(capeUUID: String, time: Double) = "Cape `$capeUUID` was changed recently, you must wait $time more minutes before you can change it again!"

    private const val capesFile = "config/capes.json"
    private const val findError = "Username is improperly formatted, try pinging or using the users ID, and make sure the user exists in this server!"
}
