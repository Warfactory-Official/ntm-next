// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineExcavator;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ExcavatorVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineExcavator>
        implements ShaderLightVisual {
    private static final float BODY_LIFT = -3F;
    private static final double CRUSHER1_PIVOT_Z = 2.8125D;
    private static final double CRUSHER2_PIVOT_Z = 2.1875D;
    private static final double CRUSHER_PIVOT_Y = 2D;
    private static final double SHAFT_STEP = 2D;
    private static final double SHAFT_END = -1.5D;
    private static final int MAX_SHAFTS = 256;
    private static final double SCROLL_PERIOD = 250D;
    private static final HFRWavefrontObject MODEL = ResourceManager.mining_drill;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.mining_drill_tex);
    private static final MeshPart CRUSHER1_PART = part("Crusher1");
    private static final MeshPart CRUSHER2_PART = part("Crusher2");
    private static final MeshPart DRILLBIT_PART = part("Drillbit");
    private static final MeshPart SHAFT_PART = part("Shaft");
    private static final MeshPart CHUTE_TOP =
            chute(
                    BODY_MATERIAL,
                    ResourceManager.mining_drill_cobble_tex,
                    .125F,
                    .125F,
                    3F,
                    2F,
                    1F,
                    1F);
    private static final MeshPart CHUTE_COBBLE =
            chute(
                    BODY_MATERIAL,
                    ResourceManager.mining_drill_cobble_tex,
                    .25F,
                    .0625F,
                    2F,
                    1F,
                    2F,
                    .5F);
    private static final MeshPart CHUTE_GRAVEL =
            chute(
                    MeshPart.litCutout(ResourceManager.mining_drill_gravel_tex),
                    ResourceManager.mining_drill_gravel_tex,
                    .5F,
                    .0625F,
                    2F,
                    1F,
                    4F,
                    .5F);
    private final AABB bodyBounds;
    private final TransformedInstance crusher1;
    private final TransformedInstance crusher2;
    private final TransformedInstance drillbit;
    private final TransformedInstance[] shafts = new TransformedInstance[MAX_SHAFTS];
    private final UvTransformedInstance chuteTop;
    private final UvTransformedInstance chuteCobble;
    private final UvTransformedInstance chuteGravel;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f crusher1Pose = new Matrix4f();
    private final Matrix4f crusher2Pose = new Matrix4f();
    private final Matrix4f drillPose = new Matrix4f();
    private final Matrix4f shaftPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastDrillRotation = Float.NaN;
    private float lastCrusherRotation = Float.NaN;
    private float lastExtension = Float.NaN;
    private float lastScroll = Float.NaN;
    private int lastShaftCount;
    private int createdShafts;
    private boolean lastTopVisible;
    private boolean lastCobbleVisible;
    private boolean lastGravelVisible;
    private boolean initialized;

    public ExcavatorVisual(
            VisualizationContext context,
            BlockEntityMachineExcavator blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, BODY_LIFT, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Main", basePose, pos);
        crusher1 =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CRUSHER1_PART.model())
                        .createInstance();
        crusher2 =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CRUSHER2_PART.model())
                        .createInstance();
        drillbit =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DRILLBIT_PART.model())
                        .createInstance();
        chuteTop =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, CHUTE_TOP.model())
                        .createInstance();
        chuteCobble =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, CHUTE_COBBLE.model())
                        .createInstance();
        chuteGravel =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, CHUTE_GRAVEL.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart part(String name) {
        return MeshPart.obj(MODEL.groups[MODEL.partId(name)], MODEL.smoothing(), BODY_MATERIAL);
    }

    public static void initModels() {}

    private static MeshPart chute(
            Material material,
            Identifier texture,
            float widthX,
            float widthZ,
            float top,
            float bottom,
            float uEdge,
            float uSide) {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(4);
        face(
                mesh,
                0F,
                0F,
                1F,
                widthX,
                top,
                2.5F + widthZ,
                0F,
                0F,
                -widthX,
                top,
                2.5F + widthZ,
                uEdge,
                0F,
                -widthX,
                bottom,
                2.5F + widthZ,
                uEdge,
                4F,
                widthX,
                bottom,
                2.5F + widthZ,
                0F,
                4F);
        face(
                mesh,
                0F,
                0F,
                -1F,
                -widthX,
                top,
                2.5F - widthZ,
                uEdge,
                0F,
                widthX,
                top,
                2.5F - widthZ,
                0F,
                0F,
                widthX,
                bottom,
                2.5F - widthZ,
                0F,
                4F,
                -widthX,
                bottom,
                2.5F - widthZ,
                uEdge,
                4F);
        face(
                mesh,
                -1F,
                0F,
                0F,
                -widthX,
                top,
                2.5F + widthZ,
                0F,
                0F,
                -widthX,
                top,
                2.5F - widthZ,
                uSide,
                0F,
                -widthX,
                bottom,
                2.5F - widthZ,
                uSide,
                4F,
                -widthX,
                bottom,
                2.5F + widthZ,
                0F,
                4F);
        face(
                mesh,
                1F,
                0F,
                0F,
                widthX,
                top,
                2.5F - widthZ,
                uSide,
                0F,
                widthX,
                top,
                2.5F + widthZ,
                0F,
                0F,
                widthX,
                bottom,
                2.5F + widthZ,
                0F,
                4F,
                widthX,
                bottom,
                2.5F - widthZ,
                uSide,
                4F);
        PackedQuadMesh built = mesh.build();
        return MeshPart.create(built, SimpleMaterial.builderOf(material).texture(texture).build());
    }

    private static void face(
            PackedQuadMesh.Builder mesh,
            float nx,
            float ny,
            float nz,
            double x0,
            double y0,
            double z0,
            double u0,
            double v0,
            double x1,
            double y1,
            double z1,
            double u1,
            double v1,
            double x2,
            double y2,
            double z2,
            double u2,
            double v2,
            double x3,
            double y3,
            double z3,
            double u3,
            double v3) {
        mesh.normal(nx, ny, nz)
                .vertex(x0, y0, z0, u0, v0, -1)
                .vertex(x1, y1, z1, u1, v1, -1)
                .vertex(x2, y2, z2, u2, v2, -1)
                .vertex(x3, y3, z3, u3, v3, -1);
    }

    private static int shaftCount(float extension) {
        if (extension < SHAFT_END) return 0;
        return Math.min(MAX_SHAFTS, (int) Math.floor((extension - SHAFT_END) / SHAFT_STEP) + 1);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float drillRotation =
                blockEntity.prevDrillRotation
                        + (blockEntity.drillRotation - blockEntity.prevDrillRotation) * partialTick;
        float crusherRotation =
                blockEntity.prevCrusherRotation
                        + (blockEntity.crusherRotation - blockEntity.prevCrusherRotation)
                                * partialTick;
        float extension =
                blockEntity.prevDrillExtension
                        + (blockEntity.drillExtension - blockEntity.prevDrillExtension)
                                * partialTick;
        boolean drillChanged =
                !initialized
                        || Float.floatToIntBits(drillRotation)
                                != Float.floatToIntBits(lastDrillRotation);
        boolean crusherChanged =
                !initialized
                        || Float.floatToIntBits(crusherRotation)
                                != Float.floatToIntBits(lastCrusherRotation);
        boolean extensionChanged =
                !initialized
                        || Float.floatToIntBits(extension) != Float.floatToIntBits(lastExtension);
        int shaftCount = extensionChanged ? shaftCount(extension) : lastShaftCount;
        boolean shaftChanged = drillChanged || extensionChanged || shaftCount != lastShaftCount;
        boolean topVisible = blockEntity.chuteTimer > 0;
        boolean cobbleVisible = topVisible && !blockEntity.enableCrusher;
        boolean gravelVisible = topVisible && blockEntity.enableCrusher;
        boolean topChanged = !initialized || topVisible != lastTopVisible;
        boolean cobbleChanged = !initialized || cobbleVisible != lastCobbleVisible;
        boolean gravelChanged = !initialized || gravelVisible != lastGravelVisible;
        float scroll = -(GameTime.now() % (long) SCROLL_PERIOD) / (float) SCROLL_PERIOD;
        boolean scrollChanged =
                !initialized || Float.floatToIntBits(scroll) != Float.floatToIntBits(lastScroll);
        if (!drillChanged
                && !crusherChanged
                && !shaftChanged
                && !topChanged
                && !cobbleChanged
                && !gravelChanged
                && !scrollChanged) return;

        if (crusherChanged) {
            crusher1Pose
                    .set(basePose)
                    .translate(0F, (float) CRUSHER_PIVOT_Y, (float) CRUSHER1_PIVOT_Z)
                    .rotateX((-crusherRotation) * Mth.DEG_TO_RAD)
                    .translate(0F, (float) -CRUSHER_PIVOT_Y, (float) -CRUSHER1_PIVOT_Z);
            write(crusher1, crusher1Pose);
            crusher2Pose
                    .set(basePose)
                    .translate(0F, (float) CRUSHER_PIVOT_Y, (float) CRUSHER2_PIVOT_Z)
                    .rotateX((crusherRotation) * Mth.DEG_TO_RAD)
                    .translate(0F, (float) -CRUSHER_PIVOT_Y, (float) -CRUSHER2_PIVOT_Z);
            write(crusher2, crusher2Pose);
        }
        if (drillChanged) {
            drillPose
                    .set(basePose)
                    .rotateY(-(drillRotation) * Mth.DEG_TO_RAD)
                    .translate(0F, -extension, 0F);
            write(drillbit, drillPose);
        }
        if (shaftChanged) {
            shaftPose.set(drillPose);
            for (int i = 0; i < shaftCount; i++) {
                if (i == createdShafts) {
                    shafts[i] =
                            instancerProvider()
                                    .instancer(InstanceTypes.TRANSFORMED, SHAFT_PART.model())
                                    .createInstance();
                    createdShafts++;
                }
                shafts[i].setVisible(true);
                write(shafts[i], shaftPose);
                shaftPose.translate(0F, (float) SHAFT_STEP, 0F);
            }
            for (int i = shaftCount; i < createdShafts; i++) {
                if (initialized && i >= lastShaftCount) break;
                shafts[i].setVisible(false);
            }
        }
        if (topChanged || (topVisible && scrollChanged))
            writeChute(chuteTop, topVisible, scroll, basePose, topChanged, scrollChanged);
        if (cobbleChanged || (cobbleVisible && scrollChanged))
            writeChute(chuteCobble, cobbleVisible, scroll, basePose, cobbleChanged, scrollChanged);
        if (gravelChanged || (gravelVisible && scrollChanged))
            writeChute(chuteGravel, gravelVisible, scroll, basePose, gravelChanged, scrollChanged);

        boolean boundsChanged =
                crusherChanged
                        || drillChanged
                        || shaftChanged
                        || topChanged
                        || cobbleChanged
                        || gravelChanged;
        if (boundsChanged) {
            LightBounds.resetBounds(lightBounds, bodyBounds);
            LightBounds.includeLightBounds(lightBounds, CRUSHER1_PART.model(), crusher1Pose, pos);
            LightBounds.includeLightBounds(lightBounds, CRUSHER2_PART.model(), crusher2Pose, pos);
            LightBounds.includeLightBounds(lightBounds, DRILLBIT_PART.model(), drillPose, pos);
            shaftPose.set(drillPose);
            for (int i = 0; i < shaftCount; i++) {
                LightBounds.includeLightBounds(lightBounds, SHAFT_PART.model(), shaftPose, pos);
                shaftPose.translate(0F, (float) SHAFT_STEP, 0F);
            }
            if (topVisible) {
                LightBounds.includeLightBounds(lightBounds, CHUTE_TOP.model(), basePose, pos);
                LightBounds.includeLightBounds(
                        lightBounds,
                        (gravelVisible ? CHUTE_GRAVEL : CHUTE_COBBLE).model(),
                        basePose,
                        pos);
            }
            lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        }
        lastDrillRotation = drillRotation;
        lastCrusherRotation = crusherRotation;
        lastExtension = extension;
        lastShaftCount = shaftCount;
        lastTopVisible = topVisible;
        lastCobbleVisible = cobbleVisible;
        lastGravelVisible = gravelVisible;
        lastScroll = scroll;
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    private void writeChute(
            UvTransformedInstance instance,
            boolean visible,
            float scroll,
            Matrix4f pose,
            boolean poseChanged,
            boolean scrollChanged) {
        if (!visible) {
            instance.setVisible(false);
            return;
        }
        if (poseChanged) {
            instance.setVisible(true);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(pose);
            instance.setTransform(instancePose).light(0);
        }
        if (poseChanged || scrollChanged) {
            instance.uvRegion(0F, scroll, 1F, 1F).setChanged();
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                level.getMinY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 5,
                                pos.getZ() + 4)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(crusher1);
        consumer.accept(crusher2);
        consumer.accept(drillbit);
        for (int i = 0; i < createdShafts; i++) consumer.accept(shafts[i]);
        consumer.accept(chuteTop);
        consumer.accept(chuteCobble);
        consumer.accept(chuteGravel);
    }

    @Override
    protected void _delete() {
        crusher1.delete();
        crusher2.delete();
        drillbit.delete();
        for (int i = 0; i < createdShafts; i++) shafts[i].delete();
        chuteTop.delete();
        chuteCobble.delete();
        chuteGravel.delete();
    }
}
