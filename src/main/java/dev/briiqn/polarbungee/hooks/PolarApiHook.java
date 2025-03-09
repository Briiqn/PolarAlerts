package dev.briiqn.polarbungee.hooks;

import dev.briiqn.polarbungee.PolarBungee;
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

public class PolarApiHook implements Runnable {

    private final PolarBungee plugin;
    private PolarApi polarApi;
    private boolean connected = false;
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String ALERTS_CHANNEL = "polarbungee:alerts";
    private final List<String> registeredChannels = new ArrayList<>();
    private boolean useBungeeForward;
    private boolean useCustomForward;
    private String serverName;
    private final Map<String, List<String>> forwardTargets = new HashMap<>();

    public PolarApiHook(PolarBungee plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // This runs after Polar is completely loaded
            polarApi = PolarApiAccessor.access().get();

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
            plugin.getLogger().info("Successfully connected to Polar API and registered messaging channels");
        } catch (PolarNotLoadedException e) {
            plugin.getLogger().severe("Failed to connect to Polar API: " + e.getMessage());
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

        // Load forwarding configuration
        useBungeeForward = plugin.getConfig().getBoolean("forward.bungee.enabled", true);
        useCustomForward = plugin.getConfig().getBoolean("forward.custom.enabled", false);

        // Load forwarding targets (for BungeeCord forwarding)
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
            polarApi.events().repository().registerListener(DetectionAlertEvent.class, event -> {
                // Process on main thread for safety
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    handleDetectionEvent(event);
                });
            });
        }

        // Cloud detection events
        if (plugin.getConfig().getBoolean("alerts.cloud", true)) {
            polarApi.events().repository().registerListener(CloudDetectionEvent.class, event -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    handleCloudDetectionEvent(event);
                });
            });
        }

        // Mitigation events
        if (plugin.getConfig().getBoolean("alerts.mitigation", true)) {
            polarApi.events().repository().registerListener(MitigationEvent.class, event -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    handleMitigationEvent(event);
                });
            });
        }

        // Punishment events
        if (plugin.getConfig().getBoolean("alerts.punishment", true)) {
            polarApi.events().repository().registerListener(PunishmentEvent.class, event -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    handlePunishmentEvent(event);
                });
            });
        }
    }

    /**
     * Handles a detection event
     */
    private void handleDetectionEvent(DetectionAlertEvent event) {
        User user = event.user();
        Check check = event.check();
        String eventType = "DETECTION";
        String playerName = user.username();
        String playerUuid = user.uuid().toString();
        String checkType = check.type().name();
        String checkName = check.name();
        double vl = check.violationLevel();
        String details = event.details();

        // Forward using appropriate methods
        forwardAlert(eventType, playerName, playerUuid, checkType, checkName, vl, details);
    }

    /**
     * Handles a cloud detection event
     */
    private void handleCloudDetectionEvent(CloudDetectionEvent event) {
        User user = event.user();
        String eventType = "CLOUD";
        String playerName = user.username();
        String playerUuid = user.uuid().toString();
        String checkType = event.cloudCheckType().name();
        String checkName = "Cloud";
        double vl = 0.0;
        String details = event.details();

        // Forward using appropriate methods
        forwardAlert(eventType, playerName, playerUuid, checkType, checkName, vl, details);
    }

    /**
     * Handles a mitigation event
     */
    private void handleMitigationEvent(MitigationEvent event) {
        User user = event.user();
        Check check = event.check();
        String eventType = "MITIGATION";
        String playerName = user.username();
        String playerUuid = user.uuid().toString();
        String checkType = check.type().name();
        String checkName = check.name();
        double vl = check.violationLevel();
        String details = event.details();

        // Forward using appropriate methods
        forwardAlert(eventType, playerName, playerUuid, checkType, checkName, vl, details);
    }

    /**
     * Handles a punishment event
     */
    private void handlePunishmentEvent(PunishmentEvent event) {
        User user = event.user();
        String eventType = "PUNISHMENT";
        String playerName = user.username();
        String playerUuid = user.uuid().toString();
        String checkType = event.type().name();
        String checkName = event.reason();
        double vl = 0.0;
        String details = "";

        // Forward using appropriate methods
        forwardAlert(eventType, playerName, playerUuid, checkType, checkName, vl, details);
    }

    /**
     * Forwards an alert using configured methods
     */
    private void forwardAlert(String eventType, String playerName, String playerUuid,
                              String checkType, String checkName, double vl, String details) {
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
        List<String> targets = forwardTargets.getOrDefault(eventType, new ArrayList<>());
        if (plugin.getConfig().getBoolean("forward.bungee.forward-to-all", false)) {
            targets.add("ALL");
        }
        if (targets.isEmpty()) {
            return;
        }
        ByteArrayDataOutput mainOut = ByteStreams.newDataOutput();
        for (String target : targets) {
            // Reset main output for this target
            mainOut.writeUTF("Forward");
            mainOut.writeUTF(target);
            mainOut.writeUTF("PolarAlert");
            ByteArrayDataOutput payloadOut = ByteStreams.newDataOutput();
            payloadOut.writeUTF(serverName);
            payloadOut.writeUTF(eventType);
            payloadOut.writeUTF(playerName);
            payloadOut.writeUTF(playerUuid);
            payloadOut.writeUTF(checkType);
            payloadOut.writeUTF(checkName);
            payloadOut.writeDouble(vl);
            payloadOut.writeUTF(truncateString(details, 1000));
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
        // Prepare the plugin message data
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

    private String truncateString(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength) + "...";
    }


    public boolean isConnected() {
        return connected;
    }
    public PolarApi getPolarApi() {
        return polarApi;
    }
    public List<String> getRegisteredChannels() {
        return registeredChannels;
    }
}