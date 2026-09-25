// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.client.render.RenderPlushie;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.BlockEntityPlushie;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class PlushieVisual extends HbmDynamicBlockEntityVisual<BlockEntityPlushie>
        implements ShaderLightVisual {
    private static final Map<RenderPlushie.Part, Material> MATERIALS = materials();
    private static final Map<PartKey, MeshPart> PARTS = new ConcurrentHashMap<>();
    private final int rotation;
    private final List<TransformedInstance> parts = new ArrayList<>();
    private final List<Matrix4f> locals = new ArrayList<>();
    private final List<RenderPlushie.Face> faces = new ArrayList<>();
    private final Matrix4f world = new Matrix4f();
    private final PoseStack rootPoses = new PoseStack();
    private @Nullable PlushieType lastType;
    private boolean lastSquishing;
    private final WorldItem cigarette = new WorldItem(visualizationContext, level, pos);
    private final PoseStack cigarettePoses = new PoseStack();

    public PlushieVisual(
            VisualizationContext context, BlockEntityPlushie blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        rotation = blockState.getValue(BlockPlushie.ROTATION);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityPlushie be) {
        return be.type == PlushieType.NUMBERNINE
                && !WorldItem.draws(RenderPlushie.cigarette(), ItemDisplayContext.NONE);
    }

    private static Map<RenderPlushie.Part, Material> materials() {
        Map<RenderPlushie.Part, Material> materials = new EnumMap<>(RenderPlushie.Part.class);
        for (RenderPlushie.Part part : RenderPlushie.Part.values()) {
            materials.put(
                    part,
                    SimpleMaterial.builderOf(MeshPart.litCutout(part.texture))
                            .backfaceCulling(part.cull)
                            .build());
        }
        return materials;
    }

    static MeshPart meshPart(RenderPlushie.Part part, HFRWavefrontObject mesh, int group) {
        return PARTS.computeIfAbsent(
                new PartKey(part, mesh, group),
                key -> MeshPart.obj(mesh.groups[group], mesh.smoothing(), MATERIALS.get(part)));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        PlushieType type = blockEntity.type;
        long timer = blockEntity.squishTimer();
        boolean squishing = timer > 0;
        if (type == lastType && !squishing && !lastSquishing) return;
        if (type != lastType) {
            for (var part : parts) part.delete();
            parts.clear();
            locals.clear();
            faces.clear();
            RenderPlushie.visitParts(
                    type,
                    new PoseStack(),
                    (part, mesh, group, face, poses) -> {
                        parts.add(
                                instancerProvider()
                                        .instancer(
                                                InstanceTypes.TRANSFORMED,
                                                meshPart(part, mesh, group).model())
                                        .createInstance());
                        locals.add(new Matrix4f(poses.last().pose()));
                        faces.add(face);
                    });
            cigarette.set(
                    type == PlushieType.NUMBERNINE ? RenderPlushie.cigarette() : ItemStack.EMPTY,
                    ItemDisplayContext.NONE);
        }
        double squish = timer - partialTick;
        rootPoses.setIdentity();
        rootPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        RenderPlushie.rootPose(rootPoses, type, rotation, squishing, squish);
        Matrix4f root = rootPoses.last().pose();
        for (int i = 0; i < parts.size(); i++) {
            TransformedInstance part = parts.get(i);
            boolean shown = faces.get(i).shows(squishing);
            part.setVisible(shown);
            if (shown) part.setTransform(world.set(root).mul(locals.get(i))).light(0).setChanged();
        }
        if (type == PlushieType.NUMBERNINE) {
            cigarettePoses.setIdentity();
            cigarettePoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
            RenderPlushie.rootPose(cigarettePoses, type, rotation, squishing, squish);
            RenderPlushie.cigarettePose(cigarettePoses);
            cigarette.write(cigarettePoses.last().pose(), partialTick);
        }
        lastType = type;
        lastSquishing = squishing;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var part : parts) consumer.accept(part);
    }

    @Override
    protected void _delete() {
        for (var part : parts) part.delete();
        cigarette.delete();
    }

    private record PartKey(RenderPlushie.Part part, HFRWavefrontObject mesh, int group) {}
}
