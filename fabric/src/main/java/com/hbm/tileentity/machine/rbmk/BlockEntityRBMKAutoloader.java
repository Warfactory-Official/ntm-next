// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.container.MenuRBMKAutoloader;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRBMKAutoloader extends BlockEntityMachineBase
        implements IControlReceiver, ICopiable, Audible, MenuProvider, SyncUnitSchema {

    public static final double SPEED = 0.005D;
    public static final int SLOTS = 18;

    public static final int OUTPUT_START = 9;
    private static final int DELAY = 40;
    private static final int[] ALL_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
    };
    private static final int[] NO_SLOTS = {};

    public static Consumer<BlockEntityRBMKAutoloader> CLIENT_SOUND = be -> {};

    public static Consumer<BlockEntityRBMKAutoloader> CLIENT_TOWER = be -> {};

    @SyncField(units = 1L)
    public double piston;

    public double renderPiston;
    public double lastPiston;
    public @Nullable AudioWrapper audioLift;

    @SyncField(units = 1L << 1)
    public int cycle = 50;

    public boolean isRetracting = true;
    private double syncPiston;
    private int turnProgress;
    private int delay = 0;

    public BlockEntityRBMKAutoloader(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_AUTOLOADER.get(), pos, state, SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkAutoloader");
    }

    @Override
    public void tickServer() {
        if (delay > 0) delay--;

        if (delay <= 0 && isRetracting && piston > 0D) {
            piston -= SPEED;
            if (piston <= 0) {
                piston = 0;
                delay = DELAY;
            }
        }

        if (isRetracting && level.getGameTime() % 20 == 0 && hasFuel() && hasSpace()) {
            BlockEntityRBMKRod rod = channel();
            if (rod != null && rod.coldEnoughForAutoloader()) {
                ItemStack loaded = rod.getItem(0);
                if (loaded.isEmpty()
                        || (loaded.getItem() instanceof ItemRBMKRod
                                && ItemRBMKRod.getEnrichment(loaded) * 100 < cycle)) {
                    isRetracting = false;
                }
            }
        }

        if (delay <= 0 && !isRetracting && piston < 1D) {
            piston += SPEED;
            if (piston >= 1) {
                piston = 1;
                delay = DELAY;
            }
        }

        if (!isRetracting && piston >= 1D) {
            piston = 1D;

            BlockEntityRBMKRod rod = channel();
            if (rod != null) {
                ItemStack loaded = rod.getItem(0);
                if (!loaded.isEmpty() && hasSpace()) {
                    for (int i = OUTPUT_START; i < SLOTS; i++) {
                        if (getItem(i).isEmpty()) {
                            setItem(i, loaded.copy());
                            rod.setItem(0, ItemStack.EMPTY);
                            break;
                        }
                    }
                }
                if (rod.getItem(0).isEmpty()) {
                    for (int i = 0; i < OUTPUT_START; i++) {
                        ItemStack stack = getItem(i);
                        if (!stack.isEmpty()
                                && stack.getItem() instanceof ItemRBMKRod
                                && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle) {
                            rod.setItem(0, stack.copy());
                            setItem(i, ItemStack.EMPTY);
                            break;
                        }
                    }
                }

                isRetracting = true;
                delay = DELAY;
            }
        }

        networkPackNT(100);
    }

    private @Nullable BlockEntityRBMKRod channel() {
        BlockPos core = MultiblockSurface.coreOfAny(level, worldPosition.below());
        if (core == null) return null;
        return level.getBlockEntity(core) instanceof BlockEntityRBMKRod rod ? rod : null;
    }

    @Override
    public void tickClient() {
        lastPiston = renderPiston;

        if (turnProgress > 0) {
            renderPiston = renderPiston + ((syncPiston - renderPiston) / turnProgress);
            --turnProgress;
        } else {
            renderPiston = syncPiston;
        }

        CLIENT_SOUND.accept(this);
        if (renderPiston > 0.99) CLIENT_TOWER.accept(this);
    }

    public boolean hasFuel() {
        for (int i = 0; i < OUTPUT_START; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()
                    && stack.getItem() instanceof ItemRBMKRod
                    && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpace() {
        for (int i = OUTPUT_START; i < SLOTS; i++) if (getItem(i).isEmpty()) return true;
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemRBMKRod
                && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle
                && slot < OUTPUT_START;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return piston <= 0 ? ALL_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        piston = input.getDoubleOr("piston", 0D);
        isRetracting = input.getBooleanOr("ret", false);
        delay = input.getIntOr("delay", 0);
        cycle = input.getIntOr("cycle", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("piston", piston);
        output.putBoolean("ret", isRetracting);
        output.putInt("delay", delay);
        output.putInt("cycle", cycle);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("minus") && cycle > 5) cycle -= 5;
        if (data.contains("plus") && cycle < 95) cycle += 5;
        cycle = Mth.clamp(cycle, 5, 95);
        markChanged();
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putInt("cycle", cycle);
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        nbt.getInt("cycle").ifPresent(value -> cycle = Mth.clamp(value, 5, 95));
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKAutoloader(id, inv, this);
    }

    private void readPiston(ByteBuf input) {
        syncPiston = input.readDouble();
    }

    @Override
    public void afterSyncUnits(long units) {
        turnProgress = 2;
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(this.piston);
            case 1 -> output.writeInt(this.cycle);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPiston(input);
            case 1 -> this.cycle = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
