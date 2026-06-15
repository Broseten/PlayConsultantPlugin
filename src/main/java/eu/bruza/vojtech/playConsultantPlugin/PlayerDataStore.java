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
            data.setLastCommentLocation(loadLocation(section, "lastCommentLocation"));
            data.setLastAdventureLocation(loadLocation(section, "lastAdventureLocation"));
            data.setLastBuildLocation(loadLocation(section, "lastBuildLocation"));


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

            saveLocation(cfg, base + ".lastCommentLocation", data.getLastCommentLocation());
            saveLocation(cfg, base + ".lastAdventureLocation", data.getLastAdventureLocation());
            saveLocation(cfg, base + ".lastBuildLocation", data.getLastBuildLocation());
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

    private void saveLocation(FileConfiguration cfg, String path, Location loc) {
        if (loc != null && loc.getWorld() != null) {
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getX());
            cfg.set(path + ".y", loc.getY());
            cfg.set(path + ".z", loc.getZ());
            cfg.set(path + ".yaw", loc.getYaw());
            cfg.set(path + ".pitch", loc.getPitch());
        }
    }

    private Location loadLocation(ConfigurationSection section, String path) {
        ConfigurationSection locSection = section.getConfigurationSection(path);
        if (locSection != null) {
            String worldName = locSection.getString("world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world != null) {
                return new Location(
                        world,
                        locSection.getDouble("x"),
                        locSection.getDouble("y"),
                        locSection.getDouble("z"),
                        (float) locSection.getDouble("yaw", 0.0),
                        (float) locSection.getDouble("pitch", 0.0)
                );
            }
        }
        return null;
    }
}