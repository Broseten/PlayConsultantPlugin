package eu.bruza.vojtech.playConsultantPlugin;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemoveCommentCommand implements CommandExecutor {
    private final PlayConsultantPlugin plugin;

    private enum TargetType {
        ENTITY,
        HOLOGRAM
    }

    private record ConfirmationData(long timestamp, String targetId, TargetType targetType) {
    }

    private final Map<UUID, ConfirmationData> pendingConfirmations = new HashMap<>();
    private static final long CONFIRMATION_TIME_MS = 10000;

    public RemoveCommentCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        if (!player.hasPermission("playconsultant.removecomment") && !player.isOp()) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Find nearest comment entity
            Entity nearestEntity = null;
            double bestEntityDistSq = Double.MAX_VALUE;
            for (Entity entity : player.getWorld().getEntities()) {
                if (entity.getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
                    double distSq = entity.getLocation().distanceSquared(player.getLocation());
                    if (distSq < bestEntityDistSq) {
                        bestEntityDistSq = distSq;
                        nearestEntity = entity;
                    }
                }
            }

            // Find nearest orphaned hologram
            Hologram nearestOrphanedHologram = null;
            double bestHologramDistSq = Double.MAX_VALUE;
            for (Hologram hologram : DecentHologramsAPI.get().getHologramManager().getHolograms()) {
                if (hologram.getName().startsWith("comment_") && player.getWorld().equals(hologram.getLocation().getWorld())) {
                    boolean hasEntity = false;
                    for (Entity entity : hologram.getLocation().getNearbyEntities(1, 1, 1)) {
                        String hologramName = entity.getPersistentDataContainer().get(plugin.getHologramNameKey(), PersistentDataType.STRING);
                        if (hologram.getName().equals(hologramName)) {
                            hasEntity = true;
                            break;
                        }
                    }
                    if (!hasEntity) {
                        double distSq = hologram.getLocation().distanceSquared(player.getLocation());
                        if (distSq < bestHologramDistSq) {
                            bestHologramDistSq = distSq;
                            nearestOrphanedHologram = hologram;
                        }
                    }
                }
            }

            Object target = null;
            TargetType targetType = null;
            String targetId = null;

            if (nearestEntity != null && (nearestOrphanedHologram == null || bestEntityDistSq <= bestHologramDistSq)) {
                target = nearestEntity;
                targetType = TargetType.ENTITY;
                targetId = nearestEntity.getUniqueId().toString();
            } else if (nearestOrphanedHologram != null) {
                target = nearestOrphanedHologram;
                targetType = TargetType.HOLOGRAM;
                targetId = nearestOrphanedHologram.getName();
            }

            if (target == null) {
                player.sendMessage(Component.text("No comments found in this world.", NamedTextColor.RED));
                return;
            }

            UUID playerId = player.getUniqueId();
            ConfirmationData lastRequest = pendingConfirmations.get(playerId);

            if (lastRequest != null && System.currentTimeMillis() - lastRequest.timestamp < CONFIRMATION_TIME_MS && targetId.equals(lastRequest.targetId)) {
                pendingConfirmations.remove(playerId);
            } else {
                pendingConfirmations.put(playerId, new ConfirmationData(System.currentTimeMillis(), targetId, targetType));
                player.sendMessage(Component.text("Are you sure you want to remove the nearest comment? Type the command again within 10 seconds to confirm.", NamedTextColor.GOLD));
                return;
            }

            if (target instanceof Entity entity) {
                String hologramName = entity.getPersistentDataContainer().get(plugin.getHologramNameKey(), PersistentDataType.STRING);
                boolean hologRemoved = false;
                if (hologramName != null) {
                    Hologram hologram = DHAPI.getHologram(hologramName);
                    if (hologram != null) {
                        hologram.delete();
                        hologRemoved = true;
                    }
                }
                entity.remove();
                player.sendMessage(Component.text("Removed comment entity." + (hologRemoved ? " Hologram removed." : ""), NamedTextColor.GREEN));
            } else {
                Hologram hologram = (Hologram) target;
                hologram.delete();
                player.sendMessage(Component.text("Removed orphaned comment hologram.", NamedTextColor.GREEN));
            }
        });

        return true;
    }
}