package eu.bruza.vojtech.playConsultantPlugin.commands;

import eu.bruza.vojtech.playConsultantPlugin.PlayConsultantPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlockBuildWorldCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;

    public UnlockBuildWorldCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Require OP or specific permission, since players shouldn't run this manually
        if (!sender.isOp() && !sender.hasPermission("playconsultant.unlockbuild")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /" + label + " unlockbuild <player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or offline.", NamedTextColor.RED));
            return true;
        }

        // Update player data
        plugin.getOrCreatePlayerData(target.getUniqueId()).setWarehouseCompleted(true);

        // Send Title and Message to the target player
        Title title = Title.title(
                Component.text("Warehouse Cleared!", NamedTextColor.GOLD),
                Component.text("Your build world plot is now unlocked.", NamedTextColor.YELLOW)
        );
        target.showTitle(title);
        target.sendMessage(Component.text("Congratulations! You have finished exploring the Warehouse. You can now build on your plot!", NamedTextColor.GREEN));

        // Optional confirmation to the sender (useful if looking at console logs)
        sender.sendMessage(Component.text("Build world plot unlocked successfully for " + target.getName(), NamedTextColor.GREEN));

        return true;
    }
}