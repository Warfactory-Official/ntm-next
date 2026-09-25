// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAnnihilator;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AnnihilatorVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineAnnihilator>
        implements ShaderLightVisual {
    private static final int ROLLER = ResourceManager.annihilator.partId("Roller");
    private static final int BELT = ResourceManager.annihilator.partId("Belt");
    private static final HFRWavefrontObject MODEL = ResourceManager.annihilator;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.annihilator_tex);
    private static final MeshPart ROLLER_PART =
            MeshPart.obj(MODEL.groups[ROLLER], MODEL.smoothing(), BODY_MATERIAL);
    private static final MeshPart BELT_PART =
            MeshPart.obj(
                    MODEL.groups[BELT],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.annihilator_belt_tex));
    private final TransformedInstance roller;
    private final UvTransformedInstance belt;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f rollerPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final AABB rawBodyBounds;
    private float lastAngle = Float.NaN;
    private float lastBeltOffset = Float.NaN;
    private boolean initialized;

    public AnnihilatorVisual(
            VisualizationContext context,
            BlockEntityMachineAnnihilator blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        base.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockEntity.getBlockState()), 90)
                                * Mth.DEG_TO_RAD);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Annihilator", base, pos));
        roller =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROLLER_PART.model())
                        .createInstance();
        belt =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, BELT_PART.model())
                        .createInstance();
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(base);
        belt.setTransform(world).light(0).setChanged();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        long time = GameTime.now();
        float angle = (float) (time * .15D % 360D);
        float beltOffset = (float) -(time / 3000D % 1D);
        if (!initialized || angle != lastAngle) {
            rollerPose
                    .set(base)
                    .translate(0F, 1.75F, 0F)
                    .rotateZ(-angle * Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 0F);
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(rollerPose);
            roller.setTransform(world).light(0).setChanged();
            lastAngle = angle;
        }
        if (!initialized || beltOffset != lastBeltOffset) {
            belt.uvRegion(beltOffset, 0F, 1F, 1F).setChanged();
            lastBeltOffset = beltOffset;
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 5,
                                pos.getY(),
                                pos.getZ() - 5,
                                pos.getX() + 6,
                                pos.getY() + 8,
                                pos.getZ() + 6)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(roller);
        consumer.accept(belt);
    }

    @Override
    protected void _delete() {
        roller.delete();
        belt.delete();
    }
}
