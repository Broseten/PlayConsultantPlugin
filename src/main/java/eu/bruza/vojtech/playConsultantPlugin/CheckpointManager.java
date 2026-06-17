package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckpointManager {
    private final JavaPlugin plugin;
    private final List<Location> checkpoints = new ArrayList<>();

    // Custom File Variables
    private File file;
    private FileConfiguration customConfig;

    public CheckpointManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupCustomConfig();
        loadCheckpoints();
    }

    // 1. Setup and load the physical file
    private void setupCustomConfig() {
        // Create the plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Create the custom YAML file
        file = new File(plugin.getDataFolder(), "checkpoints.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create checkpoints.yml!");
                e.printStackTrace();
            }
        }

        // Load the configuration from the file
        customConfig = YamlConfiguration.loadConfiguration(file);
    }

    // 2. Save method for the custom file
    private void saveCustomConfig() {
        try {
            customConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save to checkpoints.yml!");
            e.printStackTrace();
        }
    }

    // 3. Load locations into memory
    public void loadCheckpoints() {
        checkpoints.clear();
        if (customConfig.contains("locations")) {
            int i = 0;
            // Loop through the numbered keys under "locations"
            while (customConfig.contains("locations." + i)) {
                checkpoints.add((Location) customConfig.get("locations." + i));
                i++;
            }
        }
    }

    // 4. Add a new checkpoint and save to the custom file
    public void addCheckpoint(Location loc) {
        checkpoints.add(loc);
        int index = checkpoints.size() - 1;

        // Save it under the "locations" path
        customConfig.set("locations." + index, loc);
        saveCustomConfig();
    }

    public Location getCheckpoint(int index) {
        if (index >= 0 && index < checkpoints.size()) {
            return checkpoints.get(index);
        }
        return null;
    }

    // Optional: A method to reload the file if you edit it manually while the server is running
    public void reload() {
        customConfig = YamlConfiguration.loadConfiguration(file);
        loadCheckpoints();
    }
}