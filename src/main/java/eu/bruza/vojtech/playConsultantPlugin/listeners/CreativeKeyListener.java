package eu.bruza.vojtech.playConsultantPlugin.listeners;

import eu.bruza.vojtech.playConsultantPlugin.PlayConsultantPlugin;
import eu.bruza.vojtech.playConsultantPlugin.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        if (!plugin.getItemManager().isCreativeKey(item)) return;

        event.setCancelled(true);

        PlayerData data = plugin.getOrCreatePlayerData(player.getUniqueId());

        // Save location securely before opening the menu
        if (data.getCurrentZone() == PlayerData.Zones.ADVENTURE) {
            data.setLastAdventureLocation(player.getLocation());
            plugin.persistPlayerData();
        } else if (data.getCurrentZone() == PlayerData.Zones.BUILD) {
            data.setLastBuildLocation(player.getLocation());
            plugin.persistPlayerData();
        }
        // WAREHOUSE zone intentionally left out so it doesn't overwrite Adventure coords.

        openTravelMenu(player, data);
    }

    private void openTravelMenu(Player player, PlayerData data) {
        Inventory menu = Bukkit.createInventory(null, 9, Component.text("Travel Menu", NamedTextColor.DARK_AQUA));

        menu.setItem(2, createGuiItem(Material.GOLDEN_CARROT, "Return to City", "Go back to where you left off."));
        menu.setItem(4, createGuiItem(Material.BOOKSHELF, "Play Warehouse", "Get inspired by play spaces!"));

        if (data.isWarehouseCompleted()) {
            menu.setItem(6, createGuiItem(Material.GRASS_BLOCK, "My Creative Plot", "Go build your ideas!"));
        } else {
            menu.setItem(6, createGuiItem(Material.BARRIER, "???", "Explore the Warehouse to unlock!"));
        }

        player.openInventory(menu);
    }

    private ItemStack createGuiItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(java.util.Collections.singletonList(Component.text(lore, NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("Travel Menu", NamedTextColor.DARK_AQUA))) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        PlayerData data = plugin.getOrCreatePlayerData(player.getUniqueId());
        player.closeInventory();

        // Delegate all teleportation & gamemode logic to WorldTravelManager
        switch (clickedItem.getType()) {
            case GOLDEN_CARROT:
                plugin.getWorldTravelManager().travelToAdventureWorld(player);
                break;

            case BOOKSHELF:
                plugin.getWorldTravelManager().travelToWarehouse(player);
                break;

            case GRASS_BLOCK:
                if (data.isWarehouseCompleted()) {
                    plugin.getWorldTravelManager().travelToBuildWorld(player);
                } else {
                    player.sendMessage(Component.text("You must explore the Play Warehouse first!", NamedTextColor.RED));
                }
                break;

            case BARRIER:
                player.sendMessage(Component.text("You must explore the Play Warehouse first!", NamedTextColor.RED));
                break;
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