package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ResetPlayerDataCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;

    public ResetPlayerDataCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playconsultant.resetplayerdata")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /resetplayerdata <player>");
            return false;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        UUID playerUUID = targetPlayer.getUniqueId();
        if (plugin.resetPlayerData(playerUUID)) {
            sender.sendMessage(ChatColor.GREEN + "Player data for " + targetPlayer.getName() + " has been reset.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No player data found for " + targetPlayer.getName() + ".");
        }

        return true;
    }
}