// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponent;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCoreInjector;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCoreInjector extends BlockEntityMachineBase
        implements FluidTankEndpoint, IGUIProvider, SyncUnitSchema {

    public static final int SLOT_FILL_A = 0;
    public static final int SLOT_DRAIN_A = 1;
    public static final int SLOT_FILL_B = 2;
    public static final int SLOT_DRAIN_B = 3;
    public static final int SLOT_COUNT = 4;

    public static final int RANGE = 15;
    public static final int TANK_CAPACITY = 128_000;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.DEUTERIUM, TANK_CAPACITY),
        new FluidTankNTM(NTMFluids.TRITIUM, TANK_CAPACITY)
    };

    @SyncField(units = 1L << 0)
    public int beam;

    public BlockEntityCoreInjector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_INJECTOR.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        tanks[0].setType(SLOT_FILL_A, SLOT_DRAIN_A, inventory);
        tanks[1].setType(SLOT_FILL_B, SLOT_DRAIN_B, inventory);

        beam = 0;
        Direction dir = getBlockState().getValue(CoreComponent.FACING);

        for (int i = 1; i <= RANGE; i++) {
            BlockPos pos = worldPosition.relative(dir, i);
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof BlockEntityCore core) {
                for (int t = 0; t < 2; t++) {
                    FluidTankNTM target = core.tanks[t];
                    if (target.getTankType() != tanks[t].getTankType()) {
                        if (target.getFill() != 0) continue;
                        target.setTankType(tanks[t].getTankType());
                    }
                    if (target.accepts(tanks[t].getFluid())) {
                        int moved = target.receive(tanks[t].getFluid(), tanks[t].getFill());
                        tanks[t].setFill(tanks[t].getFill() - moved);
                    }
                    core.setChanged();
                }
                beam = i;
                break;
            }

            if (!level.getBlockState(pos).isAir()) break;
        }

        setChanged();
        networkPackNT(250);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_FILL_A || slot == SLOT_FILL_B;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("fuel1").ifPresent(tanks[0]::deserialize);
        input.child("fuel2").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("fuel1"));
        tanks[1].serialize(output.child("fuel2"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCoreInjector(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcInjector");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.beam);
            case 1 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.beam = input.readInt();
            case 1 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
