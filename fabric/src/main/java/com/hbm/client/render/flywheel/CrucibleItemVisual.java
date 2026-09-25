// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.CrucibleItemRenderer;
import com.hbm.client.render.HandPass;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class CrucibleItemVisual extends HbmItemVisual {
    private final ItemDisplayContext context;
    private final boolean charged;
    private final @Nullable ItemOwner owner;
    private final boolean hand;
    private final boolean gui;
    private final TransformedInstance hilt;
    private final TransformedInstance left;
    private final TransformedInstance right;
    private final TransformedInstance blade;
    private final PoseStack.Pose base = new PoseStack.Pose();
    private final PoseStack.Pose part = new PoseStack.Pose();

    public CrucibleItemVisual(
            VisualizationContext ctx,
            ItemDisplayContext context,
            boolean charged,
            @Nullable ItemOwner owner) {
        super(ctx);
        this.context = context;
        this.charged = charged;
        this.owner = owner;
        hand = HandPass.local(context, owner);
        gui = context == ItemDisplayContext.GUI;
        Material hiltMaterial =
                gui
                        ? ItemMaterials.flatCutout(ResourceManager.crucible_hilt, false)
                        : ItemMaterials.cutout(ResourceManager.crucible_hilt, false);
        Material guardMaterial =
                gui
                        ? ItemMaterials.flatCutout(ResourceManager.crucible_guard, false)
                        : ItemMaterials.cutout(ResourceManager.crucible_guard, false);
        hilt =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.crucible_sword,
                                CrucibleItemRenderer.HILT,
                                hiltMaterial));
        left =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.crucible_sword,
                                CrucibleItemRenderer.LEFT,
                                guardMaterial));
        right =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.crucible_sword,
                                CrucibleItemRenderer.RIGHT,
                                guardMaterial));
        blade =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.crucible_sword,
                                CrucibleItemRenderer.BLADE,
                                ItemMaterials.flatCutout(ResourceManager.crucible_blade, false)));
    }

    @Override
    public boolean update(ItemStack stack) {
        return ((ItemCrucible) stack.getItem()).canOperate(stack) == charged;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        base.setIdentity();
        base.pose().set(pose);
        boolean blocking = owner instanceof LivingEntity living && living.isBlocking();
        CrucibleItemRenderer.Plan plan = CrucibleItemRenderer.plan(hand, charged, blocking, base);
        int bodyLight = gui ? LightCoordsUtil.FULL_BRIGHT : light;
        write(hilt, base, bodyLight);
        part.set(base);
        CrucibleItemRenderer.guardPose(part, true, plan.guardAngle());
        write(left, part, bodyLight);
        part.set(base);
        CrucibleItemRenderer.guardPose(part, false, plan.guardAngle());
        write(right, part, bodyLight);
        blade.setVisible(plan.showBlade());
        if (plan.showBlade()) {
            part.set(base);
            CrucibleItemRenderer.bladePose(part);
            write(blade, part, LightCoordsUtil.FULL_BRIGHT);
        }
    }

    private static void write(TransformedInstance instance, PoseStack.Pose pose, int light) {
        write(instance, pose.pose(), -1, light);
    }
}
