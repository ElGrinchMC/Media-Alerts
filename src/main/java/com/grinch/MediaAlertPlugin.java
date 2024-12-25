package com.grinch;

import com.grinch.commands.MediaCommand;
import com.grinch.managers.ChannelManager;
import com.grinch.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

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

        getLogger().info("MediaAlert plugin enabled!");
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