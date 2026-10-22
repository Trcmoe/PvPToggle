package com.github.aasmus.pvptoggle;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.github.aasmus.pvptoggle.utils.Chat;
import com.github.aasmus.pvptoggle.utils.Util;

public class PvPCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) return false;

        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!requirePermission(player, "pvptoggle.allow")) return true;
                Chat.send(player, "PVP_STATUS", null, Util.getPlayerState(player.getUniqueId()));
                Chat.send(player, "HELP_HEADER");
                Chat.send(player, "HELP_GENERAL_USEAGE");
                if (player.hasPermission("pvptoggle.others")) Chat.send(player, "HELP_VIEW_OTHERS");
                if (player.hasPermission("pvptoggle.others.set")) Chat.send(player, "HELP_SET_OTHERS");
            } else {
                Chat.send(sender, "HELP_HEADER");
                Chat.send(sender, "HELP_SET_OTHERS");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player && !sender.hasPermission("pvptoggle.reload")) {
                Chat.send(sender, "COMMAND_NO_PERMISSION");
            } else {
                reloadConfig();
            }
            return true;
        }

        if (args.length == 1 && sender instanceof Player player) {
            String action = args[0];
            if (action.equalsIgnoreCase("status")) {
                if (requirePermission(player, "pvptoggle.allow"))
                    Chat.send(player, "PVP_STATUS", null, Util.getPlayerState(player.getUniqueId()));
            } else if (isAction(action)) {
                if (requirePermission(player, "pvptoggle.allow")) changeState(player, player, action, false);
            } else if (requirePermission(player, "pvptoggle.others")) {
                Player other = Bukkit.getPlayerExact(action);
                if (other == null) Chat.send(player, "NO_PLAYER", action);
                else Chat.send(player, "PVP_STATUS_OTHERS", other.getDisplayName(),
                        Util.getPlayerState(other.getUniqueId()));
            }
            return true;
        }

        if (args.length == 2 && isAction(args[0])) {
            if (sender instanceof Player && !requirePermission(sender, "pvptoggle.others.set")) return true;
            Player other = Bukkit.getPlayerExact(args[1]);
            if (other == null) Chat.send(sender, "NO_PLAYER", args[1]);
            else changeState(sender, other, args[0], true);
            return true;
        }

        Chat.send(sender, "COMMAND_INVALID_PARAMETER", args[0]);
        return true;
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        Chat.send(sender, "COMMAND_NO_PERMISSION");
        return false;
    }

    private boolean isAction(String action) {
        return action.equalsIgnoreCase("toggle") || action.equalsIgnoreCase("on")
                || action.equalsIgnoreCase("off");
    }

    private void changeState(CommandSender caller, Player target, String action, boolean other) {
        boolean current = Util.getPlayerState(target.getUniqueId()); // true means PvP is off
        boolean disabled = action.equalsIgnoreCase("toggle") ? !current : action.equalsIgnoreCase("off");
        if (!other && disabled && !caller.hasPermission("pvptoggle.bypass") && Util.getCooldown(target)) return;
        if (!Util.setPlayerState(target, disabled, caller)) return;

        if (current && !disabled) Util.setCooldownTime(target);
        Chat.send(target, disabled ? "PVP_STATE_DISABLED" : "PVP_STATE_ENABLED");
        if (current && !disabled) {
            if (PvPToggle.instance.getConfig().getBoolean("SETTINGS.PARTICLES")) Util.particleEffect(target);
            if (PvPToggle.instance.getConfig().getBoolean("SETTINGS.NAMETAG")) Util.ChangeNametag(target, "&c");
        } else if (!current && disabled && PvPToggle.instance.getConfig().getBoolean("SETTINGS.NAMETAG")) {
            Util.ChangeNametag(target, "reset");
        }
        if (other) Chat.send(caller, "PVP_STATE_CHANGED_OTHERS", target.getDisplayName(), disabled);
    }

    public void reloadConfig() {
        PvPToggle.instance.reloadConfig();
        PvPToggle.instance.config = PvPToggle.instance.getConfig();
        PvPToggle.blockedWorlds = PvPToggle.instance.config.getStringList("SETTINGS.BLOCKED_WORLDS");
    }
}
