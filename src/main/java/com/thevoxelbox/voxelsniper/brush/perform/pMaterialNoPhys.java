/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.thevoxelbox.voxelsniper.MagicValues;
import com.thevoxelbox.voxelsniper.Message;

import org.bukkit.block.Block;

/**
 * @author Voxel
 */
public class pMaterialNoPhys extends vPerformer {

    private int i;

    public pMaterialNoPhys() {
        name = "Set, No-Physics";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
        w = v.getWorld();
        i = v.getVoxelId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(Block b) {
        if (MagicValues.getIdFor(b.getType()) != i) {
            h.put(b);
            b.setBlockData(MagicValues.getBlockDataFor(i), false);
        }
    }
}
