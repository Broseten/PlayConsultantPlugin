package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.configuration.file.FileConfiguration;

public class PlayConsultantConfigManager {
    private static final int DEFAULT_MIN_WORD_COUNT = 8;
    private static final int DEFAULT_CREATIVE_UNLOCK_COMMENT_COUNT = 5;
    private static final double DEFAULT_MIN_COMMENT_DISTANCE = 30.0;
    private static final double DEFAULT_REMOVE_COMMENT_SEARCH_RADIUS = 5.0;

    private final PlayConsultantPlugin plugin;

    private volatile int minWordCount = DEFAULT_MIN_WORD_COUNT;
    private volatile int creativeUnlockCommentCount = DEFAULT_CREATIVE_UNLOCK_COMMENT_COUNT;
    private volatile double minCommentDistance = DEFAULT_MIN_COMMENT_DISTANCE;
    private volatile double removeCommentSearchRadius = DEFAULT_REMOVE_COMMENT_SEARCH_RADIUS;

    public PlayConsultantConfigManager(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        minWordCount = readPositiveInt(config, "comments.min-word-count", DEFAULT_MIN_WORD_COUNT, "comments.min-word-count");
        creativeUnlockCommentCount = readPositiveInt(
                config,
                "comments.creative-unlock-comment-count",
                DEFAULT_CREATIVE_UNLOCK_COMMENT_COUNT,
                "comments.creative-unlock-comment-count"
        );
        minCommentDistance = readPositiveDouble(
                config,
                "comments.min-comment-distance",
                DEFAULT_MIN_COMMENT_DISTANCE,
                "comments.min-comment-distance"
        );
        removeCommentSearchRadius = readPositiveDouble(
                config,
                "commands.removecomment.search-radius",
                DEFAULT_REMOVE_COMMENT_SEARCH_RADIUS,
                "commands.removecomment.search-radius"
        );

        plugin.getLogger().info(
                "Loaded settings: minWordCount=" + minWordCount
                        + ", creativeUnlockCommentCount=" + creativeUnlockCommentCount
                        + ", minCommentDistance=" + minCommentDistance
                        + ", removeCommentSearchRadius=" + removeCommentSearchRadius
        );
    }

    public int getMinWordCount() {
        return minWordCount;
    }

    public int getCreativeUnlockCommentCount() {
        return creativeUnlockCommentCount;
    }

    public double getMinCommentDistance() {
        return minCommentDistance;
    }

    public double getRemoveCommentSearchRadius() {
        return removeCommentSearchRadius;
    }

    private int readPositiveInt(FileConfiguration config, String path, int defaultValue, String label) {
        int value = config.getInt(path, defaultValue);
        if (value < 1) {
            plugin.getLogger().warning("Invalid value for " + label + ": " + value + ". Using default " + defaultValue + ".");
            return defaultValue;
        }
        return value;
    }

    private double readPositiveDouble(FileConfiguration config, String path, double defaultValue, String label) {
        double value = config.getDouble(path, defaultValue);
        if (value <= 0.0D) {
            plugin.getLogger().warning("Invalid value for " + label + ": " + value + ". Using default " + defaultValue + ".");
            return defaultValue;
        }
        return value;
    }
}

