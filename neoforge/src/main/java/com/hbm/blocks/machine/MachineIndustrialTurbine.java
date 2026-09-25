// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.BlockEntityTurbineBase;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineIndustrialTurbine extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 3, 3, 1, 1};

    private static final String[] SPINNER = {"\u2596 ", "\u2598 ", " \u2598", " \u2596"};

    public MachineIndustrialTurbine(Properties props) {
        super(props);
    }

    static InteractionResult pullLever(
            Level level, BlockPos pos, Player player, BlockEntityTurbineBase turbine) {
        if (!level.isClientSide()) {
            if (!turbine.operational) {
                level.playSound(
                        null, pos, ModSounds.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                turbine.onLeverPull();
            } else {
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.block.industrialTurbine.cannotChangeCompressor")
                                .withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 3);
        BlockPos back = core.relative(facing, -1);

        visitor.cell(front.relative(rot), MASK_WEST);
        visitor.cell(front.relative(rot, -1), MASK_EAST);
        visitor.cell(back.relative(rot), MASK_WEST);
        visitor.cell(back.relative(rot, -1), MASK_EAST);
        visitor.cell(front.above(2), MASK_UP);
        visitor.cell(back.above(2), MASK_UP);
        visitor.cell(core.relative(facing, -3).above(), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 3);
        BlockPos back = core.relative(facing, -1);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(front.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(front.relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(back.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(back.relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(front.above(2), MASK_ALL, domains);
        visitor.passiveCell(back.above(2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3).above(), MASK_ALL, domains);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineIndustrialTurbine turbine)) {
            return InteractionResult.PASS;
        }
        Direction dir = coreState.getValue(FACING);
        BlockPos clicked = hit.getBlockPos();
        if (clicked.getX() == core.getX() + dir.getStepX() * 3
                && clicked.getZ() == core.getZ() + dir.getStepZ() * 3
                && clicked.getY() == core.getY() + 1) {
            return pullLever(level, clicked, player, turbine);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineIndustrialTurbine(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineIndustrialTurbine turbine))
            return;

        FluidTankNTM in = turbine.tanks[0];
        FluidTankNTM out = turbine.tanks[1];
        Fluid inType = in.getTankType();
        FT_Coolable trait = NTMFluidProperties.getTrait(inType, FT_Coolable.class);
        Fluid outType = trait != null ? trait.coolsTo() : NTMFluids.NONE;

        int color = ((int) (0xFF - 0xFF * turbine.spin)) << 16 | ((int) (0xFF * turbine.spin)) << 8;
        int frame = (int) ((level.getGameTime() / 4) % 4);

        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFFFF00, 0x404000);
        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(in.getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", in.getFill())
                        + "/"
                        + String.format(Locale.US, "%,d", in.getMaxFill())
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(outType)
                        + ": "
                        + String.format(Locale.US, "%,d", out.getFill())
                        + "/"
                        + String.format(Locale.US, "%,d", out.getMaxFill())
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.WHITE
                        + BobMathUtil.getShortNumber(turbine.powerBuffer)
                        + "HE ("
                        + ChatFormatting.RESET
                        + SPINNER[turbine.powerBuffer <= 0 ? 0 : frame]
                        + Math.round(turbine.spin * 100)
                        + "%"
                        + ChatFormatting.WHITE
                        + ")",
                color);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(
                        BlockEntityMachineIndustrialTurbine.class,
                        BlockEntityMachineIndustrialTurbine::acceptsFace)
                .fluidFaces(
                        BlockEntityMachineIndustrialTurbine.class,
                        (be, face) ->
                                face.fluid() == null || be.acceptsFluid(face.fluid(), face.side()));
    }
}
