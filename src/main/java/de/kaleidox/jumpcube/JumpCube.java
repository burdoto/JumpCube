package de.kaleidox.jumpcube;

import com.ampznetwork.libmod.api.util.chat.BroadcastType;
import com.ampznetwork.libmod.api.util.chat.BroadcastWrapper;
import com.ampznetwork.libmod.spigot.SubMod$Spigot;
import de.kaleidox.jumpcube.cmd.JumpCubeCommand;
import de.kaleidox.jumpcube.cube.BlockPool;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.game.GameReview;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class JumpCube extends SubMod$Spigot {
    public static final Random   rng = new Random();
    public static       JumpCube instance;

    public static Stream<String> getCubeNames() {
        return ExistingCube.getNames();
    }

    public static BroadcastWrapper message() {
        return instance.broadcast;
    }

    public BlockPool defaultBlockPool;

    {
        instance = this;
    }

    public JumpCube() {
        super(Set.of(Capability.Database), Set.of(ExistingCube.class, GameReview.class));
    }

    public boolean validateSelection(CommandSender sender, Cube sel) {
        if (sel == null) {
            broadcast.target(sender).sendMessage(BroadcastType.ERROR, "No cube selected!");
            return false;
        }
        if (!(sel instanceof ExistingCube)) {
            broadcast.target(sender).sendMessage(BroadcastType.ERROR, "Cube {} is not finished!", sel.getCubeName());
            return false;
        }
        return true;
    }
    public  Map<UUID, Cube>  selections = new ConcurrentHashMap<>();
    public  BroadcastWrapper broadcast;
    private Logger           logger;
    private FileConfiguration config;

    @Override
    public void onLoad() {
        cmdr.register(JumpCubeCommand.class);
        cmdr.register(this);

        super.onLoad();

        this.config    = super.getConfig();
        this.broadcast = new BroadcastWrapper(NamedTextColor.AQUA, lib, "JumpCube");
        this.logger    = getLogger();

        logger.info("JumpCube loaded!");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        instance = null;

        logger.info("JumpCube disabled!");
    }

    @Override
    public void onEnable() {
        super.onEnable();

        final FileConfiguration config = getConfig();
        //this.defaultBlockPool = BlockPool.loadSubPool();

        logger.info("Please report bugs at https://github.com/burdoto/jumpcube/issues");
    }

    public boolean checkPerm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission))
            return true;
        else {
            messagePerm(sender, permission);
            return false;
        }
    }

    private void messagePerm(CommandSender sender, String permission) {
        broadcast.target(sender).sendMessage(BroadcastType.ERROR, "You are missing the permission: {}", permission);
    }

    public static final class Permission {
        public static final String USER = "jumpcube.user";

        public static final String START_EARLY     = "jumpcube.vip.earlystart";
        public static final String BRING_PLACEABLE = "jumpcube.vip.bringplaceable";

        public static final String TELEPORT_OUT = "jumpcube.mod.teleport";
        public static final String REGENERATE   = "jumpcube.mod.regenerate";

        public static final String ADMIN        = "jumpcube.admin";
        public static final String DEBUG_NOTIFY = "jumpcube.admin.debug";
    }
}
