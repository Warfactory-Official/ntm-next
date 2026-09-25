// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.pneumatic.PneumoTubeBlock;
import com.hbm.blocks.network.pneumatic.PneumoTubePaintableBlock;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuPneumoTube;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityPneumoTube extends BlockEntityMachineBase
        implements Audible, IGUIProvider, IFluidHandlerMK2, IControlReceiverFilter, SyncUnitSchema {

    public static final int SLOT_COUNT = 15;

    @SyncField(units = 1L << 1)
    public final ModulePatternMatcher pattern = new ModulePatternMatcher(SLOT_COUNT);

    @SyncField(units = 1L << 0)
    public final FluidTankNTM compair;

    @SyncField(units = 1L << 6)
    public @Nullable Direction insertionDir;

    @SyncField(units = 1L << 6)
    public @Nullable Direction ejectionDir;

    @SyncField(units = 1L << 6)
    public byte airMask;

    @SyncField(units = 1L << 3)
    public boolean whitelist = false;

    @SyncField(units = 1L << 2)
    public boolean redstone = false;

    private boolean powered;

    public void refreshRedstone() {
        powered = level.hasNeighborSignal(worldPosition);
    }

    @SyncField(units = 1L << 4)
    public byte sendOrder = 0;

    @SyncField(units = 1L << 5)
    public byte receiveOrder = 0;

    public int soundDelay = 0;
    public int sendCounter = 0;

    public BlockEntityPneumoTube(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PNEUMATIC_TUBE.get(), pos, state);
    }

    protected BlockEntityPneumoTube(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
        this.compair = new FluidTankNTM(NTMFluids.AIR, 4_000).withPressure(1);
    }

    public static int getRangeFromPressure(int pressure) {
        return switch (pressure) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 100;
            case 4 -> 250;
            case 5 -> 1_000;
            default -> 0;
        };
    }

    public static int getIdentifier(BlockPos pos) {
        return (pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX();
    }

    public static byte dirIndex(@Nullable Direction dir) {
        return (byte) (dir == null ? 6 : dir.ordinal());
    }

    public static @Nullable Direction dirFromIndex(int index) {
        return index < 0 || index > 5 ? null : Direction.from3DDataValue(index);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoTube");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    public boolean matchesFilter(ItemStack stack) {

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack filter = inventory.get(i);
            if (!filter.isEmpty() && this.pattern.isValidForFilter(filter, i, stack)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void tickServer() {

        if (!this.isCompressor() && !this.isEndpoint()) return;

        if (this.soundDelay > 0) this.soundDelay--;

        ServerLevel sl = (ServerLevel) level;
        long time = sl.getGameTime();

        if (this.isEndpoint()) PneumaticNetwork.setEndpoint(sl, worldPosition, true);

        if (this.isCompressor() && (!powered ^ this.redstone)) {

            int randTime = Math.abs((int) (time + getIdentifier(worldPosition)));

            PneumaticNetwork net = PneumaticNetwork.at(sl, worldPosition);

            if (randTime % 5 == 0 && net != null && this.compair.getFill() >= 50) {
                BlockPos sourcePos = worldPosition.relative(insertionDir);
                Direction sourceFace = this.insertionDir.getOpposite();

                if (PneumaticNetwork.presentAt(sl, sourcePos, sourceFace)) {

                    if (net.send(
                            sl,
                            sourcePos,
                            sourceFace,
                            this,
                            sendOrder,
                            receiveOrder,
                            getRangeFromPressure(compair.getPressure()),
                            sendCounter)) {
                        this.compair.setFill(this.compair.getFill() - 50);

                        if (this.soundDelay <= 0 && !isMuffled()) {
                            sl.playSound(
                                    null,
                                    worldPosition,
                                    ModSounds.TUBE_FWOOMP.get(),
                                    SoundSource.BLOCKS,
                                    0.25F,
                                    0.9F + sl.getRandom().nextFloat() * 0.2F);
                            this.soundDelay = SharedConstants.TICKS_PER_SECOND;
                        }
                    }

                    this.sendCounter++;
                }
            }
        }

        this.networkPackNT(15);
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return Mth.clamp((this.compair.getMaxFill() - this.compair.getFill()) / 25, 1, 100);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != compair.getPressure() || !compair.accepts(type)) return 0L;
        return (long) compair.getMaxFill() - compair.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != compair.getPressure() || !compair.accepts(type)) return amount;
        int accepted = compair.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid type) {
        return new int[] {compair.getPressure(), compair.getPressure()};
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        return dir != this.insertionDir
                && dir != this.ejectionDir
                && compair.accepts(type)
                && this.isCompressor();
    }

    public boolean isCompressor() {
        return this.insertionDir != null;
    }

    public boolean isEndpoint() {
        return this.ejectionDir != null;
    }

    private void writeNozzles(ByteBuf output) {
        output.writeByte(dirIndex(insertionDir));
        output.writeByte(dirIndex(ejectionDir));
        output.writeByte(airMask);
    }

    private void readNozzles(ByteBuf input) {
        Direction insertion = dirFromIndex(input.readByte());
        Direction ejection = dirFromIndex(input.readByte());
        byte air = input.readByte();
        Direction oldInsertion = this.insertionDir;
        Direction oldEjection = this.ejectionDir;
        byte oldAir = this.airMask;
        this.insertionDir = insertion;
        this.ejectionDir = ejection;
        this.airMask = air;
        refreshNozzleMesh(oldInsertion, oldEjection, oldAir);
    }

    public void setAirMask(int mask) {
        if (this.airMask == (byte) mask) return;
        this.airMask = (byte) mask;
        setChanged();
        syncToTracking();
    }

    private void refreshNozzleMesh(
            @Nullable Direction oldInsertion, @Nullable Direction oldEjection, byte oldAir) {
        if (oldInsertion == this.insertionDir
                && oldEjection == this.ejectionDir
                && oldAir == this.airMask) return;
        if (level == null || !level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PneumoTubeBlock
                || state.getBlock() instanceof PneumoTubePaintableBlock) {
            level.sendBlockUpdated(worldPosition, state, state, 0);
        }
    }

    @Override
    public void nextMode(int index) {
        this.pattern.nextMode(level, inventory.get(index), index);
    }

    public void initPattern(ItemStack stack, int index) {
        this.pattern.initPatternSmart(level, stack, index);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        Direction oldInsertion = this.insertionDir;
        Direction oldEjection = this.ejectionDir;
        byte oldAir = this.airMask;
        super.loadAdditional(input);
        this.insertionDir = dirFromIndex(input.getByteOr("insertionDir", (byte) 6));
        this.ejectionDir = dirFromIndex(input.getByteOr("ejectionDir", (byte) 6));
        this.airMask = input.getByteOr("airMask", (byte) 0);
        input.child("tank").ifPresent(this.compair::deserialize);
        this.pattern.load(input);

        this.sendOrder = input.getByteOr("sendOrder", (byte) 0);
        this.receiveOrder = input.getByteOr("receiveOrder", (byte) 0);
        this.sendCounter = input.getIntOr("sendCounter", 0);

        this.whitelist = input.getBooleanOr("whitelist", false);
        this.redstone = input.getBooleanOr("redstone", false);
        this.powered = input.getBooleanOr("powered", false);
        refreshNozzleMesh(oldInsertion, oldEjection, oldAir);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        syncToTracking();
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("insertionDir", dirIndex(insertionDir));
        output.putByte("ejectionDir", dirIndex(ejectionDir));
        output.putByte("airMask", airMask);
        this.compair.serialize(output.child("tank"));
        this.pattern.save(output);

        output.putByte("sendOrder", sendOrder);
        output.putByte("receiveOrder", receiveOrder);
        output.putInt("sendCounter", sendCounter);

        output.putBoolean("whitelist", whitelist);
        output.putBoolean("redstone", redstone);
        output.putBoolean("powered", powered);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPneumoTube(containerId, playerInventory, this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("whitelist")) {
            this.whitelist = !this.whitelist;
        }
        if (data.contains("redstone")) {
            this.redstone = !this.redstone;
        }
        if (data.contains("pressure")) {
            int pressure = this.compair.getPressure() + 1;
            if (pressure > 5) pressure = 1;
            this.compair.withPressure(pressure);
        }
        if (data.contains("send")) {
            this.sendOrder++;
            if (this.sendOrder > 2) this.sendOrder = 0;
        }
        if (data.contains("receive")) {
            this.receiveOrder++;
            if (this.receiveOrder > 1) this.receiveOrder = 0;
        }
        if (data.contains("slot")) {
            setFilterContents(data);
        }

        this.setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[] {0, SLOT_COUNT};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {compair};
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.compair.packetSerialize(output);
            case 1 -> this.pattern.serialize(output);
            case 2 -> output.writeBoolean(this.redstone);
            case 3 -> output.writeBoolean(this.whitelist);
            case 4 -> output.writeByte(this.sendOrder);
            case 5 -> output.writeByte(this.receiveOrder);
            case 6 -> writeNozzles(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.compair.packetDeserialize(input);
            case 1 -> this.pattern.deserialize(input);
            case 2 -> this.redstone = input.readBoolean();
            case 3 -> this.whitelist = input.readBoolean();
            case 4 -> this.sendOrder = input.readByte();
            case 5 -> this.receiveOrder = input.readByte();
            case 6 -> readNozzles(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
