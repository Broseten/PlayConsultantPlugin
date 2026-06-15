package eu.bruza.vojtech.playConsultantPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayConsultantCommand implements CommandExecutor, TabCompleter {

    private final PlayConsultantPlugin plugin;
    
    // Command executors for subcommands
    private final MegaphoneCommand megaphoneCommand;
    private final RemoveCommentCommand removeCommentCommand;
    private final ReloadConfigCommand reloadConfigCommand;
    private final ResetPlayerDataCommand resetPlayerDataCommand;
    private final CreativeKeyCommand creativeKeyCommand;

    public PlayConsultantCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
        this.megaphoneCommand = new MegaphoneCommand(plugin);
        this.removeCommentCommand = new RemoveCommentCommand(plugin);
        this.reloadConfigCommand = new ReloadConfigCommand(plugin);
        this.resetPlayerDataCommand = new ResetPlayerDataCommand(plugin);
        this.creativeKeyCommand = new CreativeKeyCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + label + " <megaphone|removecomment|reload|resetplayerdata|creativekey>", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        // Pass the remaining arguments to the subcommand
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "megaphone":
                return megaphoneCommand.onCommand(sender, command, label, subArgs);
            case "removecomment":
                return removeCommentCommand.onCommand(sender, command, label, subArgs);
            case "reload":
                return reloadConfigCommand.onCommand(sender, command, label, subArgs);
            case "resetplayerdata":
                return resetPlayerDataCommand.onCommand(sender, command, label, subArgs);
            case "creativekey":
                return creativeKeyCommand.onCommand(sender, command, label, subArgs);
            default:
                sender.sendMessage(Component.text("Unknown subcommand. Usage: /" + label + " <megaphone|removecomment|reload|resetplayerdata|creativekey>", NamedTextColor.RED));
                return true;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("megaphone");
            if (sender.hasPermission("playconsultant.removecomment") || sender.isOp()) {
                subCommands.add("removecomment");
            }
            if (sender.hasPermission("playconsultant.reloadconfig") || sender.isOp()) {
                subCommands.add("reload");
            }
            if (sender.hasPermission("playconsultant.resetplayerdata") || sender.isOp()) {
                subCommands.add("resetplayerdata");
            }
            subCommands.add("creativekey");
            
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("resetplayerdata")) {
             if (sender.hasPermission("playconsultant.resetplayerdata") || sender.isOp()) {
                 List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                 StringUtil.copyPartialMatches(args[1], playerNames, completions);
                 Collections.sort(completions);
                 return completions;
             }
        }
        
        return Collections.emptyList();
    }
}