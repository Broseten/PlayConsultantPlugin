package eu.bruza.vojtech.playConsultantPlugin;

import eu.decentsoftware.holograms.api.DHAPI;
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

public class RemoveCommentCommand implements CommandExecutor {
    private final PlayConsultantPlugin plugin;
    private static final double SEARCH_RADIUS = 6.0;

    public RemoveCommentCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
            Entity nearest = null;
            double best = Double.MAX_VALUE;

            for (Entity e : player.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
                if (e == null) continue;
                // skip players
                if (e instanceof Player) continue;
                try {
                    if (e.getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
                        double dist = e.getLocation().distance(player.getLocation());
                        if (dist < best) {
                            best = dist;
                            nearest = e;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (nearest == null) {
                player.sendMessage(Component.text("No comment marker found within " + (int) SEARCH_RADIUS + " blocks.", NamedTextColor.RED));
                return;
            }

            String hologramName = null;
            try {
                if (nearest.getPersistentDataContainer().has(plugin.getHologramNameKey(), PersistentDataType.STRING)) {
                    hologramName = nearest.getPersistentDataContainer().get(plugin.getHologramNameKey(), PersistentDataType.STRING);
                }
            } catch (Exception ignored) {
            }

            // Remember location for fallback checks
            // Remove entity
            try {
                nearest.remove();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to remove comment entity: " + ex.getMessage());
            }

            boolean hologRemoved = false;
            if (hologramName != null && !hologramName.isBlank()) {
                try {
                    Hologram hologram = DHAPI.getHologram(hologramName);
                    if (hologram != null) {
                        // Try to delete hologram via API
                        try {
                            hologram.delete();
                            hologRemoved = true;
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Failed to delete hologram via API: " + t.getMessage());
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed to remove hologram '" + hologramName + "': " + t.getMessage());
                }
            }

            if (!hologRemoved) {
                // As a fallback, attempt to delete any hologram with this name prefix (defensive)
                try {
                    Hologram hologram = DHAPI.getHologram(hologramName == null ? "" : hologramName);
                    if (hologram != null) {
                        try { hologram.delete(); hologRemoved = true; } catch (Throwable ignored) { }
                    }
                } catch (Throwable ignored) {
                }
            }

            player.sendMessage(Component.text("Removed comment." + (hologRemoved ? " Hologram removed." : " Hologram not found."), NamedTextColor.GREEN));
        });

        return true;
    }
}


