package com.grinch.managers;

import com.grinch.MediaAlertPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChannelManager {

    private final MediaAlertPlugin plugin;
    private final MessageManager messageManager;
    private final Map<String, ChannelInfo> channels; // <Abbreviation, ChannelInfo>
    private final File channelsFile;
    private FileConfiguration channelsConfig;
    private final Map<String, Long> cooldowns = new HashMap<>(); // <PlayerName, LastUse>
    private static final long COOLDOWN_TIME = TimeUnit.MINUTES.toMillis(8); // 8 minutes cooldown

    public ChannelManager(MediaAlertPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.channels = new HashMap<>();
        this.channelsFile = new File(plugin.getDataFolder(), "channels.yml");
        this.channelsConfig = YamlConfiguration.loadConfiguration(channelsFile);
        loadChannels();
    }

    public void addChannel(String shortName, String platform, String link) {
        channels.put(shortName, new ChannelInfo(platform, link));
        saveChannels();
    }

    public void removeChannel(String shortName) {
        channels.remove(shortName);
        saveChannels();
    }

    public boolean doesChannelExist(String shortName) {
        return channels.containsKey(shortName);
    }

    public String getChannelLink(String shortName) {
        ChannelInfo channelInfo = channels.get(shortName);
        if (channelInfo == null) {
            return null;
        }
        String platform = channelInfo.getPlatform().toLowerCase();
        String link = channelInfo.getLink();

        switch (platform) {
            case "twitch":
                return "https://www.twitch.tv/" + link;
            case "youtube":
                return "https://www.youtube.com/channel/" + link;
            case "kick":
                return "https://www.kick.com/" + link;
            default:
                return link; // Returns the original link if the platform is not recognized
        }
    }

    public String getChannelPlatform(String shortName) {
        ChannelInfo channelInfo = channels.get(shortName);
        return channelInfo != null ? channelInfo.getPlatform() : null;
    }

    public String getChannelShortName(String playerName) {
        for (Map.Entry<String, ChannelInfo> entry : channels.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<String> getChannelShortNames() {
        return channels.keySet();
    }

    private void loadChannels() {
        if (channelsConfig.contains("channels")) {
            for (String shortName : channelsConfig.getConfigurationSection("channels").getKeys(false)) {
                String platform = channelsConfig.getString("channels." + shortName + ".platform");
                String link = channelsConfig.getString("channels." + shortName + ".link");
                channels.put(shortName, new ChannelInfo(platform, link));
            }
        }
    }

    public void saveChannels() {
        channelsConfig.set("channels", null);
        for (Map.Entry<String, ChannelInfo> entry : channels.entrySet()) {
            String shortName = entry.getKey();
            ChannelInfo channelInfo = entry.getValue();
            channelsConfig.set("channels." + shortName + ".platform", channelInfo.getPlatform());
            channelsConfig.set("channels." + shortName + ".link", channelInfo.getLink());
        }
        try {
            channelsConfig.save(channelsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving channels.yml: " + e.getMessage());
        }
    }

    public void sendAlert(String message, String link) {
        String platform = "";
        String playerName = "";
        // Get the platform and player name
        for (Map.Entry<String, ChannelInfo> entry : channels.entrySet()) {
            if (entry.getValue().getLink().equals(link)) {
                platform = entry.getValue().getPlatform();
                playerName = getPlayerNameFromLink(link);
                break;
            }
        }

        // Format the platform name to capitalize the first letter
        if (!platform.isEmpty()) {
            platform = platform.substring(0, 1).toUpperCase() + platform.substring(1).toLowerCase();
        }

        // Replace the placeholders in the message
        String formattedPlatform = "&d" + platform + "&r";
        String finalMessage = message.replace("{platform}", formattedPlatform)
                                    .replace("{link}", link)
                                    .replace("{player}", playerName);

        // Create a base text component
        ComponentBuilder builder = new ComponentBuilder();

        //Add each line to the builder
        for (String line : finalMessage.split("\n")) {
            builder.append(TextComponent.fromLegacyText(line)).append("\n");
        }

        // Remove the last line break
        builder.removeComponent(builder.getCursor());

        // Set the click and hover events
        builder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to go to the channel").create()));

        // Send the message to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(builder.create());
        }
    }

    private String getPlayerNameFromLink(String link) {
        if (link.startsWith("https://www.twitch.tv/")) {
            return link.substring("https://www.twitch.tv/".length());
        } else if (link.startsWith("https://www.youtube.com/channel/")) {
            return link.substring("https://www.youtube.com/channel/".length());
        } else if (link.startsWith("https://www.kick.com/")) {
            return link.substring("https://www.kick.com/".length());
        } else {
            return ""; // Or handle differently if the link is not from a known platform
        }
    }

    // Cooldown methods
    public void setCooldown(String playerName, long time) {
        cooldowns.put(playerName, time);
    }

    public boolean isOnCooldown(String playerName) {
        if (!cooldowns.containsKey(playerName)) {
            return false;
        }
        long lastUseTime = cooldowns.get(playerName);
        long currentTime = System.currentTimeMillis();
        return currentTime < lastUseTime + COOLDOWN_TIME;
    }

    public long getRemainingCooldown(String playerName) {
        if (!cooldowns.containsKey(playerName)) {
            return 0;
        }
        long lastUseTime = cooldowns.get(playerName);
        long currentTime = System.currentTimeMillis();
        long remainingTime = lastUseTime + COOLDOWN_TIME - currentTime;
        return TimeUnit.MILLISECONDS.toSeconds(remainingTime); // Returns the remaining time in seconds
    }

    // Inner class to store channel information
    private static class ChannelInfo {
        private final String platform;
        private final String link;

        public ChannelInfo(String platform, String link) {
            this.platform = platform;
            this.link = link;
        }

        public String getPlatform() {
            return platform;
        }

        public String getLink() {
            return link;
        }
    }
}
