// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.GunItemRenderer;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.loader.UnitQuad;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class GunItemVisual extends HbmItemVisual {
    private final GunItemRenderer renderer;
    private final ItemDisplayContext context;
    private final @Nullable ItemOwner owner;
    private final GunItemRenderer.DrawSet set;
    private final Signature signature;
    private final List<Draw> draws = new ArrayList<>();
    private final List<Flash> flashes = new ArrayList<>();
    private final Matrix4f world = new Matrix4f();
    private ItemStack stack;

    public GunItemVisual(
            VisualizationContext ctx,
            GunItemRenderer renderer,
            GunItemRenderer.Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        super(ctx);
        this.renderer = renderer;
        this.context = context;
        this.owner = owner;
        this.stack = stack;
        set = argument.set();
        signature = signature(renderer, argument);

        if (argument.firstPerson() != null) return;
        renderer.visitDraws(
                argument,
                (index, model, parts, ops, texture, color, fullBright, balefire) -> {
                    Material material = ItemMaterials.cutout(texture, false);
                    List<Tinted> instances = new ArrayList<>();
                    for (int part : parts) {
                        instances.add(
                                new Tinted(
                                        transformed(ItemMaterials.part(model, part, material)),
                                        color,
                                        -1));
                        if (!balefire) continue;
                        for (int layer = 0; layer < GunItemRenderer.BALEFIRE_LAYERS; layer++) {
                            instances.add(
                                    new Tinted(
                                            affineUv(
                                                    ItemMaterials.part(
                                                            model,
                                                            part,
                                                            ItemMaterials.balefireGlint(
                                                                    ResourceManager.glint_bf_tex))),
                                            WeaponRenderTypes.BALEFIRE_GLINT_TINT,
                                            layer));
                        }
                    }
                    draws.add(new Draw(ops, fullBright, instances));
                });
        renderer.visitMuzzles(argument, (index, muzzle) -> flashes.add(new Flash(muzzle)));
    }

    private static Signature signature(
            GunItemRenderer renderer, GunItemRenderer.Argument argument) {
        List<Object> draws = new ArrayList<>();
        renderer.visitDraws(
                argument,
                (index, model, parts, ops, texture, color, fullBright, balefire) -> {
                    draws.add(index);
                    draws.add(texture);
                    draws.add(color);
                });
        List<Integer> muzzles = new ArrayList<>();
        renderer.visitMuzzles(argument, (index, muzzle) -> muzzles.add(index));
        return new Signature(draws, muzzles);
    }

    @Override
    public boolean update(ItemStack stack) {
        GunItemRenderer.Argument argument = renderer.argument(stack, context, owner);
        if (argument.firstPerson() != null || !signature(renderer, argument).equals(signature))
            return false;
        this.stack = stack;
        return true;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (Draw draw : draws) {
            world.set(pose).mul(draw.ops());
            int drawLight = draw.fullBright() ? LightCoordsUtil.FULL_BRIGHT : light;
            for (Tinted tinted : draw.instances()) {
                if (tinted.instance() instanceof AffineUvTransformedInstance glint) {

                    Matrix4f uv =
                            WeaponRenderTypes.balefireGlintMatrix(
                                    tinted.layer(), WeaponRenderTypes.FATMAN_GLINT_SPEED);
                    glint.uv(uv.m00(), uv.m10(), uv.m01(), uv.m11(), uv.m30(), uv.m31());
                }
                write(tinted.instance(), world, tinted.color(), drawLight);
            }
        }
        if (flashes.isEmpty()) return;
        GunItemRenderer.Shot shot = renderer.shot(stack, set, owner);
        for (Flash flash : flashes) flash.write(pose, shot);
    }

    private record Draw(Matrix4fc ops, boolean fullBright, List<Tinted> instances) {}

    private record Tinted(TransformedInstance instance, int color, int layer) {}

    private record Signature(List<Object> draws, List<Integer> muzzles) {}

    private final class Flash implements UnitQuad.Visitor {
        private final GunItemRenderer.BakedMuzzle muzzle;
        private final List<PosedInstance> slots = new ArrayList<>();
        private final List<UnitQuad> slotQuads = new ArrayList<>();
        private final PoseStack.Pose muzzlePose = new PoseStack.Pose();
        private final Matrix4f quadScratch = new Matrix4f();
        private final Matrix4f quadPose = new Matrix4f();
        private int used;
        private int argb;

        Flash(GunItemRenderer.BakedMuzzle muzzle) {
            this.muzzle = muzzle;
        }

        void write(Matrix4fc pose, GunItemRenderer.Shot shot) {
            used = 0;
            double fire =
                    shot.lastShot() < 0L
                            ? Double.NaN
                            : muzzle.kind().fire(shot.lastShot(), muzzle.duration());
            if (!Double.isNaN(fire)) {
                muzzlePose.setIdentity();
                muzzlePose.mulPose(pose);
                GunItemRenderer.muzzlePose(muzzlePose, muzzle, shot.shotRand());
                argb = muzzle.kind().argb(muzzle.color());
                muzzle.kind().quads(fire, muzzle.length(), quadScratch, this);
            }
            for (int i = used; i < slots.size(); i++) slots.get(i).setVisible(false);
        }

        @Override
        public void accept(UnitQuad quad, Matrix4fc matrix) {
            if (used == slots.size() || slotQuads.get(used) != quad) {
                PosedInstance instance =
                        posed(
                                ItemMaterials.group(
                                        quad.group(),
                                        true,
                                        ItemMaterials.flash(
                                                muzzle.kind().texture(), muzzle.kind().depth())));
                if (used == slots.size()) {
                    slots.add(instance);
                    slotQuads.add(quad);
                } else {
                    release(slots.set(used, instance));
                    slotQuads.set(used, quad);
                }
            }
            PosedInstance slot = slots.get(used++);
            slot.setVisible(true);
            HbmItemVisual.write(
                    slot,
                    quadPose.set(muzzlePose.pose()).mul(matrix),
                    muzzlePose.normal(),
                    argb,
                    ItemRenderWeaponBase.FLASH_LIGHT);
        }
    }
}
