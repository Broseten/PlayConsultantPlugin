package eu.bruza.vojtech.playConsultantPlugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PapiExpansion extends PlaceholderExpansion {

    private final PlayConsultantPlugin plugin;

    public PapiExpansion(PlayConsultantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getPluginMeta().getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        // Correctly joins the List<String> of authors
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        // Safely fetches the injected Gradle version string
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required on PAPI 2.10.0+
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "";
        }

        if ("comments_made".equals(identifier)) {
            return String.valueOf(playerData.getCommentsMade());
        }

        if ("received_creative_key".equals(identifier)) {
            return playerData.hasReceivedCreativeKey() ? "✔" : "";
        }

        return null;
    }
}
