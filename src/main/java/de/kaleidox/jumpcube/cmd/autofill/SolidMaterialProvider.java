package de.kaleidox.jumpcube.cmd.autofill;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.comroid.annotations.Instance;
import org.comroid.api.func.util.Command;

import java.util.Arrays;
import java.util.stream.Stream;

public enum SolidMaterialProvider implements Command.AutoFillProvider {
    @Instance INSTANCE;

    @Override
    public Stream<String> autoFill(Command.Usage usage, String argName, String currentValue) {
        return Arrays.stream(Material.values())
                .filter(Material::isSolid)
                .filter(Material::isBlock)
                .map(Material::getKey)
                .map(NamespacedKey::toString);
    }
}
