// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuFusionKlystron;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
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

public class BlockEntityFusionKlystron extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                IControlReceiver,
                MenuProvider,
                SyncUnitSchema {
    public static final long MAX_OUTPUT = 1_000_000;
    public static final int AIR_CONSUMPTION = 2_500;
    public static final float FAN_ACCELERATION = 0.125F;
    public static Consumer<BlockEntityFusionKlystron> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 4)
    public final FluidTankNTM compair;

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 2)
    public long outputTarget;

    @SyncField(units = 1L << 3)
    public long output;

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public long maxPower;

    public float fan;
    public float prevFan;
    public float fanSpeed;

    public BlockEntityFusionKlystron(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_KLYSTRON.get(), pos, state, 1);
        compair = new FluidTankNTM(NTMFluids.AIR, AIR_CONSUMPTION * 60);
        receiving = new FluidTankNTM[] {compair};
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.KLYSTRON,
                        core.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4),
                        dir));
    }

    public static boolean provideKyU(ServerLevel level, FusionPorts.Port link, long output) {
        BlockEntityFusionTorus torus = FusionPorts.peer(level, link, BlockEntityFusionTorus.class);
        if (torus == null) return false;
        torus.klystronEnergy += output;
        return true;
    }

    @Override
    public void tickServer() {
        maxPower = Math.max(1_000_000L, outputTarget * 100L);
        power += ItemEnergyTransfer.extract(this, 0, maxPower - power, false);

        output = 0;

        double powerFactor = BlockEntityFusionTorus.getSpeedScaled(maxPower, power);
        double airFactor =
                BlockEntityFusionTorus.getSpeedScaled(compair.getMaxFill(), compair.getFill());
        double factor = Math.min(powerFactor, airFactor);

        long powerReq = (long) Math.ceil(outputTarget * factor);
        int airReq = (int) Math.ceil(AIR_CONSUMPTION * factor);

        if (outputTarget > 0 && power >= powerReq && compair.getFill() >= airReq) {
            output = powerReq;
            power -= powerReq;
            compair.setFill(compair.getFill() - airReq);
        }

        if (output < outputTarget / 50) output = 0;

        ServerLevel serverLevel = (ServerLevel) level;
        FusionPorts.Port link = links(worldPosition, facing()).get(0);
        provideKyU(serverLevel, link, output);

        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);

        double mult = BlockEntityFusionTorus.getSpeedScaled(outputTarget, output);
        if (output > 0) fanSpeed += FAN_ACCELERATION * mult;
        else fanSpeed -= FAN_ACCELERATION;

        fanSpeed = Mth.clamp(fanSpeed, 0F, 5F * (float) mult);

        prevFan = fan;
        fan += fanSpeed;

        if (fan >= 360F) {
            fan -= 360F;
            prevFan -= 360F;
        }
    }

    private Direction facing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    @Override
    public AudioWrapper createAudioLoop() {
        float speed = fanSpeed / 5F;
        return AudioSystem.getLoopedSound(
                ModSounds.FEL_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 2.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(speed),
                15F,
                speed,
                20);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
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
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5,
                                worldPosition.getY() + 2.5,
                                worldPosition.getZ() + 0.5)
                < 20 * 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        data.getLong("amount")
                .ifPresent(
                        amount -> {
                            outputTarget = Mth.clamp(amount, 0L, MAX_OUTPUT);
                            setChanged();
                        });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        input.getLong("outputTarget").ifPresent(v -> outputTarget = v);
        input.child("t").ifPresent(compair::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putLong("outputTarget", outputTarget);
        compair.serialize(output.child("t"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionKlystron");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFusionKlystron(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.maxPower);
            case 2 -> output.writeLong(this.outputTarget);
            case 3 -> output.writeLong(this.output);
            case 4 -> this.compair.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.maxPower = input.readLong();
            case 2 -> this.outputTarget = input.readLong();
            case 3 -> this.output = input.readLong();
            case 4 -> this.compair.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
