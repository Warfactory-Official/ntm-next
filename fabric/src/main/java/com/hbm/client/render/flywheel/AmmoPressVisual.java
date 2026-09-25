// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineAmmoPress;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AmmoPressVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineAmmoPress>
        implements ShaderLightVisual {
    private static final int PRESS = ResourceManager.ammo_press.partId("Press");
    private static final int SHELLS = ResourceManager.ammo_press.partId("Shells");
    private static final int BULLETS = ResourceManager.ammo_press.partId("Bullets");
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.ammo_press_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart[] PARTS = {
        MeshPart.obj(
                ResourceManager.ammo_press.groups[PRESS],
                ResourceManager.ammo_press.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.ammo_press.groups[SHELLS],
                ResourceManager.ammo_press.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.ammo_press.groups[BULLETS],
                ResourceManager.ammo_press.smoothing(),
                MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private float lastPress = Float.NaN, lastLift = Float.NaN;
    private boolean lastBullets, initialized;

    public AmmoPressVisual(
            VisualizationContext context,
            BlockEntityMachineAmmoPress blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal = bodyPose();
        bodyBounds = LightBounds.of(ResourceManager.ammo_press, "Frame", bodyLocal, pos);
        instances = new TransformedInstance[PARTS.length];
        local[0].set(bodyLocal);
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float press = Mth.lerp(partialTick, blockEntity.prevPress, blockEntity.press);
        float lift = Mth.lerp(partialTick, blockEntity.prevLift, blockEntity.lift);
        boolean bullets =
                blockEntity.animState == BlockEntityMachineAmmoPress.AnimationState.RETRACTING
                        || blockEntity.animState
                                == BlockEntityMachineAmmoPress.AnimationState.LOWERING;
        if (press != lastPress) {
            local[1].set(local[0]).translate(0F, -press * .25F, 0F);
            write(0, local[1], true);
            lastPress = press;
        }
        boolean liftChanged = lift != lastLift;
        if (liftChanged) {
            local[2].set(local[0]).translate(0F, lift * .5F - .5F, 0F);
            write(1, local[2], true);
            lastLift = lift;
        }
        if (!initialized || liftChanged || bullets != lastBullets) {
            write(2, local[2], bullets);
            lastBullets = bullets;
        }
        initialized = true;
    }

    private void write(int index, Matrix4f pose, boolean visible) {
        TransformedInstance instance = instances[index];
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(world).light(0).setChanged();
    }

    private Matrix4f bodyPose() {
        return new Matrix4f()
                .translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 2,
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
