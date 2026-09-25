// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.part.InstanceTree;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MobModelVisual<T extends LivingEntity, S extends LivingEntityRenderState>
        extends HbmDynamicEntityVisual<T> {
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Spec<T, S> spec;
    private final InstanceTree tree;
    private final ModelPart root;
    private final EntityModel<? super S> model;
    private final S state;
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f lastPose = new Matrix4f();
    private int lastLight, lastOverlay;
    private boolean written;

    public MobModelVisual(VisualizationContext ctx, T entity, float partialTick, Spec<T, S> spec) {
        super(ctx, entity, partialTick);
        this.spec = spec;
        this.tree = InstanceTree.create(instancerProvider(), spec.tree().get());
        this.root = spec.bake().get();
        this.model = spec.model().apply(root);
        this.state = spec.state().get();
        this.nameTag =
                new NameTagComponent(ctx, entity)
                        .shouldShow(
                                () ->
                                        spec.instanced().test(entity)
                                                && (entity.shouldShowName()
                                                        || entity.hasCustomName()
                                                                && entity
                                                                        == Minecraft.getInstance()
                                                                                .getEntityRenderDispatcher()
                                                                                .crosshairPickEntity));
        this.shadow =
                new ShadowComponent(ctx, entity)
                        .radius(spec.shadowRadius())
                        .strength(spec.shadowStrength());

        writeFrame(partialTick);
    }

    private static boolean copyPose(InstanceTree tree, ModelPart part, boolean parentVisible) {
        boolean visible = parentVisible && part.visible;
        boolean shown = tree.visible() != visible || tree.skipDraw() != part.skipDraw;
        if (tree.visible() != visible) tree.visible(visible);
        if (tree.skipDraw() != part.skipDraw) tree.skipDraw(part.skipDraw);
        if (tree.xPos() != part.x
                || tree.yPos() != part.y
                || tree.zPos() != part.z
                || tree.xRot() != part.xRot
                || tree.yRot() != part.yRot
                || tree.zRot() != part.zRot
                || tree.xScale() != part.xScale
                || tree.yScale() != part.yScale
                || tree.zScale() != part.zScale) tree.copyTransform(part);
        for (int i = 0; i < tree.childCount(); i++) {
            shown |= copyPose(tree.child(i), part.getChild(tree.childName(i)), visible);
        }
        return shown;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        nameTag.beginFrame(context);
        shadow.beginFrame(context);
    }

    private void writeFrame(float partialTick) {
        boolean drawn = spec.instanced().test(entity) && !entity.isInvisible();
        boolean revealed = drawn && !tree.visible();
        if (tree.visible() != drawn) tree.visible(drawn);
        shadow.radius(drawn ? spec.shadowRadius() * entity.getScale() : 0F);
        if (!drawn) return;

        extract(partialTick);
        model.setupAnim(state);
        boolean shown = copyPose(tree, root, true);

        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        pose.translation(visualPos.x, visualPos.y, visualPos.z);
        pose.scale(state.scale);
        setupRotations();
        pose.scale(-1F, -1F, 1F);
        spec.scale().apply(pose, state);
        pose.translate(0F, -1.501F, 0F);
        if (!written || revealed || shown || !pose.equals(lastPose)) tree.updateInstances(pose);
        else tree.updateInstancesStatic(pose);
        lastPose.set(pose);

        int light =
                spec.fullBright() ? LightCoordsUtil.FULL_BRIGHT : computePackedLight(partialTick);
        int overlay =
                OverlayTexture.pack(
                        OverlayTexture.u(spec.whiteOverlay().applyAsFloat(state)),
                        OverlayTexture.v(state.hasRedOverlay));

        if (written && !revealed && !shown && light == lastLight && overlay == lastOverlay) return;
        lastLight = light;
        lastOverlay = overlay;
        written = true;
        tree.traverse(
                instance -> {
                    instance.overlay(overlay).light(light);
                    instance.setChanged();
                });
    }

    private void extract(float partialTick) {
        state.ageInTicks = entity.tickCount + partialTick;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();

        float headRot = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        state.bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        state.yRot = Mth.wrapDegrees(headRot - state.bodyRot);
        state.xRot = entity.getXRot(partialTick);
        Component customName = entity.getCustomName();
        String name = customName == null ? "" : customName.getString();
        state.isUpsideDown = "Dinnerbone".equals(name) || "Grumm".equals(name);
        if (state.isUpsideDown) {
            state.xRot *= -1F;
            state.yRot *= -1F;
        }

        if (!entity.isPassenger() && entity.isAlive()) {
            state.walkAnimationPos = entity.walkAnimation.position(partialTick);
            state.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);
        } else {
            state.walkAnimationPos = 0F;
            state.walkAnimationSpeed = 0F;
        }

        state.scale = entity.getScale();
        state.ageScale = entity.getAgeScale();
        state.pose = entity.getPose();
        state.isFullyFrozen = entity.isFullyFrozen();
        state.isBaby = entity.isBaby();
        state.isInWater = entity.isInWater();
        state.hasRedOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
        state.deathTime = entity.deathTime > 0 ? entity.deathTime + partialTick : 0F;

        spec.extractor().extract(entity, state, partialTick);
    }

    private void setupRotations() {
        float bodyRot = state.bodyRot;
        if (state.isFullyFrozen) {
            bodyRot += (float) (Math.cos(Mth.floor(state.ageInTicks) * 3.25F) * Math.PI * 0.4F);
        }
        if (!state.hasPose(Pose.SLEEPING)) pose.rotateY((180F - bodyRot) * Mth.DEG_TO_RAD);

        if (state.deathTime > 0F) {
            float fall = Mth.sqrt((state.deathTime - 1F) / 20F * 1.6F);
            if (fall > 1F) fall = 1F;
            pose.rotateZ(fall * spec.flipDegrees() * Mth.DEG_TO_RAD);
        } else if (state.isUpsideDown) {
            pose.translate(0F, (state.boundingBoxHeight + 0.1F) / state.scale, 0F);
            pose.rotateZ((float) Math.PI);
        }
    }

    @Override
    protected void _delete() {
        tree.delete();
        nameTag.delete();
        shadow.delete();
    }

    @FunctionalInterface
    public interface Extractor<T, S> {
        void extract(T entity, S state, float partialTick);
    }

    @FunctionalInterface
    public interface ScaleHook<S> {
        void apply(Matrix4f pose, S state);
    }

    public record Spec<T extends LivingEntity, S extends LivingEntityRenderState>(
            Supplier<ModelTree> tree,
            Supplier<ModelPart> bake,
            Function<ModelPart, EntityModel<? super S>> model,
            Supplier<S> state,
            Extractor<T, S> extractor,
            ScaleHook<S> scale,
            ToFloatFunction<S> whiteOverlay,
            float flipDegrees,
            float shadowRadius,
            float shadowStrength,
            boolean fullBright,
            Predicate<T> instanced) {
        public static <T extends LivingEntity, S extends LivingEntityRenderState> Spec<T, S> of(
                Supplier<ModelTree> tree,
                Supplier<ModelPart> bake,
                Function<ModelPart, EntityModel<? super S>> model,
                Supplier<S> state) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    (entity, s, partialTick) -> {},
                    (pose, s) -> {},
                    s -> 0F,
                    90F,
                    0F,
                    1F,
                    false,
                    entity -> true);
        }

        public Spec<T, S> extractor(Extractor<T, S> extractor) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    shadowRadius,
                    shadowStrength,
                    fullBright,
                    instanced);
        }

        public Spec<T, S> scale(ScaleHook<S> scale) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    shadowRadius,
                    shadowStrength,
                    fullBright,
                    instanced);
        }

        public Spec<T, S> whiteOverlay(ToFloatFunction<S> whiteOverlay) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    shadowRadius,
                    shadowStrength,
                    fullBright,
                    instanced);
        }

        public Spec<T, S> flipDegrees(float flipDegrees) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    shadowRadius,
                    shadowStrength,
                    fullBright,
                    instanced);
        }

        public Spec<T, S> shadow(float radius, float strength) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    radius,
                    strength,
                    fullBright,
                    instanced);
        }

        public Spec<T, S> asFullBright() {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    shadowRadius,
                    shadowStrength,
                    true,
                    instanced);
        }

        public Spec<T, S> instanced(Predicate<T> instanced) {
            return new Spec<>(
                    tree,
                    bake,
                    model,
                    state,
                    extractor,
                    scale,
                    whiteOverlay,
                    flipDegrees,
                    shadowRadius,
                    shadowStrength,
                    fullBright,
                    instanced);
        }
    }
}
