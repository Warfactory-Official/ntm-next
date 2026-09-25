// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class RadiationFogVisual
        implements EffectVisual<RadiationFogEffect>, SimpleDynamicVisual {
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL)
                    .texture(TextureAtlas.LOCATION_PARTICLES)
                    .build();
    private static final Model MODEL = new SingleMeshModel(FogQuadMesh.INSTANCE, MATERIAL);
    private static final SpriteId SPRITE_ID =
            new SpriteId(TextureAtlas.LOCATION_PARTICLES, Library.id("rad_fog"));
    private final RadiationFogEffect effect;
    private final BillboardInstance[] instances =
            new BillboardInstance[RadiationFogEffect.QUAD_COUNT];
    private int lastArgb;

    public RadiationFogVisual(VisualizationContext ctx, RadiationFogEffect effect) {
        this.effect = effect;
        Vec3i origin = ctx.renderOrigin();
        float bx = (float) (effect.x - origin.getX());
        float by = (float) (effect.y - origin.getY());
        float bz = (float) (effect.z - origin.getZ());

        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(SPRITE_ID);
        float offU = sprite.getU0(), offV = sprite.getV0();
        float scaleU = sprite.getU1() - sprite.getU0(), scaleV = sprite.getV1() - sprite.getV0();

        Instancer<BillboardInstance> instancer =
                ctx.instancerProvider().instancer(InstanceTypes.BILLBOARD, MODEL);
        int argb = effect.colorAt(effect.age);
        this.lastArgb = argb;
        for (int i = 0; i < RadiationFogEffect.QUAD_COUNT; i++) {
            float px = bx + (float) (RadiationFogEffect.OFF_X[i] + RadiationFogEffect.JIT_X[i]);
            float py = by + (float) (RadiationFogEffect.OFF_Y[i] + RadiationFogEffect.JIT_Y[i]);
            float pz = bz + (float) (RadiationFogEffect.OFF_Z[i] + RadiationFogEffect.JIT_Z[i]);
            BillboardInstance inst = instancer.createInstance();
            inst.position(px, py, pz)
                    .size(RadiationFogEffect.SIZE[i])
                    .uvRegion(offU, offV, scaleU, scaleV);
            inst.colorArgb(argb);
            inst.light(LightCoordsUtil.pack(15, 0));
            inst.overlay(OverlayTexture.NO_OVERLAY);
            inst.setChanged();
            instances[i] = inst;
        }
    }

    public static void initModels() {}

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        int argb = effect.colorAt(effect.age + ctx.partialTick());
        if (argb == lastArgb) return;
        lastArgb = argb;
        for (BillboardInstance inst : instances) {
            inst.colorArgb(argb);
            inst.setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance inst : instances) inst.delete();
    }
}
