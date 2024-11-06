package de.kaleidox.jumpcube.cube;

import com.ampznetwork.libmod.api.entity.DbObject;
import com.ampznetwork.libmod.api.model.EntityType;
import com.ampznetwork.libmod.spigot.converter.MaterialConverter;
import com.ampznetwork.libmod.spigot.converter.WorldConverter;
import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.exception.NoSuchCubeException;
import de.kaleidox.jumpcube.game.GameManager;
import de.kaleidox.jumpcube.interfaces.Generatable;
import de.kaleidox.jumpcube.util.WorldUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.comroid.api.Polyfill;
import org.comroid.api.tree.Initializable;
import org.jetbrains.annotations.Nullable;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static de.kaleidox.jumpcube.JumpCube.*;
import static de.kaleidox.jumpcube.cube.BlockPool.MaterialGroup.*;
import static de.kaleidox.jumpcube.util.MathUtil.dist;
import static de.kaleidox.jumpcube.util.MathUtil.mid;
import static de.kaleidox.jumpcube.util.WorldUtil.mid;
import static de.kaleidox.jumpcube.util.WorldUtil.*;
import static java.lang.Math.*;
import static java.lang.System.*;
import static org.bukkit.Material.*;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "jumpcubes")
public class ExistingCube extends DbObject.WithPoiName implements Cube, Generatable, Initializable {
    public static final EntityType<ExistingCube, ExistingCube.Builder<ExistingCube, ?>> TYPE = Polyfill.uncheckedCast(new EntityType<>(ExistingCube::builder,
            null,
            ExistingCube.class,
            ExistingCube.Builder.class));

    public static boolean exists(String name) {
        return instance.getLib().getEntityService()
                .getAccessor(ExistingCube.TYPE)
                .all()
                .map(ExistingCube::getCubeName)
                .anyMatch(name::equalsIgnoreCase);
    }

    public static ExistingCube get(String name) {
        return instance.getLib().getEntityService()
                .getAccessor(ExistingCube.TYPE)
                .all()
                .filter(cube -> cube.getCubeName().equals(name))
                .findAny().orElseThrow(() -> new NoSuchCubeException(name));
    }

    public static Cube getSelection(Player player) {
        assert JumpCube.instance != null;
        return Optional.ofNullable(JumpCube.instance.selections.get(player.getUniqueId()))
                .or(() -> instance.getLib().getEntityService()
                        .getAccessor(ExistingCube.TYPE)
                        .all()
                        .filter(cube -> cube.getWorld().equals(player.getWorld()))
                        .sorted(Comparator.comparingDouble(cube -> WorldUtil.dist(mid(cube.getPositions()), xyz(player.getLocation()))))
                        .peek(cube -> message().target(player).sendMessage("Cube {} was automatically selected!", cube.getCubeName()))
                        .findFirst())
                .orElse(null);
    }

    public final @Transient GameManager manager = new GameManager(this);

    // attributes
    private int x1, z1;
    private int x2, z2;
    private @Convert(converter = WorldConverter.class) World          world;
    private @lombok.Builder.Default @Nullable          Double         density            = null;
    private @lombok.Builder.Default @Nullable          Double         spacing            = null;
    private @lombok.Builder.Default @Nullable          Integer        height             = null;
    private @lombok.Builder.Default @Nullable          Integer        bottom             = null;
    private @lombok.Builder.Default @Nullable          Integer        galleryHeight      = null;
    @Convert(converter = MaterialConverter.class) @Column(name = "material")
    @CollectionTable(name = "jumpcube_materials_cube", joinColumns = @JoinColumn(name = "id"))
    private @Singular @ElementCollection @Nullable     List<Material> cubeMaterials      = null;
    @Convert(converter = MaterialConverter.class) @Column(name = "material")
    @CollectionTable(name = "jumpcube_materials_wall", joinColumns = @JoinColumn(name = "id"))
    private @Singular @ElementCollection @Nullable     List<Material> wallMaterials      = null;
    @Convert(converter = MaterialConverter.class) @Column(name = "material")
    @CollectionTable(name = "jumpcube_materials_gallery", joinColumns = @JoinColumn(name = "id"))
    private @Singular @ElementCollection @Nullable     List<Material> galleryMaterials   = null;
    @Convert(converter = MaterialConverter.class) @Column(name = "material")
    @CollectionTable(name = "jumpcube_materials_placeable", joinColumns = @JoinColumn(name = "id"))
    private @Singular @ElementCollection @Nullable     List<Material> placeableMaterials = null;

