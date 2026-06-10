package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlayConsultantPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("by Vojtech Bruza");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
