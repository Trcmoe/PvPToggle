package com.github.aasmus.pvptoggle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.github.aasmus.pvptoggle.utils.Chat;
import com.github.aasmus.pvptoggle.utils.Util;

class PvPCommandTest {
    private PvPToggle plugin;
    private PvPCommand command;
    private Player player;
    private World world;
    private Command bukkitCommand;
    private MockedStatic<Bukkit> bukkit;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        plugin = mock(PvPToggle.class);
        PvPToggle.instance = plugin;
        plugin.players = new HashMap<>();
        plugin.cooldowns = new HashMap<>();
        PvPToggle.blockedWorlds = List.of();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        config.set("SETTINGS.PARTICLES", false);
        config.set("SETTINGS.NAMETAG", false);
        config.set("SETTINGS.COOLDOWN", 30);
        when(plugin.getConfig()).thenReturn(config);
        plugin.config = config;

        player = mock(Player.class);
        world = mock(World.class);
        bukkitCommand = mock(Command.class);
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getWorld()).thenReturn(world);
        when(player.getDisplayName()).thenReturn("Alice");
        when(world.getPVP()).thenReturn(true);
        when(world.getName()).thenReturn("arena");
        when(player.hasPermission("pvptoggle.allow")).thenReturn(true);
        when(bukkitCommand.getName()).thenReturn("pvp");
        bukkit = mockStatic(Bukkit.class);
        command = new PvPCommand();
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        PvPToggle.instance = null;
        PvPToggle.blockedWorlds = null;
    }

    private void run(org.bukkit.command.CommandSender sender, String... args) {
        assertTrue(command.onCommand(sender, bukkitCommand, "pvp", args));
    }

    @Test
    void selfEnableStartsCooldownAndStatusRemainsReadable() {
        plugin.players.put(playerId, true);
        run(player, "on");
        assertFalse(plugin.players.get(playerId));
        assertTrue(plugin.cooldowns.containsKey(playerId));
        verify(player).sendMessage(contains("Your pvp has been enabled"));

        run(player, "status");
        verify(player).sendMessage(contains("Your pvp is set to on"));
        run(player, "toggle");
        assertFalse(plugin.players.get(playerId));
        verify(player).sendMessage(contains("Too fast"));
    }

    @Test
    void bypassCanDisableDuringCooldown() {
        plugin.players.put(playerId, false);
        plugin.cooldowns.put(playerId, new Date());
        when(player.hasPermission("pvptoggle.bypass")).thenReturn(true);
        run(player, "off");
        assertTrue(plugin.players.get(playerId));
    }

    @Test
    void noArgumentHelpRequiresAllowButReloadHasItsOwnPermission() {
        when(player.hasPermission("pvptoggle.allow")).thenReturn(false);
        run(player);
        verify(player).sendMessage(contains("permission"));
        verify(player, never()).sendMessage(contains("Your pvp is set"));
        verify(player, never()).sendMessage(contains("Commands:"));

        when(player.hasPermission("pvptoggle.reload")).thenReturn(true);
        run(player, "reload");
        verify(plugin).reloadConfig();
    }

    @Test
    void adminPermissionsWorkWithoutSelfTogglePermission() {
        when(player.hasPermission("pvptoggle.allow")).thenReturn(false);
        when(player.hasPermission("pvptoggle.others")).thenReturn(true);
        when(player.hasPermission("pvptoggle.others.set")).thenReturn(true);
        Player other = mock(Player.class);
        UUID otherId = UUID.randomUUID();
        when(other.getUniqueId()).thenReturn(otherId);
        when(other.getWorld()).thenReturn(world);
        when(other.getDisplayName()).thenReturn("Bob");
        bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(other);
        plugin.players.put(otherId, false);

        run(player, "Bob");
        verify(player).sendMessage(contains("Bob has their pvp on"));
        run(player, "off", "Bob");
        assertTrue(plugin.players.get(otherId));
    }

    @Test
    void zeroCooldownAllowsImmediateDisable() {
        plugin.config.set("SETTINGS.COOLDOWN", 0);
        plugin.players.put(playerId, true);
        run(player, "on");
        run(player, "off");
        assertTrue(plugin.players.get(playerId));
        verify(player, never()).sendMessage(contains("Too fast"));
    }

    @Test
    void cooldownAtExpiryAllowsDisable() {
        plugin.config.set("SETTINGS.COOLDOWN", 1);
        plugin.players.put(playerId, false);
        plugin.cooldowns.put(playerId, new Date(System.currentTimeMillis() - 1000));
        run(player, "off");
        assertTrue(plugin.players.get(playerId));
        verify(player, never()).sendMessage(contains("Too fast"));
    }

    @Test
    void deniedPermissionAndWorldRestrictionDoNotClaimSuccess() {
        when(player.hasPermission("pvptoggle.allow")).thenReturn(false);
        run(player, "on");
        verify(player).sendMessage(contains("permission"));
        assertFalse(plugin.players.containsKey(playerId));

        when(player.hasPermission("pvptoggle.allow")).thenReturn(true);
        when(world.getPVP()).thenReturn(false);
        plugin.players.put(playerId, true);
        run(player, "on");
        assertTrue(plugin.players.get(playerId));
        assertFalse(plugin.cooldowns.containsKey(playerId));
        verify(player, never()).sendMessage(contains("Your pvp has been enabled"));
    }

    @Test
    void otherPlayerRequiresPermissionsAndUsesTargetsState() {
        Player other = mock(Player.class);
        UUID otherId = UUID.randomUUID();
        when(other.getUniqueId()).thenReturn(otherId);
        when(other.getWorld()).thenReturn(world);
        when(other.getDisplayName()).thenReturn("Bob");
        bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(other);
        plugin.players.put(otherId, false);

        run(player, "Bob");
        verify(player).sendMessage(contains("permission"));
        when(player.hasPermission("pvptoggle.others")).thenReturn(true);
        run(player, "Bob");
        verify(player).sendMessage(contains("Bob has their pvp on"));

        run(player, "off", "Bob");
        assertFalse(plugin.players.get(otherId));
        when(player.hasPermission("pvptoggle.others.set")).thenReturn(true);
        run(player, "off", "Bob");
        assertTrue(plugin.players.get(otherId));
        verify(other).sendMessage(contains("Your pvp has been disabled"));
        verify(player).sendMessage(contains("Bob had their pvp state changed to off"));
    }

    @Test
    void consoleCanReloadAndSetButInvalidActionsCannotMutate() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Player other = mock(Player.class);
        UUID otherId = UUID.randomUUID();
        when(other.getUniqueId()).thenReturn(otherId);
        when(other.getWorld()).thenReturn(world);
        when(other.getDisplayName()).thenReturn("Bob");
        bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(other);
        plugin.players.put(otherId, true);

        run(console, "reload");
        verify(plugin).reloadConfig();
        run(console, "bogus", "Bob");
        assertTrue(plugin.players.get(otherId));
        verify(console).sendMessage(contains("invalid parameter"));
        run(console, "on", "Bob");
        assertFalse(plugin.players.get(otherId));
        verify(console).sendMessage(contains("Bob had their pvp state changed to on"));
    }

    @Test
    void reloadRefreshesBlockedWorldsAndFailedSwitchIsNotReportedAsChanged() {
        YamlConfiguration updated = new YamlConfiguration();
        updated.set("SETTINGS.BLOCKED_WORLDS", List.of("arena"));
        updated.set("MESSAGES.PVP_WORLD_CANNOT_CHANGE_OTHERS", "blocked");
        when(plugin.getConfig()).thenReturn(updated);
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Player other = mock(Player.class);
        UUID otherId = UUID.randomUUID();
        when(other.getUniqueId()).thenReturn(otherId);
        when(other.getWorld()).thenReturn(world);
        bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(other);
        plugin.players.put(otherId, false);

        run(console, "reload");
        assertEquals(List.of("arena"), PvPToggle.blockedWorlds);
        assertSame(updated, plugin.config);
        run(console, "off", "Bob");
        assertFalse(plugin.players.get(otherId));
        verify(console).sendMessage("blocked");
        verify(console, never()).sendMessage(contains("changed"));
    }

    @Test
    void otherPlayerNametagUsesTarget() {
        plugin.config.set("SETTINGS.NAMETAG", true);
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        Player other = mock(Player.class);
        UUID otherId = UUID.randomUUID();
        when(other.getUniqueId()).thenReturn(otherId);
        when(other.getWorld()).thenReturn(world);
        when(other.getDisplayName()).thenReturn("Bob");
        bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(other);
        plugin.players.put(otherId, true);
        when(player.hasPermission("pvptoggle.others.set")).thenReturn(true);

        try (MockedStatic<Util> util = mockStatic(Util.class, CALLS_REAL_METHODS)) {
            run(player, "on", "Bob");
            util.verify(() -> Util.ChangeNametag(other, "&c"));
            util.verify(() -> Util.ChangeNametag(player, "&c"), never());
        }
    }

    @Test
    void chatUsesLiteralParametersAndIgnoresMissingMessages() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        plugin.config.set("MESSAGES.CUSTOM", "&a<parameter> <pvpstate>");
        Chat.send(console, "CUSTOM", "$1\\name", false);
        verify(console).sendMessage("\u00a7a$1\\name on");
        Chat.send(console, "MISSING");
        plugin.config.set("MESSAGES.EMPTY", "");
        Chat.send(console, "EMPTY");
        verifyNoMoreInteractions(console);
    }
}
