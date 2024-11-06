package de.kaleidox.jumpcube.cmd;

import com.ampznetwork.libmod.api.util.chat.BroadcastType;
import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.cmd.autofill.CubeNameProvider;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.CubeCreationTool;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.exception.NoSuchCubeException;
import de.kaleidox.jumpcube.util.BukkitUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.comroid.annotations.Alias;
import org.comroid.api.func.util.Command;

import java.util.UUID;

import static de.kaleidox.jumpcube.JumpCube.*;

public class JumpCubeCommands {
    @Command(value = "jumpcube")
    @Alias("jc")
    public static class jc {
        @Command
        public static Component $() {return version();}

        @Command
        public static Component version() {
            return BroadcastType.INFO.colorize("JumpCube version {}", JumpCube.instance.getDescription().getVersion());
        }

        @Command
        public static Component reload() {
            return BroadcastType.ERROR.colorize("Sorry, {} not yet implemented", "Reloading");
        }

        @Command
        public static void create(JumpCube pl, CommandSender sender, @Command.Arg String name) {
            if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
            UUID senderUuid = BukkitUtil.getUuid(sender);
            Cube sel        = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

            if (ExistingCube.exists(name)) {
                message().target(sender).sendMessage(BroadcastType.ERROR, "A cube with the name {} already exists!", name);
                return;
            }

            if (sel instanceof CubeCreationTool && !((CubeCreationTool) sel).isReady()) {
                // delete old, nonready selection first
                sel.delete();
                pl.selections.remove(senderUuid);
            }

            CubeCreationTool creationTool = new CubeCreationTool(BukkitUtil.getPlayer(sender));
            creationTool.setName(name);
            pl.selections.put(senderUuid, creationTool);
            message().target(sender).sendMessage(BroadcastType.INFO, "Cube {} creation started!", name);
        }

        @Command
        @Alias("sel")
        public static void select(JumpCube pl, CommandSender sender, @Command.Arg(autoFillProvider = CubeNameProvider.class) String name) {
            if (!pl.checkPerm(sender, JumpCube.Permission.USER)) return;
            UUID senderUuid = BukkitUtil.getUuid(sender);
            Cube sel        = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

            if (sel != null && sel.getCubeName().equals(name)) {
                message().target(sender).sendMessage(BroadcastType.INFO, "Cube {} is already selected!", name);
                return;
            }
            if (!ExistingCube.exists(name)) throw new NoSuchCubeException(name);

            ExistingCube cube = ExistingCube.get(name);
            assert cube != null;
            pl.selections.put(senderUuid, cube);
            message().target(sender).sendMessage(BroadcastType.INFO, "Cube {} selected!", name);
        }

        @Command
        public static void pos(JumpCube pl, CommandSender sender, @Command.Arg(autoFill = { "1", "2" }) int n) {
            if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
            Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

            if (sel == null)
                throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
            CubeCreationTool.Commands.pos(sender, sel, n);
        }

        @Command
        public static void confirm(JumpCube pl, CommandSender sender) {
            if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
            Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

            if (sel == null)
                throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
            CubeCreationTool.Commands.confirm(sender, sel);
        }

        @Command
        @Alias("regen")
        public static void regenerate(JumpCube pl, CommandSender sender, @Command.Arg(required = false, autoFill = { "true", "false" }) boolean full) {
            if (!pl.checkPerm(sender, JumpCube.Permission.REGENERATE)) return;
            Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

            if (sel == null)
                throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
            if (!JumpCube.instance.validateSelection(sender, sel)) return;
            ExistingCube.Commands.regenerate(sender, sel, full);
        }

        @Command
        public static void join(JumpCube pl, CommandSender sender) {
            if (!pl.checkPerm(sender, JumpCube.Permission.USER)) return;
            Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));
            if (sel == null)
                throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
            if (!JumpCube.instance.validateSelection(sender, sel)) return;
            ((ExistingCube) sel).manager.join(sender);
        }

        @Command
        public static void leave(JumpCube pl, CommandSender sender) {
            Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));
            if (sel == null)
                throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
            if (!JumpCube.instance.validateSelection(sender, sel)) return;
            ((ExistingCube) sel).manager.leave(sender);
        }

        @Command
        public static void start(JumpCube pl, CommandSender sender) {
            if (!pl.checkPerm(sender, JumpCube.Permission.START_EARLY)) return;
            Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));
            if (sel == null)
                throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
            if (!JumpCube.instance.validateSelection(sender, sel)) return;
            ((ExistingCube) sel).manager.start();
        }
    }
}
