package de.kaleidox.jumpcube;

import de.kaleidox.jumpcube.chat.MessageLevel;
import de.kaleidox.jumpcube.cube.BlockBar;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.CubeCreationTool;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.exception.InnerCommandException;
import de.kaleidox.jumpcube.exception.InvalidArgumentCountException;
import de.kaleidox.jumpcube.exception.NoSuchCubeException;
import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.chat.MessageLevel.ERROR;
import static de.kaleidox.jumpcube.chat.MessageLevel.INFO;

public final class JumpCube extends JavaPlugin {
    public static final Random rng = new Random();
    @Nullable
    public static JumpCube instance;

    public Map<UUID, Cube> selections = new ConcurrentHashMap<>();
    private Logger logger;

    private static boolean validateSelection(CommandSender sender, Cube sel) {
        if (sel == null) {
            message(sender, ERROR, "No cube selected!");
            return false;
        }
        if (!(sel instanceof ExistingCube)) {
            message(sender, ERROR, "Cube %s is not finished!", sel.getCubeName());
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        UUID senderUuid = BukkitUtil.getUuid(sender);

        try {
            switch (label) {
                case "jumpcube":
                case "jc":
                    if (!checkPerm(sender, Permission.USER)) return true;
                    if (args.length == 0) {
                        message(sender, INFO, "JumpCube version %s", getDescription().getVersion());
                        return true;
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                        message(sender, ERROR, "Reloading not yet implemented");
                        return true;
                    } else {
                        subCommand(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
                        return true;
                    }
            }
        } catch (InnerCommandException cEx) {
            message(sender, cEx.getLevel(), cEx.getIngameText());
            if (cEx.getLevel() == MessageLevel.EXCEPTION)
                cEx.printStackTrace(System.out);
        }

        return super.onCommand(sender, command, label, args);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        List<String> list = new ArrayList<>();

        switch (alias) {
            case "jumpcube":
            case "jc":
                switch (args.length) {
                    case 1:
                        if (sender.hasPermission(Permission.USER)) {
                            list.add("join");
                            list.add("leave");
                            list.add("select");
                        }
                        if (sender.hasPermission(Permission.START_EARLY)) {
                            list.add("start");
                        }
                        if (sender.hasPermission(Permission.REGENERATE)) {
                            list.add("regenerate");
                            list.add("regenerate-complete");
                        }
                        if (sender.hasPermission(Permission.ADMIN)) {
                            list.add("create");
                            if (selections.get(BukkitUtil.getUuid(sender)) instanceof CubeCreationTool) {
                                // user is currently creating a cube
                                list.add("pos");
                                list.add("bar");
                                list.add("confirm");
                            }
                        }
                        break;
                    case 2:
                        switch (args[0]) {
                            case "pos":
                                if (sender.hasPermission(Permission.ADMIN)) {
                                    list.add("1");
                                    list.add("2");
                                }
                                break;
                        }
                        break;
                }
                break;
        }

        if (args.length > 0)
            list.removeIf(word -> word.indexOf(args[args.length - 1]) != 0);

        return list;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        this.logger = getLogger();

        saveConfig();
        saveDefaultConfig();

        final FileConfiguration config = getConfig();
        if (!config.isSet("cube.defaults.bar.a"))
            config.set("cube.defaults.bar.a", "red_wool");
        if (!config.isSet("cube.defaults.bar.b"))
            config.set("cube.defaults.bar.b", "yellow_wool");
        if (!config.isSet("cube.defaults.bar.c"))
            config.set("cube.defaults.bar.c", "blue_wool");
        if (!config.isSet("cube.defaults.bar.placeable"))
            config.set("cube.defaults.bar.placeable", "pumpkin");
        if (!config.isSet("cube.defaults.bar.aFix"))
            config.set("cube.defaults.bar.aFix", "red_concrete");
        if (!config.isSet("cube.defaults.bar.bFix"))
            config.set("cube.defaults.bar.bFix", "yellow_concrete");
        if (!config.isSet("cube.defaults.bar.cFix"))
            config.set("cube.defaults.bar.cFix", "blue_concrete");
        if (!config.isSet("cube.defaults.bar.dFix"))
            config.set("cube.defaults.bar.dFix", "light_gray_concrete");
        if (!config.isSet("cube.defaults.height"))
            config.set("cube.defaults.height", 120);

        saveConfig();

        BlockBar.initConfig(config);

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
                        logger.throwing(
                                ExistingCube.class.getName(),
                                "load",
                                new RuntimeException("Error loading cube: " + cubeName, t)
                        );
                    }
                }));

        logger.info("JumpCube enabled!");
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

    private void subCommand(CommandSender sender, String subCommand, String[] args) {
        final UUID senderUuid = BukkitUtil.getUuid(sender);
        Cube sel = null;
        if (subCommand.indexOf("sel") != 0)
            sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        switch (subCommand.toLowerCase()) {
            case "create":
                if (!checkPerm(sender, Permission.ADMIN)) return;
                if (args.length != 1) throw new InvalidArgumentCountException(1, args.length);

                if (ExistingCube.exists(args[0])) {
                    message(sender, ERROR, "A cube with the name %s already exists!", args[0]);
                    return;
                }

                if (sel instanceof CubeCreationTool && !((CubeCreationTool) sel).isReady()) {
                    // delete old, nonready selection first
                    sel.delete();
                    selections.remove(senderUuid);
                }

                CubeCreationTool creationTool = new CubeCreationTool(BukkitUtil.getPlayer(sender));
                creationTool.setName(args[0]);
                selections.put(senderUuid, creationTool);
                message(sender, INFO, "Cube %s creation started!", args[0]);
                return;
            case "sel":
            case "select":
                if (!checkPerm(sender, Permission.USER)) return;
                if (args.length != 1) throw new InvalidArgumentCountException(1, args.length);

                if (sel != null && sel.getCubeName().equals(args[0])) {
                    message(sender, INFO, "Cube %s is already selected!", args[0]);
                    return;
                }
                if (!ExistingCube.exists(args[0])) throw new NoSuchCubeException(args[0]);

                ExistingCube cube = ExistingCube.get(args[0]);
                assert cube != null;
                selections.put(senderUuid, cube);
                message(sender, INFO, "Cube %s selected!", args[0]);
                return;
            case "pos":
            case "pos1":
            case "pos2":
                if (!checkPerm(sender, Permission.ADMIN)) return;
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                CubeCreationTool.Commands.pos(sender, sel, subCommand, args);
                return;
            case "bar":
                if (!checkPerm(sender, Permission.ADMIN)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                CubeCreationTool.Commands.bar(sender, sel, args);
                return;
            case "confirm":
                if (!checkPerm(sender, Permission.ADMIN)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                CubeCreationTool.Commands.confirm(sender, sel);
                return;
            // Game commands
            case "regenerate":
            case "regen":
                if (!checkPerm(sender, Permission.REGENERATE)) return;
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                if (!validateSelection(sender, sel)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                ExistingCube.Commands.regenerate(sender, sel, false);
                return;
            case "regenerate-complete":
            case "regen-complete":
                if (!checkPerm(sender, Permission.REGENERATE)) return;
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                if (!validateSelection(sender, sel)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                ExistingCube.Commands.regenerate(sender, sel, true);
                return;
            case "join":
                if (!checkPerm(sender, Permission.USER)) return;
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                if (!validateSelection(sender, sel)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                ((ExistingCube) sel).manager.join(sender);
                return;
            case "leave":
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                if (!validateSelection(sender, sel)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                ((ExistingCube) sel).manager.leave(sender);
                return;
            case "start":
                if (!checkPerm(sender, Permission.START_EARLY)) return;
                if (sel == null)
                    throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
                if (!validateSelection(sender, sel)) return;
                if (args.length != 0) throw new InvalidArgumentCountException(0, args.length);
                ((ExistingCube) sel).manager.start();
                return;
            case "test":
                if (!sender.isOp()) return;
                for (MessageLevel lvl : MessageLevel.values()) message(sender, lvl, "I am a %s.", "value");
        }
    }

    private void messagePerm(CommandSender sender, String permission) {
        message(BukkitUtil.getPlayer(sender), ERROR, "You are missing the permission: %s", permission);
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
