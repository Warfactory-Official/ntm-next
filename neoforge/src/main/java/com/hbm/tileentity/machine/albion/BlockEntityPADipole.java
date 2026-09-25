// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuPADipole;
import com.hbm.items.machine.ItemPACoil.EnumCoilType;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.packet.SyncField;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.BlockEntityPASource.Particle;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityPADipole extends BlockEntityCooledBase
        implements MenuProvider,
                IControlReceiver,
                IParticleUser,
                IRORInteractive,
                IRORValueProvider {
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "temperature",
                PREFIX_VALUE + "pfmcold",
                PREFIX_VALUE + "pfm",
                PREFIX_FUNCTION + "setthreshold" + NAME_SEPARATOR + "threshold",
            };
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COIL = 1;
    public static final int SLOT_COUNT = 2;

    public static final long usage = 100_000;

    @SyncField(units = 1L << 3)
    public int dirLower;

    @SyncField(units = 1L << 4)
    public int dirUpper;

    @SyncField(units = 1L << 5)
    public int dirRedstone;

    @SyncField(units = 1L << 6)
    public int threshold;

    private boolean redstonePowered;

    public BlockEntityPADipole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DIPOLE.get(), pos, state, SLOT_COUNT);
    }

    public static Direction ditToDirection(int dir) {
        if (dir == 1) return Direction.EAST;
        if (dir == 2) return Direction.SOUTH;
        if (dir == 3) return Direction.WEST;
        return Direction.NORTH;
    }

    @Override
    public long getMaxPower() {
        return 2_500_000;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.paDipole");
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, BlockPos pos) {
        return worldPosition.getY() == pos.getY()
                && (worldPosition.getX() == pos.getX() || worldPosition.getZ() == pos.getZ());
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        @Nullable EnumCoilType type = ItemPACoil.typeOf(inventory.get(SLOT_COIL));
        boolean isInline = dir == getExitDir(particle);

        int mult = 1;
        if (type != null) {
            if (type.diMin > particle.momentum) mult *= 10;
            if (type.diDistMin > particle.distanceTraveled) mult *= 10;
            if (isInline) mult = 1;
        }

        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (this.power < usage * mult) particle.crash(PAState.CRASH_NOPOWER);
        if (type == null) particle.crash(PAState.CRASH_NOCOIL);
        if (type != null && type.diMax < particle.momentum && !isInline)
            particle.crash(PAState.CRASH_OVERSPEED);

        if (particle.invalid) return;

        if (isInline) {
            particle.addDistance(3);
        } else {
            particle.resetDistance();
        }

        this.power -= usage * mult;
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        particle.dir = getExitDir(particle);
        return worldPosition.relative(particle.dir, 2);
    }

    public Direction getExitDir(Particle particle) {
        int dit =
                particle.momentum < this.threshold
                        ? dirLower
                        : checkRedstone() ? dirRedstone : dirUpper;
        return ditToDirection(dit);
    }

    public boolean checkRedstone() {
        return redstonePowered;
    }

    public void refreshRedstone() {
        boolean powered = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos arm = worldPosition.relative(side);
            if (level.hasNeighborSignal(arm.above()) || level.hasNeighborSignal(arm.below())) {
                powered = true;
                break;
            }
        }
        redstonePowered = powered;
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
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("lower")) this.dirLower++;
        if (data.contains("upper")) this.dirUpper++;
        if (data.contains("redstone")) this.dirRedstone++;
        if (data.contains("threshold")) this.threshold = data.getIntOr("threshold", 0);

        if (this.dirLower > 3) this.dirLower -= 4;
        if (this.dirUpper > 3) this.dirUpper -= 4;
        if (this.dirRedstone > 3) this.dirRedstone -= 4;

        this.threshold = Mth.clamp(threshold, 0, 999_999_999);
        setChanged();
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
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setthreshold").equals(name) && params.length > 0) {
            threshold = IRORInteractive.parseInt(params[0], 0, 999_999_999);
            setChanged();
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuPADipole(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        redstonePowered = input.getBooleanOr("redstone", false);
        dirLower = input.getIntOr("dirLower", 0);
        dirUpper = input.getIntOr("dirUpper", 0);
        dirRedstone = input.getIntOr("dirRedstone", 0);
        threshold = input.getIntOr("threshold", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("redstone", redstonePowered);
        output.putInt("dirLower", dirLower);
        output.putInt("dirUpper", dirUpper);
        output.putInt("dirRedstone", dirRedstone);
        output.putInt("threshold", threshold);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x78L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeInt(this.dirLower);
            case 4 -> output.writeInt(this.dirUpper);
            case 5 -> output.writeInt(this.dirRedstone);
            case 6 -> output.writeInt(this.threshold);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.dirLower = input.readInt();
            case 4 -> this.dirUpper = input.readInt();
            case 5 -> this.dirRedstone = input.readInt();
            case 6 -> this.threshold = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
