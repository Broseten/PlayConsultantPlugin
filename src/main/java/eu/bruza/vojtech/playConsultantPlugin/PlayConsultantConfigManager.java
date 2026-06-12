package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PlayConsultantConfigManager {
    private static final int DEFAULT_MIN_WORD_COUNT = 8;
    private static final int DEFAULT_CREATIVE_UNLOCK_COMMENT_COUNT = 5;
    private static final double DEFAULT_MIN_COMMENT_DISTANCE = 30.0;
    private static final double DEFAULT_REMOVE_COMMENT_SEARCH_RADIUS = 5.0;
    private static final String DEFAULT_SCHEMATIC_NAME = "city_block";

    private final PlayConsultantPlugin plugin;

    private volatile int minWordCount = DEFAULT_MIN_WORD_COUNT;
    private volatile int creativeUnlockCommentCount = DEFAULT_CREATIVE_UNLOCK_COMMENT_COUNT;
    private volatile double minCommentDistance = DEFAULT_MIN_COMMENT_DISTANCE;
    private volatile double removeCommentSearchRadius = DEFAULT_REMOVE_COMMENT_SEARCH_RADIUS;
    private volatile String schematicName = DEFAULT_SCHEMATIC_NAME;

    // Mob spawn configuration
    private static final List<MobSpawnEntry> DEFAULT_MOB_SPAWNS = List.of(
            new MobSpawnEntry(EntityType.ALLAY, 20, 0.5, false),
            new MobSpawnEntry(EntityType.SHEEP, 10, 0.0, true),
            new MobSpawnEntry(EntityType.CHICKEN, 9, 0.0, true),
            new MobSpawnEntry(EntityType.COW, 9, 0.0, true),
            new MobSpawnEntry(EntityType.PIG, 8, 0.0, true),
            new MobSpawnEntry(EntityType.RABBIT, 8, 0.0, true),
            new MobSpawnEntry(EntityType.CAT, 7, 0.0, true),
            new MobSpawnEntry(EntityType.FOX, 6, 0.0, true),
            new MobSpawnEntry(EntityType.BEE, 6, 0.5, true),
            new MobSpawnEntry(EntityType.HORSE, 5, 0.0, true),
            new MobSpawnEntry(EntityType.DONKEY, 5, 0.0, true),
            new MobSpawnEntry(EntityType.MULE, 4, 0.0, true),
            new MobSpawnEntry(EntityType.LLAMA, 4, 0.0, true),
            new MobSpawnEntry(EntityType.WOLF, 4, 0.0, true),
            new MobSpawnEntry(EntityType.OCELOT, 3, 0.0, true),
            new MobSpawnEntry(EntityType.GOAT, 3, 0.0, true),
            new MobSpawnEntry(EntityType.TURTLE, 3, 0.0, true),
            new MobSpawnEntry(EntityType.PARROT, 3, 0.5, false),
            new MobSpawnEntry(EntityType.PANDA, 2, 0.0, true),
            new MobSpawnEntry(EntityType.POLAR_BEAR, 2, 0.0, true),
            new MobSpawnEntry(EntityType.MOOSHROOM, 2, 0.0, true),
            new MobSpawnEntry(EntityType.CAMEL, 2, 0.0, true),
            new MobSpawnEntry(EntityType.ARMADILLO, 1, 0.0, true),
            new MobSpawnEntry(EntityType.SNIFFER, 1, 0.0, true)
    );

    private final Random random = new Random();
    private volatile List<MobSpawnEntry> mobSpawns = new ArrayList<>(DEFAULT_MOB_SPAWNS);

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
        schematicName = config.getString("comments.creative-plot.schematic-name", DEFAULT_SCHEMATIC_NAME);

        // read mob spawn list
        mobSpawns = readMobSpawns(config);

        plugin.getLogger().info(
                "Loaded settings: minWordCount=" + minWordCount
                        + ", creativeUnlockCommentCount=" + creativeUnlockCommentCount
                        + ", minCommentDistance=" + minCommentDistance
                        + ", removeCommentSearchRadius=" + removeCommentSearchRadius
                        + ", schematicName=" + schematicName
        );
        plugin.getLogger().info("Loaded mob spawn list with " + mobSpawns.size() + " entries.");
    }

    public MobSpawnEntry pickRandomMobSpawn() {
        // Weighted random selection
        int total = 0;
        for (MobSpawnEntry e : mobSpawns) total += Math.max(0, e.weight);
        if (total <= 0 || mobSpawns.isEmpty()) {
            // fallback
            return DEFAULT_MOB_SPAWNS.getFirst();
        }
        int r = random.nextInt(total);
        int acc = 0;
        for (MobSpawnEntry e : mobSpawns) {
            acc += Math.max(0, e.weight);
            if (r < acc) return e;
        }
        return mobSpawns.getFirst();
    }

    private List<MobSpawnEntry> readMobSpawns(FileConfiguration config) {
        List<MobSpawnEntry> list = new ArrayList<>();
        try {
            List<Map<?, ?>> raw = config.getMapList("comments.mobs");
            if (raw.isEmpty()) return new ArrayList<>(DEFAULT_MOB_SPAWNS);
            for (Map<?, ?> map : raw) {
                Object mobObj = map.get("mob");
                String mobName = mobObj != null ? mobObj.toString() : "ALLAY";
                EntityType type;
                try {
                    type = EntityType.valueOf(mobName.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid mob type in comments.mobs: " + mobName + ". Skipping entry.");
                    continue;
                }

                int weight = 1;
                try {
                    Object w = map.get("weight");
                    if (w instanceof Number) weight = ((Number) w).intValue();
                    else if (w != null) weight = Integer.parseInt(w.toString());
                } catch (Exception ignored) {
                }

                double yOffset = 0.0;
                try {
                    Object y = map.get("y-offset");
                    if (y instanceof Number) yOffset = ((Number) y).doubleValue();
                    else if (y != null) yOffset = Double.parseDouble(y.toString());
                } catch (Exception ignored) {
                }

                boolean gravity = false;
                try {
                    Object g = map.get("gravity");
                    if (g != null) gravity = Boolean.parseBoolean(g.toString());
                } catch (Exception ignored) {
                }

                list.add(new MobSpawnEntry(type, Math.max(0, weight), yOffset, gravity));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read comments.mobs from config: " + e.getMessage());
            return new ArrayList<>(DEFAULT_MOB_SPAWNS);
        }
        return list;
    }

    public static final class MobSpawnEntry {
        public final EntityType type;
        public final int weight;
        public final double yOffset;
        public final boolean gravity;

        public MobSpawnEntry(EntityType type, int weight, double yOffset, boolean gravity) {
            this.type = type;
            this.weight = weight;
            this.yOffset = yOffset;
            this.gravity = gravity;
        }
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

    public String getSchematicName() {
        return schematicName;
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

