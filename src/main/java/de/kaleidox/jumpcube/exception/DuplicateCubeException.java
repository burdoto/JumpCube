package de.kaleidox.jumpcube.exception;

import org.comroid.cmdr.spigot.InnerCommandException;

public final class DuplicateCubeException extends InnerCommandException {
    public DuplicateCubeException(String name) {
        super("Cube names must be unique! [" + name + "]");
    }
}
