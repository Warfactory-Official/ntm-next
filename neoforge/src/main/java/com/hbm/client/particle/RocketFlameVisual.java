// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Random;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public final class RocketFlameVisual
        implements EffectVisual<RocketFlameEffect>, SimpleDynamicVisual {
    private static final int SUBQUADS = 10;
    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    private static final Material MATERIAL =
            SimpleMaterial.builder()
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .texture(Library.id("textures/particle/particle_base.png"))
                    .mipmap(false)
                    .build();
    private static final Model MODEL = new SingleMeshModel(FogQuadMesh.INSTANCE, MATERIAL);
    private final RocketFlameEffect effect;
    private final BillboardInstance[] instances = new BillboardInstance[SUBQUADS];
    private final float[] offX = new float[SUBQUADS];
    private final float[] offY = new float[SUBQUADS];
    private final float[] offZ = new float[SUBQUADS];
    private final float[] add = new float[SUBQUADS];
    private final float[] baseScale = new float[SUBQUADS];
    private final int originX, originY, originZ;

    public RocketFlameVisual(VisualizationContext ctx, RocketFlameEffect effect) {
        this.effect = effect;
        Vec3i origin = ctx.renderOrigin();
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();

        Random rand =
                new Random(
                        Double.doubleToLongBits(effect.spawnX) * 31L
                                ^ Double.doubleToLongBits(effect.spawnY) * 17L
                                ^ Double.doubleToLongBits(effect.spawnZ));
        Instancer<BillboardInstance> instancer =
                ctx.instancerProvider().instancer(InstanceTypes.BILLBOARD, MODEL);
        for (int i = 0; i < SUBQUADS; i++) {
            offX[i] = (float) (rand.nextGaussian() - 1D) * 0.2F;
            offY[i] = (float) (rand.nextGaussian() - 1D) * 0.5F;
            offZ[i] = (float) (rand.nextGaussian() - 1D) * 0.2F;
            add[i] = rand.nextFloat() * 0.3F;
            baseScale[i] = rand.nextFloat() * 0.5F + 0.1F;
            instances[i] = instancer.createInstance();
            instances[i].overlay(OverlayTexture.NO_OVERLAY).light(LIGHT);
        }
        writeFrame(0F);
    }

    public static void initModels() {}

    private static int clamp8(float v) {
        return Mth.clamp((int) (v * 255F), 0, 255);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        float px = (float) (effect.interpX(partialTick) - originX);
        float py = (float) (effect.interpY(partialTick) - originY);
        float pz = (float) (effect.interpZ(partialTick) - originZ);

        float ageF = effect.age + partialTick;
        float t = Mth.clamp(ageF / effect.maxAge, 0F, 1F);
        float spread = ((float) Math.pow(t * 4F, 1.5) + 1F) * effect.baseScale;
        float dark = 1F - Mth.clamp(ageF / (effect.maxAge * 0.25F), 0F, 1F);
        float alpha = (float) Math.sqrt(1F - t) * 0.75F;
        int a = clamp8(alpha);
        for (int i = 0; i < SUBQUADS; i++) {
            float scale = (baseScale[i] + t * 2F) * effect.baseScale;
            int r = clamp8(dark + add[i]);
            int g = clamp8(0.6F * dark + add[i]);
            int b = clamp8(add[i]);
            int argb = ARGB.color(a, r, g, b);

            instances[i]
                    .position(px + offX[i] * spread, py + offY[i] * spread, pz + offZ[i] * spread)
                    .size(2F * scale);
            instances[i].colorArgb(argb);
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
