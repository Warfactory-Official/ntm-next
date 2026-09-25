// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.entity.grenade.IGenericGrenade;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.items.weapon.ItemDisperser;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class GenericGrenadeVisual<T extends Entity> extends HbmDynamicEntityVisual<T> {
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_ITEM)
                    .mipmap(false)
                    .ambientOcclusion(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.ENTITY)
                    .build();
    private static final Model MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.builder(1)
                            .normal(0F, 1F, 0F)
                            .vertex(-.5F, -.25F, 0F, 0F, 1F, -1)
                            .vertex(.5F, -.25F, 0F, 1F, 1F, -1)
                            .vertex(.5F, .75F, 0F, 1F, 0F, -1)
                            .vertex(-.5F, .75F, 0F, 0F, 0F, -1)
                            .build(),
                    MATERIAL);
    private final Vector3f interpolatedPosition = new Vector3f();
    private final NameTagComponent nameTag;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f written = new Matrix4f().zero();
    private final UvTransformedInstance base;
    private final @Nullable UvTransformedInstance overlay;
    private @Nullable Identifier baseTexture, overlayTexture;
    private @Nullable TextureAtlasSprite baseSprite, overlaySprite;
    private @Nullable Fluid overlayFluid;
    private int overlayColor = -1;
    private int writtenLight = -1;
    private boolean spritesChanged;

    public GenericGrenadeVisual(VisualizationContext context, T entity, float partialTick) {
        super(context, entity, partialTick);
        nameTag = new NameTagComponent(context, entity);
        base =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, MODEL, 0)
                        .createInstance();
        base.setVisible(false);

        overlay =
                entity instanceof EntityDisperserCanister
                        ? instancerProvider()
                                .instancer(InstanceTypes.UV_TRANSFORMED, MODEL, 1)
                                .createInstance()
                        : null;
        if (overlay != null) overlay.setVisible(false);
    }

    public static void initModels() {}

    private static TextureAtlasSprite sprite(Identifier texture) {
        return Minecraft.getInstance().getAtlasManager().get(Sheets.ITEMS_MAPPER.apply(texture));
    }

    private static int overlayColor(int rgb) {
        return ARGB.colorFromFloat(
                1F,
                ((rgb >>> 16) & 0xFF) / 2 / 127F,
                ((rgb >>> 8) & 0xFF) / 2 / 127F,
                (rgb & 0xFF) / 2 / 127F);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick(), context.camera().rotation());
        nameTag.beginFrame(context);
    }

    private void writeFrame(float partialTick, Quaternionfc rotation) {
        if (entity instanceof EntityDisperserCanister canister) {
            ItemDisperser type = canister.getDisperserItem();
            resolveBase(type.iconTexture(0));
            Identifier texture = type.iconTexture(1);
            if (!texture.equals(overlayTexture)) {
                overlayTexture = texture;
                overlaySprite = sprite(texture);
                spritesChanged = true;
            }
            Fluid fluid = canister.getFluid();
            if (fluid != overlayFluid) {
                overlayFluid = fluid;
                NTMFluidProperty property = NTMFluidProperties.get(fluid);
                overlayColor = overlayColor(property == null ? 0xFFFFFF : property.color());
                spritesChanged = true;
            }
        } else {
            resolveBase(((IGenericGrenade) entity).getGrenade().iconTexture());
        }

        Vector3f position =
                entity.tickCount == 0
                        ? getVisualPosition(interpolatedPosition)
                        : getVisualPosition(partialTick, interpolatedPosition);
        pose.translation(position.x, position.y, position.z).scale(0.5F).rotate(rotation);
        int light = computePackedLight(partialTick);
        if (!spritesChanged && light == writtenLight && pose.equals(written)) return;
        spritesChanged = false;
        writtenLight = light;
        written.set(pose);
        base.setVisible(true);
        base.setTransform(pose);
        base.uvRegion(
                        baseSprite.getU0(),
                        baseSprite.getV0(),
                        baseSprite.getU1() - baseSprite.getU0(),
                        baseSprite.getV1() - baseSprite.getV0())
                .overlay(OverlayTexture.NO_OVERLAY)
                .light(light)
                .colorArgb(-1)
                .setChanged();
        if (overlay != null) {
            overlay.setVisible(true);
            overlay.setTransform(pose);
            overlay.uvRegion(
                            overlaySprite.getU0(),
                            overlaySprite.getV0(),
                            overlaySprite.getU1() - overlaySprite.getU0(),
                            overlaySprite.getV1() - overlaySprite.getV0())
                    .overlay(OverlayTexture.NO_OVERLAY)
                    .light(light)
                    .colorArgb(overlayColor)
                    .setChanged();
        }
    }

    private void resolveBase(Identifier texture) {
        if (texture.equals(baseTexture)) return;
        baseTexture = texture;
        baseSprite = sprite(texture);
        spritesChanged = true;
    }

    @Override
    protected void _delete() {
        base.delete();
        if (overlay != null) overlay.delete();
        nameTag.delete();
    }
}
