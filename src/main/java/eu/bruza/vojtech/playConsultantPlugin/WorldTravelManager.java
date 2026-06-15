package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

public class WorldTravelManager {
    private static final String ADVENTURE_WORLD_NAME = "world";
    private static final String BUILD_WORLD_NAME = "build";

    private final PlayConsultantPlugin plugin;

    public WorldTravelManager(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    public void ensureBuildWorldLoaded() {
        getOrCreateBuildWorld();
    }

    public boolean isBuildWorld(World world) {
        return world != null && BUILD_WORLD_NAME.equalsIgnoreCase(world.getName());
    }

    public boolean toggleWorld(Player player) {
        return isBuildWorld(player.getWorld())
                ? travelToAdventureWorld(player)
                : travelToBuildWorld(player);
    }

    public boolean travelToAdventureWorld(Player player) {
        World adventureWorld = Bukkit.getWorld(ADVENTURE_WORLD_NAME);
        if (adventureWorld == null) {
            return false;
        }
        
        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());
        
        if (isBuildWorld(player.getWorld())) {
            playerData.setLastBuildLocation(player.getLocation());
            plugin.persistPlayerData();
        }

        Location targetLocation = playerData.getLastAdventureLocation();
        if (targetLocation == null || targetLocation.getWorld() == null || !targetLocation.getWorld().getName().equals(ADVENTURE_WORLD_NAME)) {
            targetLocation = adventureWorld.getSpawnLocation();
        }

        return teleport(player, targetLocation, GameMode.ADVENTURE);
    }

    public boolean travelToBuildWorld(Player player) {
        World buildWorld = getOrCreateBuildWorld();
        if (buildWorld == null) {
            return false;
        }

        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());

        if (player.getWorld().getName().equals(ADVENTURE_WORLD_NAME)) {
            playerData.setLastAdventureLocation(player.getLocation());
            plugin.persistPlayerData();
        }

        Location targetLocation = playerData.getLastBuildLocation();
        if (targetLocation == null || targetLocation.getWorld() == null || !targetLocation.getWorld().getName().equals(BUILD_WORLD_NAME)) {
            targetLocation = buildWorld.getSpawnLocation();
        }

        boolean success = teleport(player, targetLocation, GameMode.CREATIVE);
        if (success && playerData.hasReceivedCreativeKey()) {
            plugin.getItemManager().giveCreativeKey(player);
        }
        return success;
    }

    private World getOrCreateBuildWorld() {
        World buildWorld = Bukkit.getWorld(BUILD_WORLD_NAME);
        if (buildWorld != null) {
            return buildWorld;
        }

        return Bukkit.createWorld(new WorldCreator(BUILD_WORLD_NAME));
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