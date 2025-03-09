package dev.briiqn.polaralerts;

import dev.briiqn.polaralerts.alerts.AlertMessageListener;
import dev.briiqn.polaralerts.hooks.PolarApiHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bukkit.scheduler.BukkitTask;

@Plugin(name = "PolarAlerts", version = "1.0")
@Author("briiqn")
public class PolarAlerts extends JavaPlugin {

    private PolarApiHook polarApiHook;
    private AlertMessageListener messageListener;
    private boolean listenOnlyMode;
    private BukkitTask polarRetryTask;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Check if we're in listen-only mode
        listenOnlyMode = getConfig().getBoolean("listen-only-mode", false);

        messageListener = new AlertMessageListener(this);

        if (getConfig().getBoolean("display.enabled", true)) {
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", messageListener);
            getLogger().info("Registered incoming message channel - will display alerts");
        }

        if (!listenOnlyMode) {
            polarApiHook = new PolarApiHook(this);
            polarRetryTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (polarApiHook.tryConnectToApi()) {
                    if (polarRetryTask != null) {
                        polarRetryTask.cancel();
                    }
                }
            }, 20L, 20L);

            getLogger().info("Waiting for Polar API to become available...");
        } else {
            getLogger().info("PolarAlerts enabled in listen-only mode");
        }
    }

    @Override
    public void onDisable() {
        if (polarRetryTask != null) {
            polarRetryTask.cancel();
            polarRetryTask = null;
        }
        if (messageListener != null) {
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord", messageListener);
        }
        if (!listenOnlyMode && polarApiHook != null && polarApiHook.isConnected()) {
            // Unregister outgoing channels
            for (String channel : polarApiHook.getRegisteredChannels()) {
                this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
                getLogger().info("Unregistered plugin messaging channel: " + channel);
            }
        }

        getLogger().info("PolarAlerts has been disabled!");
    }


    public PolarApiHook getApiHook() {
        return polarApiHook;
    }

    public boolean isListenOnlyMode() {
        return listenOnlyMode;
    }
}