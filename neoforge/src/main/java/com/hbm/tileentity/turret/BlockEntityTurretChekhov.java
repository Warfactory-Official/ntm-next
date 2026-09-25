// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.CasingEjector;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretChekhov extends BlockEntityTurretBaseNT {

    protected static final CasingEjector EJECTOR =
            new CasingEjector().setMotion(-0.8, 0.8, 0).setAngleRange(0.1F, 0.1F);

    private static @Nullable List<BulletConfig> configs;

    public float spin;
    public float lastSpin;
    protected int timer;
    private float accel;
    private boolean manual;

    public BlockEntityTurretChekhov(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_CHEKHOV.get(), pos, state);
    }

    protected BlockEntityTurretChekhov(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs =
                    List.of(
                            XFactory50.bmg50_sp,
                            XFactory50.bmg50_fmj,
                            XFactory50.bmg50_jhp,
                            XFactory50.bmg50_ap,
                            XFactory50.bmg50_du);
        }
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretChekhov");
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getBarrelLength() {
        return 3.5D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    public int getDelay() {
        return 2;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer > 20 && timer % getDelay() == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                cachedCasingConfig = conf.casing;
                spawnBullet(conf, 10F);
                consumeAmmo(conf);
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.TURRET_50BMG.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);
                muzzleFlash(barrelTip(), 1.5F, 1);
            }
        }
    }

    @Override
    public void tickServer() {
        super.tickServer();

        if (tPos == null && !manual) {
            timer--;
            if (timer > 20) timer = 20;
            if (timer < 0) timer = 0;
        }
        manual = false;
    }

    @Override
    public void tickClient() {
        super.tickClient();

        if (tPos != null || manual) accel = Math.min(45F, accel += 2);
        else accel = Math.max(0F, accel -= 2);

        manual = false;

        lastSpin = spin;
        spin += accel;

        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    @Override
    public void manualSetup() {
        manual = true;
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-1.125, 0.125, 0.25));
    }

    @Override
    protected @Nullable CasingEjector getEjector() {
        return EJECTOR;
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }
}
