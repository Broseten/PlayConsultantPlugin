package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class NavigationUtils {

    public static void startNavigation(Player player, PlayConsultantPlugin plugin) {
        plugin.getPlayerData(player.getUniqueId()).setNextTargetId(0);
    }

    public static void stopNavigation(Player player, PlayConsultantPlugin plugin) {
        plugin.getPlayerData(player.getUniqueId()).setNextTargetId(-1);
    }

    public static String getDirectionText(Player player, Location target) {
        Location pLoc = player.getLocation();

        // Ensure we are in the same world before doing math
        if (pLoc.getWorld() != target.getWorld()) {
            return "§cWrong World!";
        }

        double dX = target.getX() - pLoc.getX();
        double dZ = target.getZ() - pLoc.getZ();

        // Calculate the absolute angle to the target
        double angleToTarget = (Math.toDegrees(Math.atan2(dZ, dX)) - 90);
        double playerYaw = pLoc.getYaw();

        // Calculate relative difference and normalize between -180 and 180
        double difference = (angleToTarget - playerYaw) % 360;
        if (difference < -180) difference += 360;
        if (difference > 180) difference -= 360;

        int distance = (int) pLoc.distance(target);

        // Granular Pointing Logic
        if (Math.abs(difference) <= 15) {
            return "§a▲ " + distance + "m ▲"; // Straight Ahead (Green)

        } else if (Math.abs(difference) > 135) {
            return "§c▼ " + distance + "m ▼"; // Behind You (Red)

        } else if (difference < 0) {
            // Target is to the LEFT
            if (difference >= -45) {
                return "§e< " + distance + "m"; // Slight Left (Yellow)
            } else {
                return "§6<<< " + distance + "m"; // Hard Left (Gold)
            }

        } else {
            // Target is to the RIGHT
            if (difference <= 45) {
                return "§e" + distance + "m >"; // Slight Right (Yellow)
            } else {
                return "§6" + distance + "m >>>"; // Hard Right (Gold)
            }
        }
    }
}
