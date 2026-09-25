// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretHowardDamaged;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class TurretHowardDamagedVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityTurretHowardDamaged>
        implements ShaderLightVisual {
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_howard_damaged
                            .groups[ResourceManager.turret_howard_damaged.partId("Carriage")],
                    ResourceManager.turret_howard_damaged.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_carriage_ciws_rusted_tex));
    private static final MeshPart BODY_PART =
            MeshPart.obj(
                    ResourceManager.turret_howard_damaged
                            .groups[ResourceManager.turret_howard_damaged.partId("Body")],
                    ResourceManager.turret_howard_damaged.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_howard_rusted_tex));
    private static final MeshPart BARRELS_TOP_PART =
            MeshPart.obj(
                    ResourceManager.turret_howard_damaged
                            .groups[ResourceManager.turret_howard_damaged.partId("BarrelsTop")],
                    ResourceManager.turret_howard_damaged.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_howard_barrels_rusted_tex));
    private static final MeshPart BARRELS_BOTTOM_PART =
            MeshPart.obj(
                    ResourceManager.turret_howard_damaged
                            .groups[ResourceManager.turret_howard_damaged.partId("BarrelsBottom")],
                    ResourceManager.turret_howard_damaged.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_howard_barrels_rusted_tex));

    private final TransformedInstance carriage;
    private final TransformedInstance body;
    private final TransformedInstance barrelsTop;
    private final TransformedInstance barrelsBottom;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f bodyPose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB baseBounds;
    private @Nullable AABB lastLightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastSpin = Float.NaN;
    private boolean initialized;

    public TurretHowardDamagedVisual(
            VisualizationContext context,
            BlockEntityTurretHowardDamaged blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = BlockMultiblockCore.coreFacing(blockEntity.getBlockState());
        float offsetX = facing == Direction.NORTH || facing == Direction.WEST ? 1F : 0F;
        float offsetZ = facing == Direction.NORTH || facing == Direction.EAST ? 1F : 0F;
        placement.translation(offsetX, 0F, offsetZ);
        baseBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", placement, pos);
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        body =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BODY_PART.model())
                        .createInstance();
        barrelsTop =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BARRELS_TOP_PART.model())
                        .createInstance();
        barrelsBottom =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BARRELS_BOTTOM_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float yaw =
                (float)
                        (-Math.toDegrees(
                                        Mth.lerp(
                                                partialTick,
                                                blockEntity.lastRotationYaw,
                                                blockEntity.rotationYaw))
                                - 90D);
        float pitch =
                (float)
                        Math.toDegrees(
                                Mth.lerp(
                                        partialTick,
                                        blockEntity.lastRotationPitch,
                                        blockEntity.rotationPitch));
        float spin = Mth.lerp(partialTick, blockEntity.lastSpin, blockEntity.spin);
        if (initialized
                && Float.compare(yaw, lastYaw) == 0
                && Float.compare(pitch, lastPitch) == 0
                && Float.compare(spin, lastSpin) == 0) return;
        lastYaw = yaw;
        lastPitch = pitch;
        lastSpin = spin;
        initialized = true;
        basePose.set(placement).rotateY((yaw) * Mth.DEG_TO_RAD);
        write(carriage, basePose);
        localPose
                .set(basePose)
                .translate(0F, 2.25F, 0F)
                .rotateZ((pitch) * Mth.DEG_TO_RAD)
                .translate(0F, -2.25F, 0F);
        write(body, localPose);
        bodyPose.set(localPose);
        localPose
                .set(bodyPose)
                .translate(0F, 2.5F, 0F)
                .rotateX(-(spin) * Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F);
        write(barrelsTop, localPose);
        write(barrelsBottom, bodyPose);
        LightBounds.resetBounds(lightBoundsAccumulator, baseBounds);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, CARRIAGE_PART.model(), basePose, pos);
        LightBounds.includeLightBounds(lightBoundsAccumulator, BODY_PART.model(), bodyPose, pos);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, BARRELS_TOP_PART.model(), localPose, pos);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, BARRELS_BOTTOM_PART.model(), bodyPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return baseBounds.minmax(new AABB(pos).inflate(7));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(carriage);
        consumer.accept(body);
        consumer.accept(barrelsTop);
        consumer.accept(barrelsBottom);
    }

    @Override
    protected void _delete() {
        carriage.delete();
        body.delete();
        barrelsTop.delete();
        barrelsBottom.delete();
    }
}
