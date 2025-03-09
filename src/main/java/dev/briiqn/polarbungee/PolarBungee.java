package dev.briiqn.polarbungee.helper;

import dev.briiqn.polarbungee.helper.alerts.AlertMessageListener;
import dev.briiqn.polarbungee.helper.hooks.PolarApiHook;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import top.polar.api.loader.LoaderApi;

@Plugin(name = "PolarBungee-Helper", version = "1.0")
@Author("briiqn")
@Dependency("PolarLoader")
public class PolarBungeeHelper extends JavaPlugin {

    private PolarApiHook polarApiHook;
    private AlertMessageListener messageListener;

    @Override
    public void onLoad() {
        // Save default config
        saveDefaultConfig();

        // Create API hook and register it with Polar's loader
        polarApiHook = new PolarApiHook(this);

        // Register with Polar's loader system
        LoaderApi.registerEnableCallback(polarApiHook);
    }

    @Override
    public void onEnable() {
        // Init message listener
        messageListener = new AlertMessageListener(this);

        // Register incoming plugin messaging channel
        if (getConfig().getBoolean("display.enabled", true)) {
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", messageListener);
        }

        getLogger().info("PolarBungee-Helper has been enabled! Waiting for Polar to initialize...");
    }

    @Override
    public void onDisable() {
        // Unregister incoming plugin messaging channel
        if (messageListener != null) {
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord", messageListener);
        }

        // Only unregister outgoing if we've registered (check if Polar API was loaded)
        if (polarApiHook != null && polarApiHook.isConnected()) {
            // Unregister outgoing channels
            for (String channel : polarApiHook.getRegisteredChannels()) {
                this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
                getLogger().info("Unregistered plugin messaging channel: " + channel);
            }
        }

        getLogger().info("PolarBungee-Helper has been disabled!");
    }

    /**
     * Gets the API hook
     */
    public PolarApiHook getApiHook() {
        return polarApiHook;
    }
}