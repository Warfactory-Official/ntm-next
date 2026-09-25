// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.gui.FluidGauge;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineCrystallizer;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class CrystallizerVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineCrystallizer>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.crystallizer;
    private static final MeshPart SPINNER_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Spinner")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.crystallizer_tex));
    private static final Mesh FLUID_MESH =
            PackedQuadMesh.of(MODEL.groups[MODEL.partId("Fluid")], MODEL.smoothing());
    private static final Map<Fluid, Optional<Model>> FLUID_MODELS = new ConcurrentHashMap<>();
    private final TransformedInstance spinner;
    private final AABB bodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f spinnerPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable Fluid fluidType;
    private @Nullable TransformedInstance fluid;
    private @Nullable AABB lastLightBounds;
    private float lastAngle = Float.NaN;
    private boolean lastMoving;
    private boolean initialized;

    public CrystallizerVisual(
            VisualizationContext context,
            BlockEntityMachineCrystallizer blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Body", basePose, pos);
        spinner =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, SPINNER_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static @Nullable Model fluidModel(Fluid type) {
        return FLUID_MODELS.computeIfAbsent(type, CrystallizerVisual::buildFluidModel).orElse(null);
    }

    private static Optional<Model> buildFluidModel(Fluid type) {
        Identifier sheet = FluidGauge.sheet(type);
        if (sheet == null) return Optional.empty();
        Material material =
                SimpleMaterial.builderOf(MeshPart.litCutout(sheet))
                        .transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .build();
        return Optional.of(new SingleMeshModel(FLUID_MESH, material));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float angle = Mth.lerp(partialTick, blockEntity.prevAngle, blockEntity.angle);
        Fluid type = blockEntity.tank.getTankType();
        boolean moving = blockEntity.prevAngle != blockEntity.angle;
        boolean spinnerChanged = !initialized || angle != lastAngle;
        boolean typeChanged = !initialized || type != fluidType;
        boolean movingChanged = !initialized || moving != lastMoving;
        if (!spinnerChanged && !typeChanged && !movingChanged) return;
        if (spinnerChanged) {
            lastAngle = angle;
            spinnerPose.set(basePose).rotateY(angle * Mth.DEG_TO_RAD);
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(spinnerPose);
            spinner.setTransform(world).light(0).setChanged();
            LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, SPINNER_PART.model(), spinnerPose, pos);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }
        if (typeChanged) syncFluid(type);
        if (fluid != null && (typeChanged || movingChanged)) {
            fluid.setVisible(moving);

            if (moving) poseFluid();
        }
        lastMoving = moving;
        initialized = true;
    }

    private void syncFluid(@Nullable Fluid type) {
        fluidType = type;
        if (fluid != null) fluid.delete();
        fluid = null;
        Model model = type == null ? null : fluidModel(type);
        if (model == null) return;
        fluid = instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        poseFluid();
    }

    private void poseFluid() {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(basePose);
        fluid.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 10,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(spinner);
    }

    @Override
    protected void _delete() {
        spinner.delete();
        if (fluid != null) fluid.delete();
    }
}
