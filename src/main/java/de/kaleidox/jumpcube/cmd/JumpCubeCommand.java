package de.kaleidox.jumpcube.cmd;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.chat.Chat;
import de.kaleidox.jumpcube.chat.MessageLevel;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.CubeCreationTool;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.exception.InvalidArgumentCountException;
import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.command.CommandSender;
import org.comroid.cmdr.model.Command;

import java.util.UUID;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.chat.MessageLevel.ERROR;
import static de.kaleidox.jumpcube.chat.MessageLevel.INFO;

@Command(name = "jumpcube")
@Command.Alias("jc")
@Command.Default("version")
public class JumpCubeCommand {
    @Command
    public static String version() {
        return Chat.makeMessage(INFO, "JumpCube version %s", JumpCube.instance.getDescription().getVersion());
    }

    @Command
    public static String reload() {
        return Chat.makeMessage(ERROR, "Reloading not yet implemented");
    }

    @Command
    public static void create(JumpCube pl, CommandSender sender, @Command.Arg(true) String name) {
        if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
        UUID senderUuid = BukkitUtil.getUuid(sender);
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (ExistingCube.exists(name)) {
            message(sender, ERROR, "A cube with the name %s already exists!", name);
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
        message(sender, INFO, "Cube %s creation started!", name);
    }
}
