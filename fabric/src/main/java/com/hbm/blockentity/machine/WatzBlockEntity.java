/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.port.CapabilityPortProvider;
import com.hbm.capability.port.FluidPort;
import com.hbm.entity.projectile.ShrapnelEntity;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.WatzMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.machine.WatzPelletItem.WatzFuel;
import com.hbm.items.machine.WatzPelletItem;
import com.hbm.items.special.Autogen;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ResponseFunction;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class WatzBlockEntity extends MachineBlockEntity
        implements CapabilityPortProvider,
                MenuProvider,
                RadioValueProvider,
                FluidSettingsHandler,
                FluidFlushSender,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 24;
    public static final int TANK_CAPACITY = 64_000;
    public static final double PASSIVE_COOLING = 0.99D;
    public static final double COOLING_FACTOR = 0.2D;
    public static final int SEGMENT_PITCH = 3;
    public static final List<StructureDrop> STRUCTURE_DROPS =
            List.of(
                    new StructureDrop(() -> ModBlocks.WATZ_END.get().asItem(), 48),
                    new StructureDrop(WatzBlockEntity::duraBolt, 64),
                    new StructureDrop(WatzBlockEntity::duraBolt, 64),
                    new StructureDrop(WatzBlockEntity::duraBolt, 64),
                    new StructureDrop(() -> ModBlocks.WATZ_ELEMENT.get().asItem(), 36),
                    new StructureDrop(() -> ModBlocks.WATZ_COOLER.get().asItem(), 26),
                    new StructureDrop(() -> ModBlocks.STRUCT_WATZ_CORE.get().asItem(), 1));
    public static final String[] ROR =
            new String[] {
                VAL + "heat", VAL + "flux", VAL + "mud", VAL + "coolant_hot", VAL + "coolant_cold",
            };
    private static final int[] ACCESSIBLE_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    public final FluidTankNTM[] tanks = new FluidTankNTM[3];
    public final FluidTankNTM[] sharedTanks = new FluidTankNTM[3];
    public final NonNullList<ItemStack> locks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidPort intake;
    private final FluidPort outlet;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sendingBottom;
    public int heat;
    public double fluxLastBase;
    public double fluxLastReaction;
    public double fluxDisplay;
    public boolean isOn;
    public boolean isLocked;

    {
        hbm$syncSet$WatzBlockEntity$isLocked(false);
    }

    public WatzBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ.get(), pos, state, SLOT_COUNT);
        this.tanks[0] = new FluidTankNTM(NTMFluids.COOLANT, TANK_CAPACITY);
        this.tanks[1] = new FluidTankNTM(NTMFluids.COOLANT_HOT, TANK_CAPACITY);
        this.tanks[2] = new FluidTankNTM(NTMFluids.WATZ_MUD, TANK_CAPACITY);
        for (int i = 0; i < 3; i++) {
            this.hbm$syncArraySet$WatzBlockEntity$sharedTanks(
                    this.sharedTanks,
                    i,
                    new FluidTankNTM(this.tanks[i].getTankType(), TANK_CAPACITY));
        }
        this.intake = FluidPort.of(tanks, 1, 0, this::markChanged);
        this.outlet = FluidPort.of(tanks, 1, (1 << 1) | (1 << 2), this::markChanged);
        this.sendingBottom = new FluidTankNTM[] {tanks[1], tanks[2]};
    }

    private static void addMud(FluidTankNTM pooled, int amount, int[] mudOverflow) {
        int before = pooled.getFill();
        pooled.setFill(before + amount);
        mudOverflow[0] += amount - (pooled.getFill() - before);
    }

    private static Item duraBolt() {
        return Autogen.require(MaterialShape.BOLT, Materials.DURA_STEEL);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.watzPowerplant");
    }

    protected void resetSharedTanks() {
        for (int i = 0; i < 3; i++) {
            sharedTanks[i].setTankType(tanks[i].getTankType());
            sharedTanks[i].changeTankSize(TANK_CAPACITY);
            sharedTanks[i].setFill(tanks[i].getFill());
        }
    }

    @Override
    public void tickServer() {
        resetSharedTanks();

        if (updateLock()) return;

        boolean turnedOn = pumpAbove && level.getSignal(worldPosition.above(5), Direction.DOWN) > 0;

        List<WatzBlockEntity> segments = new ArrayList<>();
        segments.add(this);

        for (int y = worldPosition.getY() - SEGMENT_PITCH;
                y >= level.getMinY();
                y -= SEGMENT_PITCH) {
            BlockPos below = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (level.getBlockEntity(below) instanceof WatzBlockEntity segment) {
                segments.add(segment);
            } else {
                break;
            }
        }

        FluidTankNTM[] pooled = new FluidTankNTM[3];
        for (int i = 0; i < 3; i++) pooled[i] = new FluidTankNTM(tanks[i].getTankType(), 0);

        for (WatzBlockEntity segment : segments) {
            segment.setupCoolant();
            for (int i = 0; i < 3; i++) {
                pooled[i].changeTankSize(pooled[i].getMaxFill() + segment.tanks[i].getMaxFill());
                pooled[i].setFill(pooled[i].getFill() + segment.tanks[i].getFill());
            }
        }

        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).updateCoolant(pooled);
        }

        int[] mudOverflow = new int[1];
        this.updateReaction(null, pooled, turnedOn, mudOverflow);
        for (int i = 1; i < segments.size(); i++) {
            segments.get(i).updateReaction(segments.get(i - 1), pooled, turnedOn, mudOverflow);
        }

        for (WatzBlockEntity segment : segments) {
            for (int i = 0; i < 3; i++) {
                segment.sharedTanks[i].setTankType(pooled[i].getTankType());
                segment.sharedTanks[i].changeTankSize(pooled[i].getMaxFill());
                segment.sharedTanks[i].setFill(pooled[i].getFill());
            }
            segment.hbm$syncSet$WatzBlockEntity$isOn(turnedOn);
            segment.networkPackNT(25);
            segment.hbm$syncSet$WatzBlockEntity$heat((int) (segment.heat * (PASSIVE_COOLING)));
            segment.markChanged();
        }

        int[] remaining = {pooled[0].getFill(), pooled[1].getFill(), pooled[2].getFill()};
        for (int i = segments.size() - 1; i >= 0; i--) {
            WatzBlockEntity segment = segments.get(i);
            for (int j = 0; j < 3; j++) {
                int min = Math.min(segment.tanks[j].getMaxFill(), remaining[j]);
                remaining[j] -= min;
                segment.tanks[j].setFill(min);
            }
        }

        segments.get(segments.size() - 1).sendOutBottom((ServerLevel) level);

        if (remaining[2] > 0 || mudOverflow[0] > 0) meltdown();
    }

    private void sendOutBottom(ServerLevel level) {
        flush.provide(level, this);
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sendingBottom;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : sendingBottom) {
            out.add(outlet, tank, FlushFaces.activePlane((cell, side) -> side == Direction.DOWN));
        }
    }

    public void setupCoolant() {
        tanks[0].setTankType(NTMFluids.COOLANT);
        tanks[1].setTankType(
                NTMFluidProperties.getTrait(NTMFluids.COOLANT, HeatableTrait.class)
                        .first()
                        .product());
    }

    public void updateCoolant(FluidTankNTM[] tanks) {

        double heatToUse = this.heat * COOLING_FACTOR;

        HeatableTrait trait =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), HeatableTrait.class);
        HeatableTrait.Step step = trait.first();

        int heatCycles = (int) (heatToUse / step.heat());
        int coolCycles = tanks[0].getFill() / step.consumed();
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.produced();

        int cycles = Math.min(heatCycles, Math.min(hotCycles, coolCycles));
        this.hbm$syncSet$WatzBlockEntity$heat((int) (this.heat - (cycles * step.heat())));
        tanks[0].setFill(tanks[0].getFill() - cycles * step.consumed());
        tanks[1].setFill(tanks[1].getFill() + cycles * step.produced());
    }

    public void updateReaction(
            @Nullable WatzBlockEntity above,
            FluidTankNTM[] tanks,
            boolean turnedOn,
            int[] mudOverflow) {

        if (turnedOn) {
            List<ItemStack> pellets = new ArrayList<>();

            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = inventory.get(i);
                if (WatzPelletItem.isFresh(stack)) pellets.add(stack);
            }

            double baseFlux = 0D;

            for (ItemStack stack : pellets) {
                baseFlux += WatzPelletItem.typeOf(stack).passive();
            }

            double inputFlux = baseFlux + fluxLastReaction;
            double addedFlux = 0D;
            double addedHeat = 0D;

            for (ItemStack stack : pellets) {
                WatzFuel type = WatzPelletItem.typeOf(stack);
                ResponseFunction burnFunc = type.burnFunc();
                ResponseFunction heatDiv = type.heatDiv();

                if (burnFunc != null) {
                    double div = heatDiv != null ? heatDiv.apply(heat) : 1D;
                    double burn = burnFunc.apply(inputFlux) / div;
                    WatzPelletItem.setYield(stack, WatzPelletItem.getYield(stack) - burn);
                    addedFlux += burn;
                    addedHeat += type.heatEmission() * burn;
                    addMud(tanks[2], (int) Math.round(type.mudContent() * burn), mudOverflow);
                }
            }

            for (ItemStack stack : pellets) {
                WatzFuel type = WatzPelletItem.typeOf(stack);
                ResponseFunction absorbFunc = type.absorbFunc();

                if (absorbFunc != null) {
                    double absorb = absorbFunc.apply(baseFlux + fluxLastReaction);
                    addedHeat += absorb;
                    WatzPelletItem.setYield(stack, WatzPelletItem.getYield(stack) - absorb);
                    addMud(tanks[2], (int) Math.round(type.mudContent() * absorb), mudOverflow);
                }
            }

            this.hbm$syncSet$WatzBlockEntity$heat((int) (this.heat + (addedHeat)));
            this.hbm$syncSet$WatzBlockEntity$fluxLastBase(baseFlux);
            this.hbm$syncSet$WatzBlockEntity$fluxLastReaction(addedFlux);

        } else {
            this.hbm$syncSet$WatzBlockEntity$fluxLastBase(0);
            this.hbm$syncSet$WatzBlockEntity$fluxLastReaction(0);
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.get(i);

            if (WatzPelletItem.isFresh(stack) && WatzPelletItem.getEnrichment(stack) <= 0) {
                inventory.set(i, WatzPelletItem.typeOf(stack).depletedStack());
            }
        }

        if (above != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stackBottom = inventory.get(i);
                ItemStack stackTop = above.inventory.get(i);

                if (stackBottom.isEmpty() && !stackTop.isEmpty()) {
                    inventory.set(i, stackTop.copy());
                    above.inventory.set(i, ItemStack.EMPTY);
                }

                if (WatzPelletItem.isFresh(stackBottom) && WatzPelletItem.isDepleted(stackTop)) {
                    ItemStack buf = stackTop.copy();
                    above.inventory.set(i, stackBottom.copy());
                    inventory.set(i, buf);
                }
            }
        }
    }

    public boolean updateLock() {
        return level.getBlockEntity(worldPosition.above(SEGMENT_PITCH)) instanceof WatzBlockEntity;
    }

    private void meltdown() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        MultiblockCoreBlock.withoutTeardown(
                () -> {
                    for (int ox = -3; ox <= 3; ox++) {
                        for (int oy = 3; oy < 6; oy++) {
                            for (int oz = -3; oz <= 3; oz++) {
                                level.setBlock(
                                        cursor.set(x + ox, y + oy, z + oz),
                                        Blocks.AIR.defaultBlockState(),
                                        3);
                            }
                        }
                    }
                    disassemble();
                });

        if (level instanceof ServerLevel server) {
            RadiationSystemNT.incrementRad(server, worldPosition.above(), 1_000D, 1_000D * 1024D);
        }

        WorldSounds.machine(
                level,
                null,
                x + 0.5,
                y + 2,
                z + 0.5,
                ModSounds.RBMK_EXPLOSION.get(),
                SoundSource.BLOCKS,
                50.0F,
                1.0F);
        ExplosionCreator.composeEffectRBMKMush(level, x + 0.5, y + 2, z + 0.5, 5F);
    }

    private void disassemble() {
        int count = 20;
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count * 5; i++) {
            ShrapnelEntity shrapnel =
                    new ShrapnelEntity(
                            level,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 3,
                            worldPosition.getZ() + 0.5);
            shrapnel.setDeltaMovement(
                    rand.nextGaussian() * 1 * (1 + (count / 100)),
                    ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15 + rand.nextInt(21))))
                            + (rand.nextFloat() / 50 * count),
                    rand.nextGaussian() * 1 * (1 + (count / 100)));
            shrapnel.setTrail(ShrapnelEntity.TRAIL_WATZ);
            level.addFreshEntity(shrapnel);
        }

        dropStructure();

        Block mud = ModBlocks.MUD_BLOCK.get();
        for (int i = 0; i < 3; i++) {
            level.setBlock(worldPosition.above(i), mud.defaultBlockState(), 3);
        }

        Block element = ModBlocks.WATZ_ELEMENT.get();
        Block cooler = ModBlocks.WATZ_COOLER.get();
        Block end = ModBlocks.WATZ_END_BOLTED.get();

        setBrokenColumn(0, element, 1, 0);
        setBrokenColumn(0, element, 2, 0);
        setBrokenColumn(0, element, 0, 1);
        setBrokenColumn(0, element, 0, 2);
        setBrokenColumn(0, element, -1, 0);
        setBrokenColumn(0, element, -2, 0);
        setBrokenColumn(0, element, 0, -1);
        setBrokenColumn(0, element, 0, -2);
        setBrokenColumn(0, element, 1, 1);
        setBrokenColumn(0, element, 1, -1);
        setBrokenColumn(0, element, -1, 1);
        setBrokenColumn(0, element, -1, -1);
        setBrokenColumn(0, cooler, 2, 1);
        setBrokenColumn(0, cooler, 2, -1);
        setBrokenColumn(0, cooler, 1, 2);
        setBrokenColumn(0, cooler, -1, 2);
        setBrokenColumn(0, cooler, -2, 1);
        setBrokenColumn(0, cooler, -2, -1);
        setBrokenColumn(0, cooler, 1, -2);
        setBrokenColumn(0, cooler, -1, -2);

        for (int j = -1; j < 2; j++) {
            setBrokenColumn(1, end, 3, j);
            setBrokenColumn(1, end, j, 3);
            setBrokenColumn(1, end, -3, j);
            setBrokenColumn(1, end, j, -3);
        }
        setBrokenColumn(1, end, 2, 2);
        setBrokenColumn(1, end, 2, -2);
        setBrokenColumn(1, end, -2, 2);
        setBrokenColumn(1, end, -2, -2);

        if (level instanceof ServerLevel server) {
            AwardRegions.within(
                    server,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    50,
                    p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.WATZ));
        }
    }

    private void dropStructure() {
        for (StructureDrop drop : STRUCTURE_DROPS) {
            Block.popResource(level, worldPosition, new ItemStack(drop.item().get(), drop.count()));
        }
    }

    private void setBrokenColumn(int minHeight, Block block, int x, int z) {
        int height = minHeight + level.getRandom().nextInt(3 - minHeight);

        for (int i = 0; i < 3; i++) {
            BlockPos pos = worldPosition.offset(x, i, z);
            level.setBlock(
                    pos,
                    i <= height
                            ? block.defaultBlockState()
                            : ModBlocks.MUD_BLOCK.get().defaultBlockState(),
                    3);
        }
    }

    public void toggleLock() {
        if (isLocked) {
            locks.clear();
        } else {
            for (int i = 0; i < inventory.size(); i++) locks.set(i, inventory.get(i).copy());
        }
        hbm$syncSet$WatzBlockEntity$isLocked(!isLocked);
        markChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!WatzPelletItem.isFresh(stack)) return false;
        if (!this.isLocked) return true;
        ItemStack lock = locks.get(slot);
        return !lock.isEmpty() && lock.getItem() == stack.getItem();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !WatzPelletItem.isFresh(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public FluidPort fluidAccess(BlockPos cell, Direction side) {
        return side == Direction.DOWN && cell.getY() == worldPosition.getY() ? outlet : intake;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new WatzMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "heat").equals(name)) return "" + this.heat;
        if ((VAL + "flux").equals(name))
            return "" + (int) (this.fluxLastBase + this.fluxLastReaction);
        if ((VAL + "mud").equals(name)) return "" + this.tanks[2].getFill();
        if ((VAL + "coolant_hot").equals(name)) return "" + this.tanks[1].getFill();
        if ((VAL + "coolant_cold").equals(name)) return "" + this.tanks[0].getFill();
        return null;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pumpAbove = input.getBooleanOr("pumpAbove", false);
        for (int i = 0; i < tanks.length; i++) {
            int slot = i;
            input.child("t" + i).ifPresent(tanks[slot]::deserialize);
        }
        input.child("locks").ifPresent(in -> ContainerHelper.loadAllItems(in, locks));
        this.hbm$syncSet$WatzBlockEntity$heat(input.getIntOr("heat", heat));
        this.hbm$syncSet$WatzBlockEntity$fluxLastBase(input.getDoubleOr("lastFluxB", fluxLastBase));
        this.hbm$syncSet$WatzBlockEntity$fluxLastReaction(
                input.getDoubleOr("lastFluxR", fluxLastReaction));
        this.hbm$syncSet$WatzBlockEntity$isLocked(input.getBooleanOr("isLocked", isLocked));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("pumpAbove", pumpAbove);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        ContainerHelper.saveAllItems(output.child("locks"), locks);
        output.putInt("heat", this.heat);
        output.putDouble("lastFluxB", fluxLastBase);
        output.putDouble("lastFluxR", fluxLastReaction);
        output.putBoolean("isLocked", isLocked);
    }

    public record StructureDrop(Supplier<Item> item, int count) {}

    private boolean pumpAbove;

    public void refreshPumpAbove() {
        pumpAbove =
                level.getBlockState(worldPosition.above(SEGMENT_PITCH))
                        .is(ModBlocks.WATZ_PUMP.get());
    }

    private void writeFlux(ByteBuf output) {
        output.writeDouble(fluxLastReaction + fluxLastBase);
    }

    private void readFlux(ByteBuf input) {
        fluxDisplay = input.readDouble();
    }

    private void writeSharedTanks(ByteBuf output) {
        for (FluidTankNTM tank : sharedTanks) tank.packetSerialize(output);
    }

    private void readSharedTanks(ByteBuf input) {
        for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.heat);
            case 1 -> output.writeBoolean(this.isOn);
            case 2 -> output.writeBoolean(this.isLocked);
            case 3 -> writeFlux(output);
            case 4 -> writeSharedTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$WatzBlockEntity$heat(input.readInt());
            case 1 -> this.hbm$syncSet$WatzBlockEntity$isOn(input.readBoolean());
            case 2 -> this.hbm$syncSet$WatzBlockEntity$isLocked(input.readBoolean());
            case 3 -> readFlux(input);
            case 4 -> readSharedTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$WatzBlockEntity$sharedTanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.sharedTanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.sharedTanks) {
                syncArrayChanged(array, index, 3, 1L << 4);
            }
        }
        return next;
    }

    public final void hbm$syncSet$WatzBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$WatzBlockEntity$fluxLastBase(double next) {
        if (Double.doubleToRawLongBits(this.fluxLastBase) != Double.doubleToRawLongBits(next)) {
            this.fluxLastBase = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$WatzBlockEntity$fluxLastReaction(double next) {
        if (Double.doubleToRawLongBits(this.fluxLastReaction) != Double.doubleToRawLongBits(next)) {
            this.fluxLastReaction = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$WatzBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$WatzBlockEntity$isLocked(boolean next) {
        if (this.isLocked != next) {
            this.isLocked = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(WatzBlockEntity.class, "sharedTanks", 3, 1L << 4);
    }
}
