// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretRichard extends BlockEntityTurretBaseNT {

    private static @Nullable List<BulletConfig> configs;

    @SyncField(units = 1L << 11)
    public int loaded;

    private int timer;
    private int reload;

    public BlockEntityTurretRichard(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_RICHARD.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryRocket.rocket_ml);
        return configs;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretRichard");
    }

    @Override
    public double getTurretDepression() {
        return 25D;
    }

    @Override
    public double getTurretElevation() {
        return 25D;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getDetectorGrace() {
        return 8D;
    }

    @Override
    public double getDetectorRange() {
        return 64D;
    }

    @Override
    public void tickServer() {
        super.tickServer();

        if (reload > 0) {
            reload--;
            if (reload == 0) loaded = 17;
        }

        if (loaded <= 0 && reload <= 0 && getFirstConfigLoaded() != null) reload = 100;
        if (getFirstConfigLoaded() == null) loaded = 0;

        networkPackNT(250);
    }

    @Override
    public void updateFiringTick() {
        if (reload > 0) return;

        timer++;

        if (timer > 0 && timer % 10 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                spawnBullet(conf, 30F);
                consumeAmmo(conf);
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.RICHARD_FIRE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);
                loaded--;
            } else {
                loaded = 0;
            }
        }
    }

    @Override
    public void spawnBullet(BulletConfig bullet, float baseDamage) {
        Vec3 tip = barrelTip();

        EntityBulletBaseMK4 proj =
                new EntityBulletBaseMK4(
                        level,
                        bullet,
                        baseDamage,
                        bullet.spread,
                        (float) rotationYaw,
                        (float) rotationPitch);
        proj.snapTo(tip.x, tip.y, tip.z, proj.getYRot(), proj.getXRot());
        proj.lockonTarget = target;
        level.addFreshEntity(proj);
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

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 11;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> output.writeInt(this.loaded);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.loaded = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
