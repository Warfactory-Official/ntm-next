// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.armor.ArmorFullSetBonus;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.tags.HbmEntityTypeTags;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ArmorOverheadRenderer {

    private static final double RANGE_SQ = 4096D;
    private static final float LABEL_SCALE = 0.016666668F * 1.6F;

    public static final RenderPipeline THERMAL_LINES_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.LINES_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_thermal_lines")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA)))
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.ALWAYS_PASS, false)));
    private static final RenderType THERMAL_LINES =
            RenderType.create(
                    "ntm_thermal_lines",
                    RenderSetup.builder(THERMAL_LINES_PIPELINE).createRenderSetup());

    private ArmorOverheadRenderer() {}

    public static void submit(
            PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        if (!HbmPlayerProps.getData(player).enableHUD) return;

        ArmorFullSetBonus bonus = activeChestBonus(player);
        CameraRenderState camera = state.cameraRenderState;

        if (bonus != null && bonus.thermal() || aimsThermalSights(player)) {
            submitThermalSight(poseStack, collector, level, player, camera.pos);
        }
        if (bonus != null && bonus.vats() && !mc.gui.hud.isHidden()) {
            submitHealthBars(poseStack, collector, mc, level, camera, bonus.thermal());
        }
    }

    private static boolean aimsThermalSights(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemGunBaseNT gun) || ItemGunBaseNT.aimingProgress != 1F)
            return false;
        for (int i = 0; i < gun.getConfigCount(); i++) {
            if (gun.getConfig(held, i).hasThermalSights(held)) return true;
        }
        return false;
    }

    private static ArmorFullSetBonus activeChestBonus(LocalPlayer player) {
        for (ModArmorItem.Suit suit : ModArmorItem.Suit.values()) {
            ArmorFullSetBonus bonus = ArmorFullSetBonus.get(suit);
            if (bonus != null
                    && (bonus.vats() || bonus.thermal())
                    && ArmorSuitEffects.hasFullSet(player, suit)) return bonus;
        }
        return null;
    }

    private static void submitThermalSight(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            ClientLevel level,
            LocalPlayer player,
            Vec3 camera) {
        for (Entity ent : level.entitiesForRendering()) {
            if (ent == player) continue;
            if (ent.distanceToSqr(player) > RANGE_SQ) continue;

            int color = colorFor(ent, player);
            if (color < 0) continue;

            Vec3 pos = ent.position();
            AABB local = ent.getBoundingBox().move(-pos.x, -pos.y, -pos.z);
            VoxelShape shape = Shapes.create(local);

            poseStack.pushPose();
            poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
            collector.submitShapeOutline(
                    poseStack, shape, THERMAL_LINES, ARGB.opaque(color), 1.0F, false);
            poseStack.popPose();
        }
    }

    private static int colorFor(Entity ent, LocalPlayer viewer) {
        int color;
        if (ent.is(HbmEntityTypeTags.BOSSES)) color = 0xFF7F00;
        else if (ent instanceof Enemy) color = 0xFF0000;
        else if (ent instanceof Player) color = 0xFF00FF;
        else if (ent instanceof Mob) color = 0x00FF00;
        else if (ent instanceof ItemEntity) color = 0xFFFF7F;
        else if (ent instanceof ExperienceOrb)
            color = viewer.tickCount % 10 < 5 ? 0xFFFF7F : 0x7FFF7F;
        else return -1;
        return ent instanceof LivingEntity living && living.getHealth() <= 0F ? 0x000000 : color;
    }

    private static void submitHealthBars(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Minecraft mc,
            ClientLevel level,
            CameraRenderState camera,
            boolean seeThroughWalls) {
        Font font = mc.font;
        Entity viewer = mc.getCameraEntity();
        for (Entity ent : level.entitiesForRendering()) {
            if (!(ent instanceof LivingEntity living) || living == viewer) continue;
            if (living.isVehicle() || living.isInvisibleTo(mc.player)) continue;
            if (camera.pos.distanceToSqr(living.position())
                    >= (living.isDiscrete() ? 1024D : RANGE_SQ)) continue;

            int count = (int) Math.min(living.getMaxHealth(), 100F);
            int bars = Mth.ceil(living.getHealth() * count / living.getMaxHealth());

            StringBuilder filled = new StringBuilder();
            StringBuilder empty = new StringBuilder();
            for (int i = 0; i < count; i++) (i < bars ? filled : empty).append('|');

            Component text =
                    Component.literal(filled.toString())
                            .withStyle(ChatFormatting.RED)
                            .append(
                                    Component.literal(empty.toString())
                                            .withStyle(ChatFormatting.WHITE));
            FormattedCharSequence seq = text.getVisualOrderText();

            float partialTick =
                    mc.getDeltaTracker()
                            .getGameTimeDeltaPartialTick(
                                    !level.tickRateManager().isEntityFrozen(living));
            Vec3 pos =
                    living.getPosition(partialTick)
                            .add(
                                    living.getAttachments()
                                            .get(
                                                    EntityAttachment.NAME_TAG,
                                                    0,
                                                    living.getYRot(partialTick)))
                            .add(0, 0.75D, 0);

            poseStack.pushPose();
            poseStack.translate(pos.x - camera.pos.x, pos.y - camera.pos.y, pos.z - camera.pos.z);
            poseStack.mulPose(camera.orientation);
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            float tx = -font.width(seq) / 2.0F;
            collector.submitText(
                    poseStack,
                    tx,
                    0,
                    seq,
                    false,
                    seeThroughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    ARGB.opaque(0xFFFFFF),
                    0x40000000,
                    0);
            poseStack.popPose();
        }
    }
}
