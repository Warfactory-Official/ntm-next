// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineBatterySocket;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.EntityDischargeBeam;
import com.hbm.entity.ModEntities;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuBatterySocket;
import com.hbm.items.machine.EnumBatterySC;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@SyncSlots(
        value = {BlockEntityBatterySocket.SLOT_BATTERY},
        units = 0xeL)
public class BlockEntityBatterySocket extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                MenuProvider,
                IControlReceiver,
                ICopiable,
                SyncUnitSchema,
                IRORValueProvider,
                IRORInteractive {

    public static final int DELTA_WINDOW = 20;

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COUNT = 1;

    public static final int MODE_INPUT = 0;
    public static final int MODE_BUFFER = 1;
    public static final int MODE_OUTPUT = 2;
    public static final int MODE_NONE = 3;

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "maxfill",
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

    private static final int[] BATTERY_SLOTS = {SLOT_BATTERY};
    private final long[] log = new long[DELTA_WINDOW];
    private boolean poweredCache;

    @SyncField(units = 1L << 4)
    @ContainerSync
    public int redLow = MODE_INPUT;

    @SyncField(units = 1L << 5)
    @ContainerSync
    public int redHigh = MODE_OUTPUT;

    @SyncField(units = 1L << 6)
    @ContainerSync
    public ConnectionPriority priority = ConnectionPriority.LOW;

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long delta;

    public ItemStack syncStack = ItemStack.EMPTY;
    public long syncPower;
    public long syncMaxPower;

    public boolean frame;

    public int damageTimer;
    public int damageTarget;

    @SyncField(units = 1L << 2)
    public double scPowerMult = 1D;

    private long prevPowerSocket;
    private int lastComparator;
    private Item syncedBatteryItem = Items.AIR;

    public BlockEntityBatterySocket(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_SOCKET.get(), pos, state, SLOT_COUNT);
    }

    public static boolean isSocketRenderableBattery(ItemStack stack) {
        return IBatteryItem.isBattery(stack);
    }

    private ItemStack battery() {
        return inventory.get(SLOT_BATTERY);
    }

    public int getRelevantMode() {
        return poweredCache ? redHigh : redLow;
    }

    public void refreshMode() {
        Level level = getLevel();
        if (level == null) return;
        poweredCache = false;
        for (BlockPos port :
                MachineBatterySocket.ports(
                        worldPosition, BlockMultiblockCore.coreFacing(getBlockState()))) {
            if (level.hasNeighborSignal(port)) {
                poweredCache = true;
                return;
            }
        }
    }

    @Override
    public long getPower() {
        if (level != null && level.isClientSide()) return syncPower;
        return serverPower();
    }

    private long serverPower() {
        ItemStack stack = battery();
        if (stack.getItem() instanceof ItemBatterySC sc)
            return (long) (sc.getCharge(stack) * scPowerMult);
        if (stack.getItem() instanceof IBatteryItem battery) return battery.getCharge(stack);
        return 0L;
    }

    @Override
    public void setPower(long power) {
        ItemStack stack = battery();
        if (stack.getItem() instanceof IBatteryItem battery) {
            battery.setCharge(stack, power);
            setChanged();
        }
    }

    @Override
    public long getMaxPower() {
        if (level != null && level.isClientSide()) return syncMaxPower;
        return serverMaxPower();
    }

    private long serverMaxPower() {
        ItemStack stack = battery();
        return stack.getItem() instanceof IBatteryItem battery ? battery.getMaxCharge(stack) : 0L;
    }

    @Override
    public int getComparatorPower() {
        long max = getMaxPower();
        if (max <= 0L) return 0;
        return Math.min(15, Math.max(0, (int) Math.round((double) getPower() / max * 15.0)));
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode();
        if (mode != MODE_OUTPUT && mode != MODE_BUFFER) return 0L;
        ItemStack stack = battery();
        return stack.getItem() instanceof IBatteryItem battery
                ? battery.getDischargeRate(stack)
                : 0L;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode();
        if (mode != MODE_INPUT && mode != MODE_BUFFER) return 0L;
        ItemStack stack = battery();
        return stack.getItem() instanceof IBatteryItem battery ? battery.getChargeRate(stack) : 0L;
    }

    @Override
    public ConnectionPriority getPriority() {
        return priority;
    }

    @Override
    public boolean bufferedEndpoint() {
        return getRelevantMode() == MODE_BUFFER;
    }

    @Override
    public void tickClient() {
        if (TickPhase.every(this, 20)) {
            frame = !level.getBlockState(getBlockPos().above(2)).isAir();
        }
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        if (graph.getNode(worldPosition.asLong()) == null) {
            MachineBatterySocket.mintNodes(server, worldPosition, getBlockState());
        }
        graph.setSelfEndpoint(worldPosition.asLong(), true);
    }

    @Override
    public void tickServer() {
        if (hasSCLoaded()) {
            if (damageTarget == 0) pickNewSCTarget();
            damageTimer++;
            if (damageTimer >= damageTarget) discharge();
            fluctuate();
        }

        long curr = getPower();
        long avg = (curr + prevPowerSocket) / 2;
        delta = avg - log[0];
        System.arraycopy(log, 1, log, 0, log.length - 1);
        log[log.length - 1] = avg;
        prevPowerSocket = curr;
        syncStack = battery();

        int comparator = getComparatorPower();
        if (comparator != lastComparator && level instanceof ServerLevel server) {
            ((MachineBatterySocket) getBlockState().getBlock())
                    .updateComparatorOutput(server, worldPosition);
        }
        lastComparator = comparator;

        networkPackNT(100);
    }

    private boolean hasSCLoaded() {
        return battery().getItem() instanceof ItemBatterySC sc && sc.tier != EnumBatterySC.EMPTY;
    }

    private void pickNewSCTarget() {
        damageTimer = 0;
        damageTarget = 1200 + level.getRandom().nextInt(2400);
        setChanged();
    }

    private void fluctuate() {
        scPowerMult += (1D / 100D) * (level.getRandom().nextDouble() * 2D - 1D);
        scPowerMult = Mth.clamp(scPowerMult, 0.1D, 1D);
    }

    private void discharge() {
        pickNewSCTarget();

        BlockPos pos = getBlockPos();
        Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
        Direction rot = dir.getClockWise();
        double ex = pos.getX() + 0.5 - dir.getStepX() * 0.5 + rot.getStepX() * 0.5;
        double ey = pos.getY() + 1;
        double ez = pos.getZ() + 0.5 - dir.getStepZ() * 0.5 + rot.getStepX() * 0.5;

        double range = 15D;
        List<LivingEntity> targets =
                level.getEntitiesOfClass(
                        LivingEntity.class, new AABB(ex, ey, ez, ex, ey, ez).inflate(range));
        Collections.shuffle(targets);

        for (LivingEntity target : targets) {
            Vec3 delta =
                    new Vec3(
                            target.getX() - ex,
                            target.getY() + target.getBbHeight() / 2D - ey,
                            target.getZ() - ez);
            if (delta.length() > range) continue;
            Vec3 n = delta.normalize();
            double dominant = Math.max(Math.abs(n.x), Math.max(Math.abs(n.y), Math.abs(n.z)));
            Vec3 spawn = n.scale(1.125D / dominant);

            EntityDischargeBeam beam =
                    new EntityDischargeBeam(ModEntities.BEAM_DISCHARGE.get(), level);
            beam.setPos(pos.getX() + spawn.x, pos.getY() + spawn.y, pos.getZ() + spawn.z);
            Vec3 aim =
                    new Vec3(
                            target.getX() - beam.getX(),
                            target.getY() + target.getBbHeight() / 2D - beam.getY(),
                            target.getZ() - beam.getZ());
            beam.setRotationsFromVector(aim.x, aim.y, aim.z);
            beam.performHitscan(aim.length());
            level.addFreshEntity(beam);
        }

        EntityDischargeBeam.explodeDischarge(
                level,
                ex + level.getRandom().nextGaussian() * 0.5,
                ey + level.getRandom().nextGaussian() * 0.5,
                ez + level.getRandom().nextGaussian() * 0.5);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
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
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + getPower();
        if ((PREFIX_VALUE + "maxfill").equals(name)) return "" + getMaxPower();

        if ((PREFIX_VALUE + "fillpercent").equals(name))
            return ""
                    + Distribution.weightedShare(100, getPower(), Math.max(getMaxPower(), 1), 100);
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

    public boolean hasRenderBattery() {
        return isSocketRenderableBattery(
                level != null && level.isClientSide() ? syncStack : battery());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack stack = battery();
        Item item = stack.getItem();
        boolean changed = item != syncedBatteryItem;
        syncedBatteryItem = item;
        if (changed && level != null && !level.isClientSide()) syncToTracking();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && IBatteryItem.isBattery(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return BATTERY_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_BATTERY && IBatteryItem.isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot != SLOT_BATTERY || !(stack.getItem() instanceof IBatteryItem battery))
            return false;
        return switch (getRelevantMode()) {
            case MODE_OUTPUT -> battery.getCharge(stack) == 0;
            case MODE_INPUT -> battery.getCharge(stack) == battery.getMaxCharge(stack);
            default -> false;
        };
    }

    @Override
    public boolean allowDirectProvision() {
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
        return Component.translatable("container.batterySocket");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuBatterySocket(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        poweredCache = input.getBooleanOr("redstone", false);
        redLow = input.getIntOr("redLow", MODE_INPUT);
        redHigh = input.getIntOr("redHigh", MODE_OUTPUT);
        priority =
                ConnectionPriority.storage(
                        input.getIntOr("priority", ConnectionPriority.LOW.ordinal()));
        damageTimer = input.getIntOr("damageTimer", 0);
        damageTarget = input.getIntOr("damageTarget", 0);
        scPowerMult = input.getDoubleOr("scPowerMult", 1D);
        ItemStack stack = battery();
        syncedBatteryItem = stack.getItem();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("redLow", redLow);
        output.putInt("redHigh", redHigh);
        output.putBoolean("redstone", poweredCache);
        output.putInt("priority", priority.ordinal());
        output.putInt("damageTimer", damageTimer);
        output.putInt("damageTarget", damageTarget);
        output.putDouble("scPowerMult", scPowerMult);
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.delta);
            case 1 -> {
                ItemStack stack = battery();
                output.writeInt(
                        stack.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(stack.getItem()));
            }
            case 2 -> output.writeLong(serverPower());
            case 3 -> output.writeLong(serverMaxPower());
            case 4 -> output.writeShort(redLow);
            case 5 -> output.writeShort(redHigh);
            case 6 -> output.writeByte(priority.ordinal());
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.delta = input.readLong();
            case 1 -> {
                int id = input.readInt();
                if (id == -1) {
                    syncStack = ItemStack.EMPTY;
                } else {
                    Item item = BuiltInRegistries.ITEM.byId(id);
                    if (id < 0 || item == Items.AIR) throw new DecoderException();
                    syncStack = new ItemStack(item);
                }
            }
            case 2 -> syncPower = input.readLong();
            case 3 -> syncMaxPower = input.readLong();
            case 4 -> redLow = input.readShort();
            case 5 -> redHigh = input.readShort();
            case 6 -> priority = ConnectionPriority.storage(input.readByte());
            default -> throw new IllegalArgumentException();
        }
    }
}
