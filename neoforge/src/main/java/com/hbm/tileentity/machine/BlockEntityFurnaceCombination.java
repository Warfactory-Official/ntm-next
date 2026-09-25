// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuFurnaceCombination;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CombinationRecipe;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.NeighborDerived;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityFurnaceCombination extends BlockEntityMachinePolluting
        implements FluidTankEndpoint, IFluidCopiable, MenuProvider, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CANISTER_IN = 2;
    public static final int SLOT_CANISTER_OUT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int SMOKE_BUFFER = 50;

    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.25D;
    public static final int TANK_CAPACITY = 24_000;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank = new FluidTankNTM(TANK_CAPACITY);

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

    public BlockEntityFurnaceCombination(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBINATION_OVEN.get(), pos, state, SLOT_COUNT, SMOKE_BUFFER);
        sending = new FluidTankNTM[] {tank, smoke, smokeLeaded, smokePoison};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceCombination");
    }

    @Override
    public void tickServer() {
        tryPullHeat();

        flush.provide((ServerLevel) level, this);

        wasOn = false;

        if (tank.unloadTank(SLOT_CANISTER_IN, SLOT_CANISTER_OUT, inventory)) setChanged();

        if (canSmelt()) {
            int burn = heat / 100;

            if (burn > 0) {
                wasOn = true;
                progress += burn;
                heat -= burn;

                if (progress >= PROCESS_TIME) {
                    setChanged();
                    progress -= PROCESS_TIME;

                    CombinationRecipe recipe =
                            CombinationRecipes.INSTANCE.getOutput(inventory.get(SLOT_INPUT));
                    if (recipe != null) {
                        ItemStack out = recipe.makeOutput();
                        if (!out.isEmpty()) {
                            ItemStack cur = inventory.get(SLOT_OUTPUT);
                            if (cur.isEmpty()) inventory.set(SLOT_OUTPUT, out.copy());
                            else cur.grow(out.getCount());
                        }
                        if (recipe.fluidType() != null) {
                            if (tank.getTankType() != recipe.fluidType())
                                tank.setTankType(recipe.fluidType());
                            tank.setFill(tank.getFill() + recipe.fluidAmount());
                        }
                        inventory.get(SLOT_INPUT).shrink(1);
                    }
                }

                List<Entity> entities =
                        level.getEntitiesOfClass(
                                Entity.class,
                                new AABB(
                                        worldPosition.getX() - 0.5,
                                        worldPosition.getY() + 2,
                                        worldPosition.getZ() - 0.5,
                                        worldPosition.getX() + 1.5,
                                        worldPosition.getY() + 4,
                                        worldPosition.getZ() + 1.5));
                for (Entity e : entities) e.igniteForSeconds(5);

                if (TickPhase.every(this, 10)) {
                    level.playSound(
                            null,
                            worldPosition.above(),
                            ModSounds.FLAMETHROWER_SHOOT.get(),
                            SoundSource.BLOCKS,
                            0.25F,
                            0.5F);
                }
                if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                    pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
                }
            }
        } else {
            progress = 0;
        }

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        if (wasOn && level.getRandom().nextInt(15) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    worldPosition.getX() + 0.5 + level.getRandom().nextGaussian() * 0.5,
                    worldPosition.getY() + 2,
                    worldPosition.getZ() + 0.5 + level.getRandom().nextGaussian() * 0.5,
                    0,
                    0,
                    0);
        }
    }

    public boolean canSmelt() {
        CombinationRecipe recipe = CombinationRecipes.INSTANCE.getOutput(inventory.get(SLOT_INPUT));
        if (recipe == null) return false;

        ItemStack out = recipe.makeOutput();
        if (!out.isEmpty()) {
            ItemStack cur = inventory.get(SLOT_OUTPUT);
            if (!cur.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(out, cur)) return false;
                if (out.getCount() + cur.getCount() > cur.getMaxStackSize()) return false;
            }
        }

        if (recipe.fluidType() != null) {
            if (tank.getTankType() != recipe.fluidType() && tank.getFill() > 0) return false;
            return tank.getTankType() != recipe.fluidType()
                    || tank.getFill() + recipe.fluidAmount() <= tank.getMaxFill();
        }

        return true;
    }

    protected void tryPullHeat() {
        if (heat >= MAX_HEAT) return;

        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source != null) {
            int diff = source.getHeatStored(level, heatPos) - heat;
            if (diff == 0) return;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(level, heatPos, diff);
                heat += diff;
                if (heat > MAX_HEAT) heat = MAX_HEAT;
                return;
            }
        }

        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public @Nullable FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tank, FlushFaces.own(), 20);
        out.add(smoke, FlushFaces.own(), 20);
        out.add(smokeLeaded, FlushFaces.own(), 20);
        out.add(smokePoison, FlushFaces.own(), 20);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_CANISTER_IN) return FluidTankNTM.isFluidContainer(stack);
        return slot == SLOT_INPUT && CombinationRecipes.INSTANCE.getOutput(stack) != null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("prog").ifPresent(v -> progress = v);
        input.getInt("heat").ifPresent(v -> heat = v);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("prog", progress);
        output.putInt("heat", heat);
        tank.serialize(output.child("tank"));
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFurnaceCombination(containerId, playerInventory, this);
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
            case 3 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.wasOn = input.readBoolean();
            case 1 -> this.heat = input.readInt();
            case 2 -> this.progress = input.readInt();
            case 3 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
