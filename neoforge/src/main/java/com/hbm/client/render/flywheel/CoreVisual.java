// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityCore;
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
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CoreVisual extends HbmDynamicBlockEntityVisual<BlockEntityCore>
        implements ShaderLightVisual {
    private static final int SHELLS = 17;
    private static final int FANS = 150;
    private static final double PULSE_T = .8D;
    private static final Material OPAQUE_MATERIAL =
            SimpleMaterial.builder()
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.OPAQUE)
                    .writeMask(WriteMask.COLOR_DEPTH)
                    .depthTest(DepthTest.LEQUAL)
                    .build();
    private static final Material GLOW_MATERIAL =
            SimpleMaterial.builder()
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .build();
    private static final Model STANDBY_MODEL =
            MeshPart.obj(
                            ResourceManager.sphere_uv.groups[0],
                            ResourceManager.sphere_uv.smoothing(),
                            OPAQUE_MATERIAL)
                    .model();
    private static final Model STANDBY_GLOW_MODEL =
            MeshPart.obj(
                            ResourceManager.sphere_uv.groups[0],
                            ResourceManager.sphere_uv.smoothing(),
                            GLOW_MATERIAL)
                    .model();
    private static final Model ORB_MODEL =
            MeshPart.obj(
                            ResourceManager.sphere_ruv.groups[0],
                            ResourceManager.sphere_ruv.smoothing(),
                            OPAQUE_MATERIAL)
                    .model();
    private static final Model SHELL_MODEL =
            MeshPart.obj(
                            ResourceManager.sphere_ruv.groups[0],
                            ResourceManager.sphere_ruv.smoothing(),
                            GLOW_MATERIAL)
                    .model();
    private static final Model FAN_MODEL = fanModel(GLOW_MATERIAL);
    private static final Material SPARK_MATERIAL =
            SimpleMaterial.builder()
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .shaders(StandardMaterialShaders.LINE)
                    .cutout(CutoutShaders.ONE_TENTH)
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

    private final TransformedInstance standby;
    private final TransformedInstance standbyGlow;
    private final TransformedInstance orb;
    private final TransformedInstance[] shells = new TransformedInstance[SHELLS];
    private final TransformedInstance[] fans = new TransformedInstance[FANS];
    private final SparkLines sparks = new SparkLines();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f scaledPose = new Matrix4f();
    private final Matrix4f flarePose = new Matrix4f();
    private final Random flareRandom = new Random(432L);
    private long lastTime = Long.MIN_VALUE;
    private int lastHeat = Integer.MIN_VALUE;
    private int lastFill = Integer.MIN_VALUE;
    private int lastColor = Integer.MIN_VALUE;
    private boolean lastMeltdown;
    private boolean lastSparkWindow;
    private boolean standbyShown, orbShown, fansShown;
    private boolean initialized;

    public CoreVisual(
            VisualizationContext context, BlockEntityCore blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        standby =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, STANDBY_MODEL)
                        .createInstance();
        standbyGlow =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, STANDBY_GLOW_MODEL)
                        .createInstance();
        orb = instancerProvider().instancer(InstanceTypes.TRANSFORMED, ORB_MODEL).createInstance();
        for (int i = 0; i < shells.length; i++)
            shells[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, SHELL_MODEL)
                            .createInstance();
        for (int i = 0; i < fans.length; i++)
            fans[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, FAN_MODEL)
                            .createInstance();
        standbyShown = orbShown = fansShown = true;
        hideStandby();
        hideOrb();
        hideFans();
        basePose.translation(.5F, .5F, .5F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Model fanModel(Material material) {
        float[] vertices = new float[12 * 8];
        int[] colors = new int[12];
        float[][] edge = {{-.866F, 1F, -.5F}, {.866F, 1F, -.5F}, {0F, 1F, 1F}};
        for (int triangle = 0; triangle < 3; triangle++) {
            int start = triangle * 4;
            vertex(vertices, start, 0F, 0F, 0F);
            vertex(vertices, start + 1, edge[triangle][0], edge[triangle][1], edge[triangle][2]);
            vertex(
                    vertices,
                    start + 2,
                    edge[(triangle + 1) % 3][0],
                    edge[(triangle + 1) % 3][1],
                    edge[(triangle + 1) % 3][2]);
            vertex(vertices, start + 3, 0F, 0F, 0F);
            colors[start] = colors[start + 3] = 0xFFFFFFFF;
            colors[start + 1] = colors[start + 2] = 0x00FFFFFF;
        }
        return new SingleMeshModel(
                PackedQuadMesh.of(vertices, colors, new int[colors.length]), material);
    }

    private static void vertex(float[] vertices, int vertex, float x, float y, float z) {
        int at = vertex * 8;
        vertices[at] = x;
        vertices[at + 1] = y;
        vertices[at + 2] = z;
        vertices[at + 6] = 1F;
    }

    private static float pulse(double time, double rate) {
        double ix = time * rate % (Math.PI * 2D);
        float pulse =
                (float)
                        ((1D / PULSE_T)
                                * Math.atan(
                                        (PULSE_T * Math.sin(ix)) / (1D - PULSE_T * Math.cos(ix))));
        return (pulse + 1F) / 2F;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        int heat = blockEntity.heat;
        long time = level.getGameTime();
        int total = blockEntity.tanks[0].getMaxFill() + blockEntity.tanks[1].getMaxFill();
        int fill = blockEntity.tanks[0].getFill() + blockEntity.tanks[1].getFill();
        float fraction = total <= 0 ? 0F : (float) fill / total;
        boolean meltdown = blockEntity.meltdownTick;
        boolean sparkWindow = heat == 0 && time / 2L % 10L == 0L;
        boolean timedUpdate =
                heat == 0
                        ? sparkWindow != lastSparkWindow || sparkWindow && time != lastTime
                        : time != lastTime;
        if (initialized
                && heat == lastHeat
                && fill == lastFill
                && blockEntity.color == lastColor
                && meltdown == lastMeltdown
                && !timedUpdate) return;
        lastHeat = heat;
        lastFill = fill;
        lastColor = blockEntity.color;
        lastMeltdown = meltdown;
        lastTime = time;
        lastSparkWindow = sparkWindow;
        initialized = true;
        sparks.hide();
        if (heat == 0) {
            if (!standbyShown) {
                show(standby, basePose, ARGB.colorFromFloat(1F, .5F, .5F, .5F), .25F);
                scaledPose.set(basePose).scale(.25F * 1.25F);
                show(standbyGlow, scaledPose, ARGB.colorFromFloat(1F, .1F, .1F, .1F), 1F);
                standbyShown = true;
            }
            hideOrb();
            hideFans();
            if (sparkWindow) {
                long tenths = time / 2L;
                scaledPose.set(basePose).scale(.25F * 1.25F);
                for (int i = 0; i < 3; i++) {
                    sparks.write(scaledPose, (int) tenths + i * 10_000);
                    sparks.write(scaledPose, (int) time + i * 10_000);
                }
            }
            return;
        }

        hideStandby();
        if (meltdown) {
            hideOrb();
            writeFlare(time, blockEntity.color);
            fansShown = true;
        } else {
            hideFans();
            int tint =
                    ARGB.color(
                            255,
                            (int) (((blockEntity.color >> 16) & 255) * .4F),
                            (int) (((blockEntity.color >> 8) & 255) * .4F),
                            (int) ((blockEntity.color & 255) * .4F));
            float scale = (4.5F * fraction + .5F) * .25F;
            scaledPose.set(basePose).scale(scale);
            show(orb, scaledPose, tint, 1F);
            float breath = pulse(time, .1D);
            for (int i = 0; i < shells.length; i++) {
                float shellScale = 1F + .25F * i + breath * (20F - i) * .125F;
                scaledPose.set(basePose).scale(scale * shellScale);
                show(shells[i], scaledPose, tint, 1F);
            }
            orbShown = true;
        }
    }

    private void hideStandby() {
        if (!standbyShown) return;
        standby.setVisible(false);
        standbyGlow.setVisible(false);
        standbyShown = false;
    }

    private void hideOrb() {
        if (!orbShown) return;
        orb.setVisible(false);
        for (var shell : shells) shell.setVisible(false);
        orbShown = false;
    }

    private void hideFans() {
        if (!fansShown) return;
        for (var fan : fans) fan.setVisible(false);
        fansShown = false;
    }

    private void writeFlare(long time, int color) {
        float scale = .875F + pulse(time, .2D) * .125F;
        flarePose.set(basePose).scale(scale);
        flareRandom.setSeed(432L);
        int tint = ARGB.opaque(color);
        for (int i = 0; i < fans.length; i++) {
            flarePose
                    .rotateX(flareRandom.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateY(flareRandom.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateZ(flareRandom.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateX(flareRandom.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateY(flareRandom.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateZ((flareRandom.nextFloat() * 360F + time / 200F * 90F) * Mth.DEG_TO_RAD);
            float length = flareRandom.nextFloat() * 2F + 5F;
            float width = flareRandom.nextFloat() + 1F;
            flarePose.scale(.999F);
            scaledPose.set(flarePose).scale(width, length, width);
            show(fans[i], scaledPose, tint, 1F);
        }
    }

    private void show(TransformedInstance instance, Matrix4f local, int color, float scale) {
        instance.setVisible(true);
        pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        if (scale != 1F) pose.scale(scale);
        instance.setTransform(pose).colorArgb(color).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(7D);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(standby);
        consumer.accept(orb);
        sparks.collect(consumer);
    }

    @Override
    protected void _delete() {
        standby.delete();
        standbyGlow.delete();
        orb.delete();
        for (var shell : shells) shell.delete();
        for (var fan : fans) fan.delete();
        sparks.delete();
    }

    private final class SparkLines {
        private final ArrayList<TransformedInstance> core = new ArrayList<>();
        private final ArrayList<TransformedInstance> edge = new ArrayList<>();
        private final Matrix4f linePose = new Matrix4f();
        private final Vector3f from = new Vector3f();
        private final Vector3f to = new Vector3f();
        private final Quaternionf rotation = new Quaternionf();
        private final Random random = new Random();
        private int used;

        private void write(Matrix4f base, int seed) {
            random.setSeed(seed);
            Vector3f direction =
                    from.set(
                                    (float) (random.nextDouble() - .5D),
                                    (float) (random.nextDouble() - .5D),
                                    (float) (random.nextDouble() - .5D))
                            .normalize();
            double x = 0D, y = 0D, z = 0D;
            int start = used, i = 0;
            for (; i < 5 + random.nextInt(10); i++) {
                double px = x, py = y, pz = z;
                x = px + direction.x * 1.5F * random.nextFloat();
                y = py + direction.y * 1.5F * random.nextFloat();
                z = pz + direction.z * 1.5F * random.nextFloat();
                segment(base, px, py, pz, x, y, z, 0xFFFF00, true, start + i);
                segment(base, px, py, pz, x, y, z, 0xFFFFFF, false, start + i);
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
            while (target.size() <= index)
                target.add(
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, model)
                                .createInstance());
            Vector3f a = base.transformPosition(from.set((float) x0, (float) y0, (float) z0));
            Vector3f b = base.transformPosition(to.set((float) x1, (float) y1, (float) z1));
            float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            TransformedInstance instance = target.get(index);
            instance.setVisible(length > 0F);
            if (length <= 0F) return;
            rotation.rotationTo(0F, 1F, 0F, dx / length, dy / length, dz / length);
            linePose.translation(
                            pos.getX() - renderOrigin().getX() + a.x,
                            pos.getY() - renderOrigin().getY() + a.y,
                            pos.getZ() - renderOrigin().getZ() + a.z)
                    .rotate(rotation)
                    .scale(length);
            instance.setTransform(linePose).colorArgb(0xFF000000 | color).light(0).setChanged();
        }

        private void hide() {
            if (used == 0) return;
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
