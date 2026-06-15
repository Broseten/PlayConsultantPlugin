package eu.bruza.vojtech.playConsultantPlugin;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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
    
    private static class ConfirmationData {
        final long timestamp;
        final UUID targetEntityId;

        ConfirmationData(long timestamp, UUID targetEntityId) {
            this.timestamp = timestamp;
            this.targetEntityId = targetEntityId;
        }
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

        // Run on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            double searchRadius = plugin.getConfigManager().getRemoveCommentSearchRadius();
            Entity nearestEntity = null;
            double bestDistSq = Double.MAX_VALUE;

            for (Entity entity : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
                if (entity instanceof Player) continue;
                if (entity.getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
                    // Distance check for entity
                    double distSq = entity.getLocation().distanceSquared(player.getLocation());
                    if (distSq < bestDistSq && distSq <= searchRadius * searchRadius) {
                        bestDistSq = distSq;
                        nearestEntity = entity;
                    }
                }
            }

            UUID playerId = player.getUniqueId();

            if (nearestEntity == null) {
                player.sendMessage(Component.text("No comment marker found within " + searchRadius + " blocks.", NamedTextColor.RED));
                pendingConfirmations.remove(playerId);
                return;
            }

            ConfirmationData lastRequest = pendingConfirmations.get(playerId);
            if (lastRequest != null 
                    && System.currentTimeMillis() - lastRequest.timestamp < CONFIRMATION_TIME_MS
                    && nearestEntity.getUniqueId().equals(lastRequest.targetEntityId)) {
                // Confirmed for the exact same nearest entity
                pendingConfirmations.remove(playerId);
            } else {
                // First time or target changed / expired
                pendingConfirmations.put(playerId, new ConfirmationData(System.currentTimeMillis(), nearestEntity.getUniqueId()));
                player.sendMessage(Component.text("Are you sure you want to remove the nearest comment? Type the command again within 10 seconds to confirm.", NamedTextColor.GOLD));
                return;
            }

            Location entityLocation = nearestEntity.getLocation();
            String hologramName = nearestEntity.getPersistentDataContainer().get(plugin.getHologramNameKey(), PersistentDataType.STRING);

            boolean hologRemoved = false;
            // 1. Try to remove by stored name
            if (hologramName != null && !hologramName.isBlank()) {
                try {
                    Hologram hologram = DHAPI.getHologram(hologramName);
                    if (hologram != null) {
                        hologram.delete();
                        hologRemoved = true;
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed to delete hologram '" + hologramName + "' by name: " + t.getMessage());
                }
            }

            // 2. If not removed, search for the nearest hologram by location
            if (!hologRemoved) {
                final double hologramSearchRadius = 4.0;
                Hologram nearestHologram = null;
                double bestHologramDistSq = Double.MAX_VALUE;

                try {
                    for (Hologram hologram : DecentHologramsAPI.get().getHologramManager().getHolograms()) {
                        if (!hologram.getLocation().getWorld().equals(entityLocation.getWorld())) {
                            continue;
                        }
                        // Strict distance check for hologram relative to the entity's location
                        double distSq = hologram.getLocation().distanceSquared(entityLocation);
                        if (distSq < bestHologramDistSq && distSq <= hologramSearchRadius * hologramSearchRadius) {
                            bestHologramDistSq = distSq;
                            nearestHologram = hologram;
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed to search for nearby holograms: " + t.getMessage());
                }


                if (nearestHologram != null) {
                    try {
                        nearestHologram.delete();
                        hologRemoved = true;
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Failed to delete nearest hologram: " + t.getMessage());
                    }
                }
            }

            // 3. Remove the entity itself
            nearestEntity.remove();

            player.sendMessage(Component.text("Removed comment." + (hologRemoved ? " Hologram removed." : " Hologram not found or could not be removed."), NamedTextColor.GREEN));
        });

        return true;
    }
}