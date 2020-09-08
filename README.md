# bot-kt

![GitHub All Releases](https://img.shields.io/github/downloads/kami-blue/bot-kt/total)
![Discord](https://img.shields.io/discord/573954110454366214)

The moderation bot for KAMI Blue's Discord, written in Kotlin for reliability. 

Why should you use this over other Kotlin bots:
- Extremely easy to add your own commands
- Really easy to configure
- Entirely modular, including config types and commands
- Config types support providing a URL instead of a file name, allowing you to load configurations remotely
- Command registration is automatic, just create an object which extends `Command("commandName")`
- Uses Brigadier as a command system, making creating a command as easy as just writing a few lambdas 
- Has (optional) automatic updating which is compatible with pm2

## Contributing

0. `git clone git@github.com:kami-blue/bot-kt.git`
1. In Intellij IDEA select `File -> New -> Project from existing sources`
3. Import the `build.gradle` file
4. Wait for the Gradle import to finish

## Usage

Create your `config/auth.json` like below. Generate `githubToken` with `repo:status` and `public_repo` access checked [here](https://github.com/settings/tokens) and you can get your bot token [here](https://discord.com/developers/applications/BOT_ID_HERE/bot).

The `githubToken` is only required if you want to use any of the Github commands, such as `;issue`

```json
{
    "botToken": "token",
    "githubToken": "token"
}
```

<details>
    <summary>`user.json` example</summary>

All elements are optional. `statusMessageType` defaults to "Playing".

```json
{
    "autoUpdate": "true",
    "primaryServerId": "573954110454366214",
    "startUpChannel": "dev-bot",
    "statusMessage": "out for raids",
    "statusMessageType": "3"
}
```

</details>

## Running 

#### Running in Intellij

Hit the Run ▶️ button in the top right of your IDE, to the right of MainKt.

If the MainKt run configuration isn't imported automatically in Intellij, try `File -> Close Project` and then reopen the project. 

If that still does not help, Hit `Add Configuration` in the upper right of your IDE, select the MainKt configuration on the left and hit Ok.

#### Running prebuilt binaries in pm2.
 
You obviously need [pm2](https://pm2.keymetrics.io/) installed first. 

You need a pm2 config the first time you run it. Run the bot with this command to create the config:
```bash
java -jar -Dbot-kt.create-pm2-config=true bot-kt-1.0.6.jar
```

Once you have a config, you can run `pm2 start process.json` and it will start the process. You can do `pm2 list` to list active processes and `pm2 reload bot-kt` to reload it.

It will automatically start up again if it crashes, in the unlikely scenario that it does crash. Please report any bugs or crashes on the issues page
 
It will also automatically reload when auto updating.

#### Running prebuilt binaries

```bash
java -jar bot-kt-1.0.6.jar
```

#### Disabling update checking

If you're working on your own fork or just don't care for updates for some reason, you can create a file named `noUpdateCheck` in the same directory where you run the bot from.

If you're on Windows, please make sure you don't have .txt at the end, as Windows hides file extensions by default.

## Creating a new command

Create an object in the commands package that extends `Command("yourCommandName")`. Look at ExampleCommand for example usage of our Brigadier DSL wrapper.

## Creating a new config type

Simply create a new dataclass inside FileManager, and register it inside ConfigType. 

Here's an example:

```kotlin
object FileManager {
    var exampleConfigData: ExampleConfig? = null
}
```

```kotlin
/**
 * [someValue] is a value from inside your example.json
 */
data class ExampleConfig(val someValue: String)
```

```kotlin
/**
 * Note that [configPath] can also be an https URL, but you will not be able to write the config if it's a remote URL. This is fine for remotely configuring a setting.
 */
enum class ConfigType(val configPath: String, var data: Any?, val clazz: Class<*>) {
    EXAMPLE("example.json", exampleConfigData, ExampleConfig::class.java);
}
```

Storing the value: (can be a remote file online or just a local file)

```json
{
    "someValue": "This is a String value for my ExampleConfig"
}
```

Reading the value: 

```kotlin
val config = FileManager.readConfigSafe<ExampleConfig>(ConfigType.EXAMPLE, false) // setting reload to true instead of false will forcefully load it from the URL / memory instead of returning the cached version

config?.someValue?.let {
    println("Value is '$it'")
}

// > Value is 'This is a String value for my ExampleConfig'
```
