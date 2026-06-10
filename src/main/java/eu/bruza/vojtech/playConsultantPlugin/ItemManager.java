package eu.bruza.vojtech.playConsultantPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ItemManager {
    private final NamespacedKey megaphoneKey;
    private final NamespacedKey creativeKey;

    public ItemManager(Plugin plugin) {
        this.megaphoneKey = new NamespacedKey(plugin, "is_megaphone");
        this.creativeKey = new NamespacedKey(plugin, "is_creative_key");
    }

    public ItemStack createMegaphone() {
        return createTaggedItem(
                Material.GOLDEN_CARROT,
                Component.text("Megaphone", NamedTextColor.GOLD),
                megaphoneKey
        );
    }

    public ItemStack createCreativeKey() {
        ItemStack item = createTaggedItem(
                Material.TRIPWIRE_HOOK,
                Component.text("Enchanted Key", NamedTextColor.AQUA),
                creativeKey
        );
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMegaphone(ItemStack item) {
        return hasTag(item, megaphoneKey);
    }

    public boolean isCreativeKey(ItemStack item) {
        return hasTag(item, creativeKey);
    }

    public boolean hasCreativeKey(Player player) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isCreativeKey(stack)) {
                return true;
            }
        }

        return isCreativeKey(player.getInventory().getItemInOffHand());
    }

    public void giveMegaphone(Player player) {
        giveItem(player, createMegaphone());
    }

    public void giveCreativeKey(Player player) {
        giveItem(player, createCreativeKey());
    }

    private ItemStack createTaggedItem(Material material, Component displayName, NamespacedKey key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean hasTag(ItemStack item, NamespacedKey key) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}

