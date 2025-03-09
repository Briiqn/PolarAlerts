package dev.briiqn.polaralerts.alerts;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import dev.briiqn.polaralerts.PolarAlerts;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Listens for plugin messages containing Polar alerts
 */
public class AlertMessageListener implements PluginMessageListener {

    private final PolarAlerts plugin;

    public AlertMessageListener(PolarAlerts plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("PolarAlert")) {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);
            ByteArrayDataInput msgin = ByteStreams.newDataInput(msgbytes);
            String receivedSecretKey = msgin.readUTF();
            if (!receivedSecretKey.equals(plugin.getSecretKey())) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("Received alert with invalid secret key - ignoring");
                }
                return;
            }

            String serverName = msgin.readUTF();
            String eventType = msgin.readUTF();
            String playerName = msgin.readUTF();
            String playerUuid = msgin.readUTF();
            String checkType = msgin.readUTF();
            String checkName = msgin.readUTF();
            double vl = msgin.readDouble();
            String details = msgin.readUTF();
            String alertPermission = plugin.getConfig().getString("display.permission", "anticheat.alerts");
            if (plugin.getConfig().getBoolean("display.enabled", true) &&
                    plugin.getConfig().getStringList("display.alert-types").contains(eventType)) {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission(alertPermission))
                        .forEach(p -> sendClickableAlert(p, serverName, eventType, playerName, checkName, checkType, vl, details));
            }

            if (plugin.getConfig().getBoolean("display.console-log", true)) {
                String alertMessage = String.format("[%s] %s triggered %s (%s) - VL: %.1f",
                        serverName, playerName, checkName, eventType, vl);
                plugin.getLogger().info(alertMessage);
            }
        }
    }

    /**
     * Sends a clickable alert to a player
     */
    private void sendClickableAlert(Player player, String serverName, String eventType, String playerName,
                                    String checkName, String checkType, double vl, String details) {
        String format = plugin.getConfig().getString("display.formats." + eventType.toLowerCase(),
                "§c[%server%] §f%player% §7triggered §f%check% §7(%type%) §8[§f%vl%§8]");
        String message = format
                .replace("%server%", serverName)
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%type%", checkType)
                .replace("%vl%", String.format("%.1f", vl));
        TextComponent component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + serverName));
        String hoverText = plugin.getConfig().getString("display.hover-text",
                "§7Click to connect to §f%server%\n§7Player: §f%player%\n§7Check: §f%check%");

        hoverText = hoverText
                .replace("%server%", serverName)
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%type%", checkType)
                .replace("%vl%", String.format("%.1f", vl));
        if (details != null && !details.isEmpty()) {
            if (plugin.getConfig().getBoolean("display.show-details-in-hover", true)) {
                hoverText += "\n\n§7Details:\n§f" + details.replaceAll("<[^>]*>", "");
            }
        }

        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText).create()));
        player.spigot().sendMessage(component);
    }
}