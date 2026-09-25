// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.lib.Library;
import com.hbm.particle.ParticleContrail;
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
import net.minecraft.util.Mth;

public final class ContrailVisual implements EffectVisual<ContrailEffect>, SimpleDynamicVisual {
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL)
                    .texture(TextureAtlas.LOCATION_PARTICLES)
                    .build();
    private static final Model MODEL = new SingleMeshModel(FogQuadMesh.INSTANCE, MATERIAL);
    private static final SpriteId SPRITE_ID =
            new SpriteId(TextureAtlas.LOCATION_PARTICLES, Library.id("contrail"));
    private final ContrailEffect effect;
    private final BillboardInstance[] instances = new BillboardInstance[ParticleContrail.SUBQUADS];
    private final int[] rgb = new int[ParticleContrail.SUBQUADS];
    private int lastAlpha = -1;

    public ContrailVisual(VisualizationContext ctx, ContrailEffect effect) {
        this.effect = effect;
        Vec3i origin = ctx.renderOrigin();
        float bx = (float) (effect.x - origin.getX());
        float by = (float) (effect.y - origin.getY());
        float bz = (float) (effect.z - origin.getZ());

        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(SPRITE_ID);
        float uOff = sprite.getU0(), vOff = sprite.getV0();
        float uSize = sprite.getU1() - sprite.getU0(), vSize = sprite.getV1() - sprite.getV0();

        Instancer<BillboardInstance> instancer =
                ctx.instancerProvider().instancer(InstanceTypes.BILLBOARD, MODEL);
        for (int i = 0; i < ParticleContrail.SUBQUADS; i++) {
            float m = effect.mod[i];
            rgb[i] =
                    channel(effect.r + m) << 16
                            | channel(effect.g + m) << 8
                            | channel(effect.b + m);

            BillboardInstance inst = instancer.createInstance();
            inst.position(
                    bx + (float) (effect.gaussX[i] * 0.5D) * effect.scale,
                    by + (float) (effect.gaussY[i] * 0.5D) * effect.scale,
                    bz + (float) (effect.gaussZ[i] * 0.5D) * effect.scale);
            inst.uvRegion(uOff, vOff, uSize, vSize);
            inst.overlay(OverlayTexture.NO_OVERLAY);
            inst.light(LightCoordsUtil.pack(15, 0));
            instances[i] = inst;
        }
        writeFrame(effect.age);
    }

    public static void initModels() {}

    private static int channel(float v) {
        return Mth.clamp((int) (v * 255F), 0, 255);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        writeFrame(effect.age + ctx.partialTick());
    }

    private void writeFrame(float ageF) {
        float fade = 1F - Mth.clamp(ageF / effect.maxAge, 0F, 1F);
        int a = channel(fade);
        if (a == lastAlpha) return;
        lastAlpha = a;

        float size = (fade + 0.5F) * effect.scale;
        for (int i = 0; i < ParticleContrail.SUBQUADS; i++) {
            instances[i].size(size);
            instances[i].colorArgb(a << 24 | rgb[i]);
            instances[i].setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance instance : instances) instance.delete();
    }
}
