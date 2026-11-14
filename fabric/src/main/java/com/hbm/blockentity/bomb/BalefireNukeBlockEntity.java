/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.bomb.TntRule;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.BalefireEntity;
import com.hbm.interfaces.TriggerResult;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.NukeFstbmbMenu;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BalefireNukeBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {

    public static final int SLOT_EGG = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_COUNT = 2;
    public static final int DEFAULT_TIMER = 18000;
    public static final int MIN_SECONDS = 1;
    public static final int MAX_SECONDS = 999;
    public static final int BATTERY_NONE = 0;
    public static final int BATTERY_SPARK = 1;
    public static final int BATTERY_TRIXITE = 2;

    private static final int TIMER_UNIT = 0;
    private static final int RUNNING_UNIT = 1;
    private static final int READINESS_UNIT = 2;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = 1200;
    private static final int EFFECT_RANGE = 250;

    public int timer;

    {
        hbm$syncSet$BalefireNukeBlockEntity$timer(DEFAULT_TIMER);
    }

    public boolean started;
    public boolean loaded;

    public BalefireNukeBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.NUKE_FSTBMB.get(), position, state, SLOT_COUNT);
    }

    public boolean hasEgg() {
        return getItem(SLOT_EGG).is(ModItems.EGG_BALEFIRE.get());
    }

    public int getBattery() {
        var stack = getItem(SLOT_BATTERY);
        if (stack.is(ModItems.BATTERY_SPARK.get())) return BATTERY_SPARK;
        if (stack.is(ModItems.BATTERY_TRIXITE.get())) return BATTERY_TRIXITE;
        return BATTERY_NONE;
    }

    public boolean isLoaded() {
        return hasEgg() && getBattery() != BATTERY_NONE;
    }

    @Override
    public void tickServer() {
        hbm$syncSet$BalefireNukeBlockEntity$loaded(isLoaded());
        if (!loaded && started) {
            hbm$syncSet$BalefireNukeBlockEntity$started(false);
            setChanged();
        }
        if (started) {
            if (timer > Integer.MIN_VALUE) {
                hbm$syncSet$BalefireNukeBlockEntity$timer((int) (timer - 1));
                setChanged();
                if (timer % TICKS_PER_SECOND == 0) {
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition.getX(),
                            worldPosition.getY(),
                            worldPosition.getZ(),
                            ModSounds.FSTBMB_PING.get(),
                            SoundSource.BLOCKS,
                            5F,
                            1F);
                }
            }
            if (timer <= 0 && requestDetonation() == TriggerResult.DETONATED) return;
        }
        networkPackNT(EFFECT_RANGE);
    }

    public void startFromControl() {
        if (!isLoaded()) return;
        WorldSounds.machine(
                level,
                null,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                ModSounds.FSTBMB_START.get(),
                SoundSource.BLOCKS,
                5F,
                1F);
        if (!started) {
            hbm$syncSet$BalefireNukeBlockEntity$started(true);
            setChanged();
        }
    }

    public void setTimerSeconds(int seconds) {
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) return;
        int remaining = seconds * TICKS_PER_SECOND;
        if (timer != remaining) {
            hbm$syncSet$BalefireNukeBlockEntity$timer(remaining);
            setChanged();
        }
    }

    public TriggerResult requestDetonation() {
        if (!isLoaded()) return TriggerResult.MISSING_COMPONENT;
        if (!TntRule.explodes(level)) return TriggerResult.TNT_DISABLED;

        var serverLevel = (ServerLevel) level;
        clearContent();
        serverLevel.destroyBlock(worldPosition, false);
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        var blast = new BalefireEntity(ModEntities.BALEFIRE.get(), serverLevel);
        blast.setPos(x, y, z);
        blast.destructionRange = EFFECT_RANGE;
        serverLevel.addFreshEntity(blast);
        NukeTorexEntity.statFacBale(serverLevel, x, y, z, EFFECT_RANGE);
        return TriggerResult.DETONATED;
    }

    public String getMinutes() {
        String minutes = Integer.toString(timer / TICKS_PER_MINUTE);
        return minutes.length() == 1 ? "0" + minutes : minutes;
    }

    public String getSeconds() {
        String seconds = Integer.toString(timer / TICKS_PER_SECOND % 60);
        return seconds.length() == 1 ? "0" + seconds : seconds;
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeFstbmb");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new NukeFstbmbMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$BalefireNukeBlockEntity$started(input.getBooleanOr("started", false));
        hbm$syncSet$BalefireNukeBlockEntity$timer(input.getIntOr("timer", DEFAULT_TIMER));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("started", started);
        output.putInt("timer", timer);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case TIMER_UNIT -> output.writeInt(timer);
            case RUNNING_UNIT -> output.writeBoolean(started);
            case READINESS_UNIT -> output.writeBoolean(loaded);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case TIMER_UNIT -> hbm$syncSet$BalefireNukeBlockEntity$timer(input.readInt());
            case RUNNING_UNIT -> hbm$syncSet$BalefireNukeBlockEntity$started(input.readBoolean());
            case READINESS_UNIT -> hbm$syncSet$BalefireNukeBlockEntity$loaded(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$BalefireNukeBlockEntity$timer(int next) {
        if (this.timer != next) {
            this.timer = next;
            if ((1L << TIMER_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << TIMER_UNIT);
        }
    }

    public final void hbm$syncSet$BalefireNukeBlockEntity$started(boolean next) {
        if (this.started != next) {
            this.started = next;
            if ((1L << RUNNING_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << RUNNING_UNIT);
        }
    }

    public final void hbm$syncSet$BalefireNukeBlockEntity$loaded(boolean next) {
        if (this.loaded != next) {
            this.loaded = next;
            if ((1L << READINESS_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << READINESS_UNIT);
        }
    }
}
