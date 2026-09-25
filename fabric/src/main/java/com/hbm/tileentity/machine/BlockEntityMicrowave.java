// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.container.MenuMachineMicrowave;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMicrowave extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_COUNT = 3;

    public static final long MAX_POWER = 50_000L;
    public static final int CONSUMPTION = 50;
    public static final int MAX_TIME = 300;
    public static final int MAX_SPEED = 5;

    private static final int[] SLOTS_DOWN = {SLOT_OUTPUT};
    private static final int[] SLOTS_SIDE = {SLOT_INPUT};
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public int time;

    @SyncField(units = 1L << 2)
    public int speed;

    public BlockEntityMicrowave(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICROWAVE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.microwave");
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        if (canProcess()) {

            if (speed >= MAX_SPEED) {
                level.destroyBlock(worldPosition, false);
                ExplosionVNT vnt =
                        new ExplosionVNT(
                                level,
                                worldPosition.getX() + 0.5,
                                worldPosition.getY() + 0.5,
                                worldPosition.getZ() + 0.5,
                                5);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                return;
            }

            if (time >= MAX_TIME) {
                process();
                time = 0;
            }

            if (canProcess()) {
                power -= CONSUMPTION;
                time += speed * 2;
            }
        }

        networkPackNT(50);
    }

    private void process() {
        ItemStack result = smeltResult();
        if (result == null || result.isEmpty()) return;
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) inventory.set(SLOT_OUTPUT, result.copy());
        else out.grow(result.getCount());
        inventory.get(SLOT_INPUT).shrink(1);
        setChanged();
    }

    private @Nullable ItemStack smeltResult() {
        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty() || !(level instanceof ServerLevel server)) return null;
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                quickCheck.getRecipeFor(new SingleRecipeInput(in), server);
        return recipe.map(r -> r.value().assemble(new SingleRecipeInput(in))).orElse(null);
    }

    private boolean canProcess() {
        if (speed == 0) return false;
        if (power < CONSUMPTION) return false;

        ItemStack in = inventory.get(SLOT_INPUT);
        ItemStack result = smeltResult();
        if (in.isEmpty() || result == null || result.isEmpty()) return false;

        if (!in.has(DataComponents.FOOD) && !result.has(DataComponents.FOOD)) return false;

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(result, out)) return false;
        return result.getCount() + out.getCount() <= result.getMaxStackSize();
    }

    private boolean hasSmeltingResult(ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return false;
        return quickCheck.getRecipeFor(new SingleRecipeInput(stack), server).isPresent();
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int button = data.getIntOr("button", -1);
        if (button == 0) speed++;
        if (button == 1) speed--;
        if (speed < 0) speed = 0;
        if (speed > MAX_SPEED) speed = MAX_SPEED;
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> hasSmeltingResult(stack);
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT && hasSmeltingResult(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineMicrowave(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("speed").ifPresent(v -> speed = v);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("speed", speed);
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.time);
            case 2 -> output.writeInt(this.speed);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.time = input.readInt();
            case 2 -> this.speed = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
