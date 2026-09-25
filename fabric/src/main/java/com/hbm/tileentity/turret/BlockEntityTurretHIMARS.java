// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretHIMARS extends BlockEntityTurretBaseArtillery {

    public static final short MODE_AUTO = 0;
    public static final short MODE_MANUAL = 1;

    @SyncField(units = 1L << 11)
    public short mode = 0;

    @SyncField(units = 1L << 12)
    public int typeLoaded = -1;

    @SyncField(units = 1L << 13)
    public int ammo = 0;

    @SyncField(units = 1L << 14)
    public float crane;

    public float lastCrane;

    private int timer;

    public BlockEntityTurretHIMARS(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_HIMARS.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return List.of();
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemAmmoHIMARS.HIMARSRocketType type : ItemAmmoHIMARS.HIMARSRocketType.values()) {
            stacks.add(ModItems.AMMO_HIMARS.stack(type));
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretHIMARS");
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    @Override
    public double getBarrelLength() {
        return 0.5D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 5D;
    }

    @Override
    public double getHeightOffset() {
        return 5D;
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
    public boolean doLOSCheck() {
        return false;
    }

    @Override
    protected void alignTurret() {
        Vec3 pos = getTurretPos();
        Vec3 delta = new Vec3(tPos.x - pos.x, tPos.y - pos.y, tPos.z - pos.z);
        double targetYaw = -Math.atan2(delta.x, delta.z);
        double targetPitch = Math.PI / 4D;

        turnTowardsAngle(targetPitch, targetYaw);
    }

    public int getSpareRocket() {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            ItemAmmoHIMARS.HIMARSRocketType type = ModItems.AMMO_HIMARS.typeOf(stack);
            if (type != null) return type.ordinal();
        }

        return -1;
    }

    public void consumeRocket(int variant) {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (ModItems.AMMO_HIMARS.is(stack, ItemAmmoHIMARS.byIndex(variant))) {
                removeItem(i, 1);
                return;
            }
        }

        setChanged();
    }

    @Override
    public void tickClient() {
        lastCrane = crane;
        super.tickClient();
    }

    @Override
    public void tickServer() {
        if (mode == MODE_MANUAL) {
            if (!targetQueue.isEmpty()) tPos = targetQueue.get(0);
        } else {
            targetQueue.clear();
        }

        aligned = false;

        if (target != null && !target.isAlive()) {
            target = null;
            stattrak++;
        }

        if (target != null && mode != MODE_MANUAL && !entityInLOS(target)) target = null;

        if (target != null) tPos = getEntityPos(target);
        else if (mode != MODE_MANUAL) tPos = null;

        if (isOn() && hasPower()) {

            if (!hasAmmo() || crane > 0) {
                turnTowardsAngle(0, rotationYaw);

                if (aligned) {
                    if (hasAmmo()) {
                        crane -= 0.0125F;
                    } else {
                        crane += 0.0125F;

                        if (crane >= 1F) {
                            int available = getSpareRocket();

                            if (available != -1) {
                                typeLoaded = available;
                                ammo = ItemAmmoHIMARS.byIndex(available).amount;
                                consumeRocket(available);
                            }
                        }
                    }
                }

                crane = Mth.clamp(crane, 0F, 1F);

            } else {
                if (tPos != null) alignTurret();
            }

        } else {
            target = null;
            tPos = null;
        }

        if (!isOn()) targetQueue.clear();

        if (target != null && !target.isAlive()) {
            target = null;
            tPos = null;
            stattrak++;
        }

        if (isOn() && hasPower()) {
            searchTimer--;
            setPower(getPower() - getConsumption());

            if (searchTimer <= 0) {
                searchTimer = getDetectorInterval();
                if (target == null && mode != MODE_MANUAL) seekNewTarget();
            }
        } else {
            searchTimer = 0;
        }

        if (aligned && crane <= 0) updateFiringTick();

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);

        networkPackNT(250);
    }

    public boolean hasAmmo() {
        return typeLoaded >= 0 && ammo > 0;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        int delay = 40;

        if (timer % delay == 0) {
            if (hasAmmo() && tPos != null) {
                spawnShell(typeLoaded);
                ammo--;
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.WEAPON_ROCKET_FLAME.get(),
                        SoundSource.BLOCKS,
                        25.0F,
                        1.0F);
            }

            if (mode == MODE_MANUAL && !targetQueue.isEmpty()) {
                targetQueue.remove(0);
                tPos = null;
            }
        }
    }

    public void spawnShell(int type) {
        Vec3 vec = alongBarrel(getBarrelLength(), 0, 0);
        Vec3 pos = getTurretPos().add(vec);

        EntityArtilleryRocket proj = new EntityArtilleryRocket(ModEntities.HIMARS.get(), level);
        proj.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        proj.setThrowableHeading(vec.x, vec.y, vec.z, 25F, 0.0F);

        if (target != null) proj.setTarget(target);
        else proj.setTarget(tPos.x, tPos.y, tPos.z);

        proj.setType(type);

        level.addFreshEntity(proj);
    }

    @Override
    public void handleButtonPacket(int meta) {
        if (meta == 5) {
            mode++;
            if (mode > 1) mode = 0;

            abandonEngagement();
        } else {
            super.handleButtonPacket(meta);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = (short) input.getShortOr("mode", (short) 0);

        typeLoaded = input.getIntOr("type", -1);
        ammo = input.getIntOr("ammo", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putShort("mode", mode);
        output.putInt("type", typeLoaded);
        output.putInt("ammo", ammo);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }

    private void writeType(ByteBuf output) {
        output.writeShort(typeLoaded);
    }

    private void readType(ByteBuf input) {
        typeLoaded = input.readShort();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7800L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> output.writeShort(this.mode);
            case 12 -> writeType(output);
            case 13 -> output.writeInt(this.ammo);
            case 14 -> output.writeFloat(this.crane);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.mode = input.readShort();
            case 12 -> readType(input);
            case 13 -> this.ammo = input.readInt();
            case 14 -> this.crane = input.readFloat();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
