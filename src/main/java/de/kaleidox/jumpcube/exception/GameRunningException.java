package de.kaleidox.jumpcube.exception;

import org.comroid.cmdr.spigot.InnerCommandException;
import org.comroid.cmdr.spigot.SpigotCmdr;

public final class GameRunningException extends InnerCommandException {
    public GameRunningException(String message) {
        super(SpigotCmdr.ErrorColorizer, message);
    }
}
