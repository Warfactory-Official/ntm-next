// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ILaserable;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponent;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCoreReceiver;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCoreReceiver extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, FluidTankEndpoint, ILaserable, IGUIProvider, SyncUnitSchema {

    public static final long HE_PER_SPK = 5000L;
    public static final int TANK_CAPACITY = 64_000;
    public static final int CRYOGEL_PER_TICK = 20;

    private static final float MISFIRE_YIELD = 2.5F;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.CRYOGEL, TANK_CAPACITY);

    public long power;

    @SyncField(units = 1L << 0)
    public long joules;

    public BlockEntityCoreReceiver(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_RECEIVER.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        power = joules * HE_PER_SPK;

        if (joules > 0) {
            if (tank.getFill() >= CRYOGEL_PER_TICK) {
                tank.setFill(tank.getFill() - CRYOGEL_PER_TICK);
            } else {
                level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
                return;
            }
        }

        networkPackNT(50);
        joules = 0;
    }

    @Override
    public void addEnergy(Level level, BlockPos pos, long energy, Direction dir) {
        if (dir.getOpposite() == getBlockState().getValue(CoreComponent.FACING)) {
            joules += energy;
            return;
        }
        level.destroyBlock(worldPosition, false);
        level.explode(
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                MISFIRE_YIELD,
                Level.ExplosionInteraction.BLOCK);
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
        return power;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        joules = input.getLongOr("joules", 0L);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("joules", joules);
        tank.serialize(output.child("tank"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCoreReceiver(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcReceiver");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.joules);
            case 1 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.joules = input.readLong();
            case 1 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
