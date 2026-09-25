// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.BlockEntityMachineExposureChamber;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ExposureChamberVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineExposureChamber>
        implements ShaderLightVisual {
    private static final int MAX_BEAMS = 7;
    private static final int FLICKER_PERIOD = 8;
    private static final int FLICKER_CHANCE = 2;
    private static final int FLICKER_TINT = 0x80D0FF;
    private static final int FLICKER_PLAIN = 0xFFFFFF;
    private static final Vec3 INJECTOR = new Vec3(0D, 0D, 5D);
    private static final Vec3 COLUMN = new Vec3(0D, 1.5D, 0D);
    private static final Vec3 RING = new Vec3(0D, 0D, -1D);
    private static final Material MAGNET_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.exposure_chamber_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final Material CORE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.exposure_chamber_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final MeshPart MAGNET_PART =
            MeshPart.obj(
                    ResourceManager.exposure_chamber
                            .groups[ResourceManager.exposure_chamber.partId("Magnets")],
                    ResourceManager.exposure_chamber.smoothing(),
                    MAGNET_MATERIAL);
    private static final MeshPart CORE_PART =
            MeshPart.obj(
                    ResourceManager.exposure_chamber
                            .groups[ResourceManager.exposure_chamber.partId("Core")],
                    ResourceManager.exposure_chamber.smoothing(),
                    CORE_MATERIAL);

    private final TransformedInstance magnets;
    private final TransformedInstance core;
    private final BeamVisual[] beams = new BeamVisual[MAX_BEAMS];
    private final Matrix4f magnetsPose = new Matrix4f();
    private final Matrix4f corePose = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f columnPose = new Matrix4f();
    private final Matrix4f ringPose = new Matrix4f();
    private final Matrix4f[] injectorPoses = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f instancePose = new Matrix4f();
    private final Random flickerRandom = new Random();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private float lastRotation = Float.NaN;
    private float lastBob = Float.NaN;
    private boolean lastActive;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public ExposureChamberVisual(
            VisualizationContext context,
            BlockEntityMachineExposureChamber blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(BlockMultiblockCore.FACING), 90)
                                * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.exposure_chamber,
                                        "Chamber",
                                        basePose,
                                        pos));
        magnets =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MAGNET_PART.model())
                        .createInstance();
        core =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CORE_PART.model())
                        .createInstance();
        for (int i = 0; i < beams.length; i++)
            beams[i] = new BeamVisual(context, level, pos, true, 0, 1F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float rotation = Mth.lerp(partialTick, blockEntity.prevRotation, blockEntity.rotation);
        long gameTime = level.getGameTime();
        float bob = (float) (Math.sin((gameTime % (Math.PI * 16D) + partialTick) * .125D) * .0625D);
        boolean active = blockEntity.isOn;
        boolean poseChanged =
                !initialized
                        || Float.compare(rotation, lastRotation) != 0
                        || Float.compare(bob, lastBob) != 0
                        || active != lastActive;
        if (!poseChanged && !active) return;
        boolean activeChanged = !initialized || active != lastActive;
        lastRotation = rotation;
        lastBob = bob;
        lastActive = active;
        initialized = true;
        if (poseChanged) {
            magnetsPose.set(basePose).rotateY(rotation * Mth.DEG_TO_RAD);
            corePose.set(basePose).rotateY(rotation * .5F * Mth.DEG_TO_RAD).translate(0F, bob, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(magnetsPose);
            magnets.setTransform(instancePose).light(0).setChanged();
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(corePose);
            core.setVisible(active);
            if (active)
                core.setTransform(instancePose).light(LightCoordsUtil.FULL_BRIGHT).setChanged();
            LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, MAGNET_PART.model(), magnetsPose, pos);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }

        if (!active) {
            if (activeChanged) for (var beam : beams) beam.hide();
            return;
        }
        long beamStart = GameTime.millis(level) + (long) (partialTick * 50F);
        int start = (int) (beamStart % 1000L) / 50;
        flickerRandom.setSeed(gameTime / FLICKER_PERIOD);
        int color = gameTime % FLICKER_PERIOD >= FLICKER_PERIOD / 2 ? FLICKER_TINT : FLICKER_PLAIN;
        flickerRandom.nextInt(FLICKER_CHANCE);
        int used = 0;
        if (flickerRandom.nextInt(FLICKER_CHANCE) == 0)
            used = injector(used, basePose, 0D, 3.675D, color, start);
        if (flickerRandom.nextInt(FLICKER_CHANCE) == 0)
            used = injector(used, basePose, 1.1875D, 2.5D, color, start);
        if (flickerRandom.nextInt(FLICKER_CHANCE) == 0)
            used = injector(used, basePose, -1.1875D, 2.5D, color, start);
        columnPose.set(basePose).translate(0F, 1.75F, 0F);
        used =
                beam(
                        used,
                        columnPose,
                        COLUMN,
                        EnumWaveType.RANDOM,
                        FLICKER_TINT,
                        FLICKER_PLAIN,
                        start,
                        10,
                        .125F);
        used =
                beam(
                        used,
                        columnPose,
                        COLUMN,
                        EnumWaveType.RANDOM,
                        0x8080FF,
                        FLICKER_PLAIN,
                        (int) (beamStart + 5L) / 50,
                        10,
                        .125F);
        ringPose.set(basePose).translate(0F, 2.5F, 0F);
        int spin = (int) (beamStart % 360L);
        used =
                beam(
                        used,
                        ringPose,
                        RING,
                        EnumWaveType.SPIRAL,
                        0xFFFF80,
                        FLICKER_PLAIN,
                        spin,
                        15,
                        .125F);
        used =
                beam(
                        used,
                        ringPose,
                        RING,
                        EnumWaveType.SPIRAL,
                        0xFF8080,
                        FLICKER_PLAIN,
                        spin + 180,
                        15,
                        .125F);
        while (used < beams.length) beams[used++].hide();
    }

    private int injector(int index, Matrix4f base, double x, double y, int color, int start) {
        Matrix4f pose =
                injectorPoses[Math.min(index, injectorPoses.length - 1)]
                        .set(base)
                        .translate((float) x, (float) y, -7.5F);
        return beam(
                index, pose, INJECTOR, EnumWaveType.RANDOM, color, FLICKER_PLAIN, start, 15, .125F);
    }

    private int beam(
            int index,
            Matrix4f pose,
            Vec3 skeleton,
            EnumWaveType wave,
            int outer,
            int inner,
            int start,
            int count,
            float size) {
        beams[index].update(pose, skeleton, wave, start, count, size, 0F, outer, inner);
        return index + 1;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 8,
                                pos.getY(),
                                pos.getZ() - 8,
                                pos.getX() + 9,
                                pos.getY() + 5,
                                pos.getZ() + 9)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(magnets);
    }

    @Override
    protected void _delete() {
        magnets.delete();
        core.delete();
        for (var beam : beams) beam.delete();
    }
}
