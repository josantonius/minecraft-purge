# Minecraft Purge Plugin

[![License](https://img.shields.io/github/license/josantonius/minecraft-purge)](LICENSE)

Purge plugin for Minecraft servers.

## [Watch demo on YouTube](https://www.youtube.com/watch?v=rLDzeU9BkUI)

## Requirements

- Java 17 or higher.
- Purpur server 1.19.3 or Bukkit/Spigot/Paper server compatible with the Purpur API version used.

## Installation

1. Download the JAR file: [purge-1.0.1-purpur-1.19.3.jar](/build/libs/purge-1.0.1-purpur-1.19.3.jar).

1. Place the JAR file in the plugins folder of your Minecraft server.

1. Restart the server to load the plugin.

## Building

To build the plugin yourself, follow these steps:

1. Make sure you have `Java 17` or higher and `Gradle` installed on your system.

1. Clone the plugin repository on your local machine:

    ```bash
    git clone https://github.com/josantonius/minecraft-purge.git
    ```

1. Navigate to the directory of the cloned repository:

    ```bash
    cd minecraft-purge
    ```

1. Use Gradle to compile the plugin:

    ```bash
    gradle build
    ```

### Introduction and duration of The Purge

- The Purge begins with a 2-minute introduction that announces the start of the game and plays the
announcement sound.

  During the announcement, the list of rules and privileges that will be nullified during the purge
  will be displayed in the chat. You can add or remove rules and privileges by editing the
  corresponding lines in the plugin's message file (`plugins/Purge/messages.yml`).

  ```yml
  rules:
    rule_1: "<dark_gray>Rule 04 <red>- <dark_aqua>PvP abuse is allowed"
    rule_2: "<dark_gray>Rule 11 <red>- <dark_aqua>TPA Kill is allowed"

  privileges:
    privilege_1: "<dark_gray>Privilege 08 <red>- <dark_aqua>Experience is dropped upon death"
    privilege_2: "<dark_gray>Privilege 15 <red>- <dark_aqua>Inventory is dropped upon death"
    privilege_3: "<dark_gray>Privilege 24 <red>- <dark_aqua>Backpack is dropped upon death"
  ```

- After 2 minutes, the game starts playing the startup sound (it lasts 03:04 and repeats in a loop)
and displaying a message in the chat announcing the start of the purge.

- At the end, the end sound is played and a message is displayed in the chat announcing the end of
the purge.

## Game Modes

The Purge has two game modes: supervised and unsupervised.

### Supervised Mode

In supervised mode, players can request immunity, but operators or players with `purge.admin`
permission must manually grant it. Operators and players with `purge.admin` permission have
guaranteed immunity in this mode and will not require prior approval.

To grant or withdraw immunity at any time, operators or players with `purge.admin` permission can
use the following command:

```yml
/purge immune [add/remove] <player>
```

### Unsupervised Mode

In unsupervised mode, immunity is automatically granted. If players request immunity during the
announcement time, it will be automatically granted at the start of The Purge. If they request it
after The Purge has started, it will be automatically granted between 3 and 8 minutes later. If they
request immunity when there are 10 minutes or less to start, no immunity will be granted.

### Immunity

Players with immunity:

- Cannot be attacked by players without immunity.
- Cannot attack players without immunity.
- Can attack other immune players.
- Can be attacked by other immune players.
- Will not lose their inventory, experience, and backpack upon death.
- Will be able to execute commands blocked during The Purge.
- Will have the "glowing" effect during The Purge to be easily identified.
- They are prefixed with `[Immune]` in the list of players if the dependency to the TAB plugin exists.

Players without immunity:

- Cannot be attacked by immune players.
- Cannot attack immune players.
- Can attack other players without immunity.
- Can be attacked by other players without immunity.
- Will lose their inventory, experience, and backpack upon death.
- Will not be able to execute commands blocked during The Purge.
- Will not have the "glowing" effect during The Purge.

**No distinction is made between operators or game modes. All players, both immune and non-immune,
are treated the same way.**

**Immunity protection covers players against direct attacks (hits, projectiles, throwable potions...),
but not against indirect damages such as those caused by explosions, fires, or lava.**

## Commands

- `/purge help` - Displays the plugin help. Permission: `purge.use`.

- `/purge immune <action> <player>` - Grants or withdraws immunity to a player. Permission: `purge.admin`.

- `/purge exit` - Requests immunity during The Purge. Permission: `purge.use`.

- `/purge start <time> <mode>` - Starts The Purge.  Permission: `purge.admin`.

- `/purge end` - Ends The Purge immediately.  Permission: `purge.admin`.

- `/purge cancel` - Cancels The Purge.  Permission: `purge.admin`.

- `/purge reload` - Reloads the plugin.  Permission: `purge.admin`.

## Configuration

The `plugins/Purge/config.yml` file contains specific plugin configurations.

Here you can set different options such as the sounds that will be played during the different
moments of The Purge and the commands that will be blocked for players without immunity during The Purge.

```yaml
# Sound to be played when The Purge is announced.
announcePurgeSound: "minecraft:purge.sound.announce"

# Sound that will be played during The Purge.
ongoingPurgeSound: "minecraft:purge.sound.ongoing"

# Sound that will be played when The Purge ends.
endPurgeSound: "minecraft:purge.sound.end"

# Commands that players without immunity will not be able to use during The Purge.
blockedCommands:
  - "/spawn"
  - "/home"

# Main world. Here players will be sent from the locked worlds.
mainWorld: "spawn"

# Worlds that will not be used during the purge.
blockedWorlds:
  - "world_nether"
  - "world_the_end"

# Apply a cooldown of n seconds between uses to the ender pearls.
enderPearlCooldown: 10

# Apply a cooldown of n seconds between uses to the firework rockets.
fireworkRocketCooldown: 10
```

## Messages

The `plugins/Purge/messages.yml` file contains all the messages that the plugin uses.
You can change the messages to your liking.

## Resources pack

The plugin includes a resource pack that adds sounds to the game. The resource pack is optional and
is not required for the plugin to work. You can download the resource pack for the version of the
game you are using:

- [Resource pack for 1.19 version](resource-packs/purge-resource-pack-1.19.zip)
- [Resource pack for 1.20 version](resource-packs/purge-resource-pack-1.20.zip)

**The announcement sound do not include the voice of the announcer. If you want to add voices like
the one used in the video demo you can do it from [https://genny.lovo.ai](https://genny.lovo.ai).**

Three sounds are played during the purge: an announcement sound, a sound during the process,
and a sound at the end. These sounds can be customized in the plugin's configuration file
(`plugins/Purge/config.yml`). Each of these sounds are defined as follows:

### Announcement Sound

This is the sound that plays when The Purge is announced. By default, the sound is
`minecraft:purge.sound.announce`. To change it, you must edit the corresponding line in the
configuration file:

```yaml
ongoingPurgeSound: "minecraft:purge.sound.announce"
```

Replace `minecraft:purge.sound.announce` with the name of the sound you want to use.

### Sound During The Purge

This is the sound that plays during The Purge. By default, the sound is
`minecraft:purge.sound.ongoing`. To change it, you must edit the corresponding
line in the configuration file:

```yaml
ongoingPurgeSound: "minecraft:purge.sound.ongoing"
```

Replace `minecraft:purge.sound.ongoing` with the name of the sound you want to use.

### Sound at the End of The Purge

This is the sound that plays when The Purge ends. By default, the sound is
`minecraft:purge.sound.end`. To change it, you must edit the corresponding line
in the configuration file:
  
```yaml
endPurgeSound: "minecraft:purge.sound.end"
```

Replace `minecraft:purge.sound.end` with the name of the sound you want to use.

To add custom sounds, you must include them in a resource pack and add the corresponding entries in
the `sounds.json` file of the package. The following shows an example of how the sounds are defined
in this file:

```yml
{
  "purge.sound.announce": {
    "sounds": [
      {
        "name": "purge/announce_purge",
        "stream": true
      }
    ]
  },
  "purge.sound.ongoing": {
    "sounds": [
      {
        "name": "purge/ongoing_purge",
        "stream": true
      }
    ]
  },
  "purge.sound.end": {
    "sounds": [
      {
        "name": "purge/end_purge",
        "stream": true
      }
    ]
  }
}
```

## TODO

- [ ] Add new feature
- [ ] Create tests
- [ ] Improve documentation

## Changelog

Detailed changes for each release are documented in the
[release notes](https://github.com/josantonius/minecraft-purge/releases).

## Contribution

Please make sure to read the [Contributing Guide](.github/CONTRIBUTING.md), before making a pull
request, start a discussion or report a issue.

Thanks to all [contributors](https://github.com/josantonius/minecraft-purge/graphs/contributors)! :heart:

## Sponsor

If this project helps you to reduce your development time,
[you can sponsor me](https://github.com/josantonius#sponsor) to support my open source work :blush:

## License

This repository is licensed under the [MIT License](LICENSE).

Copyright © 2023-present, [Josantonius](https://github.com/josantonius#contact)
