// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretJeremy extends BlockEntityTurretBaseNT {

    private static @Nullable List<BulletConfig> configs;

    private int timer;
    private int reload;

    public BlockEntityTurretJeremy(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_JEREMY.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs =
                    List.of(
                            XFactoryTurret.shell_normal,
                            XFactoryTurret.shell_explosive,
                            XFactoryTurret.shell_ap,
                            XFactoryTurret.shell_du,
                            XFactoryTurret.shell_w9);
        }
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretJeremy");
    }

    @Override
    public double getDetectorGrace() {
        return 16D;
    }

    @Override
    public double getTurretDepression() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getBarrelLength() {
        return 4.25D;
    }

    @Override
    public double getDetectorRange() {
        return 80D;
    }

    @Override
    public void tickServer() {
        if (reload > 0) reload--;
        if (reload == 1)
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.TURRET_JEREMY_RELOAD.get(),
                    SoundSource.BLOCKS,
                    2.0F,
                    1.0F);

        super.tickServer();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 40 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                cachedCasingConfig = conf.casing;
                spawnBullet(conf, 50F);
                consumeAmmo(conf);
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.TURRET_JEREMY_FIRE.get(),
                        SoundSource.BLOCKS,
                        4.0F,
                        1.0F);
                reload = 20;
                muzzleFlash(barrelTip(), 0F, 5);
            }
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
                -0.2,
                -0.2,
                0,
                0.01,
                -5,
                0,
                cachedCasingConfig.getName(),
                true,
                100,
                0.5,
                20);

        cachedCasingConfig = null;
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-2, 0, 0));
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    public int casingDelay() {
        return 22;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }
}
