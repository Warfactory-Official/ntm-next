// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineAutosaw;
import com.hbm.tileentity.machine.BlockEntityThresher;
import com.hbm.util.I18nUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MachineAutosaw extends Block
        implements ITickingBlock, IToolable, ILookOverlay, ICapabilityBlock {

    public MachineAutosaw(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineAutosaw(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(stack.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineAutosaw be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null
                || type == Fluids.EMPTY
                || !BlockEntityThresher.acceptsFuel(NTMFluidProperties.kindOf(type)))
            return InteractionResult.PASS;

        be.tank.setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.autosawSet", NTMFluidProperties.getDisplayName(type)));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineAutosaw be)) return false;
        be.toggleSuspended();
        be.networkPackNT(25);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineAutosaw be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                NTMFluidProperties.clientName(be.tank.getFluid())
                        + ": "
                        + be.tank.getFill()
                        + "/"
                        + be.tank.getMaxFill()
                        + "mB");
        if (be.isSuspended)
            info.line(
                    "! " + I18nUtil.resolveKey(getDescriptionId() + ".suspended") + " !", 0xFF5555);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.AUTOSAW)
                .fluidIn()
                .faces(BlockEntityMachineAutosaw.class, BlockEntityMachineAutosaw::acceptsFace);
    }
}
