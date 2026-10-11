// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.*;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.PersistentDrop;
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
import com.hbm.platform.FabricCaps;
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
import net.minecraft.world.level.SignalGetter;
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

    @ContainerSync private int redLow = MODE_INPUT;
    @ContainerSync private int redHigh = MODE_OUTPUT;
    @ContainerSync public ConnectionPriority priority = ConnectionPriority.LOW;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public long delta;

    private @Nullable BlockLookupCache<IEnergyHandlerMK2>[] neighbourReceivers;
    private @Nullable ArrayList<IEnergyHandlerMK2> directFlush;
    private boolean poweredCache;
    private boolean graphReady;
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

    public int getRedLow() {
        return redLow;
    }

    public int getRedHigh() {
        return redHigh;
    }

    public void setModes(int low, int high) {
        if (updateModes(low, high)) setChanged();
    }

    public void setRedLow(int mode) {
        setModes(mode, redHigh);
    }

    public void setRedHigh(int mode) {
        setModes(redLow, mode);
    }

    private boolean updateModes(int low, int high) {
        if (redLow == low && redHigh == high) return false;
        int previous = getRelevantMode();
        redLow = low;
        redHigh = high;
        modeChanged(previous);
        return true;
    }

    public void refreshMode() {
        if (!graphReady || isRemoved()) return;
        int previous = getRelevantMode();
        poweredCache = readsSignal(level);
        modeChanged(previous);
    }

    protected boolean readsSignal(SignalGetter signals) {
        return signals.hasNeighborSignal(worldPosition);
    }

    protected boolean conducts(int mode) {
        return mode == MODE_BUFFER;
    }

    private void modeChanged(int previous) {
        if (!graphReady || isRemoved()) return;
        boolean before = conducts(previous);
        boolean after = conducts(getRelevantMode());
        if (before == after) return;
        LevelNodeGraph<CableData> graph = PowerGraph.get((ServerLevel) level);
        if (after) addConductorNodes(graph);
        else removeConductorNodes(graph);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        if (graphReady) return;
        poweredCache = readsSignal(ChunkPublication.signals(server, worldPosition));
        graphReady = true;
        if (conducts(getRelevantMode())) addConductorNodes(PowerGraph.get(server));
    }

    protected void addConductorNodes(LevelNodeGraph<CableData> graph) {
        graph.addMachineNode(
                worldPosition.asLong(), CableData.INSTANCE, CableConductorBlockBase.OPEN_ALL, true);
    }

    protected void removeConductorNodes(LevelNodeGraph<CableData> graph) {
        graph.removeNode(worldPosition.asLong());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (graphReady) onGraphDetach((ServerLevel) level);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        if (graphReady && conducts(getRelevantMode())) removeConductorNodes(PowerGraph.get(server));
        graphReady = false;
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

    protected void provideToNeighbours(ServerLevel server) {
        if (neighbourReceivers == null) {
            directFlush = new ArrayList<>(Direction.VALUES.length);
            neighbourReceivers = new BlockLookupCache[Direction.VALUES.length];
            for (int i = 0; i < Direction.VALUES.length; i++) {
                Direction dir = Direction.VALUES[i];
                neighbourReceivers[i] =
                        FabricCaps.createCache(
                                EnergyCaps.RECEIVER,
                                server,
                                worldPosition.relative(dir),
                                dir.getOpposite());
            }
        }
        if (power <= 0) return;
        List<IEnergyHandlerMK2> flush = directFlush;
        for (int i = 0; i < Direction.VALUES.length; i++) {
            IEnergyHandlerMK2 rec = neighbourReceivers[i].find();
            if (rec == null || rec == this) continue;
            if (!rec.allowDirectProvision()) continue;
            flush.add(rec);
        }
        if (flush.isEmpty()) return;
        try {
            PowerNetwork.provideDirect(this, flush);
        } finally {
            flush.clear();
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        boolean changed =
                updateModes(
                        data.getBooleanOr("redLow", false) ? (redLow + 1) & 3 : redLow,
                        data.getBooleanOr("redHigh", false) ? (redHigh + 1) & 3 : redHigh);
        if (data.getBooleanOr("priority", false)) {
            priority = ConnectionPriority.nextStorage(priority);
            changed = true;
        }
        if (changed) setChanged();
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
            setRedLow(RorModes.select(redLow, params));
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            setRedHigh(RorModes.select(redHigh, params));
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
        boolean changed =
                updateModes(
                        nbt.getShortOr("redLow", (short) redLow),
                        nbt.getShortOr("redHigh", (short) redHigh));
        ConnectionPriority next =
                ConnectionPriority.storage(nbt.getByteOr("priority", (byte) priority.ordinal()));
        changed |= next != priority;
        priority = next;
        if (changed) setChanged();
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
        updateModes(state.redLow(), state.redHigh());
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
        int previous = getRelevantMode();
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        redLow = input.getIntOr("redLow", MODE_INPUT);
        redHigh = input.getIntOr("redHigh", MODE_OUTPUT);
        priority =
                ConnectionPriority.storage(
                        input.getIntOr("priority", ConnectionPriority.LOW.ordinal()));
        modeChanged(previous);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("redLow", redLow);
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
