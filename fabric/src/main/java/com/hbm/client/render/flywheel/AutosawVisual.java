// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAutosaw;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AutosawVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineAutosaw>
        implements ShaderLightVisual {
    private static final int MAIN = ResourceManager.autosaw.partId("Main");
    private static final int ENGINE = ResourceManager.autosaw.partId("Engine");
    private static final int ARM_UPPER = ResourceManager.autosaw.partId("ArmUpper");
    private static final int ARM_LOWER = ResourceManager.autosaw.partId("ArmLower");
    private static final int ARM_TIP = ResourceManager.autosaw.partId("ArmTip");
    private static final int SAWBLADE = ResourceManager.autosaw.partId("Sawblade");
    private static final HFRWavefrontObject MODEL = ResourceManager.autosaw;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.autosaw_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[MAIN], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ENGINE], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ARM_UPPER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ARM_LOWER], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[ARM_TIP], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[SAWBLADE], MODEL.smoothing(), MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f[] local = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(),
        new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final Matrix4f world = new Matrix4f();
    private float lastTurn = Float.NaN;
    private float lastAngle = Float.NaN;
    private float lastSpin = Float.NaN;
    private float lastEngine = Float.NaN;

    public AutosawVisual(
            VisualizationContext context,
            BlockEntityMachineAutosaw blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F);
        bodyBounds = LightBounds.of(MODEL, "Base", basePose, pos);
        instances = new TransformedInstance[PARTS.length];
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
        float turn = Mth.lerp(partialTick, blockEntity.prevRotationYaw, blockEntity.rotationYaw);
        float angle =
                80F
                        - Mth.lerp(
                                partialTick,
                                blockEntity.prevRotationPitch,
                                blockEntity.rotationPitch);
        float spin = Mth.lerp(partialTick, blockEntity.lastSpin, blockEntity.spin);
        float engine =
                blockEntity.isOn
                        ? (float) Math.sin((GameTime.now() * 2 % (2 * Math.PI)) + partialTick)
                        : 0F;
        boolean turnChanged = turn != lastTurn;
        boolean armChanged = turnChanged || angle != lastAngle;
        boolean bladeChanged = armChanged || spin != lastSpin;
        boolean engineChanged = turnChanged || engine != lastEngine;
        lastTurn = turn;
        lastAngle = angle;
        lastSpin = spin;
        lastEngine = engine;

        if (turnChanged) {
            local[0].set(basePose).rotateY(-turn * Mth.DEG_TO_RAD);
            write(0);
        }
        if (engineChanged) {
            local[1].set(local[0]).translate(0F, engine * .01F, 0F);
            write(1);
        }
        if (armChanged) {
            local[2].set(local[0])
                    .translate(0F, 1.75F, 0F)
                    .rotateX(angle * Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 0F);
            local[3].set(local[2])
                    .translate(0F, 1.75F, -4F)
                    .rotateX(-angle * 2F * Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 4F)
                    .translate(-.01F, 0F, 0F);
            local[4].set(local[3])
                    .translate(.01F, 0F, 0F)
                    .translate(0F, 1.75F, -8F)
                    .rotateX(angle * Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 8F);
            for (int i = 2; i <= 4; i++) write(i);
        }
        if (bladeChanged) {
            local[5].set(local[4])
                    .translate(0F, 1.75F, -10F)
                    .rotateY(-spin * Mth.DEG_TO_RAD)
                    .translate(0F, -1.75F, 10F);
            write(5);
        }
    }

    private void write(int index) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local[index]);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 12,
                                pos.getY(),
                                pos.getZ() - 12,
                                pos.getX() + 13,
                                pos.getY() + 10,
                                pos.getZ() + 13)
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
