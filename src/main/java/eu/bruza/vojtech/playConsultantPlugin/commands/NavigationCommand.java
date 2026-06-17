package eu.bruza.vojtech.playConsultantPlugin.commands;

import eu.bruza.vojtech.playConsultantPlugin.PlayConsultantPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

public class NavigationCommand implements CommandExecutor {
    private final PlayConsultantPlugin plugin;

    public NavigationCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        if (args.length == 0) return false;

        // /nav add
        if (args[0].equalsIgnoreCase("add") && player.hasPermission("playconsultant.nav.add")) {
            plugin.getCheckpointsManager().addCheckpoint(player.getLocation());
            player.sendMessage("§aCheckpoint added at your current location!");
            return true;
        }

        // /nav compass
        if (args[0].equalsIgnoreCase("compass")) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Navigation Compass");
                compass.setItemMeta(meta);
            }
            player.getInventory().addItem(compass);
            player.sendMessage("§aYou received the navigation compass.");
            return true;
        }

        return false;
    }
}
