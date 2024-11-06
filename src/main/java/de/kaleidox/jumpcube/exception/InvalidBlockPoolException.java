package de.kaleidox.jumpcube.exception;

import org.bukkit.Material;
import org.comroid.api.func.util.Command;

public final class InvalidBlockPoolException extends Command.Error {
    public InvalidBlockPoolException(Material errorMaterial, Cause cause) {
        super("Invalid block: " + errorMaterial.name() + " is not a valid block; it is " + cause.s);
    }

    public enum Cause {
        NON_SOLID("not solid"),
        INTERACTABLE("interactable (only placeables can be interactable)");

        private final String s;

        Cause(String s) {
            this.s = s;
        }
    }
}