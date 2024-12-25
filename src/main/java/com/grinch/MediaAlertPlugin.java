package com.grinch;

import com.grinch.commands.MediaCommand;
import com.grinch.managers.ChannelManager;
import com.grinch.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

public class MediaAlertPlugin extends JavaPlugin {

    private ChannelManager channelManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        // Inicializar config.yml y channels.yml
        saveDefaultConfig();
        saveResource("channels.yml", false);

        messageManager = new MessageManager(this);
        channelManager = new ChannelManager(this, messageManager);

        // Registrar comandos
        getCommand("media").setExecutor(new MediaCommand(channelManager, messageManager));
        getCommand("media").setTabCompleter(new MediaCommand(channelManager, messageManager));

        // Mensaje de inicio con caja de texto
        getLogger().info(ChatColor.GREEN + "---------------------------------------");
        getLogger().info(ChatColor.AQUA + "         MediaAlert Plugin" + ChatColor.RESET);
        getLogger().info("");
        getLogger().info(ChatColor.YELLOW + "       Version: " + getDescription().getVersion());
        getLogger().info(ChatColor.YELLOW + "       Author: " + ChatColor.AQUA + "@ELGrinchMC");
        getLogger().info("");
        getLogger().info(ChatColor.GREEN + "      Plugin enabled successfully!");
        getLogger().info(ChatColor.GREEN + "---------------------------------------");
    }

    @Override
    public void onDisable() {
        // Guardar la lista de canales al archivo
        channelManager.saveChannels();
        getLogger().info("MediaAlert plugin disabled!");
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
}
