// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuNukeCustom;
import com.hbm.items.ModItems;
import com.hbm.tileentity.BlockEntityMachineBase;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukeCustom extends BlockEntityMachineBase implements IGUIProvider {

    public static final int SLOT_COUNT = 27;

    public static final float NUKE_NEEDS_TNT = 16F;
    public static final float HYDRO_NEEDS_NUKE = 100F;
    public static final float AMAT_NEEDS_NUKE = 50F;
    public static final float SCHRAB_NEEDS_NUKE = 50F;

    public BlockEntityNukeCustom(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_CUSTOM.get(), pos, state, SLOT_COUNT);
    }

    public Totals totals() {
        float[] level = new float[CustomNukeEntries.Stage.values().length];
        float[] mod = new float[level.length];
        Arrays.fill(mod, 1F);

        for (ItemStack stack : inventory) {
            CustomNukeEntries.Entry entry = CustomNukeEntries.of(stack);
            if (entry == null) continue;
            int stage = entry.stage().ordinal();

            if (entry.multiplier()) {

                if (entry.stage() != CustomNukeEntries.Stage.EUPH) {
                    mod[stage] *= entry.value() * stack.getCount();
                }
            } else {
                level[stage] += entry.value() * stack.getCount();
            }
        }

        float tnt =
                level[CustomNukeEntries.Stage.TNT.ordinal()]
                        * mod[CustomNukeEntries.Stage.TNT.ordinal()];
        float nuke =
                level[CustomNukeEntries.Stage.NUKE.ordinal()]
                        * mod[CustomNukeEntries.Stage.NUKE.ordinal()];
        float hydro =
                level[CustomNukeEntries.Stage.HYDRO.ordinal()]
                        * mod[CustomNukeEntries.Stage.HYDRO.ordinal()];
        float amat =
                level[CustomNukeEntries.Stage.AMAT.ordinal()]
                        * mod[CustomNukeEntries.Stage.AMAT.ordinal()];
        float dirty =
                level[CustomNukeEntries.Stage.DIRTY.ordinal()]
                        * mod[CustomNukeEntries.Stage.DIRTY.ordinal()];
        float schrab =
                level[CustomNukeEntries.Stage.SCHRAB.ordinal()]
                        * mod[CustomNukeEntries.Stage.SCHRAB.ordinal()];
        float euph = level[CustomNukeEntries.Stage.EUPH.ordinal()];

        if (tnt < NUKE_NEEDS_TNT) nuke = 0;
        if (nuke < HYDRO_NEEDS_NUKE) hydro = 0;
        if (nuke < AMAT_NEEDS_NUKE) amat = 0;
        if (nuke < SCHRAB_NEEDS_NUKE) schrab = 0;
        if (schrab == 0) euph = 0;
        return new Totals(tnt, nuke, hydro, amat, dirty, schrab, euph);
    }

    public record Totals(
            float tnt, float nuke, float hydro, float amat, float dirty, float schrab, float euph) {

        public float nukeAdj() {
            return nuke == 0 ? 0 : Math.min(nuke + tnt / 2, ExplosionNukeCustom.MAX_NUKE);
        }

        public float hydroAdj() {
            return hydro == 0
                    ? 0
                    : Math.min(hydro + nuke / 2 + tnt / 4, ExplosionNukeCustom.MAX_HYDRO);
        }

        public float amatAdj() {
            return amat == 0
                    ? 0
                    : Math.min(amat + hydro / 2 + nuke / 4 + tnt / 8, ExplosionNukeCustom.MAX_AMAT);
        }

        public float schrabAdj() {
            if (schrab == 0) return 0;
            return Math.min(
                    schrab + amat / 2 + hydro / 4 + nuke / 8 + tnt / 16,
                    ExplosionNukeCustom.MAX_SCHRAB);
        }
    }

    public boolean isFalling() {
        for (ItemStack stack : inventory) {
            if (stack.is(ModItems.CUSTOM_FALL.get())) return true;
        }
        return false;
    }

    public void clearSlots() {
        clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {

        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeCustom");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuNukeCustom(containerId, playerInventory, this);
    }
}
