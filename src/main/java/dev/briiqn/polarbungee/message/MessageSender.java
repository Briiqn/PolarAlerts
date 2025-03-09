package dev.briiqn.polarbungee.message;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.briiqn.polarbungee.PolarBungee;
import dev.briiqn.polarbungee.alerts.AlertType;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;


public class MessageSender {
    public static final String CHANNEL_NAME = "polarbungee:alert";
    private final PolarBungee plugin;
    public MessageSender(PolarBungee plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends an alert to BungeeCord
     *
     * @param alertType the type of alert
     * @param playerName the name of the player
     * @param playerUuid the UUID of the player
     * @param checkType the check type
     * @param checkName the check name
     * @param vl the violation level
     * @param details the alert details
     */
    public void sendAlert(AlertType alertType, String playerName, UUID playerUuid,
                          String checkType, String checkName, double vl, String details) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(plugin.getServer().getServerName());
        out.writeUTF(alertType.name());
        out.writeUTF(playerName);
        out.writeUTF(playerUuid.toString());
        out.writeUTF(checkType);
        out.writeUTF(checkName);
        out.writeDouble(vl);

        // Write details (limit to prevent oversized packets)
        String truncatedDetails = truncateString(details, 1000);
        out.writeUTF(truncatedDetails);

        // Get any online player to send the plugin message
        Player sender = getRandomPlayer();
        if (sender != null) {
            sender.sendPluginMessage(plugin, CHANNEL_NAME, out.toByteArray());
        } else {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Could not forward alert: no online players to send plugin message");
            }
        }
    }

    private Player getRandomPlayer() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return null;
        }
        return Bukkit.getOnlinePlayers().iterator().next();
    }

    /**
     * Truncates a string to a maximum length
     *
     * @param input the input string
     * @param maxLength the maximum length
     * @return the truncated string
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
}