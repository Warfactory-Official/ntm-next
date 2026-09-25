// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.MachineStirling;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityStirling;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class StirlingVisual extends HbmDynamicBlockEntityVisual<BlockEntityStirling>
        implements ShaderLightVisual {
    private static final int SKINS = 3;
    private static final String[] NAMES = {"Cog", "CogSmall", "Piston"};
    private static final Identifier[] TEXTURES = {
        ResourceManager.stirling_tex,
        ResourceManager.stirling_steel_tex,
        ResourceManager.stirling_creative_tex
    };
    private static final MeshPart[][] PARTS = buildParts();
    private final AABB bodyBounds;
    private final MeshPart[] parts;
    private final TransformedInstance[] instances = new TransformedInstance[NAMES.length];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f[] partPoses = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private float lastRotation = Float.NaN;
    private boolean lastHasCog;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public StirlingVisual(
            VisualizationContext context, BlockEntityStirling blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        parts = PARTS[((MachineStirling) blockState.getBlock()).tier().gear];
        Matrix4f bodyPose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180))
                                        * Mth.DEG_TO_RAD);
        basePose.set(bodyPose);
        bodyBounds = LightBounds.of(ResourceManager.stirling, "Base", bodyPose, pos);
        for (int i = 0; i < NAMES.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, parts[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart[][] buildParts() {
        var parts = new MeshPart[SKINS][NAMES.length];
        for (int skin = 0; skin < SKINS; skin++)
            for (int i = 0; i < NAMES.length; i++)
                parts[skin][i] =
                        MeshPart.obj(
                                ResourceManager.stirling
                                        .groups[ResourceManager.stirling.partId(NAMES[i])],
                                ResourceManager.stirling.smoothing(),
                                MeshPart.litCutout(TEXTURES[skin]));
        return parts;
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float rot = blockEntity.lastSpin + (blockEntity.spin - blockEntity.lastSpin) * partialTick;
        boolean hasCog = blockEntity.hasCog;
        if (initialized && hasCog == lastHasCog && Float.compare(rot, lastRotation) == 0) return;
        lastHasCog = hasCog;
        lastRotation = rot;
        initialized = true;
        partPoses[0].set(basePose);
        if (hasCog)
            partPoses[0]
                    .translate(0F, 1.375F, 0F)
                    .rotateZ((-rot) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.375F, 0F);
        instances[0].setVisible(hasCog);
        if (hasCog) write(0);
        partPoses[1]
                .set(basePose)
                .translate(0F, 1.375F, .25F)
                .rotateX((rot * 2F + 3F) * Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, -.25F);
        write(1);
        partPoses[2]
                .set(basePose)
                .translate((float) (Math.sin(rot * Math.PI / 90D) * .25D + .125D), 0F, 0F);
        write(2);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        for (int i = hasCog ? 0 : 1; i < NAMES.length; i++)
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, parts[i].model(), partPoses[i], pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private void write(int index) {
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(partPoses[index]);
        instances[index].setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).expandTowards(1, 2, 1).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (var instance : instances) instance.delete();
    }
}
