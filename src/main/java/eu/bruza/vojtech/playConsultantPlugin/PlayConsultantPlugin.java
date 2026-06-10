package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PlayConsultantPlugin extends JavaPlugin {
    // Safely identifies our custom item
    public NamespacedKey megaphoneKey;

    // Tracks who clicked the megaphone and is currently typing
    public final Set<UUID> typingPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getLogger().info("by Vojtech Bruza");


        this.megaphoneKey = new NamespacedKey(this, "is_megaphone");

        // Register Command
        Objects.requireNonNull(getCommand("megaphone")).setExecutor(new MegaphoneCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new MegaphoneListener(this), this);

        getLogger().info("PlayConsultant core loaded!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
