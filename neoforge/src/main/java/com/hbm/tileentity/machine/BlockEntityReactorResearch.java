// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuReactorResearch;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.tool.ItemSwordMeteorite;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@Deprecated
public class BlockEntityReactorResearch extends BlockEntityMachineBase
        implements MenuProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_COUNT = 12;
    public static final int MAX_HEAT = 50_000;

    public static final double ROD_SPEED = 0.04D;

    public static final float COOLING_PER_WATER = 0.07F;
    public static final float EXPLOSION_RADIUS = 18.0F;
    public static final float EXPLOSION_RADIATION = 50F;

    public static final double CONTROL_RANGE = 20D;

    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    private static final int[][] NEIGHBOURS = {
        {1, 5}, {0, 6}, {3, 7}, {2, 4, 8}, {3, 9}, {0, 6, 10},
        {1, 5, 11}, {2, 8}, {3, 7, 9}, {4, 8}, {5, 11}, {6, 10}
    };

    @SyncField(units = 1L << 2)
    public double controlLevel;

    @SyncField(units = 1L << 3)
    public double targetLevel;

    public double lastControlLevel;

    @SyncField(units = 1L << 0)
    @ContainerSync
    public int heat;

    @SyncField(units = 1L << 1)
    public byte water;

    @SyncField(units = 1L << 4)
    public int[] slotFlux = new int[SLOT_COUNT];

    @SyncField(units = 1L << 5)
    @ContainerSync
    public int totalFlux;

    public BlockEntityReactorResearch(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_RESEARCH.get(), pos, state, SLOT_COUNT);
    }

    private static @Nullable ItemStack spentFor(ItemStack fuel) {
        return fuel.getItem() instanceof ItemPlateFuel plate ? plate.spent().hot() : null;
    }

    @Override
    public void tickClient() {
        lastControlLevel = controlLevel;
    }

    @Override
    public void tickServer() {
        rodControl();
        totalFlux = 0;

        if (controlLevel > 0) reaction();

        if (heat > 0) {
            water = countWater();

            if (water > 0) {

                heat = (int) (heat - heat * COOLING_PER_WATER * water / 12F);
            } else {
                heat -= 1;
            }

            if (heat < 0) heat = 0;
        }

        if (heat > MAX_HEAT) {
            explode();
            return;
        }

        if (controlLevel > 0 && heat > 0 && !isShielded()) {
            double rad = (double) heat / MAX_HEAT * 50D;
            RadiationSystemNT.incrementRad((ServerLevel) getLevel(), worldPosition, rad);
        }

        networkPackNT(150);
    }

    private void rodControl() {
        if (controlLevel < targetLevel) {
            controlLevel += ROD_SPEED;
            if (controlLevel >= targetLevel) controlLevel = targetLevel;
        } else if (controlLevel > targetLevel) {
            controlLevel -= ROD_SPEED;
            if (controlLevel <= targetLevel) controlLevel = targetLevel;
        }
    }

    private void reaction() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.get(i);

            if (stack.isEmpty()) {
                slotFlux[i] = 0;
                continue;
            }

            if (stack.getItem() instanceof ItemPlateFuel plate) {
                int outFlux = plate.react(stack, slotFlux[i]);
                heat += outFlux * 2;
                slotFlux[i] = 0;
                totalFlux += outFlux;

                if (ItemPlateFuel.getLifeTime(stack) > plate.lifeTime) {
                    ItemStack spent = spentFor(stack);
                    inventory.set(i, spent == null ? ItemStack.EMPTY : spent);
                }

                for (int neighbour : NEIGHBOURS[i])
                    slotFlux[neighbour] += (int) (outFlux * controlLevel);
                continue;
            }

            ItemSwordMeteorite.upgrade(
                    inventory,
                    i,
                    ModItems.METEORITE_SWORD_BRED,
                    ModItems.METEORITE_SWORD_IRRADIATED);
            slotFlux[i] = 0;
        }
    }

    public byte countWater() {
        byte count = 0;

        for (Direction dir : Direction.VALUES) {
            if (dir.getAxis() == Direction.Axis.Y) {
                if (isWater(worldPosition.offset(0, 1 + dir.getStepY() * 2, 0))) count++;
            } else {
                for (int i = 0; i < 3; i++) {
                    if (isWater(worldPosition.relative(dir).above(i))) count++;
                }
            }
        }

        return count;
    }

    private boolean isWater(BlockPos pos) {
        return getLevel().getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

    public boolean isSubmerged() {
        return isWater(worldPosition.offset(1, 1, 0))
                || isWater(worldPosition.offset(0, 1, 1))
                || isWater(worldPosition.offset(-1, 1, 0))
                || isWater(worldPosition.offset(0, 1, -1));
    }

    private boolean isShielded() {
        return blocksRad(worldPosition.offset(1, 1, 0))
                && blocksRad(worldPosition.offset(-1, 1, 0))
                && blocksRad(worldPosition.offset(0, 1, 1))
                && blocksRad(worldPosition.offset(0, 1, -1));
    }

    private boolean blocksRad(BlockPos pos) {
        Level level = getLevel();
        BlockState state = level.getBlockState(pos);

        if (state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource()) {
            return true;
        }

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        Block block = (core == null ? state : level.getBlockState(core)).getBlock();
        if (block == ModBlocks.BLOCK_LEAD.get()
                || block == ModBlocks.BLOCK_DESH.get()
                || block == ModBlocks.REACTOR_RESEARCH.get()
                || block == ModBlocks.MACHINE_REACTOR_BREEDING.get()) {
            return true;
        }

        return block.getExplosionResistance() >= 100F;
    }

    private void explode() {
        Level world = getLevel();
        inventory.clear();

        world.removeBlock(worldPosition, false);

        for (Direction dir : Direction.VALUES) {
            if (dir.getAxis() == Direction.Axis.Y) {
                BlockPos pos = worldPosition.offset(0, 1 + dir.getStepY() * 2, 0);
                if (isWater(pos)) world.removeBlock(pos, false);
            } else {
                for (int i = 0; i < 3; i++) {
                    BlockPos pos = worldPosition.relative(dir).above(i);
                    if (isWater(pos)) world.removeBlock(pos, false);
                }
            }
        }

        world.explode(
                null,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                EXPLOSION_RADIUS,
                Level.ExplosionInteraction.BLOCK);

        Block deco = ModBlocks.DECO_STEEL.get();
        world.setBlockAndUpdate(worldPosition, deco.defaultBlockState());
        world.setBlockAndUpdate(worldPosition.above(), ModBlocks.CORIUM.get().defaultBlockState());
        world.setBlockAndUpdate(worldPosition.above(2), deco.defaultBlockState());

        RadiationSystemNT.incrementRad((ServerLevel) world, worldPosition, EXPLOSION_RADIATION);
        BossSpawnHandler.markRad(
                (ServerLevel) world,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5);
    }

    public int[] getDisplayData() {
        return new int[] {totalFlux, (int) Math.round(heat * 0.00002D * 980D + 20D)};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < CONTROL_RANGE * CONTROL_RANGE;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        data.getDouble("level").ifPresent(v -> targetLevel = v);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof ItemPlateFuel;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorResearch");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuReactorResearch(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = input.getIntOr("heat", 0);
        water = (byte) input.getIntOr("water", 0);
        controlLevel = input.getDoubleOr("level", 0D);
        targetLevel = input.getDoubleOr("targetLevel", 0D);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putInt("water", water);
        output.putDouble("level", controlLevel);
        output.putDouble("targetLevel", targetLevel);
    }

    private void writeSlotFlux(ByteBuf output) {
        for (int value : slotFlux) output.writeInt(value);
    }

    private void readSlotFlux(ByteBuf input) {
        for (int i = 0; i < SLOT_COUNT; i++) slotFlux[i] = input.readInt();
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.heat);
            case 1 -> output.writeByte(this.water);
            case 2 -> output.writeDouble(this.controlLevel);
            case 3 -> output.writeDouble(this.targetLevel);
            case 4 -> writeSlotFlux(output);
            case 5 -> output.writeInt(this.totalFlux);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.heat = input.readInt();
            case 1 -> this.water = input.readByte();
            case 2 -> this.controlLevel = input.readDouble();
            case 3 -> this.targetLevel = input.readDouble();
            case 4 -> readSlotFlux(input);
            case 5 -> this.totalFlux = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
