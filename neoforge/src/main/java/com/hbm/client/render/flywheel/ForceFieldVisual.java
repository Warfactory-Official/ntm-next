// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityForceField;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class ForceFieldVisual extends HbmDynamicBlockEntityVisual<BlockEntityForceField>
        implements ShaderLightVisual {
    private static final float LINE_WIDTH = 1F;
    private static final HFRWavefrontObject BODY_SOURCE =
            Meshes.load(Library.id("models/machines/radar_base.obj"));
    private static final Material TOP_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.forcefield_top_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart[] TOP_PARTS =
            MeshPart.objParts(ResourceManager.forcefield_top, TOP_MATERIAL);
    private static final Material LINE_MATERIAL =
            SimpleMaterial.builder()
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .shaders(StandardMaterialShaders.LINE)
                    .light(LightShaders.NONE)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .depthTest(DepthTest.LEQUAL)
                    .build();
    private static final Model LINE_MODEL = MeshPart.line(LINE_WIDTH, LINE_MATERIAL);

    private final AABB bodyBounds;
    private final AABB litBounds;
    private final TransformedInstance[] topInstances = new TransformedInstance[TOP_PARTS.length];
    private final ArrayList<TransformedInstance> lines = new ArrayList<>();
    private final Matrix4f topPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f linePose = new Matrix4f();
    private final Vector3f from = new Vector3f();
    private final Vector3f to = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Matrix4f topBasePose = new Matrix4f();
    private final Matrix4f shellBasePose = new Matrix4f();
    private final double[] sphere = new double[3];
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastSpin = Float.NaN;
    private float lastRadius = Float.NaN;
    private int lastColor = Integer.MIN_VALUE;
    private volatile int lineLight;
    private int lastLineLight;
    private boolean lastUp;
    private boolean initialized;
    private int usedLines;
    private float boundsRadius = Float.NaN;

    public ForceFieldVisual(
            VisualizationContext context, BlockEntityForceField blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyPose = new Matrix4f().translate(.5F, 0F, .5F).rotateY((float) Math.PI);
        topBasePose.translation(.5F, 1F, .5F).rotateY((float) Math.PI);
        shellBasePose.translation(.5F, 0F, .5F).rotateY((float) Math.PI).translate(0F, .5F, 0F);
        bodyBounds = LightBounds.of(BODY_SOURCE, "Cube_Cube.001", bodyPose, pos);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        for (MeshPart part : TOP_PARTS)
            LightBounds.includeLightBounds(lightBoundsAccumulator, part.model(), topBasePose, pos);
        litBounds =
                new AABB(
                        lightBoundsAccumulator[0],
                        lightBoundsAccumulator[1],
                        lightBoundsAccumulator[2],
                        lightBoundsAccumulator[3],
                        lightBoundsAccumulator[4],
                        lightBoundsAccumulator[5]);
        for (int i = 0; i < TOP_PARTS.length; i++) {
            topInstances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, TOP_PARTS[i].model())
                            .createInstance();
        }
        trackExtent();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static int segments(float radius) {
        return (int) (16F + radius * .125F);
    }

    private static void rotateX(double[] vector, float angle) {
        float cos = Mth.cos(angle), sin = Mth.sin(angle);
        double y = vector[1] * cos + vector[2] * sin;
        double z = vector[2] * cos - vector[1] * sin;
        vector[1] = y;
        vector[2] = z;
    }

    private static void rotateY(double[] vector, float angle) {
        float cos = Mth.cos(angle), sin = Mth.sin(angle);
        double x = vector[0] * cos + vector[2] * sin;
        double z = vector[2] * cos - vector[0] * sin;
        vector[0] = x;
        vector[2] = z;
    }

    private static void rotateZ(double[] vector, float angle) {
        float cos = Mth.cos(angle), sin = Mth.sin(angle);
        double x = vector[0] * cos + vector[1] * sin;
        double y = vector[1] * cos - vector[0] * sin;
        vector[0] = x;
        vector[1] = y;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        long millis = GameTime.millis(level) + (long) (partialTick * 50F);
        float spin = (float) ((millis / 10D) % 360D);
        boolean up =
                blockEntity.isOn
                        && blockEntity.health > 0
                        && blockEntity.power > 0
                        && blockEntity.cooldown == 0;
        float radius = blockEntity.radius;
        int color = ARGB.opaque(blockEntity.color);
        int light = lineLight;
        boolean upChanged = !initialized || up != lastUp;
        boolean topChanged = upChanged || up && spin != lastSpin;
        boolean shellChanged =
                upChanged
                        || up
                                && (radius != lastRadius
                                        || color != lastColor
                                        || light != lastLineLight);
        if (!topChanged && !shellChanged) return;
        lastUp = up;
        lastSpin = spin;
        lastRadius = radius;
        lastColor = color;
        lastLineLight = light;
        initialized = true;
        if (topChanged) {
            topPose.set(topBasePose);
            if (up) topPose.rotateY(-spin * Mth.DEG_TO_RAD);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(topPose);
            for (TransformedInstance instance : topInstances)
                instance.setTransform(instancePose).light(0).setChanged();
            LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
            for (MeshPart part : TOP_PARTS)
                LightBounds.includeLightBounds(lightBoundsAccumulator, part.model(), topPose, pos);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }
        if (shellChanged) {
            hideLines();
            if (up) {
                int l = segments(radius);
                emitShell(l, l * 2, radius, color);
            }
        }
    }

    private void emitShell(int l, int s, float radius, int color) {
        float meridianRotation = 360F / s;
        float latitudeRotation = (float) Math.PI / l;
        double[] vector = sphere;
        for (int k = 0; k < s; k++) {
            double yaw = Math.toRadians((k + 1) * (double) meridianRotation);
            double cos = Math.cos(yaw), sin = Math.sin(yaw);
            vector[0] = 0D;
            vector[1] = radius;
            vector[2] = 0D;
            for (int i = 0; i < l; i++) {
                double px = vector[0], py = vector[1], pz = vector[2];
                rotateX(vector, latitudeRotation);
                segment(
                        px * cos + pz * sin,
                        py,
                        pz * cos - px * sin,
                        vector[0] * cos + vector[2] * sin,
                        vector[1],
                        vector[2] * cos - vector[0] * sin,
                        color);
            }
        }
        float parallelRotation = (float) Math.PI * 2F / s;
        vector[0] = 0D;
        vector[1] = radius;
        vector[2] = 0D;
        for (int k = 0; k < l; k++) {
            rotateZ(vector, latitudeRotation);
            for (int i = 0; i < s; i++) {
                double px = vector[0], py = vector[1], pz = vector[2];
                rotateY(vector, parallelRotation);
                segment(px, py, pz, vector[0], vector[1], vector[2], color);
            }
        }
    }

    private void segment(
            double x0, double y0, double z0, double x1, double y1, double z1, int color) {
        while (lines.size() <= usedLines)
            lines.add(
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LINE_MODEL)
                            .createInstance());
        Vector3f a = shellBasePose.transformPosition(from.set((float) x0, (float) y0, (float) z0));
        Vector3f b = shellBasePose.transformPosition(to.set((float) x1, (float) y1, (float) z1));
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        TransformedInstance line = lines.get(usedLines++);
        line.setVisible(length > 0F);
        if (length <= 0F) return;
        rotation.rotationTo(0F, 1F, 0F, dx / length, dy / length, dz / length);
        linePose.translation(
                        pos.getX() - renderOrigin().getX() + a.x,
                        pos.getY() - renderOrigin().getY() + a.y,
                        pos.getZ() - renderOrigin().getZ() + a.z)
                .rotate(rotation)
                .scale(length);
        line.setTransform(linePose).colorArgb(color).light(lastLineLight).setChanged();
    }

    private void hideLines() {
        for (int i = 0; i < usedLines; i++) lines.get(i).setVisible(false);
        usedLines = 0;
    }

    @Override
    protected void trackExtent() {
        if (blockEntity.radius != boundsRadius) {
            boundsRadius = blockEntity.radius;
            refreshVisibleBounds();
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return litBounds;
    }

    @Override
    protected AABB visibleBounds() {
        return litBounds.minmax(new AABB(pos).inflate(boundsRadius + 2D));
    }

    @Override
    public void updateLight(float partialTick) {
        lineLight = LightCoordsUtil.getLightCoords(level, pos);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : topInstances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (var instance : topInstances) instance.delete();
        for (var line : lines) line.delete();
    }
}
