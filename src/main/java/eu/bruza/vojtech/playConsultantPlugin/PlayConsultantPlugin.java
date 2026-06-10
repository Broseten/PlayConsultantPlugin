package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayConsultantPlugin extends JavaPlugin {
    private final Map<UUID, PlayerData> activePlayers = new ConcurrentHashMap<>();

    private ItemManager itemManager;
    private WorldTravelManager worldTravelManager;

    @Override
    public void onEnable() {
        getLogger().info("by Vojtech Bruza");

        this.itemManager = new ItemManager(this);
        this.worldTravelManager = new WorldTravelManager();
        this.worldTravelManager.ensureBuildWorldLoaded();

        // Register Command
        Objects.requireNonNull(getCommand("megaphone")).setExecutor(new MegaphoneCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new MegaphoneListener(this), this);
        getServer().getPluginManager().registerEvents(new CreativeKeyListener(this), this);

        getLogger().info("PlayConsultant core loaded!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public WorldTravelManager getWorldTravelManager() {
        return worldTravelManager;
    }

    public PlayerData getPlayerData(UUID playerId) {
        return activePlayers.get(playerId);
    }

    public PlayerData getOrCreatePlayerData(UUID playerId) {
        return activePlayers.computeIfAbsent(playerId, k -> new PlayerData());
    }

    public void startTyping(UUID playerId) {
        getOrCreatePlayerData(playerId).setTypingComment(true);
    }

    public boolean isTyping(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isTypingComment();
    }

    public void stopTyping(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        if (data != null) {
            data.setTypingComment(false);
        }
    }

    public int incrementAndGetComments(UUID playerId) {
        PlayerData data = getOrCreatePlayerData(playerId);
        data.incrementComments();
        return data.getCommentsMade();
    }

    public boolean markCreativeKeyGranted(UUID playerId) {
        PlayerData data = getOrCreatePlayerData(playerId);
        if (data.hasReceivedCreativeKey()) {
            return false;
        }

        data.setReceivedCreativeKey(true);
        return true;
    }
}
