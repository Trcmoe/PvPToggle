package com.github.aasmus.pvptoggle.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.github.aasmus.pvptoggle.PvPToggle;

public class Chat {

    public static void send(CommandSender sender, String message) {
        send(sender, message, null, null);
    }

    public static void send(CommandSender sender, String message, String parameter) {
        send(sender, message, parameter, null);
    }

    public static void send(CommandSender sender, String message, String parameter, Boolean pvpState) {
        String msg = PvPToggle.instance.getConfig().getString("MESSAGES." + message);
        if (msg == null || msg.isEmpty()) return;
        String output = msg.replace("<parameter>", parameter == null ? "" : parameter);
        if (pvpState != null) output = output.replace("<pvpstate>", pvpState ? "off" : "on");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', output));
    }
}
