// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.GrenadeItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.ItemGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class GrenadeItemRenderer
        implements SpecialModelRenderer<GrenadeItemRenderer.Argument>,
                ItemVisuals.Factory<GrenadeItemRenderer.Argument> {
    private static final int FRAG = ResourceManager.grenades.partId("Frag");
    private static final int FRAG_RING = ResourceManager.grenades.partId("FragRing");
    private static final int FRAG_SPOON = ResourceManager.grenades.partId("FragSpoon");
    private static final int STICK = ResourceManager.grenades.partId("Stick");
    private static final int STICK_CAP = ResourceManager.grenades.partId("StickCap");
    private static final int TECH = ResourceManager.grenades.partId("Tech");
    private static final int TECH_RING = ResourceManager.grenades.partId("TechRing");
    private static final int NUKA = ResourceManager.grenades.partId("Nuka");
    private static final int NUKA_RING = ResourceManager.grenades.partId("NukaRing");
    private static final int NUKA_SPOON = ResourceManager.grenades.partId("NukaSpoon");
    private final float[] bounds = ResourceManager.grenades.boundsExcluding(Set.of());

    public static void draw(
            SubmitNodeCollector collector,
            PoseStack pose,
            GrenadeData data,
            ItemDisplayContext context,
            int light) {
        visit(
                pose,
                data,
                context == ItemDisplayContext.NONE ? Arm.PROJECTILE : arm(context),
                context,
                false,
                submitter(collector, light));
    }

    public static void visit(
            PoseStack pose,
            GrenadeData data,
            ItemDisplayContext context,
            boolean hand,
            PartVisitor visitor) {
        visit(pose, data, arm(context), context, hand, visitor);
    }

    private static PartVisitor submitter(SubmitNodeCollector collector, int light) {
        return (pose, texture, part, color, fullBright) -> {
            int partLight = fullBright ? LightCoordsUtil.FULL_BRIGHT : light;
            RenderType type =
                    color == -1
                            ? RenderTypes.entityCutout(texture)
                            : RenderTypes.entityTranslucent(texture, false);
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (p, buffer) ->
                            ResourceManager.grenades.renderPart(p, buffer, partLight, color, part));
        };
    }

    private static Arm arm(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> Arm.INVENTORY;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> Arm.EQUIPPED;
            default -> Arm.OTHER;
        };
    }

    private static void visit(
            PoseStack pose,
            GrenadeData data,
            Arm arm,
            ItemDisplayContext context,
            boolean hand,
            PartVisitor visitor) {
        pose.pushPose();
        if (context.firstPerson()) {
            drawFirstPerson(visitor, pose, data, hand);
        } else {
            transformShell(pose, data.shell(), arm);

            drawShell(visitor, pose, data, arm != Arm.PROJECTILE);
        }
        pose.popPose();
    }

    private static void transformShell(
            PoseStack pose, ItemGrenadeShell.EnumGrenadeShell shell, Arm arm) {
        switch (shell) {
            case FRAG -> {
                if (arm == Arm.INVENTORY) {
                    pose.scale(3F, 3F, 3F);
                    pose.translate(0, -2, 0);
                } else if (arm == Arm.PROJECTILE) pose.translate(0, -2, 0);
            }
            case STICK -> {
                if (arm == Arm.INVENTORY) {
                    pose.scale(2F, 2F, 2F);
                    pose.translate(0, -4.5, 0);
                } else if (arm == Arm.EQUIPPED || arm == Arm.PROJECTILE) pose.translate(0, -2, 0);
            }
            case TECH -> {
                if (arm == Arm.INVENTORY) {
                    pose.scale(3.5F, 3.5F, 3.5F);
                    pose.translate(0, -1.75, 0);
                } else if (arm == Arm.EQUIPPED) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0.5, -1, 0.5);
                } else if (arm == Arm.PROJECTILE) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0, -1, 0);
                }
            }
            case NUKE -> {
                if (arm == Arm.INVENTORY) {
                    pose.scale(2.5F, 2.5F, 2.5F);
                    pose.translate(0, -2.75, 0);
                } else if (arm == Arm.EQUIPPED) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0.5, -3, 0.5);
                } else if (arm == Arm.PROJECTILE) {
                    pose.scale(1.5F, 1.5F, 1.5F);
                    pose.translate(0, -3, 0);
                }
            }
        }
    }

    private static void drawFirstPerson(
            PartVisitor visitor, PoseStack pose, GrenadeData data, boolean hand) {
        double[] bodyMove = bus(hand, "BODYMOVE");
        double[] bodyTurn = bus(hand, "BODYTURN");
        double[] ringMove = bus(hand, "RINGMOVE");
        double[] ringTurn = bus(hand, "RINGTURN");
        double[] renderRing = bus(hand, "RENDERRING");
        pose.translate(bodyMove[0], bodyMove[1], bodyMove[2]);
        switch (data.shell()) {
            case FRAG -> {
                pose.mulPose(Axis.XP.rotationDegrees((float) bodyTurn[2]));
                drawStandardBody(
                        visitor,
                        pose,
                        data,
                        FRAG,
                        ResourceManager.grenade_frag_tex,
                        ResourceManager.grenade_frag_body_tex,
                        ResourceManager.grenade_frag_label_tex,
                        ResourceManager.grenade_frag_fuze_tex);
                visitor.accept(pose, ResourceManager.grenade_frag_tex, FRAG_SPOON, -1, false);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.mulPose(Axis.XP.rotationDegrees((float) ringTurn[2]));
                    visitor.accept(pose, ResourceManager.grenade_frag_tex, FRAG_RING, -1, false);
                }
            }
            case STICK -> {
                pose.mulPose(Axis.ZP.rotationDegrees((float) bodyTurn[2]));
                drawStandardBody(
                        visitor,
                        pose,
                        data,
                        STICK,
                        ResourceManager.grenade_stick_tex,
                        ResourceManager.grenade_stick_body_tex,
                        ResourceManager.grenade_stick_label_tex,
                        ResourceManager.grenade_stick_fuze_tex);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.mulPose(Axis.YP.rotationDegrees((float) ringTurn[1]));
                    visitor.accept(pose, ResourceManager.grenade_stick_tex, STICK_CAP, -1, false);
                    visitor.accept(
                            pose,
                            ResourceManager.grenade_stick_body_tex,
                            STICK_CAP,
                            color(data.filling().bodyColor()),
                            false);
                }
            }
            case TECH -> {
                pose.mulPose(Axis.XP.rotationDegrees((float) bodyTurn[2]));
                drawTechBody(visitor, pose, data);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.mulPose(Axis.XP.rotationDegrees((float) ringTurn[2]));
                    visitor.accept(pose, ResourceManager.grenade_tech_tex, TECH_RING, -1, false);
                }
            }
            case NUKE -> {
                pose.mulPose(Axis.ZP.rotationDegrees((float) bodyTurn[2]));
                drawStandardBody(
                        visitor,
                        pose,
                        data,
                        NUKA,
                        ResourceManager.grenade_nuka_tex,
                        ResourceManager.grenade_nuka_body_tex,
                        ResourceManager.grenade_nuka_label_tex,
                        ResourceManager.grenade_nuka_fuze_tex);
                visitor.accept(pose, ResourceManager.grenade_nuka_tex, NUKA_SPOON, -1, false);
                if (renderRing[0] != 0) {
                    pose.translate(ringMove[0], ringMove[1], ringMove[2]);
                    pose.translate(-1, 5, 0);
                    pose.mulPose(Axis.ZN.rotationDegrees((float) ringTurn[2]));
                    pose.translate(1, -5, 0);
                    visitor.accept(pose, ResourceManager.grenade_nuka_tex, NUKA_RING, -1, false);
                }
            }
        }
    }

    private static double[] bus(boolean hand, String name) {
        return hand ? HbmAnimations.getRelevantTransformation(name) : HbmAnimations.identity();
    }

    private static void drawShell(
            PartVisitor visitor, PoseStack pose, GrenadeData data, boolean accessories) {
        switch (data.shell()) {
            case FRAG -> {
                drawStandardBody(
                        visitor,
                        pose,
                        data,
                        FRAG,
                        ResourceManager.grenade_frag_tex,
                        ResourceManager.grenade_frag_body_tex,
                        ResourceManager.grenade_frag_label_tex,
                        ResourceManager.grenade_frag_fuze_tex);
                if (accessories) {
                    visitor.accept(pose, ResourceManager.grenade_frag_tex, FRAG_SPOON, -1, false);
                    visitor.accept(pose, ResourceManager.grenade_frag_tex, FRAG_RING, -1, false);
                }
            }
            case STICK -> {
                drawStandardBody(
                        visitor,
                        pose,
                        data,
                        STICK,
                        ResourceManager.grenade_stick_tex,
                        ResourceManager.grenade_stick_body_tex,
                        ResourceManager.grenade_stick_label_tex,
                        ResourceManager.grenade_stick_fuze_tex);
                if (accessories)
                    visitor.accept(
                            pose,
                            ResourceManager.grenade_stick_body_tex,
                            STICK_CAP,
                            color(data.filling().bodyColor()),
                            false);
            }
            case TECH -> {
                drawTechBody(visitor, pose, data);
                if (accessories)
                    visitor.accept(pose, ResourceManager.grenade_tech_tex, TECH_RING, -1, false);
            }
            case NUKE -> {
                drawStandardBody(
                        visitor,
                        pose,
                        data,
                        NUKA,
                        ResourceManager.grenade_nuka_tex,
                        ResourceManager.grenade_nuka_body_tex,
                        ResourceManager.grenade_nuka_label_tex,
                        ResourceManager.grenade_nuka_fuze_tex);
                if (accessories) {
                    visitor.accept(pose, ResourceManager.grenade_nuka_tex, NUKA_SPOON, -1, false);
                    visitor.accept(pose, ResourceManager.grenade_nuka_tex, NUKA_RING, -1, false);
                }
            }
        }
    }

    private static void drawStandardBody(
            PartVisitor visitor,
            PoseStack pose,
            GrenadeData data,
            int part,
            Identifier base,
            Identifier body,
            Identifier label,
            Identifier fuze) {
        visitor.accept(pose, base, part, -1, false);
        visitor.accept(pose, body, part, color(data.filling().bodyColor()), false);
        visitor.accept(pose, label, part, color(data.filling().labelColor()), false);
        visitor.accept(pose, fuze, part, color(data.fuze().bandColor()), false);
    }

    private static void drawTechBody(PartVisitor visitor, PoseStack pose, GrenadeData data) {
        visitor.accept(pose, ResourceManager.grenade_tech_tex, TECH, -1, false);
        visitor.accept(
                pose,
                ResourceManager.grenade_tech_body_tex,
                TECH,
                color(data.filling().bodyColor()),
                false);
        visitor.accept(
                pose,
                ResourceManager.grenade_tech_fuze_tex,
                TECH,
                color(data.fuze().bandColor()),
                false);
        visitor.accept(
                pose,
                ResourceManager.grenade_tech_lights_tex,
                TECH,
                color(data.filling().labelColor()),
                true);
    }

    private static int color(int rgb) {
        return 0xFF000000 | rgb;
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {

        return new Argument(ItemGrenadeUniversal.getData(stack), ItemDisplayContext.NONE, false);
    }

    @Override
    public void submit(
            @Nullable Argument argument,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (argument == null) return;

        visit(
                pose,
                argument.data(),
                argument.context(),
                argument.hand(),
                submitter(collector, light));
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new GrenadeItemVisual(
                ctx,
                argument == null ? ItemGrenadeUniversal.getData(stack) : argument.data(),
                context);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    output.accept(
                            new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
                }
    }

    private enum Arm {
        PROJECTILE,
        INVENTORY,
        EQUIPPED,
        OTHER
    }

    public record Argument(GrenadeData data, ItemDisplayContext context, boolean hand) {}

    @FunctionalInterface
    public interface PartVisitor {
        void accept(PoseStack pose, Identifier texture, int part, int color, boolean fullBright);
    }

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                                Identifier.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base))
                                        .apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            GrenadeItemRenderer renderer = new GrenadeItemRenderer();
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, display, owner) ->
                            new Argument(
                                    ItemGrenadeUniversal.getData(stack),
                                    display,
                                    HandPass.local(display, owner)),
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
