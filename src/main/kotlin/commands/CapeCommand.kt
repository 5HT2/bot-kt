package org.kamiblue.botkt.commands

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.emoji.Emoji
import org.kamiblue.botkt.*
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.Send.error
import org.kamiblue.botkt.Send.normal
import org.kamiblue.botkt.Send.success
import org.kamiblue.botkt.helpers.MathHelper
import org.kamiblue.botkt.helpers.ShellHelper.bash
import org.kamiblue.botkt.helpers.ShellHelper.systemBash
import org.kamiblue.botkt.helpers.StringHelper.toHumanReadable
import org.kamiblue.botkt.helpers.StringHelper.toUserID
import org.kamiblue.capeapi.*
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
    private val changedTimeouts = HashMap<String, Long>()
    private val capeUserMap = HashMap<Long, CapeUser>()
    private val cachedEmojis = LinkedHashMap<String, Emoji>()
    private var cachedServer: Server? = null

    private const val capesFile = "config/capes.json"
    private const val findError = "Username is improperly formatted, try pinging or using the users ID, and make sure the user exists in this server!"
    private const val missingTexture = "<:cssource:775893099527929926> "

    init {
        literal("create") {
            greedyString("id") {
                doesLaterIfHas(PermissionTypes.AUTHORIZE_CAPES) { context ->
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

                    val newCape = Cape(type = type)
                    capeUserMap.getOrPut(user.id) {
                        CapeUser(user.id, arrayListOf(newCape), type == CapeType.DONOR)
                    }.addCape(newCape)

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
                    doesLaterIfHas(PermissionTypes.AUTHORIZE_CAPES) { context ->
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

                        user.deleteCape(cape)

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
                            val playerName = UUIDManager.getByUUID(it.playerUUID)?.name ?: "Not attached"
                            field("Cape UUID ${it.capeUUID}", "Player Name: $playerName\nCape Type: ${it.type.realName}")
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

                        val profilePair = UUIDManager.getByString(username)

                        val msg = if (profilePair != null) {
                            message.normal("Found UUID to attach to Cape `$capeUUID` - verifying")
                        } else {
                            message.error(
                                    "Couldn't find an account with the UUID/Name `$username`!\n" +
                                    "Make sure you have a real Mojang account and with correct UUID/Name, " +
                                    "contact a moderator if this keeps happening."
                            ).edit {
                                title = "Error"
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        val alreadyAttached = capes.find { it.playerUUID == profilePair.uuid }
                        if (alreadyAttached != null) {
                            msg.edit {
                                description = "You already have ${profilePair.name} attached to Cape `${alreadyAttached.capeUUID}`!"
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        // this is after everything because we don't care about Mojang requests that much, but we don't want to commit every 5 minutes or whatever
                        changeTimeOut(capeUUID)?.let {
                            msg.edit {
                                description = changeError(capeUUID, it)
                                color = Colors.error
                            }
                            return@doesLater
                        }

                        cape.playerUUID = profilePair.uuid

                        msg.edit {
                            description = "Successfully attached Cape `${cape.capeUUID}` to user `${profilePair.name}`"
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

                    val name = UUIDManager.getByUUID(cape.playerUUID)?.name
                    cape.playerUUID = null

                    message.success("Successfully removed $name from Cape `${cape.capeUUID}`!")
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
            doesLaterIfHas(PermissionTypes.AUTHORIZE_CAPES) {
                save()
                message.success("Saved!")
            }
        }

        literal("load") {
            doesLaterIfHas(PermissionTypes.AUTHORIZE_CAPES) {
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
                val cacheList = Gson().fromJson<List<CapeUser>>(bufferedReader, object : TypeToken<List<CapeUser>>() {}.type)
                    capeUserMap.clear()
                    capeUserMap.putAll(cacheList.associateBy { it.id })
            }
        } catch (e: Exception) {
            println("=".repeat(20))
            println("Error reading capes!!")
            println("=".repeat(20))
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
        } ?: return

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

    private fun CapeUser.deleteCape(cape: Cape) {
        this.capes.remove(cape)
    }

    private fun CapeUser.addCape(cape: Cape): CapeUser {
        return apply {
            this.capes.removeIf { it.capeUUID == cape.capeUUID }
            this.capes.add(cape)
            this.isPremium = this.isPremium || capes.any { it.type == CapeType.DONOR }
        }
    }

    private suspend fun Message.getCapes(): ArrayList<Cape>? {
        return author?.let { author ->
            capeUserMap[author.id]?.capes.also {
                if (it == null) error("User ${author.mention} does not have any capes!")
            }
        }
    }

    private suspend fun CapeColor.toEmoji(): String {
        return StringBuilder(4).run {
            append(makeEmojiFromHex(primary)?.let { "<:${it.name}:${it.id}> " } ?: missingTexture)
            append("Primary (#${primary})\n")

            append(makeEmojiFromHex(border)?.let { "<:${it.name}:${it.id}> " } ?: missingTexture)
            append("Border (#${border})")

            toString()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun makeEmojiFromHex(hex: String): Emoji? {
        trimAndSyncEmojis()

        return server?.let {
            try {
                cachedEmojis.getOrPut(hex) {
                    val b = ByteArrayOutputStream()
                    val bufferedImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)

                    for (x in 0 until bufferedImage.width) for (y in 0 until bufferedImage.height) {
                        bufferedImage.setRGB(x, y, Integer.decode("0x$hex"))
                    }

                    ImageIO.write(bufferedImage, "jpg", b)

                    it.createEmoji {
                        name = hex
                        image = b.toByteArray()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun trimAndSyncEmojis() {
        server?.let { server ->
            // Sync emojis
            cachedEmojis.values.removeIf { !server.emojis.contains(it) }
            server.emojis.reversed().forEach {
                cachedEmojis[it.name] = it
            }

            // Delete emoji if getting close to emoji limit
            val maxEmojiSlots = server.maxEmojiSlots()
            while (cachedEmojis.size + 5 >= maxEmojiSlots) {
                val key = cachedEmojis.keys.first()
                cachedEmojis.remove(key)?.delete()
            }
        }
    }

    private fun changeTimeOut(capeUUID: String): Double? {
        val time = changedTimeouts.getOrPut(capeUUID) {
            System.currentTimeMillis()
            return null
        }

        val difference = System.currentTimeMillis() - time

        if (difference > 900000) { // waited more than 15 minutes, reset the timer
            changedTimeouts[capeUUID] = System.currentTimeMillis()
            return null
        }

        return MathHelper.round((900000 - difference) / 60000.0, 2) // / convert ms to minutes, with 2 decimal places
    }

    private val server
        get() = cachedServer ?: run {
            val server = Main.client?.servers?.find(775449131736760361)
            cachedServer = server
            server
        }

    private fun capeError(capeUUID: String) = "Couldn't find a Cape with a UUID of `$capeUUID`. Make sure you're entering the short UUID as the Cape UUID, not your player UUID"
    private fun changeError(capeUUID: String, time: Double) = "Cape `$capeUUID` was changed recently, you must wait $time more minutes before you can change it again!"
}
