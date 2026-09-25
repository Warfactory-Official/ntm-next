// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.MenuPAQuadrupole;
import com.hbm.items.machine.ItemPACoil.EnumCoilType;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityPAQuadrupole extends BlockEntityCooledBase
        implements MenuProvider, IParticleUser, IRORValueProvider {
    public static final String[] ROR = {
        PREFIX_VALUE + "temperature", PREFIX_VALUE + "pfmcold", PREFIX_VALUE + "pfm"
    };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COIL = 1;
    public static final int SLOT_COUNT = 2;

    public static final long usage = 100_000;
    public static final int focusGain = 100;

    public BlockEntityPAQuadrupole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_QUADRUPOLE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public long getMaxPower() {
        return 2_500_000;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paQuadrupole");
    }

    private Direction beamlineDir() {
        return BlockMultiblockCore.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, BlockPos pos) {
        Direction beamlineDir = beamlineDir();
        return worldPosition.relative(beamlineDir, -1).equals(pos) && beamlineDir == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        @Nullable EnumCoilType type = ItemPACoil.typeOf(inventory.get(SLOT_COIL));

        int mult = 1;
        if (type != null) mult = type.quadMin > particle.momentum ? 10 : 1;

        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (this.power < usage * mult) particle.crash(PAState.CRASH_NOPOWER);
        if (type == null) particle.crash(PAState.CRASH_NOCOIL);
        if (type != null && type.quadMax < particle.momentum)
            particle.crash(PAState.CRASH_OVERSPEED);

        if (particle.invalid) return;

        particle.addDistance(3);
        particle.focus(focusGain);
        this.power -= usage * mult;
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        return worldPosition.relative(beamlineDir(), 2);
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
            case SLOT_COIL -> stack.getItem() instanceof ItemPACoil;
            default -> false;
        };
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
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
        return new MenuPAQuadrupole(containerId, playerInventory, this);
    }
}
