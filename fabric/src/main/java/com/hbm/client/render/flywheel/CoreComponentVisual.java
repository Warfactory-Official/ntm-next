// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.CoreComponent;
import com.hbm.client.model.Meshes;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.lib.Library;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.machine.BlockEntityCoreEmitter;
import com.hbm.tileentity.machine.BlockEntityCoreInjector;
import com.hbm.tileentity.machine.BlockEntityCoreStabilizer;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class CoreComponentVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineBase> {
    private static final HFRWavefrontObject EMITTER_BODY =
            Meshes.load(Library.id("models/core_emitter.obj"));
    private static final HFRWavefrontObject INJECTOR_BODY =
            Meshes.load(Library.id("models/core_injector.obj"));
    private final BeamVisual[] beams;
    private final AABB rawBodyBounds;
    private final int maxRange;
    private final Matrix4f basePose = new Matrix4f();
    private final Vec3[] injectorDirections = new Vec3[2];
    private final int[] injectorColors = new int[2];
    private final Fluid[] injectorFluids = new Fluid[2];
    private final boolean[] injectorShown = new boolean[2];
    private @Nullable Vec3 emitterDirection;
    private @Nullable Vec3 stabilizerDirection;
    private boolean beamsHidden;

    public CoreComponentVisual(
            VisualizationContext context, BlockEntityMachineBase blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        boolean emitter = blockEntity instanceof BlockEntityCoreEmitter;
        boolean injector = blockEntity instanceof BlockEntityCoreInjector;
        maxRange =
                emitter
                        ? BlockEntityCoreEmitter.RANGE
                        : injector
                                ? BlockEntityCoreInjector.RANGE
                                : BlockEntityCoreStabilizer.RANGE;
        basePose.translate(.5F, 0F, .5F).rotateY((90F) * Mth.DEG_TO_RAD);
        switch (blockState.getValue(CoreComponent.FACING)) {
            case DOWN -> basePose.translate(0F, .5F, -.5F).rotateX((90F) * Mth.DEG_TO_RAD);
            case UP -> basePose.translate(0F, .5F, .5F).rotateX((-90F) * Mth.DEG_TO_RAD);
            case NORTH -> basePose.rotateY((90F) * Mth.DEG_TO_RAD);
            case WEST -> basePose.rotateY((180F) * Mth.DEG_TO_RAD);
            case SOUTH -> basePose.rotateY((270F) * Mth.DEG_TO_RAD);
            case EAST -> {}
        }
        basePose.translate(0F, .5F, 0F);
        var extent = new AABB(pos);
        for (GroupObject group : (emitter ? EMITTER_BODY : INJECTOR_BODY).groups)
            extent = extent.minmax(LightBounds.of(group, basePose, pos));
        rawBodyBounds = extent;
        beams = new BeamVisual[injector ? 2 : 3];
        for (int i = 0; i < beams.length; i++)
            beams[i] =
                    new BeamVisual(context, level, pos, !emitter, emitter ? i == 0 ? 2 : 4 : 0, 1F);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        long time = level.getGameTime();
        int phase = (int) time % 1000;
        if (blockEntity instanceof BlockEntityCoreEmitter emitter) {
            if (emitter.beam <= 0) {
                hide();
                return;
            }
            beamsHidden = false;
            if (emitterDirection == null || emitterDirection.z != emitter.beam)
                emitterDirection = new Vec3(0, 0, emitter.beam);
            beams[0].update(
                    basePose,
                    emitterDirection,
                    EnumWaveType.SPIRAL,
                    0,
                    1,
                    0,
                    .0625F,
                    0x404000,
                    0x404000);
            beams[1].update(
                    basePose,
                    emitterDirection,
                    EnumWaveType.RANDOM,
                    phase,
                    emitter.beam * 2,
                    .125F,
                    .0625F,
                    0x401500,
                    0x401500);
            beams[2].update(
                    basePose,
                    emitterDirection,
                    EnumWaveType.RANDOM,
                    phase + 1,
                    emitter.beam * 2,
                    .125F,
                    .0625F,
                    0x401500,
                    0x401500);
        } else if (blockEntity instanceof BlockEntityCoreInjector injector) {
            for (int i = 0; i < 2; i++) {
                var tank = injector.tanks[i];
                if (injector.beam <= 0 || tank.getFill() <= 0) {
                    if (injectorShown[i]) beams[i].hide();
                    injectorShown[i] = false;
                    continue;
                }
                injectorShown[i] = true;
                Fluid fluid = tank.getFluid();
                if (fluid != injectorFluids[i]) {
                    var properties = NTMFluidProperties.get(fluid);
                    injectorColors[i] = properties == null ? 0xFFFFFF : properties.color();
                    injectorFluids[i] = fluid;
                }
                if (injectorDirections[i] == null || injectorDirections[i].z != injector.beam)
                    injectorDirections[i] = new Vec3(0, 0, injector.beam);
                beams[i].update(
                        basePose,
                        injectorDirections[i],
                        EnumWaveType.RANDOM,
                        phase + i,
                        injector.beam,
                        .0625F,
                        0,
                        injectorColors[i],
                        0x808080);
            }
        } else {
            var stabilizer = (BlockEntityCoreStabilizer) blockEntity;
            if (stabilizer.beam <= 0) {
                hide();
                return;
            }
            beamsHidden = false;
            if (stabilizerDirection == null || stabilizerDirection.z != stabilizer.beam)
                stabilizerDirection = new Vec3(0, 0, stabilizer.beam);
            beams[0].update(
                    basePose,
                    stabilizerDirection,
                    EnumWaveType.SPIRAL,
                    (int) (time * -25 % 360),
                    stabilizer.beam * 3,
                    .125F,
                    0,
                    0xFFA200,
                    0xFFD000);
            beams[1].update(
                    basePose,
                    stabilizerDirection,
                    EnumWaveType.SPIRAL,
                    (int) (time * -15 % 360) + 180,
                    stabilizer.beam * 3,
                    .125F,
                    0,
                    0xFFA200,
                    0xFFD000);
            beams[2].update(
                    basePose,
                    stabilizerDirection,
                    EnumWaveType.SPIRAL,
                    (int) (time * -5 % 360) + 180,
                    stabilizer.beam * 3,
                    .125F,
                    0,
                    0xFFA200,
                    0xFFD000);
        }
    }

    private void hide() {
        if (beamsHidden) return;
        for (var beam : beams) beam.hide();
        beamsHidden = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        Vector3f end = basePose.transformPosition(0F, 0F, maxRange, new Vector3f());
        return rawBodyBounds
                .minmax(new AABB(pos).inflate(1))
                .minmax(
                        new AABB(
                                        pos.getX() + end.x,
                                        pos.getY() + end.y,
                                        pos.getZ() + end.z,
                                        pos.getX() + end.x,
                                        pos.getY() + end.y,
                                        pos.getZ() + end.z)
                                .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var beam : beams) beam.delete();
    }
}
