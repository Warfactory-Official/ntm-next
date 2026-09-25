// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.items.machine.ItemStamp;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {BlockEntityConveyorPress.SLOT_STAMP},
        units = 1L << 2)
public class BlockEntityConveyorPress extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, Audible, SyncUnitSchema {

    public static final int SLOT_STAMP = 0;
    public static final int USAGE = 100;
    public static final long MAX_POWER = 50_000;
    public static final double SPEED = 0.125;

    public static final int DELAY = 5;

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public double press;

    protected boolean isRetracting;
    private int delay;

    public double renderPress;
    public double lastPress;
    private double syncPress;
    private int turnProgress;
    public ItemStack syncStack = ItemStack.EMPTY;

    public BlockEntityConveyorPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVEYOR_PRESS.get(), pos, state, 1);
    }

    private AABB headBox() {
        return new AABB(
                worldPosition.getX(),
                worldPosition.getY() + 1,
                worldPosition.getZ(),
                worldPosition.getX() + 1,
                worldPosition.getY() + 1.5,
                worldPosition.getZ() + 1);
    }

    private List<EntityMovingItem> riders(ServerLevel level) {
        return level.getEntitiesOfClass(EntityMovingItem.class, headBox());
    }

    @Override
    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;

        if (delay > 0) {
            delay--;
        } else if (isRetracting) {
            if (canRetract()) {
                press -= SPEED;
                power -= USAGE;
                if (press <= 0) {
                    press = 0;
                    isRetracting = false;
                    delay = 0;
                }
            }
        } else if (canExtend(server)) {
            press += SPEED;
            power -= USAGE;
            if (press >= 1) {
                press = 1;
                isRetracting = true;
                delay = DELAY;
                process(server);
            }
        }

        networkPackNT(50);
    }

    @Override
    public void tickClient() {

        lastPress = renderPress;

        if (turnProgress > 0) {
            renderPress += (syncPress - renderPress) / turnProgress;
            turnProgress--;
        } else {
            renderPress = syncPress;
        }
    }

    public boolean canExtend(ServerLevel level) {
        if (power < USAGE) return false;
        if (getItem(SLOT_STAMP).isEmpty()) return false;

        for (EntityMovingItem item : riders(level)) {
            ItemStack stack = item.getItemStack();
            if (pressed(stack).isEmpty() || stack.getCount() != 1) continue;

            double lower = worldPosition.getX() + 0.35;
            double upper = worldPosition.getX() + 0.65;
            double lowerZ = worldPosition.getZ() + 0.35;
            double upperZ = worldPosition.getZ() + 0.65;

            if (item.getX() > lower
                    && item.getX() < upper
                    && item.getZ() > lowerZ
                    && item.getZ() < upperZ) {
                item.snapTo(worldPosition.getX() + 0.5, item.getY(), worldPosition.getZ() + 0.5);
            }
            return true;
        }

        return false;
    }

    public void process(ServerLevel level) {
        for (EntityMovingItem item : riders(level)) {
            ItemStack stack = item.getItemStack();
            ItemStack output = pressed(stack);

            if (output.isEmpty() || stack.getCount() != 1) continue;

            item.discard();
            EntityMovingItem out = new EntityMovingItem(level);
            out.snapTo(item.getX(), item.getY(), item.getZ());
            out.setItemStack(output.copy());
            level.addFreshEntity(out);
        }

        level.playSound(
                null,
                worldPosition,
                ModSounds.PRESS_OPERATE.get(),
                SoundSource.BLOCKS,
                getVolume(1.5F),
                1.0F);

        ItemStack stamp = getItem(SLOT_STAMP);
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage())
                setItem(SLOT_STAMP, ItemStack.EMPTY);
        }
    }

    private ItemStack pressed(ItemStack input) {
        StampType type = ItemStamp.typeOf(getItem(SLOT_STAMP));
        return type == null ? ItemStack.EMPTY : PressRecipes.INSTANCE.getOutput(input, type);
    }

    public boolean canRetract() {
        return power >= USAGE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemStamp;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {SLOT_STAMP};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        press = input.getDoubleOr("press", 0D);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putDouble("press", press);
    }

    private void readPress(ByteBuf input) {
        syncPress = input.readDouble();
    }

    private void writeStamp(ByteBuf output) {
        ItemStack stamp = getItem(SLOT_STAMP);
        output.writeInt(stamp.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(stamp.getItem()));
    }

    private void readStamp(ByteBuf input) {
        int stampId = input.readInt();
        syncStack =
                stampId < 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.byId(stampId));
    }

    @Override
    public void afterSyncUnits(long units) {
        turnProgress = 2;
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeDouble(this.press);
            case 2 -> writeStamp(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readPress(input);
            case 2 -> readStamp(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
