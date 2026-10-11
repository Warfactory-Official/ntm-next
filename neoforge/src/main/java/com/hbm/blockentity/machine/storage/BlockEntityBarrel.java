// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLane;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.IFluidCopiable;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.storage.BlockFluidBarrel;
import com.hbm.blocks.network.FluidDuctBlockBase;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.inventory.container.MenuBarrel;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.TomSaveData;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityBarrel extends BlockEntityMachineBase
        implements FluidTankAccess,
                FluidFlushSender,
                MenuProvider,
                IControlReceiver,
                PersistentDrop,
                IFluidCopiable,
                SyncUnitSchema,
                IRORValueProvider,
                IRORInteractive {
    private static final String[] PERSISTENT_KEYS = {"tank"};

    private static final FlushFaces FACES = FlushFaces.own();

    public static final int SLOT_COUNT = 6;

    public static final int MODE_RECEIVE = 0;
    public static final int MODE_BUFFER = 1;
    public static final int MODE_SEND = 2;
    public static final int MODE_NONE = 3;
    public static final int MODES = 4;
    private static final int[] ACCESSIBLE_SLOTS = {2, 3, 4, 5};
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "type",
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION
                        + "setmode"
                        + NAME_SEPARATOR
                        + "mode"
                        + PARAM_SEPARATOR
                        + "fallback (0-3)",
            };

    @SyncField(units = 1L << 1)
    public final FluidTankNTM tank;

    private final FluidTankNTM[] tanks;

    @SyncField(units = 1L << 0)
    private int mode = MODE_RECEIVE;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private boolean graphReady;

    public BlockEntityBarrel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BARREL.get(), pos, state, SLOT_COUNT);
        this.tank = createTank(((BlockFluidBarrel) state.getBlock()).capacity);
        this.tanks = new FluidTankNTM[] {tank};
    }

    protected BlockEntityBarrel(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, SLOT_COUNT);
        this.tank = createTank(capacity);
        this.tanks = new FluidTankNTM[] {tank};
    }

    private FluidTankNTM createTank(int capacity) {
        return new FluidTankNTM(capacity) {
            @Override
            protected void declaredFluidChanged(@Nullable Fluid previous) {
                if (!graphReady || isRemoved()) return;
                ServerLevel server = (ServerLevel) level;
                changeBufferNodes(server, mode, previous);
                refreshConnectors(server, true, Block.UPDATE_ALL);
            }
        };
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        if (graphReady) return;
        graphReady = true;
        Fluid type = tank.getDeclaredFluid();
        if (mode == MODE_BUFFER && type != null) {
            addBufferNodes(FluidPipeGraph.get(server, type), new PipeData(type));
        }
        refreshConnectors(server, false, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private void changeBufferNodes(
            ServerLevel server, int previousMode, @Nullable Fluid previousType) {
        Fluid type = tank.getDeclaredFluid();
        boolean hadNodes = previousMode == MODE_BUFFER && previousType != null;
        boolean needsNodes = mode == MODE_BUFFER && type != null;
        if (hadNodes == needsNodes && (!needsNodes || previousType == type)) return;
        LevelNodeGraph<PipeData> previous = null;
        if (hadNodes) {
            previous = FluidPipeGraph.get(server, previousType);
            removeBufferNodes(previous, getBlockState());
        }
        if (needsNodes) {
            LevelNodeGraph<PipeData> target =
                    previous != null
                                    && FluidPipeGraph.typeFor(previousType)
                                            == FluidPipeGraph.typeFor(type)
                            ? previous
                            : FluidPipeGraph.get(server, type);
            addBufferNodes(target, new PipeData(type));
        }
    }

    protected void addBufferNodes(LevelNodeGraph<PipeData> graph, PipeData data) {
        graph.addMachineNode(worldPosition.asLong(), data, FluidDuctBlockBase.OPEN_ALL, true);
    }

    protected void removeBufferNodes(LevelNodeGraph<PipeData> graph, BlockState state) {
        graph.removeNode(worldPosition.asLong());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (graphReady) onGraphDetach((ServerLevel) level);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        Fluid type = tank.getDeclaredFluid();
        if (graphReady && mode == MODE_BUFFER && type != null) {
            removeBufferNodes(FluidPipeGraph.get(server, type), getBlockState());
        }
        graphReady = false;
    }

    private static boolean isCorrosive(Fluid type) {
        return NTMFluidProperties.hasTrait(type, FT_Corrosive.class);
    }

    private static boolean isHot(Fluid type) {
        NTMFluidProperty prop = NTMFluidProperties.get(type);
        return prop != null && prop.temperature() >= 100;
    }

    @Override
    public void tickServer() {
        boolean changed = tank.setType(0, 1, inventory);
        changed |= tank.loadTank(2, 3, inventory);
        changed |= tank.unloadTank(4, 5, inventory);
        if (changed) setChanged();

        ServerLevel server = (ServerLevel) level;
        flush.provide(server, this);
        if (tank.getFill() > 0) checkFluidInteraction();
        networkPackNT(50);
    }

    public void refreshConnectors() {
        if (graphReady && !isRemoved())
            refreshConnectors((ServerLevel) level, false, Block.UPDATE_ALL);
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(new FlushLane(this, tank, FACES).onlyWhen(() -> mode == MODE_SEND && !isTilted()));
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return mode == MODE_SEND || mode == MODE_BUFFER ? tanks : NO_TANKS;
    }

    private void refreshConnectors(ServerLevel server, boolean typeChanged, int flags) {
        Fluid type = tank.getDeclaredFluid();
        BlockState state = getBlockState();
        if (state.getBlock() instanceof BlockFluidBarrel) {
            BlockState updated = state;
            updated = arm(updated, BlockFluidBarrel.NORTH, type, Direction.NORTH);
            updated = arm(updated, BlockFluidBarrel.EAST, type, Direction.EAST);
            updated = arm(updated, BlockFluidBarrel.SOUTH, type, Direction.SOUTH);
            updated = arm(updated, BlockFluidBarrel.WEST, type, Direction.WEST);
            if (updated != state) {
                server.setBlock(worldPosition, updated, flags);
                return;
            }
        }
        if (typeChanged) {
            FACES.collect(
                    server,
                    worldPosition,
                    (contact, side) -> FluidPipeBlock.refreshConnections(server, contact));
        }
    }

    private BlockState arm(
            BlockState state, BooleanProperty arm, @Nullable Fluid type, Direction dir) {
        BlockPos neighbour = worldPosition.relative(dir);
        if (ChunkUtil.chunkIfLoaded(level, neighbour) == null) return state;
        return state.setValue(
                arm,
                type != null
                        && PhysicalGraph.fluidContact(level, neighbour, dir.getOpposite(), type));
    }

    protected void checkFluidInteraction() {
        Fluid type = tank.getTankType();
        if (type == null) return;
        BlockState state = getBlockState();
        BlockPos pos = worldPosition;

        boolean isAntimatterBarrel = state.is(ModBlocks.BARREL_ANTIMATTER.get());
        if (!isAntimatterBarrel && NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
            level.destroyBlock(pos, false);
            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    5F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
            return;
        }

        if (state.is(ModBlocks.BARREL_PLASTIC.get()) && (isCorrosive(type) || isHot(type))) {
            level.destroyBlock(pos, false);
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
            return;
        }

        if (state.is(ModBlocks.BARREL_CORRODED.get())) {
            if (level.getRandom().nextInt(3) == 0) {
                tank.setFill(tank.getFill() - 1);
                FluidTrait.onRelease(level, pos, type, tank, FluidTrait.FluidReleaseType.SPILL, 1);
                setChanged();
            }
            if (level.getRandom().nextInt(3 * 60 * 20) == 0) level.destroyBlock(pos, false);
        }

        if (type == Fluids.WATER
                && TomSaveData.get((ServerLevel) level).fire > 1e-5F
                && level.getBrightness(LightLayer.SKY, pos) > 7) {
            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    5F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof FluidIdentifierItem;
            case 2 -> within(tank.containerContent(stack));
            case 4 -> within(tank.containerRoom(stack));
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    private boolean within(long moved) {
        return moved > 0 && moved <= tank.getMaxFill();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 3 || slot == 5;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int next) {
        assert next >= 0 && next < MODES;
        if (mode == next) return;
        int previous = mode;
        mode = next;
        if (graphReady && !isRemoved())
            changeBufferNodes((ServerLevel) level, previous, tank.getDeclaredFluid());
        setChanged();
    }

    public void cycleMode() {
        setMode((mode + 1) % MODES);
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.getBooleanOr("mode", false)) cycleMode();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "type").equals(name)) return NTMFluids.legacyName(tank.getTankType());
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "fillpercent").equals(name))
            return "" + (tank.getFill() * 100 / tank.getMaxFill());
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            setMode(RorModes.select(mode, params));
        }
        return null;
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        return tank.joins(type);
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tank.joins(type);
    }

    @Override
    public boolean canProvideFluid(Fluid type) {
        return tank.joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tank);
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (pressure != tank.getPressure()) return 0L;
        if (!tank.provides(type)) return 0L;
        return tank.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tank.provides(type) || pressure != tank.getPressure()) return;
        tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        setChanged();
    }

    @Override
    public boolean bufferedEndpoint() {
        return mode == MODE_BUFFER;
    }

    public boolean isTilted() {
        return false;
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        if (mode == MODE_BUFFER) return Long.MAX_VALUE;
        return mode == MODE_SEND && !isTilted() ? Long.MAX_VALUE : 0L;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (isTilted() || mode == MODE_SEND || mode == MODE_NONE) return 0L;
        if (pressure != tank.getPressure()) return 0L;
        if (!tank.accepts(type)) return 0L;
        return (long) tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tank.getPressure()) return amount;
        int accepted = tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return (mode == MODE_RECEIVE || mode == MODE_BUFFER) ? Long.MAX_VALUE : 0L;
    }

    @Override
    public IEnergyHandlerMK2.ConnectionPriority getFluidPriority() {
        return mode == MODE_BUFFER
                ? IEnergyHandlerMK2.ConnectionPriority.LOW
                : IEnergyHandlerMK2.ConnectionPriority.NORMAL;
    }

    @Override
    public int getComparatorPower() {
        if (tank.getFill() == 0) return 0;
        return Math.clamp((long) ((double) tank.getFill() / tank.getMaxFill() * 15.0) + 1, 0, 15);
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public String[] getFluidIDToCopy() {
        Fluid type = tank.getDeclaredFluid();
        return new String[] {
            BuiltInRegistries.FLUID.getKey(type == null ? NTMFluids.NONE : type).toString()
        };
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        Fluid previous = tank.getDeclaredFluid();
        boolean strict = tank.isStrict();
        IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
        if (previous != tank.getDeclaredFluid() || strict != tank.isStrict()) setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm.barrel");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuBarrel(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        int previousMode = mode;
        Fluid previousType = tank.getDeclaredFluid();
        super.loadAdditional(input);
        input.getInt("mode").ifPresent(v -> mode = v);
        input.child("tank").ifPresent(tank::deserialize);
        if (graphReady && !isRemoved()) {
            ServerLevel server = (ServerLevel) level;
            changeBufferNodes(server, previousMode, previousType);
            if (previousType != tank.getDeclaredFluid())
                refreshConnectors(server, true, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode);
        tank.serialize(output.child("tank"));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        Fluid type = tank.getFluid();
        if (type != null && type != Fluids.EMPTY && tank.getFill() > 0) {
            components.set(
                    ModDataComponents.FLUID_CONTENT.get(),
                    new FluidStackNTM(type, tank.getFill(), tank.getPressure()));
        }
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        FluidStackNTM content = components.get(ModDataComponents.FLUID_CONTENT.get());
        if (content != null && content.type() != Fluids.EMPTY && content.amount() > 0) {
            tank.setTankTypeByIdentifier(content.type());
            tank.receive(content.type(), (int) Math.min(content.amount(), Integer.MAX_VALUE));
        }
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.mode);
            case 1 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.mode = input.readInt();
            case 1 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
