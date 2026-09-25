// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineStirling;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.data.MachineData;
import com.hbm.entity.projectile.EntityCog;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.NeighborDerived;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.util.Facing;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityStirling extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, PersistentDrop, SyncUnitSchema {

    private static final String[] PERSISTENT_KEYS = {"hasCog"};

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    @SyncField(units = 1L << 0)
    public long powerBuffer;

    @SyncField(units = 1L << 1)
    public int heat;

    @SyncField(units = 1L << 2)
    public boolean hasCog = true;

    public float spin, lastSpin;
    private int warnCooldown;
    private int overspeed;

    public BlockEntityStirling(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STIRLING.get(), pos, state, 0);
    }

    private MachineStirling machine() {
        return (MachineStirling) getBlockState().getBlock();
    }

    public int maxHeat() {
        return machine().tier().maxHeat;
    }

    public boolean isCreative() {
        return machine().tier().creative;
    }

    public int gearVariant() {
        return machine().tier().gear;
    }

    @Override
    public void tickServer() {
        ServerLevel level = (ServerLevel) this.level;

        if (hasCog) {
            powerBuffer = 0;
            tryPullHeat();

            powerBuffer =
                    (long) (heat * (isCreative() ? 1 : MachineData.STIRLING_EFFICIENCY.get()));

            if (warnCooldown > 0) warnCooldown--;

            if (heat > maxHeat() && !isCreative()) {
                overspeed++;

                if (overspeed > 60 && warnCooldown == 0) {
                    warnCooldown = 100;
                    level.playSound(
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5,
                            ModSounds.WARN_OVERSPEED.get(),
                            SoundSource.BLOCKS,
                            2.0F,
                            1.0F);
                }

                if (overspeed > MachineData.STIRLING_OVERSPEED_LIMIT.get()) {
                    hasCog = false;
                    level.explode(
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5,
                            5F,
                            false,
                            Level.ExplosionInteraction.NONE);
                    throwCog(level);
                    setChanged();
                }
            } else {
                overspeed = 0;
            }
        } else {
            overspeed = 0;
            warnCooldown = 0;
        }

        networkPackNT(150);

        if (!hasCog && powerBuffer > 0) powerBuffer--;

        heat = 0;
    }

    private void throwCog(ServerLevel level) {
        Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
        EntityCog cog =
                new EntityCog(
                                level,
                                worldPosition.getX() + 0.5 + dir.getStepX(),
                                worldPosition.getY() + 1,
                                worldPosition.getZ() + 0.5 + dir.getStepZ())
                        .setOrientation(dir.get3DDataValue())
                        .setMeta(gearVariant());
        Direction rot = Facing.rotate(dir, Direction.DOWN);
        cog.setDeltaMovement(rot.getStepX(), 1 + (heat - maxHeat()) * 0.0001D, rot.getStepZ());
        level.addFreshEntity(cog);
    }

    @Override
    public void tickClient() {
        float momentum = powerBuffer * 50F / maxHeat();

        if (isCreative()) momentum = Math.min(momentum, 45F);

        lastSpin = spin;
        spin += momentum;

        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    private void tryPullHeat() {
        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source != null) {
            int heatSrc =
                    (int)
                            (source.getHeatStored(level, heatPos)
                                    * MachineData.STIRLING_DIFFUSION.get());
            if (heatSrc > 0) {
                source.useUpHeat(level, heatPos, heatSrc);
                this.heat += heatSrc;
                return;
            }
        }
        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.powerBuffer = power;
    }

    @Override
    public long getMaxPower() {
        return powerBuffer;
    }

    @Override
    public long getProviderSpeed() {
        return hasCog ? powerBuffer : 0L;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (!hasCog) components.set(ModDataComponents.HAS_COG.get(), false);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        hasCog = components.getOrDefault(ModDataComponents.HAS_COG.get(), true);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        powerBuffer = input.getLongOr("powerBuffer", powerBuffer);
        hasCog = input.getBooleanOr("hasCog", hasCog);
        overspeed = input.getIntOr("overspeed", overspeed);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerBuffer", powerBuffer);
        output.putBoolean("hasCog", hasCog);
        output.putInt("overspeed", overspeed);
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.powerBuffer);
            case 1 -> output.writeInt(this.heat);
            case 2 -> output.writeBoolean(this.hasCog);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.powerBuffer = input.readLong();
            case 1 -> this.heat = input.readInt();
            case 2 -> this.hasCog = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
