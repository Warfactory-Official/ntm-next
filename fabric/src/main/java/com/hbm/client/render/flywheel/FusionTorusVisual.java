// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FusionTorusVisual extends HbmDynamicBlockEntityVisual<BlockEntityFusionTorus>
        implements ShaderLightVisual {
    private static final int BOLT_COUNT = 4;
    private static final double EXTRA_LAYER_RANGE_SQ = 100D * 100D;
    private static final MeshPart MAGNET_PART =
            MeshPart.obj(
                    ResourceManager.fusion_torus
                            .groups[ResourceManager.fusion_torus.partId("Magnet")],
                    ResourceManager.fusion_torus.smoothing(),
                    MeshPart.litCutout(ResourceManager.fusion_torus_tex));
    private static final MeshPart[] BOLT_PARTS = buildBoltParts();
    private static final MeshPart PLASMA_PART =
            MeshPart.obj(
                    ResourceManager.fusion_torus
                            .groups[ResourceManager.fusion_torus.partId("Plasma")],
                    ResourceManager.fusion_torus.smoothing(),
                    plasma(ResourceManager.fusion_plasma_tex));
    private static final MeshPart GLOW_PART =
            MeshPart.obj(
                    ResourceManager.fusion_torus
                            .groups[ResourceManager.fusion_torus.partId("Plasma")],
                    ResourceManager.fusion_torus.smoothing(),
                    plasma(ResourceManager.fusion_plasma_glow_tex));
    private static final MeshPart SPARKLE_PART =
            MeshPart.obj(
                    ResourceManager.fusion_torus
                            .groups[ResourceManager.fusion_torus.partId("Plasma")],
                    ResourceManager.fusion_torus.smoothing(),
                    plasma(ResourceManager.fusion_plasma_sparkle_tex));
    private final TransformedInstance magnet;
    private final TransformedInstance[] bolts = new TransformedInstance[BOLT_COUNT];
    private final UvTransformedInstance plasma;
    private final UvTransformedInstance glow;
    private final UvTransformedInstance sparkle;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private final double[] lightBounds = new double[6];
    private final Matrix4f fixedBasePose = new Matrix4f();
    private @Nullable AABB lastLightBounds;
    private float lastMagnetAngle = Float.NaN;
    private int lastConnectionMask = -1;
    private boolean lastBurning;
    private boolean lastNearby;
    private boolean initialized;
    private boolean plasmaShown, glowShown, sparkleShown;

    public FusionTorusVisual(
            VisualizationContext context, BlockEntityFusionTorus blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal = new Matrix4f().translation(.5F, 0F, .5F);
        if (blockEntity.isTilted())
            rawBodyLocal
                    .translate(0F, -1F, 0F)
                    .rotateZ((10F) * Mth.DEG_TO_RAD)
                    .rotateY((5F) * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.fusion_torus, "Torus", rawBodyLocal, pos));
        fixedBasePose.set(rawBodyLocal);
        magnet =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MAGNET_PART.model())
                        .createInstance();
        for (int i = 0; i < BOLT_COUNT; i++)
            bolts[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, BOLT_PARTS[i].model())
                            .createInstance();
        plasma =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, PLASMA_PART.model())
                        .createInstance();
        glow =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, GLOW_PART.model())
                        .createInstance();
        sparkle =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, SPARKLE_PART.model())
                        .createInstance();
        writeFrame(partialTick, null);
    }

    private static MeshPart[] buildBoltParts() {
        int[] ids = ResourceManager.fusion_torus.partIds("Bolts2", "Bolts4", "Bolts3", "Bolts1");
        var parts = new MeshPart[BOLT_COUNT];
        var material = MeshPart.litCutout(ResourceManager.fusion_torus_tex);
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(
                            ResourceManager.fusion_torus.groups[ids[i]],
                            ResourceManager.fusion_torus.smoothing(),
                            material);
        return parts;
    }

    public static void initModels() {}

    private static Material plasma(Identifier texture) {
        return SimpleMaterial.builder()
                .texture(texture)
                .mipmap(false)
                .useLight(false)
                .useOverlay(false)
                .cardinalLightingMode(CardinalLightingMode.OFF)
                .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                .writeMask(WriteMask.COLOR)
                .backfaceCulling(false)
                .build();
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static int color(float r, float g, float b, float a) {
        return ARGB.colorFromFloat(
                Mth.clamp(a, 0F, 1F),
                Mth.clamp(r, 0F, 1F),
                Mth.clamp(g, 0F, 1F),
                Mth.clamp(b, 0F, 1F));
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick(), context.camera().position());
    }

    private void writeFrame(float partialTick, @Nullable Vec3 camera) {
        float magnetAngle = Mth.lerp(partialTick, blockEntity.prevMagnet, blockEntity.magnet);
        FusionRecipe recipe = blockEntity.module.getRecipe() instanceof FusionRecipe f ? f : null;
        boolean burning = blockEntity.plasmaEnergy > 0 && recipe != null;
        int connectionMask = 0;
        for (int i = 0; i < BOLT_COUNT; i++)
            if (blockEntity.connections[i]) connectionMask |= 1 << i;
        boolean magnetChanged = !initialized || magnetAngle != lastMagnetAngle;
        boolean connectionsChanged = !initialized || connectionMask != lastConnectionMask;
        if (magnetChanged) {
            localPose.set(fixedBasePose).rotateY((magnetAngle) * Mth.DEG_TO_RAD);
            writeBody(magnet, localPose);
            lastMagnetAngle = magnetAngle;
        }
        if (connectionsChanged || magnetChanged) {
            LightBounds.resetBounds(lightBounds, rawBodyBounds);
            LightBounds.includeLightBounds(lightBounds, MAGNET_PART.model(), localPose, pos);
            for (int i = 0; i < BOLT_COUNT; i++) {
                writeBolt(i, blockEntity.connections[i], fixedBasePose);
                if (blockEntity.connections[i])
                    LightBounds.includeLightBounds(
                            lightBounds, BOLT_PARTS[i].model(), fixedBasePose, pos);
            }
            lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
            lastConnectionMask = connectionMask;
        }
        boolean nearby = camera == null || distanceToCamera(camera) < EXTRA_LAYER_RANGE_SQ;
        long time = GameTime.now() + Math.floorMod(pos.hashCode(), 30_000);
        if (!burning) {
            if (lastBurning || !initialized) {
                plasma.setVisible(false);
                glow.setVisible(false);
                sparkle.setVisible(false);
                plasmaShown = glowShown = sparkleShown = false;
            }
            lastBurning = false;
            lastNearby = nearby;
            initialized = true;
            return;
        }
        float alpha = .35F + (float) (Math.sin(time / 1000D) * .25D);
        writePlasma(
                plasma,
                fixedBasePose,
                color(recipe.r, recipe.g, recipe.b, alpha),
                0F,
                (float) (sps(time / 1000D) % 1D),
                !plasmaShown);
        plasmaShown = true;
        if (nearby) {
            writePlasma(
                    glow,
                    fixedBasePose,
                    color(recipe.r * 2F, recipe.g * 2F, recipe.b * 2F, alpha * 2F),
                    0F,
                    (float) (Math.sin(time / 2000D) % 1D + time / 10000D % 1D),
                    !glowShown);
            writePlasma(
                    sparkle,
                    fixedBasePose,
                    color(recipe.r * 2F, recipe.g * 2F, recipe.b * 2F, .75F),
                    (float) (time / 500D * -1 % 1D),
                    (float) (Math.sin(time / 1000D) * .5D % 1D),
                    !sparkleShown);
            glowShown = sparkleShown = true;
        } else {
            if (lastNearby || !initialized) {
                glow.setVisible(false);
                sparkle.setVisible(false);
                glowShown = sparkleShown = false;
            }
        }
        lastBurning = true;
        lastNearby = nearby;
        initialized = true;
    }

    private double distanceToCamera(Vec3 camera) {
        double dx = pos.getX() + .5D - camera.x;
        double dy = pos.getY() + 2.5D - camera.y;
        double dz = pos.getZ() + .5D - camera.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void writeBody(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    private void writeBolt(int index, boolean visible, Matrix4f pose) {
        TransformedInstance instance = bolts[index];
        instance.setVisible(visible);
        if (!visible) return;
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    private void writePlasma(
            UvTransformedInstance instance,
            Matrix4f pose,
            int color,
            float u,
            float v,
            boolean transform) {
        instance.setVisible(true);
        if (transform) {
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(pose);
            instance.setTransform(instancePose);
        }
        instance.colorArgb(color).light(0);
        instance.uvRegion(u, v, 1F, 1F).setChanged();
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
        consumer.accept(magnet);
        for (var bolt : bolts) consumer.accept(bolt);
    }

    @Override
    protected void _delete() {
        magnet.delete();
        for (var bolt : bolts) bolt.delete();
        plasma.delete();
        glow.delete();
        sparkle.delete();
    }
}
