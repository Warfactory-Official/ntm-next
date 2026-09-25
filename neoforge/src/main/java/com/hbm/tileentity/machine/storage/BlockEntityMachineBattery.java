// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.*;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MachineBattery;
import com.hbm.blocks.network.CableConductorBlockBase;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineBattery;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.uninos.graph.LevelNodeGraph;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineBattery extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                MenuProvider,
                IControlReceiver,
                ICopiable,
                PersistentDrop,
                SyncUnitSchema,
                IRORValueProvider,
                IRORInteractive {

    public static final int DELTA_WINDOW = 20;

    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_DISCHARGE = 1;
    public static final int SLOT_COUNT = 2;

    public static final long MAX_POWER = 1_000_000L;

    public static final int MODE_INPUT = 0;
    public static final int MODE_BUFFER = 1;
    public static final int MODE_OUTPUT = 2;
    public static final int MODE_NONE = 3;

    private static final String[] PERSISTENT_KEYS = {"power", "redLow", "redHigh", "priority"};

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "fillpercent",
                PREFIX_VALUE + "delta",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION
                        + "setmode"
                        + NAME_SEPARATOR
                        + "mode"
                        + PARAM_SEPARATOR
                        + "fallback (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION
                        + "setredmode"
                        + NAME_SEPARATOR
                        + "mode"
                        + PARAM_SEPARATOR
                        + "fallback (0-3)",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
            };

    private static final int[] SLOTS_CHARGE = {SLOT_CHARGE};
    private static final int[] SLOTS_DISCHARGE = {SLOT_DISCHARGE};
    private static final int[] SLOTS_BOTH = {SLOT_CHARGE, SLOT_DISCHARGE};
    private final long[] log = new long[DELTA_WINDOW];

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @ContainerSync public int redLow = MODE_INPUT;
    @ContainerSync public int redHigh = MODE_OUTPUT;
    @ContainerSync public ConnectionPriority priority = ConnectionPriority.LOW;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public long delta;

    private @Nullable BlockLookupCache<IEnergyHandlerMK2>[] neighbourReceivers;
    private boolean poweredCache;
    private long prevPower;
    private long bufferedMax;

    protected BlockEntityMachineBattery(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    public BlockEntityMachineBattery(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state, SLOT_COUNT);
    }

    public int getRelevantMode() {
        return poweredCache ? redHigh : redLow;
    }

    public void refreshMode() {
        poweredCache = readsSignal();
    }

    protected boolean readsSignal() {
        Level level = getLevel();
        return level != null && level.hasNeighborSignal(getBlockPos());
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
        if (bufferedMax == 0L && getBlockState().getBlock() instanceof MachineBattery battery) {
            bufferedMax = battery.maxPower;
        }
        return bufferedMax;
    }

    public long getPowerRemainingScaled(long i) {
        return (power * i) / getMaxPower();
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode();
        return mode == MODE_OUTPUT || mode == MODE_BUFFER ? getMaxPower() / 600L : 0L;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode();
        return mode == MODE_INPUT || mode == MODE_BUFFER ? getMaxPower() / 200L : 0L;
    }

    @Override
    public boolean bufferedEndpoint() {
        return getRelevantMode() == MODE_BUFFER;
    }

    @Override
    public ConnectionPriority getPriority() {
        return priority;
    }

    @Override
    public int getComparatorPower() {
        long max = getMaxPower();
        if (power == 0L || max <= 0L) return 0;
        return Math.min(15, Math.max(0, (int) ((double) power / (double) max * 15D) + 1));
    }

    @Override
    public void tickServer() {
        long max = getMaxPower();
        power += ItemEnergyTransfer.extract(this, SLOT_CHARGE, max - power, false);
        power -= ItemEnergyTransfer.insert(this, SLOT_DISCHARGE, power, false);
        if (power > max) power = max;

        updateBufferNode((ServerLevel) level);

        if (getRelevantMode() == MODE_OUTPUT) {
            provideToNeighbours((ServerLevel) level);
        }

        long avg = averagePower(power, prevPower);
        delta = avg - log[0];
        System.arraycopy(log, 1, log, 0, log.length - 1);
        log[log.length - 1] = avg;
        prevPower = power;

        networkPackNT(50);
    }

    protected long averagePower(long current, long previous) {
        return (current + previous) / 2;
    }

    protected void updateBufferNode(ServerLevel server) {
        long key = worldPosition.asLong();
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        if (getRelevantMode() != MODE_BUFFER) {
            if (graph.getNode(key) != null) graph.removeNode(key);
            return;
        }
        if (graph.getNode(key) == null) {
            graph.addNode(
                    key,
                    PowerGraphProvider.INSTANCE.createData(getBlockState()),
                    CableConductorBlockBase.OPEN_ALL);
        }
        graph.setSelfEndpoint(key, true);
    }

    protected void provideToNeighbours(ServerLevel server) {
        if (neighbourReceivers == null) {
            neighbourReceivers = new BlockLookupCache[Direction.VALUES.length];
            for (int i = 0; i < Direction.VALUES.length; i++) {
                Direction dir = Direction.VALUES[i];
                neighbourReceivers[i] =
                        Services.CAPS.createCache(
                                EnergyCaps.RECEIVER,
                                server,
                                worldPosition.relative(dir),
                                dir.getOpposite());
            }
        }
        if (power <= 0) return;

        List<IEnergyHandlerMK2> flush = null;
        for (int i = 0; i < Direction.VALUES.length; i++) {
            IEnergyHandlerMK2 rec = neighbourReceivers[i].find();
            if (rec == null || rec == this) continue;
            Direction touching = Direction.VALUES[i].getOpposite();
            if (!rec.allowDirectProvision()) continue;
            if (flush == null) flush = new ArrayList<>(Direction.VALUES.length);
            flush.add(rec);
        }
        if (flush == null) return;
        PowerNetwork.provideDirect(this, flush);
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.getBooleanOr("redLow", false)) redLow = (redLow + 1) & 0x3;
        if (data.getBooleanOr("redHigh", false)) redHigh = (redHigh + 1) & 0x3;
        if (data.getBooleanOr("priority", false)) {
            priority = ConnectionPriority.nextStorage(priority);
        }
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + getPowerRemainingScaled(100);
        if ((PREFIX_VALUE + "delta").equals(name)) return "" + delta;
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int next = RorModes.select(redLow, params);
            if (next != redLow) {
                redLow = next;
                setChanged();
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            int next = RorModes.select(redHigh, params);
            if (next != redHigh) {
                redHigh = next;
                setChanged();
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            priority = ConnectionPriority.VALUES[IRORInteractive.parseInt(params[0], 0, 2) + 1];
            setChanged();
        }
        return null;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return IBatteryItem.isBattery(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case DOWN -> SLOTS_BOTH;
            case UP -> SLOTS_CHARGE;
            default -> SLOTS_DISCHARGE;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return IBatteryItem.isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (!(stack.getItem() instanceof IBatteryItem battery)) return false;
        if (slot == SLOT_CHARGE) return battery.getCharge(stack) == 0L;
        if (slot == SLOT_DISCHARGE) return battery.getCharge(stack) == battery.getMaxCharge(stack);
        return false;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putShort("redLow", (short) redLow);
        data.putShort("redHigh", (short) redHigh);
        data.putByte("priority", (byte) priority.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (nbt.contains("redLow")) redLow = nbt.getShortOr("redLow", (short) redLow);
        if (nbt.contains("redHigh")) redHigh = nbt.getShortOr("redHigh", (short) redHigh);
        if (nbt.contains("priority")) {
            priority =
                    ConnectionPriority.storage(
                            nbt.getByteOr("priority", (byte) priority.ordinal()));
        }
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.battery");
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(
                ModDataComponents.MACHINE_BATTERY_STATE.get(),
                new BatteryCharge(power, prevPower, redLow, redHigh, priority.ordinal()));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        BatteryCharge state = components.get(ModDataComponents.MACHINE_BATTERY_STATE.get());
        if (state == null) return;
        power = state.power();
        prevPower = state.prevPower();
        redLow = state.redLow();
        redHigh = state.redHigh();
        priority = ConnectionPriority.storage(state.priority());
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineBattery(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        poweredCache = input.getBooleanOr("redstone", false);
        power = input.getLongOr("power", 0L);
        redLow = input.getIntOr("redLow", MODE_INPUT);
        redHigh = input.getIntOr("redHigh", MODE_OUTPUT);
        priority =
                ConnectionPriority.storage(
                        input.getIntOr("priority", ConnectionPriority.LOW.ordinal()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("redLow", redLow);
        output.putBoolean("redstone", poweredCache);
        output.putInt("redHigh", redHigh);
        output.putInt("priority", priority.ordinal());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.delta);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.delta = input.readLong();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
