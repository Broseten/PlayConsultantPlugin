package eu.bruza.vojtech.playConsultantPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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

        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());
        if (!playerData.hasReceivedCreativeKey()) {
            player.sendMessage(Component.text("You haven't unlocked the ability to use this key yet.", NamedTextColor.RED));
            return;
        }

        if (plugin.getWorldTravelManager().toggleWorld(player)) {
            player.sendMessage(Component.text("The key warps you to another world!", NamedTextColor.AQUA));
        } else {
            player.sendMessage(Component.text("The key fizzles, but nothing happens.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());
        
        if (playerData.getAssignedPlotId() != null || playerData.hasReceivedCreativeKey()) {
            if (!plugin.getItemManager().hasCreativeKey(player)) {
                plugin.getItemManager().giveCreativeKey(player);
            }
        }
    }
}