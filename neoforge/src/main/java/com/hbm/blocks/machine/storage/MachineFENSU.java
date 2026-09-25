// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.storage.BatteryCharge;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBattery;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFENSU;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineFENSU extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, ILookOverlay, IPersistentInfoProvider {

    public static final MapCodec<MachineFENSU> CODEC = simpleCodec(MachineFENSU::new);

    private static final int[] DIMENSIONS = {4, 0, 1, 1, 2, 2};

    public MachineFENSU(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
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
    public int coreMask() {
        return MASK_DOWN;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FENSU).powerIn().powerOut().items().fe();
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.getBlockEntity(core) instanceof BlockEntityMachineBattery battery)
            battery.refreshMode();
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        info.title(getName().getString(), 0xffff00, 0x404000);
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineBattery battery)) return;

        info.line(
                BobMathUtil.getShortNumber(battery.getPower())
                        + " / "
                        + BobMathUtil.getShortNumber(battery.getMaxPower())
                        + "HE");

        double percent = (double) battery.getPower() / (double) battery.getMaxPower();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        info.line((charge / 100D) + "%", color);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineFENSU(pos, state);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        BatteryCharge state = stack.get(ModDataComponents.MACHINE_BATTERY_STATE.get());
        if (state == null) return;
        adder.accept(
                Component.literal(
                                BobMathUtil.getShortNumber(state.power())
                                        + "/"
                                        + BobMathUtil.getShortNumber(Long.MAX_VALUE)
                                        + "HE")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
