package de.kaleidox.jumpcube;

import com.ampznetwork.libmod.spigot.SubMod$Spigot;
import de.kaleidox.jumpcube.cmd.JumpCubeCommand;
import de.kaleidox.jumpcube.cube.BlockBar;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.util.BukkitUtil;
import net.kyori.adventure.audience.Audience;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.comroid.cmdr.spigot.SpigotCmdr;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.kaleidox.jumpcube.chat.Chat.message;

public final class JumpCube extends SubMod$Spigot {
    public static final Random rng = new Random();
    @Nullable
    public static JumpCube instance;

    public Map<UUID, Cube> selections = new ConcurrentHashMap<>();
    private Logger logger;
    private FileConfiguration config;

    @Override
    public String getChatPrefix() {
        return ChatColor.DARK_GRAY + "[" +
                ChatColor.BLUE + "JumpCube" +
                ChatColor.DARK_GRAY + "] ";
    }

    public static Stream<String> getCubeNames() {
        return ExistingCube.getNames();
    }

    public static boolean validateSelection(CommandSender sender, Cube sel) {
        if (sel == null) {
            message(sender, ErrorColorizer, "No cube selected!");
            return false;
        }
        if (!(sel instanceof ExistingCube)) {
            message(sender, ErrorColorizer, "Cube %s is not finished!", sel.getCubeName());
            return false;
        }
        return true;
    }

    @Override
    public void onLoad() {
        cmdr.register(JumpCubeCommand.class);
        cmdr.register(this);

        super.onLoad();

        this.config = super.getConfig();
        this.logger = getLogger();

        BlockBar.initConfig(config);

        logger.info("JumpCube loaded!");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        final FileConfiguration config = getConfig();

        Optional.ofNullable(config.getString("cubes.created"))
                .map(str -> str.split(";"))
                .map(Arrays::asList)
                .ifPresent(list -> list.forEach(cubeName -> {
                    try {
                        ExistingCube.load(config, cubeName, null);
                        logger.info("Loaded cube: " + cubeName);
                    } catch (Throwable t) {
                        logger.throwing(ExistingCube.class.getName(), "load", t);
                    }
                }));

        logger.info("JumpCube enabled!");
        logger.info("Please report bugs at https://github.com/burdoto/jumpcube/issues");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        instance = null;

        logger.info("JumpCube disabled!");
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
        message(BukkitUtil.getPlayer(sender), ErrorColorizer, "You are missing the permission: %s", permission);
    }

    public static final class Permission {
        public static final String USER = "jumpcube.user";

        public static final String START_EARLY = "jumpcube.vip.earlystart";
        public static final String BRING_PLACEABLE = "jumpcube.vip.bringplaceable";

        public static final String TELEPORT_OUT = "jumpcube.mod.teleport";
        public static final String REGENERATE = "jumpcube.mod.regenerate";

        public static final String ADMIN = "jumpcube.admin";
        public static final String DEBUG_NOTIFY = "jumpcube.admin.debug";
    }
}
