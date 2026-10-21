package com.nametagedit.plugin.api;

import org.bukkit.entity.Player;

public interface INametagApi {
    void clearNametag(Player player);
    void setPrefix(Player player, String prefix);
}
