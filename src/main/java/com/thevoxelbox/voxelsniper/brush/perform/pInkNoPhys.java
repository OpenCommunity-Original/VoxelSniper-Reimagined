package com.thevoxelbox.voxelsniper.brush.perform;

import com.thevoxelbox.voxelsniper.MagicValues;
import org.bukkit.block.Block;

import com.thevoxelbox.voxelsniper.Message;

/**
 * @author Voxel
 */
public class pInkNoPhys extends vPerformer {

    private byte d;

    public pInkNoPhys() {
        name = "Ink, No Physics";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
        w = v.getWorld();
        d = v.getData();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.data();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(Block b) {
        h.put(b);
        b.setBlockData(MagicValues.getBlockDataFor(MagicValues.getIdFor(b.getType()), d), false);
    }
}
