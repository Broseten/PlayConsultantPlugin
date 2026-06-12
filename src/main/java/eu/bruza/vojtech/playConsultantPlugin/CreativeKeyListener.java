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

        event.setCancelled(true);

        // First try to teleport to their assigned plot
        if (plugin.getPlotManager().teleportToAssignedPlot(player)) {
            player.sendMessage(Component.text("The key pulls you to your plot!", NamedTextColor.AQUA));
            return;
        }

        // Fallback: toggle between worlds (old behavior) if no plot is assigned yet
        boolean toggled = plugin.getWorldTravelManager().toggleWorld(player);
        if (!toggled) {
            player.sendMessage(Component.text("That key cannot find the destination world right now.", NamedTextColor.RED));
            return;
        }

        if (plugin.getWorldTravelManager().isBuildWorld(player.getWorld())) {
            player.sendMessage(Component.text("The key pulls you into the build world!", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("The key returns you to the adventure world.", NamedTextColor.GOLD));
        }
    }
}

