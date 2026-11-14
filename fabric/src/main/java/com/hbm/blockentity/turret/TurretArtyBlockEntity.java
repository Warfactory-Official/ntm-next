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
import com.hbm.entity.projectile.ArtilleryShellEntity;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.AmmoArtyItem;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.particle.helper.CasingCreator;
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

public class TurretArtyBlockEntity extends TurretBaseArtilleryBlockEntity {

    public static final short MODE_ARTILLERY = 0;
    public static final short MODE_CANNON = 1;
    public static final short MODE_MANUAL = 2;

    public short mode;

    {
        hbm$syncSet$TurretArtyBlockEntity$mode((short) (0));
    }

    public double barrelPos;
    public double lastBarrelPos;

    private boolean didJustShoot;
    private boolean retracting;

    public TurretArtyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_ARTY.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return List.of();
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        for (AmmoArtyItem.ArtilleryShellType type : AmmoArtyItem.ArtilleryShellType.values()) {
            stacks.add(ModItems.AMMO_ARTY.stack(type));
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretArty");
    }

    @Override
    public long getMaxPower() {
        return 100000;
    }

    @Override
    public double getBarrelLength() {
        return 9D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 0;
    }

    @Override
    public double getHeightOffset() {
        return 3D;
    }

    @Override
    public double getDetectorRange() {
        return mode == MODE_CANNON ? 250D : 3000D;
    }

    @Override
    public double getDetectorGrace() {
        return mode == MODE_CANNON ? 32D : 250D;
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
    public double getTurretDepression() {
        return 30D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public int getDetectorInterval() {
        return mode == MODE_CANNON ? 20 : 200;
    }

    @Override
    public boolean doLOSCheck() {
        return mode == MODE_CANNON;
    }

    @Override
    protected void alignTurret() {
        Vec3 pos = barrelTip();
        Vec3 delta = new Vec3(tPos.x - pos.x, tPos.y - pos.y, tPos.z - pos.z);
        double targetYaw = -Math.atan2(delta.x, delta.z);

        double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double y = delta.y;
        double v0 = getV0();
        double v02 = v0 * v0;
        double g = 9.81 * 0.05;
        double upperLower = mode == MODE_CANNON ? -1 : 1;
        double targetPitch =
                Math.atan(
                        (v02 + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02)) * upperLower)
                                / (g * x));

        turnTowardsAngle(targetPitch, targetYaw);
    }

    public double getV0() {
        return mode == MODE_CANNON ? 20D : 50D;
    }

    public ItemStack getShellLoaded() {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (ModItems.AMMO_ARTY.typeOf(stack) != null) return stack;
        }

        return ItemStack.EMPTY;
    }

    public void consumeShell() {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            if (ModItems.AMMO_ARTY.typeOf(getItem(i)) != null) {
                removeItem(i, 1);
                return;
            }
        }

        setChanged();
    }

    public void spawnShell(ItemStack type) {
        Vec3 vec = alongBarrel(getBarrelLength(), 0, 0);
        Vec3 pos = getTurretPos().add(vec);

        ArtilleryShellEntity proj =
                new ArtilleryShellEntity(ModEntities.ARTILLERY_SHELL.get(), level);
        proj.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        proj.setThrowableHeading(vec.x, vec.y, vec.z, (float) getV0(), 0.0F);
        proj.setDeltaMovement(proj.getDeltaMovement().add(0, proj.getGravityVelocity() * 0.5, 0));
        proj.setTarget((int) tPos.x, (int) tPos.y, (int) tPos.z);
        proj.setType(ModItems.AMMO_ARTY.typeOf(type).ordinal());

        if (ModItems.AMMO_ARTY.is(type, AmmoArtyItem.ArtilleryShellType.CARGO)) {
            var cargo = type.get(ModDataComponents.ARTY_CARGO.get());
            if (cargo != null) proj.setCargo(cargo.create());
        }

        if (mode != MODE_CANNON) proj.setWhistle(true);

        level.addFreshEntity(proj);

        casingDelay = casingDelay();
    }

    @Override
    public int casingDelay() {
        return 7;
    }

    @Override
    public void tickClient() {
        lastBarrelPos = barrelPos;

        if (retracting) {
            barrelPos += 0.5;
            if (barrelPos >= 1) retracting = false;
        } else {
            barrelPos -= 0.05;
            if (barrelPos < 0) barrelPos = 0;
        }

        super.tickClient();
    }

    @Override
    protected boolean aimsManually() {
        return mode == MODE_MANUAL;
    }

    @Override
    protected void afterSync() {
        hbm$syncSet$TurretArtyBlockEntity$didJustShoot(false);
        if (casingDelay > 0) casingDelay--;
        else spawnCasing();
    }

    @Override
    public void updateFiringTick() {
        if (beat(mode == MODE_ARTILLERY ? 300 : 40)) {
            ItemStack conf = getShellLoaded();

            if (!conf.isEmpty()) {
                cachedCasingConfig = ModItems.AMMO_ARTY.typeOf(conf).casing;
                spawnShell(conf);
                consumeShell();
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.TURRET_JEREMY_FIRE.get(),
                        SoundSource.BLOCKS,
                        25.0F,
                        1.0F);
                hbm$syncSet$TurretArtyBlockEntity$didJustShoot(true);
                muzzleFlash(barrelTip(), 0F, 5);
            }

            finishFireWindow();
        }
    }

    @Override
    protected void spawnCasing() {
        if (cachedCasingConfig == null) return;

        Vec3 spawn = getCasingSpawnPos();
        CasingCreator.composeEffect(
                level,
                spawn.x,
                spawn.y,
                spawn.z,
                (float) Math.toDegrees(rotationYaw),
                (float) -Math.toDegrees(rotationPitch),
                -0.6,
                0.3,
                0,
                0.01,
                level.getRandom().nextFloat() * 20F - 10F,
                0,
                cachedCasingConfig.name(),
                true,
                200,
                1,
                20);

        cachedCasingConfig = null;
    }

    @Override
    public void handleButtonPacket(int meta) {
        if (meta == 5) {
            hbm$syncSet$TurretArtyBlockEntity$mode((short) (mode + 1));
            if (mode > 2) hbm$syncSet$TurretArtyBlockEntity$mode((short) (0));

            abandonEngagement();
        } else {
            super.handleButtonPacket(meta);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TurretArtyBlockEntity$mode((short) input.getShortOr("mode", (short) 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putShort("mode", mode);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & (1L << 12)) != 0) retracting = didJustShoot;
    }

    @Override
    public void afterInitialSyncUnits() {
        retracting = false;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1800L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> output.writeShort(this.mode);
            case 12 -> output.writeBoolean(this.didJustShoot);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.hbm$syncSet$TurretArtyBlockEntity$mode(input.readShort());
            case 12 -> this.hbm$syncSet$TurretArtyBlockEntity$didJustShoot(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$TurretArtyBlockEntity$mode(short next) {
        if (this.mode != next) {
            this.mode = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }

    public final void hbm$syncSet$TurretArtyBlockEntity$didJustShoot(boolean next) {
        if (this.didJustShoot != next) {
            this.didJustShoot = next;
            if ((1L << 12) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 12);
        }
    }
}
