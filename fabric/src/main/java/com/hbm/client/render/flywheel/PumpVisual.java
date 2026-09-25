// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachinePumpBase;
import com.hbm.tileentity.machine.BlockEntityMachinePumpElectric;
import com.hbm.util.Facing;
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
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PumpVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachinePumpBase>
        implements ShaderLightVisual {
    private static final int ROTOR = ResourceManager.pump.partId("Rotor");
    private static final int ARMS = ResourceManager.pump.partId("Arms");
    private static final int PISTON = ResourceManager.pump.partId("Piston");
    private static final HFRWavefrontObject MODEL = ResourceManager.pump;
    private static final MeshPart[] ELECTRIC = buildAssets(ResourceManager.pump_electric_tex);
    private static final MeshPart[] STEAM = buildAssets(ResourceManager.pump_steam_tex);
    private final AABB bodyBounds;
    private final MeshPart[] parts;
    private final TransformedInstance[] instances;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private float lastRotation = Float.NaN;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public PumpVisual(
            VisualizationContext context,
            BlockEntityMachinePumpBase blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 270)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Base", basePose, pos);
        parts = blockEntity instanceof BlockEntityMachinePumpElectric ? ELECTRIC : STEAM;
        instances = new TransformedInstance[parts.length];
        for (int i = 0; i < parts.length; i++) {
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, parts[i].model())
                            .createInstance();
        }
        updateMovingParts(partialTick);
    }

    private static MeshPart[] buildAssets(Identifier texture) {
        Material material =
                SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                        .texture(texture)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .backfaceCulling(false)
                        .build();
        return new MeshPart[] {
            MeshPart.obj(MODEL.groups[ROTOR], MODEL.smoothing(), material),
            MeshPart.obj(MODEL.groups[ARMS], MODEL.smoothing(), material),
            MeshPart.obj(MODEL.groups[PISTON], MODEL.smoothing(), material)
        };
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float rotation = Mth.lerp(partialTick, blockEntity.lastRotor, blockEntity.rotor);
        if (initialized && Float.compare(rotation, lastRotation) == 0) return;
        lastRotation = rotation;
        initialized = true;
        local[0].set(basePose)
                .translate(0F, 2.25F, 0F)
                .rotateZ((rotation - 90F) * Mth.DEG_TO_RAD)
                .translate(0F, -2.25F, 0F);
        double sin = Math.sin(Math.toRadians(rotation)) * .5D - .5D;
        double cos = Math.cos(Math.toRadians(rotation)) * .5D;
        double ang = Math.acos(cos / 2D);
        double cath = Math.sqrt(1D + (cos * cos) / 2D);
        local[1].set(basePose)
                .translate(0F, (float) (1D - cath + sin), 0F)
                .translate(0F, 4.75F, 0F)
                .rotateZ((float) (-(Math.toDegrees(ang) - 90D)) * Mth.DEG_TO_RAD)
                .translate(0F, -4.75F, 0F);
        local[2].set(basePose).translate(0F, (float) (1D - cath + sin), 0F);
        for (int i = 0; i < instances.length; i++) write(i, local[i]);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        for (int i = 0; i < parts.length; i++)
            LightBounds.includeLightBounds(lightBoundsAccumulator, parts[i].model(), local[i], pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private void write(int index, Matrix4f pose) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 5,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }
}
