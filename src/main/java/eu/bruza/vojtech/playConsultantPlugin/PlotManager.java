package eu.bruza.vojtech.playConsultantPlugin;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.location.Location;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
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
     *
     * @param player The player to reward
     */
    public void rewardPlayerWithCreativePlot(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerData(playerId);

        // Check if they already have a plot assigned
        if (playerData != null && playerData.getAssignedPlotId() != null) {
            player.sendMessage(Component.text(
                    "You already have an assigned plot!",
                    NamedTextColor.AQUA
            ));
            return;
        }

        try {
            PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea(CREATIVE_PLOT_AREA_NAME, null);

            if (area == null) {
                plugin.getLogger().warning("Creative plot area '" + CREATIVE_PLOT_AREA_NAME + "' not found in PlotSquared!");
                player.sendMessage(Component.text(
                        "Error: The creative plot world is not configured. Contact staff.",
                        NamedTextColor.RED
                ));
                return;
            }

            // Find the next available plot ID
            PlotId nextAvailableId = null;
            for (int x = 0; x < 100; x++) {
                for (int y = 0; y < 100; y++) {
                    PlotId testId = PlotId.of(x, y);
                    Plot testPlot = area.getPlot(testId);
                    if (testPlot == null || !testPlot.hasOwner()) {
                        nextAvailableId = testId;
                        break;
                    }
                }
                if (nextAvailableId != null) break;
            }

            if (nextAvailableId == null) {
                plugin.getLogger().warning("No available plots found in creative_plot area!");
                player.sendMessage(Component.text(
                        "Error: No available plots. Contact staff.",
                        NamedTextColor.RED
                ));
                return;
            }

            Plot plot = area.getPlot(nextAvailableId);
            if (plot == null) {
                plugin.getLogger().warning("Failed to get plot at ID: " + nextAvailableId);
                player.sendMessage(Component.text(
                        "Error: Could not allocate a plot. Contact staff.",
                        NamedTextColor.RED
                ));
                return;
            }

            // 1. Claim the plot for the player
            boolean claimSuccess = false;
            try {
                // Try setOwner first (most direct method)
                plot.setOwner(player.getUniqueId());
                claimSuccess = true;
                plugin.getLogger().info("Claimed plot " + nextAvailableId + " for player " + player.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("setOwner failed, trying claim(): " + e.getMessage());
            }

            if (!claimSuccess) {
                player.sendMessage(Component.text(
                        "Error: Could not claim the plot. Contact staff.",
                        NamedTextColor.RED
                ));
                return;
            }

            // 2. Get the schematic file name from config
            String schematicName = plugin.getConfigManager().getSchematicName();
            File schematicFile = getSchematicFile(schematicName);

            // 3. Try to paste the schematic programmatically
            if (schematicFile.exists()) {
                tryPasteSchematic(plot, schematicFile, player);
            } else {
                plugin.getLogger().warning("Schematic not found at: " + schematicFile.getAbsolutePath());
                player.sendMessage(Component.text(
                        "§cWarning: The schematic file could not be found. Your plot is ready but empty.",
                        NamedTextColor.YELLOW
                ));
                teleportPlayerToPlot(plot, player);
            }

            // Store the plot ID in player data
            PlayerData data = plugin.getOrCreatePlayerData(playerId);
            data.setAssignedPlotId(nextAvailableId);
            plugin.getLogger().info("Stored plot ID " + nextAvailableId + " for player " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("Error rewarding player with creative plot: " + e.getMessage());
            plugin.getLogger().warning("Stack trace: " + e);
            player.sendMessage(Component.text(
                    "An error occurred while setting up your plot. Contact staff.",
                    NamedTextColor.RED
            ));
        }
    }

    /**
     * Gets the schematic file for the given schematic name.
     */
    private File getSchematicFile(String schematicName) {
        // Try multiple possible locations
        File[] possiblePaths = {
                new File("plugins/PlotSquared/schematics/" + schematicName + ".schem"),
                new File("plugins/PlotSquared/schematics/" + schematicName + ".schematic"),
                new File("schematics/" + schematicName + ".schem"),
                new File("schematics/" + schematicName + ".schematic"),
        };

        for (File path : possiblePaths) {
            if (path.exists()) {
                plugin.getLogger().info("Found schematic at: " + path.getAbsolutePath());
                return path;
            }
        }

        // Return the default path (even if it doesn't exist, for error message)
        return possiblePaths[0];
    }

    /**
     * Attempts to paste a schematic into a plot using FastAsyncWorldEdit.
     */
    private void tryPasteSchematic(Plot plot, File schematicFile, Player player) {
        try {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getLogger().info("Attempting to paste schematic: " + schematicFile.getAbsolutePath());

                    // Load the schematic file using WorldEdit
                    ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                    if (format == null) {
                        plugin.getLogger().warning("Could not determine schematic format for: " + schematicFile.getName());
                        teleportWithoutSchematic(plot, player);
                        return;
                    }

                    Clipboard clipboard;
                    try (var inputStream = new java.io.FileInputStream(schematicFile)) {
                        clipboard = format.getReader(inputStream).read();
                    }

                    if (clipboard == null) {
                        plugin.getLogger().warning("Failed to read clipboard from schematic file");
                        teleportWithoutSchematic(plot, player);
                        return;
                    }

                    // Get the plot world and paste location
                    Location plotCenter = plot.getTopAbs();
                    if (plotCenter == null) {
                        plugin.getLogger().warning("Could not get plot center location");
                        teleportWithoutSchematic(plot, player);
                        return;
                    }

                    String worldName = plotCenter.getWorldName();
                    org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);
                    if (bukkitWorld == null) {
                        plugin.getLogger().warning("Cannot paste schematic: world not loaded: " + worldName);
                        teleportWithoutSchematic(plot, player);
                        return;
                    }

                    World weWorld;
                    try {
                        weWorld = (World) plotCenter.getWorld();
                    } catch (ClassCastException castException) {
                        plugin.getLogger().warning("Cannot paste schematic: plot world is not a WorldEdit world: " + plotCenter.getWorld());
                        teleportWithoutSchematic(plot, player);
                        return;
                    }
                    int pasteX = plotCenter.getX();
                    int pasteY = plotCenter.getY();
                    int pasteZ = plotCenter.getZ();

                    com.sk89q.worldedit.math.BlockVector3 pastePos = com.sk89q.worldedit.math.BlockVector3.at(pasteX, pasteY, pasteZ);

                    // Paste the schematic
                    try (var editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                        ClipboardHolder holder = new ClipboardHolder(clipboard);
                        com.sk89q.worldedit.function.operation.Operations.complete(
                                holder.createPaste(editSession)
                                        .to(pastePos)
                                        .ignoreAirBlocks(false)
                                        .build()
                        );
                        plugin.getLogger().info("Schematic pasted successfully for player " + player.getName());
                    }

                    // Teleport player on main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        teleportPlayerToPlot(plot, player);
                        player.sendMessage(Component.text(
                                "§aYour plot has been generated! Welcome to the Build World.",
                                NamedTextColor.GREEN
                        ));
                    });

                } catch (Exception e) {
                    plugin.getLogger().warning("FAWE schematic paste failed: " + e.getMessage());
                    plugin.getLogger().warning("FAWE paste exception: " + e);
                    // Still teleport the player
                    teleportWithoutSchematic(plot, player);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Error scheduling schematic paste: " + e.getMessage());
            teleportWithoutSchematic(plot, player);
        }
    }

    /**
     * Teleports player without pasting schematic.
     */
    private void teleportWithoutSchematic(Plot plot, Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            teleportPlayerToPlot(plot, player);
            player.sendMessage(Component.text(
                    "Your plot is ready. You can start building!",
                    NamedTextColor.GREEN
            ));
        });
    }

    /**
     * Teleports a player to a plot location.
     */
    private void teleportPlayerToPlot(Plot plot, Player player) {
        try {
            // Calculate center from plot bounds
            try {
                Location top = plot.getTopAbs();
                Location bottom = plot.getBottomAbs();

                if (top != null && bottom != null) {
                    double centerX = (top.getX() + bottom.getX()) / 2.0;
                    double centerY = top.getY();
                    double centerZ = (top.getZ() + bottom.getZ()) / 2.0;

                    String worldName = top.getWorldName();
                    org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);

                    if (bukkitWorld != null) {
                        org.bukkit.Location bukkitLoc = new org.bukkit.Location(
                                bukkitWorld,
                                centerX,
                                centerY,
                                centerZ
                        );
                        player.teleport(bukkitLoc);
                        plugin.getLogger().info("Teleported " + player.getName() + " to plot center at " + plot.getId());
                        return;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().info("Could not calculate center from bounds: " + e.getMessage());
            }

            // Fallback: use the plot's top-left corner
            try {
                Location top = plot.getTopAbs();
                if (top != null) {
                    String worldName = top.getWorldName();
                    org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);

                    if (bukkitWorld != null) {
                        org.bukkit.Location bukkitLoc = new org.bukkit.Location(
                                bukkitWorld,
                                top.getX() + 1,
                                top.getY() + 1,
                                top.getZ() + 1
                        );
                        player.teleport(bukkitLoc);
                        plugin.getLogger().info("Teleported " + player.getName() + " to plot corner at " + plot.getId());
                        return;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not use getTopAbs: " + e.getMessage());
            }

            plugin.getLogger().warning("Could not teleport player to plot - no valid location found");
        } catch (Exception e) {
            plugin.getLogger().warning("Error teleporting player to plot: " + e.getMessage());
        }
    }

    /**
     * Teleports a player to their assigned plot.
     *
     * @param player The player to teleport
     * @return true if teleportation was successful, false otherwise
     */
    public boolean teleportToAssignedPlot(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerData(playerId);

        if (playerData == null || playerData.getAssignedPlotId() == null) {
            return false;
        }

        try {
            PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea(CREATIVE_PLOT_AREA_NAME, null);

            if (area == null) {
                plugin.getLogger().warning("Creative plot area not found!");
                return false;
            }

            PlotId plotId = playerData.getAssignedPlotId();
            Plot plot = area.getPlot(plotId);

            if (plot == null) {
                plugin.getLogger().warning("Plot not found: " + plotId);
                return false;
            }

            teleportPlayerToPlot(plot, player);
            player.sendMessage(Component.text(
                    "Welcome back to your plot!",
                    NamedTextColor.AQUA
            ));
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error teleporting player to plot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the creative plot world name used in this server.
     *
     * @return The creative plot world name
     */
    public static String getCreativePlotWorldName() {
        return CREATIVE_PLOT_AREA_NAME;
    }
}

