// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blocks.BlockDirectionalBase;
import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.storage.BlockEntityMachineCapacitor;
import com.hbm.tileentity.machine.storage.CapacitorCharge;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

public class MachineCapacitor extends BlockDirectionalBase
        implements ITickingBlock, ICapabilityBlock, IPersistentInfoProvider {

    public final long maxPower;
    private final MapCodec<MachineCapacitor> ownCodec;

    public MachineCapacitor(long maxPower, Properties props) {
        super(props);
        this.maxPower = maxPower;
        this.ownCodec = simpleCodec(p -> new MachineCapacitor(maxPower, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return ownCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCapacitor(pos, state, maxPower);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CAPACITOR)
                .powerIn()
                .powerOut()
                .fe()
                .faces(BlockEntityMachineCapacitor.class, BlockEntityMachineCapacitor::acceptsFace);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        CapacitorCharge state = stack.get(ModDataComponents.CAPACITOR_STATE.get());
        if (state == null) return;
        adder.accept(
                Component.translatable(
                                "desc.shared.storesUpTo", BobMathUtil.getShortNumber(maxPower))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.shared.chargeSpeed",
                                BobMathUtil.getShortNumber(maxPower / 200))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.shared.dischargeSpeed",
                                BobMathUtil.getShortNumber(maxPower / 600))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.literal(
                                BobMathUtil.getShortNumber(state.power())
                                        + "/"
                                        + BobMathUtil.getShortNumber(state.maxPower())
                                        + "HE")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
