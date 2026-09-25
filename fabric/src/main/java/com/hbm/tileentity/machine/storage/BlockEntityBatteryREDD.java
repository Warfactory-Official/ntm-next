// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.network.MachineBatteryREDD;
import com.hbm.inventory.container.MenuBatteryREDD;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncField;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityBatteryREDD extends BlockEntityMachineBattery implements AudioLoop {

    public static final long REDD_MAX_POWER = Long.MAX_VALUE / 100L;

    private static final String[] REDD_KEYS = {"bigPower", "muffled"};
    private static final String[] NO_ROR = new String[0];

    public static @Nullable Consumer<BlockEntityBatteryREDD> CLIENT_SOUND;

    @SyncField(units = 1L << 2)
    public BigInteger bigPower = BigInteger.ZERO;

    @SyncField(units = 1L << 3)
    public BigInteger bigDelta = BigInteger.ZERO;

    private final BigInteger[] bigLog = new BigInteger[DELTA_WINDOW];

    public float rotation;
    public float prevRotation;

    public BlockEntityBatteryREDD(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_REDD.get(), pos, state);
        Arrays.fill(bigLog, BigInteger.ZERO);
    }

    @Override
    public long getPower() {
        return bigPower.min(BigInteger.valueOf(getMaxPower() / 2L)).longValue();
    }

    @Override
    public void setPower(long power) {}

    @Override
    public long getMaxPower() {
        return REDD_MAX_POWER;
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode();
        return mode == MODE_OUTPUT || mode == MODE_BUFFER ? getMaxPower() : 0L;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode();
        return mode == MODE_INPUT || mode == MODE_BUFFER ? getMaxPower() : 0L;
    }

    @Override
    public boolean allowDirectProvision() {
        return false;
    }

    @Override
    public String[] getFunctionInfo() {
        return NO_ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        return null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    protected boolean readsSignal() {
        Level level = getLevel();
        if (level == null) return false;
        for (BlockPos port :
                MachineBatteryREDD.ports(
                        worldPosition, BlockMultiblockCore.coreFacing(getBlockState()))) {
            if (level.hasNeighborSignal(port)) return true;
        }
        return false;
    }

    @Override
    protected void updateBufferNode(ServerLevel server) {
        PowerGraph.get(server).setSelfEndpoint(worldPosition.asLong(), true);
    }

    @Override
    public void usePower(long power) {
        bigPower = bigPower.subtract(BigInteger.valueOf(power));
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (!simulate) bigPower = bigPower.add(BigInteger.valueOf(power));
        return 0L;
    }

    @Override
    public void tickServer() {
        BigInteger before = bigPower;

        long toAdd = ItemEnergyTransfer.extract(this, SLOT_CHARGE, getMaxPower(), false);
        if (toAdd > 0) bigPower = bigPower.add(BigInteger.valueOf(toAdd));
        long held = getPower();
        long toRemove = ItemEnergyTransfer.insert(this, SLOT_DISCHARGE, held, false);
        if (toRemove > 0) bigPower = bigPower.subtract(BigInteger.valueOf(toRemove));

        updateBufferNode((ServerLevel) level);
        if (getRelevantMode() == MODE_OUTPUT) provideToNeighbours((ServerLevel) level);

        BigInteger avg = bigPower.add(before).divide(BigInteger.TWO);
        bigDelta = avg.subtract(bigLog[0]);
        System.arraycopy(bigLog, 1, bigLog, 0, bigLog.length - 1);
        bigLog[bigLog.length - 1] = avg;

        networkPackNT(100);
    }

    public float getSpeed() {
        double speed = Math.pow(Math.log(bigPower.doubleValue() * 0.05D + 1D) * 0.05F, 5D);
        return (float) Math.min(speed, 15D);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        rotation += getSpeed();
        if (rotation >= 360F) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    public float audioPitch() {
        return 0.5F + getSpeed() / 15F * 1.5F;
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.FENSU_HUM_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(1.5F),
                25F,
                audioPitch(),
                5);
    }

    private static void writeBig(ByteBuf buf, BigInteger value) {
        byte[] bytes = value.toByteArray();
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static BigInteger readBig(ByteBuf buf) {
        byte[] bytes = new byte[buf.readInt()];
        buf.readBytes(bytes);
        return new BigInteger(bytes);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.batteryREDD");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuBatteryREDD(containerId, playerInventory, this);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(ModDataComponents.REDD_CHARGE.get(), bigPower);
        if (isMuffled()) components.set(ModDataComponents.MUFFLED.get(), true);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        if (Boolean.TRUE.equals(components.get(ModDataComponents.MUFFLED.get()))) setMuffled();
        BigInteger stored = components.get(ModDataComponents.REDD_CHARGE.get());
        if (stored == null) return;
        bigPower = stored;
    }

    @Override
    public String[] persistentKeys() {
        return REDD_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bigPower = new BigInteger(input.getString("bigPower").orElse("0"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("bigPower", bigPower.toString());
    }

    private void writePower(ByteBuf output) {
        writeBig(output, bigPower);
    }

    private void readPower(ByteBuf input) {
        bigPower = readBig(input);
    }

    private void writeDelta(ByteBuf output) {
        writeBig(output, bigDelta);
    }

    private void readDelta(ByteBuf input) {
        bigDelta = readBig(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xcL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> writePower(output);
            case 3 -> writeDelta(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> readPower(input);
            case 3 -> readDelta(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
