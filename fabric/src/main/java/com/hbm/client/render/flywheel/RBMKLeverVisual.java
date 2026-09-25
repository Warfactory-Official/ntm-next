// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever.LeverUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RBMKLeverVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKLever>
        implements ShaderLightVisual {
    private static final int LEVERS = BlockEntityRBMKLever.LEVERS;
    private static final MeshPart BASE_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_lever.groups[ResourceManager.rbmk_lever.partId("Base")],
                    ResourceManager.rbmk_lever.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_lever_tex));
    private static final MeshPart LEVER_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_lever.groups[ResourceManager.rbmk_lever.partId("Lever")],
                    ResourceManager.rbmk_lever.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_lever_tex));
    private final TransformedInstance[] bases = new TransformedInstance[LEVERS];
    private final TransformedInstance[] levers = new TransformedInstance[LEVERS];
    private final Matrix4f[] basePoses = new Matrix4f[LEVERS];
    private final Matrix4f[] leverPoses = new Matrix4f[LEVERS];
    private final Matrix4f world = new Matrix4f();
    private final double[] lastFlip = new double[LEVERS];
    private final boolean[] lastActive = new boolean[LEVERS];
    private final WorldText[] labels = new WorldText[LEVERS];
    private final WorldText.Posing[] labelPosings = new WorldText.Posing[LEVERS];
    private final String[] lastLabels = new String[LEVERS];
    private final boolean[] textActive = new boolean[LEVERS];
    private boolean initialized;
    private boolean textInitialized;

    public RBMKLeverVisual(
            VisualizationContext context, BlockEntityRBMKLever blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var panelFacing = blockEntity.getBlockState().getValue(RBMKMiniPanelBase.FACING);
        Matrix4f root =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(Facing.yaw(panelFacing, 90) * Mth.DEG_TO_RAD);
        for (int i = 0; i < LEVERS; i++) {
            bases[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, BASE_PART.model())
                            .createInstance();
            levers[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LEVER_PART.model())
                            .createInstance();
            basePoses[i] = new Matrix4f(root).translate(.25F, 0F, i * -.5F + .25F);
            leverPoses[i] = new Matrix4f();
            labels[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            Matrix4f base = basePoses[i];
            labelPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                        out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(base)
                                .translate(.01F, .0625F, 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        updateMovingParts(partialTick);
        updateText();
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateText();
    }

    private void updateText() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < LEVERS; i++) {
            LeverUnit unit = blockEntity.levers[i];
            if (!textInitialized
                    || unit.active != textActive[i]
                    || !unit.label.equals(lastLabels[i])) {
                textActive[i] = unit.active;
                lastLabels[i] = unit.label;
                labels[i].set(
                        unit.active && !unit.label.isEmpty() ? Component.literal(unit.label) : null,
                        0F,
                        y,
                        ARGB.opaque(0x00FF00));
            }
            labels[i].write(labelPosings[i]);
        }
        textInitialized = true;
    }

    public void updateMovingParts(float partialTick) {
        for (int i = 0; i < LEVERS; i++) {
            LeverUnit unit = blockEntity.levers[i];
            double flip = Mth.lerp(partialTick, unit.prevFlipProgress, unit.flipProgress);
            boolean activeChanged = !initialized || unit.active != lastActive[i];
            boolean flipChanged =
                    !initialized
                            || Double.doubleToLongBits(flip)
                                    != Double.doubleToLongBits(lastFlip[i]);
            if (activeChanged) write(bases[i], basePoses[i], unit.active);
            if (unit.active && (activeChanged || flipChanged)) {
                leverPoses[i]
                        .set(basePoses[i])
                        .translate(.125F, .5625F, 0F)
                        .rotateZ((float) (-180D * flip) * Mth.DEG_TO_RAD)
                        .translate(-.125F, -.5625F, 0F);
                write(levers[i], leverPoses[i], true);
            } else if (!unit.active && activeChanged) {
                write(levers[i], leverPoses[i], false);
            }
            lastActive[i] = unit.active;
            lastFlip[i] = flip;
        }
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f local, boolean visible) {
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int i = 0; i < LEVERS; i++) {
            consumer.accept(bases[i]);
            consumer.accept(levers[i]);
        }
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < LEVERS; i++) {
            bases[i].delete();
            levers[i].delete();
            labels[i].delete();
        }
    }
}
