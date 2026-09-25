// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineReactorBreeding;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BreederVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineReactorBreeding>
        implements ShaderLightVisual {
    private static final int SPARKS = 3;
    private static final double SPARK_Y = 1.5625D;
    private static final float SPARK_LENGTH = .15F;
    private static final int SPARK_MIN = 3, SPARK_MAX = 4;
    private static final int SPARK_CORE = 0x00FF00, SPARK_EDGE = 0xFFFFFF;
    private static final HFRWavefrontObject BODY_SOURCE = ResourceManager.breeder;
    private static final Material SPARK_MATERIAL =
            SimpleMaterial.builder()
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .shaders(StandardMaterialShaders.LINE)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .transparency(Transparency.OPAQUE)
                    .writeMask(WriteMask.COLOR_DEPTH)
                    .depthTest(DepthTest.LEQUAL)
                    .build();
    private static final Model SPARK_CORE_MODEL = MeshPart.line(5F, SPARK_MATERIAL);
    private static final Model SPARK_EDGE_MODEL = MeshPart.line(2F, SPARK_MATERIAL);
    private final SparkLines sparks;
    private final AABB rawBodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f[] sparkPoses = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private int lastSeed;
    private boolean lastBreeding;
    private boolean initialized;

    public BreederVisual(
            VisualizationContext context,
            BlockEntityMachineReactorBreeding blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90)
                                        * Mth.DEG_TO_RAD);
        var rawBodyExtent = new AABB(pos);
        for (var group : BODY_SOURCE.groups)
            rawBodyExtent = rawBodyExtent.minmax(LightBounds.of(group, rawBodyLocal, pos));
        rawBodyBounds = rawBodyExtent;
        sparks = new SparkLines();
        basePose.set(rawBodyLocal);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean breeding = blockEntity.progress > 0F;
        int seed =
                (int)
                        ((GameTime.millis(blockEntity.getLevel()) + (long) (partialTick * 50F))
                                % 10_000L
                                / 100L);
        if (initialized && breeding == lastBreeding && (!breeding || seed == lastSeed)) return;
        sparks.hide();
        if (!breeding) {
            lastBreeding = false;
            lastSeed = seed;
            initialized = true;
            return;
        }
        for (int i = 0; i < SPARKS; i++) {
            sparkPoses[i].set(basePose).rotateY((float) (Math.PI * i) * Mth.DEG_TO_RAD);
            sparks.write(sparkPoses[i], seed + i);
        }
        lastBreeding = true;
        lastSeed = seed;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 3,
                                pos.getZ() + 1)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        sparks.collect(consumer);
    }

    @Override
    protected void _delete() {
        sparks.delete();
    }

    private final class SparkLines {
        private final ArrayList<TransformedInstance> core = new ArrayList<>();
        private final ArrayList<TransformedInstance> edge = new ArrayList<>();
        private final Random random = new Random();
        private final Matrix4f pose = new Matrix4f();
        private final Vector3f from = new Vector3f();
        private final Vector3f to = new Vector3f();
        private final Quaternionf rotation = new Quaternionf();
        private int used;

        private void write(Matrix4f base, int seed) {
            random.setSeed(seed);
            Vector3f direction =
                    from.set(
                                    (float) (random.nextDouble() - .5D),
                                    (float) (random.nextDouble() - .5D),
                                    (float) (random.nextDouble() - .5D))
                            .normalize();
            double x = 0D, y = SPARK_Y, z = 0D;
            int start = used, i = 0;
            for (; i < SPARK_MIN + random.nextInt(SPARK_MAX); i++) {
                double px = x, py = y, pz = z;
                x = px + direction.x * SPARK_LENGTH * random.nextFloat();
                y = py + direction.y * SPARK_LENGTH * random.nextFloat();
                z = pz + direction.z * SPARK_LENGTH * random.nextFloat();
                segment(base, px, py, pz, x, y, z, SPARK_CORE, true, start + i);
                segment(base, px, py, pz, x, y, z, SPARK_EDGE, false, start + i);
            }
            used += i;
        }

        private void segment(
                Matrix4f base,
                double x0,
                double y0,
                double z0,
                double x1,
                double y1,
                double z1,
                int color,
                boolean wide,
                int index) {
            ArrayList<TransformedInstance> target = wide ? core : edge;
            Model model = wide ? SPARK_CORE_MODEL : SPARK_EDGE_MODEL;
            while (target.size() <= index) {
                target.add(
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, model)
                                .createInstance());
            }
            Vector3f a = base.transformPosition(from.set((float) x0, (float) y0, (float) z0));
            Vector3f b = base.transformPosition(to.set((float) x1, (float) y1, (float) z1));
            float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            TransformedInstance instance = target.get(index);
            instance.setVisible(length > 0F);
            if (length > 0F) {
                rotation.rotationTo(0F, 1F, 0F, dx / length, dy / length, dz / length);
                pose.translation(
                                pos.getX() - renderOrigin().getX() + a.x,
                                pos.getY() - renderOrigin().getY() + a.y,
                                pos.getZ() - renderOrigin().getZ() + a.z)
                        .rotate(rotation)
                        .scale(length);
                instance.setTransform(pose).colorArgb(0xFF000000 | color).light(0).setChanged();
            }
        }

        private void hide() {
            for (var instance : core) instance.setVisible(false);
            for (var instance : edge) instance.setVisible(false);
            used = 0;
        }

        private void collect(Consumer<Instance> consumer) {
            for (var instance : core) consumer.accept(instance);
            for (var instance : edge) consumer.accept(instance);
        }

        private void delete() {
            for (var instance : core) instance.delete();
            for (var instance : edge) instance.delete();
        }
    }
}
