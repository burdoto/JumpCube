package de.kaleidox.jumpcube.cube;

import com.ampznetwork.libmod.api.util.chat.BroadcastType;
import de.kaleidox.jumpcube.util.BukkitUtil;
import de.kaleidox.jumpcube.util.WorldUtil;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static de.kaleidox.jumpcube.JumpCube.*;
import static de.kaleidox.jumpcube.util.WorldUtil.*;

@Data
public class CubeCreationTool implements Cube {
    public final  Player    player;
    private final World     world;
    private       String    name;
    private       int[][]   pos = new int[2][3];
    private       BlockPool bar;

    public CubeCreationTool(Player player) {
        this.player = player;
        this.world  = player.getWorld();
    }

    public boolean isReady() {
        return name != null
               && pos[0] != null
               && pos[1] != null
               && bar != null;
    }

    @Override
    public String getCubeName() {
        return name;
    }

    @Override
    public int[][] getPositions() {
        return pos;
    }

    @Override
    public int getGalleryHeight() {
        return -1;
    }

    @Override
    public int getHeight() {
        return -1;
    }

    @Override
    public int getBottom() {
        return -1;
    }

    @Override
    public BlockPool getBlockPool() {
        return bar;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void delete() {
        // release pointers
        name = null;
        pos = null;
        bar = null;
    }

    public void setPos(int y, Location location) {
        pos[y - 1] = WorldUtil.xyz(location);
    }

    public ExistingCube create() {
        return instance.getLib().getEntityService()
                .getAccessor(ExistingCube.TYPE)
                .create().complete(builder -> builder.name(name)
                        .world(world)
                        .x1(pos[0][0]).x2(pos[1][0])
                        .z1(pos[0][2]).z2(pos[1][2])
                        .cubeMaterials(bar.get(BlockPool.MaterialGroup.CUBE))
                        .wallMaterials(bar.get(BlockPool.MaterialGroup.WALL))
                        .galleryMaterials(bar.get(BlockPool.MaterialGroup.GALLERY))
                        .placeableMaterials(bar.get(BlockPool.MaterialGroup.PLACEABLE)));
    }

    public static final class Commands {
        public static void pos(CommandSender sender, Cube sel, int n) {
            if (!validateEditability(sender, sel)) return;

            Location location = BukkitUtil.getPlayer(sender).getLocation();
            switch (n) {
                case 1:
                    ((CubeCreationTool) sel).setPos(1, location);
                    message().target(sender).sendMessage(BroadcastType.INFO, "Position {} was set to your current location!", 1);
                    break;
                case 2:
                    ((CubeCreationTool) sel).setPos(2, location);
                    message().target(sender).sendMessage(BroadcastType.INFO, "Position {} was set to your current location!", 2);
                    break;
            }

            int[][] pos = sel.getPositions();
            if (pos[0] != null && pos[1] != null) {
                double dist = dist(pos[0], pos[1]);
                if (dist < 0) dist = dist * -1;
                if (dist < 32)
                    message().target(sender).sendMessage(BroadcastType.ERROR, "Size: {} (Cannot be smaller than 32)", (int) dist);
                else if (dist > 64)
                    message().target(sender).sendMessage(BroadcastType.ERROR, "Size: {} (Cannot be larger than 64)", (int) dist);
                else message().target(sender).sendMessage(BroadcastType.INFO, "Size: {} (Even sizes are recommended)", (int) dist);
            }
        }

        public static void confirm(CommandSender sender, Cube sel) {
            if (!validateEditability(sender, sel)) return;

            if (!((CubeCreationTool) sel).isReady()) {
                message().target(sender).sendMessage(BroadcastType.ERROR, "Cube setup isn't complete yet!");
                return;
            }

            int[][] positions = sel.getPositions();
            if (dist(positions[0], positions[1]) < 32) {
                message().target(sender).sendMessage(BroadcastType.ERROR, "Cube must be at least {} blocks wide!", 32);
                return;
            } else if (dist(positions[0], positions[1]) > 64) {
                message().target(sender).sendMessage(BroadcastType.ERROR, "Cube cant be wider than {} blocks!", 64);
                return;
            }

            ExistingCube cube = ((CubeCreationTool) sel).create();
            cube.generateFull();

            message().target(sender).sendMessage(BroadcastType.INFO, "Cube {} was created!", cube.getCubeName());
        }

        private static boolean validateEditability(CommandSender sender, Cube sel) {
            if (sel == null) {
                message().target(sender).sendMessage(BroadcastType.ERROR, "No cube selected!");
                return false;
            }
            if (!(sel instanceof CubeCreationTool)) {
                message().target(sender).sendMessage(BroadcastType.ERROR, "Cube {} is not editable!", sel.getCubeName());
                return false;
            }
            return true;
        }
    }
}
