package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayConsultantPlugin extends JavaPlugin {
    // Safely identifies our custom item
    public NamespacedKey megaphoneKey;

    // Accessed from both sync interaction and async chat events.
    public final Map<UUID, PlayerData> activePlayers = new ConcurrentHashMap<>();

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

    public void startTyping(UUID playerId) {
        activePlayers.computeIfAbsent(playerId, k -> new PlayerData()).setTypingComment(true);
    }

    public boolean isTyping(UUID playerId) {
        PlayerData data = activePlayers.get(playerId);
        return data != null && data.isTypingComment();
    }

    public void stopTyping(UUID playerId) {
        PlayerData data = activePlayers.get(playerId);
        if (data != null) {
            data.setTypingComment(false);
        }
    }

    public int incrementAndGetComments(UUID playerId) {
        PlayerData data = activePlayers.computeIfAbsent(playerId, k -> new PlayerData());
        data.incrementComments();
        return data.getCommentsMade();
    }
}
