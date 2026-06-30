package eu.bruza.vojtech.playConsultantPlugin.commands;

import eu.bruza.vojtech.playConsultantPlugin.PlayConsultantPlugin;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClearAllCommentsCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;
    private final Map<String, Long> pendingConfirmations = new HashMap<>();
    private static final long CONFIRMATION_TIME_MS = 10000;

    public ClearAllCommentsCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (sender instanceof Player && !sender.isOp()) {
            sender.sendMessage("This command is for operators only.");
            return true;
        }

        String senderId = sender.getName();
        Long lastRequest = pendingConfirmations.get(senderId);

        if (lastRequest != null && System.currentTimeMillis() - lastRequest < CONFIRMATION_TIME_MS) {
            pendingConfirmations.remove(senderId);
        } else {
            pendingConfirmations.put(senderId, System.currentTimeMillis());
            sender.sendMessage("Are you sure you want to clear all comments? Type the command again within 10 seconds to confirm.");
            return true;
        }

        sender.sendMessage("Starting to clear all comments and holograms...");

        Set<String> hologramNamesToDelete = new HashSet<>();
        int entitiesRemoved = 0;

        // First, iterate through entities to find their associated holograms, then remove the entities.
        // This mimics the logic of the working RemoveCommentCommand.
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
                    String hologramName = entity.getPersistentDataContainer().get(plugin.getHologramNameKey(), PersistentDataType.STRING);
                    if (hologramName != null) {
                        hologramNamesToDelete.add(hologramName);
                    }
                    entity.remove();
                    entitiesRemoved++;
                }
            }
        }

        // Also, collect any holograms that might be orphaned already, just in case.
        for (Hologram hologram : DecentHologramsAPI.get().getHologramManager().getHolograms()) {
            if (hologram.getName().startsWith("comment_")) {
                hologramNamesToDelete.add(hologram.getName());
            }
        }

        // Now, delete all collected holograms using the reliable DHAPI.getHologram() method.
        int hologramsRemoved = 0;
        for (String hologramName : hologramNamesToDelete) {
            Hologram hologram = DHAPI.getHologram(hologramName);
            if (hologram != null) {
                hologram.delete();
                hologramsRemoved++;
            }
        }

        sender.sendMessage(String.format("Cleanup complete. Removed %d holograms and %d comment entities.", hologramsRemoved, entitiesRemoved));

        return true;
    }
}