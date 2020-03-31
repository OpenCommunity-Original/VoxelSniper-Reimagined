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
public class pMatCombo extends vPerformer {

    private byte dr;
    private int i;
    private int ir;

    public pMatCombo() {
        name = "Mat-Combo";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
        w = v.getWorld();
        dr = v.getReplaceData();
        i = v.getVoxelId();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.replace();
        vm.replaceData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(Block b) {
        if (MagicValues.getIdFor(b.getType()) == ir && b.getData() == dr) {
            h.put(b);
            b.setBlockData(MagicValues.getBlockDataFor(i), true);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
