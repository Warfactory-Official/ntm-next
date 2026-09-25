// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.ICopiable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.NeighborDerived;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityHeaterElectric extends BlockEntityMachineBase
        implements AudioLoop, IHeatSource, IEnergyHandlerMK2, ICopiable, SyncUnitSchema {

    public static Consumer<BlockEntityHeaterElectric> CLIENT_SOUND = heater -> {};
    public long power;

    @SyncField(units = 1L << 1)
    public int heatEnergy;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    @SyncField(units = 1L << 0)
    private int setting;

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    public BlockEntityHeaterElectric(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_HEATER.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        int previousHeat = heatEnergy;
        boolean previousOn = isOn;

        heatEnergy *= 0.999D;
        tryPullHeat();

        isOn = false;
        long consumption = getConsumption();
        if (setting > 0 && power >= consumption) {
            power -= consumption;
            heatEnergy += getHeatGen();
            isOn = true;
        }

        if (power != previousPower || heatEnergy != previousHeat || isOn != previousOn)
            setChanged();
        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ELECTRIC_HUM_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.25F,
                7.5F,
                1.0F,
                20);
    }

    private void tryPullHeat() {

        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source == null) return;

        int stored = source.getHeatStored(level, heatPos);
        heatEnergy += stored * 0.85D;
        source.useUpHeat(level, heatPos, stored);
    }

    public void toggleSetting() {
        setting++;
        if (setting > 10) setting = 0;
    }

    public int getSetting() {
        return setting;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("setting", setting);
        return nbt;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        setting = nbt.getIntOr("setting", 0);

        setChanged();
        networkPackNT(25);
    }

    public long getConsumption() {
        return (long) (Math.pow(setting, 1.4D) * 200D);
    }

    public int getHeatGen() {
        return setting * 100;
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
        return getConsumption() * 20L;
    }

    @Override
    public int getHeatStored(Level level, BlockPos pos) {
        return BlockMultiblockCore.vendsHeatAt(this, pos) ? heatEnergy : 0;
    }

    @Override
    public void useUpHeat(Level level, BlockPos pos, int heat) {
        if (!BlockMultiblockCore.vendsHeatAt(this, pos)) return;
        heatEnergy = Math.max(0, heatEnergy - heat);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        power = input.getLongOr("power", power);
        setting = input.getIntOr("setting", setting);
        heatEnergy = input.getIntOr("heatEnergy", heatEnergy);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("setting", setting);
        output.putInt("heatEnergy", heatEnergy);
    }

    private void writeSetting(ByteBuf output) {
        output.writeByte(setting);
    }

    private void readSetting(ByteBuf input) {
        setting = input.readByte();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeSetting(output);
            case 1 -> output.writeInt(this.heatEnergy);
            case 2 -> output.writeBoolean(this.isOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readSetting(input);
            case 1 -> this.heatEnergy = input.readInt();
            case 2 -> this.isOn = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
