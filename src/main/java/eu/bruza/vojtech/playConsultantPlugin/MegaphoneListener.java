package eu.bruza.vojtech.playConsultantPlugin;

import eu.decentsoftware.holograms.api.DHAPI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MegaphoneListener implements Listener {
    private final PlayConsultantPlugin plugin;

    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public MegaphoneListener(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Wait a brief moment to ensure player data is fully loaded into your Map
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerData data = plugin.getPlayerData(player.getUniqueId());
            if (data != null) {
                int target = plugin.getConfigManager().getCreativeUnlockCommentCount();

                // Show them their current BossBar progress immediately when they log in!
                updateBossBar(player, data.getCommentsMade(), target);
            }
        }, 20L); // 20 ticks = 1 second delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Prevent memory leaks by removing the BossBar from the map when they leave
        BossBar oldBar = activeBossBars.remove(event.getPlayer().getUniqueId());

        // Hide the bar (necessary so the server stops sending packets to a disconnected client)
        if (oldBar != null) {
            event.getPlayer().hideBossBar(oldBar);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());

        if (data != null) {
            int target = plugin.getConfigManager().getCreativeUnlockCommentCount();

            // This will automatically show the bar if they entered the correct world,
            // or hide it if they left the correct world.
            updateBossBar(player, data.getCommentsMade(), target);
        }
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

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
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
            Entity marker = addCommentMarker(player, playerId, message);

            // Store the last comment location
            PlayerData playerData = plugin.getPlayerData(playerId);
            if (playerData != null) {
                playerData.setLastCommentLocation(marker.getLocation());
                plugin.persistPlayerData();
            }

            updateCommentsMade(playerId, player);

            plugin.stopTyping(playerId);
        });
    }

    private @NonNull Entity addCommentMarker(Player player, UUID playerId, String message) {
        Location commentLocation = player.getLocation().clone();
        plugin.logComment(playerId, player.getName(), message, commentLocation);

        // pick a mob from config (weighted)
        PlayConsultantConfigManager.MobSpawnEntry spawn = plugin.getConfigManager().pickRandomMobSpawn();
        Location markerLocation = commentLocation.clone().add(0, spawn.yOffset, 0);
        Entity marker = player.getWorld().spawnEntity(markerLocation, spawn.type);

        if (marker instanceof Lootable) {
            Lootable lootable = (Lootable) marker;
            // Force it to use the vanilla empty loot table
            lootable.setLootTable(Bukkit.getLootTable(NamespacedKey.minecraft("empty")));
        }

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
                marker.getLocation().clone().add(0, marker.getHeight() + 0.25, 0),
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
        return marker;
    }

    private void updateCommentsMade(UUID playerId, Player player) {
        int commentsMade = plugin.incrementAndGetComments(playerId);
        int targetComments = plugin.getConfigManager().getCreativeUnlockCommentCount();

        // 1. Send the instant Action Bar confirmation
        player.sendActionBar(Component.text("Comment saved!", NamedTextColor.GREEN));

        // 2. Update the persistent BossBar
        updateBossBar(player, commentsMade, targetComments);

        // 5. Keep the instant action bar feedback so they know the command worked
        player.sendActionBar(Component.text("Comment saved!", NamedTextColor.GREEN));

        // 3. Grant rewards if applicable
        if (commentsMade >= targetComments && plugin.markCreativeKeyGranted(playerId)) {
            plugin.getPlotManager().rewardPlayerWithCreativePlot(player);

            player.sendMessage(Component.text(
                    "You've unlocked the Build World! Your plot is being prepared...",
                    NamedTextColor.GOLD
            ));
        }
    }

    public void updateBossBar(Player player, int commentsMade, int target) {
        // 1. WORLD CHECK
        if (!player.getWorld().getName().equals(plugin.getConfigManager().getAdventureWorldName())) {
            BossBar existingBar = activeBossBars.remove(player.getUniqueId());
            if (existingBar != null) {
                player.hideBossBar(existingBar); // Hide it if they left the world
            }
            return; // Stop running the rest of the method
        }

        // 2. Fetch Leaderboard Data safely
        List<PlayerData> top = plugin.getTopCommenters(1);
        String top1Name = "Unknown";
        int top1Value = 0;

        if (!top.isEmpty()) {
            top1Name = Bukkit.getOfflinePlayer(top.getFirst().getUuid()).getName();
            if (top1Name == null) top1Name = "Unknown";
            top1Value = top.getFirst().getCommentsMade();
        }

        // 3. Calculate Progress & Status
        boolean hasUnlocked = commentsMade >= target;
        float progress = Math.min(1.0f, (float) commentsMade / target);
        BossBar.Color barColor = hasUnlocked ? BossBar.Color.PURPLE : BossBar.Color.GREEN;

        // 4. Build the dynamic title
        Component bossBarTitle;
        if (hasUnlocked) {
            bossBarTitle = Component.text("Build World Unlocked! | Total: ", NamedTextColor.GOLD)
                    .append(Component.text(commentsMade, NamedTextColor.WHITE))
                    .append(Component.text(" | #1 Leader: ", NamedTextColor.GRAY))
                    .append(Component.text(top1Name + " (" + top1Value + ")", NamedTextColor.YELLOW));
        } else {
            bossBarTitle = Component.text("Progress to Build World: ", NamedTextColor.WHITE)
                    .append(Component.text(commentsMade + "/" + target, NamedTextColor.GREEN))
                    .append(Component.text(" | #1 Leader: ", NamedTextColor.GRAY))
                    .append(Component.text(top1Name + " (" + top1Value + ")", NamedTextColor.GOLD));
        }

        // 5. Update existing BossBar or create a new one
        BossBar existingBar = activeBossBars.get(player.getUniqueId());

        if (existingBar != null) {
            existingBar.name(bossBarTitle);
            existingBar.progress(progress);
            existingBar.color(barColor);
        } else {
            BossBar newBar = BossBar.bossBar(bossBarTitle, progress, barColor, BossBar.Overlay.PROGRESS);
            player.showBossBar(newBar);
            activeBossBars.put(player.getUniqueId(), newBar);
        }
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