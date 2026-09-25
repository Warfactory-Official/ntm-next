// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLane;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.inventory.container.MenuDroneCrate;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityDroneCrate extends BlockEntityMachineBase
        implements MenuProvider,
                IControlReceiver,
                IDroneLinkable,
                FluidFlushSender,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int CARGO_SLOTS = 18;
    public static final int SLOT_FLUID_ID = 18;
    public static final int SLOT_COUNT = 19;
    public static final int CAPACITY = 64_000;

    private static final int[] SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17
    };
    private static final int LINE_PARTICLE_RANGE = 150;
    private static final int LINE_COLOUR = 0x00ffff;

    @SyncField(units = 1L)
    public final FluidTankNTM tank = new FluidTankNTM(CAPACITY);

    @SyncField(units = 1L << 1)
    public @Nullable BlockPos next;

    @SyncField(units = 1L << 2)
    public boolean sendingMode = false;

    @SyncField(units = 1L << 3)
    public boolean itemType = true;

    private final FluidTankNTM[] sending = {tank};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityDroneCrate(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_CRATE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        ServerLevel server = (ServerLevel) level;
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();

        flush.provide(server, this);

        if (next != null) {
            for (EntityDeliveryDrone drone :
                    server.getEntitiesOfClass(
                            EntityDeliveryDrone.class, new AABB(worldPosition.above()))) {
                if (drone.getDeltaMovement().length() >= 0.05) continue;

                drone.setTarget(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);

                if (sendingMode && itemType) loadItems(server, drone);
                if (!sendingMode && itemType) unloadItems(server, drone);
                if (sendingMode && !itemType) loadFluid(server, drone);
                if (!sendingMode && !itemType) unloadFluid(server, drone);
            }

            ParticleCreators.droneLine(
                    server,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    next.getX() - worldPosition.getX(),
                    next.getY() - worldPosition.getY() - 1,
                    next.getZ() - worldPosition.getZ(),
                    LINE_COLOUR,
                    LINE_PARTICLE_RANGE);
        }

        networkPackNT(25);
    }

    private void loadItems(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 0) return;

        boolean loaded = false;

        for (int i = 0; i < CARGO_SLOTS; i++) {
            if (inventory.get(i).isEmpty()) continue;
            loaded = true;
            drone.setItem(i, inventory.get(i).copy());
            inventory.set(i, ItemStack.EMPTY);
        }

        if (loaded) {
            setChanged();
            drone.setAppearance(1);
            playUnpack(server);
        }
    }

    private void unloadItems(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 1) return;

        boolean emptied = true;

        for (int i = 0; i < CARGO_SLOTS; i++) {
            ItemStack carried = drone.getItem(i);
            if (carried.isEmpty()) continue;

            ItemStack left =
                    InventoryUtil.tryAddItemToInventory(inventory, 0, CARGO_SLOTS - 1, carried);
            drone.setItem(i, left);
            if (!left.isEmpty()) emptied = false;
        }

        setChanged();

        if (emptied) {
            drone.setAppearance(0);
            playUnpack(server);
        }
    }

    private void loadFluid(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 0 || tank.getFill() <= 0 || tank.getTankType() == null) return;

        drone.fluid = new FluidStackNTM(tank.getFluid(), tank.getFill());
        tank.setFill(0);
        drone.setAppearance(2);
        playUnpack(server);
        setChanged();
    }

    private void unloadFluid(ServerLevel server, EntityDeliveryDrone drone) {
        if (drone.getAppearance() != 2 || drone.fluid == null || !tank.accepts(drone.fluid.type()))
            return;

        long left =
                drone.fluid.amount()
                        - tank.receive(
                                drone.fluid.type(),
                                (int) Math.min(drone.fluid.amount(), Integer.MAX_VALUE));

        if (left <= 0) {
            drone.fluid = null;
            drone.setAppearance(0);
        } else {
            drone.fluid = new FluidStackNTM(drone.fluid.type(), left);
        }

        playUnpack(server);
        setChanged();
    }

    private void playUnpack(ServerLevel server) {
        server.playSound(
                null, worldPosition, ModSounds.ITEM_UNPACK.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
    }

    @Override
    public BlockPos getPoint() {
        return worldPosition.above();
    }

    @Override
    public void setNextTarget(BlockPos target) {
        this.next = target;
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("mode")) {
            sendingMode = !sendingMode;
            markChanged();
        }

        if (data.contains("type")) {
            itemType = !itemType;
            markChanged();
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (itemType || !sendingMode) return 0L;
        if (pressure != tank.getPressure() || !tank.accepts(type)) return 0L;
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
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                new FlushLane(this, tank, FlushFaces.own())
                        .every(20)
                        .onlyWhen(() -> !itemType && !sendingMode));
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (itemType || sendingMode) return 0L;
        if (pressure != tank.getPressure() || !tank.provides(type)) return 0L;
        return tank.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tank.provides(type) || pressure != tank.getPressure()) return;
        tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        next = input.read("next", BlockPos.CODEC).orElse(null);
        sendingMode = input.getBooleanOr("mode", false);
        itemType = input.getBooleanOr("type", false);
        input.child("t").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (next != null) output.store("next", BlockPos.CODEC, next);
        output.putBoolean("mode", sendingMode);
        output.putBoolean("type", itemType);
        tank.serialize(output.child("t"));
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droneCrate");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuDroneCrate(containerId, playerInventory, this);
    }

    private void writeNext(ByteBuf output) {
        output.writeBoolean(next != null);
        if (next != null) {
            output.writeInt(next.getX());
            output.writeInt(next.getY());
            output.writeInt(next.getZ());
        }
    }

    private void readNext(ByteBuf input) {
        next =
                input.readBoolean()
                        ? new BlockPos(input.readInt(), input.readInt(), input.readInt())
                        : null;
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.tank.packetSerialize(output);
            case 1 -> writeNext(output);
            case 2 -> output.writeBoolean(this.sendingMode);
            case 3 -> output.writeBoolean(this.itemType);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.tank.packetDeserialize(input);
            case 1 -> readNext(input);
            case 2 -> this.sendingMode = input.readBoolean();
            case 3 -> this.itemType = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
