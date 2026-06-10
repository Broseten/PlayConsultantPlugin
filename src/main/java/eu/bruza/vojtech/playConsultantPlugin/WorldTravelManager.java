package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

public class WorldTravelManager {
    private static final String ADVENTURE_WORLD_NAME = "world";
    private static final String BUILD_WORLD_NAME = "build";

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

        return teleport(player, adventureWorld, GameMode.ADVENTURE);
    }

    public boolean travelToBuildWorld(Player player) {
        World buildWorld = getOrCreateBuildWorld();
        if (buildWorld == null) {
            return false;
        }

        return teleport(player, buildWorld, GameMode.CREATIVE);
    }

    private World getOrCreateBuildWorld() {
        World buildWorld = Bukkit.getWorld(BUILD_WORLD_NAME);
        if (buildWorld != null) {
            return buildWorld;
        }

        return Bukkit.createWorld(new WorldCreator(BUILD_WORLD_NAME));
    }

    private boolean teleport(Player player, World world, GameMode gameMode) {
        player.setFallDistance(0.0f);
        boolean teleported = player.teleport(world.getSpawnLocation());
        if (teleported) {
            player.setGameMode(gameMode);
        }
        return teleported;
    }
}


