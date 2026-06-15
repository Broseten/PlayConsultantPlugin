package eu.bruza.vojtech.playConsultantPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        sender.sendMessage(Component.text("--- PlayConsultant Help ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Use the ", NamedTextColor.GRAY)
                .append(Component.text("Megaphone", NamedTextColor.YELLOW))
                .append(Component.text(" item to leave comments. Right-click with the Megaphone in your hand to start writing in the chat.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("To open the chat and start typing, press the 'T' key (by default).", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Your goal is to leave insightful comments to improve the city.", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Once you have left enough comments, you will be rewarded!", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("-------------------------", NamedTextColor.GOLD, TextDecoration.BOLD));

        return true;
    }
}