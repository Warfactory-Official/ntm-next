// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.client.render.RenderMassStorage;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MassStorageVisual extends HbmDynamicBlockEntityVisual<BlockEntityMassStorage> {
    private static final Model BAR_MODEL = buildBar();
    private static final double VIEW_DISTANCE_SQ =
            RenderMassStorage.VIEW_DISTANCE * RenderMassStorage.VIEW_DISTANCE;
    private final TransformedInstance bar;
    private final WorldItem icon;
    private final WorldText count;
    private final Matrix4f panel = new Matrix4f();
    private final Matrix4f iconPose = new Matrix4f();
    private final Matrix4f barPose = new Matrix4f();
    private final WorldText.Posing countPosing;
    private final Vec3 center;
    private float lastFraction = Float.NaN;
    private String lastCount = "";
    private boolean lastShown;
    private boolean initialized;

    public MassStorageVisual(
            VisualizationContext context, BlockEntityMassStorage blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockState.getValue(BlockMassStorage.FACING);
        PoseStack poses = new PoseStack();
        poses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        RenderMassStorage.panelPose(poses, facing);
        panel.set(poses.last().pose());
        RenderMassStorage.iconPose(poses);
        iconPose.set(poses.last().pose());
        center = Vec3.atCenterOf(pos);
        bar = instancerProvider().instancer(InstanceTypes.TRANSFORMED, BAR_MODEL).createInstance();
        icon = new WorldItem(visualizationContext, level, pos, true);
        count = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true, true);
        countPosing =
                (width, out) ->
                        out.set(panel)
                                .scale(4F / 16F, 4F / 16F, 4F / 16F)
                                .translate(32F - width / 2F, 0F, 0F);
        writeFrame(true, partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityMassStorage be) {
        ItemStack type = be.getItem(BlockEntityMassStorage.SLOT_TYPE);
        return !type.isEmpty() && !WorldItem.draws(type, ItemDisplayContext.GUI);
    }

    private static Model buildBar() {
        Material material =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(ResourceManager.white_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.NONE)
                        .useLight(false)
                        .useOverlay(false)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .build();
        float[] vertices = {
            0F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 0F, 0F, 0F, 1F, 0F, 1F, 0F, 0F, 0F, 0F, 0F,
            1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F
        };
        return new SingleMeshModel(
                PackedQuadMesh.of(vertices, new int[] {-1, -1, -1, -1}, new int[4]), material);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(
                context.camera().position().distanceToSqr(center) < VIEW_DISTANCE_SQ,
                context.partialTick());
    }

    private void writeFrame(boolean near, float partialTick) {
        ItemStack type = blockEntity.getItem(BlockEntityMassStorage.SLOT_TYPE);
        boolean shown = near && !type.isEmpty();
        float fraction =
                blockEntity.getCapacity() > 0
                        ? (float) blockEntity.getStockpile() / blockEntity.getCapacity()
                        : 0F;
        String text =
                shown
                        ? RenderMassStorage.countText(
                                blockEntity.getStockpile(),
                                Minecraft.getInstance().options.forceUnicodeFont().get())
                        : "";
        icon.set(shown ? type : ItemStack.EMPTY, ItemDisplayContext.GUI);
        icon.write(iconPose, partialTick);
        if (!initialized || !text.equals(lastCount) || shown != lastShown) {
            count.set(
                    shown ? Component.literal(text) : null, 0F, 44F, RenderMassStorage.TEXT_COLOR);
            lastCount = text;
        }
        count.write(countPosing);
        if (initialized && shown == lastShown && fraction == lastFraction) return;
        lastShown = shown;
        lastFraction = fraction;
        bar.setVisible(shown);
        if (shown) {
            barPose.set(panel).translate(2F, 13.5F, 0F).scale(fraction * 12F, .5F, 1F);
            bar.setTransform(barPose)
                    .colorArgb(ARGB.colorFromFloat(1F, 1F - fraction, fraction, 0F))
                    .light(0)
                    .setChanged();
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        bar.delete();
        icon.delete();
        count.delete();
    }
}
