// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuForceField;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockEntityForceField extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_RADIUS = 1;
    public static final int SLOT_HEALTH = 2;
    public static final int SLOT_COUNT = 3;

    public static final int BASE_HEALTH = 100;
    public static final int COLOR_OK = 0x00FF00;
    public static final int COLOR_HIT = 0xFF0000;

    private static final int SWEEP_MARGIN = 25;

    public static final int BLINK_TICKS = 5;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_BATTERY};

    private final IntSet outside = new IntOpenHashSet();
    private final IntSet inside = new IntOpenHashSet();

    @SyncField(units = 1L << 3)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    public int health = BASE_HEALTH;

    @SyncField(units = 1L << 2)
    public int maxHealth = BASE_HEALTH;

    @SyncField(units = 1L << 6)
    public int cooldown;

    public int blink;

    @SyncField(units = 1L << 0)
    public float radius = MachineData.FORCE_FIELD_BASE_RADIUS.get();

    @SyncField(units = 1L << 4)
    public boolean isOn;

    @SyncField(units = 1L << 5)
    public int color = 0x0000FF;

    @SyncField(units = 1L << 7)
    private int powerCons = MachineData.FORCE_FIELD_BASE_CONSUMPTION.get();

    public BlockEntityForceField(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORCEFIELD.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        int radiusStacked = 0;
        int healthStacked = 0;
        radius = MachineData.FORCE_FIELD_BASE_RADIUS.get();
        maxHealth = BASE_HEALTH;

        ItemStack radiusUpgrade = inventory.get(SLOT_RADIUS);
        if (radiusUpgrade.is(ModItems.UPGRADE_RADIUS.get())) {
            radiusStacked = radiusUpgrade.getCount();
            radius += radiusStacked * MachineData.FORCE_FIELD_RADIUS_UPGRADE.get();
        }

        ItemStack healthUpgrade = inventory.get(SLOT_HEALTH);
        if (healthUpgrade.is(ModItems.UPGRADE_HEALTH.get())) {
            healthStacked = healthUpgrade.getCount();
            maxHealth += healthStacked * MachineData.FORCE_FIELD_SHIELD_UPGRADE.get();
        }

        powerCons =
                MachineData.FORCE_FIELD_BASE_CONSUMPTION.get()
                        + radiusStacked * MachineData.FORCE_FIELD_RADIUS_CONSUMPTION.get()
                        + healthStacked * MachineData.FORCE_FIELD_SHIELD_CONSUMPTION.get();

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);

        if (blink > 0) {
            blink--;
            color = COLOR_HIT;
        } else {
            color = COLOR_OK;
        }

        if (runField()) power -= powerCons();

        if (power < powerCons()) power = 0;

        networkPackNT(500);
    }

    public int powerCons() {
        return powerCons;
    }

    @Override
    public void tickClient() {
        runField();
    }

    private boolean runField() {
        if (cooldown > 0) {
            cooldown--;
        } else {

            if (health < maxHealth) {
                health =
                        (int)
                                (health
                                        + (maxHealth / 100)
                                                * MachineData.FORCE_FIELD_HEALTH_REGEN_MODIFIER
                                                        .get());
            }
            if (health > maxHealth) health = maxHealth;
        }

        if (isOn && cooldown == 0 && health > 0 && power >= powerCons()) {
            doField(radius);
            return true;
        }

        outside.clear();
        inside.clear();
        return false;
    }

    private void doField(float rad) {
        IntSet outLegacy = new IntOpenHashSet(outside);
        IntSet inLegacy = new IntOpenHashSet(inside);
        outside.clear();
        inside.clear();

        double cx = worldPosition.getX() + 0.5D;
        double cy = worldPosition.getY() + 0.5D;
        double cz = worldPosition.getZ() + 0.5D;
        double sweep = rad + SWEEP_MARGIN;
        AABB box = new AABB(cx - sweep, cy - sweep, cz - sweep, cx + sweep, cy + sweep, cz + sweep);

        for (Entity entity : level.getEntities((Entity) null, box, e -> !(e instanceof Player))) {
            int id = entity.getId();
            double dist =
                    Math.sqrt(
                            Math.pow(cx - entity.getX(), 2)
                                    + Math.pow(cy - entity.getY(), 2)
                                    + Math.pow(cz - entity.getZ(), 2));
            boolean out = dist > rad;

            if (!outLegacy.contains(id) && !inLegacy.contains(id)) {
                (out ? outside : inside).add(id);
            } else if (outLegacy.contains(id) && !out) {
                bounce(entity, rad + 1, -1);

                if (!isMuffled()) {
                    level.playSound(
                            null,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ModSounds.GUN_SPARK_SHOOT.get(),
                            SoundSource.BLOCKS,
                            2.5F,
                            1.0F);
                }
                outside.add(id);
                if (!level.isClientSide()) damage(impact(entity));
            } else if (inLegacy.contains(id) && out) {
                bounce(entity, rad - 1, 1);
                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        ModSounds.GUN_SPARK_SHOOT.get(),
                        SoundSource.BLOCKS,
                        2.5F,
                        1.0F);
                inside.add(id);
                if (!level.isClientSide()) damage(impact(entity));
            } else {
                (out ? outside : inside).add(id);
            }
        }
    }

    private void bounce(Entity entity, float shell, double sign) {
        double cx = worldPosition.getX() + 0.5D;
        double cy = worldPosition.getY() + 0.5D;
        double cz = worldPosition.getZ() + 0.5D;

        Vec3 vec = new Vec3(cx - entity.getX(), cy - entity.getY(), cz - entity.getZ()).normalize();
        entity.snapTo(cx - vec.x * shell, cy - vec.y * shell, cz - vec.z * shell, 0F, 0F);

        double speed = entity.getDeltaMovement().length();
        Vec3 motion = vec.scale(sign * speed);
        entity.setDeltaMovement(motion);
        entity.setPos(entity.getX() - motion.x, entity.getY() - motion.y, entity.getZ() - motion.z);
    }

    private int impact(Entity entity) {
        double mass = entity.getBbHeight() * entity.getBbWidth() * entity.getBbWidth();
        return (int) (mass * travelled(entity) * 50);
    }

    private double travelled(Entity entity) {
        double declared = entity.getDeltaMovement().length();

        double covered =
                new Vec3(
                                entity.getX() - entity.xo,
                                entity.getY() - entity.yo,
                                entity.getZ() - entity.zo)
                        .length();

        if (declared == 0) return covered;
        if (covered == 0) return declared;
        return Math.min(declared, covered);
    }

    private void damage(int ouch) {
        health -= ouch;

        if (ouch >= (maxHealth / 250)) blink = BLINK_TICKS;

        if (health <= 0) {
            health = 0;
            cooldown =
                    (int)
                            (100
                                    + radius
                                            * MachineData.FORCE_FIELD_COOLDOWN_MODIFIER
                                                    .get()
                                                    .floatValue());
        }
    }

    public int getHealthScaled(int i) {
        return (health * i) / maxHealth;
    }

    public long getPowerScaled(long i) {
        return (power * i) / getMaxPower();
    }

    public void setOn(boolean on) {
        this.isOn = on;
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) setOn(!isOn);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return MachineData.FORCE_FIELD_MAX_POWER.get();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);

        return slot == SLOT_RADIUS;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.forceField");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuForceField(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        health = input.getIntOr("health", BASE_HEALTH);
        cooldown = input.getIntOr("cooldown", 0);
        blink = input.getIntOr("blink", 0);
        isOn = input.getBooleanOr("isOn", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("health", health);
        output.putInt("cooldown", cooldown);
        output.putInt("blink", blink);
        output.putBoolean("isOn", isOn);
    }

    @Override
    public long syncUnitMask() {
        return 0xffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeFloat(this.radius);
            case 1 -> output.writeInt(this.health);
            case 2 -> output.writeInt(this.maxHealth);
            case 3 -> output.writeLong(this.power);
            case 4 -> output.writeBoolean(this.isOn);
            case 5 -> output.writeInt(this.color);
            case 6 -> output.writeInt(this.cooldown);
            case 7 -> output.writeInt(this.powerCons);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.radius = input.readFloat();
            case 1 -> this.health = input.readInt();
            case 2 -> this.maxHealth = input.readInt();
            case 3 -> this.power = input.readLong();
            case 4 -> this.isOn = input.readBoolean();
            case 5 -> this.color = input.readInt();
            case 6 -> this.cooldown = input.readInt();
            case 7 -> this.powerCons = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
