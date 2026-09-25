// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.BlockEntityMachineTurbineGas;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class MachineTurbineGas extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 1, 1, 4, 5};

    public MachineTurbineGas(Properties props) {
        super(props);
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(-dx + rx, 0, -dz + rz), MASK_NORTH);
        visitor.cell(core.offset(dx + rx, 0, dz + rz), MASK_SOUTH);
        visitor.cell(core.offset(-dx + rx * -4, 0, -dz + rz * -4), MASK_NORTH);
        visitor.cell(core.offset(dx + rx * -4, 0, dz + rz * -4), MASK_SOUTH);
        visitor.cell(core.offset(rx * 4, 1, rz * 4), MASK_WEST);
        visitor.cell(core.offset(rx * -5, 1, rz * -5), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(core.offset(-dx + rx, 0, -dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx, 0, dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx + rx * -4, 0, -dz + rz * -4), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx * -4, 0, dz + rz * -4), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rx * 4, 1, rz * 4), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rx * -5, 1, rz * -5), MASK_ALL, domains);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineTurbineGas(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockPos hit = info.getHitPos();
        if (hit == null) return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineTurbineGas be)) return;

        Direction dir = BlockMultiblockCore.coreFacing(level.getBlockState(pos));
        Direction rot = dir.getClockWise();
        BlockPos front = pos.relative(dir);
        BlockPos back = pos.relative(dir.getOpposite());

        if (hit.equals(back.relative(rot)) || hit.equals(front.relative(rot))) {
            info.line(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tankName(be.tanks[0]));
            info.line(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tankName(be.tanks[1]));
        }

        if (hit.equals(back.relative(rot, -4)) || hit.equals(front.relative(rot, -4))) {
            info.line(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tankName(be.tanks[2]));
        }

        if (hit.equals(pos.relative(rot, -5).above())) {
            info.line(ChatFormatting.RED + "<- " + ChatFormatting.RESET + tankName(be.tanks[3]));
        }

        if (hit.equals(pos.relative(rot, 4).above())) {
            info.line(ChatFormatting.RED + "<- " + ChatFormatting.RESET + "Power");
        }

        if (!info.isEmpty()) {
            info.title(getName().getString(), 0xFFFF00, 0x404000);
        }
    }

    private static String tankName(FluidTankNTM tank) {
        Fluid type = tank.getFluid();
        return type == null ? "None" : NTMFluidProperties.clientName(type);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MACHINE_GASTURBINE)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .items()
                .fe()
                .faces(BlockEntityMachineTurbineGas.class, (be, side) -> side != Direction.DOWN)
                .fluidFaces(
                        BlockEntityMachineTurbineGas.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }
}
