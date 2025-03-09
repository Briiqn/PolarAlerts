package dev.briiqn.polaralerts.hooks;

import dev.briiqn.polaralerts.PolarAlerts;
import dev.briiqn.polaralerts.alerts.AlertKey;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import top.polar.api.PolarApi;
import top.polar.api.PolarApiAccessor;
import top.polar.api.check.Check;
import top.polar.api.exception.PolarNotLoadedException;
import top.polar.api.user.User;
import top.polar.api.user.event.CloudDetectionEvent;
import top.polar.api.user.event.DetectionAlertEvent;
import top.polar.api.user.event.MitigationEvent;
import top.polar.api.user.event.PunishmentEvent;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles interaction with the Polar API
 */
public class PolarApiHook implements Runnable {

    private final PolarAlerts plugin;
    private PolarApi polarApi;
    private boolean connected = false;
    private boolean initialized = false;
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String ALERTS_CHANNEL = "polar:alerts";
    private final List<String> registeredChannels = new ArrayList<>();
    private boolean useBungeeForward;
    private boolean useCustomForward;
    private String serverName;
    private final Map<String, List<String>> forwardTargets = new HashMap<>();
    private final Map<AlertKey, String> lastAlerts = new HashMap<>();
    private final Map<AlertKey, Integer> mitigationCounter = new HashMap<>();
    private int mitigationAlertFrequency;

    public PolarApiHook(PolarAlerts plugin) {
        this.plugin = plugin;
    }

