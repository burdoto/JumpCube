package de.kaleidox.jumpcube.cube;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.exception.InvalidArgumentCountException;
import de.kaleidox.jumpcube.util.BukkitUtil;
import de.kaleidox.jumpcube.util.WorldUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.chat.MessageLevel.ERROR;
import static de.kaleidox.jumpcube.chat.MessageLevel.INFO;
import static de.kaleidox.jumpcube.util.WorldUtil.dist;
import static java.lang.Math.max;

public class CubeCreationTool implements Cube {
    public final Player player;
    private final World world;
    private String name;
    private int[][] pos = new int[2][3];
    private BlockBar bar;

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
    public BlockBar getBlockBar() {
        return bar;
    }

    @Override
    public World getWorld() {
        return world;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CubeCreationTool(Player player) {
        this.player = player;
        this.world = player.getWorld();
    }

    public void setPos(int y, Location location) {
        pos[y - 1] = WorldUtil.xyz(location);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void delete() {
        // release pointers
        name = null;
        pos = null;
        bar = null;
    }

    public ExistingCube create() {
        assert JumpCube.instance != null;

        final String basePath = "cubes." + name + ".";
        final FileConfiguration config = JumpCube.instance.getConfig();

        // save world
        config.set(basePath + "world", world.getName());
        config.set(basePath + "height", getHeight());
        config.set(basePath + "bottom", getBottom());
        config.set(basePath + "gallery.height", getGalleryHeight());

        // save first position
        config.set(basePath + "pos1.x", pos[0][0]);
        config.set(basePath + "pos1.y", pos[0][1]);
        config.set(basePath + "pos1.z", pos[0][2]);

        // save second position
        config.set(basePath + "pos2.x", pos[1][0]);
        config.set(basePath + "pos2.y", pos[1][1]);
        config.set(basePath + "pos2.z", pos[1][2]);

        // save bar
        bar.save(config, basePath + "bar.");

        // add name to list
        config.set("cubes.created", (config.isSet("cubes.created")
                ? (config.getString("cubes.created") + ";")
                : "") + name);

        JumpCube.instance.saveConfig();

        return ExistingCube.load(config, name, bar);
    }

    public static final class Commands {
        public static void pos(CommandSender sender, Cube sel, String subCommand, String[] args) {
            if (!validateEditability(sender, sel)) return;

            if ((args.length != 1 && subCommand.equals("pos"))
                    || (args.length > 0 && !subCommand.equals("pos")))
                throw new InvalidArgumentCountException(subCommand.equals("pos") ? 1 : 0, args.length);
            if (subCommand.equals("pos") && !args[0].matches("[12]")) {
                message(sender, ERROR, "Illegal argument: %s", args[0]);
                return;
            }

            Location location = BukkitUtil.getPlayer(sender).getLocation();
            switch (subCommand) {
                case "pos":
                    int argInt = Integer.parseInt(args[0]);
                    ((CubeCreationTool) sel).setPos(argInt, location);
                    message(sender, INFO, "Position %s was set to your current location!", argInt);
                    break;
                case "pos1":
                    ((CubeCreationTool) sel).setPos(1, location);
                    message(sender, INFO, "Position %s was set to your current location!", 1);
                    break;
                case "pos2":
                    ((CubeCreationTool) sel).setPos(2, location);
                    message(sender, INFO, "Position %s was set to your current location!", 2);
                    break;
            }

            int[][] pos = sel.getPositions();
            if (pos[0] != null && pos[1] != null) {
                double dist = dist(pos[0], pos[1]);
                if (dist < 0) dist = dist * -1;
                if (dist < 32) message(sender, ERROR, "Size: %s (Cannot be smaller than 32)", (int) dist);
                else if (dist > 64) message(sender, ERROR, "Size: %s (Cannot be larger than 64)", (int) dist);
                else message(sender, INFO, "Size: %s (Even sizes are recommended)", (int) dist);
            }
        }

        public static void bar(CommandSender sender, Cube sel, String[] args) {
            if (!validateEditability(sender, sel)) return;

            Player player = BukkitUtil.getPlayer(sender);
            ((CubeCreationTool) sel).bar = new BlockBar(player);

            message(sender, INFO, "The BlockBar has been pasted relative to you.");
        }

        public static void confirm(CommandSender sender, Cube sel) {
            if (!validateEditability(sender, sel)) return;

            if (!((CubeCreationTool) sel).isReady()) {
                message(sender, ERROR, "Cube setup isn't complete yet!");
                return;
            }

            int[][] positions = sel.getPositions();
            if (dist(positions[0], positions[1]) < 32) {
                message(sender, ERROR, "Cube must be at least %s blocks wide!", 32);
                return;
            } else if (dist(positions[0], positions[1]) > 64) {
                message(sender, ERROR, "Cube cant be wider than %s blocks!", 64);
                return;
            }

            ExistingCube cube = ((CubeCreationTool) sel).create();
            cube.generateFull();

            message(sender, INFO, "Cube %s was created!", cube.getCubeName());
        }

        private static boolean validateEditability(CommandSender sender, Cube sel) {
            if (sel == null) {
                message(sender, ERROR, "No cube selected!");
                return false;
            }
            if (!(sel instanceof CubeCreationTool)) {
                message(sender, ERROR, "Cube %s is not editable!", sel.getCubeName());
                return false;
            }
            return true;
        }
    }
}
