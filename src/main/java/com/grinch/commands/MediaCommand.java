package com.grinch.commands;

import com.grinch.managers.ChannelManager;
import com.grinch.managers.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MediaCommand implements CommandExecutor, TabCompleter {

    private final ChannelManager channelManager;
    private final MessageManager messageManager;

    public MediaCommand(ChannelManager channelManager, MessageManager messageManager) {
        this.channelManager = channelManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Use: /media <add/remove/alert>");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            // /media add <abreviatura> <plataforma> <link>
            if (!sender.hasPermission("media.add")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Use: /media add <nick> <platform> <service_username>");
                return true;
            }
            String shortName = args[1].toLowerCase();
            String platform = args[2].toLowerCase();
            String link = args[3];

            if (!platform.equalsIgnoreCase("twitch") && !platform.equalsIgnoreCase("youtube") && !platform.equalsIgnoreCase("kick")) {
                sender.sendMessage(ChatColor.RED + "Invalid platform. Must be 'twitch', 'youtube', or 'kick'.");
                return true;
            }

            if (channelManager.doesChannelExist(shortName)) {
                sender.sendMessage(ChatColor.RED + "There is already a channel with this nickname.");
                return true;
            }

            // Eliminar la URL base del enlace si está presente
            link = link.replace("https://www.twitch.tv/", "")
                    .replace("https://www.youtube.com/channel/", "")
                    .replace("https://www.kick.com/", "");

            channelManager.addChannel(shortName, platform, link);
            sender.sendMessage(ChatColor.GREEN + "Channel " + shortName + " (" + platform + ") added with link: " + link);
            return true;

        } else if (args[0].equalsIgnoreCase("remove")) {
            // /media remove <abreviatura>
            if (!sender.hasPermission("media.remove")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Use: /media remove <nick>");
                return true;
            }
            String shortName = args[1].toLowerCase();

            if (!channelManager.doesChannelExist(shortName)) {
                sender.sendMessage(ChatColor.RED + "There is no channel with that nickname.");
                return true;
            }

            channelManager.removeChannel(shortName);
            sender.sendMessage(ChatColor.GREEN + "Channel " + shortName + " removed.");
            return true;

        } else if (args[0].equalsIgnoreCase("alert")) {
            // /media alert <abreviatura>
            if (!sender.hasPermission("media.alert")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            String shortName;
            String playerName;
            if (args.length < 2) {
                // Si no se especifica la abreviatura, se asume que se refiere a sí mismo
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must specify a nickname from the console.");
                    return true;
                }
                Player player = (Player) sender;
                playerName = player.getName();
                shortName = channelManager.getChannelShortName(playerName);
                if (shortName == null) {
                    sender.sendMessage(ChatColor.RED + "You do not have a channel associated with your nickname. Use /media alert <nick>.");
                    return true;
                }
            } else {
                // Si se especifica la abreviatura, se usa esa
                shortName = args[1].toLowerCase();
                if (sender.hasPermission("media.alert.others")) {
                    // Si tiene el permiso media.alert.others, puede especificar el jugador
                    if (args.length > 2) {
                        playerName = args[2];
                    } else {
                        playerName = sender.getName(); // Valor predeterminado si no se especifica
                    }
                } else {
                    // Si no tiene el permiso, se asume que se refiere a sí mismo
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "You must specify a nickname from the console.");
                        return true;
                    }
                    Player player = (Player) sender;
                    playerName = player.getName();
                }
            }

            if (!channelManager.doesChannelExist(shortName)) {
                sender.sendMessage(ChatColor.RED + "There is no channel with that nick.");
                return true;
            }

            String link = channelManager.getChannelLink(shortName);
            String platform = channelManager.getChannelPlatform(shortName);

            String formattedMessage = messageManager.getFormattedMessage(playerName, platform, link);
            channelManager.sendAlert(formattedMessage, link);

            return true;
        }

        sender.sendMessage(ChatColor.RED + "Use: /media <add/remove/alert>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            //Sugerencias para el primer argumento ("/media <subcomando>")
            StringUtil.copyPartialMatches(args[0], Arrays.asList("add", "remove", "alert"), completions);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("add")) {
            //Sugerencias para el segundo argumento y posteriores
            if (args.length == 2) {
                // Sugerencia para la abreviatura (segundo argumento)
                if(sender.hasPermission("media.add")){
                    completions.add("<nick>");
                }
            } else if (args.length == 3) {
                // Sugerencia para la plataforma (tercer argumento)
                if(sender.hasPermission("media.add")){
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("twitch", "youtube", "kick"), completions);
                }
            } else if (args.length == 4) {
                // Sugerencia para el enlace (cuarto argumento)
                if(sender.hasPermission("media.add")){
                    completions.add("<link>");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Sugerencias para el segundo argumento ("/media remove <abreviatura>")
            if (sender.hasPermission("media.remove")) {
                StringUtil.copyPartialMatches(args[1], channelManager.getChannelShortNames(), completions);
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("alert")) {
            // Sugerencias para el segundo argumento ("/media alert <abreviatura>")
            if (sender.hasPermission("media.alert")) {
                StringUtil.copyPartialMatches(args[1], channelManager.getChannelShortNames(), completions);
            }
            if (sender.hasPermission("media.alert.others") && args.length == 3) {
                // Sugerencia para el nombre del jugador (tercer argumento)
                for (Player onlinePlayer : sender.getServer().getOnlinePlayers()) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }
}