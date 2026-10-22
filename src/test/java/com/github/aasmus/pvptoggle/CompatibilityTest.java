package com.github.aasmus.pvptoggle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.aasmus.pvptoggle.listeners.PvP;
import com.github.aasmus.pvptoggle.utils.Util;

class CompatibilityTest {
    private PvPToggle plugin;
    private Player attacker;
    private Player victim;
    private UUID attackerId;
    private UUID victimId;
    private World world;

    @BeforeEach
    void setUp() {
        plugin = mock(PvPToggle.class);
        PvPToggle.instance = plugin;
        plugin.players = new HashMap<>();
        plugin.cooldowns = new HashMap<>();
        PvPToggle.blockedWorlds = List.of();
        YamlConfiguration config = new YamlConfiguration();
        config.set("MESSAGES.PVP_DISABLED", "disabled");
        config.set("MESSAGES.PVP_DISABLED_OTHERS", "disabled for <parameter>");
        when(plugin.getConfig()).thenReturn(config);

        world = mock(World.class);
        when(world.getName()).thenReturn("arena");
        attacker = player("Attacker");
        victim = player("Victim");
        attackerId = attacker.getUniqueId();
        victimId = victim.getUniqueId();
    }

    @AfterEach
    void tearDown() {
        PvPToggle.instance = null;
        PvPToggle.blockedWorlds = null;
        TestNametagPlugin.api = null;
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.getDisplayName()).thenReturn(name);
        return player;
    }

    private EntityDamageByEntityEvent melee(Entity damager, Player target) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        when(event.getEntity()).thenReturn(target);
        return event;
    }

    @Test
    void meleeBlocksWhenEitherPlayersPvpIsDisabled() {
        PvP listener = new PvP();
        plugin.players.put(attackerId, true);
        plugin.players.put(victimId, false);
        EntityDamageByEntityEvent attackerDisabled = melee(attacker, victim);

        listener.onHit(attackerDisabled);

        verify(attackerDisabled).setCancelled(true);
        assertTrue(plugin.cooldowns.isEmpty());

        plugin.players.put(attackerId, false);
        plugin.players.put(victimId, true);
        EntityDamageByEntityEvent victimDisabled = melee(attacker, victim);

        listener.onHit(victimDisabled);

        verify(victimDisabled).setCancelled(true);
        assertTrue(plugin.cooldowns.isEmpty());
    }

    @Test
    void meleeAllowsBothEnabledPlayersAndStartsBothCooldowns() {
        plugin.players.put(attackerId, false);
        plugin.players.put(victimId, false);
        EntityDamageByEntityEvent event = melee(attacker, victim);

        new PvP().onHit(event);

        verify(event, never()).setCancelled(true);
        assertTrue(plugin.cooldowns.containsKey(attackerId));
        assertTrue(plugin.cooldowns.containsKey(victimId));
    }

    @Test
    void projectileIsBlockedWhenVictimHasPvpDisabled() {
        plugin.players.put(attackerId, false);
        plugin.players.put(victimId, true);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(attacker);
        EntityDamageByEntityEvent event = melee(arrow, victim);

        new PvP().onHit(event);

        verify(event).setCancelled(true);
        assertTrue(plugin.cooldowns.isEmpty());
    }

    @Test
    void missingOrDisabledNametagEditIsSkipped() {
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        when(manager.getPlugin("NametagEdit")).thenReturn(null);

        assertDoesNotThrow(() -> Util.ChangeNametag(victim, "&a"));

        TestNametagPlugin nametag = mock(TestNametagPlugin.class);
        when(manager.getPlugin("NametagEdit")).thenReturn(nametag);
        when(nametag.isEnabled()).thenReturn(false);
        assertDoesNotThrow(() -> Util.ChangeNametag(victim, "&a"));
    }

    @Test
    void installedNametagApiReceivesPrefixAndResetCalls() {
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        TestNametagPlugin nametag = mock(TestNametagPlugin.class);
        when(nametag.isEnabled()).thenReturn(true);
        when(manager.getPlugin("NametagEdit")).thenReturn(nametag);
        com.nametagedit.plugin.api.INametagApi api = mock(com.nametagedit.plugin.api.INametagApi.class);
        TestNametagPlugin.api = api;

        Util.ChangeNametag(victim, "&aVIP");
        Util.ChangeNametag(victim, "reset");

        verify(api).setPrefix(victim, "&aVIP");
        verify(api).clearNametag(victim);
    }

    @Test
    void dustParticlesRenderAndStopAfterPlayerStateIsRemoved() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(task.getTaskId()).thenReturn(7);
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTaskTimer(eq(plugin), runnable.capture(), eq(0L), eq(2L))).thenReturn(task);
        when(victim.isOnline()).thenReturn(true);
        when(victim.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        plugin.players.put(victimId, false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            Util.particleEffect(victim);
            runnable.getValue().run();
            verify(world, times(25)).spawnParticle(eq(Particle.DUST), anyDouble(), eq(64.0),
                    anyDouble(), eq(0), eq(0.0), eq(1.0), eq(0.0), any(Particle.DustOptions.class));

            plugin.players.remove(victimId);
            assertDoesNotThrow(() -> runnable.getValue().run());
            verify(scheduler).cancelTask(7);
        }
    }
}
