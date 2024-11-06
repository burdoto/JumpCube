package de.kaleidox.jumpcube.cube;

import org.bukkit.World;

public interface Cube {
    String getCubeName();

    int[][] getPositions();

    int getGalleryHeight();

    int getHeight();

    int getBottom();

    BlockPool getBlockPool();

    World getWorld();

    void delete();
}
