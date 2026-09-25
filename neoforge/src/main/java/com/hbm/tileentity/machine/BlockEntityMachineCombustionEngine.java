// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineCombustionEngine;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.items.ItemPistons.EnumPistonType;
import com.hbm.items.ItemPistons;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

public class BlockEntityMachineCombustionEngine extends BlockEntityMachinePolluting
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IControlReceiver,
                IFluidCopiable,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "throttle",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "efficiency",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "throttle"
            };
    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_PISTON = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    public static final int SLOT_COUNT = 5;

    public static final long maxPower = 2_500_000L;
    public static final int FLUID_CAP = 24_000;
    public static @Nullable Consumer<BlockEntityMachineCombustionEngine> CLIENT_SOUND;

    @SyncField(units = 1L << 5)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.DIESEL, FLUID_CAP);

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 2)
    public long power;

    @SyncField(units = 1L << 3)
    public boolean isOn = false;

    @SyncField(units = 1L << 1)
    public int setting = 0;

    @SyncField(units = 1L << 4)
    public boolean wasOn = false;

    public int tenth = 0;

    @SyncField(units = 1L << 0)
    public int playersUsing = 0;

    public float doorAngle = 0;
    public float prevDoorAngle = 0;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineCombustionEngine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_ENGINE.get(), pos, state, SLOT_COUNT, 50);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory)) setChanged();

        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) {
            tenth = 0;
            setChanged();
        }

        wasOn = false;

        int fill = tank.getFill() * 10 + tenth;
        ItemStack pistonStack = inventory.get(SLOT_PISTON);
        FT_Combustible trait =
                NTMFluidProperties.getTrait(tank.getTankType(), FT_Combustible.class);

        if (isOn
                && setting > 0
                && pistonStack.getItem() instanceof ItemPistons pistons
                && fill > 0
                && trait != null) {
            EnumPistonType piston = pistons.type;
            double eff = piston.eff[trait.getGrade().ordinal()];

            if (eff > 0) {
                int speed = setting * 2;
                int toBurn = Math.min(fill, speed);

                this.power += toBurn * (trait.getCombustionEnergy() / 10_000D) * eff;
                fill -= toBurn;

                if (TickPhase.every(this, 5) && toBurn > 0) {
                    pollute(tank.getTankType(), FluidReleaseType.BURN, toBurn * 0.5F);
                }

                if (toBurn > 0) wasOn = true;

                tank.setFill(fill / 10);
                tenth = fill % 10;
            }
        }

        this.power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        flush.provide((ServerLevel) level, this);

        if (power > maxPower) power = maxPower;

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = (doorAngle / 10F) + 3;
        if (playersUsing > 0) this.doorAngle += swingSpeed;
        else this.doorAngle -= swingSpeed;
        this.doorAngle = Mth.clamp(this.doorAngle, 0F, 135F);
    }

    public void openInventory() {
        if (level != null && !level.isClientSide()) {
            playersUsing++;
            setChanged();
        }
    }

    public void closeInventory() {
        if (level != null && !level.isClientSide()) {
            playersUsing--;
            setChanged();
        }
    }

    public double getEfficiency() {
        ItemStack pistonStack = inventory.get(SLOT_PISTON);
        FT_Combustible trait =
                NTMFluidProperties.getTrait(tank.getTankType(), FT_Combustible.class);
        if (!(pistonStack.getItem() instanceof ItemPistons pistons) || trait == null) return 0;
        return pistons.type.eff[trait.getGrade().ordinal()];
    }

    @Override
    public AudioWrapper createAudioLoop() {

        return AudioSystem.getLoopedSound(
                ModSounds.IGENERATOR_OPERATE.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F,
                20);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 25 * 25;
    }

    @Override
    public void receiveControl(CompoundTag data) {

        if (data.contains("turnOn")) isOn = !isOn;
        if (data.contains("setting")) setting = Mth.clamp(data.getIntOr("setting", setting), 0, 30);
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "state").equals(name)) return "" + (isOn ? 1 : 0);
        if ((PREFIX_VALUE + "throttle").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "efficiency").equals(name))
            return "" + (int) Math.round(getEfficiency() * 100);
        return null;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            try {
                isOn = Integer.parseInt(params[0]) == 1;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            try {
                setting = Mth.clamp(Integer.parseInt(params[0]), 0, 30);
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        return null;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_PISTON -> stack.getItem() instanceof ItemPistons;
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineCombustionEngine(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("setting").ifPresent(v -> setting = v);
        input.getLong("power").ifPresent(v -> power = v);
        isOn = input.getBooleanOr("isOn", isOn);
        input.getInt("tenth").ifPresent(v -> tenth = v);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("setting", setting);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putInt("tenth", tenth);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.playersUsing);
            case 1 -> output.writeInt(this.setting);
            case 2 -> output.writeLong(this.power);
            case 3 -> output.writeBoolean(this.isOn);
            case 4 -> output.writeBoolean(this.wasOn);
            case 5 -> this.tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.playersUsing = input.readInt();
            case 1 -> this.setting = input.readInt();
            case 2 -> this.power = input.readLong();
            case 3 -> this.isOn = input.readBoolean();
            case 4 -> this.wasOn = input.readBoolean();
            case 5 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
