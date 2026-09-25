// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.RenderTextures;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
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
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKCraneConsoleVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityCraneConsole> implements ShaderLightVisual {
    private static final int JOYSTICK = ResourceManager.rbmk_crane_console.partId("Joystick");
    private static final int METER1 = ResourceManager.rbmk_crane_console.partId("Meter1");
    private static final int METER2 = ResourceManager.rbmk_crane_console.partId("Meter2");
    private static final int LAMP1 = ResourceManager.rbmk_crane_console.partId("Lamp1");
    private static final int LAMP2 = ResourceManager.rbmk_crane_console.partId("Lamp2");
    private static final int GIRDER = ResourceManager.rbmk_crane.partId("Girder");
    private static final int MAIN = ResourceManager.rbmk_crane.partId("Main");
    private static final int TUBE = ResourceManager.rbmk_crane.partId("Tube");
    private static final int CARRIAGE = ResourceManager.rbmk_crane.partId("Carriage");
    private static final int LIFT = ResourceManager.rbmk_crane.partId("Lift");
    private static final int LOADING = ARGB.colorFromFloat(1F, .8F, .8F, 0F);
    private static final int LOADED = ARGB.colorFromFloat(1F, 0F, 1F, 0F);
    private static final int UNLOADED = ARGB.colorFromFloat(1F, 0F, .1F, 0F);
    private static final int VALID = ARGB.colorFromFloat(1F, 0F, 1F, 0F);
    private static final int INVALID = ARGB.colorFromFloat(1F, 1F, 0F, 0F);
    private static final Material LAMP_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(RenderTextures.WHITE)
                    .mipmap(false)
                    .cutout(CutoutShaders.OFF)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final Material CONSOLE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.rbmk_crane_console_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final Material CRANE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.rbmk_crane_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .useLight(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart[] CONSOLE_PARTS = buildConsoleParts();
    private static final Model LAMP1_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(ResourceManager.rbmk_crane_console.groups[LAMP1], true),
                    LAMP_MATERIAL);
    private static final Model LAMP2_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(ResourceManager.rbmk_crane_console.groups[LAMP2], true),
                    LAMP_MATERIAL);
    private static final MeshPart[] CRANE_PARTS = buildCraneParts();
    private final TransformedInstance[] consoleInstances = new TransformedInstance[5];
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f joystickPose = new Matrix4f();
    private final Matrix4f meter1Pose = new Matrix4f();
    private final Matrix4f meter2Pose = new Matrix4f();
    private final Matrix4f craneRootPose = new Matrix4f();
    private final Matrix4f girderPose = new Matrix4f();
    private final Matrix4f tubePose = new Matrix4f();
    private final Matrix4f liftPose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private int lastFacing = Integer.MIN_VALUE;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterY = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;
    private int lastCraneRotation = Integer.MIN_VALUE;
    private CraneSet crane;
    private AABB renderBounds;
    private boolean extentSetUp;
    private int extentCenterX, extentCenterY, extentCenterZ, extentReach, extentHeight;
    private @Nullable AABB lastLightBounds;
    private long lastTime = Long.MIN_VALUE;
    private float lastTiltFront = Float.NaN;
    private float lastTiltLeft = Float.NaN;
    private double lastMeter1 = Double.NaN;
    private double lastMeter2 = Double.NaN;
    private int lastLamp1 = Integer.MIN_VALUE;
    private int lastLamp2 = Integer.MIN_VALUE;
    private float lastFront = Float.NaN;
    private float lastLeft = Float.NaN;
    private float lastProgress = Float.NaN;
    private int lastGirders = Integer.MIN_VALUE;
    private int lastTubes = Integer.MIN_VALUE;
    private boolean lastSetUp;
    private boolean initialized;

    public RBMKCraneConsoleVisual(
            VisualizationContext context, BlockEntityCraneConsole blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90)
                                        * Mth.DEG_TO_RAD)
                        .translate(.5F, 0F, 0F);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.rbmk_crane_console,
                                        "Console_Coonsole",
                                        rawBodyLocal,
                                        pos));
        consoleInstances[0] =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CONSOLE_PARTS[0].model())
                        .createInstance();
        consoleInstances[1] =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CONSOLE_PARTS[1].model())
                        .createInstance();
        consoleInstances[2] =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CONSOLE_PARTS[2].model())
                        .createInstance();
        consoleInstances[3] =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LAMP1_MODEL)
                        .createInstance();
        consoleInstances[4] =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LAMP2_MODEL)
                        .createInstance();
        trackExtent();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] buildConsoleParts() {
        var source = ResourceManager.rbmk_crane_console;
        return new MeshPart[] {
            MeshPart.obj(source.groups[JOYSTICK], true, CONSOLE_MATERIAL),
            MeshPart.obj(source.groups[METER1], true, CONSOLE_MATERIAL),
            MeshPart.obj(source.groups[METER2], true, CONSOLE_MATERIAL)
        };
    }

    private static MeshPart[] buildCraneParts() {
        var source = ResourceManager.rbmk_crane;
        return new MeshPart[] {
            MeshPart.obj(source.groups[GIRDER], true, CRANE_MATERIAL),
            MeshPart.obj(source.groups[MAIN], true, CRANE_MATERIAL),
            MeshPart.obj(source.groups[TUBE], true, CRANE_MATERIAL),
            MeshPart.obj(source.groups[CARRIAGE], true, CRANE_MATERIAL),
            MeshPart.obj(source.groups[LIFT], true, CRANE_MATERIAL)
        };
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int facingKey =
                BlockMultiblockCore.coreFacing(blockEntity.getBlockState()).get3DDataValue();
        float facing = Facing.yaw(BlockMultiblockCore.coreFacing(blockEntity.getBlockState()), 90);
        float tiltFront =
                (float) Mth.lerp(partialTick, blockEntity.lastTiltFront, blockEntity.tiltFront);
        float tiltLeft =
                (float) Mth.lerp(partialTick, blockEntity.lastTiltLeft, blockEntity.tiltLeft);
        long time = GameTime.now();
        double meter1Angle =
                Math.sin(time * .01D % 360D) * 180D / Math.PI * .05D
                        + 135D
                        - 270D * blockEntity.loadedHeat;
        double meter2Angle =
                Math.sin(time * .01D % 360D) * 180D / Math.PI * .05D
                        + 135D
                        - 270D * blockEntity.loadedEnrichment;
        int lamp1 =
                blockEntity.isCraneLoading()
                        ? LOADING
                        : blockEntity.hasItemLoaded() ? LOADED : UNLOADED;
        int lamp2 = blockEntity.isAboveValidTarget() ? VALID : INVALID;
        boolean setUp = blockEntity.setUpCrane;
        int span =
                blockEntity.craneRotationOffset == 90 || blockEntity.craneRotationOffset == 270
                        ? blockEntity.spanL + blockEntity.spanR + 1
                        : blockEntity.spanF + blockEntity.spanB + 1;
        int girderSpan = Math.max(0, span);
        int tubeCount = Math.max(0, blockEntity.height - 6);
        float front = (float) Mth.lerp(partialTick, blockEntity.lastPosFront, blockEntity.posFront);
        float left = (float) Mth.lerp(partialTick, blockEntity.lastPosLeft, blockEntity.posLeft);
        float progress =
                (float) Mth.lerp(partialTick, blockEntity.lastProgress, blockEntity.progress);
        boolean facingChanged = !initialized || facingKey != lastFacing;
        boolean centerChanged =
                !initialized
                        || blockEntity.centerX != lastCenterX
                        || blockEntity.centerY != lastCenterY
                        || blockEntity.centerZ != lastCenterZ;
        boolean rotationChanged =
                !initialized || blockEntity.craneRotationOffset != lastCraneRotation;
        boolean structureChanged =
                !initialized
                        || setUp != lastSetUp
                        || girderSpan != lastGirders
                        || tubeCount != lastTubes;
        boolean rootChanged =
                structureChanged
                        || facingChanged
                        || centerChanged
                        || rotationChanged
                        || front != lastFront
                        || left != lastLeft;
        boolean liftChanged = rootChanged || Float.compare(progress, lastProgress) != 0;
        boolean consoleChanged =
                !initialized
                        || Float.compare(tiltFront, lastTiltFront) != 0
                        || Float.compare(tiltLeft, lastTiltLeft) != 0;
        boolean metersChanged =
                !initialized
                        || Double.compare(meter1Angle, lastMeter1) != 0
                        || Double.compare(meter2Angle, lastMeter2) != 0;
        boolean lampsChanged = !initialized || lamp1 != lastLamp1 || lamp2 != lastLamp2;
        if (initialized
                && !rootChanged
                && !liftChanged
                && !consoleChanged
                && !metersChanged
                && !lampsChanged
                && time == lastTime) return;
        lastFacing = facingKey;
        lastCenterX = blockEntity.centerX;
        lastCenterY = blockEntity.centerY;
        lastCenterZ = blockEntity.centerZ;
        lastCraneRotation = blockEntity.craneRotationOffset;
        lastFront = front;
        lastLeft = left;
        lastProgress = progress;
        lastGirders = girderSpan;
        lastTubes = tubeCount;
        lastSetUp = setUp;
        lastTime = time;
        lastTiltFront = tiltFront;
        lastTiltLeft = tiltLeft;
        lastMeter1 = meter1Angle;
        lastMeter2 = meter2Angle;
        lastLamp1 = lamp1;
        lastLamp2 = lamp2;
        initialized = true;

        if (facingChanged)
            rootPose.identity()
                    .translate(.5F, 0F, .5F)
                    .rotateY(facing * Mth.DEG_TO_RAD)
                    .translate(.5F, 0F, 0F);
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        if (consoleChanged) {
            joystickPose
                    .set(rootPose)
                    .translate(.75F, 1F, 0F)
                    .rotateZ(tiltFront * Mth.DEG_TO_RAD)
                    .rotateX(tiltLeft * Mth.DEG_TO_RAD)
                    .translate(-.75F, -1.015F, 0F);
            writePhysical(0, CONSOLE_PARTS[0], joystickPose);
        }
        if (metersChanged) {
            meter(meter1Pose, rootPose, .75F, meter1Angle);
            meter(meter2Pose, rootPose, .25F, meter2Angle);
            writePhysical(1, CONSOLE_PARTS[1], meter1Pose);
            writePhysical(2, CONSOLE_PARTS[2], meter2Pose);
        }
        if (lampsChanged) {
            writeLamp(3, rootPose, lamp1);
            writeLamp(4, rootPose, lamp2);
        }

        if (!setUp) {
            if (crane != null) {
                crane.delete();
                crane = null;
            }
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
            return;
        }

        if (crane == null || crane.girders != girderSpan || crane.tubes != tubeCount) {
            if (crane != null) crane.delete();
            crane = new CraneSet(girderSpan, tubeCount, this);
            structureChanged = true;
            rootChanged = true;
            liftChanged = true;
        }
        if (rootChanged) {
            craneRootPose
                    .identity()
                    .translate(.5F, -1F, .5F)
                    .translate(
                            blockEntity.centerX - pos.getX(),
                            blockEntity.centerY - pos.getY() + 1F,
                            blockEntity.centerZ - pos.getZ())
                    .rotateY(facing * Mth.DEG_TO_RAD)
                    .translate(-front, 0F, left)
                    .rotateY(blockEntity.craneRotationOffset * Mth.DEG_TO_RAD);
            girderPose
                    .set(craneRootPose)
                    .rotateY(-blockEntity.craneRotationOffset * Mth.DEG_TO_RAD);
            switch (blockEntity.craneRotationOffset) {
                case 90 -> girderPose.translate(0F, 0F, -left - blockEntity.spanR);
                case 180 -> girderPose.translate(front - blockEntity.spanF, 0F, 0F);
                case 270 -> girderPose.translate(0F, 0F, -left + blockEntity.spanL);
                default -> girderPose.translate(front + blockEntity.spanB, 0F, 0F);
            }
            girderPose.rotateY(blockEntity.craneRotationOffset * Mth.DEG_TO_RAD);
            int slot = 0;
            for (int i = 0; i < girderSpan; i++) {
                writeCrane(slot++, girderPose);
                girderPose.translate(-1F, 0F, 0F);
            }
            writeCrane(slot++, craneRootPose);
            tubePose.set(craneRootPose);
            for (int i = 0; i < tubeCount; i++) {
                writeCrane(slot++, tubePose);
                tubePose.translate(0F, 1F, 0F);
            }
            tubePose.translate(0F, -1F, 0F);
            writeCrane(slot++, tubePose);
        }
        if (liftChanged) {
            int slot = girderSpan + tubeCount + 2;
            liftPose.set(craneRootPose).translate(0F, (float) (-3.25D * (1D - progress)), 0F);
            writeCrane(slot, liftPose);
        }
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private Matrix4f meter(Matrix4f output, Matrix4f root, float z, double angle) {
        return output.set(root)
                .translate(0F, 1.25F, z)
                .rotateX((float) angle * Mth.DEG_TO_RAD)
                .translate(0F, -1.25F, -z);
    }

    private void writePhysical(int index, MeshPart part, Matrix4f local) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        consoleInstances[index].setTransform(world).light(0).colorArgb(-1).setChanged();
        LightBounds.includeLightBounds(lightBoundsAccumulator, part.model(), local, pos);
    }

    private void writeLamp(int index, Matrix4f local, int color) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        consoleInstances[index]
                .setTransform(world)
                .light(LightCoordsUtil.FULL_BRIGHT)
                .colorArgb(color)
                .setChanged();
    }

    @Override
    protected void trackExtent() {
        boolean setUp = blockEntity.setUpCrane;
        int reach = blockEntity.spanF + blockEntity.spanB + blockEntity.spanL + blockEntity.spanR;
        if (renderBounds != null
                && setUp == extentSetUp
                && blockEntity.centerX == extentCenterX
                && blockEntity.centerY == extentCenterY
                && blockEntity.centerZ == extentCenterZ
                && reach == extentReach
                && blockEntity.height == extentHeight) return;
        extentSetUp = setUp;
        extentCenterX = blockEntity.centerX;
        extentCenterY = blockEntity.centerY;
        extentCenterZ = blockEntity.centerZ;
        extentReach = reach;
        extentHeight = blockEntity.height;
        renderBounds = setUp ? getRenderBoundsForEntity(reach) : new AABB(pos).inflate(1);
        refreshVisibleBounds();
    }

    private AABB getRenderBoundsForEntity(int reach) {
        AABB box = new AABB(pos);
        AABB gantry =
                new AABB(
                                new BlockPos(
                                        blockEntity.centerX,
                                        blockEntity.centerY,
                                        blockEntity.centerZ))
                        .inflate(reach, 0D, reach)
                        .expandTowards(0D, blockEntity.height - 6, 0D)
                        .expandTowards(0D, -3.25D, 0D);
        return box.minmax(gantry).inflate(1D);
    }

    private void writeCrane(int index, Matrix4f local) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        crane.instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    protected AABB visibleBounds() {
        return rawBodyBounds.minmax(renderBounds);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int i = 0; i < CONSOLE_PARTS.length; i++) consumer.accept(consoleInstances[i]);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : consoleInstances) instance.delete();
        if (crane != null) crane.delete();
    }

    private static final class CraneSet {
        private final int girders;
        private final int tubes;
        private final TransformedInstance[] instances;

        private CraneSet(int girders, int tubes, RBMKCraneConsoleVisual owner) {
            this.girders = girders;
            this.tubes = tubes;
            instances = new TransformedInstance[girders + tubes + 3];
            for (int i = 0; i < instances.length; i++) {
                MeshPart part =
                        i < girders
                                ? CRANE_PARTS[0]
                                : i == girders
                                        ? CRANE_PARTS[1]
                                        : i < girders + 1 + tubes
                                                ? CRANE_PARTS[2]
                                                : i == girders + 1 + tubes
                                                        ? CRANE_PARTS[3]
                                                        : CRANE_PARTS[4];
                instances[i] =
                        owner.instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, part.model())
                                .createInstance();
            }
        }

        private void delete() {
            for (TransformedInstance instance : instances) instance.delete();
        }
    }
}
