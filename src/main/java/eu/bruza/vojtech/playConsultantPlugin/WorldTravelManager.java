package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldTravelManager {
    private final PlayConsultantPlugin plugin;

    public WorldTravelManager(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    private String getAdventureWorldName() {
        return plugin.getConfigManager().getAdventureWorldName();
    }

    private String getBuildWorldName() {
        return plugin.getConfigManager().getBuildWorldName();
    }

    public boolean travelToAdventureWorld(Player player) {
        World adventureWorld = Bukkit.getWorld(getAdventureWorldName());
        if (adventureWorld == null) {
            player.sendMessage("§cThe adventure world (" + getAdventureWorldName() + ") could not be found.");
            return false;
        }

        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());

        Location targetLocation = playerData.getLastAdventureLocation();
        if (targetLocation == null || targetLocation.getWorld() == null || !targetLocation.getWorld().getName().equals(getAdventureWorldName())) {
            targetLocation = adventureWorld.getSpawnLocation();
        }

        boolean teleported = teleport(player, targetLocation, GameMode.ADVENTURE);
        if (teleported) {
            playerData.setCurrentZone(PlayerData.Zones.ADVENTURE);
            plugin.persistPlayerData();
            ensureHasKey(player);
        }
        return teleported;
    }

    public boolean travelToWarehouse(Player player) {
        World adventureWorld = Bukkit.getWorld(getAdventureWorldName());
        if (adventureWorld == null) {
            player.sendMessage("§cThe world containing the warehouse could not be found.");
            return false;
        }

        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());

        Location targetLocation = new Location(
                adventureWorld,
                plugin.getConfigManager().getWarehouseSpawnX(),
                plugin.getConfigManager().getWarehouseSpawnY(),
                plugin.getConfigManager().getWarehouseSpawnZ()
        );

        // Warehouse is an exploration zone, so we keep them in Adventure mode
        boolean teleported = teleport(player, targetLocation, GameMode.ADVENTURE);
        if (teleported) {
            playerData.setCurrentZone(PlayerData.Zones.WAREHOUSE);
            plugin.persistPlayerData();
            ensureHasKey(player);
        }
        return teleported;
    }

    public boolean travelToBuildWorld(Player player) {
        World buildWorld = Bukkit.getWorld(getBuildWorldName());
        if (buildWorld == null) {
            player.sendMessage("§cThe build world (" + getBuildWorldName() + ") could not be found.");
            return false;
        }

        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());

        Location targetLocation = playerData.getLastBuildLocation();
        boolean success;

        // Use your PlotManager logic if they don't have a saved location
        if (targetLocation == null || targetLocation.getWorld() == null || !targetLocation.getWorld().getName().equals(getBuildWorldName())) {
            success = plugin.getPlotManager().teleportToHome(player);
        } else {
            success = teleport(player, targetLocation, GameMode.CREATIVE);
        }

        if (success) {
            playerData.setCurrentZone(PlayerData.Zones.BUILD);
            plugin.persistPlayerData();
            ensureHasKey(player);
        }
        return success;
    }

    private void ensureHasKey(Player player) {
        if (!plugin.getItemManager().hasCreativeKey(player)) {
            plugin.getItemManager().giveCreativeKey(player);
        }
    }

    boolean teleport(Player player, Location targetLocation, GameMode gameMode) {
        player.setFallDistance(0.0f);
        boolean teleported = player.teleport(targetLocation);
        if (teleported) {
            player.setGameMode(gameMode);
        }
        return teleported;
    }
}