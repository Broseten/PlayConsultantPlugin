package eu.bruza.vojtech.playConsultantPlugin;

import eu.decentsoftware.holograms.api.DHAPI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class MegaphoneListener implements Listener {
    private final PlayConsultantPlugin plugin;

    public MegaphoneListener(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;
        if (!plugin.getItemManager().isMegaphone(item)) return;

        event.setCancelled(true); // Stop default carrot-on-a-stick usage/interaction

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        UUID playerId = player.getUniqueId();
        PlayerData data = plugin.getOrCreatePlayerData(playerId);

        if (data.isTypingComment()) {
            player.sendMessage(Component.text("You are already typing a comment!", NamedTextColor.RED));
            return;
        }

        Location lastCommentLocation = data.getLastCommentLocation();
        if (lastCommentLocation != null
                && lastCommentLocation.getWorld() != null
                && lastCommentLocation.getWorld().equals(player.getWorld())
                && player.getLocation().distance(lastCommentLocation) < plugin.getConfigManager().getMinCommentDistance()) {
            player.sendMessage(Component.text("You already commented near here! Keep exploring.", NamedTextColor.RED));
            return;
        }

        plugin.startTyping(playerId);
        player.sendMessage(Component.text(
                "Type your comment in the chat (at least " + plugin.getConfigManager().getMinWordCount() + " words):",
                NamedTextColor.GREEN
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
            return;
        }

        // Prevent taking items from allays, milking cows, mounting horses, etc.
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!event.getRightClicked().getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // If they aren't typing a comment, ignore the chat event
        if (!plugin.isTyping(playerId)) return;

        // Has another plugin (like a chat filter or mute system) canceled this event already?
        if (event.isCancelled()) {
            // The filter blocked it. Stop the typing state and abort the hologram.
            // The filter likely already sent the player a warning, so we stay silent.
            plugin.stopTyping(playerId);
            return;
        }

        // They are typing a comment, so stop it from showing to everyone
        event.setCancelled(true);

        // Get the plain text they typed
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (message.isEmpty()) {
            player.sendMessage(Component.text("Please type a non-empty comment.", NamedTextColor.RED));
            return;
        }

        int wordCount = message.split("\\s+").length;
        int minWordCount = plugin.getConfigManager().getMinWordCount();
        if (wordCount < minWordCount) {
            player.sendMessage(Component.text(
                    "That comment was only " + wordCount + " words. Please use at least " + minWordCount + " words and try again!",
                    NamedTextColor.RED
            ));
            return;
        }

        // Continue on the main thread for world/entity/hologram operations.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location commentLocation = player.getLocation().clone();
            plugin.logComment(playerId, player.getName(), message, commentLocation);

            // pick a mob from config (weighted)
            PlayConsultantConfigManager.MobSpawnEntry spawn = plugin.getConfigManager().pickRandomMobSpawn();
            Location markerLocation = commentLocation.clone().add(0, spawn.yOffset, 0);
            Entity marker = player.getWorld().spawnEntity(markerLocation, spawn.type);

            // disable AI for mobs that support it (most mob types implement org.bukkit.entity.Mob)
            try {
                if (marker instanceof Mob mobEntity) {
                    mobEntity.setAI(false);
                }
            } catch (NoClassDefFoundError | Exception ex) {
                // ignore if API differences exist
            }

            if (marker instanceof Allay allay) {
                try {
                    allay.setCanPickupItems(false);
                } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                }
            }

//            if (marker instanceof LivingEntity livingEntity) {
//                // periodically look at players
//                new BukkitRunnable() {
//                    @Override
//                    public void run() {
//                        if (!livingEntity.isValid() || livingEntity.isDead()) {
//                            cancel();
//                            return;
//                        }
//
//                        Player nearest = null;
//                        double nearestDistance = 12.0 * 12.0;
//                        for (Player nearby : livingEntity.getWorld().getPlayers()) {
//                            double distance = nearby.getLocation().distanceSquared(livingEntity.getLocation());
//                            if (distance <= nearestDistance) {
//                                nearestDistance = distance;
//                                nearest = nearby;
//                            }
//                        }
//
//                        if (nearest != null) {
//                            Location lookLocation = livingEntity.getLocation().clone();
//                            lookLocation.setDirection(
//                                    nearest.getEyeLocation().toVector().subtract(livingEntity.getEyeLocation().toVector())
//                            );
//                            livingEntity.teleport(lookLocation);
//                        }
//                    }
//                }.runTaskTimer(plugin, 0L, 10L);
//            }

            // Make silent if possible
            try {
                marker.setSilent(true);
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            }

            marker.setInvulnerable(true);
            // respect gravity setting from config (true = has gravity)
            try {
                marker.setGravity(spawn.gravity);
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            }
            marker.setPersistent(true);

            String hologramName = "comment_" + playerId + "_" + System.currentTimeMillis();
            // Tag the entity with a marker key and store hologram name so admins can remove it later
            try {
                marker.getPersistentDataContainer().set(
                        plugin.getCommentMarkerKey(),
                        PersistentDataType.BYTE,
                        (byte) 1
                );
                marker.getPersistentDataContainer().set(
                        plugin.getHologramNameKey(),
                        PersistentDataType.STRING,
                        hologramName
                );
            } catch (Exception ignored) {
                // If PDC fails for any reason, continue silently (hologram still created)
            }
            boolean hologramCreated = createHologram(
                    hologramName,
                    marker.getLocation().clone().add(0, 1.5, 0),
                    List.of(
                            "&e\"" + message + "\"",
                            "&7- " + player.getName()
                    )
            );

            if (!hologramCreated) {
                player.sendMessage(Component.text(
                        "Comment was captured, but hologram creation failed. Contact staff.",
                        NamedTextColor.RED
                ));
            }

            int commentsMade = plugin.incrementAndGetComments(playerId);
            plugin.stopTyping(playerId);

            // Store the last comment location
            PlayerData playerData = plugin.getPlayerData(playerId);
            if (playerData != null) {
                playerData.setLastCommentLocation(marker.getLocation());
            }

            player.sendMessage(Component.text("Comment saved! Total comments: " + commentsMade, NamedTextColor.GREEN));

            if (commentsMade >= plugin.getConfigManager().getCreativeUnlockCommentCount() && plugin.markCreativeKeyGranted(playerId)) {
                if (!plugin.getItemManager().hasCreativeKey(player)) {
                    plugin.getItemManager().giveCreativeKey(player);
                }
                player.sendMessage(Component.text(
                        "You've unlocked the Build World! Right-click your enchanted key to travel.",
                        NamedTextColor.GOLD
                ));
            }
        });
    }

    private boolean createHologram(String name, Location location, List<String> lines) {
        try {
            // Use directly the dependency on DecentHolograms instead of using reflection
            DHAPI.createHologram(name, location, true, lines);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create hologram: " + e.getMessage());
            return false;
        }
    }
}
