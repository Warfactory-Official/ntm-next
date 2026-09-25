// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.EntityRBMKDebris.DebrisType;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.inventory.container.MenuRBMKOutgasser;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.OutgasserRecipe;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRBMKOutgasser extends BlockEntityRBMKBase
        implements IRBMKFluxReceiver, FluidTankEndpoint, MenuProvider, SyncUnitSchema {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM gas = new FluidTankNTM(NTMFluids.TRITIUM, 64_000);

    private final FluidTankNTM[] sending;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    @SyncField(units = 1L << 5)
    public double progress;

    public int duration = OutgasserRecipes.DURATION;

    public BlockEntityRBMKOutgasser(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_OUTGASSER.get(), pos, state, 2);
        sending = new FluidTankNTM[] {gas};
    }

    @Override
    public void tickServer() {
        if (!canProcess()) this.progress = 0;

        flush.provide((ServerLevel) level, this);
        super.tickServer();
    }

    @Override
    public void receiveFlux(NeutronStream stream) {
        if (!canProcess()) return;
        double efficiency = Math.min(1 - stream.fluxRatio * 0.8, 1);
        progress += stream.fluxQuantity * efficiency * RBMKConfig.getOutgasserMod(level);
        if (progress > duration) {
            process();
            markChanged();
        }
    }

    public boolean canProcess() {
        ItemStack in = inventory.get(0);
        if (in.isEmpty()) return false;

        OutgasserRecipe recipe = OutgasserRecipes.INSTANCE.getRecipe(in, level);
        if (recipe == null) return false;

        FluidStackNTM fluid = recipe.fluidOutput();
        if (fluid != null) {
            Fluid type = fluid.type();
            if (gas.getTankType() != type && gas.getFill() > 0) return false;
            gas.setTankType(type);
            if (gas.getFill() + (int) fluid.amount() > gas.getMaxFill()) return false;
        }

        ItemStack out = recipe.solidOutput();
        if (out.isEmpty()) return true;
        ItemStack slot1 = inventory.get(1);
        if (slot1.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(slot1, out)
                && slot1.getCount() + out.getCount() <= slot1.getMaxStackSize();
    }

    private void process() {
        OutgasserRecipe recipe = OutgasserRecipes.INSTANCE.getRecipe(inventory.get(0), level);
        if (recipe == null) return;

        inventory.get(0).shrink(1);
        if (inventory.get(0).isEmpty()) inventory.set(0, ItemStack.EMPTY);
        this.progress = 0;

        FluidStackNTM fluid = recipe.fluidOutput();
        if (fluid != null) gas.setFill(gas.getFill() + (int) fluid.amount());

        ItemStack out = recipe.solidOutput();
        if (!out.isEmpty()) {
            ItemStack slot1 = inventory.get(1);
            if (slot1.isEmpty()) inventory.set(1, out.copy());
            else slot1.grow(out.getCount());
        }
    }

    @Override
    public void onMelt(int reduce) {
        int count = 4 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(DebrisType.BLANK);
        super.onMelt(reduce);
    }

    @Override
    public RBMKType getRBMKType() {
        return RBMKType.OUTGASSER;
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.OUTGASSER;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0
                && (level == null || OutgasserRecipes.INSTANCE.getRecipe(stack, level) != null);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0, 1};
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(
                gas,
                (server, core, contacts) -> {
                    if (!collectColumnOutputs(server, core, contacts))
                        contacts.contact(core.below(), Direction.UP);
                });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getDoubleOr("progress", progress);
        input.child("gas").ifPresent(gas::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
        gas.serialize(output.child("gas"));
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("progress", progress);
        writeDiagnostics(tag, "gas", gas);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkOutgasser");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKOutgasser(id, inv, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> this.gas.packetSerialize(output);
            case 5 -> output.writeDouble(this.progress);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.gas.packetDeserialize(input);
            case 5 -> this.progress = input.readDouble();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
