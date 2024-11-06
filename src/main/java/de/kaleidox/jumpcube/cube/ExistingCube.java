package de.kaleidox.jumpcube.cube;

import com.ampznetwork.libmod.api.entity.DbObject;
import com.ampznetwork.libmod.api.model.EntityType;
import com.ampznetwork.libmod.spigot.converter.WorldConverter;
import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.exception.DuplicateCubeException;
import de.kaleidox.jumpcube.exception.NoSuchCubeException;
import de.kaleidox.jumpcube.game.GameManager;
import de.kaleidox.jumpcube.game.GameReview;
import de.kaleidox.jumpcube.interfaces.Generatable;
import de.kaleidox.jumpcube.util.WorldUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.comroid.api.Polyfill;
import org.comroid.api.tree.Initializable;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
@Table(name = "jumpcubes")
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ExistingCube extends DbObject.WithPoiName implements Cube, Generatable, Initializable {
    public static final  EntityType<ExistingCube, ExistingCube.Builder<ExistingCube, ?>> TYPE
            = Polyfill.uncheckedCast(new EntityType<>(ExistingCube::builder, null, ExistingCube.class, ExistingCube.Builder.class));
    private final static Map<String, Cube>                                         instances = new ConcurrentHashMap<>();

    public static Stream<String> getNames() {
        return instances.values().stream().map(Cube::getCubeName);
    }

    @Nullable
    public static ExistingCube get(String name) {
        return (ExistingCube) instances.get(name);
    }

    public static boolean exists(String name) {
        return instances.containsKey(name);
    }

    public static Cube getSelection(Player player) {
        assert JumpCube.instance != null;

        return Optional.ofNullable(JumpCube.instance.selections.get(player.getUniqueId()))
                .orElseGet(() -> {
                    Cube sel = null;
                    if (instances.size() == 0)
                        return null;
                    if (instances.size() == 1)
                        sel = instances.entrySet().iterator().next().getValue();
                    if (sel == null)
                        sel = instances.values()
                                .stream()
                                .filter(cube -> cube.getWorld().equals(player.getWorld()))
                                .min(Comparator.comparingDouble(cube -> WorldUtil.dist(
                                        mid(cube.getPositions()),
                                        xyz(player.getLocation())
                                )))
                                .orElseThrow(() -> new NoSuchCubeException(player));
                    JumpCube.instance.selections.put(player.getUniqueId(), sel);
                    message().target(player).sendMessage("Cube {} was automatically selected!", sel.getCubeName());
                    return sel;
                });
    }

    @Deprecated
    public static ExistingCube load(final FileConfiguration config, String name, @Nullable BlockPool bar) {
        final String basePath = "cubes." + name + ".";

        if (bar == null) bar = BlockPool.create(config, basePath + "bar.");

        // get world
        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString(basePath + "world"),
                "No world defined for cube: " + name));

        // get positions
        int[][] locs = new int[2][3];

        var x1 = config.getInt(basePath + "pos1.x");
        var z1 = config.getInt(basePath + "pos1.z");

        var x2 = config.getInt(basePath + "pos2.x");
        var z2 = config.getInt(basePath + "pos2.z");

        assert world != null : "Unknown world: " + config.getString(basePath + "world");

        return builder().name(name)
                .world(world)
                .blockPool(bar)
                .x1(x1)
                .z1(z1)
                .x2(x2)
                .z2(z2)
                .minX(min(x1,x2))
                .maxX(max(z1,z2))
                .minZ(min(x1,x2))
                .maxZ(max(z1,z2))
                .build();
    }

    public final @Transient GameManager manager = new GameManager(this);

    // attributes
    private int x1, z1;
    private int x2, z2;
    private @Convert(converter = WorldConverter.class) World        world;
    private @lombok.Builder.Default @Nullable          Double       density            = null;
    private @lombok.Builder.Default @Nullable          Double       spacing            = null;
    private @lombok.Builder.Default @Nullable          Integer      height             = null;
    private @lombok.Builder.Default @Nullable          Integer      bottom             = null;
    private @lombok.Builder.Default @Nullable          Integer      galleryHeight      = null;
    private @Singular @ElementCollection @Nullable     List<String> cubeMaterials      = null;
    private @Singular @ElementCollection @Nullable     List<String> wallMaterials      = null;
    private @Singular @ElementCollection @Nullable     List<String> galleryMaterials   = null;
    private @Singular @ElementCollection @Nullable     List<String> placeableMaterials = null;

    // internals
    private @Transient int[][]   tpPos;
    private @Transient BlockPool blockPool;
    private @Transient int       minX, maxX, minZ, maxZ;
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
    public BlockPool getBlockBar() {
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
            if (value == this)
                JumpCube.instance.selections.remove(key, value);
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
                        if (off == 0)
                            for (y = bottom+height; y > bottom; y--) {
                                Block block = world.getBlockAt(x, y, z);
                                if (y < 50 || block.getType() != AIR)
                                    block.setType(blockPool.getRandomMaterial(WALLS));
                            }
                        else {
                            world.getBlockAt(x, galleryHeight, z).setType(blockPool.getRandomMaterial(GALLERY));
                            if (off == 1)
                                world.getBlockAt(x, galleryHeight + 3, z).setType(GLASS);
                        }
                    }

                    if (off == 1) {
                        world.getBlockAt(x, bottom + 1, z).setType(LAVA);
                        world.getBlockAt(x, bottom + 2, z).setType(LAVA);
                        world.getBlockAt(x, bottom + 3, z).setType(LAVA);
                    }
                }
        }

        var spacing = getSpacing();
        final int sx = (int) ((maxX - minX) * spacing);
        final int sz = (int) ((maxZ - minZ) * spacing);

        IntStream.range(2, mid(sx, sz))
                .forEach(off -> {
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
        final int spaceX = (int) (Math.abs(maxX-minX) * spacing);
        final int spaceZ = (int) (Math.abs(maxZ-minZ) * spacing);

        int       x, y, z;
        final int height = getHeight();
        final int bottom = getBottom();

        for (x = minX + spaceX; x <= maxX - spaceX; x++)
            for (z = minZ + spaceZ; z <= maxZ - spaceZ; z++)
                for (y = bottom + 10; y < height; y++)
                    if (JumpCube.rng.nextDouble() % 1 > density) world.getBlockAt(x, y, z).setType(AIR);
                    else world.getBlockAt(x, y, z).setType(blockPool.getRandomMaterial(CUBE));

        assert JumpCube.instance != null;
        JumpCube.instance.getLogger().info("Cube " + getName() + " was generated, took "
                                           + (nanoTime() - startNanos) + " nanoseconds.");
        startNanos = -1;

        start();
    }

    public void start() {
        System.out.println("gen bridge");
        final var spacing = getSpacing();
        final int spaceX = (int) (Math.abs(maxX-minX) * spacing);
        final int spaceZ = (int) (Math.abs(maxZ-minZ) * spacing);

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
        (otherX > mid ? IntStream.range(mid, otherX)
                      : (otherX == mid ? IntStream.range(otherX, mid + 1)
                                       : IntStream.range(otherX, mid)))
                .forEach(xBridge -> IntStream.range(2, space)
                        .flatMap(zOff -> IntStream.of(minZ, maxZ)
                                .map(z -> z == minZ ? z + zOff : z - zOff))
                        .forEach(zBridge -> world.getBlockAt(xBridge, galleryHeight, zBridge)
                                .setType(blockPool.getRandomMaterial(GALLERY))));
    }

    @Override
    public void initialize() {
        minX = min(x1, x2);
        maxX = max(x1, x2);
        minZ = min(z1, z2);
        maxZ = max(z1, z2);

        manager.initialize();

        final int galleryHeight = getGalleryHeight();

        this.tpPos = new int[][]{
                new int[]{ minX + 1, galleryHeight + 1, minZ + 1 },
                new int[]{ maxX - 1, galleryHeight + 1, maxZ - 1 },
                new int[]{ minX + 1, galleryHeight + 1, maxZ - 1 },
                new int[]{ maxX - 1, galleryHeight + 1, minZ + 1 }
        };
    }

    public final static class Commands {
        public static void regenerate(CommandSender sender, Cube sel, boolean full) {
            sel.getBlockBar().validate();
            if (full) ((ExistingCube) sel).generateFull();
            else ((ExistingCube) sel).generate();

            message().target(sender).sendMessage("Cube was regenerated!");
        }
    }
}
