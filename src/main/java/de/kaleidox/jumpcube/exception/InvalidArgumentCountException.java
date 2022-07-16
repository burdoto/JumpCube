package de.kaleidox.jumpcube.exception;

import org.comroid.cmdr.spigot.InnerCommandException;
import org.comroid.cmdr.spigot.SpigotCmdr;

public final class InvalidArgumentCountException extends InnerCommandException {
    @Override
    public String getIngameText() {
        return getMessage();
    }

    public InvalidArgumentCountException(int expected, int actual) {
        super(SpigotCmdr.ErrorColorizer, String.format("Too %s arguments! Expected: %d",
                (actual < expected ? "few" : "many"), expected));
    }
}
