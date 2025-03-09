package dev.briiqn.polarbungee.alerts;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import dev.briiqn.polarbungee.PolarBungee;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Listens for plugin messages containing Polar alerts
 */
public class AlertMessageListener implements PluginMessageListener {

    private final PolarBungee plugin;

    public AlertMessageListener(PolarBungee plugin) {
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
                    .forEach(p -> sendAlert(p, serverName, eventType, playerName, checkName, checkType, vl, details));
            }
            if (plugin.getConfig().getBoolean("display.console-log", true)) {
                String alertMessage = String.format("[%s] %s triggered %s (%s) - VL: %.1f", 
                                serverName, playerName, checkName, eventType, vl);
                plugin.getLogger().info(alertMessage);
            }
        }
    }
    
    /**
     * Sends an alert to a player
     */
    private void sendAlert(Player player, String serverName, String eventType, String playerName, 
                          String checkName, String checkType, double vl, String details) {
        String format = plugin.getConfig().getString("display.formats." + eventType.toLowerCase(),
                "§c[%server%] §f%player% §7triggered §f%check% §7(%type%) §8[§f%vl%§8]");
        
        String message = format
                .replace("%server%", serverName)
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%type%", checkType)
                .replace("%vl%", String.format("%.1f", vl));
        
        player.sendMessage(message);
    }
}