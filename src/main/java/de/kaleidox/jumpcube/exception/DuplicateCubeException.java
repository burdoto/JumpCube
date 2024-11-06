package de.kaleidox.jumpcube.exception;

import org.comroid.api.func.util.Command;

public final class DuplicateCubeException extends Command.Error {
    public DuplicateCubeException(String name) {
        super("Cube names must be unique! [" + name + "]");
    }
}
