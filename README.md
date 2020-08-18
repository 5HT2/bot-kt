# bot-kt

The moderation bot for KAMI Blue's Discord, written in Kotlin for reliability. 

Uses [Brigadier](https://github.com/Mojang/brigadier/) with a wrapper for the command system.

## Contributing

0. `git clone git@github.com:kami-blue/bot-kt.git`
1. In Intellij IDEA select `File -> New -> Project from existing sources`
3. Import the `build.gradle` file
4. Wait for the Gradle import to finish

## Usage

Create your `auth.json` like below. Generate `githubToken` with full `repo:status` and `public_repo` access checked [here](https://github.com/settings/tokens) and you can get your bot token [here](https://discord.com/developers/applications/BOT_ID_HERE/bot).
```json
{
    "botToken": "token",
    "githubToken": "token"
}
```