// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLane;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MachineFluidTank;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.interfaces.IOverpressurable;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.container.MenuMachineFluidTank;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidContainerInfinite;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.GasFlamePayload;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineFluidTank extends BlockEntityMachineBase
        implements FluidFlushSender,
                MenuProvider,
                IControlReceiver,
                PersistentDrop,
                IFluidCopiable,
                IOverpressurable,
                IRepairable,
                SyncUnitSchema,
                IRORValueProvider,
                IRORInteractive {
    private static final String[] PERSISTENT_KEYS = {"tank", "mode", "onFire"};

    private static final List<CountIngredient> REPAIR_MATERIALS =
            List.of(CountIngredient.of(OreDictManager.STEEL.plate(), 6));

    public static final int SLOT_COUNT = 6;
    public static final int CAPACITY = 256_000;

    public static final int MODE_RECEIVE = 0;
    public static final int MODE_BUFFER = 1;
    public static final int MODE_SEND = 2;
    public static final int MODE_NONE = 3;
    public static final int MODES = 4;
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
    public final FluidTankNTM tank = new FluidTankNTM(CAPACITY);

    @SyncField(units = 1L << 0)
    public int mode = MODE_RECEIVE;

    private int lastComparator;
    public boolean onFire;
    public @Nullable Explosion lastExplosion;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineFluidTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, SLOT_COUNT);
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public void tickServer() {
        ServerLevel server = (ServerLevel) level;
        updateBufferNode(server);
        boolean changed = false;
        if (!isDamaged()) {
            flush.provide(server, this);
            changed = tank.loadTank(2, 3, inventory);
            changed |= tank.setType(0, 1, inventory);
            if (TickPhase.every(this, 20)) setChanged();
        }
        int comparator = getComparatorPower();
        if (comparator != lastComparator) {
            setChanged();
            ((MachineFluidTank) getBlockState().getBlock())
                    .updateComparatorOutput(server, worldPosition);
        }
        lastComparator = comparator;
        if (tank.getFill() > 0) {
            Fluid type = tank.getTankType();
            if (NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
                antimatterBlast();
                explode();
                tank.setFill(0);
            }
            FT_Corrosive corrosive = NTMFluidProperties.getTrait(type, FT_Corrosive.class);
            if (corrosive != null && corrosive.isHighlyCorrosive()) explode();
            if (isDamaged()) {
                int amount =
                        NTMFluidProperties.hasTrait(type, FT_Amat.class)
                                ? tank.getFill()
                                : Math.min(
                                        tank.getFill(),
                                        tank.getMaxFill() / (gaseous(type) ? 100 : 10_000));
                updateLeak(amount);
            }
        }

        changed |= tank.unloadTank(4, 5, inventory);
        if (changed) setChanged();
        networkPackNT(150);
    }

    private static boolean gaseous(Fluid type) {
        return NTMFluidProperties.hasTrait(type, FT_Gaseous.class)
                || NTMFluidProperties.hasTrait(type, FT_Gaseous_ART.class);
    }

    private void antimatterBlast() {
        new ExplosionVNT(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.5,
                        worldPosition.getZ() + 0.5,
                        5F)
                .makeAmat()
                .setBlockAllocator(null)
                .setBlockProcessor(null)
                .explode();
    }

    public void explode() {
        setDamaged(true);
        onFire = NTMFluidProperties.hasTrait(tank.getTankType(), FT_Flammable.class);
        setChanged();
    }

    @Override
    public void explode(Level level, BlockPos pos) {
        if (!isDamaged()) explode();
    }

    public void updateLeak(int amount) {
        if (!isDamaged() || amount <= 0) return;
        tank.drain(amount, true);
        setChanged();
        Fluid type = tank.getTankType();
        if (NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
            antimatterBlast();
        } else if (NTMFluidProperties.hasTrait(type, FT_Flammable.class) && onFire) {
            AABB box =
                    new AABB(
                            worldPosition.getX() - 1.5,
                            worldPosition.getY(),
                            worldPosition.getZ() - 1.5,
                            worldPosition.getX() + 2.5,
                            worldPosition.getY() + 5,
                            worldPosition.getZ() + 2.5);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box))
                entity.igniteForSeconds(5F);
            var random = level.getRandom();
            double x = worldPosition.getX() + random.nextDouble();
            double y = worldPosition.getY() + 0.5 + random.nextDouble();
            double z = worldPosition.getZ() + random.nextDouble();
            Services.NETWORK.sendToAllAround(
                    new GasFlamePayload(
                            x,
                            y,
                            z,
                            random.nextGaussian() * 0.2,
                            0.1,
                            random.nextGaussian() * 0.2,
                            6.5F),
                    new TargetPoint((ServerLevel) level, x, y, z, 150));
            if (TickPhase.every(this, 5)) {
                FluidTrait.onRelease(
                        level, worldPosition, type, tank, FluidReleaseType.BURN, amount * 5);
            }
        } else if (gaseous(type) && TickPhase.every(this, 5)) {
            var options =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(1F)
                            .setBaseScale(1F)
                            .setMaxScale(5F)
                            .setLife(100 + level.getRandom().nextInt(20))
                            .setColor(NTMFluidProperties.get(type).color())
                            .build();
            ServerLevel server = (ServerLevel) level;
            for (var player : server.players()) {
                if (player.distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                        < 150 * 150) {
                    server.sendParticles(
                            player,
                            options,
                            true,
                            false,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5,
                            0,
                            0,
                            0,
                            0,
                            0);
                }
            }
            FluidTrait.onRelease(
                    level, worldPosition, type, tank, FluidReleaseType.SPILL, amount * 5);
        }
    }

    @Override
    public boolean isDamaged() {
        return getBlockState().getValue(MachineFluidTank.DAMAGED);
    }

    private void setDamaged(boolean damaged) {
        if (isDamaged() == damaged) return;
        level.setBlock(
                worldPosition,
                getBlockState().setValue(MachineFluidTank.DAMAGED, damaged),
                Block.UPDATE_CLIENTS);
        if (damaged && level instanceof ServerLevel server) removeNodes(server);
    }

    @Override
    public List<CountIngredient> getRepairMaterials() {
        return REPAIR_MATERIALS;
    }

    @Override
    public void repair(Player player) {
        setDamaged(false);
        setChanged();
    }

    @Override
    public void tryExtinguish(Level level, BlockPos pos, EnumExtinguishType type) {
        if (!isDamaged() || !onFire) return;
        if (type == EnumExtinguishType.WATER) {
            if (NTMFluidProperties.hasTrait(tank.getTankType(), FT_Liquid.class)) {
                level.explode(
                        null,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.5,
                        worldPosition.getZ() + 0.5,
                        5F,
                        true,
                        Level.ExplosionInteraction.BLOCK);
            } else {
                onFire = false;
                setChanged();
            }
        } else if (type == EnumExtinguishType.FOAM || type == EnumExtinguishType.CO2) {
            onFire = false;
            setChanged();
        }
    }

    private void updateBufferNode(ServerLevel server) {
        long core = worldPosition.asLong();
        Fluid type = tank.getDeclaredFluid();
        BlockState state = getBlockState();
        LevelNodeGraph<PipeData> current = FluidPipeGraph.graphAt(server, core);

        if (isDamaged()
                || mode != MODE_BUFFER
                || type == null
                || type == Fluids.EMPTY
                || !(state.getBlock() instanceof BlockMultiblockCore block)) {
            if (current != null) removeNodes(server);
            return;
        }

        LevelNodeGraph<PipeData> target = FluidPipeGraph.get(server, type);

        if (current != null && (current != target || current.getNode(core).data.fluid() != type)) {
            removeNodes(server);
            current = null;
        }
        Direction facing = state.getValue(BlockMultiblockCore.FACING);
        if (current == null) {
            target.addNode(core, new PipeData(type), 0);

            MultiblockSurface.forEachActiveFace(
                    block,
                    worldPosition,
                    facing,
                    null,
                    (cell, side) -> {
                        long key = cell.asLong();
                        if (key == core) return;
                        if (target.getNode(key) == null) {
                            target.addNode(key, new PipeData(type), 1 << side.ordinal());
                        }
                        target.addRemoteLink(core, key);
                    });
        }
        target.setSelfEndpoint(core, true);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);

        if (level instanceof ServerLevel server
                && FluidPipeGraph.graphAt(server, worldPosition.asLong()) != null) {
            removeNodes(server);
        }
    }

    public void removeNodes(ServerLevel server) {
        BlockState state = getBlockState();
        removeNode(server, worldPosition.asLong());
        if (!(state.getBlock() instanceof BlockMultiblockCore block)) return;
        MultiblockSurface.forEachActiveFace(
                block,
                worldPosition,
                state.getValue(BlockMultiblockCore.FACING),
                null,
                (cell, side) -> removeNode(server, cell.asLong()));
    }

    private static void removeNode(ServerLevel server, long key) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(server, key);
        if (graph != null) graph.removeNode(key);
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                new FlushLane(this, tank, FlushFaces.activePlane())
                        .onlyWhen(() -> !isDamaged() && mode == MODE_SEND));
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return !isDamaged() && (mode == MODE_SEND || mode == MODE_BUFFER)
                ? new FluidTankNTM[] {tank}
                : NO_TANKS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof FluidIdentifierItem;
            case 2, 4 -> isFluidContainer(stack);
            default -> false;
        };
    }

    public void cycleMode() {
        mode = (mode + 1) % MODES;
        setChanged();
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
            int next = RorModes.select(mode, params);
            if (next != mode) {
                mode = next;
                setChanged();
            }
        }
        return null;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (isDamaged() || mode == MODE_RECEIVE || mode == MODE_NONE) return 0L;
        if (pressure != tank.getPressure()) return 0L;
        if (!tank.provides(type)) return 0L;
        return tank.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (isDamaged() || mode == MODE_RECEIVE || mode == MODE_NONE) return;
        if (!tank.provides(type) || pressure != tank.getPressure()) return;
        tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        setChanged();
    }

    @Override
    public boolean bufferedEndpoint() {
        return !isDamaged() && mode == MODE_BUFFER;
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return !isDamaged() && (mode == MODE_SEND || mode == MODE_BUFFER)
                ? Math.max(500L, tank.getFill() / 100L)
                : 0L;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (isDamaged() || mode == MODE_SEND || mode == MODE_NONE) return 0L;
        if (pressure != tank.getPressure()) return 0L;
        if (!tank.accepts(type)) return 0L;
        return (long) tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        int request = (int) Math.min(amount, getDemand(type, pressure));
        int accepted = tank.fill(type, request, true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return !isDamaged() && (mode == MODE_RECEIVE || mode == MODE_BUFFER)
                ? Math.max(500L, (tank.getMaxFill() - tank.getFill()) / 100L)
                : 0L;
    }

    @Override
    public int getComparatorPower() {
        if (tank.getFill() == 0) return 0;
        return Math.clamp((long) ((double) tank.getFill() / tank.getMaxFill() * 15.0) + 1, 0, 15);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
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
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fluidtank");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineFluidTank(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getInt("mode").ifPresent(v -> mode = v);
        onFire = input.getBooleanOr("onFire", onFire);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode);
        output.putBoolean("onFire", onFire);
        tank.serialize(output.child("tank"));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        Fluid type = tank.getFluid();
        if (tank.getFill() == 0 && !isDamaged()) return;
        components.set(
                ModDataComponents.FLUID_TANK_CONTENTS.get(),
                new FluidTankContents(
                        new FluidStackNTM(
                                type == null ? Fluids.EMPTY : type,
                                tank.getFill(),
                                tank.getPressure()),
                        tank.getMaxFill(),
                        mode,
                        isDamaged(),
                        onFire));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        FluidTankContents contents = components.get(ModDataComponents.FLUID_TANK_CONTENTS.get());
        if (contents == null) return;
        tank.changeTankSize(contents.capacity());
        FluidStackNTM stored = contents.tank();
        tank.setTankTypeByIdentifier(stored.type());
        tank.withPressure(stored.pressure())
                .receive(stored.type(), (int) Math.min(stored.amount(), Integer.MAX_VALUE));
        mode = contents.mode();
        onFire = contents.onFire();
        setDamaged(contents.damaged());
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
