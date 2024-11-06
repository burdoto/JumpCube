package de.kaleidox.jumpcube.cube;

import org.bukkit.World;

public interface Cube {
    String getCubeName();

    int[][] getPositions();

    int getGalleryHeight();

    int getHeight();

    int getBottom();

    BlockPool getBlockBar();

    World getWorld();

    void delete();
}
