// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad.KeyUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKKeyPadVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKKeyPad>
        implements ShaderLightVisual {
    private static final int KEYS = BlockEntityRBMKKeyPad.KEYS;
    private static final float PRESS_DEPTH = -.03125F;
    private static final PackedQuadMesh SOCKET_MESH =
            PackedQuadMesh.of(
                    ResourceManager.rbmk_button
                            .groups[ResourceManager.rbmk_button.partId("Socket")],
                    ResourceManager.rbmk_button.smoothing());
    private static final PackedQuadMesh BUTTON_MESH =
            PackedQuadMesh.of(
                    ResourceManager.rbmk_button
                            .groups[ResourceManager.rbmk_button.partId("Button")],
                    ResourceManager.rbmk_button.smoothing());
    private static final Material ORDINARY = MeshPart.litCutout(ResourceManager.rbmk_keypad_tex);
    private static final MeshPart SOCKET_PART = MeshPart.create(SOCKET_MESH, ORDINARY);
    private static final MeshPart BUTTON_PART = MeshPart.create(BUTTON_MESH, ORDINARY);
    private static final Model BRIGHT_MODEL =
            new SingleMeshModel(
                    BUTTON_MESH,
                    SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                            .texture(ResourceManager.rbmk_keypad_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .useLight(false)
                            .useOverlay(false)
                            .cardinalLightingMode(CardinalLightingMode.CHUNK)
                            .ambientOcclusion(false)
                            .backfaceCulling(false)
                            .build());
    private final TransformedInstance[] sockets = new TransformedInstance[KEYS];
    private final TransformedInstance[] buttons = new TransformedInstance[KEYS];
    private final TransformedInstance[] brights = new TransformedInstance[KEYS];
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final AABB bodyBounds;
    private final Matrix4f[] socketPoses = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final Matrix4f[] buttonPoses = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final double[] lightBoundsAccumulator = new double[6];
    private final boolean[] lastActive = new boolean[KEYS];
    private final boolean[] lastPressed = new boolean[KEYS];
    private final int[] lastColor = new int[KEYS];
    private final WorldText[] labels = new WorldText[KEYS];
    private final WorldText.Posing[] labelPosings = new WorldText.Posing[KEYS];
    private final String[] lastLabels = new String[KEYS];
    private final boolean[] textActive = new boolean[KEYS];
    private @Nullable AABB lastLightBounds;
    private boolean initialized;
    private boolean textInitialized;

    public RBMKKeyPadVisual(
            VisualizationContext context, BlockEntityRBMKKeyPad blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockEntity.getBlockState().getValue(RBMKMiniPanelBase.FACING);
        rootPose.translation(.5F, 0F, .5F).rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        bodyBounds = new AABB(pos).inflate(1);
        for (int i = 0; i < KEYS; i++) {
            sockets[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, SOCKET_PART.model())
                            .createInstance();
            buttons[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, BUTTON_PART.model())
                            .createInstance();
            brights[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, BRIGHT_MODEL)
                            .createInstance();
            labels[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            Matrix4f key =
                    new Matrix4f(rootPose)
                            .translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
            labelPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                        out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(key)
                                .translate(.01F, .3125F, 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        updateMovingParts(partialTick);
        updateText();
    }

    public static void initModels() {}

    private static int dim(int color, float mult) {
        return ARGB.colorFromFloat(
                1F,
                ((color >> 16) & 0xFF) / 255F * mult,
                ((color >> 8) & 0xFF) / 255F * mult,
                (color & 0xFF) / 255F * mult);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateText();
    }

    private void updateText() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < KEYS; i++) {
            KeyUnit unit = blockEntity.keys[i];
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
        boolean changed = !initialized;
        for (int i = 0; i < KEYS; i++) {
            KeyUnit unit = blockEntity.keys[i];
            changed |=
                    unit.active != lastActive[i]
                            || unit.isPressed != lastPressed[i]
                            || unit.color != lastColor[i];
        }
        if (!changed) return;
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        for (int i = 0; i < KEYS; i++) {
            KeyUnit unit = blockEntity.keys[i];
            boolean keyChanged =
                    !initialized
                            || unit.active != lastActive[i]
                            || unit.isPressed != lastPressed[i]
                            || unit.color != lastColor[i];
            lastActive[i] = unit.active;
            lastPressed[i] = unit.isPressed;
            lastColor[i] = unit.color;
            if (!keyChanged) {
                if (unit.active)
                    LightBounds.includeLightBounds(
                            lightBoundsAccumulator, SOCKET_PART.model(), socketPoses[i], pos);
                if (unit.active && !unit.isPressed)
                    LightBounds.includeLightBounds(
                            lightBoundsAccumulator, BUTTON_PART.model(), buttonPoses[i], pos);
                continue;
            }
            Matrix4f socket =
                    socketPoses[i]
                            .set(rootPose)
                            .translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
            Matrix4f button =
                    buttonPoses[i].set(socket).translate(unit.isPressed ? PRESS_DEPTH : 0F, 0F, 0F);
            write(sockets[i], socket, unit.active, -1);
            write(buttons[i], button, unit.active && !unit.isPressed, dim(unit.color, .65F));
            writeBright(brights[i], button, unit.active && unit.isPressed, unit.color);
            if (unit.active)
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, SOCKET_PART.model(), socket, pos);
            if (unit.active && !unit.isPressed)
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, BUTTON_PART.model(), button, pos);
        }
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f local, boolean visible, int color) {
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(world).light(0).colorArgb(color).setChanged();
    }

    private void writeBright(
            TransformedInstance instance, Matrix4f local, boolean visible, int color) {
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(world)
                .light(LightCoordsUtil.FULL_BRIGHT)
                .colorArgb(0xFF000000 | color)
                .setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int i = 0; i < KEYS; i++) {
            consumer.accept(sockets[i]);
            consumer.accept(buttons[i]);
        }
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < KEYS; i++) {
            sockets[i].delete();
            buttons[i].delete();
            brights[i].delete();
            labels[i].delete();
        }
    }
}
