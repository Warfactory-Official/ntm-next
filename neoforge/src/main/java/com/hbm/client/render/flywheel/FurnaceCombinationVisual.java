// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityFurnaceCombination;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class FurnaceCombinationVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityFurnaceCombination> {
    private static final Identifier FLAME_TEX = Library.id("textures/particle/rbmk_fire.png");
    private static final Material FLAME_MATERIAL =
            SimpleMaterial.builder()
                    .texture(FLAME_TEX)
                    .mipmap(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .build();
    private static final Model FLAME_MODEL = new SingleMeshModel(flameMesh(), FLAME_MATERIAL);
    private final UvTransformedInstance flame;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private float lastCameraYaw = Float.NaN;
    private int lastTextureIndex = -1;
    private boolean lastOn;
    private boolean initialized;

    public FurnaceCombinationVisual(
            VisualizationContext context,
            BlockEntityFurnaceCombination blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        flame =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, FLAME_MODEL)
                        .createInstance();
        writeFlame(0F);
    }

    public static void initModels() {}

    private static PackedQuadMesh flameMesh() {
        return PackedQuadMesh.builder(1)
                .vertex(-1F, 0F, 0F, 1F, 1F, -1)
                .vertex(-1F, 3F, 0F, 1F, 0F, -1)
                .vertex(1F, 3F, 0F, 0F, 0F, -1)
                .vertex(1F, 0F, 0F, 0F, 1F, -1)
                .build();
    }

    @Override
    protected void frame(Context context) {
        writeFlame(context.camera().yRot());
    }

    private void writeFlame(float cameraYaw) {
        boolean on = blockEntity.wasOn;
        int texIndex = (int) (level.getGameTime() / 2 % 14);
        if (initialized
                && on == lastOn
                && (!on || texIndex == lastTextureIndex && cameraYaw == lastCameraYaw)) return;
        lastOn = on;
        lastTextureIndex = texIndex;
        lastCameraYaw = cameraYaw;
        initialized = true;
        flame.setVisible(on);
        if (!on) return;
        float cell = 1F / 14F;
        localPose
                .identity()
                .translate(.5F, 1.75F, .5F)
                .rotateY(-cameraYaw * (float) Math.PI / 180F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        flame.setTransform(instancePose).light(LightCoordsUtil.pack(15, 0));
        flame.uvRegion(texIndex % 5 * cell, 0F, cell, 1F).setChanged();
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos).expandTowards(1, 5, 1).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        flame.delete();
    }
}
