// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.CasingEjector;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretSentry extends BlockEntityTurretBaseNT {

    private static final CasingEjector EJECTOR =
            new CasingEjector().setMotion(0.2, 0.2, 0).setAngleRange(0.01F, 0.01F);

    private static @Nullable List<BulletConfig> configs;

    public double barrelLeftPos;
    public double lastBarrelLeftPos;
    public double barrelRightPos;
    public double lastBarrelRightPos;

    @SyncField(units = 1L << 11)
    protected boolean didJustShootLeft;

    protected boolean retractingLeft;

    @SyncField(units = 1L << 12)
    protected boolean didJustShootRight;

    protected boolean retractingRight;

    protected boolean shotSide;
    protected int timer;

    public BlockEntityTurretSentry(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_SENTRY.get(), pos, state);
    }

    protected BlockEntityTurretSentry(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean hasConnectorPlugs() {
        return false;
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs =
                    List.of(
                            XFactory9mm.p9_sp,
                            XFactory9mm.p9_fmj,
                            XFactory9mm.p9_jhp,
                            XFactory9mm.p9_ap);
        }
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretSentry");
    }

    @Override
    public double getTurretDepression() {
        return 20D;
    }

    @Override
    public double getTurretElevation() {
        return 20D;
    }

    @Override
    public double getDetectorRange() {
        return 24D;
    }

    @Override
    public double getDetectorGrace() {
        return 2D;
    }

    @Override
    public long getMaxPower() {
        return 1_000;
    }

    @Override
    public long getConsumption() {
        return 5;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public Vec3 getHorizontalOffset() {
        return new Vec3(0.5, 0, 0.5);
    }

    @Override
    public void tickClient() {
        lastBarrelLeftPos = barrelLeftPos;
        lastBarrelRightPos = barrelRightPos;

        float retractSpeed = 0.5F;
        float pushSpeed = 0.25F;

        if (retractingLeft) {
            barrelLeftPos += retractSpeed;
            if (barrelLeftPos >= 1) retractingLeft = false;
        } else {
            barrelLeftPos -= pushSpeed;
            if (barrelLeftPos < 0) barrelLeftPos = 0;
        }

        if (retractingRight) {
            barrelRightPos += retractSpeed;
            if (barrelRightPos >= 1) retractingRight = false;
        } else {
            barrelRightPos -= pushSpeed;
            if (barrelRightPos < 0) barrelRightPos = 0;
        }

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 10 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                cachedCasingConfig = conf.casing;
                spawnBullet(conf, 5F);
                consumeAmmo(conf);
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.TURRET_SENTRY_FIRE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);

                Vec3 side = new Vec3(0.125 * (shotSide ? 1 : -1), 0, 0).yRot((float) -rotationYaw);
                muzzleFlash(barrelTip().add(side.x, 0, side.z), 1F, 1);

                if (shotSide) didJustShootLeft = true;
                else didJustShootRight = true;
                shotSide = !shotSide;
            }
        }
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(0, 0.25, -0.125));
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
    protected void seekNewTarget() {
        Entity lastTarget = target;
        super.seekNewTarget();

        if (lastTarget != target && target != null) {
            level.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    ModSounds.TURRET_SENTRY_LOCKON.get(),
                    SoundSource.BLOCKS,
                    2.0F,
                    1.5F);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        didJustShootLeft = false;
        didJustShootRight = false;
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & (1L << 11)) != 0) retractingLeft = didJustShootLeft;
        if ((units & (1L << 12)) != 0) retractingRight = didJustShootRight;
    }

    @Override
    public void afterInitialSyncUnits() {
        retractingLeft = false;
        retractingRight = false;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1800L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> output.writeBoolean(this.didJustShootLeft);
            case 12 -> output.writeBoolean(this.didJustShootRight);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.didJustShootLeft = input.readBoolean();
            case 12 -> this.didJustShootRight = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
