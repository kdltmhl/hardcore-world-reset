package com.github.kdltmhl.hardcoreworldreset;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final HardcoreWorldReset plugin;

    public PlayerListener(HardcoreWorldReset plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World activeWorld = plugin.getActiveWorld();

        if (activeWorld != null) {
            player.setBedSpawnLocation(activeWorld.getSpawnLocation(), true);

            if (!player.getWorld().getName().startsWith(activeWorld.getName())) {
                player.teleport(activeWorld.getSpawnLocation());
            }
        }

        if (Bukkit.getOnlinePlayers().size() == 1 && !plugin.isSwapping()) {
            plugin.startOrResumeTimer();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World activeWorld = plugin.getActiveWorld();

        if (player.getWorld().getEnvironment() == World.Environment.THE_END && activeWorld != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(activeWorld.getSpawnLocation());
            }, 1L); // 1-tick delay to ensure the respawn process completes first.
            return;
        }

        if (activeWorld != null) {
            event.setRespawnLocation(activeWorld.getSpawnLocation());
        }
    }


    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        World activeWorld = plugin.getActiveWorld();
        if (activeWorld == null || !event.getEntity().getWorld().getName().equals(activeWorld.getName() + "_the_end")) {
            return;
        }

        // The timer now stops reliably when the dragon dies.
        String finalTime = plugin.stopTimerAndAnnounce();
        String messageTemplate = plugin.getConfig().getString("messages.dragon-defeat", "&aThe Ender Dragon has been defeated! &fFinal Time: &e%time%");
        String finalMessage = messageTemplate.replace("%time%", finalTime);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', finalMessage));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player deadPlayer = event.getEntity();
        World activeWorld = plugin.getActiveWorld();

        if (activeWorld == null || !deadPlayer.getWorld().getName().startsWith(activeWorld.getName())) {
            return;
        }

        final GameMode originalGameMode = deadPlayer.getGameMode();
        event.getDrops().clear();
        event.setDroppedExp(0);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            deadPlayer.spigot().respawn();
            plugin.triggerWorldSwap(deadPlayer, originalGameMode);
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                plugin.pauseTimer();
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (plugin.isSwapping()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.RED + "The world is currently resetting. Please try again in a moment."
            );
        }
    }
}