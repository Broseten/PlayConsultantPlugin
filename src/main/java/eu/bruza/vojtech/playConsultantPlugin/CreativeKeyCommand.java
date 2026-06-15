package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreativeKeyCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;

    public CreativeKeyCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getOrCreatePlayerData(player.getUniqueId());

        if (playerData.hasReceivedCreativeKey()) {
            plugin.getItemManager().giveCreativeKey(player);
            player.sendMessage(ChatColor.GREEN + "You have been given a new Creative Key.");
        } else {
            player.sendMessage(ChatColor.RED + "You have not unlocked the Creative Key yet.");
        }

        return true;
    }
}