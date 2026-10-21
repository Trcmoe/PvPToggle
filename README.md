# PvPToggle
Plugin to toggle the ability to pvp

### Server compatibility and installation

PvPToggle 1.8.0 builds against the Spigot 26.3 API and supports Minecraft 26.3
servers using Spigot, Paper, or compatible forks. Run 26.3 servers with Java 25.
The plugin uses the Bukkit API only; Folia is not supported.

The minimum API version is 1.20.5 and the JAR targets Java 21 bytecode. Older
1.20.5–1.21.x servers can use the same JAR, but 26.3 is the primary test target.

Copy `PvPToggle-1.8.0.jar` into `plugins/` and restart the server. Existing
`plugins/PvPToggle/config.yml` and player data are preserved. PlaceholderAPI
(built against 2.12.3) and NametagEdit are optional. To use `SETTINGS.NAMETAG`,
install a NametagEdit build compatible with your server. Its API is resolved
only when installed, so neither it nor its retired Maven repository is needed
to build or run PvPToggle. The placeholder is `%pvptoggle_pvp_state%`.

### Build

Install JDK 25 and Maven 3.9+, then run:

```sh
mvn --batch-mode verify
```

The plugin is written to `target/PvPToggle-1.8.0.jar`. The server and optional
plugin APIs are not bundled. GitHub Actions runs the tests and uploads the JAR.

### Commands
/pvp {toggle|on|off|status} - used to change pvp state

/pvp <player> - used to see the pvp state of another player

/pvp {toggle|on|off} <player> - used to set the pvp state of another player

/pvp reload - reload configuration, including blocked worlds (also available in console)

### Permission Nodes
pvptoggle.allow - grants access to /pvp {toggle|on|off|status} command

pvptoggle.others - grants access to /pvp (player) command

pvptoggle.others.set - grants access to /pvp (player) {toggle|on|off} command

pvptoggle.bypass - allows player to bypass the /pvp off cooldown

pvptoggle.reload - allows player to reload the config
