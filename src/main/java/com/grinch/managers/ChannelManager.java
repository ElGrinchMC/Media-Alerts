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
import java.util.stream.Collectors;

public class ChannelManager {

    private final MediaAlertPlugin plugin;
    private final MessageManager messageManager;
    private final Map<String, ChannelInfo> channels; // <Abreviatura, ChannelInfo>
    private final File channelsFile;
    private FileConfiguration channelsConfig;

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
                return link; // Devuelve el link original si la plataforma no se reconoce
        }
    }

    public String getChannelPlatform(String shortName) {
        ChannelInfo channelInfo = channels.get(shortName);
        return channelInfo != null ? channelInfo.getPlatform() : null;
    }

    public String getChannelShortName(String playerName) {
        for (Map.Entry<String, ChannelInfo> entry : channels.entrySet()) {
            if (entry.getValue().getLink().contains(playerName)) {
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
            plugin.getLogger().severe("Error al guardar channels.yml: " + e.getMessage());
        }
    }

    public void sendAlert(String message, String link) {
        String platform = "";
        String playerName = "";
        // Obtener la plataforma y el nombre del jugador
        for (Map.Entry<String, ChannelInfo> entry : channels.entrySet()) {
            if (entry.getValue().getLink().equals(link)) {
                platform = entry.getValue().getPlatform();
                playerName = getPlayerNameFromLink(link);
                break;
            }
        }

        // Formatear el nombre de la plataforma para que la primera letra sea mayúscula
        if (!platform.isEmpty()) {
            platform = platform.substring(0, 1).toUpperCase() + platform.substring(1).toLowerCase();
        }

        // Reemplazar los placeholders en el mensaje
        String formattedPlatform = "&d" + platform + "&r";
        String finalMessage = message.replace("{platform}", formattedPlatform)
                                    .replace("{link}", link)
                                    .replace("{player}", playerName);

        // Crear un componente de texto base
        ComponentBuilder builder = new ComponentBuilder();

        //Añadir cada linea al builder
        for (String line : finalMessage.split("\n")) {
            builder.append(TextComponent.fromLegacyText(line)).append("\n");
        }
        
        // Eliminar el último salto de línea
        builder.removeComponent(builder.getCursor());

        // Configurar el evento de clic y hover
        builder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clic para ir al canal").create()));

        // Enviar el mensaje a todos los jugadores
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
            return ""; // O manejar de otra manera si el enlace no es de una plataforma conocida
        }
    }

    // Clase interna para almacenar la información del canal
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