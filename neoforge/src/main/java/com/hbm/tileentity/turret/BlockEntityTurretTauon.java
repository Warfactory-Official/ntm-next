// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretTauon extends BlockEntityTurretBaseNT {

    private static @Nullable List<BulletConfig> configs;

    public int beam;
    public float spin;
    public float lastSpin;
    public double lastDist;
    private int timer;

    @SyncField(units = 1L << 11)
    private int shotSequence;

    private int clientShotSequence;

    public BlockEntityTurretTauon(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_TAUON.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryAccelerator.tau_uranium);
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretTauon");
    }

    @Override
    public double getDetectorGrace() {
        return 3D;
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
        return 35D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDetectorRange() {
        return 128D;
    }

    @Override
    public double getBarrelLength() {
        return 2.0D - 0.0625D;
    }

    @Override
    public long getMaxPower() {
        return 100000;
    }

    @Override
    public long getConsumption() {
        return 1000;
    }

    @Override
    public void tickClient() {
        if (tPos != null) lastDist = tPos.subtract(getTurretPos()).length();

        if (beam > 0) beam--;

        lastSpin = spin;
        if (tPos != null) spin += 45;

        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 5 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && target != null && level instanceof ServerLevel server) {
                target.hurtServer(
                        server,
                        level.damageSources().source(ModDamageTypes.ELECTRICITY),
                        30F + level.getRandom().nextInt(11));
                consumeAmmo(conf);
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.WEAPON_TAU_SHOOT.get(),
                        SoundSource.BLOCKS,
                        4.0F,
                        0.9F + level.getRandom().nextFloat() * 0.3F);

                shotSequence++;
                networkPackNT(250);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }

    @Override
    public void afterSyncUnits(long units) {
        if (shotSequence != clientShotSequence) {
            beam = 3;
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
