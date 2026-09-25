// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuMachineCoker;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CokerRecipe;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.NeighborDerived;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineCoker extends BlockEntityMachineBase
        implements FluidTankEndpoint, MenuProvider, IFluidCopiable, SyncUnitSchema {
    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.25D;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_OUTPUT};

    @SyncField(units = 1L << 3)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 0)
    public boolean wasOn;

    @SyncField(units = 1L << 2)
    public int progress;

    @SyncField(units = 1L << 1)
    public int heat;

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineCoker(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKER.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.HEAVYOIL, 16_000);
        tanks[1] = new FluidTankNTM(NTMFluids.OIL_COKER, 8_000);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCoker");
    }

    @Override
    public void tickServer() {
        tryPullHeat();
        tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        this.wasOn = false;

        if (canProcess()) {
            int burn = heat / 100;

            if (burn > 0) {
                this.wasOn = true;
                this.progress += burn;
                this.heat -= burn;

                if (progress >= PROCESS_TIME) {
                    setChanged();
                    progress -= PROCESS_TIME;

                    CokerRecipe recipe = CokerRecipes.INSTANCE.getOutput(tanks[0].getTankType());
                    ItemStack output = recipe.output();
                    FluidStackNTM byproduct = recipe.byproduct();

                    if (!output.isEmpty()) {
                        ItemStack slot = inventory.get(SLOT_OUTPUT);
                        if (slot.isEmpty()) inventory.set(SLOT_OUTPUT, output.copy());
                        else slot.grow(output.getCount());
                    }
                    if (byproduct != null)
                        tanks[1].setFill(tanks[1].getFill() + (int) byproduct.amount());
                    tanks[0].setFill(tanks[0].getFill() - recipe.fillReq());
                }
            }

            if (wasOn && TickPhase.every(this, 5)) {
                PollutionHandler.incrementPollution(
                        level,
                        worldPosition,
                        PollutionType.SOOT,
                        PollutionHandler.SOOT_PER_SECOND * 5);
            }
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        if (!wasOn) return;
        if (!TickPhase.every(this, 2)) return;

        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(10F)
                        .setBaseScale(0.75F)
                        .setMaxScale(3F)
                        .setLife(200 + level.getRandom().nextInt(50))
                        .setColor(0x404040)
                        .build();
        level.addParticle(
                opts,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 22,
                worldPosition.getZ() + 0.5,
                0,
                0,
                0);
    }

    public boolean canProcess() {
        CokerRecipe recipe = CokerRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        if (recipe == null) return false;

        FluidStackNTM byproduct = recipe.byproduct();
        if (byproduct != null) tanks[1].setTankType(byproduct.type());

        if (tanks[0].getFill() < recipe.fillReq()) return false;
        if (byproduct != null && byproduct.amount() + tanks[1].getFill() > tanks[1].getMaxFill())
            return false;

        ItemStack output = recipe.output();
        ItemStack slot = inventory.get(SLOT_OUTPUT);
        if (!output.isEmpty() && !slot.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, slot)) return false;
            return output.getCount() + slot.getCount() <= output.getMaxStackSize();
        }
        return true;
    }

    protected void tryPullHeat() {
        if (this.heat >= MAX_HEAT) return;

        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source != null) {
            int diff = source.getHeatStored(level, heatPos) - this.heat;
            if (diff == 0) return;

            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(level, heatPos, diff);
                this.heat += diff;
                if (this.heat > MAX_HEAT) this.heat = MAX_HEAT;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
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
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_FLUID_ID && stack.getItem() instanceof FluidIdentifierItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineCoker(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.getInt("heat").ifPresent(v -> heat = v);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putInt("heat", heat);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.wasOn);
            case 1 -> output.writeInt(this.heat);
            case 2 -> output.writeInt(this.progress);
            case 3 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.wasOn = input.readBoolean();
            case 1 -> this.heat = input.readInt();
            case 2 -> this.progress = input.readInt();
            case 3 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
