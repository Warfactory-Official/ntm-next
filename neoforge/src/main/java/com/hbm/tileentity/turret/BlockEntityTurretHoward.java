// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.CasingEjector;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.SpentCasing;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretHoward extends BlockEntityTurretBaseNT {

    public static final SpentCasing DGK_CASING =
            new SpentCasing(CasingType.STRAIGHT)
                    .setScale(1.5F)
                    .setBounceMotion(1F, .5F)
                    .setColor(SpentCasing.COLOR_CASE_BRASS)
                    .register("DGK")
                    .setupSmoke(.02F, .5D, 60, 20)
                    .setMaxAge(60);

    private static final CasingEjector EJECTOR =
            new CasingEjector().setMotion(0.4, 0, 0).setAngleRange(0.02F, 0.03F);

    protected static final int CIWS_HITRATE = 50;

    private static @Nullable List<BulletConfig> configs;

    public float spin;
    public float lastSpin;
    private int loaded;
    protected int timer;

    public BlockEntityTurretHoward(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_HOWARD.get(), pos, state);
    }

    protected BlockEntityTurretHoward(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryTurret.dgk_normal);
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretHoward");
    }

    @Override
    public double getHeightOffset() {
        return 2.25D;
    }

    @Override
    public double getDetectorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 12D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 8D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public double getTurretDepression() {
        return 50D;
    }

    @Override
    public double getDetectorRange() {
        return 250D;
    }

    @Override
    public double getBarrelLength() {
        return 3.25D;
    }

    @Override
    public long getMaxPower() {
        return 50000;
    }

    @Override
    public long getConsumption() {
        return 500;
    }

    @Override
    public void tickServer() {
        if (loaded <= 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                consumeAmmo(conf);
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.TURRET_CIWS_RELOAD.get(),
                        SoundSource.BLOCKS,
                        4.0F,
                        1F);
                loaded = 200;
            }
        }

        super.tickServer();
    }

    @Override
    public void tickClient() {
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

        if (loaded > 0 && tPos != null) {
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.TURRET_HOWARD_FIRE.get(),
                    SoundSource.BLOCKS,
                    4.0F,
                    0.9F + level.getRandom().nextFloat() * 0.3F);
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.TURRET_HOWARD_FIRE.get(),
                    SoundSource.BLOCKS,
                    4.0F,
                    1F + level.getRandom().nextFloat() * 0.3F);

            for (int i = 0; i < 2; i++) {
                cachedCasingConfig = DGK_CASING;
                spawnCasing();
            }

            if (timer % 2 == 0) {
                loaded--;

                if (level.getRandom().nextInt(100) + 1 <= CIWS_HITRATE && target != null) {
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(
                            target,
                            level.damageSources().source(ModDamageTypes.SHRAPNEL),
                            2F + level.getRandom().nextInt(2));
                }

                Vec3 tip = barrelTip();
                Vec3 hOff = alongBarrel(0, 0.25, 0);
                muzzleFlash(tip.add(hOff), 1.5F, 1);
                muzzleFlash(tip.subtract(hOff), 1.5F, 1);
            }
        }
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-0.875, 0.2, -0.125));
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loaded = input.getIntOr("loaded", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("loaded", loaded);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }
}
