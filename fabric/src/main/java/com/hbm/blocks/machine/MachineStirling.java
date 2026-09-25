// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.EnumGearType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityStirling;
import com.hbm.util.BobMathUtil;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityStirling.class, calling = "refreshHeatBelow")
public class MachineStirling extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 1, 1, 1};

    private final Tier tier;

    public MachineStirling(Tier tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityStirling(pos, state);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack stack,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityStirling stirling))
            return InteractionResult.PASS;

        EnumGearType gear = ModItems.GEAR_LARGE.typeOf(stack);
        if (stirling.hasCog || gear == null || gear.ordinal() != stirling.gearVariant()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        stack.consume(1, player);
        stirling.hasCog = true;
        stirling.setChanged();
        level.playSound(
                null,
                core.getX() + 0.5,
                core.getY() + 0.5,
                core.getZ() + 0.5,
                ModSounds.UPGRADE_PLUG.get(),
                SoundSource.BLOCKS,
                1.5F,
                0.75F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityStirling stirling)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(stirling.heat + "TU/t");
        info.line((stirling.hasCog ? stirling.powerBuffer : 0) + "HE/t");

        if (tier.creative) return;

        int maxHeat = stirling.maxHeat();
        double percent = (double) stirling.heat / maxHeat;
        int color =
                percent > 1D
                        ? 0xFF0000
                        : (((int) (0xFF - 0xFF * percent)) << 16) | (((int) (0xFF * percent)) << 8);
        info.line(
                String.format(Locale.US, "%.1f", stirling.heat * 1000 / maxHeat / 10D) + "%",
                color);

        if (stirling.heat > maxHeat) {
            info.line("! ! ! OVERSPEED ! ! !", BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
        }
        if (!stirling.hasCog) info.line("Gear missing!", 0xFF0000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerOut().fe();
    }

    public enum Tier {
        NORMAL(EnumGearType.LARGE.ordinal(), 300, false),
        STEEL(EnumGearType.LARGE_STEEL.ordinal(), 1500, false),

        CREATIVE(2, 1500, true);

        public final int gear;
        public final int maxHeat;
        public final boolean creative;

        Tier(int gear, int maxHeat, boolean creative) {
            this.gear = gear;
            this.maxHeat = maxHeat;
            this.creative = creative;
        }
    }
}
