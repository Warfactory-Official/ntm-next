// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.render.FlatCutout;
import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.client.render.RenderBeam;
import com.hbm.client.render.RenderBulletMK4;
import com.hbm.client.render.RibbonPass;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.hud.HUDComponentAmmoCounter;
import com.hbm.items.weapon.sedna.hud.HUDComponentDurabilityBar;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.ItemRenderFatMan;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class LegoClient {
    private static final int GRENADE = ResourceManager.projectiles.partId("Grenade");
    public static final RenderBulletMK4.Tracer RENDER_GRAPHITE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(2F, 2F, 2F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutoutCull(ResourceManager.deb_graphite_tex),
                        (p, buffer) ->
                                ResourceManager.deb_graphite.render(p, buffer, state.light, -1));
                pose.popPose();
            };
    private static final int MINI_NUKE = ResourceManager.fatman.partId("MiniNuke");
    private static final int PANZERSCHRECK_ROCKET = ResourceManager.panzerschreck.partId("Rocket");
    private static final int PROJECTILE_ROCKET = ResourceManager.projectiles.partId("Rocket");
    private static final int MISSILE_MIRV = ResourceManager.projectiles.partId("MissileMIRV");
    private static final int CT_MORTAR = ResourceManager.charge_thrower.partId("Mortar");
    private static final int CT_OOMPH = ResourceManager.charge_thrower.partId("Oomph");
    private static final int CT_HOOK = ResourceManager.charge_thrower.partId("Hook");
    private static final int MISSILE = ResourceManager.missile_launcher.partId("Missile");

    private static final Identifier FLARE_TEX = Library.id("textures/particle/flare.png");
    public static HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY =
            new HUDComponentDurabilityBar();
    public static HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY_MIRROR =
            new HUDComponentDurabilityBar(true);
    public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO = new HUDComponentAmmoCounter(0);
    public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO_MIRROR =
            new HUDComponentAmmoCounter(0).mirror();
    public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO_NOCOUNTER =
            new HUDComponentAmmoCounter(0).noCounter();
    public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO_SECOND =
            new HUDComponentAmmoCounter(1);
    public static RenderBulletMK4.Tracer RENDER_STANDARD_BULLET = tracer(0xFFBF00, 0xFFFFFF, false);
    public static RenderBulletMK4.Tracer RENDER_FLECHETTE_BULLET =
            tracer(0x8C8C8C, 0xCACACA, false);
    public static RenderBulletMK4.Tracer RENDER_AP_BULLET = tracer(0xFF6A00, 0xFFE28D, false);
    public static RenderBulletMK4.Tracer RENDER_FRAGMENTATION = tracer(0xFF6A00, 0xFFE28D, true);
    public static RenderBulletMK4.Tracer RENDER_EXPRESS_BULLET = tracer(0x9E082E, 0xFF8A79, false);
    public static RenderBulletMK4.Tracer RENDER_DU_BULLET = tracer(0x5CCD41, 0xE9FF8D, false);
    public static RenderBulletMK4.Tracer RENDER_HE_BULLET = tracer(0xD8CA00, 0xFFF19D, true);
    public static RenderBulletMK4.Tracer RENDER_SM_BULLET = tracer(0x42A8DD, 0xFFFFFF, true);
    public static RenderBulletMK4.Tracer RENDER_BLACK_BULLET = tracer(0x000000, 0x7F006E, true);
    public static RenderBulletMK4.Tracer RENDER_TRACER_BULLET = tracer(0x9E082E, 0xFF8A79, true);
    public static RenderBulletMK4.Tracer RENDER_LEGENDARY_BULLET = tracer(0x7F006E, 0xFF7FED, true);
    public static RenderBulletMK4.Tracer RENDER_FLARE = flare(1F, 0.5F, 0.5F);
    public static RenderBulletMK4.Tracer RENDER_FLARE_SUPPLY = flare(0.5F, 0.5F, 1F);
    public static RenderBulletMK4.Tracer RENDER_FLARE_WEAPON = flare(0.5F, 1F, 0.5F);
    public static RenderBulletMK4.Tracer RENDER_GRENADE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.25F, 0.25F, 0.25F);
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.grenade_tex),
                        (p, buf) ->
                                ResourceManager.projectiles.renderPart(
                                        p, buf, state.light, -1, GRENADE));
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_NUKE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.translate(0, -1, 1F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.fatman_mininuke_tex),
                        (p, buf) ->
                                ResourceManager.fatman.renderPart(
                                        p, buf, state.light, -1, MINI_NUKE));
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_BOMB =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.0625F, 0.0625F, 0.0625F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.translate(0, -1, 1F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.cluster_submunition_tex),
                        (p, buf) ->
                                ResourceManager.fatman.renderPart(
                                        p, buf, state.light, -1, MINI_NUKE));
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_BIG_NUKE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.rocket_mirv_tex),
                        (p, buf) ->
                                ResourceManager.projectiles.renderPart(
                                        p, buf, state.light, -1, MISSILE_MIRV));
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_CT_MORTAR =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(0, 0, -6F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.charge_thrower_mortar_tex),
                        (p, buf) ->
                                ResourceManager.charge_thrower.renderPart(
                                        p, buf, state.light, -1, CT_MORTAR));
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_CT_MORTAR_CHARGE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(0, 0, -6F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.charge_thrower_mortar_tex),
                        (p, buf) -> {
                            ResourceManager.charge_thrower.renderPart(
                                    p, buf, state.light, -1, CT_MORTAR);
                            ResourceManager.charge_thrower.renderPart(
                                    p, buf, state.light, -1, CT_OOMPH);
                        });
                pose.popPose();
            };
    private static final int WIRE_SEGMENTS = 10;
    private static final double WIRE_GIRTH = 0.03125D;

    public static RenderBulletMK4.Tracer RENDER_CT_HOOK =
            new RenderBulletMK4.Tracer() {

                @Override
                public void extract(
                        EntityBulletBaseMK4 bullet,
                        RenderBulletMK4.State state,
                        float partialTicks) {
                    state.wire = false;
                    LivingEntity thrower = bullet.getThrower();
                    if (!(thrower instanceof Player player)) return;
                    ItemStack held = player.getMainHandItem();
                    if (held.getItem() != ModItems.GUN_CHARGE_THROWER.get()) return;
                    if (ItemGunChargeThrower.getLastHook(held) != bullet.getId()) return;

                    double bx = Mth.lerp(partialTicks, bullet.xOld, bullet.getX());
                    double by = Mth.lerp(partialTicks, bullet.yOld, bullet.getY());
                    double bz = Mth.lerp(partialTicks, bullet.zOld, bullet.getZ());
                    double ex = Mth.lerp(partialTicks, player.xOld, player.getX());
                    double ey = Mth.lerp(partialTicks, player.yOld, player.getY());
                    double ez = Mth.lerp(partialTicks, player.zOld, player.getZ());
                    float eyaw = Mth.lerp(partialTicks, player.yRotO, player.getYRot());
                    float epitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());

                    double ox = 0.125D;
                    double oy = 0.25D;
                    double oz = -0.75D;
                    float ax = (float) (-epitch / 180F * Math.PI);
                    double cx = Math.cos(ax);
                    double sx = Math.sin(ax);
                    double ry = oy * cx + oz * sx;
                    double rz = oz * cx - oy * sx;
                    float ay = (float) (-eyaw / 180F * Math.PI);
                    double cy = Math.cos(ay);
                    double sy = Math.sin(ay);
                    double rx = ox * cy + rz * sy;
                    rz = rz * cy - ox * sy;

                    double tx = ex - rx;
                    double ty = ey + player.getEyeHeight() - ry;
                    double tz = ez - rz;

                    state.wireX = tx - bx;
                    state.wireY = ty - by;
                    state.wireZ = tz - bz;
                    if (state.wireLight == null) state.wireLight = new int[WIRE_SEGMENTS];

                    double hang =
                            Math.min(
                                    Math.sqrt(
                                                    state.wireX * state.wireX
                                                            + state.wireY * state.wireY
                                                            + state.wireZ * state.wireZ)
                                            / 15D,
                                    0.5D);
                    for (int j = 0; j < WIRE_SEGMENTS; j++) {
                        double sagJ = Math.sin((double) j / WIRE_SEGMENTS * Math.PI) * hang;
                        double sagK = Math.sin((double) (j + 1) / WIRE_SEGMENTS * Math.PI) * hang;
                        double ja = j + 0.5D;
                        BlockPos at =
                                BlockPos.containing(
                                        bx + state.wireX / WIRE_SEGMENTS * ja,
                                        by + state.wireY / WIRE_SEGMENTS * ja - (sagJ + sagK) / 2D,
                                        bz + state.wireZ / WIRE_SEGMENTS * ja);
                        state.wireLight[j] =
                                LightCoordsUtil.pack(
                                        bullet.level().getBrightness(LightLayer.BLOCK, at),
                                        bullet.level().getBrightness(LightLayer.SKY, at));
                    }
                    state.wire = true;
                }

                @Override
                public void draw(
                        RenderBulletMK4.State state,
                        PoseStack pose,
                        SubmitNodeCollector collector) {
                    pose.pushPose();
                    pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
                    pose.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180));
                    pose.scale(0.125F, 0.125F, 0.125F);
                    pose.mulPose(Axis.YN.rotationDegrees(90));
                    pose.mulPose(Axis.ZP.rotationDegrees(180));
                    pose.translate(0, 0, -6F);
                    collector.submitCustomGeometry(
                            pose,
                            RenderTypes.entityCutout(ResourceManager.charge_thrower_hook_tex),
                            (p, buf) ->
                                    ResourceManager.charge_thrower.renderPart(
                                            p, buf, state.light, -1, CT_HOOK));
                    pose.popPose();

                    if (state.wire) drawWire(state, pose, collector);
                }
            };

    private static void drawWire(
            RenderBulletMK4.State state, PoseStack pose, SubmitNodeCollector collector) {
        double dx = state.wireX;
        double dy = state.wireY;
        double dz = state.wireZ;
        double hang = Math.min(Math.sqrt(dx * dx + dy * dy + dz * dz) / 15D, 0.5D);

        double hyp = Math.sqrt(dx * dx + dz * dz);
        double yaw = Math.atan2(dx, dz);
        double pitch = Math.atan2(dy, hyp);
        double rotator = Math.PI * 0.5D;
        double newPitch = pitch + rotator;
        double newYaw = yaw + rotator;
        double iZ = Math.cos(yaw) * Math.cos(newPitch) * WIRE_GIRTH;
        double iX = Math.sin(yaw) * Math.cos(newPitch) * WIRE_GIRTH;
        double iY = Math.sin(newPitch) * WIRE_GIRTH;
        double jZ = Math.cos(newYaw) * WIRE_GIRTH;
        double jX = Math.sin(newYaw) * WIRE_GIRTH;

        collector.submitCustomGeometry(
                pose,
                FlatCutout.of(ResourceManager.wire_greyscale_tex),
                (p, buf) -> {
                    for (int j = 0; j < WIRE_SEGMENTS; j++) {
                        int k = j + 1;
                        double sagJ = Math.sin((double) j / WIRE_SEGMENTS * Math.PI) * hang;
                        double sagK = Math.sin((double) k / WIRE_SEGMENTS * Math.PI) * hang;
                        segment(
                                buf,
                                p,
                                state.wireLight[j],
                                dx * j / WIRE_SEGMENTS,
                                dy * j / WIRE_SEGMENTS - sagJ,
                                dz * j / WIRE_SEGMENTS,
                                dx * k / WIRE_SEGMENTS,
                                dy * k / WIRE_SEGMENTS - sagK,
                                dz * k / WIRE_SEGMENTS,
                                iX,
                                iY,
                                iZ,
                                jX,
                                jZ);
                    }
                });
    }

    private static void segment(
            VertexConsumer buf,
            PoseStack.Pose p,
            int light,
            double x,
            double y,
            double z,
            double a,
            double b,
            double c,
            double iX,
            double iY,
            double iZ,
            double jX,
            double jZ) {
        double dx = a - x;
        double dy = b - y;
        double dz = c - z;
        int wrap = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 8);
        if (dx + dz < 0) {
            wrap *= -1;
            jZ *= -1;
            jX *= -1;
        }

        wire(buf, p, x + iX, y + iY, z + iZ, 0, 0, light);
        wire(buf, p, x - iX, y - iY, z - iZ, 0, 1, light);
        wire(buf, p, a - iX, b - iY, c - iZ, wrap, 1, light);
        wire(buf, p, a + iX, b + iY, c + iZ, wrap, 0, light);
        wire(buf, p, x + jX, y, z + jZ, 0, 0, light);
        wire(buf, p, x - jX, y, z - jZ, 0, 1, light);
        wire(buf, p, a - jX, b, c - jZ, wrap, 1, light);
        wire(buf, p, a + jX, b, c + jZ, wrap, 0, light);
    }

    private static void wire(
            VertexConsumer buf,
            PoseStack.Pose p,
            double x,
            double y,
            double z,
            float u,
            float v,
            int light) {
        Vertices.emit(buf, p, (float) x, (float) y, (float) z, 0xFF606060, u, v, light, 0F, 1F, 0F);
    }

    public static RenderBulletMK4.Tracer RENDER_NUKE_BALEFIRE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.translate(0, -1, 1F);
                ItemRenderFatMan.renderBalefire(collector, pose, state.light);
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_HIVE =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.translate(0, 0, 3.5F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.panzerschreck_tex),
                        (p, buf) ->
                                ResourceManager.panzerschreck.renderPart(
                                        p, buf, state.light, -1, PANZERSCHRECK_ROCKET));
                pose.popPose();
            };
    public static RenderBulletMK4.Tracer RENDER_RPZB =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.translate(0, 0, 3.5F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.panzerschreck_tex),
                        (p, buf) ->
                                ResourceManager.panzerschreck.renderPart(
                                        p, buf, state.light, -1, PANZERSCHRECK_ROCKET));
                pose.popPose();
                pose.translate(0.375F, 0, 0);
                if (state.length > 0)
                    renderBulletStandard(
                            pose,
                            collector,
                            0x808080,
                            0xFFF2A7,
                            state.length * 2,
                            true,
                            state.light);
            };
    public static RenderBulletMK4.Tracer RENDER_QD =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.rocket_tex),
                        (p, buf) ->
                                ResourceManager.projectiles.renderPart(
                                        p, buf, state.light, -1, PROJECTILE_ROCKET));
                pose.popPose();
                pose.translate(0.375F, 0, 0);
                if (state.length > 0)
                    renderBulletStandard(
                            pose,
                            collector,
                            0x808080,
                            0xFFF2A7,
                            state.length * 2,
                            true,
                            state.light);
            };
    public static RenderBulletMK4.Tracer RENDER_ML =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.25F, 0.25F, 0.25F);
                pose.mulPose(Axis.YN.rotationDegrees(90));
                pose.translate(0, -1, -4.5F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.missile_launcher_tex),
                        (p, buf) ->
                                ResourceManager.missile_launcher.renderPart(
                                        p, buf, state.light, -1, MISSILE));
                pose.popPose();
                pose.translate(0.375F, 0, 0);
                if (state.length > 0)
                    renderBulletStandard(
                            pose,
                            collector,
                            0x808080,
                            0xFFF2A7,
                            state.length * 2,
                            true,
                            state.light);
            };
    public static RenderBeam.BeamDrawer RENDER_NI4NI_BOLT = bolt(0xAAD2E5, 0xFFFFFF);
    public static RenderBeam.BeamDrawer RENDER_CRACKLE = bolt(0xE3D692, 0xFFFFFF);
    public static RenderBeam.BeamDrawer RENDER_BLACK_LIGHTNING = bolt(0x4C3093, 0x000000);
    public static RenderBeam.BeamDrawer RENDER_FOLLY =
            (state, pose, collector) -> {
                double age = beamAge(state);

                pose.pushPose();
                if (state.cameraOrientation != null) pose.mulPose(state.cameraOrientation);
                float outer = (float) ((1 - age) * 7.5 + 1.5);
                float outerAlpha = 0.5F * (float) age;
                float innerAlpha = 0.75F * (float) age;
                collector.submitCustomGeometry(
                        pose,
                        ParticleRenderTypes.flashNoFog(FLARE_TEX),
                        (p, buf) -> {
                            flareQuad(p, buf, outer, 1F, 1F, 1F, outerAlpha);
                            flareQuad(p, buf, outer * 0.5F, 1F, 1F, 1F, innerAlpha);
                        });
                pose.popPose();

                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(180 - state.yaw));
                pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90));
                pose.scale((float) ((1 - age) * 25 + 2.5), 1F, (float) ((1 - age) * 25 + 2.5));
                int shade = (int) (0x20 * age);
                int colorInner = (shade << 16) | (shade << 8) | shade;
                BeamPronter.prontBeamNoFog(
                        pose,
                        collector,
                        new Vec3(0, state.beamLength, 0),
                        BeamPronter.EnumWaveType.RANDOM,
                        colorInner,
                        colorInner,
                        state.age / 3,
                        (int) (state.beamLength / 2 + 1),
                        0F,
                        8,
                        0.0625F);
                pose.popPose();
            };
    public static RenderBeam.BeamDrawer RENDER_LIGHTNING = lightning(0.5F);
    public static RenderBeam.BeamDrawer RENDER_LIGHTNING_SUB = lightning(0.15F);
    public static RenderBeam.BeamDrawer RENDER_TAU = tau(0x30, 0x25, 0x10, 0xFFBF00);
    public static RenderBeam.BeamDrawer RENDER_TAU_CHARGE = tau(0x60, 0x50, 0x30, 0xFFF0A0);
    public static RenderBeam.BeamDrawer RENDER_LASER_RED = laser(0x80, 0x15, 0x15);
    public static RenderBeam.BeamDrawer RENDER_LASER_EMERALD = laser(0x15, 0x80, 0x15);
    public static RenderBeam.BeamDrawer RENDER_LASER_CYAN = laser(0x15, 0x15, 0x80);
    public static RenderBeam.BeamDrawer RENDER_LASER_PURPLE = laser(0x60, 0x15, 0x80);
    public static RenderBeam.BeamDrawer RENDER_LASER_WHITE = laser(0x15, 0x15, 0x15);

    private static double beamAge(RenderBeam.State state) {
        return Mth.clamp(
                1D - ((double) state.age - 2 + state.partialTicks) / (double) state.config.expires,
                0,
                1);
    }

    private static RenderBeam.BeamDrawer bolt(int dark, int light) {
        return (state, pose, collector) -> {
            double age = beamAge(state);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180 - state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90));
            pose.scale((float) (age * 5), 1F, (float) (age * 5));
            pose.translate(0, state.beamLength, 0);
            pose.mulPose(Axis.ZN.rotationDegrees(90));
            renderBeamTracer(pose, collector, dark, light, state.beamLength);
            pose.popPose();
        };
    }

    private static RenderBulletMK4.Tracer flare(float r, float g, float b) {
        return (state, pose, collector) -> {
            if (state.age < 2) return;
            float scale =
                    (float) (Math.min(5, (state.age - 2) * 0.5) * (0.8 + Math.random() * 0.4));

            pose.pushPose();
            if (state.cameraOrientation != null) pose.mulPose(state.cameraOrientation);
            float outer = scale;
            float inner = scale * 0.5F;
            collector.submitCustomGeometry(
                    pose,
                    WeaponRenderTypes.flash(FLARE_TEX),
                    (p, buf) -> {
                        flareQuad(p, buf, outer, r, g, b, 0.5F);
                        flareQuad(p, buf, inner, 1F, 1F, 1F, 0.75F);
                    });
            pose.popPose();
        };
    }

    private static void flareQuad(
            PoseStack.Pose p, VertexConsumer buf, float s, float r, float g, float b, float a) {
        int light = LightCoordsUtil.FULL_BRIGHT;
        int color = ARGB.colorFromFloat(a, r, g, b);
        Vertices.emit(buf, p, -s, -s, 0, color, 1, 1, light, 0, 0, 1);
        Vertices.emit(buf, p, -s, s, 0, color, 1, 0, light, 0, 0, 1);
        Vertices.emit(buf, p, s, s, 0, color, 0, 0, light, 0, 0, 1);
        Vertices.emit(buf, p, s, -s, 0, color, 0, 1, light, 0, 0, 1);
    }

    private static RenderBeam.BeamDrawer tau(int r, int g, int b, int tracerDark) {
        return (state, pose, collector) -> {
            double age =
                    Mth.clamp(
                            1D
                                    - ((double) state.age - 2 + state.partialTicks)
                                            / (double) state.config.expires,
                            0,
                            1);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180 - state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90));

            pose.pushPose();
            Vec3 delta = new Vec3(0, state.beamLength, 0);
            pose.scale((float) (age / 2 + 0.5), 1F, (float) (age / 2 + 0.5));
            int colorInner = ((int) (r * age) << 16) | ((int) (g * age) << 8) | (int) (b * age);
            BeamPronter.prontBeamNoFog(
                    pose,
                    collector,
                    delta,
                    BeamPronter.EnumWaveType.RANDOM,
                    colorInner,
                    colorInner,
                    (state.age + state.entityId) / 2,
                    (int) (state.beamLength / 2 + 1),
                    0.3F,
                    2,
                    0.0625F);
            pose.popPose();

            pose.scale((float) (age * 2), 1F, (float) (age * 2));
            pose.translate(0, state.beamLength, 0);
            pose.mulPose(Axis.ZN.rotationDegrees(90));
            renderBeamTracer(pose, collector, tracerDark, 0xFFFFFF, state.beamLength);

            pose.popPose();
        };
    }

    private static RenderBeam.BeamDrawer lightning(float baseScale) {
        return (state, pose, collector) -> {
            double age =
                    Mth.clamp(
                            1D
                                    - ((double) state.age - 2 + state.partialTicks)
                                            / (double) state.config.expires,
                            0,
                            1);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180 - state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90));
            Vec3 delta = new Vec3(0, state.beamLength, 0);
            pose.scale((float) (age / 2 + baseScale), 1F, (float) (age / 2 + baseScale));
            float scale = 0.075F;
            int colorInner =
                    ((int) (0x20 * age) << 16) | ((int) (0x20 * age) << 8) | (int) (0x40 * age);
            int colorOuter =
                    ((int) (0x40 * age) << 16) | ((int) (0x40 * age) << 8) | (int) (0x80 * age);
            int segments = (int) (state.beamLength / 2 + 1);
            BeamPronter.prontBeamNoFog(
                    pose,
                    collector,
                    delta,
                    BeamPronter.EnumWaveType.RANDOM,
                    colorInner,
                    colorInner,
                    (state.age + state.entityId) / 3,
                    segments,
                    scale,
                    4,
                    0.25F);
            BeamPronter.prontBeamNoFog(
                    pose,
                    collector,
                    delta,
                    BeamPronter.EnumWaveType.RANDOM,
                    colorOuter,
                    colorOuter,
                    state.age + state.entityId,
                    segments,
                    scale * 7F,
                    2,
                    0.0625F);
            BeamPronter.prontBeamNoFog(
                    pose,
                    collector,
                    delta,
                    BeamPronter.EnumWaveType.RANDOM,
                    colorOuter,
                    colorOuter,
                    (state.age + state.entityId) / 2,
                    segments,
                    scale * 7F,
                    2,
                    0.0625F);
            pose.popPose();
        };
    }

    private static RenderBeam.BeamDrawer laser(int r, int g, int b) {
        return (state, pose, collector) -> {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180 - state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90));
            Vec3 delta = new Vec3(0, state.beamLength, 0);
            double age =
                    Mth.clamp(
                            1D
                                    - ((double) state.age - 2 + state.partialTicks)
                                            / (double) state.config.expires,
                            0,
                            1);
            pose.scale((float) (age / 2 + 0.5), 1F, (float) (age / 2 + 0.5));
            int colorInner = ((int) (r * age) << 16) | ((int) (g * age) << 8) | (int) (b * age);
            BeamPronter.prontBeamNoFog(
                    pose,
                    collector,
                    delta,
                    BeamPronter.EnumWaveType.RANDOM,
                    colorInner,
                    colorInner,
                    state.age / 3,
                    (int) (state.beamLength / 2 + 1),
                    0F,
                    4,
                    0.025F);
            pose.popPose();
        };
    }

    private static RenderBulletMK4.Tracer tracer(int dark, int light, boolean fullbright) {
        return (state, pose, collector) -> {
            if (state.length <= 0) return;
            renderBulletStandard(
                    pose, collector, dark, light, state.length, fullbright, state.light);
        };
    }

    public static void renderBulletStandard(
            PoseStack pose,
            SubmitNodeCollector collector,
            int dark,
            int light,
            double length,
            boolean fullbright,
            int packedLight) {
        renderBulletStandard(
                pose,
                collector,
                dark,
                light,
                length,
                0.03125D,
                0.03125D * 0.25D,
                fullbright,
                packedLight);
    }

    public static void renderBulletStandard(
            PoseStack pose,
            SubmitNodeCollector collector,
            int dark,
            int light,
            double length,
            double widthF,
            double widthB,
            boolean fullbright,
            int packedLight) {
        submitRibbon(
                pose,
                collector,
                fullbright ? WeaponRenderTypes.TRACER_FULLBRIGHT : WeaponRenderTypes.TRACER,
                dark,
                light,
                length,
                widthF,
                widthB,
                fullbright ? ItemRenderWeaponBase.FULL_BRIGHT : packedLight);
    }

    private static void renderBeamTracer(
            PoseStack pose, SubmitNodeCollector collector, int dark, int light, double length) {
        submitRibbon(
                pose,
                collector,
                WeaponRenderTypes.TRACER_FULLBRIGHT_NO_FOG,
                dark,
                light,
                length,
                0.03125D,
                0.03125D * 0.25D,
                ItemRenderWeaponBase.FULL_BRIGHT);
    }

    private static void submitRibbon(
            PoseStack pose,
            SubmitNodeCollector collector,
            RenderType type,
            int dark,
            int light,
            double length,
            double widthF,
            double widthB,
            int lc) {

        if (RibbonPass.isShadow()) return;
        collector.submitCustomGeometry(
                pose,
                type,
                (p, tess) ->
                        Vertices.emitRibbon(
                                tess,
                                p,
                                (float) length,
                                (float) widthF,
                                (float) widthB,
                                0xFF000000 | light,
                                0xFF000000 | dark,
                                lc));
    }
}
