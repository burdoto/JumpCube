package de.kaleidox.jumpcube.exception;

import org.comroid.api.func.util.Command;

public final class GameRunningException extends Command.Error {
    public GameRunningException(String message) {
        super(message);
    }
}
