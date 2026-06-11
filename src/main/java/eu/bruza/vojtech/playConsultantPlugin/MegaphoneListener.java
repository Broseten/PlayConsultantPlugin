package eu.bruza.vojtech.playConsultantPlugin;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class MegaphoneListener implements Listener {
    private static final int MIN_WORD_COUNT = 1;
    private static final int CREATIVE_UNLOCK_COMMENT_COUNT = 2;
    private static final double MIN_COMMENT_DISTANCE = 3.0;

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
                && player.getLocation().distance(lastCommentLocation) < MIN_COMMENT_DISTANCE) {
            player.sendMessage(Component.text("You already commented near here! Keep exploring.", NamedTextColor.RED));
            return;
        }

        plugin.startTyping(playerId);
        player.sendMessage(Component.text("Type your comment in the chat (at least " + MIN_WORD_COUNT + " words):", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // If they aren't typing a comment, ignore the chat event
        if (!plugin.isTyping(playerId)) return;

        // They are typing a comment, so stop it from showing to everyone
        event.setCancelled(true);

        // Get the plain text they typed
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (message.isEmpty()) {
            player.sendMessage(Component.text("Please type a non-empty comment.", NamedTextColor.RED));
            return;
        }

        int wordCount = message.split("\\s+").length;
        if (wordCount < MIN_WORD_COUNT) {
            player.sendMessage(Component.text(
                    "That comment was only " + wordCount + " words. Please use at least " + MIN_WORD_COUNT + " words and try again!",
                    NamedTextColor.RED
            ));
            return;
        }

        // Continue on the main thread for world/entity/hologram operations.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location commentLocation = player.getLocation().clone();
            plugin.logComment(playerId, player.getName(), message, commentLocation);

            Location markerLocation = commentLocation.clone().add(0, 0.5, 0);
            Entity marker = player.getWorld().spawnEntity(markerLocation, EntityType.ALLAY);
            if (marker instanceof Allay allay) {
                allay.setAI(false);
                allay.setSilent(true);
            }
            marker.setInvulnerable(true);
            marker.setGravity(false);
            marker.setPersistent(true);

            String hologramName = "comment_" + playerId + "_" + System.currentTimeMillis();
            boolean hologramCreated = createHologram(
                    hologramName,
                    marker.getLocation().clone().add(0, 1.5, 0),
                    List.of(
                            "&e\"" + message + "\"",
                            "&7- Laptop " + player.getName()
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

            if (commentsMade >= CREATIVE_UNLOCK_COMMENT_COUNT && plugin.markCreativeKeyGranted(playerId)) {
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
            Hologram hologram = DHAPI.createHologram(name, location, true, lines);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create hologram: " + e.getMessage());
            return false;
        }
    }
}
