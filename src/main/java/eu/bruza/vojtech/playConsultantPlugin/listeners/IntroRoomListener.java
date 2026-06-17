package eu.bruza.vojtech.playConsultantPlugin.listeners;

import eu.bruza.vojtech.playConsultantPlugin.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class IntroRoomListener implements Listener {

    private final PlayConsultantPlugin plugin;
    private final PlayConsultantConfigManager configManager;

    public IntroRoomListener(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    // Optional: Lock them when they join if they spawn inside the area
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();

        // If they spawn inside the box, lock them and ask the question
        if (isInsideArea(loc)) {
            PlayerData data = plugin.getOrCreatePlayerData(player.getUniqueId());
            data.setIntroCompleted(false);

            // Delay the message slightly so it doesn't get lost in login spam
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(Component.text("Every child has the right to play!", NamedTextColor.GOLD));
                player.sendMessage(Component.text("To leave this area, tell us in the chat: " + configManager.getIntroRoomExitQuestion(), NamedTextColor.AQUA));
            }, 40L);
        }
    }

    // Block them from leaving
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Only check if they actually moved to a new block to save server performance
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getOrCreatePlayerData(player.getUniqueId());

        // If they are locked, and their NEXT step is outside the area
        if (!data.isIntroCompleted()) {
            if (isInsideArea(event.getFrom()) && !isInsideArea(event.getTo())) {
                // Cancel the movement so they bounce back
                event.setCancelled(true);
                // Inform them why
                player.sendMessage(Component.text("Wait! To leave, you must first answer the question in the chat: " + configManager.getIntroRoomExitQuestion(), NamedTextColor.RED));
            }
        }
    }

    // Detect their answer
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getOrCreatePlayerData(player.getUniqueId());

        // If they haven't completed the intro and are currently inside the area
        if (!data.isIntroCompleted() && isInsideArea(player.getLocation())) {

            String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

            // Prevent spamming chat
            event.setCancelled(true);

            // Log the answer separately since we do not write it to the chat
            plugin.logComment(player.getUniqueId(), player.getName(), configManager.getIntroRoomExitQuestion() + ": " + message, player.getLocation());


            // Accept any reasonable attempt (more than 2 characters)
            if (message.length() > 3) {
                // Unlock them
                data.setIntroCompleted(true);

                // Let them know they can leave!
                player.sendMessage(Component.text("Great answer! You may now leave this area and explore the town.", NamedTextColor.GREEN));

                // Start the navigation between the locations
                NavigationUtils.startNavigation(player, plugin);
            } else {
                player.sendMessage(Component.text("Please give a slightly longer answer!", NamedTextColor.RED));
            }
        }
    }

    // Helper method to check boundaries
    private boolean isInsideArea(Location loc) {
        double x = loc.getX();
        double z = loc.getZ();
        double halfSize = configManager.getIntroRoomSize() / 2.0;
        double minX = configManager.getIntroRoomCenterX() - halfSize;
        double maxX = configManager.getIntroRoomCenterX() + halfSize;
        double minZ = configManager.getIntroRoomCenterZ() - halfSize;
        double maxZ = configManager.getIntroRoomCenterZ() + halfSize;
        return (x >= minX && x <= maxX && z >= minZ && z <= maxZ);
    }
}