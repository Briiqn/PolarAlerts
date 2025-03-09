package dev.briiqn.polarbungee;

import dev.briiqn.polarbungee.alerts.AlertMessageListener;
import dev.briiqn.polarbungee.hooks.PolarApiHook;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import top.polar.api.loader.LoaderApi;

@Plugin(name = "PolarBungee", version = "1.0")
@Author("briiqn")
@Dependency("PolarLoader")
public class PolarBungee extends JavaPlugin {

    private PolarApiHook polarApiHook;
    private AlertMessageListener messageListener;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        polarApiHook = new PolarApiHook(this);
        LoaderApi.registerEnableCallback(polarApiHook);
    }

    @Override
    public void onEnable() {
        messageListener = new AlertMessageListener(this);
        if (getConfig().getBoolean("display.enabled", true)) {
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", messageListener);
        }
        getLogger().info("PolarBungee has been enabled! Waiting for Polar to initialize...");
    }

    @Override
    public void onDisable() {
        if (messageListener != null) {
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord", messageListener);
        }

        if (polarApiHook != null && polarApiHook.isConnected()) {
            for (String channel : polarApiHook.getRegisteredChannels()) {
                this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
                getLogger().info("Unregistered plugin messaging channel: " + channel);
            }
        }

        getLogger().info("PolarBungee has been disabled!");
    }

    public PolarApiHook getApiHook() {
        return polarApiHook;
    }
}