    /**
     * Tries to connect to the Polar API
     *
     * @return true if successful, false otherwise
     */
    public boolean tryConnectToApi() {
        if (connected) {
            return true; // Already connected
        }

        try {
            // Try to get the Polar API
            polarApi = PolarApiAccessor.access().get();

            if (polarApi != null) {
                plugin.getLogger().info("Successfully connected to Polar API");

                // Register with Polar's loader system to complete initialization
                top.polar.api.loader.LoaderApi.registerEnableCallback(this);
                return true;
            }
        } catch (PolarNotLoadedException e) {
            // Not loaded yet, will retry
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Polar API not loaded yet, will retry...");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error accessing Polar API: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public void run() {
        try {
            // This will be called by Polar's loader after Polar is fully loaded

            // Load configuration
            loadConfig();

            // Register plugin messaging channels
            if (useBungeeForward) {
                plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
                registeredChannels.add(BUNGEE_CHANNEL);
            }

            if (useCustomForward) {
                plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ALERTS_CHANNEL);
                registeredChannels.add(ALERTS_CHANNEL);
            }

            // Register listeners for Polar events
            registerEventListeners();

            connected = true;
            initialized = true;
            plugin.getLogger().info("Successfully initialized Polar API hook and registered messaging channels");
        } catch (Exception e) {
            plugin.getLogger().severe("Error initializing Polar API: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads configuration for this hook
     */
    private void loadConfig() {
        // Load server name
        serverName = plugin.getConfig().getString("server-name", "unknown");

        // Try to get from Bukkit if not set
        if (serverName.equals("unknown")) {
            String bukkitName = Bukkit.getServerId();
            if (bukkitName != null && !bukkitName.isEmpty()) {
                serverName = bukkitName;
            }
        }
        useBungeeForward = plugin.getConfig().getBoolean("forward.bungee.enabled", true);
        useCustomForward = plugin.getConfig().getBoolean("forward.custom.enabled", false);
        mitigationAlertFrequency = Math.max(1, plugin.getConfig().getInt("alerts.mitigation-frequency", 1));
        if (mitigationAlertFrequency > 1) {
            plugin.getLogger().info("Mitigation alerts will be sent every " + mitigationAlertFrequency + " mitigations");
        }
        if (useBungeeForward) {
            ConfigurationSection targetsConfig = plugin.getConfig().getConfigurationSection("forward.bungee.targets");
            if (targetsConfig != null) {
                for (String key : targetsConfig.getKeys(false)) {
                    List<String> servers = targetsConfig.getStringList(key);
                    forwardTargets.put(key.toUpperCase(), servers);
                }
            }
        }
    }

    /**
     * Registers event listeners with the Polar API
     */
    private void registerEventListeners() {
        // Detection events
        if (plugin.getConfig().getBoolean("alerts.detection", true)) {
            polarApi.events().repository().registerListener(DetectionAlertEvent.class, this::handleDetectionEvent);
        }

        // Cloud detection events
        if (plugin.getConfig().getBoolean("alerts.cloud", true)) {
            polarApi.events().repository().registerListener(CloudDetectionEvent.class, this::handleCloudDetectionEvent);
        }

        // Mitigation events
        if (plugin.getConfig().getBoolean("alerts.mitigation", true)) {
            polarApi.events().repository().registerListener(MitigationEvent.class, this::handleMitigationEvent);
        }

        // Punishment events
        if (plugin.getConfig().getBoolean("alerts.punishment", true)) {
            polarApi.events().repository().registerListener(PunishmentEvent.class, this::handlePunishmentEvent);
        }
    }

    /**
     * Handles a detection event
     */
    private void handleDetectionEvent(DetectionAlertEvent event) {
        // Process on main thread for safety
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                User user = event.user();
                Check check = event.check();

                forwardAlert(
                        "DETECTION",
                        user.username(),
                        user.uuid().toString(),
                        check.type().name(),
                        check.name(),
                        check.violationLevel(),
                        event.details()
                );

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Forwarded detection alert: " + user.username() + " - " + check.name());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error handling detection event: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Handles a cloud detection event
     */
    private void handleCloudDetectionEvent(CloudDetectionEvent event) {
        // Process on main thread for safety
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                User user = event.user();

                forwardAlert(
                        "CLOUD",
                        user.username(),
                        user.uuid().toString(),
                        event.cloudCheckType().name(),
                        "Cloud",
                        0.0,
                        event.details()
                );

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Forwarded cloud detection alert: " + user.username() + " - " + event.cloudCheckType().name());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error handling cloud detection event: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Handles a mitigation event
     */
    private void handleMitigationEvent(MitigationEvent event) {
        // Process on main thread for safety
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                User user = event.user();
                Check check = event.check();

                // Create key for this mitigation
                AlertKey mitigationKey = new AlertKey(serverName, user.username(), check.name());

                // Increment the mitigation counter
                int counter = mitigationCounter.getOrDefault(mitigationKey, 0) + 1;
                mitigationCounter.put(mitigationKey, counter);

                // Only forward if we've reached the configured frequency
                if (counter >= mitigationAlertFrequency) {
                    // Reset the counter
                    mitigationCounter.put(mitigationKey, 0);

                    forwardAlert(
                            "MITIGATION",
                            user.username(),
                            user.uuid().toString(),
                            check.type().name(),
                            check.name(),
                            check.violationLevel(),
                            event.details()
                    );

                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Forwarded mitigation alert: " + user.username() + " - " + check.name());
                    }
                } else if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Skipped mitigation alert: " + user.username() + " - " + check.name() +
                            " (" + counter + "/" + mitigationAlertFrequency + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error handling mitigation event: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Handles a punishment event
     */
    private void handlePunishmentEvent(PunishmentEvent event) {
        // Process on main thread for safety
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                User user = event.user();

                forwardAlert(
                        "PUNISHMENT",
                        user.username(),
                        user.uuid().toString(),
                        event.type().name(),
                        event.reason(),
                        0.0,
                        ""
                );

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Forwarded punishment alert: " + user.username() + " - " + event.type().name());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error handling punishment event: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Forwards an alert using configured methods
     */
    private void forwardAlert(String eventType, String playerName, String playerUuid,
                              String checkType, String checkName, double vl, String details) {

        // Create a key for this alert
        AlertKey alertKey = new AlertKey(serverName, playerName, checkName);

        // Check if this is a duplicate alert (same player, server, check, and VL up to first decimal place)
        String currentVlString = String.format("%.1f", vl);

        if (lastAlerts.containsKey(alertKey)) {
            String lastVlString = lastAlerts.get(alertKey);

            if (lastVlString.equals(currentVlString)) {
                // This is a duplicate alert with the same VL (up to first decimal), ignore it
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Ignored duplicate alert: " + playerName + " - " + checkName +
                            " - VL: " + currentVlString);
                }
                return;
            }
        }

        // Update the last alert for this key
        lastAlerts.put(alertKey, currentVlString);

        // Use BungeeCord forwarding if enabled
        if (useBungeeForward) {
            forwardViaBungee(eventType, playerName, playerUuid, checkType, checkName, vl, details);
        }

        // Use custom channel if enabled
        if (useCustomForward) {
            forwardViaCustomChannel(eventType, playerName, playerUuid, checkType, checkName, vl, details);
        }
    }

    /**
     * Forwards an alert via BungeeCord's plugin messaging (compatible with vanilla BungeeCord)
     */
    private void forwardViaBungee(String eventType, String playerName, String playerUuid,
                                  String checkType, String checkName, double vl, String details) {
        // Get target servers for this event type
        List<String> targets = forwardTargets.getOrDefault(eventType, new ArrayList<>());

        // Add default "ALL" target if configured
        if (plugin.getConfig().getBoolean("forward.bungee.forward-to-all", true)) {
            targets.add("ALL");
        }

        // If no targets, no need to forward
        if (targets.isEmpty()) {
            return;
        }

        // For each target, create a Forward message
        for (String target : targets) {
            // Prepare the plugin message data
            ByteArrayDataOutput mainOut = ByteStreams.newDataOutput();
            mainOut.writeUTF("Forward");
            mainOut.writeUTF(target);
            mainOut.writeUTF("PolarAlert");

            // Prepare the payload
            ByteArrayDataOutput payloadOut = ByteStreams.newDataOutput();
            payloadOut.writeUTF(serverName);
            payloadOut.writeUTF(eventType);
            payloadOut.writeUTF(playerName);
            payloadOut.writeUTF(playerUuid);
            payloadOut.writeUTF(checkType);
            payloadOut.writeUTF(checkName);
            payloadOut.writeDouble(vl);
            payloadOut.writeUTF(truncateString(details, 1000));

            // Write the payload to the main output
            byte[] payload = payloadOut.toByteArray();
            mainOut.writeShort(payload.length);
            mainOut.write(payload);

            // Send via any online player
            sendPluginMessage(BUNGEE_CHANNEL, mainOut.toByteArray());
        }
    }

    /**
     * Forwards an alert via custom channel (for custom BungeeCord plugins)
     */
    private void forwardViaCustomChannel(String eventType, String playerName, String playerUuid,
                                         String checkType, String checkName, double vl, String details) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(serverName);
        out.writeUTF(eventType);
        out.writeUTF(playerName);
        out.writeUTF(playerUuid);
        out.writeUTF(checkType);
        out.writeUTF(checkName);
        out.writeDouble(vl);
        out.writeUTF(truncateString(details, 1000));

        sendPluginMessage(ALERTS_CHANNEL, out.toByteArray());
    }

    /**
     * Sends a plugin message using any online player
     */
    private void sendPluginMessage(String channel, byte[] data) {
        // Find an online player to send the message through
        if (!plugin.getServer().getOnlinePlayers().isEmpty()) {
            Player player = plugin.getServer().getOnlinePlayers().iterator().next();
            player.sendPluginMessage(plugin, channel, data);
        } else if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().warning("Could not forward alert: no players online");
        }
    }

    /**
     * Truncates a string to a maximum length
     */
    private String truncateString(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength) + "...";
    }

    /**
     * Checks if connected to the Polar API
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Checks if fully initialized with event listeners
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets the list of registered channels
     */
    public List<String> getRegisteredChannels() {
        return registeredChannels;
    }
}