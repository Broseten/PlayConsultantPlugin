package eu.bruza.vojtech.playConsultantPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class NavigationTask extends BukkitRunnable {
    private final CheckpointManager checkpointManager;
    private final PlayConsultantPlugin plugin;

    public NavigationTask(CheckpointManager checkpointManager, PlayConsultantPlugin plugin) {
        this.checkpointManager = checkpointManager;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            int currentTargetId = plugin.getPlayerData(player.getUniqueId()).getNextTargetId();

            Location target = checkpointManager.getCheckpoint(currentTargetId);

            // If target is null, they finished the route or it doesn't exist
            if (target == null) continue;

            // 1. Update Vanilla Compass
            player.setCompassTarget(target);

            // 2. Check for Arrival (e.g., within 3 blocks)
            if (player.getLocation().getWorld() == target.getWorld() &&
                    player.getLocation().distance(target) <= 10.0) {

                plugin.getPlayerData(player.getUniqueId()).setNextTargetId(currentTargetId + 1);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendTitle("§aCheckpoint Reached!", "§7Updating navigation...", 10, 40, 10);
                continue; // Skip the action bar update this tick so they read the title
            }

            // 3. Update Action Bar
            String actionBarText = NavigationUtils.getDirectionText(player, target);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarText));
        }
    }
}
