package eu.bruza.vojtech.playConsultantPlugin;

import com.plotsquared.core.plot.PlotId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists per-player data (comment counts, assigned plot, creative-key state,
 * last comment location) across server restarts using a YAML file in the
 * plugin's data folder.
 */
public class PlayerDataStore {
    private static final String FILE_NAME = "playerdata.yml";

    private final PlayConsultantPlugin plugin;
    private final File file;
    private final Logger logger;

    public PlayerDataStore(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("Could not create plugin data folder: " + folder.getAbsolutePath());
        }
        this.file = new File(folder, FILE_NAME);
    }

    /**
     * Loads all stored player data into the supplied map.
     */
    public void loadAll(Map<UUID, PlayerData> target) {
        if (!file.exists()) {
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) return;

        int loaded = 0;
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                logger.warning("Skipping invalid UUID in playerdata.yml: " + key);
                continue;
            }

            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) continue;

            PlayerData data = new PlayerData();
            data.setCommentsMade(section.getInt("commentsMade", 0));
            data.setReceivedCreativeKey(section.getBoolean("receivedCreativeKey", false));

            // Last comment location (only restored if the world is loaded)
            ConfigurationSection loc = section.getConfigurationSection("lastCommentLocation");
            if (loc != null) {
                String worldName = loc.getString("world");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world != null) {
                    Location l = new Location(
                            world,
                            loc.getDouble("x"),
                            loc.getDouble("y"),
                            loc.getDouble("z"),
                            (float) loc.getDouble("yaw", 0.0),
                            (float) loc.getDouble("pitch", 0.0)
                    );
                    data.setLastCommentLocation(l);
                }
            }

            // Assigned plot id stored as "x;y"
            String plotIdStr = section.getString("assignedPlotId");
            if (plotIdStr != null && !plotIdStr.isBlank()) {
                PlotId pid = parsePlotId(plotIdStr);
                if (pid != null) {
                    data.setAssignedPlotId(pid);
                }
            }

            target.put(uuid, data);
            loaded++;
        }

        logger.info("Loaded persistent player data for " + loaded + " player(s).");
    }

    /**
     * Synchronously saves all player data from the supplied map to disk.
     * Must be called on shutdown to guarantee no data is lost.
     */
    public void saveAll(Map<UUID, PlayerData> source) {
        FileConfiguration cfg = new YamlConfiguration();

        // Take a defensive snapshot of the map keys/values to avoid concurrent
        // modification when saving asynchronously.
        Map<UUID, PlayerData> snapshot = new HashMap<>(source);

        for (Map.Entry<UUID, PlayerData> e : snapshot.entrySet()) {
            PlayerData data = e.getValue();
            if (data == null) continue;

            String base = "players." + e.getKey();
            cfg.set(base + ".commentsMade", data.getCommentsMade());
            cfg.set(base + ".receivedCreativeKey", data.hasReceivedCreativeKey());

            PlotId pid = data.getAssignedPlotId();
            if (pid != null) {
                cfg.set(base + ".assignedPlotId", pid.getX() + ";" + pid.getY());
            }

            Location loc = data.getLastCommentLocation();
            if (loc != null && loc.getWorld() != null) {
                cfg.set(base + ".lastCommentLocation.world", loc.getWorld().getName());
                cfg.set(base + ".lastCommentLocation.x", loc.getX());
                cfg.set(base + ".lastCommentLocation.y", loc.getY());
                cfg.set(base + ".lastCommentLocation.z", loc.getZ());
                cfg.set(base + ".lastCommentLocation.yaw", loc.getYaw());
                cfg.set(base + ".lastCommentLocation.pitch", loc.getPitch());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save player data to " + file.getAbsolutePath(), ex);
        }
    }

    /**
     * Schedules an asynchronous save so frequent updates do not block the main thread.
     */
    public void saveAllAsync(Map<UUID, PlayerData> source) {
        if (!plugin.isEnabled()) {
            // The scheduler refuses tasks while the plugin is disabling; save inline instead.
            saveAll(source);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveAll(source));
    }

    private PlotId parsePlotId(String raw) {
        try {
            String[] parts = raw.split(";");
            if (parts.length != 2) return null;
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            return PlotId.of(x, y);
        } catch (Exception ex) {
            logger.warning("Failed to parse assignedPlotId '" + raw + "': " + ex.getMessage());
            return null;
        }
    }
}
