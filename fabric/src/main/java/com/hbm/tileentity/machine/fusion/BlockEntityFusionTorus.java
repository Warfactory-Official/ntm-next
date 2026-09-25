// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.ItemPort;
import com.hbm.inventory.container.MenuFusionTorus;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.modules.machine.ModuleMachineFusion;
import com.hbm.packet.SyncField;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.Tiltable;
import com.hbm.tileentity.machine.albion.BlockEntityCooledBase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityFusionTorus extends BlockEntityCooledBase
        implements AudioLoop,
                Tiltable,
                IControlReceiver,
                MenuProvider,
                IPortHost,
                IRORValueProvider,
                IRORInteractive {
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "plasma",
                PREFIX_VALUE + "consumption",
                PREFIX_VALUE + "progress",
                PREFIX_VALUE + "recipe",
                PREFIX_VALUE + "active",
                PREFIX_VALUE + "temp",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final float MAGNET_ACCELERATION = 0.25F;
    private static final int[] ACCESSIBLE_SLOTS = {SLOT_OUTPUT};
    private static final int PORTS = 4;

    private static final ItemPort OUTPUT_PORT = ItemPort.extractOnly(SLOT_OUTPUT);
    public static Consumer<BlockEntityFusionTorus> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 8)
    public final FluidTankNTM[] tanks;

    @SyncField(units = 1L << 9)
    public final boolean[] connections = new boolean[PORTS];

    private final FusionItemAccessCache itemPorts = new FusionItemAccessCache(OUTPUT_PORT);

    @SyncField(units = 1L << 3)
    public boolean didProcess = false;

    @SyncField(units = 1L << 4)
    public long klystronEnergy;

    @SyncField(units = 1L << 5)
    public long plasmaEnergy;

    @SyncField(units = 1L << 6)
    public double fuelConsumption;

    @SyncField(units = 1L << 7)
    public final ModuleMachineFusion module;

    public float magnet;
    public float prevMagnet;
    public float magnetSpeed;
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    public BlockEntityFusionTorus(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_TORUS.get(), pos, state, SLOT_COUNT);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(4_000), new FluidTankNTM(4_000),
                    new FluidTankNTM(4_000), new FluidTankNTM(4_000)
                };
        receiving = new FluidTankNTM[] {coolantTanks[0], tanks[0], tanks[1], tanks[2]};
        sending = new FluidTankNTM[] {coolantTanks[1], tanks[3]};

        module =
                new ModuleMachineFusion(
                        0,
                        this,
                        FusionRecipes.INSTANCE,
                        this,
                        new int[0],
                        new int[] {SLOT_OUTPUT},
                        new FluidTankNTM[] {tanks[0], tanks[1], tanks[2]},
                        new FluidTankNTM[] {tanks[3]});
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        List<FusionPorts.Port> list = new ArrayList<>(PORTS * 2);
        for (int i = 0; i < PORTS; i++) {
            Direction dir = Direction.from3DDataValue(i + 2);
            BlockPos node = core.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7);
            list.add(new FusionPorts.Port(FusionPorts.Kind.KLYSTRON, node, dir));
            list.add(new FusionPorts.Port(FusionPorts.Kind.PLASMA, node, dir));
        }
        return list;
    }

    public static double getSpeedScaled(double max, double level) {
        if (max == 0) return 0D;
        if (level >= max * 0.5) return 1D;
        return level / max * 2D;
    }

    public static double getOutputIntensity(int receiverCount) {
        if (receiverCount == 1) return 1D;
        if (receiverCount == 2) return 0.625D;
        if (receiverCount == 3) return 0.5D;
        return 0.4375D;
    }

    private FusionPorts.Port klystronLink(int port) {
        Direction dir = Direction.from3DDataValue(port + 2);
        return new FusionPorts.Port(
                FusionPorts.Kind.KLYSTRON,
                worldPosition.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7),
                dir);
    }

    private FusionPorts.Port plasmaLink(int port) {
        Direction dir = Direction.from3DDataValue(port + 2);
        return new FusionPorts.Port(
                FusionPorts.Kind.PLASMA,
                worldPosition.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7),
                dir);
    }

    @Override
    public long getMaxPower() {
        return 10_000_000;
    }

    @Override
    public void tickServer() {
        ServerLevel serverLevel = (ServerLevel) level;

        checkTilt(Tiltable.TiltType.CONFIG, true);
        tickCooling();

        flush.provide(serverLevel, this);

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);

        int receiverCount = 0;
        int collectors = 0;

        for (int i = 0; i < PORTS; i++) {
            connections[i] =
                    FusionPorts.peer(serverLevel, klystronLink(i), BlockEntityFusionKlystron.class)
                                    != null
                            || FusionPorts.peer(
                                            serverLevel,
                                            klystronLink(i),
                                            BlockEntityFusionKlystronCreative.class)
                                    != null
                            || FusionPorts.peer(
                                            serverLevel,
                                            klystronLink(i),
                                            BlockEntityFusionCoupler.class)
                                    != null;

            IFusionPowerReceiver first =
                    FusionPorts.peer(serverLevel, plasmaLink(i), IFusionPowerReceiver.class);
            BlockEntityFusionCollector collector =
                    FusionPorts.peer(serverLevel, plasmaLink(i), BlockEntityFusionCollector.class);
            if (first != null || collector != null) connections[i] = true;
            if (first != null && first.receivesFusionPower()) receiverCount++;
            if (collector != null) collectors++;
        }

        FusionRecipe recipe = module.getRecipe() instanceof FusionRecipe f ? f : null;

        double powerFactor = getSpeedScaled(getMaxPower(), power);
        double fuel0Factor =
                recipe != null && recipe.inputFluid.length > 0
                        ? getSpeedScaled(tanks[0].getMaxFill(), tanks[0].getFill())
                        : 1D;
        double fuel1Factor =
                recipe != null && recipe.inputFluid.length > 1
                        ? getSpeedScaled(tanks[1].getMaxFill(), tanks[1].getFill())
                        : 1D;
        double fuel2Factor =
                recipe != null && recipe.inputFluid.length > 2
                        ? getSpeedScaled(tanks[2].getMaxFill(), tanks[2].getFill())
                        : 1D;

        double factor =
                Math.min(Math.min(powerFactor, fuel0Factor), Math.min(fuel1Factor, fuel2Factor));
        boolean ignition = recipe == null || recipe.ignitionTemp <= klystronEnergy;

        float r = 0F;
        float g = 0F;
        float b = 0F;
        plasmaEnergy = 0;
        fuelConsumption = 0;

        module.preUpdate(factor, collectors * 0.5D);
        module.update(1D, 1D, !isTilted() && isCool() && ignition, inventory.get(SLOT_BLUEPRINT));
        didProcess = module.didProcess;
        if (module.markDirty) setChanged();

        if (didProcess && recipe != null) {
            plasmaEnergy = (long) Math.ceil(recipe.outputTemp * factor);
            fuelConsumption = factor;
            r = recipe.r;
            g = recipe.g;
            b = recipe.b;
            if (level.getGameTime() % 20 == 15) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.HIGH_ENERGY_PARTICLES,
                        200);
            }
        }

        double outputIntensity = getOutputIntensity(receiverCount);
        double outputFlux = recipe != null ? recipe.neutronFlux * factor : 0D;

        if (plasmaEnergy > 0) {
            long powerReceived = (long) Math.ceil(plasmaEnergy * outputIntensity);
            float pr = r;
            float pg = g;
            float pb = b;
            for (int i = 0; i < PORTS; i++) {

                IFusionPowerReceiver receiver =
                        FusionPorts.peer(serverLevel, plasmaLink(i), IFusionPowerReceiver.class);
                if (receiver != null)
                    receiver.receiveFusionPower(powerReceived, outputFlux, pr, pg, pb);
            }
        }

        networkPackNT(150);
        klystronEnergy = 0;
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);

        double powerFactor = getSpeedScaled(getMaxPower(), power);
        if (didProcess) magnetSpeed += MAGNET_ACCELERATION;
        else magnetSpeed -= MAGNET_ACCELERATION;

        magnetSpeed = Mth.clamp(magnetSpeed, 0F, 30F * (float) powerFactor);

        prevMagnet = magnet;
        magnet += magnetSpeed;

        if (magnet >= 360F) {
            magnet -= 360F;
            prevMagnet -= 360F;
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        float speed = magnetSpeed / 30F;
        return AudioSystem.getLoopedSound(
                ModSounds.FUSION_REACTOR_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 2.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(speed),
                30F,
                speed,
                20);
    }

    @Override
    public int getFloorCount() {
        return 6 * 6;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return worldPosition.offset(-5 + (index / 6) * 2, -1, -5 + (index % 6) * 2);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.getItem() instanceof ItemBlueprints;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    protected double interactionRangeSq() {
        return 32 * 32;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getIntOr("index", 0) != 0) return;
            String key = data.getStringOr("selection", "");

            module.setRecipe(key);
            setChanged();
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "plasma").equals(name)) return "" + plasmaEnergy;
        if ((PREFIX_VALUE + "consumption").equals(name)) return "" + (int) (fuelConsumption * 100);
        if ((PREFIX_VALUE + "progress").equals(name))
            return "" + (int) Math.round(module.progress * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return module.legacyRecipeName();
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (didProcess ? 1 : 0);
        if ((PREFIX_VALUE + "temp").equals(name)) return "" + (int) temperature;
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrecipe").equals(name) && params.length == 1) {
            module.setRecipe(params[0], false);
            setChanged();
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < tanks.length; i++) {
            int idx = i;
            input.child("ft" + i).ifPresent(tanks[idx]::deserialize);
        }
        module.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("ft" + i));
        module.save(output);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionTorus");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFusionTorus(containerId, playerInventory, this);
    }

    @Override
    public @Nullable ItemPort itemAccess(BlockPos cell, Direction side) {
        return itemPorts.get(this, cell);
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 4; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 4; i++) tanks[i].packetDeserialize(input);
    }

    private void writeConnections(ByteBuf output) {
        for (boolean connected : connections) output.writeBoolean(connected);
    }

    private void readConnections(ByteBuf input) {
        for (int i = 0; i < PORTS; i++) connections[i] = input.readBoolean();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3f8L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeBoolean(this.didProcess);
            case 4 -> output.writeLong(this.klystronEnergy);
            case 5 -> output.writeLong(this.plasmaEnergy);
            case 6 -> output.writeDouble(this.fuelConsumption);
            case 7 -> this.module.serialize(output);
            case 8 -> writeTanks(output);
            case 9 -> writeConnections(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.didProcess = input.readBoolean();
            case 4 -> this.klystronEnergy = input.readLong();
            case 5 -> this.plasmaEnergy = input.readLong();
            case 6 -> this.fuelConsumption = input.readDouble();
            case 7 -> this.module.deserialize(input);
            case 8 -> readTanks(input);
            case 9 -> readConnections(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
