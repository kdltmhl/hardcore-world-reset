package com.github.kdltmhl.hardcoreworldreset;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public final class HardcoreWorldReset extends JavaPlugin {

    private MultiverseCore multiverse;
    private MVWorldManager worldManager;
    private String worldPrefix, activeWorldName, standbyWorldName;
    private int worldCounter;
    private String swapMethod;
    private BukkitTask timerTask;
    private long startTime = 0L, pausedTime = 0L;
    private boolean isTimerRunning = false, isSwapping = false;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        if (!getConfig().getBoolean("plugin-enabled", true) || !Bukkit.isHardcore()) {
            if (!Bukkit.isHardcore()) getLogger().severe("SERVER IS NOT IN HARDCORE MODE! Disabling plugin.");
            else getLogger().warning("Plugin is disabled via config.yml. Shutting down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        multiverse = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (multiverse == null) {
            getLogger().severe("Multiverse-Core not found! This plugin requires it to function. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("Multiverse-NetherPortals") == null) {
            getLogger().severe("Multiverse-NetherPortals not found! This is required for portal linking. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        worldManager = multiverse.getMVWorldManager();
        Bukkit.getScheduler().runTaskLater(this, this::initialize, 1L);
    }

    private void initialize() {
        loadConfigValues();
        getLogger().info("HardcoreWorldReset Initializing... Active world: " + activeWorldName);
        setupInitialWorlds();
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void loadConfigValues() {
        this.swapMethod = getConfig().getString("swap-method", "SEAMLESS").toUpperCase();

        this.worldPrefix = getConfig().getString("world-prefix", "hardcore_");
        String savedActiveWorld = getConfig().getString("state.active-world");
        if (savedActiveWorld == null || !savedActiveWorld.startsWith(this.worldPrefix)) {
            getLogger().warning("No valid state found or world-prefix has changed. Resetting state to use new prefix: '" + this.worldPrefix + "'");
            this.activeWorldName = this.worldPrefix + "1";
            this.standbyWorldName = this.worldPrefix + "2";
            this.worldCounter = 2;
            savePluginState();
        } else {
            this.activeWorldName = savedActiveWorld;
            this.standbyWorldName = getConfig().getString("state.standby-world");
            this.worldCounter = getConfig().getInt("state.world-counter");
        }
    }

    private void savePluginState() {
        getConfig().set("state.active-world", this.activeWorldName);
        getConfig().set("state.standby-world", this.standbyWorldName);
        getConfig().set("state.world-counter", this.worldCounter);
        saveConfig();
    }

    private void setupInitialWorlds() {
        if (worldManager.isMVWorld("world")) {
            worldManager.unloadWorld("world", false);
        }
        createWorldSet(activeWorldName);
        createWorldSet(standbyWorldName);
        multiverse.getMVWorldManager().setFirstSpawnWorld(activeWorldName);
        getLogger().info("Set first spawn world to: " + activeWorldName);
    }

    private void createWorldSet(String baseName) {
        if (!worldManager.isMVWorld(baseName)) {
            worldManager.addWorld(baseName, World.Environment.NORMAL, null, null, null, null, true);
            MultiverseWorld mvWorld = worldManager.getMVWorld(baseName);
            if (mvWorld != null) {
                mvWorld.setDifficulty(Difficulty.HARD);
                mvWorld.setGameMode(GameMode.SURVIVAL);
            }
        }
        if (!worldManager.isMVWorld(baseName + "_nether")) {
            worldManager.addWorld(baseName + "_nether", World.Environment.NETHER, null, null, null, null, true);
            MultiverseWorld mvWorld = worldManager.getMVWorld(baseName + "_nether");
            if (mvWorld != null) mvWorld.setDifficulty(Difficulty.HARD);
        }
        if (!worldManager.isMVWorld(baseName + "_the_end")) {
            worldManager.addWorld(baseName + "_the_end", World.Environment.THE_END, null, null, null, null, true);
            MultiverseWorld mvWorld = worldManager.getMVWorld(baseName + "_the_end");
            if (mvWorld != null) mvWorld.setDifficulty(Difficulty.HARD);
        }
        linkWorldSet(baseName);
        getLogger().info("Verified, configured, and linked world set: " + baseName);
    }

    private void linkWorldSet(String baseName) {
        Bukkit.getScheduler().runTask(this, () -> {
            String netherLinkCommand = "mvnp link " + baseName + " " + baseName + "_nether";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), netherLinkCommand);
            String endLinkCommand = "mv link " + baseName + "_the_end " + baseName;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), endLinkCommand);
            getLogger().info("Linking dimensions for " + baseName);
        });
    }

    public void triggerWorldSwap(Player deadPlayer, GameMode originalGameMode) {
        this.isSwapping = true;
        this.resetTimer();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Iterator<Advancement> iterator = Bukkit.getServer().advancementIterator();
            while (iterator.hasNext()) {
                AdvancementProgress progress = player.getAdvancementProgress(iterator.next());
                for (String criteria : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }
        }

        if ("SEAMLESS".equals(swapMethod)) {
            MultiverseWorld newWorld = worldManager.getMVWorld(standbyWorldName);
            if (newWorld != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.teleport(newWorld.getSpawnLocation());
                    String title = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.title-main", "&cPlayer Died"));
                    String subtitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.title-subtitle", "Welcome to the new world."));
                    player.sendTitle(title, subtitle, 10, 70, 20);
                }
            } else {
                getLogger().severe("Standby world '" + standbyWorldName + "' not found! Cannot swap.");
            }
        } else if ("DISCONNECT".equals(swapMethod)) {
            String kickReason = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.kick-reason", "&6A player has died! The world is resetting."));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(kickReason);
            }
        }

        if (deadPlayer != null && deadPlayer.isOnline()) {
            if (originalGameMode == GameMode.CREATIVE || originalGameMode == GameMode.SPECTATOR) {
                deadPlayer.setGameMode(originalGameMode);
            } else {
                deadPlayer.setGameMode(GameMode.SURVIVAL);
            }
        }

        this.startOrResumeTimer();

        String oldWorldBaseName = this.activeWorldName;
        String newStandbyBaseName = this.worldPrefix + (++worldCounter);
        this.activeWorldName = this.standbyWorldName;
        this.standbyWorldName = newStandbyBaseName;
        savePluginState();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            worldManager.deleteWorld(oldWorldBaseName, true, true);
            createWorldSet(newStandbyBaseName);
            this.isSwapping = false;
        }, 100L);
    }

    public boolean isSwapping() {
        return this.isSwapping;
    }

    public World getActiveWorld() {
        MultiverseWorld mvWorld = worldManager.getMVWorld(activeWorldName);
        return mvWorld != null ? mvWorld.getCBWorld() : null;
    }

    public void startOrResumeTimer() {
        if (isTimerRunning) return;
        startTime = System.currentTimeMillis() - pausedTime;
        pausedTime = 0L;
        isTimerRunning = true;
        timerTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            String formattedTime = formatTime(elapsedMillis);
            String footer = ChatColor.GOLD + "Time: " + formattedTime;
            for (Player player : Bukkit.getOnlinePlayers()) player.setPlayerListFooter(footer);
        }, 0L, 1L);
    }

    public void pauseTimer() {
        if (!isTimerRunning) return;
        if (timerTask != null) timerTask.cancel();
        pausedTime = System.currentTimeMillis() - startTime;
        isTimerRunning = false;
    }

    public String stopTimerAndAnnounce() {
        if (!isTimerRunning && pausedTime == 0L) return "";
        if (timerTask != null) timerTask.cancel();
        long finalMillis = isTimerRunning ? (System.currentTimeMillis() - startTime) : pausedTime;
        String formattedTime = formatTime(finalMillis);
        String footer = ChatColor.GREEN + "Final Time: " + formattedTime;
        for (Player player : Bukkit.getOnlinePlayers()) player.setPlayerListFooter(footer);
        isTimerRunning = false;
        pausedTime = 0L;
        return formattedTime;
    }

    public void resetTimer() {
        if (timerTask != null) timerTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) player.setPlayerListFooter(null);
        isTimerRunning = false;
        startTime = 0L;
        pausedTime = 0L;
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long hundreds = (millis / 10) % 100;
        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundreds);
    }
}