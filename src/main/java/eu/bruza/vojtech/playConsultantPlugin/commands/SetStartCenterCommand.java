package eu.bruza.vojtech.playConsultantPlugin.commands;

import eu.bruza.vojtech.playConsultantPlugin.PlayConsultantPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetStartCenterCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;

    public SetStartCenterCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("playconsultant.setstartcenter")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        Location location = player.getLocation();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        plugin.getConfig().set("intro-room.center.x", x);
        plugin.getConfig().set("intro-room.center.z", z);
        plugin.saveConfig();
        plugin.getConfigManager().reload();

        player.sendMessage(Component.text("Starting area center set to your current location (" + x + ", " + z + ").", NamedTextColor.GREEN));
        return true;
    }
}