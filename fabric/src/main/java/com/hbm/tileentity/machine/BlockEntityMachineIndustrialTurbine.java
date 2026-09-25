// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.packet.SyncField;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineIndustrialTurbine extends BlockEntityTurbineBase
        implements AudioLoop {

    public static final String[] ROR =
            new String[] {PREFIX_VALUE + "output", PREFIX_VALUE + "flywheel"};
    public static final double FLYWHEEL_MAX_ENERGY = 0.5e8;
    public static @Nullable Consumer<BlockEntityMachineIndustrialTurbine> CLIENT_SOUND;

    @SyncField(units = 1L << 3)
    public double spin = 0;

    public long maxPower = 0;
    public long lastPowerTarget = 0;
    public long flywheel_energy = 0;

    public float rotor;
    public float lastRotor;

    public BlockEntityMachineIndustrialTurbine(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.IND_TURBINE.get(),
                pos,
                state,
                MachineData.INDUSTRIAL_TURBINE_INPUT_TANK_SIZE.get(),
                MachineData.INDUSTRIAL_TURBINE_OUTPUT_TANK_SIZE.get());
    }

    @Override
    public double consumptionPercent() {
        return 0.2D;
    }

    @Override
    public double getEfficiency() {
        return MachineData.INDUSTRIAL_TURBINE_EFFICIENCY.get();
    }

    @Override
    public boolean doesResizeCompressor() {
        return true;
    }

    @Override
    public void generatePower(long power, int steamConsumed) {
        FT_Coolable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        double eff =
                trait.getEfficiency(CoolingType.TURBINE)
                        * MachineData.INDUSTRIAL_TURBINE_EFFICIENCY.get();
        int maxOps =
                (int) Math.ceil((tanks[0].getMaxFill() * consumptionPercent()) / trait.amountReq);
        this.maxPower = (long) (maxOps * trait.heatEnergy * eff);
        this.flywheel_energy += power;
    }

    @Override
    protected void onServerTick() {

        this.spin = (double) flywheel_energy / FLYWHEEL_MAX_ENERGY;
        this.lastPowerTarget =
                Math.min((long) (Math.max(this.spin, 0.05) * maxPower), this.flywheel_energy);
        this.flywheel_energy -= this.lastPowerTarget;
        this.powerBuffer = this.lastPowerTarget;
    }

    @Override
    protected void onClientTick() {
        this.lastRotor = this.rotor;
        float speed = this.spin >= 0.5 ? 30 : (float) (Math.pow(this.spin * 2, 0.5) * 30);
        this.rotor += speed;
        if (this.rotor >= 360) {
            this.lastRotor -= 360;
            this.rotor -= 360;
        }
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.LARGE_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(audioVolume()),
                20F,
                audioPitch(),
                20);
    }

    public float audioVolume() {
        return 0.25F + spinRamp() * 0.75F;
    }

    public float audioPitch() {
        return 0.5F + spinRamp() * 0.5F + Mth.positiveModulo(worldPosition.hashCode(), 50) / 1000F;
    }

    private float spinRamp() {
        return (float) Math.min(1F, this.spin * 2);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) this.powerBuffer;
        if ((PREFIX_VALUE + "flywheel").equals(name)) return "" + (int) (spin * 100);
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("lastPowerTarget").ifPresent(v -> lastPowerTarget = v);
        input.getLong("flywheel_energy").ifPresent(v -> flywheel_energy = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        spin = input.getDoubleOr("spin", spin);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("lastPowerTarget", lastPowerTarget);
        output.putLong("flywheel_energy", flywheel_energy);
        output.putLong("maxPower", maxPower);
        output.putDouble("spin", spin);
    }

    public boolean acceptsFace(Direction dir) {
        return dir == facing().getOpposite();
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        if (!NTMFluidProperties.hasTrait(type, FT_Coolable.class) && type != NTMFluids.SPENTSTEAM)
            return false;
        Direction myDir = facing();
        return dir != null && dir != myDir && dir != myDir.getOpposite();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeDouble(this.spin);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.spin = input.readDouble();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
