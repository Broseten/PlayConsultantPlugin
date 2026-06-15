package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayConsultantPlugin extends JavaPlugin {
    private final Map<UUID, PlayerData> activePlayers = new ConcurrentHashMap<>();

    private ItemManager itemManager;
    private WorldTravelManager worldTravelManager;
    private PlotManager plotManager;
    private CommentCsvLogger commentCsvLogger;
    private PlayConsultantConfigManager configManager;
    private PlayerDataStore playerDataStore;
    // Keys used to tag comment marker entities and store their hologram name
    private NamespacedKey commentMarkerKey;
    private NamespacedKey hologramNameKey;

    @Override
    public void onEnable() {
        getLogger().info("by Vojtech Bruza");

        this.configManager = new PlayConsultantConfigManager(this);
        this.configManager.load();

        this.itemManager = new ItemManager(this);
        this.worldTravelManager = new WorldTravelManager(this);
        this.worldTravelManager.ensureBuildWorldLoaded();
        this.plotManager = new PlotManager(this);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String logFileName = "comments_" + timestamp + ".csv";
        this.commentCsvLogger = new CommentCsvLogger(getDataFolder().toPath().resolve(logFileName), getLogger());

        // Load persistent player data (comments count, assigned plot, etc.)
        this.playerDataStore = new PlayerDataStore(this);
        this.playerDataStore.loadAll(activePlayers);

        // Register Command
        Objects.requireNonNull(getCommand("megaphone")).setExecutor(new MegaphoneCommand(this));
        Objects.requireNonNull(getCommand("removecomment")).setExecutor(new RemoveCommentCommand(this));
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor(new ReloadConfigCommand(this));
        Objects.requireNonNull(getCommand("resetplayerdata")).setExecutor(new ResetPlayerDataCommand(this));
        Objects.requireNonNull(getCommand("creativekey")).setExecutor(new CreativeKeyCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new MegaphoneListener(this), this);
        getServer().getPluginManager().registerEvents(new CreativeKeyListener(this), this);

        getLogger().info("PlayConsultant core loaded!");
    }

    @Override
    public void onDisable() {
        if (playerDataStore != null) {
            // Save synchronously on shutdown so nothing is lost
            playerDataStore.saveAll(activePlayers);
        }
        if (commentCsvLogger != null) {
            commentCsvLogger.close();
        }
    }

    public PlayConsultantConfigManager getConfigManager() {
        return configManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public WorldTravelManager getWorldTravelManager() {
        return worldTravelManager;
    }

    public PlotManager getPlotManager() {
        return plotManager;
    }

    public PlayerData getPlayerData(UUID playerId) {
        return activePlayers.get(playerId);
    }

    public PlayerData getOrCreatePlayerData(UUID playerId) {
        return activePlayers.computeIfAbsent(playerId, k -> new PlayerData());
    }

    /**
     * Persists the current state of all player data to disk asynchronously.
     * Call this after any meaningful mutation (new comment, plot assignment, key grant, ...).
     */
    public void persistPlayerData() {
        if (playerDataStore != null) {
            playerDataStore.saveAllAsync(activePlayers);
        }
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
        persistPlayerData();
        return data.getCommentsMade();
    }

    public boolean markCreativeKeyGranted(UUID playerId) {
        PlayerData data = getOrCreatePlayerData(playerId);
        if (data.hasReceivedCreativeKey()) {
            return false;
        }

        data.setReceivedCreativeKey(true);
        persistPlayerData();
        return true;
    }

    public void logComment(UUID playerId, String playerName, String comment, Location loc) {
        CommentCsvLogger logger = this.commentCsvLogger;
        if (logger != null) {
            logger.logComment(playerId, playerName, comment, loc);
        }
    }

    // NamespacedKey accessors for tagging entities/holograms
    public NamespacedKey getCommentMarkerKey() {
        if (commentMarkerKey == null) commentMarkerKey = new NamespacedKey(this, "is_comment_marker");
        return commentMarkerKey;
    }

    public NamespacedKey getHologramNameKey() {
        if (hologramNameKey == null) hologramNameKey = new NamespacedKey(this, "comment_hologram_name");
        return hologramNameKey;
    }
    
    public boolean resetPlayerData(UUID playerUUID) {
        if (activePlayers.containsKey(playerUUID)) {
            activePlayers.remove(playerUUID);
            persistPlayerData();
            return true;
        }
        return false;
    }
}