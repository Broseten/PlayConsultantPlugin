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

    public void ensureBuildWorldLoaded() {
        // Nothing to do if we don't want to auto-create
        // PlotSquared or Multiverse should handle world loading
    }

    public boolean isBuildWorld(World world) {
        return world != null && getBuildWorldName().equalsIgnoreCase(world.getName());
    }

    public boolean toggleWorld(Player player) {
        return isBuildWorld(player.getWorld())
                ? travelToAdventureWorld(player)
                : travelToBuildWorld(player);
    }

    public boolean travelToAdventureWorld(Player player) {
        World adventureWorld = Bukkit.getWorld(getAdventureWorldName());
        if (adventureWorld == null) {
            player.sendMessage("§cThe adventure world (" + getAdventureWorldName() + ") could not be found.");
            return false;
        }
        
        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());
        
        if (isBuildWorld(player.getWorld())) {
            playerData.setLastBuildLocation(player.getLocation());
            plugin.persistPlayerData();
        }

        Location targetLocation = playerData.getLastAdventureLocation();
        if (targetLocation == null || targetLocation.getWorld() == null || !targetLocation.getWorld().getName().equals(getAdventureWorldName())) {
            targetLocation = adventureWorld.getSpawnLocation();
        }

        return teleport(player, targetLocation, GameMode.ADVENTURE);
    }

    public boolean travelToBuildWorld(Player player) {
        World buildWorld = Bukkit.getWorld(getBuildWorldName());
        if (buildWorld == null) {
            player.sendMessage("§cThe build world (" + getBuildWorldName() + ") could not be found.");
            return false;
        }

        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());

        if (player.getWorld().getName().equals(getAdventureWorldName())) {
            playerData.setLastAdventureLocation(player.getLocation());
            plugin.persistPlayerData();
        }

        Location targetLocation = playerData.getLastBuildLocation();
        if (targetLocation == null || targetLocation.getWorld() == null || !targetLocation.getWorld().getName().equals(getBuildWorldName())) {
            targetLocation = buildWorld.getSpawnLocation();
        }

        boolean success = teleport(player, targetLocation, GameMode.CREATIVE);
        if (success && playerData.hasReceivedCreativeKey()) {
            plugin.getItemManager().giveCreativeKey(player);
        }
        return success;
    }

    private boolean teleport(Player player, Location targetLocation, GameMode gameMode) {
        player.setFallDistance(0.0f);
        boolean teleported = player.teleport(targetLocation);
        if (teleported) {
            player.setGameMode(gameMode);
        }
        return teleported;
    }
}