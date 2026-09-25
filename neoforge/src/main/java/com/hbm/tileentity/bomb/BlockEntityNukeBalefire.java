// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuNukeFstbmb;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityNukeBalefire extends BlockEntityMachineBase
        implements IControlReceiver, IGUIProvider, SyncUnitSchema {

    public static final int SLOT_EGG = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_COUNT = 2;

    public static final int DEFAULT_TIMER = 18_000;
    public static final int PING_PERIOD = 20;
    public static final int DESTRUCTION_RANGE = 250;

    public static final int MIN_SECONDS = 1;
    public static final int MAX_SECONDS = 999;

    public static final int BATTERY_NONE = 0;
    public static final int BATTERY_SPARK = 1;
    public static final int BATTERY_TRIXITE = 2;

    @SyncField(units = 1L << 2)
    public boolean loaded;

    @SyncField(units = 1L << 1)
    public boolean started;

    @SyncField(units = 1L << 0)
    public int timer = DEFAULT_TIMER;

    public BlockEntityNukeBalefire(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_FSTBMB.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        loaded = isLoaded();

        if (!loaded) started = false;

        if (started) {
            timer--;
            if (timer % PING_PERIOD == 0) {
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.FSTBMB_PING.get(),
                        SoundSource.BLOCKS,
                        5.0F,
                        1.0F);
            }
        }

        if (timer <= 0) explode();
        networkPackNT(250);
    }

    public boolean isLoaded() {
        return hasEgg() && getBattery() > BATTERY_NONE;
    }

    public boolean hasEgg() {
        return getItem(SLOT_EGG).is(ModItems.EGG_BALEFIRE.get());
    }

    public int getBattery() {
        ItemStack stack = getItem(SLOT_BATTERY);
        if (stack.is(ModItems.BATTERY_SPARK.get())) return BATTERY_SPARK;
        if (stack.is(ModItems.BATTERY_TRIXITE.get())) return BATTERY_TRIXITE;
        return BATTERY_NONE;
    }

    public void explode() {
        if (!(level instanceof ServerLevel server)) return;
        clearContent();
        server.destroyBlock(worldPosition, false);

        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        EntityBalefire blast = new EntityBalefire(ModEntities.BALEFIRE.get(), server);
        blast.setPos(x, y, z);
        blast.destructionRange = DESTRUCTION_RANGE;
        server.addFreshEntity(blast);
        EntityNukeTorex.statFacBale(server, x, y, z, DESTRUCTION_RANGE);
    }

    public String getMinutes() {
        return pad(timer / 1200);
    }

    public String getSeconds() {
        return pad((timer / 20) % 60);
    }

    private static String pad(int value) {
        String text = Integer.toString(value);
        return text.length() == 1 ? "0" + text : text;
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("start") && isLoaded()) {
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.FSTBMB_START.get(),
                    SoundSource.BLOCKS,
                    5.0F,
                    1.0F);
            started = true;
        }
        if (data.contains("timer")) {
            timer = data.getIntOr("timer", timer / 20) * 20;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {

        return false;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(timer);
            case 1 -> output.writeBoolean(started);
            case 2 -> output.writeBoolean(loaded);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> timer = input.readInt();
            case 1 -> started = input.readBoolean();
            case 2 -> loaded = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        started = input.getBooleanOr("started", false);
        timer = input.getIntOr("timer", DEFAULT_TIMER);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("started", started);
        output.putInt("timer", timer);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeFstbmb");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuNukeFstbmb(containerId, playerInventory, this);
    }
}
