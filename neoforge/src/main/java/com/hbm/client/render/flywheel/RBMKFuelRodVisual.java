// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderTextures;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RBMKFuelRodVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKRod>
        implements ShaderLightVisual {
    private static final int MAX_LAYERS = 16;
    private static final int CHERENKOV_LAYERS = 61;
    private static final int RODS = ResourceManager.rbmk_element_rods.partId("Rods");
    private static final int CHERENKOV_COLOR = ARGB.colorFromFloat(.1F, .4F, .9F, 1F);
    private static final Material GLOW_MATERIAL =
            SimpleMaterial.builderOf(Materials.ADDITIVE_NO_CULL)
                    .texture(RenderTextures.WHITE)
                    .mipmap(false)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .light(LightShaders.SMOOTH)
                    .useLight(true)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final Model GLOW_MODEL = new SingleMeshModel(glowMesh(), GLOW_MATERIAL);
    private static final MeshPart ROD_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_element_rods.groups[RODS],
                    ResourceManager.rbmk_element_rods.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_fuel_tex));
    private final TransformedInstance[] rods = new TransformedInstance[MAX_LAYERS];
    private final TransformedInstance[] glow = new TransformedInstance[CHERENKOV_LAYERS];
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private int lastLayers, lastRodColor;
    private boolean lastHasRod, lastGlow, written;

    public RBMKFuelRodVisual(
            VisualizationContext context, BlockEntityRBMKRod blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        for (int i = 0; i < MAX_LAYERS; i++)
            rods[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, ROD_PART.model())
                            .createInstance();
        for (int i = 0; i < CHERENKOV_LAYERS; i++) {
            glow[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, GLOW_MODEL)
                            .createInstance();
            glow[i].light(0).colorArgb(CHERENKOV_COLOR);
        }
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static PackedQuadMesh glowMesh() {
        return PackedQuadMesh.builder(1)
                .vertex(-.5F, 0F, -.5F, -1)
                .vertex(-.5F, 0F, .5F, -1)
                .vertex(.5F, 0F, .5F, -1)
                .vertex(.5F, 0F, -.5F, -1)
                .build();
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int layers = RBMKConfig.getColumnHeightRuleValue(blockEntity.getLevel());
        boolean hasRod = blockEntity.hasRod;
        int rodColor = ARGB.opaque(blockEntity.rodColor);
        boolean hasGlow = blockEntity.fluxQuantity > 5;
        if (written
                && lastLayers == layers
                && lastHasRod == hasRod
                && lastRodColor == rodColor
                && lastGlow == hasGlow) return;
        for (int i = 0; i < MAX_LAYERS; i++) {
            pose.identity().translate(.5F, i, .5F);
            writeRod(i, pose, hasRod && i < layers, rodColor);
        }
        int height = Math.max(0, layers - 1);
        for (int i = 0; i < CHERENKOV_LAYERS; i++) {
            boolean visible = hasRod && hasGlow && i * .25F <= height;
            glow[i].setVisible(visible);
            if (visible) {
                pose.identity().translate(.5F, .75F + i * .25F, .5F);
                world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
                glow[i].setTransform(world).light(0).colorArgb(CHERENKOV_COLOR).setChanged();
            }
        }
        lastLayers = layers;
        lastHasRod = hasRod;
        lastRodColor = rodColor;
        lastGlow = hasGlow;
        written = true;
    }

    private void writeRod(int index, Matrix4f local, boolean visible, int color) {
        TransformedInstance rod = rods[index];
        rod.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        rod.setTransform(world).light(0).colorArgb(color).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        pos.getX() + 1D,
                        pos.getY() + 17D,
                        pos.getZ() + 1D)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance rod : rods) consumer.accept(rod);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance rod : rods) rod.delete();
        for (TransformedInstance instance : glow) instance.delete();
    }
}
