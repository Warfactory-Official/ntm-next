// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuPADetector;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipe;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.items.ModItems;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.Particle;
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

public class BlockEntityPADetector extends BlockEntityCooledBase
        implements MenuProvider, IParticleUser, IRORValueProvider {
    public static final String[] ROR = {
        PREFIX_VALUE + "temperature", PREFIX_VALUE + "pfmcold", PREFIX_VALUE + "pfm"
    };
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

    public BlockEntityPADetector(BlockPos pos, BlockState state) {
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
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);
        super.tickServer();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
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
        return BlockMultiblockCore.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, BlockPos pos) {
        Direction detectorDir = detectorDir();
        return worldPosition.relative(detectorDir, -4).equals(pos) && detectorDir == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
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
        this.power -= usage;

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
            SatelliteDetector.reportEvent(
                    server,
                    SatelliteDetector.DURATION_MEDIUM,
                    SatelliteDetector.BurstIntensity.MEDIUM,
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
    public @Nullable BlockPos getExitPos(Particle particle) {
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "temperature").equals(name)) return Integer.toString((int) temperature);
        if ((PREFIX_VALUE + "pfmcold").equals(name))
            return Integer.toString(coolantTanks[0].getFill());
        if ((PREFIX_VALUE + "pfm").equals(name)) return Integer.toString(coolantTanks[1].getFill());
        return null;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPADetector(containerId, playerInventory, this);
    }
}
