package de.kaleidox.jumpcube.exception;

import org.comroid.api.func.util.Command;

public final class InvalidArgumentCountException extends Command.Error {
    public InvalidArgumentCountException(int expected, int actual) {
        super(String.format("Too {} arguments! Expected: %d", (actual < expected ? "few" : "many"), expected));
    }
}
