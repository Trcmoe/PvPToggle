# 在 TAB 中显示 PvPToggle 状态

适用于本仓库的 PvPToggle 1.8.1，以及上一版 1.8.0。下文使用 TAB 官方文档中的配置格式。

## 1. 安装与汉化

单服：在同一台 Spigot/Paper 服务端的 `plugins/` 中安装 PvPToggle、PlaceholderAPI 和 TAB，然后完整重启服务端。PlaceholderAPI 支持由 PvPToggle 内置注册，不需要执行 `/papi ecloud download PvPToggle`，也不需要下载名称相近的其他 PvP 扩展。

语言文本位于 `plugins/PvPToggle/config.yml` 的 `MESSAGES` 中，没有单独的 `messages.yml`。

- 新装插件：1.8.1 自动生成中文默认配置。
- 已有配置：备份原文件，将仓库 `src/main/resources/config.yml` 的整个 `MESSAGES` 段复制到服务端配置中，保留自己的 `SETTINGS`，然后执行 `/pvp reload`。
- 更新 JAR：停服，移除旧版 PvPToggle JAR，放入 1.8.1，再启动。更新 JAR 本身不会覆盖已有语言文本。

`MESSAGES.PVP_STATE_ON` / `PVP_STATE_OFF` 控制聊天提示中 `<pvpstate>` 的文字。它们不改变下文 PlaceholderAPI 的原始返回值，已有 TAB 替换规则可以继续使用。

## 2. 先确认 PlaceholderAPI 能解析

**正确变量只有下面这一项：**

```text
%pvptoggle_pvp_state%
```

变量包含 `pvptoggle`、`pvp`、`state` 三部分，不是 `%pvp_state%`。

用有权限的在线玩家执行：

```text
/papi list
/papi parse me %pvptoggle_pvp_state%
```

扩展列表应包含 PvPToggle。控制台调试时，用一个在线玩家名替换 `me`，例如 `papi parse Steve %pvptoggle_pvp_state%`。没有玩家上下文时，本插件返回空字符串，因此不要用 `--null` 测试玩家状态。

| 玩家状态 | 原始返回文本 | 含义 |
| --- | --- | --- |
| `/pvp on` | `&cOn` | PvP 开启，红色 |
| `/pvp off` | `&aOff` | PvP 关闭，绿色 |

聊天窗口可能已经渲染颜色，看不见 `&c` / `&a`。下一步的 `/tab parse` 可以查看原始颜色信息。这是玩家的 PvP 开关状态，不是统计玩家此刻是否正在交战。

如果这里仍显示变量原文，先检查 PvPToggle 与 PlaceholderAPI 是否启用、启动日志是否有异常，并在安装两者后完整重启；先解决这一层，再检查 TAB。[PlaceholderAPI 命令文档](https://wiki.placeholderapi.com/users/commands/)、[TAB 占位符排查](https://github.com/NEZNAMY/TAB/wiki/Placeholders#placeholderapi-placeholder-is-not-working)。

## 3. 在 TAB 中把 On / Off 显示为中文

编辑 `plugins/TAB/config.yml`，将下列内容**合并到已有的同名节点**。不要在文件底部再创建重复的 `placeholder-output-replacements`、`placeholder-refresh-intervals` 或 `tablist-name-formatting` 节点，也不要覆盖其他变量规则。

```yaml
tablist-name-formatting:
  enabled: true

placeholder-output-replacements:
  '%pvptoggle_pvp_state%':
    '&cOn': '&c开启'
    '&aOff': '&a关闭'

placeholder-refresh-intervals:
  '%pvptoggle_pvp_state%': 500
```

替换必须连颜色代码和大小写一起匹配：`&cOn` 与 `On` 不相同。500 的单位是毫秒，即每半秒更新一次。[输出替换规则](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Placeholder-output-replacements)、[刷新间隔](https://github.com/NEZNAMY/TAB/wiki/Placeholders#refresh-intervals)。

## 4. 显示在 TAB 玩家名字后面

编辑 `plugins/TAB/groups.yml`，在已有 `_DEFAULT_` 下合并 `tabsuffix`：

```yaml
_DEFAULT_:
  tabsuffix: ' &8[&7PvP：%pvptoggle_pvp_state%&8]'
```

效果是每名玩家自己的状态，例如 `Steve [PvP：开启]` 或 `Alex [PvP：关闭]`。

若原来已有 `tabsuffix`，在原字符串后追加这一段。例如原值为 `%luckperms-suffix%`，改为：

```yaml
_DEFAULT_:
  tabsuffix: '%luckperms-suffix% &8[&7PvP：%pvptoggle_pvp_state%&8]'
```

`_DEFAULT_` 是后备值。若具体组、玩家或按世界/服务器设置中已有 `tabsuffix`，需要把这段变量也追加到那些实际生效的值中。[TAB 名称格式与优先级](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Tablist-name-formatting)。

如果要放在已经配置好的侧边栏或页眉页脚文本中，可以直接加入一行 `&7PvP：%pvptoggle_pvp_state%`，同样会应用 TAB 的输出替换。

## 5. 重载与验证

保存后执行：

```text
/tab reload
/tab parse Steve %pvptoggle_pvp_state%
```

将 `Steve` 换成在线玩家名。分别让该玩家执行 `/pvp on`、`/pvp off`，等待半秒，查看 TAB 是否显示红色“开启”和绿色“关闭”。如果配置了冷却或世界限制，请以 `/pvp status` 的实际结果为准，不要只看命令是否输入成功。

TAB 的替换只影响 TAB 中的显示；`/papi parse` 仍返回原来的 On / Off，属于正常现象。[TAB 调试命令](https://github.com/NEZNAMY/TAB/wiki/Commands-&-Permissions#tab-parse-player-text)。

若 PAPI 能解析而 TAB 不能，使用 `/tab parse 在线玩家名 %pvptoggle_pvp_state%` 查看结果。若能解析却看不到后缀，检查 `tablist-name-formatting.enabled`、已有的 `disable-condition` 和 `groups.yml` / `users.yml` 中覆盖默认值的后缀设置。

如果 NametagEdit 和 TAB 同时管理玩家头顶名称，建议保留 PvPToggle 的 `SETTINGS.NAMETAG: false`，交由 TAB 配置显示；单纯显示这里的状态后缀不需要 NametagEdit。

## 6. TAB 装在 Velocity / BungeeCord 时

PvPToggle 与 PlaceholderAPI 装在实际运行游戏的后端服，TAB 装在代理端。在每个需要读取状态的后端服安装 [TAB-Bridge](https://github.com/NEZNAMY/TAB-Bridge) 并重启，再在代理端 TAB 的配置里加入上述规则。

代理端 TAB 使用 `/btab reload`、`/btab parse 在线玩家名 %pvptoggle_pvp_state%`；`/papi parse` 仍在对应后端服调试。代理端管理权限也要在代理端授予，后端 OP 不等于代理端权限。[TAB 占位符桥接要求](https://github.com/NEZNAMY/TAB/wiki/Placeholders#placeholderapi-placeholder-is-not-working)、[代理端命令与权限](https://github.com/NEZNAMY/TAB/wiki/Commands-&-Permissions)。
