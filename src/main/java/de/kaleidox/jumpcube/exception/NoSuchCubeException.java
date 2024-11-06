package de.kaleidox.jumpcube.exception;

import com.ampznetwork.libmod.api.util.chat.BroadcastType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.comroid.api.func.util.Command;

public final class NoSuchCubeException extends Command.Error {
    public NoSuchCubeException(Player selectForPlayer) {
        super("Could not auto-select cube for player: " + selectForPlayer.getDisplayName() + "" +
              "\nPlease use " + ChatColor.LIGHT_PURPLE + "/jc select <Name>" +
              BroadcastType.ERROR.getTextColor() + " to select a cube.");
    }

    public NoSuchCubeException(String name) {
        super("No cube with name " + ChatColor.BLUE + name + BroadcastType.ERROR.getTextColor() + " could be found.");
    }
}
