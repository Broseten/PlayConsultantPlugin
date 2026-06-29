package eu.bruza.vojtech.playConsultantPlugin.commands;

import eu.bruza.vojtech.playConsultantPlugin.PlayConsultantPlugin;
import eu.bruza.vojtech.playConsultantPlugin.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GrantRewardCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;

    public GrantRewardCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /" + label + " <player>", NamedTextColor.RED));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        PlayerData playerData = plugin.getOrCreatePlayerData(targetPlayer.getUniqueId());

        // Mark progression as complete to allow access via the key menu
        playerData.setWarehouseCompleted(true);

        // This handles both new rewards and re-running for players who already have a plot.
        // It will create the plot, paste the schematic, and give the key upon completion.
        // It does NOT teleport the player.
        plugin.getPlotManager().rewardPlayerWithCreativePlot(targetPlayer);

        // Notify the target player
        targetPlayer.sendMessage(Component.text(
                "You've been granted access to the Build World! Your plot is being prepared...",
                NamedTextColor.GOLD
        ));

        // Notify the command sender
        sender.sendMessage(Component.text("Creative reward granted to " + targetPlayer.getName() + ". They will receive a key when their plot is ready.", NamedTextColor.GREEN));
        return true;
    }
}