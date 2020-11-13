package commands

import Cape
import CapeColor
import CapeType
import CapeUser
import Colors
import Command
import ConfigManager.readConfigSafe
import ConfigType
import Main
import PermissionTypes.AUTHORIZE_CAPES
import Send.error
import Send.normal
import Send.success
import User
import UserConfig
import arg
import cachedName
import com.google.gson.Gson
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
import helpers.StringHelper.prepend
import helpers.StringHelper.toHumanReadable
import helpers.StringHelper.toUserID
import kotlinx.coroutines.delay
import literal
import maxEmojiSlots
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.emoji.Emoji
import string
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

// TODO: this is pretty server specific. Will be removed in the future and changed to a plugin
object CapeCommand : Command("cape") {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val hexRegex = Regex("^[A-Fa-f0-9]{6}\$")
    private val changedTimeouts = hashMapOf<String, Long>()
    private val capeUserMap = HashMap<Long, CapeUser>()
    private val cachedEmojis = LinkedHashMap<String, Emoji?>()
    private var cachedServer: Server? = null

    private const val capesFile = "config/capes.json"
    private const val findError = "Username is improperly formatted, try pinging or using the users ID, and make sure the user exists in this server!"

    init {
        literal("create") {
            greedyString("id") {
                doesLaterIfHas(AUTHORIZE_CAPES) { context ->
                    val id: String = context arg "id"

                    val args = id.split(" ") // we have to do this because it's a greedy string, and <> aren't parsed as a single string
                    var finalID: Long = 0

                    if (args.size != 2) {
                        message.error(findError) // TODO TEMPORARY
                        return@doesLaterIfHas
                    }

                    args[0].toUserID()?.let {
                        finalID = it
                    } ?: run {
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
                    val existingCapeUser = user.id.getUser(null)
                    val updatedCapeUser = existingCapeUser?.editCapes(newCape) ?: CapeUser(user.id, arrayListOf(newCape), type == CapeType.DONOR)
                    capeUserMap[updatedCapeUser.id] = updatedCapeUser

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
                        var finalID: Long = 0

                        userID.toUserID()?.let {
                            finalID = it
                        } ?: run {
                            message.error(findError)
                            return@doesLaterIfHas
                        }

                        val user = capeUserMap[finalID] ?: run {
                            message.error("Couldn't find a Cape User with the ID `$finalID`!")
                            return@doesLaterIfHas
                        }

                        val cape = user.capes.find { it.capeUUID.equals(capeUUID, true) } ?: run {
                            message.error(capeError(capeUUID))
                            return@doesLaterIfHas
                        }

                        user.deleteCape(cape.capeUUID)

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

                        user ?: return@doesLater

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

                            val capes = message.getCapes() ?: return@doesLater

                            val cape = capes.find { it.capeUUID == capeUUID } ?: run {
                                message.error(capeError(capeUUID))
                                return@doesLater
                            }

                            if (cape.type != CapeType.CONTEST) {
                                message.error("You're only able to change the colors of Contest Capes, `${capeUUID} is a `${cape.type.realName} Cape!")
                                return@doesLater
                            }

                            if (!hexRegex.matches(colorPrimary) || !hexRegex.matches(colorBorder)) {
                                message.error("You must enter both colors in 6-long hex format, eg `9b90ff` or `8778ff`.")
                                return@doesLater
                            }

                            changeTimeOut(capeUUID)?.let {
                                message.error(changeError(capeUUID, it))
                                return@doesLater
                            }

                            val oldCapeColor = cape.color
                            cape.color = CapeColor(colorPrimary.toLowerCase(), colorBorder.toLowerCase())

                            // TODO() properly generate
                            //val dir = "/home/mika/projects/assets-kamiblue/assets/capes/templates"
                            //"./recolor.sh ${cape.color.primary} ${cape.color.border} ${cape.capeUUID}".systemBash(dir)

                            val oldColors = oldCapeColor.toEmoji()
                            val newColors = cape.color.toEmoji()

                            message.channel.send {
                                embed {
                                    description = "Successfully changed the colors of Cape `${cape.capeUUID}`!"
                                    field(
                                        "Old Colors",
                                        oldColors,
                                        true
                                    )
                                    field(
                                        "New Colors",
                                        newColors,
                                        true
                                    )
                                    color = Colors.success
                                }
                            }
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
            Files.newBufferedReader(Paths.get(capesFile)).use { bufferedReader ->
                val readCapeUsers = Gson().fromJson<ArrayList<CapeUser>?>(bufferedReader, object : TypeToken<List<CapeUser>>() {}.type)
                readCapeUsers?.let {
                    capeUserMap.clear()
                    capeUserMap.putAll(readCapeUsers.associateBy { it.id })
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
        val capeUsers = capeUserMap.values.toList()
        Files.newBufferedWriter(Paths.get(capesFile)).use {
            it.write(gson.toJson(capeUsers, object : TypeToken<List<CapeUser>>() {}.type))
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

    private fun CapeUser.deleteCape(capeUUID: String) {
        this.capes.removeIf { it.capeUUID == capeUUID }
    }

    private fun CapeUser.editCapes(cape: Cape): CapeUser {
        return apply {
            this.capes.removeIf { it.capeUUID == cape.capeUUID }
            this.capes.add(cape)
            this.isPremium = this.isPremium || capes.any { it.type == CapeType.DONOR }
        }
    }

    private suspend fun Message.getCapes(): ArrayList<Cape>? {
        return author?.id.getUser(this)?.capes
    }

    private suspend fun Long?.getUser(message: Message?): CapeUser? {
        return capeUserMap[this] ?: run {
            message?.error("User <@$this> does not have any capes!")
            null
        }
    }

    private suspend fun CapeColor.toEmoji(): String {
        val primary = makeEmojiFromHex(primary)
        val border = makeEmojiFromHex(border)

        var primaryEmoji = "Primary (#${this.primary})"
        var borderEmoji = "Border (#${this.border})"

        primaryEmoji = primary?.let {
            primaryEmoji.prepend("<:${it.name}:${it.id}> ") } ?: run {
            primaryEmoji.prepend("<:cssource:775893099527929926> ")
        }

        borderEmoji = border?.let {
            borderEmoji.prepend("<:${it.name}:${it.id}> ") } ?: run {
            borderEmoji.prepend("<:cssource:775893099527929926> ")
        }

        return "$primaryEmoji\n$borderEmoji"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeEmojiFromHex(hex: String): Emoji? {
        trimAndSyncEmojis()

        return try {
            cachedEmojis.getOrPut(hex) {
                val b = ByteArrayOutputStream()
                val bufferedImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)

                for (x in 0 until bufferedImage.width) for (y in 0 until bufferedImage.height) {
                    bufferedImage.setRGB(x, y, Integer.decode("0x$hex"))
                }

                ImageIO.write(bufferedImage, "jpg", b)

                server?.createEmoji {
                    name = hex
                    image = b.toByteArray()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun trimAndSyncEmojis() {
        val server = server ?: return
        syncEmojis()
        val maxEmojiSlots = server.maxEmojiSlots()

        while (cachedEmojis.size + 5 >= maxEmojiSlots) {
            val key = cachedEmojis.keys.last()
            cachedEmojis.remove(key)?.delete()
        }
    }

    private fun syncEmojis() {
        server?.emojis?.let { serverEmojis ->
            serverEmojis.forEach {
                cachedEmojis[it.name] = it
            }
            cachedEmojis.values.removeIf { !serverEmojis.contains(it) }
        }
    }

    private fun changeTimeOut(capeUUID: String): Double? {
        val time = changedTimeouts.getOrPut(capeUUID) {
            System.currentTimeMillis()
        }

        val difference = System.currentTimeMillis() - time

        if (difference > 900000) { // waited more than 15 minutes, reset the timer
            changedTimeouts[capeUUID] = System.currentTimeMillis()
            return null
        }

        return round((900000 - difference) / 60000.0, 2) // / convert ms to minutes, with 2 decimal places
    }

    private val server get() = run {
        cachedServer ?: run {
            val server = Main.client?.servers?.find(775449131736760361)
            cachedServer = server
            server
        }
    }

    private fun capeError(capeUUID: String) = "Couldn't find a Cape with a UUID of `$capeUUID`. Make sure you're entering the short UUID as the Cape UUID, not your player UUID"
    private fun changeError(capeUUID: String, time: Double) = "Cape `$capeUUID` was changed recently, you must wait $time more minutes before you can change it again!"
}
