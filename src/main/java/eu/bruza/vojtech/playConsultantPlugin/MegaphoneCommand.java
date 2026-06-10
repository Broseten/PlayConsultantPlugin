package eu.bruza.vojtech.playConsultantPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class MegaphoneCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;

    public MegaphoneCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        // Create the item
        ItemStack megaphone = new ItemStack(Material.GOLDEN_CARROT);
        ItemMeta meta = megaphone.getItemMeta();
        meta.displayName(Component.text("Megaphone", NamedTextColor.GOLD));

        // Tag it invisibly so players can't just rename a regular carrot to "Megaphone"
        meta.getPersistentDataContainer().set(plugin.megaphoneKey, PersistentDataType.BYTE, (byte) 1);
        megaphone.setItemMeta(meta);

        // Give it to the player
        player.getInventory().addItem(megaphone);
        player.sendMessage(Component.text("You received the Megaphone!", NamedTextColor.GREEN));

        return true;
    }
}
