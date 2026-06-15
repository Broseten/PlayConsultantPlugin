package eu.bruza.vojtech.playConsultantPlugin;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CleanupCommentsCommand implements CommandExecutor {

    private final PlayConsultantPlugin plugin;
    private final Map<String, Long> pendingConfirmations = new HashMap<>();
    private static final long CONFIRMATION_TIME_MS = 10000;

    public CleanupCommentsCommand(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
            sender.sendMessage("Are you sure you want to clean up all orphaned comments? Type the command again within 10 seconds to confirm.");
            return true;
        }

        sender.sendMessage("Starting cleanup of orphaned comments and holograms...");

        Set<UUID> entitiesWithHologram = new HashSet<>();
        int hologramsRemoved = 0;

        // Step 1: Scan all holograms and check for nearby entities
        for (Hologram hologram : DecentHologramsAPI.get().getHologramManager().getHolograms()) {
            if (!hologram.getName().startsWith("comment_")) {
                continue;
            }
            
            World world = hologram.getLocation().getWorld();
            if (world == null) continue;

            boolean entityFound = false;
            for (Entity entity : world.getNearbyEntities(hologram.getLocation(), 2.0, 2.0, 2.0)) {
                if (entity.getPersistentDataContainer().has(plugin.getHologramNameKey(), PersistentDataType.STRING)) {
                    String hologramName = entity.getPersistentDataContainer().get(plugin.getHologramNameKey(), PersistentDataType.STRING);
                    if (hologram.getName().equals(hologramName)) {
                        entitiesWithHologram.add(entity.getUniqueId());
                        entityFound = true;
                        break;
                    }
                }
            }

            if (!entityFound) {
                hologram.delete();
                hologramsRemoved++;
            }
        }

        int entitiesRemoved = 0;
        // Step 2: Scan all entities and check for holograms
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(plugin.getCommentMarkerKey(), PersistentDataType.BYTE)) {
                    if (!entitiesWithHologram.contains(entity.getUniqueId())) {
                        entity.remove();
                        entitiesRemoved++;
                    }
                }
            }
        }

        sender.sendMessage(String.format("Cleanup complete. Removed %d orphaned holograms and %d orphaned comment entities.", hologramsRemoved, entitiesRemoved));

        return true;
    }
}