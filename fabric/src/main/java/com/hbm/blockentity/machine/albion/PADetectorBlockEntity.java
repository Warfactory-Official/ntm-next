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

package com.hbm.blockentity.machine.albion;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.machine.albion.AcceleratorComponent;
import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.PAState;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.Particle;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.PADetectorMenu;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipe;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.items.ModItems;
import com.hbm.saveddata.satellites.RadiationBursts;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class PADetectorBlockEntity extends CooledAcceleratorBlockEntity
        implements MenuProvider, AcceleratorComponent, RadioValueProvider {
    public static final String[] ROR = {VAL + "temperature", VAL + "pfmcold", VAL + "pfm"};
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CONTAINER_1 = 1;
    public static final int SLOT_CONTAINER_2 = 2;
    public static final int SLOT_OUTPUT_1 = 3;
    public static final int SLOT_OUTPUT_2 = 4;
    public static final int SLOT_COUNT = 5;

    public static final long usage = 100_000;

    private static final int[] ACCESSIBLE_SLOTS = {
        SLOT_CONTAINER_1, SLOT_CONTAINER_2, SLOT_OUTPUT_1, SLOT_OUTPUT_2
    };

    public PADetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DETECTOR.get(), pos, state, SLOT_COUNT);
    }

    private static boolean isDigamma(@Nullable ItemStack stack) {
        return stack != null && stack.is(ModItems.PARTICLE_DIGAMMA.get());
    }

    private static boolean hasRemainder(ItemStack stack) {
        return stack.getCraftingRemainder() != null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paDetector");
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    @Override
    public void tickServer() {
        hbm$syncSet$CooledAcceleratorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));
        super.tickServer();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_CONTAINER_1, SLOT_CONTAINER_2 -> true;
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_CONTAINER_1 || slot == SLOT_CONTAINER_2;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    private Direction detectorDir() {
        return MultiblockCoreBlock.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canAcceptParticle(Particle particle, Direction dir, BlockPos pos) {
        Direction detectorDir = detectorDir();
        return worldPosition.relative(detectorDir, -4).equals(pos) && detectorDir == dir;
    }

    @Override
    public void onParticleEnter(Particle particle, Direction dir) {
        particle.invalid = true;
        if (particle.defocus > 0) {
            particle.crash(PAState.CRASH_DEFOCUS);
            return;
        }
        if (this.power < usage) {
            particle.crash(PAState.CRASH_NOPOWER);
            return;
        }
        if (!isCool()) {
            particle.crash(PAState.CRASH_NOCOOL);
            return;
        }
        this.hbm$syncSet$CooledAcceleratorBlockEntity$power((long) (this.power - (usage)));
        markChanged();

        for (ParticleAcceleratorRecipe recipe : ParticleAcceleratorRecipes.INSTANCE.recipes()) {
            if (!recipe.matchesRecipe(particle.input1, particle.input2)) continue;

            if (particle.momentum < recipe.momentum) {
                particle.crash(PAState.CRASH_UNDERSPEED);
                return;
            }

            ItemStack output1 = recipe.output1();
            ItemStack output2 = recipe.output2();

            if (canAccept(recipe)) {
                if (hasRemainder(output1)) removeItem(SLOT_CONTAINER_1, 1);
                if (output2 != null && hasRemainder(output2)) removeItem(SLOT_CONTAINER_2, 1);

                store(SLOT_OUTPUT_1, output1);
                if (output2 != null) store(SLOT_OUTPUT_2, output2);
            }

            ItemStack digamma =
                    isDigamma(output1) ? output1 : isDigamma(output2) ? output2 : ItemStack.EMPTY;
            if (!digamma.isEmpty() && level instanceof ServerLevel server) {
                AwardRegions.nearby(
                        server,
                        new AABB(
                                        worldPosition.getX() + 0.5,
                                        worldPosition.getY() + 0.5,
                                        worldPosition.getZ() + 0.5,
                                        worldPosition.getX() + 0.5,
                                        worldPosition.getY() + 0.5,
                                        worldPosition.getZ() + 0.5)
                                .inflate(100, 50, 100),
                        p -> HbmCriteria.particleProduced(p, digamma));
            }

            ServerLevel server = (ServerLevel) level;
            RadiationBursts.report(
                    server,
                    RadiationBursts.Intensity.MEDIUM,
                    worldPosition.getX(),
                    worldPosition.getZ());
            SatelliteRayEvents.report(
                    server, worldPosition, SatelliteRayEvents.HIGH_ENERGY_PARTICLES, 600);
            particle.crash(PAState.SUCCESS);
            return;
        }

        particle.crash(PAState.CRASH_NORECIPE);
    }

    private void store(int slot, ItemStack output) {
        ItemStack current = inventory.get(slot);
        if (current.isEmpty()) inventory.set(slot, output.copy());
        else current.grow(output.getCount());
        setChanged();
    }

    public boolean canAccept(ParticleAcceleratorRecipe recipe) {
        return checkSlot(recipe.output1(), SLOT_CONTAINER_1, SLOT_OUTPUT_1)
                && checkSlot(recipe.output2(), SLOT_CONTAINER_2, SLOT_OUTPUT_2);
    }

    public boolean checkSlot(@Nullable ItemStack output, int containerSlot, int outputSlot) {
        if (output != null) {
            ItemStack out = inventory.get(outputSlot);
            if (!out.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(out, output)) return false;
                if (out.getCount() + output.getCount() > output.getMaxStackSize()) return false;
            }
            ItemStackTemplate remainder = output.getCraftingRemainder();
            if (remainder != null) {
                return ItemStack.isSameItemSameComponents(
                        inventory.get(containerSlot), remainder.create());
            }
        }

        return true;
    }

    @Override
    public @Nullable BlockPos getParticleExit(Particle particle) {
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "temperature").equals(name)) return Integer.toString((int) temperature);
        if ((VAL + "pfmcold").equals(name)) return Integer.toString(coolantTanks[0].getFill());
        if ((VAL + "pfm").equals(name)) return Integer.toString(coolantTanks[1].getFill());
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PADetectorMenu(containerId, playerInventory, this);
    }
}
