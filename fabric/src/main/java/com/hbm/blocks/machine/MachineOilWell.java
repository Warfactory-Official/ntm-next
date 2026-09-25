// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.tileentity.machine.oil.BlockEntityMachineOilWell;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineOilWell extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int[] DIMENSIONS = {9, 0, 1, 1, 1, 1};

    public MachineOilWell(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        return MultiblockHandlerXR.checkSpace(
                        level, placed, new int[] {1, -1, 0, 0, 0, 0}, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level, placed.above(), new int[] {8, 0, 1, 1, 1, 1}, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level, placed.offset(1, 1, 1), new int[] {-1, 1, 0, 0, 0, 0}, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level, placed.offset(1, 1, -1), new int[] {-1, 1, 0, 0, 0, 0}, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level, placed.offset(-1, 1, 1), new int[] {-1, 1, 0, 0, 0, 0}, placed, dir)
                && MultiblockHandlerXR.checkSpace(
                        level,
                        placed.offset(-1, 1, -1),
                        new int[] {-1, 1, 0, 0, 0, 0},
                        placed,
                        dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, new int[] {1, -1, 0, 0, 0, 0}, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(), new int[] {8, 0, 1, 1, 1, 1}, facing, visitor);
        MultiblockHandlerXR.visitBox(
                core.offset(1, 1, 1), new int[] {-1, 1, 0, 0, 0, 0}, facing, visitor);
        MultiblockHandlerXR.visitBox(
                core.offset(1, 1, -1), new int[] {-1, 1, 0, 0, 0, 0}, facing, visitor);
        MultiblockHandlerXR.visitBox(
                core.offset(-1, 1, 1), new int[] {-1, 1, 0, 0, 0, 0}, facing, visitor);
        MultiblockHandlerXR.visitBox(
                core.offset(-1, 1, -1), new int[] {-1, 1, 0, 0, 0, 0}, facing, visitor);
    }

    @Override
    public int coreMask() {
        return MASK_HORIZONTAL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineOilWell(pos, state);
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos core,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        boolean rupture =
                level.getBlockEntity(core) instanceof BlockEntityMachineOilWell well
                        && (well.tanks[0].getFill() > 0 || well.tanks[1].getFill() > 0);
        super.onExplosionHit(state, level, core, explosion, onHit);
        if (!rupture) return;

        double x = core.getX() + 0.5, y = core.getY() + 0.5, z = core.getZ() + 0.5;
        ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 15F);
        xnt.setBlockAllocator(new BlockAllocatorStandard(24));
        xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        xnt.setEntityProcessor(new EntityProcessorStandard());
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        ExplosionCreator.composeEffectSmall(level, x, y, z);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.DERRICK).powerIn().fluidOut().items().fe();
    }
}
