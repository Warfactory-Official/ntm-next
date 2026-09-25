// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Viscous;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.SplashParticleOptions;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockEntityMachineDrain extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                FluidTankEndpoint,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int CAPACITY = 2_000;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM tank = new FluidTankNTM(CAPACITY);

    private final FluidTankNTM[] receiving;

    public BlockEntityMachineDrain(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DRAIN.get(), pos, state);
        receiving = new FluidTankNTM[] {tank};
    }

    public void tickServer() {
        networkPackNT(50);

        if (tank.getFill() <= 0) return;

        if (NTMFluidProperties.hasTrait(tank.getTankType(), FT_Amat.class)) {
            level.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    10F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
            return;
        }

        int toSpill = Math.max(tank.getFill() / 2, 1);
        tank.setFill(tank.getFill() - toSpill);
        FluidTrait.onRelease(
                level, worldPosition, tank.getTankType(), tank, FluidReleaseType.SPILL, toSpill);

        if (toSpill >= 100
                && level.getRandom().nextInt(20) == 0
                && NTMFluidProperties.hasTrait(tank.getTankType(), FT_Liquid.class)
                && NTMFluidProperties.hasTrait(tank.getTankType(), FT_Viscous.class)
                && NTMFluidProperties.hasTrait(tank.getTankType(), FT_Flammable.class)) {
            spillPuddle();
        }
    }

    private void spillPuddle() {
        Direction dir = facing();
        Vec3 start =
                new Vec3(
                        worldPosition.getX() + 0.5 - dir.getStepX() * 3,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5 - dir.getStepZ() * 3);
        Vec3 end =
                start.add(
                        level.getRandom().nextGaussian() * 5,
                        -25,
                        level.getRandom().nextGaussian() * 5);
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                start,
                                end,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                CollisionContext.empty()));

        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;

        BlockPos above = hit.getBlockPos().above();
        BlockState state = level.getBlockState(above);
        BlockState puddle = ModBlocks.OIL_SPILL.get().defaultBlockState();
        if (!state.liquid() && state.canBeReplaced() && puddle.canSurvive(level, above)) {
            level.setBlock(above, puddle, 3);
        }
    }

    public void tickClient() {
        if (tank.getFill() <= 0 || tank.getTankType() == null) return;

        Direction dir = facing();
        double x = worldPosition.getX() + 0.5 - dir.getStepX() * 2.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5 - dir.getStepZ() * 2.5;

        if (level.getNearestPlayer(x, y, z, 100.0D, false) == null) return;

        var props = NTMFluidProperties.get(tank.getFluid());
        int color =
                props != null ? ARGB.transparent(props.colorARGB()) : SplashParticleOptions.NO_TINT;

        if (NTMFluidProperties.hasTrait(tank.getTankType(), FT_Gaseous.class)) {
            level.addParticle(
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(0.5F)
                            .setBaseScale(0.375F)
                            .setMaxScale(3F)
                            .setLife(100 + level.getRandom().nextInt(50))
                            .setColor(color)
                            .build(),
                    true,
                    false,
                    x,
                    y,
                    z,
                    0,
                    0,
                    0);
            return;
        }
        level.addParticle(new SplashParticleOptions(color), true, false, x, y, z, 0, 0, 0);
    }

    private Direction facing() {
        return getBlockState().getValue(BlockMultiblockCore.FACING);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
