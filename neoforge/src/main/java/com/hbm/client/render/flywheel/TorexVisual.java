// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.particle.FogQuadMesh;
import com.hbm.client.render.RenderTorex;
import com.hbm.entity.effect.EntityNukeTorex.Cloudlet;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public final class TorexVisual extends HbmDynamicEntityVisual<EntityNukeTorex> {

    private static final Identifier CLOUDLET_TEX =
            Library.id("textures/particle/particle_base.png");
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE)
                    .texture(CLOUDLET_TEX)
                    .mipmap(false)
                    .fog(FogShaders.NONE)
                    .build();
    private static final Model MODEL = new SingleMeshModel(FogQuadMesh.INSTANCE, MATERIAL);
    private static final Identifier FLARE_TEX = Library.id("textures/particle/flare.png");

    private static final Material FLARE_MATERIAL =
            SimpleMaterial.builder()
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .useLight(false)
                    .texture(FLARE_TEX)
                    .mipmap(false)
                    .fog(FogShaders.NONE)
                    .build();
    private static final Model FLARE_MODEL =
            new SingleMeshModel(FogQuadMesh.INSTANCE, FLARE_MATERIAL);

    private static final Material FLASH_MATERIAL =
            SimpleMaterial.builder()
                    .transparency(Transparency.LIGHTNING)
                    .writeMask(WriteMask.COLOR_DEPTH)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .useLight(false)
                    .texture(EffectVisuals.Shared.WHITE)
                    .mipmap(false)
                    .fog(FogShaders.NONE)
                    .build();
    private static final Model FLASH_MODEL =
            new SingleMeshModel(TorexFlashMesh.INSTANCE, FLASH_MATERIAL);
    private static final int FLARE_QUADS = 3;
    private final BillboardInstance[] flare = new BillboardInstance[FLARE_QUADS];
    private final TransformedInstance flash;
    private final Matrix4f flashPose = new Matrix4f();

    private final float[] flareGaussX = new float[FLARE_QUADS];
    private final float[] flareGaussY = new float[FLARE_QUADS];
    private final float[] flareGaussZ = new float[FLARE_QUADS];

    private Instancer<BillboardInstance> instancer;
    private BillboardInstance[] pool = new BillboardInstance[0];
    private int created;
    private int live;
    private boolean flareShown = true;
    private boolean flashShown = true;
    private int extentTick = Integer.MIN_VALUE;
    private float extent;

    public TorexVisual(VisualizationContext ctx, EntityNukeTorex entity, float partialTick) {
        super(ctx, entity, partialTick);

        Instancer<BillboardInstance> flareInstancer =
                ctx.instancerProvider().instancer(InstanceTypes.BILLBOARD, FLARE_MODEL);
        Random rand = new Random(entity.getId());
        for (int i = 0; i < FLARE_QUADS; i++) {
            flareGaussX[i] = (float) rand.nextGaussian();
            flareGaussY[i] = (float) rand.nextGaussian();
            flareGaussZ[i] = (float) rand.nextGaussian();
            BillboardInstance inst = flareInstancer.createInstance();
            inst.uvRegion(0F, 0F, 1F, 1F).overlay(OverlayTexture.NO_OVERLAY);
            inst.size(0F);
            flare[i] = inst;
        }

        this.flash =
                ctx.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLASH_MODEL)
                        .createInstance();
        this.flash.overlay(OverlayTexture.NO_OVERLAY);
        this.flash.light(LightCoordsUtil.FULL_BRIGHT);

        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        if (extentTick != entity.tickCount) {
            extentTick = entity.tickCount;
            extent = extent();
        }
        return sphereVisible(frustum, 0F, extent);
    }

    private float extent() {
        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        double reach = 0D;
        for (Cloudlet c : entity.cloudlets) {
            double size = c.getScale() * 2D;
            reach =
                    Math.max(
                            reach,
                            Math.sqrt(sq(c.posX - ex) + sq(c.posY - ey) + sq(c.posZ - ez)) + size);
            reach =
                    Math.max(
                            reach,
                            Math.sqrt(
                                            sq(c.prevPosX - ex)
                                                    + sq(c.prevPosY - ey)
                                                    + sq(c.prevPosZ - ez))
                                    + size);
        }
        float scale = (float) entity.getScaleVal();
        if (entity.age() < scale * RenderTorex.flareBaseDuration + 1) {
            double roller = entity.rollerSize;
            double spread = .5D * roller;
            for (int i = 0; i < FLARE_QUADS; i++) {
                double dy = entity.coreHeight + flareGaussY[i] * spread;
                reach =
                        Math.max(
                                reach,
                                Math.sqrt(
                                                sq(flareGaussX[i] * spread)
                                                        + sq(dy)
                                                        + sq(flareGaussZ[i] * spread))
                                        + 20D * roller);
            }
        }
        if (entity.age() < scale * RenderTorex.flashBaseDuration)
            reach =
                    Math.max(
                            reach,
                            .8D * entity.coreHeight
                                    + TorexFlashMesh.INSTANCE.boundingSphere().w() * 50D * scale);
        return (float) reach;
    }

    private static double sq(double value) {
        return value * value;
    }

    @Override
    protected void frame(Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        List<Cloudlet> cloudlets = entity.cloudlets;
        int n = cloudlets.size();
        ensureCapacity(n);

        Vec3i origin = renderOrigin();
        double ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        for (int i = 0; i < n; i++) {
            Cloudlet c = cloudlets.get(i);
            int shading = RenderTorex.cloudletShading(c, partialTick);
            BillboardInstance inst = pool[i];
            if (i >= live) inst.setVisible(true);
            inst.position(
                            (float) (c.prevPosX + (c.posX - c.prevPosX) * partialTick - ox),
                            (float) (c.prevPosY + (c.posY - c.prevPosY) * partialTick - oy),
                            (float) (c.prevPosZ + (c.posZ - c.prevPosZ) * partialTick - oz))
                    .size(c.getScale() * 2F);
            inst.colorArgb(shading);
            inst.setChanged();
        }
        for (int i = n; i < live; i++) pool[i].setVisible(false);
        live = n;

        writeFlare(partialTick, ox, oy, oz);
        writeFlash(partialTick, ox, oy, oz);
    }

    private void writeFlare(float partialTick, double ox, double oy, double oz) {
        float scale = (float) entity.getScaleVal();
        float flareDuration = scale * RenderTorex.flareBaseDuration;
        if (entity.age() >= flareDuration + 1) {
            if (flareShown) for (BillboardInstance inst : flare) inst.setVisible(false);
            flareShown = false;
            return;
        }
        float roller = (float) entity.rollerSize;
        float size = 10F * roller;
        float spread = 0.5F * roller;

        double age = Math.min(entity.age() + partialTick, flareDuration);
        float alpha = (float) Math.min(1D, (flareDuration - age) / flareDuration);
        int color = ARGB.colorFromFloat(alpha, 1F, 1F, 1F);

        float bx = (float) (entity.getX() - ox);
        float by = (float) (entity.getY() - oy);
        float bz = (float) (entity.getZ() - oz);
        for (int i = 0; i < FLARE_QUADS; i++) {
            BillboardInstance inst = flare[i];
            inst.position(
                            bx + flareGaussX[i] * spread,
                            by + (float) entity.coreHeight + flareGaussY[i] * spread,
                            bz + flareGaussZ[i] * spread)
                    .size(size * 2F);
            inst.colorArgb(color);
            inst.setChanged();
        }
    }

    private void writeFlash(float partialTick, double ox, double oy, double oz) {
        float scale = (float) entity.getScaleVal();
        float flashDuration = scale * RenderTorex.flashBaseDuration;
        if (entity.age() >= flashDuration) {
            if (flashShown) flash.setVisible(false);
            flashShown = false;
            return;
        }
        double intensityRaw = (entity.age() + partialTick) / flashDuration;
        double intensity = intensityRaw * Math.pow(Math.E, -intensityRaw) * 2.717391304D;
        float k = (float) (intensity * 50F * scale);

        float bx = (float) (entity.getX() - ox);
        float by = (float) (entity.getY() - oy);
        float bz = (float) (entity.getZ() - oz);
        flash.setTransform(
                flashPose
                        .identity()
                        .translate(bx, by + 0.8F * (float) entity.coreHeight, bz)
                        .scale(k));
        flash.colorArgb(ARGB.colorFromFloat((float) (1D - intensity), 1F, 1F, 1F));
        flash.setChanged();
    }

    private void ensureCapacity(int n) {
        if (created >= n) return;

        if (instancer == null) {
            instancer = instancerProvider().instancer(InstanceTypes.BILLBOARD, MODEL);
        }
        if (pool.length < n) {
            pool = Arrays.copyOf(pool, Math.max(n, pool.length * 2));
        }
        for (int i = created; i < n; i++) {
            BillboardInstance inst = instancer.createInstance();
            inst.uvRegion(0F, 0F, 1F, 1F).overlay(OverlayTexture.NO_OVERLAY);
            inst.size(0F);
            pool[i] = inst;
        }
        created = n;
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < created; i++) {
            pool[i].delete();
        }
        for (BillboardInstance inst : flare) {
            inst.delete();
        }
        flash.delete();
        pool = new BillboardInstance[0];
        created = 0;
        live = 0;
    }
}
