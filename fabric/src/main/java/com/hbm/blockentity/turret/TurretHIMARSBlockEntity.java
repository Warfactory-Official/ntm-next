/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.ArtilleryRocketEntity;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.AmmoHIMARSItem.HIMARSRocketType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TurretHIMARSBlockEntity extends TurretBaseArtilleryBlockEntity {

    private static final float CRANE_STEP = 0.0125F;
    private static final int SALVO_PERIOD = 40;
    private static final double FIRING_PITCH = Math.PI / 4;
    private static final float LAUNCH_SPEED = 25F;
    private static final int MODE_UNIT = 11, TYPE_UNIT = 12, ROCKETS_UNIT = 13, CRANE_UNIT = 14;
    private static final short AUTO = 0, MANUAL = 1;

    private boolean manual;
    private @Nullable HIMARSRocketType loadedType;
    private int rocketsLeft;
    public float crane;
    public float lastCrane;

    public TurretHIMARSBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_HIMARS.get(), pos, state);
    }

    public boolean isManual() {
        return manual;
    }

    public @Nullable HIMARSRocketType loadedType() {
        return loadedType;
    }

    public int rocketsLeft() {
        return rocketsLeft;
    }

    public boolean isLoaded() {
        return loadedType != null && rocketsLeft > 0;
    }

    public void enterManualMode() {
        hbm$syncSet$TurretHIMARSBlockEntity$manual(true);
        target = null;
        markChanged();
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return List.of();
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;
        List<ItemStack> stacks = new ArrayList<>();
        for (var rocket : ModItems.AMMO_HIMARS.creative()) stacks.add(new ItemStack(rocket.get()));
        ammoStacks = List.copyOf(stacks);
        return ammoStacks;
    }

    @Override
    protected boolean aimsManually() {
        return manual;
    }

    @Override
    public boolean doLOSCheck() {
        return false;
    }

    @Override
    protected void engage() {
        if (isLoaded() && crane <= 0F) {
            if (tPos != null) alignTurret();
            return;
        }
        turnTowardsAngle(0D, rotationYaw);
        if (!aligned) return;
        float next;
        if (isLoaded()) {
            next = crane - CRANE_STEP;
        } else {
            next = crane + CRANE_STEP;
            if (next >= 1F) loadSpareBox();
        }
        hbm$syncSet$TurretHIMARSBlockEntity$crane(Math.clamp(next, 0F, 1F));
    }

    private void loadSpareBox() {
        for (int slot = SLOT_AMMO_FIRST; slot <= SLOT_AMMO_LAST; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            HIMARSRocketType type = ModItems.AMMO_HIMARS.typeOf(stack);
            if (type == null) continue;
            hbm$syncSet$TurretHIMARSBlockEntity$loadedType(type);
            hbm$syncSet$TurretHIMARSBlockEntity$rocketsLeft(type.amount);
            removeItem(slot, 1);
            markChanged();
            return;
        }
    }

    @Override
    protected void alignTurret() {
        Vec3 pivot = getTurretPos();
        turnTowardsAngle(FIRING_PITCH, -Math.atan2(tPos.x - pivot.x, tPos.z - pivot.z));
    }

    @Override
    protected boolean readyToFire() {
        return aligned && crane <= 0F;
    }

    @Override
    public void updateFiringTick() {
        if (!beat(SALVO_PERIOD)) return;
        if (isLoaded() && tPos != null) {
            launch(loadedType, tPos);
            hbm$syncSet$TurretHIMARSBlockEntity$rocketsLeft((int) (rocketsLeft - 1));
            markChanged();
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.WEAPON_ROCKET_FLAME.get(),
                    SoundSource.BLOCKS,
                    25.0F,
                    1.0F);
        }
        finishFireWindow();
    }

    private void launch(HIMARSRocketType type, Vec3 aim) {
        Vec3 heading = alongBarrel(0.5, 0, 0);
        Vec3 from = getTurretPos().add(heading);
        var rocket = new ArtilleryRocketEntity(ModEntities.HIMARS.get(), level);
        rocket.snapTo(from.x, from.y, from.z, 0F, 0F);
        rocket.setThrowableHeading(heading.x, heading.y, heading.z, LAUNCH_SPEED, 0F);
        if (target != null) rocket.setTarget(target);
        else rocket.setTarget(aim.x, aim.y, aim.z);
        rocket.setType(type.ordinal());
        level.addFreshEntity(rocket);
    }

    @Override
    public void handleButtonPacket(int meta) {
        if (meta != 5) {
            super.handleButtonPacket(meta);
            return;
        }
        hbm$syncSet$TurretHIMARSBlockEntity$manual(!manual);
        abandonEngagement();
    }

    @Override
    public void tickClient() {
        lastCrane = crane;
        super.tickClient();
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    @Override
    public double getDetectorRange() {
        return 5000D;
    }

    @Override
    public double getDetectorGrace() {
        return 250D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 1D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 0.5D;
    }

    @Override
    public double getHeightOffset() {
        return 5D;
    }

    @Override
    public double getBarrelLength() {
        return 0.5D;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretHIMARS");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    private static boolean decodeMode(int mode) {
        return switch (mode) {
            case AUTO -> false;
            case MANUAL -> true;
            default -> throw new IllegalArgumentException("HIMARS mode " + mode);
        };
    }

    private static short encodeMode(boolean manual) {
        return manual ? MANUAL : AUTO;
    }

    private static @Nullable HIMARSRocketType decodeType(int ordinal) {
        return ordinal == -1 ? null : HIMARSRocketType.VALUES[ordinal];
    }

    private static int encodeType(@Nullable HIMARSRocketType type) {
        return type == null ? -1 : type.ordinal();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TurretHIMARSBlockEntity$manual(decodeMode(input.getShortOr("mode", AUTO)));
        hbm$syncSet$TurretHIMARSBlockEntity$loadedType(decodeType(input.getIntOr("type", -1)));
        hbm$syncSet$TurretHIMARSBlockEntity$rocketsLeft(input.getIntOr("ammo", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putShort("mode", encodeMode(manual));
        output.putInt("type", encodeType(loadedType));
        output.putInt("ammo", rocketsLeft);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask()
                | 1L << MODE_UNIT
                | 1L << TYPE_UNIT
                | 1L << ROCKETS_UNIT
                | 1L << CRANE_UNIT;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case MODE_UNIT -> output.writeShort(encodeMode(manual));
            case TYPE_UNIT -> output.writeShort(encodeType(loadedType));
            case ROCKETS_UNIT -> output.writeInt(rocketsLeft);
            case CRANE_UNIT -> output.writeFloat(crane);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case MODE_UNIT ->
                    hbm$syncSet$TurretHIMARSBlockEntity$manual(decodeMode(input.readShort()));
            case TYPE_UNIT ->
                    hbm$syncSet$TurretHIMARSBlockEntity$loadedType(decodeType(input.readShort()));
            case ROCKETS_UNIT -> hbm$syncSet$TurretHIMARSBlockEntity$rocketsLeft(input.readInt());
            case CRANE_UNIT -> hbm$syncSet$TurretHIMARSBlockEntity$crane(input.readFloat());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$TurretHIMARSBlockEntity$manual(boolean next) {
        if (this.manual != next) {
            this.manual = next;
            if ((1L << MODE_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << MODE_UNIT);
        }
    }

    public final void hbm$syncSet$TurretHIMARSBlockEntity$loadedType(HIMARSRocketType next) {
        if (this.loadedType != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.loadedType);
            this.loadedType = next;
            if (bound) bindSyncValue(next, 3, 1L << TYPE_UNIT);
            if ((1L << TYPE_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << TYPE_UNIT);
        }
    }

    public final void hbm$syncSet$TurretHIMARSBlockEntity$rocketsLeft(int next) {
        if (this.rocketsLeft != next) {
            this.rocketsLeft = next;
            if ((1L << ROCKETS_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << ROCKETS_UNIT);
        }
    }

    public final void hbm$syncSet$TurretHIMARSBlockEntity$crane(float next) {
        if (Float.floatToRawIntBits(this.crane) != Float.floatToRawIntBits(next)) {
            this.crane = next;
            if ((1L << CRANE_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << CRANE_UNIT);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                TurretHIMARSBlockEntity.class, "loadedType", 3, 1L << TYPE_UNIT);
    }
}
