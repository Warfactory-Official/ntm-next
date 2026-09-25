// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.EntityDamageUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
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

public class BlockEntityTurretMaxwell extends BlockEntityTurretBaseNT
        implements IUpgradeInfoProvider {

    private static final int DEATH_TICKS = 30 * 60 * 20;

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

    @SyncField(units = 1L << 11)
    private int shotSequence;

    private int clientShotSequence;

    public BlockEntityTurretMaxwell(BlockPos pos, BlockState state) {
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
                if (!ItemMachineUpgrade.isUpgrade(stack)) continue;
                redLevel += ItemMachineUpgrade.getLevel(stack, UpgradeType.SPEED);
                greenLevel += ItemMachineUpgrade.getLevel(stack, UpgradeType.EFFECT);
                blueLevel += ItemMachineUpgrade.getLevel(stack, UpgradeType.POWER);
                pinkLevel += ItemMachineUpgrade.getLevel(stack, UpgradeType.AFTERBURN);
                blackLevel += ItemMachineUpgrade.getLevel(stack, UpgradeType.OVERDRIVE);
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
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    target,
                    level.damageSources().source(ModDamageTypes.MICROWAVE),
                    (blackLevel * 10 + redLevel + 1F) * 0.25F);
        }

        if (pinkLevel > 0) target.igniteForSeconds(pinkLevel * 3);

        if (!target.isAlive() && target instanceof LivingEntity living) {
            ParticleCreators.giblets(server, living, ParticleGiblet.TYPE_MEAT, 0);
            if (screm) {
                server.playSound(
                        null,
                        living.getX(),
                        living.getY(),
                        living.getZ(),
                        ModSounds.BLOCK_SCREM.get(),
                        SoundSource.BLOCKS,
                        20F,
                        1F);
            } else {
                server.playSound(
                        null,
                        living.getX(),
                        living.getY(),
                        living.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.HOSTILE,
                        2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
            }
        }

        power -= demand;

        shotSequence++;
        networkPackNT(250);
    }

    @Override
    public int[] getValidUpgrades() {
        return IUpgradeInfoProvider.upgradeCaps(
                UpgradeType.SPEED,
                27,
                UpgradeType.POWER,
                27,
                UpgradeType.EFFECT,
                27,
                UpgradeType.AFTERBURN,
                27,
                UpgradeType.OVERDRIVE,
                27);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
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
            case 11 -> output.writeInt(this.shotSequence);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.shotSequence = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
