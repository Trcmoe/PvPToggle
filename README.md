# PvPToggle
Plugin to toggle the ability to pvp

### Server compatibility and installation

PvPToggle 1.8.1 builds against the Spigot 26.3 API and supports Minecraft 26.3
servers using Spigot, Paper, or compatible forks. Run 26.3 servers with Java 25.
The plugin uses the Bukkit API only; Folia is not supported.

The minimum API version is 1.20.5 and the JAR targets Java 21 bytecode. Older
1.20.5–1.21.x servers can use the same JAR, but 26.3 is the primary test target.

Copy `PvPToggle-1.8.1.jar` into `plugins/` and restart the server. Existing
`plugins/PvPToggle/config.yml` and player data are preserved. PlaceholderAPI
(built against 2.12.3) and NametagEdit are optional. To use `SETTINGS.NAMETAG`,
install a NametagEdit build compatible with your server. Its API is resolved
only when installed, so neither it nor its retired Maven repository is needed
to build or run PvPToggle. The placeholder is `%pvptoggle_pvp_state%`.

### 中文配置与 TAB 联动

默认提示和配置注释已翻译为简体中文。已有服务器请备份配置，将
`src/main/resources/config.yml` 中的 `MESSAGES` 段合并到现有配置，再运行 `/pvp reload`；
原有 `SETTINGS` 可以保留。聊天状态文字由 `PVP_STATE_ON` / `PVP_STATE_OFF` 配置。

[TAB 中文设置教程](docs/TAB-中文教程.md) 包含占位符检测、中文输出替换、玩家后缀和代理端配置。
`%pvptoggle_pvp_state%` 继续返回 `&cOn` / `&aOff`，兼容已有规则。

### Build

Install JDK 25 and Maven 3.9+, then run:

```sh
mvn --batch-mode verify
```

The plugin is written to `target/PvPToggle-1.8.1.jar`. The server and optional
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
