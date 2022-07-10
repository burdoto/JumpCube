package de.kaleidox.jumpcube.chat;

import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Chat {
    private Chat() {
    }

    public static void message(CommandSender sender, MessageLevel msgLevel, String format, Object... vars) {
        message(BukkitUtil.getPlayer(sender), msgLevel, format, vars);
    }

    public static void message(Player player, MessageLevel msgLevel, String format, Object... vars) {
        player.sendMessage(prefix() + makeMessage(msgLevel, format, vars));
    }

    public static void broadcast(MessageLevel msgLevel, String format, Object... vars) {
        Bukkit.broadcastMessage(prefix() + makeMessage(msgLevel, format, vars));
    }

    public static void broadcast(String permission, MessageLevel msgLevel, String format, Object... vars) {
        Bukkit.broadcast(prefix() + makeMessage(msgLevel, format, vars), permission);
    }

    private static String prefix() {
        return ChatColor.DARK_GRAY + "[" +
                ChatColor.BLUE + "JumpCube" +
                ChatColor.DARK_GRAY + "] ";
    }

    public static String makeMessage(MessageLevel msgLevel, String format, Object... vars) {
        return msgLevel.chatColor + String.format(format, (Object[]) formatStrings(msgLevel, vars));
    }

    private static String[] formatStrings(MessageLevel msgLevel, Object[] vars) {
        String[] strings = new String[vars.length];

        for (int i = 0; i < vars.length; i++)
            strings[i] = msgLevel.varColor + String.valueOf(vars[i]) + msgLevel.chatColor;

        return strings;
    }
}
