// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponent;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuCoreStabilizer;
import com.hbm.items.ModItems;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityCoreStabilizer extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IGUIProvider,
                IControlReceiver,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "durability",
                PREFIX_VALUE + "durabilitypercent",
                PREFIX_FUNCTION + "setpower" + NAME_SEPARATOR + "percent",
            };
    public static final int SLOT_LENS = 0;
    public static final int SLOT_COUNT = 1;

    public static final long MAX_POWER = 2_500_000_000L;
    public static final int RANGE = 15;

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public int watts;

    @SyncField(units = 1L << 2)
    public int beam;

    public BlockEntityCoreStabilizer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_STABILIZER.get(), pos, state, SLOT_COUNT);
    }

    public int getDemand() {
        return (int) Math.pow(watts, 4);
    }

    @Override
    public void tickServer() {
        watts = Math.clamp(watts, 1, 100);
        int demand = getDemand();

        beam = 0;

        ItemStack lens = inventory.get(SLOT_LENS);
        if (power >= demand && isUsableLens(lens)) fire(demand, lens);

        networkPackNT(250);
    }

    private void fire(int demand, ItemStack lens) {
        Direction dir = getBlockState().getValue(CoreComponent.FACING);

        for (int i = 1; i <= RANGE; i++) {
            BlockPos pos = worldPosition.relative(dir, i);
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof BlockEntityCore core) {
                core.field = Math.max(core.field, watts);
                this.power -= demand;
                beam = i;

                lens.setDamageValue(lens.getDamageValue() + watts);
                if (lens.getDamageValue() >= lens.getMaxDamage())
                    inventory.set(SLOT_LENS, ItemStack.EMPTY);
                setChanged();
                return;
            }

            if (!level.getBlockState(pos).isAir()) return;
        }
    }

    public static boolean isUsableLens(ItemStack stack) {
        return stack.is(ModItems.AMS_LENS.get()) && stack.getDamageValue() < stack.getMaxDamage();
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

    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_LENS && stack.is(ModItems.AMS_LENS.get());
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("watts")) watts = Math.clamp(data.getIntOr("watts", watts), 1, 100);
        markChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        ItemStack lens = inventory.get(SLOT_LENS);
        if ((PREFIX_VALUE + "durability").equals(name))
            return lens.is(ModItems.AMS_LENS.get()) ? "" + lensLeft(lens) : "0";
        if ((PREFIX_VALUE + "durabilitypercent").equals(name))
            return lens.is(ModItems.AMS_LENS.get())
                    ? "" + (lensLeft(lens) * 100 / lens.getMaxDamage())
                    : "0";
        return null;
    }

    private static long lensLeft(ItemStack lens) {
        return lens.getMaxDamage() - (long) lens.getDamageValue();
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpower").equals(name) && params.length > 0) {
            watts = IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
        }
        return null;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        watts = input.getIntOr("watts", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("watts", watts);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCoreStabilizer(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcStabilizer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.watts);
            case 2 -> output.writeInt(this.beam);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.watts = input.readInt();
            case 2 -> this.beam = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