    // internals
    private @Transient                         int[][]   tpPos;
    private @Transient                         BlockPool blockPool;
    private @Transient @lombok.Builder.Default int       minX = 0, maxX = 0, minZ = 0, maxZ = 0;
    private @Transient @lombok.Builder.Default int  tpCycle    = -1;
    private @Transient @lombok.Builder.Default long startNanos = -1;

    @Override
    public String getCubeName() {
        return getName();
    }

    @Override
    public int[][] getPositions() {
        return new int[][]{ new int[]{ x1, z1 }, new int[]{ x2, z2 } };
    }

    public int getGalleryHeight() {
        return Objects.requireNonNullElseGet(galleryHeight, () -> JumpCube.instance.getConfig().getInt("defaults.settings.gallery"));
    }

    public int getHeight() {
        return Objects.requireNonNullElseGet(height, () -> JumpCube.instance.getConfig().getInt("defaults.settings.height"));
    }

    public int getBottom() {
        return Objects.requireNonNullElseGet(bottom, () -> JumpCube.instance.getConfig().getInt("defaults.settings.bottom"));
    }

    @Override
    public BlockPool getBlockPool() {
        return blockPool;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void delete() {
        assert JumpCube.instance != null;

        // remove from selections
        JumpCube.instance.selections.forEach((key, value) -> {
            if (value == this) JumpCube.instance.selections.remove(key, value);
        });
        // delete from db. goodbye!
        JumpCube.instance.getLib().getEntityService().delete(this);
    }

    public double getDensity() {
        return Objects.requireNonNullElseGet(density, () -> JumpCube.instance.getConfig().getDouble("defaults.settings.density")) * 0.1835;
    }

    public double getSpacing() {
        return Objects.requireNonNullElseGet(spacing, () -> JumpCube.instance.getConfig().getDouble("defaults.settings.spacing"));
    }

    public void reloadBlockPool() {
    }

    @Override
    @PostLoad
    @PostUpdate
    public void initialize() {
        minX = min(x1, x2);
        maxX = max(x1, x2);
        minZ = min(z1, z2);
        maxZ = max(z1, z2);

        var galleryHeight = getGalleryHeight();
        tpPos     = new int[][]{
                new int[]{ minX + 1, galleryHeight + 1, minZ + 1 }, new int[]{ maxX - 1, galleryHeight + 1, maxZ - 1 },
                new int[]{ minX + 1, galleryHeight + 1, maxZ - 1 }, new int[]{ maxX - 1, galleryHeight + 1, minZ + 1 }
        };
        blockPool = new BlockPool(
                cubeMaterials,
                wallMaterials,
                galleryMaterials,
                placeableMaterials);

        manager.initialize();
    }

    public void teleportIn(Player player) {
        if (++tpCycle >= tpPos.length) tpCycle = 0;
        Location location = WorldUtil.location(world, tpPos[tpCycle]);
        player.teleport(location.add(0, 1.2, 0));
    }

    public void generateFull() {
        startNanos = nanoTime();

        //final int maxY = max(y1, y2);

        final int height        = getHeight();
        final int bottom        = getBottom();
        final int galleryHeight = getGalleryHeight();

        int x, y, z;

        for (x = minX + 1; x < maxX; x++)
            for (z = minZ + 1; z < maxZ; z++)
                for (y = height; y > bottom; y--)
                    world.getBlockAt(x, y, z).setType(AIR);

        for (int off : new int[]{ 0, 1, 2 }) {
            final int minXloop = minX + off;
            final int maxXloop = maxX - off;
            final int minZloop = minZ + off;
            final int maxZloop = maxZ - off;

            for (x = minXloop; x <= maxXloop; x++)
                for (z = minZloop; z <= maxZloop; z++) {
                    if (x == minXloop || x == maxXloop || z == minZloop || z == maxZloop) {
                        if (off == 0) for (y = bottom + height; y > bottom; y--) {
                            Block block = world.getBlockAt(x, y, z);
                            if (y < 50 || block.getType() != AIR) block.setType(blockPool.getRandomMaterial(WALL));
                        }
                        else {
                            world.getBlockAt(x, galleryHeight, z).setType(blockPool.getRandomMaterial(GALLERY));
                            if (off == 1) world.getBlockAt(x, galleryHeight + 3, z).setType(GLASS);
                        }
                    }

                    if (off == 1) {
                        world.getBlockAt(x, bottom + 1, z).setType(LAVA);
                        world.getBlockAt(x, bottom + 2, z).setType(LAVA);
                        world.getBlockAt(x, bottom + 3, z).setType(LAVA);
                    }
                }
        }

        var       spacing = getSpacing();
        final int sx      = (int) ((maxX - minX) * spacing);
        final int sz      = (int) ((maxZ - minZ) * spacing);

        IntStream.range(2, mid(sx, sz)).forEach(off -> {
            final int minXoff = minX + off;
            final int maxXoff = maxX - off;
            final int minZoff = minZ + off;
            final int maxZoff = maxZ - off;

            int x_, z_;

            for (x_ = minXoff; x_ <= maxXoff; x_++)
                for (z_ = minZoff; z_ <= maxZoff; z_++)
                    if (off == 2 && (x_ == minXoff || x_ == maxXoff || z_ == minZoff || z_ == maxZoff))
                        // renew glass panes
                        world.getBlockAt(x_, galleryHeight + 1, z_).setType(GLASS_PANE);
                    else // remove gallery extensions
                        world.getBlockAt(x_, galleryHeight, z_).setType(AIR);
        });

        generate();
    }

    @Override
    public void generate() {
        if (startNanos == -1) startNanos = nanoTime();

        final var spacing = getSpacing();
        final var density = getDensity();
        final int spaceX = (int) (Math.abs(maxX - minX) * spacing);
        final int spaceZ = (int) (Math.abs(maxZ - minZ) * spacing);

        int       x, y, z;
        final int height = getHeight();
        final int bottom = getBottom();

        for (x = minX + spaceX; x <= maxX - spaceX; x++)
            for (z = minZ + spaceZ; z <= maxZ - spaceZ; z++)
                for (y = bottom + 10; y < height; y++)
                    if (JumpCube.rng.nextDouble() % 1 > density) world.getBlockAt(x, y, z).setType(AIR);
                    else world.getBlockAt(x, y, z).setType(blockPool.getRandomMaterial(CUBE));

        assert JumpCube.instance != null;
        JumpCube.instance.getLogger().info("Cube " + getName() + " was generated, took " + (nanoTime() - startNanos) + " nanoseconds.");
        startNanos = -1;

        start();
    }

    public void start() {
        System.out.println("gen bridge");
        final var spacing = getSpacing();
        final int spaceX = (int) (Math.abs(maxX - minX) * spacing);
        final int spaceZ = (int) (Math.abs(maxZ - minZ) * spacing);

        final int midX = mid(minX, maxX);
        final int midZ = mid(minZ, maxZ);

        final int xDistA = dist(midX, minX);
        final int xDistB = dist(midX, maxX);
        final int zDistA = dist(midZ, minZ);
        final int zDistB = dist(midZ, maxZ);

        final int galleryHeight = getGalleryHeight();

        System.out.println("spaceZ = " + spaceZ);

        generateGallery(spaceZ, midX, xDistA, xDistB);
        generateGallery(spaceX, midZ, zDistA, zDistB);

        world.getBlockAt(midX, galleryHeight + 1, minZ + 2).setType(AIR);
        world.getBlockAt(midX, galleryHeight + 1, maxZ - 2).setType(AIR);
        world.getBlockAt(minX + 2, galleryHeight + 1, midZ).setType(AIR);
        world.getBlockAt(maxZ - 2, galleryHeight + 1, midZ).setType(AIR);
    }

    private void generateGallery(int space, int mid, int distA, int distB) {
        final int galleryHeight = getGalleryHeight();

        int otherX = (mid - Integer.compare(distA, distB));
        (otherX > mid
         ? IntStream.range(mid, otherX)
         : (otherX == mid ? IntStream.range(otherX, mid + 1) : IntStream.range(otherX, mid))).forEach(xBridge -> IntStream.range(2, space)
                .flatMap(zOff -> IntStream.of(minZ, maxZ).map(z -> z == minZ ? z + zOff : z - zOff))
                .forEach(zBridge -> world.getBlockAt(xBridge, galleryHeight, zBridge).setType(blockPool.getRandomMaterial(GALLERY))));
    }

    public final static class Commands {
        public static void regenerate(CommandSender sender, Cube sel, boolean full) {
            sel.getBlockPool();
            if (full) ((ExistingCube) sel).generateFull();
            else ((ExistingCube) sel).generate();

            message().target(sender).sendMessage("Cube was regenerated!");
        }
    }
}
