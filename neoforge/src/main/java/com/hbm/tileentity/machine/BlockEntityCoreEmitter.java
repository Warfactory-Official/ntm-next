// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ILaserable;
import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.CoreComponent;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuCoreEmitter;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityCoreEmitter extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                ILaserable,
                IGUIProvider,
                IControlReceiver,
                IRORInteractive,
                SyncUnitSchema {

    public static final String[] ROR =
            new String[] {
                PREFIX_FUNCTION + "setpower" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "toggle",
                PREFIX_FUNCTION + "switch" + NAME_SEPARATOR + "on/off",
            };
    public static final long MAX_POWER = 1_000_000_000L;
    public static final int RANGE = 50;
    public static final int TANK_CAPACITY = 64_000;

    public static final int CRYOGEL_PER_TICK = 20;

    private static final float INDESTRUCTIBLE = 6000F;

    @SyncField(units = 1L << 5)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.CRYOGEL, TANK_CAPACITY);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public int watts;

    @SyncField(units = 1L << 3)
    public int beam;

    public long joules;

    @SyncField(units = 1L << 4)
    public boolean isOn;

    @SyncField(units = 1L << 2)
    public long prev;

    public BlockEntityCoreEmitter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_EMITTER.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        watts = Math.clamp(watts, 1, 100);
        long demand = MAX_POWER * watts / 2000;

        beam = 0;

        if (joules > 0 || prev > 0) {
            if (tank.getFill() >= CRYOGEL_PER_TICK) {
                tank.setFill(tank.getFill() - CRYOGEL_PER_TICK);
            } else {
                level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
                return;
            }
        }

        if (isOn) {
            if (power >= demand) {
                power -= demand;
                joules += watts * 100L;
            }
            prev = joules;

            if (joules > 0) fire();
        } else {
            joules = 0;
            prev = 0;
        }

        setChanged();
        networkPackNT(250);
    }

    private void fire() {
        long out = joules * 95 / 100;
        Direction dir = getBlockState().getValue(CoreComponent.FACING);

        for (int i = 1; i <= RANGE; i++) {
            beam = i;
            BlockPos pos = worldPosition.relative(dir, i);
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof BlockEntityCore core) {
                out = core.burn(out);
                continue;
            }
            if (be instanceof ILaserable laserable) {
                laserable.addEnergy(level, pos, out, dir);
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            if (state.liquid()) {
                level.playSound(
                        null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.removeBlock(pos, false);
                break;
            }

            if (state.getBlock().getExplosionResistance() < INDESTRUCTIBLE
                    && level.getRandom().nextInt(20) == 0) {
                level.destroyBlock(pos, false);
            }
            break;
        }

        joules = 0;
        burnEntitiesAlongBeam(dir);
    }

    private void burnEntitiesAlongBeam(Direction dir) {
        BlockPos end = worldPosition.relative(dir, beam);
        AABB box =
                new AABB(
                        Math.min(worldPosition.getX(), end.getX()) + 0.2,
                        Math.min(worldPosition.getY(), end.getY()) + 0.2,
                        Math.min(worldPosition.getZ(), end.getZ()) + 0.2,
                        Math.max(worldPosition.getX(), end.getX()) + 0.8,
                        Math.max(worldPosition.getY(), end.getY()) + 0.8,
                        Math.max(worldPosition.getZ(), end.getZ()) + 0.8);

        ServerLevel server = (ServerLevel) level;
        DamageSource source = server.damageSources().source(ModDamageTypes.AMS_CORE);
        for (Entity e : server.getEntities((Entity) null, box, e -> true)) {
            e.hurtServer(server, source, 50);
            e.igniteForSeconds(10.0F);
        }
    }

    @Override
    public void addEnergy(Level level, BlockPos pos, long energy, Direction dir) {
        if (dir.getOpposite() != getBlockState().getValue(CoreComponent.FACING)) joules += energy;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[] {tank};
    }

    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("watts")) watts = Math.clamp(data.getIntOr("watts", watts), 1, 100);
        if (data.contains("toggle")) isOn = !isOn;
        markChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpower").equals(name) && params.length > 0) {
            watts = IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "toggle").equals(name)) {
            isOn = !isOn;
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "switch").equals(name) && params.length > 0) {
            if ("on".equals(params[0])) {
                isOn = true;
                setChanged();
                return null;
            }
            if ("off".equals(params[0])) {
                isOn = false;
                setChanged();
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        watts = input.getIntOr("watts", 0);
        joules = input.getLongOr("joules", 0L);
        prev = input.getLongOr("prev", 0L);
        isOn = input.getBooleanOr("isOn", false);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("watts", watts);
        output.putLong("joules", joules);
        output.putLong("prev", prev);
        output.putBoolean("isOn", isOn);
        tank.serialize(output.child("tank"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCoreEmitter(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcEmitter");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.watts);
            case 2 -> output.writeLong(this.prev);
            case 3 -> output.writeInt(this.beam);
            case 4 -> output.writeBoolean(this.isOn);
            case 5 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.watts = input.readInt();
            case 2 -> this.prev = input.readLong();
            case 3 -> this.beam = input.readInt();
            case 4 -> this.isOn = input.readBoolean();
            case 5 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
