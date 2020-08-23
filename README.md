# bot-kt

The moderation bot for KAMI Blue's Discord, written in Kotlin for reliability. 

Uses [Brigadier](https://github.com/Mojang/brigadier/) with a wrapper for the command system, and [reflections](https://github.com/ronmamo/reflections) for automatic command registering.

## Contributing

0. `git clone git@github.com:kami-blue/bot-kt.git`
1. In Intellij IDEA select `File -> New -> Project from existing sources`
3. Import the `build.gradle` file
4. Wait for the Gradle import to finish

## Usage

Create your `auth.json` like below. Generate `githubToken` with `repo:status` and `public_repo` access checked [here](https://github.com/settings/tokens) and you can get your bot token [here](https://discord.com/developers/applications/BOT_ID_HERE/bot).
```json
{
    "botToken": "token",
    "githubToken": "token"
}
```

### Running in Intellij

Hit the Run ▶️ button in the top right of your IDE, to the right of MainKt
️
If the MainKt run configuration isn't imported automatically in Intellij, try `File -> Close Project` and then reopen the project. 

If that still does not help, Hit `Add Configuration` in the upper right of your IDE, select the MainKt configuration on the left and hit Ok.

### Running prebuilt binaries

```bash
java -jar bot-kt-${VER}-SNAPSHOT.jar
```

### Disabling update checking

If you're working on your own fork or just don't care for updates for some reason, you can create a file named `noUpdateCheck` in the same directory where you run the bot from.

If you're on Windows, please make sure you don't have .txt at the end, as Windows hides file extensions by default.

