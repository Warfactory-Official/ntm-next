// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.client.render.flywheel.BobbleVisual;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.BlockEntityBobble;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RenderBobble
        implements BlockEntityRenderer<BlockEntityBobble, RenderBobble.State>,
                ConcurrentRenderStateExtraction {
    public static final int SOCKET_PART = ResourceManager.bobble.partId("Socket");
    public static final int DRILLGON = ResourceManager.bobble.partId("Drillgon");
    public static final int PEEP_TAIL = ResourceManager.bobble.partId("PeepTail");
    public static final int PEEP_HAT = ResourceManager.bobble.partId("PeepHat");
    public static final int HORN = ResourceManager.bobble.partId("Horn");
    public static final int PELLET = ResourceManager.bobble.partId("Pellet");
    public static final int PELLET_SHINE = ResourceManager.bobble.partId("PelletShine");
    public static final int FUMO = ResourceManager.bobble.partId("Fumo");
    public static final int FUMO_HEAD = ResourceManager.bobble.partId("FumoHead");
    public static final int FLUORO = ResourceManager.bobble.partId("Fluoro");
    public static final int GLOW = ResourceManager.bobble.partId("Glow");
    public static final int HEV_HEAD = ResourceManager.armor_hev.partId("Head");
    public static final int MINI_NUKE = ResourceManager.fatman.partId("MiniNuke");

    public static final int[] NI4NI =
            ResourceManager.n_i_4_n_i.partIds(
                    "FrameDark", "Grip", "FrameLight", "Cylinder", "Barrel");
    public static final int[] DOUBLE_BARREL =
            ResourceManager.double_barrel.partIds("Stock", "BarrelShort", "Buckle", "Lever");
    public static final Skin LAYERED = skinSet("");
    public static final Skin FLAT = skinSet("17");

    private static final Map<BobbleType, RenderType> SKINS = new EnumMap<>(BobbleType.class);
    private static final RenderType SOCKET =
            RenderTypes.entityCutoutCull(ResourceManager.bobble_socket_tex);

    private static final RenderType SHINE = BobbleRenderTypes.figurine(ResourceManager.white_tex);
    private static final double[] ZERO = {0, 0, 0};
    private static final Map<BobbleType, Pose> POSES = new EnumMap<>(BobbleType.class);
    private static final Pose NO_POSE = new Pose(ZERO, ZERO, ZERO, ZERO, 0, ZERO);
    private static final Quaternionfc TURN = Axis.YP.rotationDegrees(90);

    static {
        for (BobbleType type : BobbleType.values())
            SKINS.put(type, BobbleRenderTypes.figurine(skinTexture(type)));
    }

    static {
        POSES.put(
                BobbleType.STRENGTH,
                new Pose(
                        new double[] {0, 25, 135},
                        new double[] {0, -45, 135},
                        new double[] {0, 0, -5},
                        new double[] {0, 0, 5},
                        0,
                        new double[] {15, 0, 0}));
        POSES.put(
                BobbleType.PERCEPTION,
                new Pose(new double[] {0, -15, 135}, new double[] {-5, 0, 0}, ZERO, ZERO, 0, ZERO));
        POSES.put(
                BobbleType.ENDURANCE,
                new Pose(
                        new double[] {0, -25, 30},
                        new double[] {0, 45, 30},
                        ZERO,
                        ZERO,
                        45,
                        new double[] {0, -45, 0}));
        POSES.put(
                BobbleType.CHARISMA,
                new Pose(
                        ZERO,
                        new double[] {0, -45, 90},
                        new double[] {0, 0, -5},
                        new double[] {0, 0, 5},
                        45,
                        new double[] {-5, -45, 0}));
        POSES.put(
                BobbleType.INTELLIGENCE,
                new Pose(
                        new double[] {5, 0, 0},
                        new double[] {15, 0, 170},
                        ZERO,
                        ZERO,
                        0,
                        new double[] {0, 30, 0}));
        POSES.put(
                BobbleType.AGILITY,
                new Pose(
                        new double[] {0, 0, 60},
                        new double[] {0, 0, -45},
                        new double[] {0, 0, -15},
                        new double[] {0, 0, 45},
                        0,
                        ZERO));
        POSES.put(
                BobbleType.LUCK,
                new Pose(
                        new double[] {135, 45, 0},
                        new double[] {-135, -45, 0},
                        ZERO,
                        new double[] {-5, 0, 0},
                        0,
                        ZERO));
        POSES.put(
                BobbleType.VT,
                new Pose(
                        new double[] {0, -45, 60},
                        new double[] {0, 0, 45},
                        new double[] {2, 0, 0},
                        new double[] {-2, 0, 0},
                        0,
                        ZERO));
        POSES.put(
                BobbleType.BLUEHAT, new Pose(new double[] {0, 90, 60}, ZERO, ZERO, ZERO, 0, ZERO));
        POSES.put(
                BobbleType.FRIZZLE,
                new Pose(
                        new double[] {0, 15, 45},
                        new double[] {0, 0, 80},
                        new double[] {0, 0, 2},
                        new double[] {0, 0, -2},
                        0,
                        ZERO));
        POSES.put(BobbleType.ADAM29, new Pose(ZERO, new double[] {0, 0, 60}, ZERO, ZERO, 0, ZERO));
        POSES.put(
                BobbleType.PHEO,
                new Pose(new double[] {0, 0, 80}, new double[] {0, 0, 45}, ZERO, ZERO, 0, ZERO));
        POSES.put(
                BobbleType.VAER,
                new Pose(new double[] {0, -5, 45}, new double[] {0, 15, 45}, ZERO, ZERO, 0, ZERO));
        POSES.put(
                BobbleType.PEEP,
                new Pose(new double[] {0, 0, 1}, new double[] {0, 0, 1}, ZERO, ZERO, 0, ZERO));
        POSES.put(
                BobbleType.MELLOW,
                new Pose(
                        new double[] {0, 10, 0},
                        new double[] {0, -10, 0},
                        new double[] {3, 5, 2},
                        new double[] {-3, -5, 0},
                        0,
                        ZERO));
        POSES.put(
                BobbleType.ABEL,
                new Pose(new double[] {0, 80, 90}, new double[] {0, -80, 90}, ZERO, ZERO, 0, ZERO));
    }

    private final Font font;
    private final ItemModelResolver itemModelResolver;

    public RenderBobble(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
        this.itemModelResolver = context.itemModelResolver();
    }

    private RenderBobble(Font font, ItemModelResolver itemModelResolver) {
        this.font = font;
        this.itemModelResolver = itemModelResolver;
    }

    private static Skin skinSet(String suffix) {
        return new Skin(
                ResourceManager.bobble.partId("LL" + suffix),
                        ResourceManager.bobble.partId("RL" + suffix),
                ResourceManager.bobble.partId("LA" + suffix),
                        ResourceManager.bobble.partId("RA" + suffix),
                ResourceManager.bobble.partId("Body" + suffix),
                        ResourceManager.bobble.partId("Head" + suffix));
    }

    public static Pose pose(BobbleType type) {
        return POSES.getOrDefault(type, NO_POSE);
    }

    public static float wobbleX(long time) {
        return (float) Math.sin(time * 0.005);
    }

    public static float wobbleZ(long time) {
        return (float) Math.sin(time * 0.005 + (Math.PI * 0.5));
    }

    public static int shine(long time) {

        float alpha = 0.1F + (float) Math.sin(time * 0.001D) * 0.05F;
        return ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 0.0F);
    }

    public static int labelColor(BobbleType type) {
        return type == BobbleType.VT ? 0xFFFF0000 : 0xFFFFFFFF;
    }

    public static Matrix4f labelBase(Matrix4f pose) {

        float f3 = 0.01F;
        return pose.translate(0.63F, 0.175F, 0F).scale(f3, -f3, f3);
    }

    public static Matrix4f labelWidth(Matrix4f pose, int width) {
        return pose.translate(0F, 0F, width * 0.5F).rotate(TURN).translate(0F, 1F, 0F);
    }

    public static Identifier skinTexture(BobbleType type) {
        return switch (type) {
            case STRENGTH, PERCEPTION, ENDURANCE, CHARISMA, INTELLIGENCE, AGILITY, LUCK ->
                    ResourceManager.bobble_vaultboy_tex;
            case BOB -> ResourceManager.bobble_hbm_tex;
            case PU238 -> ResourceManager.bobble_pu238_tex;
            case FRIZZLE -> ResourceManager.bobble_frizzle_tex;
            case VT -> ResourceManager.bobble_vt_tex;
            case DOC -> ResourceManager.bobble_doc_tex;
            case BLUEHAT -> ResourceManager.bobble_blue_tex;
            case PHEO -> ResourceManager.bobble_pheo_tex;
            case CIRNO -> ResourceManager.bobble_cirno_tex;
            case ADAM29 -> ResourceManager.bobble_adam_tex;
            case UFFR -> ResourceManager.bobble_uffr_tex;
            case VAER -> ResourceManager.bobble_vaer_tex;
            case NOS -> ResourceManager.bobble_nos_tex;
            case DRILLGON -> ResourceManager.bobble_drillgon_tex;
            case MICROWAVE -> ResourceManager.bobble_microwave_tex;
            case PEEP -> ResourceManager.bobble_peep_tex;
            case MELLOW -> ResourceManager.bobble_mellow_tex;
            case ABEL -> ResourceManager.bobble_abel_tex;
            default -> ResourceManager.universal_tex;
        };
    }

    public static RenderBobble forItem() {
        Minecraft minecraft = Minecraft.getInstance();
        return new RenderBobble(minecraft.font, minecraft.getItemModelResolver());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityBobble be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.type = be.type;
        state.rotation = be.getBlockState().getValue(BlockBobble.ROTATION);
        state.time = GameTime.now();

        state.propOnly = HbmBlockEntityVisual.hasVisual(be);
        if (state.propOnly && !BobbleVisual.vanillaNeeded(be)) return;
        if (state.type == BobbleType.VAER) resolveCigarette(state.cigarette, be.getLevel());
        if (state.type == BobbleType.ADAM29) resolveRedBomb(state.redBomb, be.getLevel());
        if (state.type == BobbleType.FRIZZLE) resolveDoubloons(state.doubloons, be.getLevel());
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.scale(0.25F, 0.25F, 0.25F);

        ps.mulPose(Axis.YN.rotationDegrees(22.5F * s.rotation + 90.0F));
        if (s.propOnly) renderProp(s, ps, col);
        else
            renderBobble(
                    s.type, s.time, ps, col, s.lightCoords, s.cigarette, s.redBomb, s.doubloons);
        ps.popPose();
    }

    private static void renderProp(
            State state, PoseStack poseStack, SubmitNodeCollector collector) {
        ItemStackRenderState prop;
        switch (state.type) {
            case VAER -> {
                Pose pose = pose(state.type);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) pose.body));
                poseStack.translate(0D, 1.75D, 0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(wobbleX(state.time)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(wobbleZ(state.time)));
                poseStack.mulPose(Axis.XP.rotationDegrees((float) pose.head[0]));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) pose.head[1]));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) pose.head[2]));
                poseStack.translate(0D, -1.75D, 0D);
                prop = state.cigarette;
            }
            case FRIZZLE -> prop = state.doubloons;
            case ADAM29 -> prop = state.redBomb;
            default -> {
                return;
            }
        }
        propPose(state.type, poseStack);
        prop.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

    public static ItemStack prop(BobbleType type) {
        return switch (type) {
            case VAER -> new ItemStack(ModItems.CIGARETTE);
            case FRIZZLE -> ModItems.WEAPON_MOD_SPECIAL.stack(EnumModSpecial.DOUBLOONS);
            case ADAM29 -> new ItemStack(ModItems.CAN_REDBOMB);
            default -> ItemStack.EMPTY;
        };
    }

    public static void propPose(BobbleType type, PoseStack ps) {
        switch (type) {
            case VAER -> {
                ps.translate(0.25, 1.9, 0.075);
                ps.mulPose(Axis.ZP.rotationDegrees(-60));
                ps.scale(0.5F, 0.5F, 0.5F);
            }
            case FRIZZLE -> {
                ps.translate(0.3, 1.4, -0.2);
                ps.mulPose(Axis.XP.rotationDegrees(-100));
                ps.scale(0.5F, 0.5F, 0.5F);
            }
            case ADAM29 -> {
                ps.translate(0.4, 1.15, 0.4);
                ps.scale(0.5F, 0.5F, 0.5F);
            }
            default -> throw new IllegalArgumentException(type.name());
        }
    }

    public void renderBobble(
            BobbleType type,
            long time,
            PoseStack ps,
            SubmitNodeCollector col,
            int light,
            @Nullable ItemStackRenderState cigarette,
            @Nullable ItemStackRenderState redBomb,
            @Nullable ItemStackRenderState doubloons) {
        part(col, ps, SOCKET, ResourceManager.bobble, light, SOCKET_PART);

        switch (type) {
            case PU238 -> renderPellet(time, ps, col, light);
            case UFFR -> renderFumo(time, ps, col, light);

            case DRILLGON ->
                    part(
                            col,
                            ps,
                            RenderTypes.entityCutoutCull(ResourceManager.bobble_drillgon_tex),
                            ResourceManager.bobble,
                            light,
                            DRILLGON);
            default -> renderGuy(time, ps, col, 0, light, type, SKINS.get(type), cigarette);
        }

        ps.pushPose();
        renderPost(type, time, ps, col, light, redBomb, doubloons);
        ps.popPose();

        if (col != null) renderSocket(type, ps, col);
    }

    private void renderGuy(
            long time,
            PoseStack ps,
            SubmitNodeCollector col,
            int order,
            int light,
            BobbleType type,
            RenderType skin,
            @Nullable ItemStackRenderState cigarette) {
        Pose pose = pose(type);
        HFRWavefrontObject m = ResourceManager.bobble;

        OrderedSubmitNodeCollector out = col == null ? null : col.order(order);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) pose.body));

        if (type == BobbleType.PEEP) part(out, ps, skin, m, light, PEEP_TAIL);

        Skin set = type.skinLayers ? LAYERED : FLAT;

        limb(out, ps, skin, m, light, set.leftLeg, 0, 1, -0.125, pose.leftLeg);
        limb(out, ps, skin, m, light, set.rightLeg, 0, 1, 0.125, pose.rightLeg);
        limb(out, ps, skin, m, light, set.leftArm, 0, 1.625, -0.25, pose.leftArm);
        limb(out, ps, skin, m, light, set.rightArm, 0, 1.625, 0.25, pose.rightArm);

        part(out, ps, skin, m, light, set.body);

        ps.pushPose();
        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees(wobbleX(time)));
        ps.mulPose(Axis.ZP.rotationDegrees(wobbleZ(time)));
        ps.mulPose(Axis.XP.rotationDegrees((float) pose.head[0]));
        ps.mulPose(Axis.YP.rotationDegrees((float) pose.head[1]));
        ps.mulPose(Axis.ZP.rotationDegrees((float) pose.head[2]));
        ps.translate(0, -1.75, 0);
        part(out, ps, skin, m, light, set.head);

        if (type == BobbleType.VT) part(out, ps, skin, m, light, HORN);
        if (type == BobbleType.PEEP) part(out, ps, skin, m, light, PEEP_HAT);

        if (type == BobbleType.VAER && cigarette != null) {
            propPose(type, ps);
            cigarette.submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
        }

        if (type == BobbleType.NOS) {
            ps.translate(0, 1.75, 0);
            ps.mulPose(Axis.XP.rotationDegrees(180));
            ps.scale(0.095F, 0.095F, 0.095F);
            all(
                    out,
                    ps,
                    RenderTypes.entityCutoutCull(ResourceManager.hat_tex),
                    ResourceManager.armor_hat,
                    light);
        }

        ps.popPose();
        ps.popPose();
    }

    private void limb(
            OrderedSubmitNodeCollector col,
            PoseStack ps,
            RenderType skin,
            HFRWavefrontObject m,
            int light,
            int partName,
            double jx,
            double jy,
            double jz,
            double[] rot) {
        ps.pushPose();
        ps.translate(jx, jy, jz);
        ps.mulPose(Axis.XP.rotationDegrees((float) rot[0]));
        ps.mulPose(Axis.YP.rotationDegrees((float) rot[1]));
        ps.mulPose(Axis.ZP.rotationDegrees((float) rot[2]));
        ps.translate(-jx, -jy, -jz);
        part(col, ps, skin, m, light, partName);
        ps.popPose();
    }

    private void renderPellet(long time, PoseStack ps, SubmitNodeCollector col, int light) {

        part(
                col,
                ps,
                RenderTypes.entityCutoutCull(ResourceManager.bobble_pu238_tex),
                ResourceManager.bobble,
                LightCoordsUtil.FULL_BRIGHT,
                PELLET);

        partTinted(
                col,
                ps,
                SHINE,
                ResourceManager.bobble,
                LightCoordsUtil.FULL_BRIGHT,
                shine(time),
                PELLET_SHINE);
    }

    private void renderFumo(long time, PoseStack ps, SubmitNodeCollector col, int light) {
        RenderType skin = WorldRenderPipeline.oneSidedCutout(ResourceManager.bobble_uffr_tex);
        part(
                col,
                ps,
                RenderTypes.entityCutoutCull(ResourceManager.bobble_uffr_tex),
                ResourceManager.bobble,
                light,
                FUMO);

        ps.pushPose();
        ps.translate(0, 0.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees(wobbleX(time)));
        ps.mulPose(Axis.ZP.rotationDegrees(wobbleZ(time)));
        ps.translate(0, -0.75, 0);
        part(col, ps, skin, ResourceManager.bobble, light, FUMO_HEAD);
        ps.popPose();
    }

    private void renderPost(
            BobbleType type,
            long time,
            PoseStack ps,
            SubmitNodeCollector col,
            int light,
            @Nullable ItemStackRenderState redBomb,
            @Nullable ItemStackRenderState doubloons) {
        switch (type) {
            case BLUEHAT -> {
                ps.translate(0.0, 0.875, -0.5);
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                ps.mulPose(Axis.ZP.rotationDegrees(-160));
                ps.scale(0.0625F, 0.0625F, 0.0625F);
                part(
                        col,
                        ps,
                        RenderTypes.entityCutoutCull(ResourceManager.hev_helmet_tex),
                        ResourceManager.armor_hev,
                        light,
                        HEV_HEAD);
            }
            case FRIZZLE -> {
                ps.pushPose();
                ps.translate(0.8, 1.6, 0.4);
                ps.scale(0.125F, 0.125F, 0.125F);
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.mulPose(Axis.XP.rotationDegrees(10));

                RenderType gun = RenderTypes.entityCutoutCull(ResourceManager.n_i_4_n_i_tex);
                for (int p : NI4NI) part(col, ps, gun, ResourceManager.n_i_4_n_i, light, p);
                ps.popPose();

                if (doubloons != null) {
                    propPose(type, ps);
                    doubloons.submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
                }
            }
            case ADAM29 -> {
                if (redBomb != null) {
                    propPose(type, ps);
                    redBomb.submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
                }
            }
            case PHEO -> {
                ps.translate(0.5, 1.15, 0.45);
                ps.mulPose(Axis.XP.rotationDegrees(-60));
                ps.scale(2F, 2F, 2F);
                all(
                        col,
                        ps,
                        RenderTypes.entityCutoutCull(ResourceManager.shimmer_axe_tex),
                        ResourceManager.shimmer_axe,
                        light);
            }
            case BOB -> {
                ps.pushPose();
                ps.translate(0, 0.6875, 0.625);
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.scale(0.125F, 0.125F, 0.125F);
                RenderType nuke = RenderTypes.entityCutoutCull(ResourceManager.fatman_mininuke_tex);

                ps.translate(-6, 0, 0);
                for (int i = -1; i <= 1; i++) {
                    ps.translate(3, 0, 0);
                    part(col, ps, nuke, ResourceManager.fatman, light, MINI_NUKE);
                }
                ps.popPose();

                ps.pushPose();
                ps.translate(0.25, 0.3125, -0.5);
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.scale(0.1F, 0.1F, 0.1F);
                RenderType shotgun =
                        RenderTypes.entityCutoutCull(
                                ResourceManager.double_barrel_sacred_dragon_tex);
                for (int p : DOUBLE_BARREL)
                    part(col, ps, shotgun, ResourceManager.double_barrel, light, p);
                ps.popPose();
            }
            case VAER -> {}
            case MELLOW -> {
                renderGuy(
                        time,
                        ps,
                        col,
                        1,
                        LightCoordsUtil.FULL_BRIGHT,
                        type,
                        BobbleRenderTypes.figurine(ResourceManager.bobble_mellow_glow_tex),
                        null);
                part(
                        col == null ? null : col.order(2),
                        ps,
                        BobbleRenderTypes.glow(ResourceManager.fluorescent_lamp_tex),
                        ResourceManager.bobble,
                        LightCoordsUtil.FULL_BRIGHT,
                        FLUORO);
                part(
                        col == null ? null : col.order(3),
                        ps,
                        BobbleRenderTypes.glow(ResourceManager.bobble_glow_tex),
                        ResourceManager.bobble,
                        LightCoordsUtil.FULL_BRIGHT,
                        GLOW);
            }
            case ABEL -> {
                renderGuy(
                        time,
                        ps,
                        col,
                        1,
                        LightCoordsUtil.FULL_BRIGHT,
                        type,
                        BobbleRenderTypes.figurine(ResourceManager.bobble_abel_glow_tex),
                        null);
            }
            default -> {}
        }
    }

    private void renderSocket(BobbleType type, PoseStack ps, SubmitNodeCollector col) {
        labelWidth(labelBase(ps.last().pose()), font.width(type.label));

        col.submitText(
                ps,
                0,
                0,
                FormattedCharSequence.forward(type.label, Style.EMPTY),
                false,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                labelColor(type),
                0,
                0);
    }

    void resolveCigarette(ItemStackRenderState state, @Nullable Level level) {
        itemModelResolver.updateForTopItem(
                state, prop(BobbleType.VAER), ItemDisplayContext.NONE, level, null, 0);
    }

    void resolveDoubloons(ItemStackRenderState state, @Nullable Level level) {
        itemModelResolver.updateForTopItem(
                state, prop(BobbleType.FRIZZLE), ItemDisplayContext.NONE, level, null, 0);
    }

    void resolveRedBomb(ItemStackRenderState state, @Nullable Level level) {
        itemModelResolver.updateForTopItem(
                state, prop(BobbleType.ADAM29), ItemDisplayContext.NONE, level, null, 0);
    }

    private void part(
            OrderedSubmitNodeCollector col,
            PoseStack ps,
            RenderType t,
            HFRWavefrontObject m,
            int light,
            int name) {
        partTinted(col, ps, t, m, light, -1, name);
    }

    private void partTinted(
            OrderedSubmitNodeCollector col,
            PoseStack ps,
            RenderType t,
            HFRWavefrontObject m,
            int light,
            int color,
            int name) {
        if (col != null)
            col.submitCustomGeometry(
                    ps, t, (pose, buffer) -> m.renderPart(pose, buffer, light, color, name));
    }

    private void all(
            OrderedSubmitNodeCollector col,
            PoseStack ps,
            RenderType t,
            HFRWavefrontObject m,
            int light) {
        if (col != null)
            col.submitCustomGeometry(ps, t, (pose, buffer) -> m.render(pose, buffer, light, -1));
    }

    public record Skin(int leftLeg, int rightLeg, int leftArm, int rightArm, int body, int head) {}

    public record Pose(
            double[] leftArm,
            double[] rightArm,
            double[] leftLeg,
            double[] rightLeg,
            double body,
            double[] head) {}

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState cigarette = new ItemStackRenderState();
        public final ItemStackRenderState redBomb = new ItemStackRenderState();
        public final ItemStackRenderState doubloons = new ItemStackRenderState();
        public BobbleType type = BobbleType.NONE;
        public int rotation;
        public long time;
        public boolean propOnly;
    }
}
