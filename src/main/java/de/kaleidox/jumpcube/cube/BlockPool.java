package de.kaleidox.jumpcube.cube;

import de.kaleidox.jumpcube.JumpCube;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.util.function.Predicate.*;

@Value
@Builder
public class BlockPool {
    public static BlockPool load(ConfigurationSection config) {
        return builder().cubeMaterials(loadSubPool(config, MaterialGroup.CUBE))
                .wallMaterials(loadSubPool(config, MaterialGroup.WALL))
                .galleryMaterials(loadSubPool(config, MaterialGroup.GALLERY))
                .placeableMaterials(loadSubPool(config, MaterialGroup.PLACEABLE))
                .build();
    }

    public static List<Material> loadSubPool(ConfigurationSection rootConfig, MaterialGroup group) {
        if (rootConfig == null) return JumpCube.instance.defaultBlockPool.get(group);
        var key = group.name().toLowerCase();
        if (rootConfig.isList(key)) return convertMaterials(rootConfig.getStringList(key));
        if (rootConfig.isString(key)) return convertMaterials(List.of(key));
        throw new RuntimeException("Invalid configuration");
    }

    public static List<Material> convertMaterials(List<String> strings) {
        return strings == null ? null : strings.stream().map(Material::getMaterial).toList();
    }

    @Singular @Nullable List<Material> cubeMaterials;
    @Singular @Nullable List<Material> wallMaterials;
    @Singular @Nullable List<Material> galleryMaterials;
    @Singular @Nullable List<Material> placeableMaterials;

    public List<Material> getCubeMaterials() {
        return get(MaterialGroup.CUBE);
    }

    public List<Material> getWallMaterials() {
        return get(MaterialGroup.WALL);
    }

    public List<Material> getGalleryMaterials() {
        return get(MaterialGroup.GALLERY);
    }

    public List<Material> getPlaceableMaterials() {
        return get(MaterialGroup.PLACEABLE);
    }

    public @Nullable List<Material> getRaw(MaterialGroup group) {
        return switch (group) {
            case CUBE -> cubeMaterials;
            case WALL -> wallMaterials;
            case GALLERY -> galleryMaterials;
            case PLACEABLE -> placeableMaterials;
        };
    }

    public List<Material> get(MaterialGroup group) {
        return Optional.ofNullable(getRaw(group)).filter(not(List::isEmpty)).orElseGet(() -> JumpCube.instance.defaultBlockPool.get(group));
    }

    public boolean isDefault(MaterialGroup group) {
        return getRaw(group) != null;
    }

    public Material getRandomMaterial(MaterialGroup group) {
        var pool = get(group);
        return pool.get(JumpCube.rng.nextInt(pool.size()));
    }

    public enum MaterialGroup {
        CUBE, WALL, GALLERY, PLACEABLE
    }
}
