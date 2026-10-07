// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blockentity.machine.storage.BatteryCharge;
import com.hbm.blockentity.machine.storage.BlockEntityMachineBattery;
import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.IGUIProvider;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineBattery extends BlockMachineHorizontal
        implements ICapabilityBlock, IPersistentInfoProvider, ILookOverlay {

    public final long maxPower;

    public MachineBattery(Properties props) {
        this(props, BlockEntityMachineBattery.MAX_POWER);
    }

    public MachineBattery(Properties props, long maxPower) {
        super(props);
        this.maxPower = maxPower;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityMachineBattery battery) {
            battery.refreshMode();
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineBattery(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            IGUIProvider.openBlockMenu(player, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineBattery battery)) return;
        info.title(getName(), 0xffff00, 0x404000).charge(battery.getPower(), battery.getMaxPower());
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BATTERY).powerIn().powerOut().items().fe();
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        BatteryCharge state = stack.get(ModDataComponents.MACHINE_BATTERY_STATE.get());
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
                                        + BobMathUtil.getShortNumber(maxPower)
                                        + "HE")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
