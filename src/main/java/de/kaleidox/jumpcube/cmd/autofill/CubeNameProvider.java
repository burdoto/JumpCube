package de.kaleidox.jumpcube.cmd.autofill;

import com.ampznetwork.libmod.api.LibMod;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.ExistingCube;
import org.comroid.annotations.Instance;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Streams;

import java.util.stream.Stream;

public enum CubeNameProvider implements Command.AutoFillProvider {
    @Instance INSTANCE;

    @Override
    public Stream<String> autoFill(Command.Usage usage, String argName, String currentValue) {
        return usage.getContext().stream()
                .flatMap(Streams.cast(LibMod.class))
                .flatMap(lib -> lib.getEntityService().getAccessor(ExistingCube.TYPE).all())
                .map(Cube::getCubeName);
    }
}
