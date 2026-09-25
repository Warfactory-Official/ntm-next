// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderFoundry;
import com.hbm.tileentity.machine.BlockEntityFoundryCastingBase;
import com.hbm.tileentity.machine.IRenderFoundry;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FoundryVisual extends HbmDynamicBlockEntityVisual<BlockEntityFoundryCastingBase>
        implements ShaderLightVisual {
    private static final Material SHEEN =
            SimpleMaterial.builderOf(FoundryTankVisual.MOLTEN)
                    .useLight(false)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .fog(EffectVisuals.FADE)
                    .build();
    private static final MeshPart MELT =
            FoundryTankVisual.molten(MeshPart.face(Direction.UP, FoundryTankVisual.MOLTEN));
    private static final MeshPart OUTPUT =
            MeshPart.face(Direction.UP, MeshPart.litCutout(TextureAtlas.LOCATION_BLOCKS));
    private static final MeshPart SHEEN_PART = MeshPart.face(Direction.UP, SHEEN);
    private final AffineUvTransformedInstance melt, sheen, output;
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private float lastX0 = Float.NaN, lastZ0 = Float.NaN, lastX1 = Float.NaN, lastZ1 = Float.NaN;
    private float lastMoltenLevel = Float.NaN, lastOutHeight = Float.NaN;
    private int lastLiquidColor;
    private boolean lastLiquid, lastOutputBlock, initialized;
    private Item lastOutputItem;
    private @Nullable TextureAtlasSprite outputSprite;
    private final WorldItem moldItem = new WorldItem(visualizationContext, level, pos);
    private final WorldItem outItem = new WorldItem(visualizationContext, level, pos);
    private final PoseStack itemPoses = new PoseStack();

    public FoundryVisual(
            VisualizationContext context,
            BlockEntityFoundryCastingBase blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        melt =
                instancerProvider()
                        .instancer(AffineUvTransformedInstance.TYPE, MELT.model())
                        .createInstance();
        sheen =
                instancerProvider()
                        .instancer(AffineUvTransformedInstance.TYPE, SHEEN_PART.model())
                        .createInstance();
        output =
                instancerProvider()
                        .instancer(AffineUvTransformedInstance.TYPE, OUTPUT.model())
                        .createInstance();
        updateMovingParts(partialTick);
        updateItems(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityFoundryCastingBase be) {
        ItemStack out = be.inventory.get(1);
        return !WorldItem.draws(be.inventory.get(0), ItemDisplayContext.NONE)
                || !(out.getItem() instanceof BlockItem)
                        && !WorldItem.draws(out, ItemDisplayContext.NONE);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateItems(context.partialTick());
    }

    private void updateItems(float partialTick) {
        var foundry = (IRenderFoundry) blockEntity;
        ItemStack out = blockEntity.inventory.get(1);
        writeItem(moldItem, blockEntity.inventory.get(0), foundry.moldHeight(), partialTick);
        writeItem(
                outItem,
                out.getItem() instanceof BlockItem ? ItemStack.EMPTY : out,
                foundry.outHeight(),
                partialTick);
    }

    private void writeItem(WorldItem item, ItemStack stack, double height, float partialTick) {
        if (!item.set(stack, ItemDisplayContext.NONE)) return;
        itemPoses.setIdentity();
        itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        RenderFoundry.flatItemPose(itemPoses, height);
        item.write(itemPoses.last().pose(), partialTick);
    }

    public void updateMovingParts(float partialTick) {
        var foundry = (IRenderFoundry) blockEntity;
        float x0 = (float) foundry.minX(),
                x1 = (float) foundry.maxX(),
                z0 = (float) foundry.minZ(),
                z1 = (float) foundry.maxZ();
        float dx = x1 - x0, dz = z1 - z0;
        boolean liquid = foundry.shouldRender();
        float moltenLevel = liquid ? (float) foundry.getMoltenLevel() : 0F;
        int liquidColor = liquid ? 0xFF000000 | foundry.getMat().moltenColor : 0;
        boolean liquidChanged =
                !initialized
                        || liquid != lastLiquid
                        || x0 != lastX0
                        || x1 != lastX1
                        || z0 != lastZ0
                        || z1 != lastZ1
                        || moltenLevel != lastMoltenLevel
                        || liquidColor != lastLiquidColor;
        if (liquidChanged) {
            melt.setVisible(liquid);
            sheen.setVisible(liquid);
            if (liquid) {
                local.translation(x0, moltenLevel - 1F, z0).scale(dx, 1F, dz);
                pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
                melt.setTransform(pose).colorArgb(liquidColor).light(LightCoordsUtil.FULL_BRIGHT);
                melt.uv(0, dz, -dx, 0, z0, x1).setChanged();
                sheen.setTransform(pose).colorArgb(0x4CFFFFFF).light(0);
                sheen.uv(0, dz, -dx, 0, z0, x1).setChanged();
            }
            lastLiquid = liquid;
            lastMoltenLevel = moltenLevel;
            lastLiquidColor = liquidColor;
        }
        var stack = blockEntity.inventory.get(1);
        boolean block = stack.getItem() instanceof BlockItem;
        Item outputItem = stack.getItem();
        float outHeight = (float) foundry.outHeight();
        boolean outputChanged =
                !initialized
                        || block != lastOutputBlock
                        || outputItem != lastOutputItem
                        || x0 != lastX0
                        || x1 != lastX1
                        || z0 != lastZ0
                        || z1 != lastZ1
                        || outHeight != lastOutHeight;
        if (outputChanged) {
            output.setVisible(block);
            if (block) {
                var item = (BlockItem) stack.getItem();
                if (outputItem != lastOutputItem || outputSprite == null)
                    outputSprite =
                            Minecraft.getInstance()
                                    .getModelManager()
                                    .getBlockStateModelSet()
                                    .getParticleMaterial(item.getBlock().defaultBlockState())
                                    .sprite();
                TextureAtlasSprite sprite = outputSprite;
                local.translation(x0, outHeight - 1F, z0).scale(dx, 1F, dz);
                pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
                float du = sprite.getU1() - sprite.getU0(), dv = sprite.getV1() - sprite.getV0();
                output.setTransform(pose).light(0);
                output.uv(0, du, -dv, 0, sprite.getU0(), sprite.getV1()).setChanged();
            }
            lastOutputBlock = block;
            lastOutputItem = outputItem;
            lastOutHeight = outHeight;
        }
        lastX0 = x0;
        lastX1 = x1;
        lastZ0 = z0;
        lastZ1 = z1;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(output);
    }

    @Override
    protected void _delete() {
        melt.delete();
        sheen.delete();
        output.delete();
        moldItem.delete();
        outItem.delete();
    }
}
