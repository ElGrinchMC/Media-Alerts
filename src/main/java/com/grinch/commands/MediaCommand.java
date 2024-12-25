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
            sender.sendMessage(ChatColor.RED + "Usage: /media <add/remove/alert>");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            // /media add <abbreviation> <platform> <link>
            if (!sender.hasPermission("media.add")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /media add <abbreviation> <platform> <link>");
                return true;
            }
            String shortName = args[1];
            String platform = args[2].toLowerCase();
            String link = args[3];

            if (!platform.equalsIgnoreCase("twitch") && !platform.equalsIgnoreCase("youtube") && !platform.equalsIgnoreCase("kick")) {
                sender.sendMessage(ChatColor.RED + "Invalid platform. Must be 'twitch', 'youtube' or 'kick'.");
                return true;
            }

            if (channelManager.doesChannelExist(shortName)) {
                sender.sendMessage(ChatColor.RED + "A channel with that abbreviation already exists.");
                return true;
            }

            // Remove the base URL if present
            link = link.replace("https://www.twitch.tv/", "")
                    .replace("https://www.youtube.com/channel/", "")
                    .replace("https://www.kick.com/", "");

            channelManager.addChannel(shortName, platform, link);
            sender.sendMessage(ChatColor.GREEN + "Channel " + shortName + " (" + platform + ") added with link: " + link);
            return true;

        } else if (args[0].equalsIgnoreCase("remove")) {
            // /media remove <abbreviation>
            if (!sender.hasPermission("media.remove")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /media remove <abbreviation>");
                return true;
            }
            String shortName = args[1];

            if (!channelManager.doesChannelExist(shortName)) {
                sender.sendMessage(ChatColor.RED + "There is no channel with that abbreviation.");
                return true;
            }

            channelManager.removeChannel(shortName);
            sender.sendMessage(ChatColor.GREEN + "Channel " + shortName + " removed.");
            return true;

        } else if (args[0].equalsIgnoreCase("alert")) {
            // /media alert <abbreviation>
            if (!sender.hasPermission("media.alert")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            String shortName;
            String playerName;
            long currentTime = System.currentTimeMillis();
            if (args.length < 2) {
                // If no abbreviation is specified, assume the player is referring to themselves
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must specify an abbreviation from the console.");
                    return true;
                }
                Player player = (Player) sender;
                playerName = player.getName();
                shortName = channelManager.getChannelShortName(playerName);
                if (shortName == null) {
                    sender.sendMessage(ChatColor.RED + "You don't have a channel associated with your name. Use /media alert <abbreviation>.");
                    return true;
                }
            } else {
                // If abbreviation is specified, use that
                shortName = args[1];
                if (sender.hasPermission("media.alert.others")) {
                    // If the player has the media.alert.others permission, they can specify the player
                    if (args.length > 2) {
                        playerName = args[2];
                    } else {
                        playerName = sender.getName(); // Default value if not specified
                    }
                } else {
                    // If the player doesn't have the permission, assume they are referring to themselves
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "You must specify an abbreviation from the console.");
                        return true;
                    }
                    Player player = (Player) sender;
                    playerName = player.getName();
                }
            }
            if (!channelManager.doesChannelExist(shortName)) {
                sender.sendMessage(ChatColor.RED + "There is no channel with that abbreviation.");
                return true;
            }
            
            // Check cooldown
            if (channelManager.isOnCooldown(playerName)) {
                long remainingTime = channelManager.getRemainingCooldown(playerName);
                sender.sendMessage(ChatColor.RED + "You need to wait " + remainingTime + " seconds before using this command again.");
                return true;
            }

            String link = channelManager.getChannelLink(shortName);
            String platform = channelManager.getChannelPlatform(shortName);

            String formattedMessage = messageManager.getFormattedMessage(playerName, platform, link);
            channelManager.sendAlert(formattedMessage, link);
            
            channelManager.setCooldown(playerName, currentTime);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /media <add/remove/alert>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            //Suggestions for the first argument ("/media <subcommand>")
            StringUtil.copyPartialMatches(args[0], Arrays.asList("add", "remove", "alert"), completions);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("add")) {
            //Suggestions for the second argument and later
            if (args.length == 2) {
                // Suggestion for the abbreviation (second argument)
                if(sender.hasPermission("media.add")){
                    completions.add("<abbreviation>");
                }
            } else if (args.length == 3) {
                // Suggestion for the platform (third argument)
                if(sender.hasPermission("media.add")){
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("twitch", "youtube", "kick"), completions);
                }
            } else if (args.length == 4) {
                // Suggestion for the stream_nick (fourth argument)
                if(sender.hasPermission("media.add")){
                    completions.add("<stream_nick>");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Suggestions for the second argument ("/media remove <abbreviation>")
            if (sender.hasPermission("media.remove")) {
                StringUtil.copyPartialMatches(args[1], channelManager.getChannelShortNames(), completions);
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("alert")) {
            // Suggestions for the second argument ("/media alert <abbreviation>")
            if (sender.hasPermission("media.alert")) {
                StringUtil.copyPartialMatches(args[1], channelManager.getChannelShortNames(), completions);
            }
            if (sender.hasPermission("media.alert.others") && args.length == 3) {
                // Suggestion for the player name (third argument)
                for (Player onlinePlayer : sender.getServer().getOnlinePlayers()) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }
}
