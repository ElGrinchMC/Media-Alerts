package com.grinch.managers;

import com.grinch.MediaAlertPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageManager {

    private final MediaAlertPlugin plugin;

    public MessageManager(MediaAlertPlugin plugin) {
        this.plugin = plugin;
    }

    public String getFormattedMessage(String playerName, String platform, String shortName) {
        FileConfiguration config = plugin.getConfig();
        String message = config.getString("message");
        message = message.replace("{player}", playerName);
        message = message.replace("{platform}", platform);
        message = message.replace("{link}", shortName);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}