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

import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TurretMaxwellBlockEntity extends TurretBaseNTBlockEntity
        implements UpgradeInfoProvider {
    private static final int DEATH_TICKS = 30 * 60 * 20;
    private static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 27)
                    .and(UpgradeType.POWER, 27)
                    .and(UpgradeType.EFFECT, 27)
                    .and(UpgradeType.AFTERBURN, 27)
                    .and(UpgradeType.OVERDRIVE, 27);

    public int beam;
    public double lastDist;

    private int redLevel;
    private int greenLevel;
    private int blueLevel;
    private int blackLevel;
    private int pinkLevel;
    private boolean fiveG;
    private boolean screm;
    private int checkDelay;
    private int shotSequence;
    private int clientShotSequence;

    public TurretMaxwellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_MAXWELL.get(), pos, state);
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretMaxwell");
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return null;
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        ammoStacks =
                List.of(
                        new ItemStack(ModItems.UPGRADE_5G),
                        new ItemStack(ModItems.UPGRADE_SPEED_1),
                        new ItemStack(ModItems.UPGRADE_SPEED_2),
                        new ItemStack(ModItems.UPGRADE_SPEED_3),
                        new ItemStack(ModItems.UPGRADE_EFFECT_1),
                        new ItemStack(ModItems.UPGRADE_EFFECT_2),
                        new ItemStack(ModItems.UPGRADE_EFFECT_3),
                        new ItemStack(ModItems.UPGRADE_POWER_1),
                        new ItemStack(ModItems.UPGRADE_POWER_2),
                        new ItemStack(ModItems.UPGRADE_POWER_3),
                        new ItemStack(ModItems.UPGRADE_AFTERBURN_1),
                        new ItemStack(ModItems.UPGRADE_AFTERBURN_2),
                        new ItemStack(ModItems.UPGRADE_AFTERBURN_3),
                        new ItemStack(ModItems.UPGRADE_OVERDRIVE_1),
                        new ItemStack(ModItems.UPGRADE_OVERDRIVE_2),
                        new ItemStack(ModItems.UPGRADE_OVERDRIVE_3),
                        new ItemStack(ModItems.UPGRADE_SCREM));
        return ammoStacks;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 2;
    }

    @Override
    public double getDetectorGrace() {
        return 5D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 9D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 6D;
    }

    @Override
    public double getTurretElevation() {
        return 40D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDetectorRange() {
        return 64D + greenLevel * 3;
    }

    @Override
    public long getMaxPower() {
        return 10000000;
    }

    @Override
    public long getConsumption() {
        return fiveG ? 10L : 10000 - blueLevel * 300L;
    }

    @Override
    public double getBarrelLength() {
        return 2.125D;
    }

    @Override
    public double getHeightOffset() {
        return 2D;
    }

    @Override
    public void tickServer() {
        if (checkDelay <= 0) {
            checkDelay = 20;
            redLevel = 0;
            greenLevel = 0;
            blueLevel = 0;
            blackLevel = 0;
            pinkLevel = 0;
            fiveG = false;
            screm = false;

            for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
                ItemStack stack = getItem(i);
                if (stack.is(ModItems.UPGRADE_5G.get())) fiveG = true;
                if (stack.is(ModItems.UPGRADE_SCREM.get())) screm = true;
                if (!MachineUpgradeItem.isUpgrade(stack)) continue;
                redLevel += MachineUpgradeItem.getLevel(stack, UpgradeType.SPEED);
                greenLevel += MachineUpgradeItem.getLevel(stack, UpgradeType.EFFECT);
                blueLevel += MachineUpgradeItem.getLevel(stack, UpgradeType.POWER);
                pinkLevel += MachineUpgradeItem.getLevel(stack, UpgradeType.AFTERBURN);
                blackLevel += MachineUpgradeItem.getLevel(stack, UpgradeType.OVERDRIVE);
            }
        }

        checkDelay--;

        super.tickServer();
    }

    @Override
    public void tickClient() {
        if (tPos != null) lastDist = tPos.subtract(getTurretPos()).length();
        if (beam > 0) beam--;

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        long demand = getConsumption() * 10;

        if (target == null || getPower() < demand || !(level instanceof ServerLevel server)) return;

        if (fiveG && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(HbmPotion.death(), DEATH_TICKS, 0, true, true));
        } else {
            target.hurtServer(
                    server,
                    ModifiedDamageSource.bypassingCooldown(
                            level.damageSources().source(ModDamageTypes.MICROWAVE)),
                    (blackLevel * 10 + redLevel + 1F) * 0.25F);
        }

        if (pinkLevel > 0) target.igniteForSeconds(pinkLevel * 3);

        if (!target.isAlive() && target instanceof LivingEntity living) {
            ParticleCreators.giblets(server, living, GibletType.MEAT, 0);
            if (screm) {
                EntitySounds.play(
                        living,
                        living.getX(),
                        living.getY(),
                        living.getZ(),
                        ModSounds.BLOCK_SCREM.get(),
                        SoundSource.BLOCKS,
                        20F,
                        1F);
            } else {
                EntitySounds.play(
                        living,
                        living.getX(),
                        living.getY(),
                        living.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.HOSTILE,
                        2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
            }
        }

        hbm$syncSet$TurretBaseNTBlockEntity$power((long) (power - (demand)));

        hbm$syncSet$TurretMaxwellBlockEntity$shotSequence((int) (shotSequence + 1));
        networkPackNT(250);
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        switch (type) {
            case SPEED ->
                    tooltip.add(
                            Component.translatable("tooltip.hbm.upgrade.damage", level * 0.25)
                                    .withStyle(ChatFormatting.GREEN));
            case POWER -> UpgradeInfo.percent(tooltip, "upgrade.consumption", -level * 3, true);
            case EFFECT -> UpgradeInfo.range(tooltip, level * 3);
            case AFTERBURN ->
                    tooltip.add(
                            Component.translatable("tooltip.hbm.upgrade.afterburn", level * 3)
                                    .withStyle(ChatFormatting.GREEN));
            default -> throw new IllegalStateException("Maxwell tooltip: " + type);
        }
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    @Override
    public void afterSyncUnits(long units) {
        if (shotSequence != clientShotSequence) {
            beam = 5;
            clientShotSequence = shotSequence;
        }
    }

    @Override
    public void afterInitialSyncUnits() {
        clientShotSequence = shotSequence;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 11;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> output.writeInt(shotSequence);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> hbm$syncSet$TurretMaxwellBlockEntity$shotSequence(input.readInt());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$TurretMaxwellBlockEntity$shotSequence(int next) {
        if (this.shotSequence != next) {
            this.shotSequence = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }
}
