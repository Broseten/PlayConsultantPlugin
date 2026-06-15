package eu.bruza.vojtech.playConsultantPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CreativeKeyListener implements Listener {
    private final PlayConsultantPlugin plugin;

    public CreativeKeyListener(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!plugin.getItemManager().isCreativeKey(item)) {
            return;
        }

        // Always cancel default item interaction so the key never does anything else.
        event.setCancelled(true);

        // The key has only one purpose: teleport the player to their assigned plot.
        if (plugin.getPlotManager().teleportToAssignedPlot(player)) {
            player.sendMessage(Component.text("The key pulls you to your plot!", NamedTextColor.AQUA));
            return;
        }

        player.sendMessage(Component.text(
                "Your key is dormant — your plot is not ready yet.",
                NamedTextColor.RED
        ));
    }
}