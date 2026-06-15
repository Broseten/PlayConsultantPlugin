package eu.bruza.vojtech.playConsultantPlugin;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.location.Location;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

public class PlotManager {
    private static final String CREATIVE_PLOT_AREA_NAME = "creative_plot";
    private final PlayConsultantPlugin plugin;

    public PlotManager(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Rewards a player with a plot in the creative_plot world.
     * Assigns them an unused plot, claims it, loads the schematic, and stores the plot ID.
     * Does NOT teleport the player.
     *
     * @param player The player to reward
     */
    public void rewardPlayerWithCreativePlot(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerData(playerId);

        if (playerData != null && playerData.getAssignedPlotId() != null) {
            player.sendMessage(Component.text("You already have an assigned plot!", NamedTextColor.AQUA));
            // Make sure they have the key so they can travel back
            ensureKey(player);
            return;
        }

        try {
            PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea(CREATIVE_PLOT_AREA_NAME, null);

            if (area == null) {
                plugin.getLogger().warning("Creative plot area '" + CREATIVE_PLOT_AREA_NAME + "' not found in PlotSquared!");
                player.sendMessage(Component.text("Error: The creative plot world is not configured. Contact staff.", NamedTextColor.RED));
                return;
            }

            // Defensive: if PlotSquared already records this player as the owner of a plot
            // (e.g. local plugin data was wiped), reuse it instead of handing out a new one.
            Plot existing = findExistingOwnedPlot(area, playerId);
            if (existing != null) {
                PlayerData data = plugin.getOrCreatePlayerData(playerId);
                data.setAssignedPlotId(existing.getId());
                plugin.persistPlayerData();
                plugin.getLogger().info("Reusing existing plot " + existing.getId()
                        + " already owned by " + player.getName());
                player.sendMessage(Component.text("Your previous plot has been restored.", NamedTextColor.AQUA));
                ensureKey(player);
                return;
            }

            // Convert Bukkit Player to PlotPlayer for API compatibility
            PlotPlayer<?> plotPlayer = PlotPlayer.from(player);

            // Get the next free plot efficiently using PlotSquared API
            Plot plot = area.getNextFreePlot(plotPlayer, null);

            if (plot == null) {
                plugin.getLogger().warning("No available plots found in creative_plot area!");
                player.sendMessage(Component.text("Error: No available plots. Contact staff.", NamedTextColor.RED));
                return;
            }

            PlotId nextAvailableId = plot.getId();

            // 1. Claim the plot for the player safely
            try {
                plot.setOwner(player.getUniqueId());
                plugin.getLogger().info("Claimed plot " + nextAvailableId + " for player " + player.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to claim plot: " + e.getMessage());
                player.sendMessage(Component.text("Error: Could not claim the plot. Contact staff.", NamedTextColor.RED));
                return;
            }

            // Store the plot ID in player data and persist immediately so a crash
            // before the schematic finishes still keeps the assignment.
            PlayerData data = plugin.getOrCreatePlayerData(playerId);
            data.setAssignedPlotId(nextAvailableId);
            plugin.persistPlayerData();

            // 2. Locate and paste the schematic programmatically
            String schematicName = plugin.getConfigManager().getSchematicName();
            File schematicFile = getSchematicFile(schematicName);

            if (schematicFile.exists()) {
                pasteSchematic(plot, schematicFile, player);
            } else {
                plugin.getLogger().warning("Schematic not found at: " + schematicFile.getAbsolutePath());
                player.sendMessage(Component.text("§cWarning: The schematic file could not be found. Your plot is ready but empty.", NamedTextColor.YELLOW));
                // Plot exists, even if empty — still give them the key.
                grantKeyOnMainThread(player);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error rewarding player with creative plot: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(Component.text("An error occurred while setting up your plot. Contact staff.", NamedTextColor.RED));
        }
    }

    /**
     * Looks for a plot in the given area that already belongs to the player.
     * Used to prevent assigning a second plot if the persistent player-data file was lost.
     */
    private Plot findExistingOwnedPlot(PlotArea area, UUID playerId) {
        try {
            for (Plot plot : area.getPlots()) {
                if (plot != null && playerId.equals(plot.getOwnerAbs())) {
                    return plot;
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not scan area for previously owned plots: " + ex.getMessage());
        }
        return null;
    }

    private File getSchematicFile(String schematicName) {
        File[] possiblePaths = {
                new File("plugins/PlotSquared/schematics/" + schematicName + ".schem"),
                new File("plugins/PlotSquared/schematics/" + schematicName + ".schematic"),
                new File("schematics/" + schematicName + ".schem"),
                new File("schematics/" + schematicName + ".schematic"),
        };

        for (File path : possiblePaths) {
            if (path.exists()) {
                return path;
            }
        }
        return possiblePaths[0];
    }

    /**
     * Attempts to paste a schematic into a plot using FastAsyncWorldEdit natively.
     */
    private void pasteSchematic(Plot plot, File schematicFile, Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    plugin.getLogger().warning("Could not determine schematic format for: " + schematicFile.getName());
                    return;
                }

                Clipboard clipboard;
                try (FileInputStream inputStream = new FileInputStream(schematicFile)) {
                    clipboard = format.getReader(inputStream).read();
                }

                // Safely translate the PlotSquared World into a FAWE World via Bukkit Adapter
                org.bukkit.World bukkitWorld = Bukkit.getWorld(plot.getWorldName());
                if (bukkitWorld == null) {
                    plugin.getLogger().warning("Cannot paste schematic: Bukkit world not loaded!");
                    return;
                }

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

                // Use the bottom-left coordinate of the plot (lowest X, Y, Z)
                Location bottomLoc = plot.getBottomAbs();
                BlockVector3 pastePos = BlockVector3.at(bottomLoc.getX(), bottomLoc.getY(), bottomLoc.getZ());

                // Execute the FAWE Paste
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    Operation operation = holder.createPaste(editSession)
                            .to(pastePos)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(operation);

                    plugin.getLogger().info("Schematic pasted successfully for player " + player.getName());

                    // Only NOW hand out the key — the plot is ready to be teleported to.
                    grantKeyOnMainThread(player);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("FAWE schematic paste failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Gives the player the creative key (if they don't already have it) and notifies them
     * that the plot is ready. Always runs on the main thread because it touches the inventory.
     */
    private void grantKeyOnMainThread(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            ensureKey(player);
            player.sendMessage(Component.text(
                    "Your Build World plot has been generated! Right-click your enchanted key to travel.",
                    NamedTextColor.GREEN
            ));
        });
    }

    private void ensureKey(Player player) {
        if (!plugin.getItemManager().hasCreativeKey(player)) {
            plugin.getItemManager().giveCreativeKey(player);
        }
    }


    public boolean teleportToHome(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerData(playerId);

        if (playerData == null || playerData.getAssignedPlotId() == null) {
            player.sendMessage(Component.text("You don't have a plot assigned.", NamedTextColor.RED));
            return false;
        }

        try {
            PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea(CREATIVE_PLOT_AREA_NAME, null);
            if (area == null) {
                 player.sendMessage(Component.text("The creative plot world isn't available.", NamedTextColor.RED));
                return false;
            }

            PlotId plotId = playerData.getAssignedPlotId();
            Plot plot = area.getPlot(plotId);
            if (plot == null) {
                player.sendMessage(Component.text("Your assigned plot could not be found.", NamedTextColor.RED));
                return false;
            }

            // Calculate center of plot for safe teleport
            Location top = plot.getTopAbs();
            Location bottom = plot.getBottomAbs();

            if (top != null && bottom != null) {
                double centerX = (top.getX() + bottom.getX()) / 2.0;
                double centerZ = bottom.getZ(); // Spawn on the plot border, facing it

                org.bukkit.World bukkitWorld = Bukkit.getWorld(plot.getWorldName());
                if (bukkitWorld != null) {
                    // Teleport to the center, finding the highest safe block (Y coordinate)
                    int highestY = bukkitWorld.getHighestBlockYAt((int) centerX, (int) centerZ);
                    org.bukkit.Location tpLoc = new org.bukkit.Location(bukkitWorld, centerX, highestY + 1, centerZ, 0, 0);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.teleport(tpLoc);
                        player.sendMessage(Component.text("Welcome to your private plot!", NamedTextColor.AQUA));
                    });
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            plugin.getLogger().warning("Error teleporting player to plot: " + e.getMessage());
            return false;
        }
    }

    public static String getCreativePlotWorldName() {
        return CREATIVE_PLOT_AREA_NAME;
    }
}