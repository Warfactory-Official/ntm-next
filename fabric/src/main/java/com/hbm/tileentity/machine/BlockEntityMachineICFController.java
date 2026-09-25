// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.MachineICFController;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.data.MachineData;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineICFController extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IEnergyHandlerMK2, SyncUnitSchema {

    private static final int MAX_LASER_LENGTH = 50;

    private static final float INDESTRUCTIBLE_RESISTANCE = 6000F;
    private static final float BEAM_DAMAGE = 50F;
    private static final int BEAM_FIRE_SECONDS = 5;
    private final List<BlockPos> ports = new ArrayList<>();

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 3)
    public int laserLength;

    public int cellCount;
    public int emitterCount;

    @SyncField(units = 1L << 1)
    public int capacitorCount;

    @SyncField(units = 1L << 2)
    public int turbochargerCount;

    public boolean assembled;
    private @Nullable BoundingBox members;

    public BlockEntityMachineICFController(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    public void setup(
            Set<BlockPos> ports,
            Set<BlockPos> cells,
            Set<BlockPos> emitters,
            Set<BlockPos> capacitors,
            Set<BlockPos> turbochargers) {
        this.cellCount = 0;
        this.emitterCount = 0;
        this.capacitorCount = 0;
        this.turbochargerCount = 0;

        Direction dir = getBlockState().getValue(MachineICFController.FACING).getOpposite();

        Set<BlockPos> validCells = new HashSet<>();
        Set<BlockPos> validEmitters = new HashSet<>();
        Set<BlockPos> validCapacitors = new HashSet<>();

        for (int i = 0; i < cells.size(); i++) {
            int j = i + 1;
            BlockPos step = worldPosition.offset(dir.getStepX() * j, 0, dir.getStepZ() * j);
            if (!cells.contains(step)) break;
            this.cellCount++;
            validCells.add(step);
        }

        for (BlockPos emitter : emitters) {
            for (Direction offset : Direction.VALUES) {
                if (validCells.contains(emitter.relative(offset))) {
                    this.emitterCount++;
                    validEmitters.add(emitter);
                    break;
                }
            }
        }

        for (BlockPos capacitor : capacitors) {
            for (Direction offset : Direction.VALUES) {
                if (validEmitters.contains(capacitor.relative(offset))) {
                    this.capacitorCount++;
                    validCapacitors.add(capacitor);
                    break;
                }
            }
        }

        for (BlockPos turbo : turbochargers) {
            for (Direction offset : Direction.VALUES) {
                if (validCapacitors.contains(turbo.relative(offset))) {
                    this.turbochargerCount++;
                    break;
                }
            }
        }

        this.ports.clear();
        this.ports.addAll(ports);
    }

    public List<BlockPos> getPorts() {
        return ports;
    }

    public void assembled(BoundingBox members) {
        this.members = members;
        assembled = true;
        markChanged();
    }

    public void disassemble() {
        if (!assembled) return;
        assembled = false;
        markChanged();
        restoreMembers();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        assembled = false;
        restoreMembers();
    }

    private void restoreMembers() {
        if (members == null || !(level instanceof ServerLevel server)) return;
        BoundingBox bounds = members;
        members = null;
        for (BlockPos pos : AssembledMembers.members(server, worldPosition, bounds)) {
            BlockState shell = server.getBlockState(pos);
            if (shell.getBlock() instanceof BlockICF) {
                server.setBlock(pos, shell.getValue(BlockICF.PART).original(), Block.UPDATE_ALL);
            }
        }
    }

    public void tickServer() {
        this.networkPackNT(50);

        if (!assembled || this.power <= 0) {
            this.laserLength = 0;
            return;
        }

        Direction dir = getBlockState().getValue(MachineICFController.FACING);
        fireLaser(level, dir);
        power = 0;
    }

    private void fireLaser(Level level, Direction dir) {
        for (int i = 1; i < MAX_LASER_LENGTH; i++) {
            this.laserLength = i;
            BlockPos hit = worldPosition.offset(dir.getStepX() * i, 0, dir.getStepZ() * i);
            BlockState state = level.getBlockState(hit);

            if (state.isAir()) continue;

            long owner =
                    MultiblockSurface.foldedCore(state) != null
                            ? hit.asLong()
                            : MultiblockSurface.recordedCorePacked(
                                    (ServerLevel) level, hit.getX(), hit.getY(), hit.getZ());

            BlockPos corePos =
                    worldPosition.offset(dir.getStepX() * (i + 8), -3, dir.getStepZ() * (i + 8));
            if (MultiblockSurface.hasCore(owner)
                    && owner == corePos.asLong()
                    && level.getBlockEntity(corePos) instanceof BlockEntityICF icf) {
                icf.laser += this.getPower();
                icf.maxLaser += this.getMaxPower();
                break;
            }

            if (state.getBlock().getExplosionResistance() < INDESTRUCTIBLE_RESISTANCE) {
                level.destroyBlock(hit, false);
            }
            break;
        }

        BlockPos tip =
                worldPosition.offset(
                        dir.getStepX() * laserLength,
                        dir.getStepY() * laserLength,
                        dir.getStepZ() * laserLength);
        AABB beam =
                new AABB(
                        Math.min(worldPosition.getX(), tip.getX()) + 0.2,
                        Math.min(worldPosition.getY(), tip.getY()) + 0.2,
                        Math.min(worldPosition.getZ(), tip.getZ()) + 0.2,
                        Math.max(worldPosition.getX(), tip.getX()) + 0.8,
                        Math.max(worldPosition.getY(), tip.getY()) + 0.8,
                        Math.max(worldPosition.getZ(), tip.getZ()) + 0.8);

        DamageSource fire = level.damageSources().inFire();
        for (Entity e : level.getEntitiesOfClass(Entity.class, beam)) {
            if (level instanceof ServerLevel server) e.hurtServer(server, fire, BEAM_DAMAGE);
            e.igniteForSeconds(BEAM_FIRE_SECONDS);
        }
    }

    public void tickClient() {
        if (laserLength <= 0 || level.getRandom().nextInt(5) != 0) return;
        Direction dir = getBlockState().getValue(MachineICFController.FACING);
        Direction rot = dir.getClockWise();
        double offXZ = level.getRandom().nextDouble() * 0.25 - 0.125;
        double offY = level.getRandom().nextDouble() * 0.25 - 0.125;
        double dist = 0.55;

        level.addParticle(
                DustParticleOptions.REDSTONE,
                worldPosition.getX() + 0.5 + dir.getStepX() * dist + rot.getStepX() * offXZ,
                worldPosition.getY() + 0.5 + offY,
                worldPosition.getZ() + 0.5 + dir.getStepZ() * dist + rot.getStepZ() * offXZ,
                0,
                0,
                0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = input.getLongOr("power", 0L);
        this.assembled = input.getBooleanOr("assembled", false);
        this.members = input.read("members", BoundingBox.CODEC).orElse(null);
        this.cellCount = input.getIntOr("cellCount", 0);
        this.emitterCount = input.getIntOr("emitterCount", 0);
        this.capacitorCount = input.getIntOr("capacitorCount", 0);
        this.turbochargerCount = input.getIntOr("turbochargerCount", 0);

        ports.clear();
        ports.addAll(input.read("ports", BlockPos.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("assembled", assembled);
        if (members != null) output.store("members", BoundingBox.CODEC, members);
        output.putInt("cellCount", cellCount);
        output.putInt("emitterCount", emitterCount);
        output.putInt("capacitorCount", capacitorCount);
        output.putInt("turbochargerCount", turbochargerCount);
        output.store("ports", BlockPos.CODEC.listOf(), List.copyOf(ports));
    }

    @Override
    public long getPower() {
        return Math.min(power, this.getMaxPower());
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return (long)
                (Math.sqrt(capacitorCount) * MachineData.ICF_CAPACITOR_POWER.get()
                        + Math.sqrt(Math.min(turbochargerCount, capacitorCount))
                                * MachineData.ICF_TURBO_POWER.get());
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.capacitorCount);
            case 2 -> output.writeInt(this.turbochargerCount);
            case 3 -> output.writeInt(this.laserLength);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.capacitorCount = input.readInt();
            case 2 -> this.turbochargerCount = input.readInt();
            case 3 -> this.laserLength = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
