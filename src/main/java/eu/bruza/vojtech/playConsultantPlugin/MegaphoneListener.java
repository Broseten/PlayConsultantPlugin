package eu.bruza.vojtech.playConsultantPlugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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

        // Check if the item is our Megaphone
        if (item.getItemMeta().getPersistentDataContainer().has(plugin.megaphoneKey)) {
            event.setCancelled(true); // Stop them from eating it

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Add them to the typing list
                plugin.typingPlayers.add(player.getUniqueId());
                player.sendMessage(Component.text("Type your comment in the chat:", NamedTextColor.AQUA));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // If they aren't typing a comment, ignore the chat event
        if (!plugin.typingPlayers.contains(player.getUniqueId())) return;

        // They are typing a comment, so stop it from showing to everyone
        event.setCancelled(true);

        // Get the plain text they typed
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Echo it back just to them for now
        player.sendMessage(Component.text("You commented: " + message, NamedTextColor.GREEN));

        // Remove them from the typing list so they can chat normally again
        plugin.typingPlayers.remove(player.getUniqueId());
    }
}
