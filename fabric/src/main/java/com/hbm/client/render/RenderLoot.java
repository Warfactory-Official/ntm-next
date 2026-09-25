// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockLoot;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class RenderLoot implements BlockEntityRenderer<BlockLoot.TileEntityLoot, RenderLoot.State> {
    private static final AABB SPRITE_BOUNDS = new AABB(.25, 0, .25, .75, .03125, .75);
    private final ItemModelResolver itemModelResolver;

    private final Map<ItemStack, AABB> extents = new WeakHashMap<>();
    private final ItemSprites sprites;
    private final Part[] nuke;
    private final Part[] shotgun;
    private final Part[][] trenchmaster;
    private final Part[][] ncrpa;

    public RenderLoot(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
        sprites = new ItemSprites(itemModelResolver);
        nuke =
                new Part[] {
                    part(
                            ResourceManager.projectiles,
                            Library.id("textures/models/projectiles/mini_nuke.png"),
                            new Matrix4f().scale(.5F).translate(1, .5F, 1),
                            true,
                            "MiniNuke")
                };
        shotgun =
                new Part[] {
                    part(
                            ResourceManager.maresleg,
                            ResourceManager.maresleg_tex,
                            new Matrix4f()
                                    .scale(.125F)
                                    .translate(3, 0, 0)
                                    .rotateY(radians(25))
                                    .rotateX(radians(90))
                                    .rotateY(radians(90)),
                            false)
                };
        trenchmaster =
                armor(
                        ResourceManager.armor_trenchmaster,
                        ResourceManager.trenchmaster_helmet_tex,
                        ResourceManager.trenchmaster_chest_tex,
                        ResourceManager.trenchmaster_arm_tex,
                        ResourceManager.trenchmaster_leg_tex,
                        "Light");
        ncrpa =
                armor(
                        ResourceManager.armor_ncr,
                        ResourceManager.ncrpa_helmet_tex,
                        ResourceManager.ncrpa_chest_tex,
                        ResourceManager.ncrpa_arm_tex,
                        ResourceManager.ncrpa_leg_tex,
                        "Eyes");
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    public static void fixedView(PoseStack pose, AABB model) {

        pose.translate(.5, .5 * model.maxZ, .5);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(.5F, .5F, .5F);
    }

    public static AABB fixedModel(
            ItemModelResolver resolver,
            ItemStackRenderState out,
            ItemStack stack,
            @Nullable Level level) {
        resolver.updateForTopItem(out, stack, ItemDisplayContext.FIXED, level, null, 0);
        return out.getModelBoundingBox();
    }

    public static AABB stackBounds(AABB model) {
        return SPRITE_BOUNDS.minmax(
                new AABB(
                        .5 + .5 * model.minX,
                        0,
                        .5 + .5 * model.minY,
                        .5 + .5 * model.maxX,
                        .5 * (model.maxZ - model.minZ),
                        .5 + .5 * model.maxY));
    }

    private static Part[][] armor(
            HFRWavefrontObject mesh,
            Identifier helmet,
            Identifier chest,
            Identifier arm,
            Identifier leg,
            String glow) {
        Matrix4f body =
                new Matrix4f().translate(.5F, 1.5F, .5F).scale(.0625F).rotateX(radians(180));
        Matrix4f arms = new Matrix4f(body).rotateX(radians(-3));
        Matrix4f rightLeg = new Matrix4f(body).rotateX(radians(-.1F));
        return new Part[][] {
            {
                new Part(
                        mesh,
                        body,
                        ArmorRenderTypes.LOOT_HELMET.apply(helmet),
                        false,
                        false,
                        "Helmet"),
                new Part(mesh, body, ArmorRenderTypes.LOOT_GLOW.apply(helmet), false, true, glow)
            },
            {
                part(mesh, chest, body, false, "Chest"),
                part(mesh, arm, arms, false, "LeftArm", "RightArm")
            },
            {part(mesh, leg, body, false, "LeftLeg"), part(mesh, leg, rightLeg, false, "RightLeg")},
            {
                part(mesh, leg, body, false, "LeftBoot"),
                part(mesh, leg, rightLeg, false, "RightBoot")
            }
        };
    }

    private static Part part(
            HFRWavefrontObject mesh,
            Identifier texture,
            Matrix4fc pose,
            boolean smooth,
            String... names) {
        return new Part(mesh, pose, RenderTypes.entityCutoutCull(texture), smooth, false, names);
    }

    private Part @Nullable [] parts(ItemStack stack) {
        EnumAmmo ammo = ModItems.AMMO_STANDARD.typeOf(stack);
        if (ammo != null
                && ammo.ordinal() >= EnumAmmo.NUKE_STANDARD.ordinal()
                && ammo.ordinal() <= EnumAmmo.NUKE_HIVE.ordinal()) return nuke;
        if (stack.is(ModItems.GUN_MARESLEG.get())) return shotgun;
        if (stack.getItem() instanceof ModArmorItem armor) {
            Part[][] family =
                    switch (armor.suit()) {
                        case TRENCHMASTER -> trenchmaster;
                        case NCRPA -> ncrpa;
                        default -> null;
                    };
            if (family != null) {
                int slot =
                        switch (stack.get(DataComponents.EQUIPPABLE).slot()) {
                            case HEAD -> 0;
                            case CHEST -> 1;
                            case LEGS -> 2;
                            case FEET -> 3;
                            default -> throw new AssertionError();
                        };
                return family[slot];
            }
        }
        return null;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {

        return true;
    }

    @Override
    public void extractRenderState(
            BlockLoot.TileEntityLoot be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.visualized = HbmBlockEntityVisual.hasVisual(be);
        state.items.clear();
        for (BlockLoot.TileEntityLoot.Entry entry : be.items) {
            Part[] parts = parts(entry.stack());
            if (parts != null) {
                state.items.add(
                        new ItemEntry(entry.x(), entry.y(), entry.z(), parts, null, null, false));
                continue;
            }
            List<ItemSprites.Pass> passes =
                    sprites.resolve(entry.stack(), (ClientLevel) be.getLevel());
            if (passes != null) {
                state.items.add(
                        new ItemEntry(entry.x(), entry.y(), entry.z(), null, passes, null, false));
                continue;
            }
            ItemStackRenderState model = new ItemStackRenderState();
            fixedModel(itemModelResolver, model, entry.stack(), be.getLevel());
            state.items.add(
                    new ItemEntry(
                            entry.x(),
                            entry.y(),
                            entry.z(),
                            null,
                            null,
                            model,
                            !WorldItem.draws(entry.stack(), ItemDisplayContext.FIXED)));
        }
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int order = 0;
        for (ItemEntry item : state.items) {
            if (state.visualized && !item.nativeOwned) continue;
            pose.pushPose();
            pose.translate(item.x, item.y, item.z);
            if (item.parts != null) {
                for (Part part : item.parts)
                    part.submit(pose, collector.order(order++), state.lightCoords);
            } else if (item.sprites != null) {
                pose.translate(.25, 0, .25);
                pose.scale(.5F, .5F, .5F);
                pose.mulPose(Axis.XP.rotationDegrees(90));
                for (ItemSprites.Pass pass : item.sprites)
                    pass.submit(pose, collector.order(order++), state.lightCoords);
            } else {
                fixedView(pose, item.model.getModelBoundingBox());
                item.model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            pose.popPose();
        }
    }

    private static final class Part {
        private final GroupObject[] groups;
        private final Matrix4f transform;
        private final RenderType type;
        private final boolean fullbright;
        private final AABB bounds;

        private Part(
                HFRWavefrontObject mesh,
                Matrix4fc transform,
                RenderType type,
                boolean smooth,
                boolean fullbright,
                String... names) {
            this.transform = new Matrix4f(transform);
            this.type = type;
            this.fullbright = fullbright;
            groups = new GroupObject[names.length == 0 ? mesh.groups.length : names.length];
            AABB.Builder bounds = new AABB.Builder();
            Vector3f position = new Vector3f();
            for (int i = 0; i < groups.length; i++) {
                GroupObject source = mesh.groups[names.length == 0 ? i : mesh.partId(names[i])];
                groups[i] = smooth ? source : source.flatShaded();
                float[] vertices = source.quads(true);
                for (int v = 0; v < vertices.length; v += GroupObject.STRIDE) {
                    bounds.include(
                            position.set(vertices[v], vertices[v + 1], vertices[v + 2])
                                    .mulPosition(transform));
                }
            }
            this.bounds = bounds.build();
        }

        private void submit(PoseStack pose, OrderedSubmitNodeCollector collector, int light) {
            pose.pushPose();
            pose.mulPose(transform);
            int usedLight = fullbright ? 0xF000F0 : light;
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (transformed, vertices) -> {
                        for (GroupObject group : groups)
                            group.emit(transformed, vertices, usedLight, -1, true);
                    });
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        private final List<ItemEntry> items = new ArrayList<>();
        public boolean visualized;
    }

    private record ItemEntry(
            double x,
            double y,
            double z,
            Part @Nullable [] parts,
            @Nullable List<ItemSprites.Pass> sprites,
            @Nullable ItemStackRenderState model,
            boolean nativeOwned) {}
}
