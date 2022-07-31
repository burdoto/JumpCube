package de.kaleidox.jumpcube.chat;

import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.comroid.cmdr.spigot.MessageColorizer;

public final class Chat {
    private Chat() {
    }

    @Deprecated
    public static void message(CommandSender sender, MessageColorizer msgLevel, String format, Object... vars) {
        message(BukkitUtil.getPlayer(sender), msgLevel, format, vars);
    }

    @Deprecated
    public static void message(Player player, MessageColorizer msgLevel, String format, Object... vars) {
        player.sendMessage(msgLevel.makeMessage(format, vars));
    }

    @Deprecated
    public static void broadcast(MessageColorizer msgLevel, String format, Object... vars) {
        Bukkit.broadcastMessage(msgLevel.makeMessage(format, vars));
    }

    @Deprecated
    public static void broadcast(String permission, MessageColorizer msgLevel, String format, Object... vars) {
        Bukkit.broadcast(msgLevel.makeMessage(format, vars), permission);
    }
}
