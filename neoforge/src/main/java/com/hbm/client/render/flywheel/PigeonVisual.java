// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.entity.mob.EntityPigeon;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.component.NameTagComponent;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PigeonVisual extends HbmDynamicEntityVisual<EntityPigeon> {
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final Models MODELS = Models.create();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance head;
    private final TransformedInstance beak;
    private final TransformedInstance body;
    private final TransformedInstance bodyFat;
    private final TransformedInstance leftLeg;
    private final TransformedInstance rightLeg;
    private final TransformedInstance ass;
    private final TransformedInstance feathers;
    private final TransformedInstance leftWing;
    private final TransformedInstance rightWing;
    private final TransformedInstance leftWingFat;
    private final TransformedInstance rightWingFat;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f lastPose = new Matrix4f();
    private final byte[] visibility = new byte[12];
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private float lastHeadPitch, lastHeadYaw, lastLeftLeg, lastRightLeg, lastFlap;
    private int lastLight, lastOverlay;
    private boolean lastFat, visible;

    public PigeonVisual(VisualizationContext context, EntityPigeon entity, float partialTick) {
        super(context, entity, partialTick);
        head = instance(MODELS.head);
        beak = instance(MODELS.beak);
        body = instance(MODELS.body);
        bodyFat = instance(MODELS.bodyFat);
        leftLeg = instance(MODELS.leg);
        rightLeg = instance(MODELS.leg);
        ass = instance(MODELS.ass);
        feathers = instance(MODELS.feathers);
        leftWing = instance(MODELS.leftWing);
        rightWing = instance(MODELS.rightWing);
        leftWingFat = instance(MODELS.leftWing);
        rightWingFat = instance(MODELS.rightWing);
        setVisible(head, 0, false);
        setVisible(beak, 1, false);
        setVisible(body, 2, false);
        setVisible(bodyFat, 3, false);
        setVisible(leftLeg, 4, false);
        setVisible(rightLeg, 5, false);
        setVisible(ass, 6, false);
        setVisible(feathers, 7, false);
        setVisible(leftWing, 8, false);
        setVisible(rightWing, 9, false);
        setVisible(leftWingFat, 10, false);
        setVisible(rightWingFat, 11, false);
        nameTag = new NameTagComponent(context, entity).shouldShow(() -> visibleForTag());
        shadow = new ShadowComponent(context, entity).radius(.3F).strength(1F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Model model(CuboidMesh mesh, Material material) {
        return new SingleMeshModel(mesh.mesh(), material);
    }

    private static Model cube(
            Material material,
            float u,
            float v,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            float inflate) {
        return model(
                new CuboidMesh(64F, 32F)
                        .box(new Matrix4f(), u, v, x, y, z, width, height, depth, inflate),
                material);
    }

    private TransformedInstance instance(Model model) {
        return instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        nameTag.beginFrame(context);
        shadow.beginFrame(context);
    }

    private boolean visibleForTag() {
        return !VanishedEntities.isVanished(entity)
                && (entity.shouldShowName()
                        || entity.hasCustomName()
                                && entity
                                        == Minecraft.getInstance()
                                                .getEntityRenderDispatcher()
                                                .crosshairPickEntity);
    }

    private void writeFrame(float partialTick) {
        boolean drawn = !VanishedEntities.isVanished(entity) && !entity.isInvisible();
        shadow.radius(drawn ? .3F * entity.getScale() : 0F);
        if (!drawn) {
            if (!visible) return;
            setVisible(head, 0, false);
            setVisible(beak, 1, false);
            setVisible(body, 2, false);
            setVisible(bodyFat, 3, false);
            setVisible(leftLeg, 4, false);
            setVisible(rightLeg, 5, false);
            setVisible(ass, 6, false);
            setVisible(feathers, 7, false);
            setVisible(leftWing, 8, false);
            setVisible(rightWing, 9, false);
            setVisible(leftWingFat, 10, false);
            setVisible(rightWingFat, 11, false);
            visible = false;
            return;
        }

        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        pose.identity().translation(visualPos.x, visualPos.y, visualPos.z).scale(entity.getScale());
        livingPose(pose, partialTick);
        pose.scale(-1F, -1F, 1F);
        pose.translate(0F, -1.501F, 0F);
        int light = computePackedLight(partialTick);
        int overlay = OverlayTexture.pack(0F, entity.hurtTime > 0 || entity.deathTime > 0);

        float headPitch = entity.getXRot(partialTick);
        float headYaw =
                Mth.wrapDegrees(
                        Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot)
                                - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
        if (upsideDown()) {
            headPitch *= -1F;
            headYaw *= -1F;
        }
        boolean fat = entity.isFat();

        float walkPosition =
                entity.isPassenger() || !entity.isAlive()
                        ? 0F
                        : entity.walkAnimation.position(partialTick);
        float walkSpeed =
                entity.isPassenger() || !entity.isAlive()
                        ? 0F
                        : entity.walkAnimation.speed(partialTick);
        float rightLegRotation = Mth.cos(walkPosition * 0.6662F) * 1.4F * walkSpeed;
        float leftLegRotation = Mth.cos(walkPosition * 0.6662F + Mth.PI) * 1.4F * walkSpeed;
        float fall = Mth.lerp(partialTick, entity.prevFallTime, entity.fallTime);
        float dest = Mth.lerp(partialTick, entity.prevDest, entity.dest);
        float flap = (Mth.sin(fall) + 1F) * dest;
        if (visible
                && lastPose.equals(pose)
                && lastHeadPitch == headPitch
                && lastHeadYaw == headYaw
                && lastFat == fat
                && lastLeftLeg == leftLegRotation
                && lastRightLeg == rightLegRotation
                && lastFlap == flap
                && lastLight == light
                && lastOverlay == overlay) return;

        local.identity()
                .translate(0F, 1F, (fat ? -4F : -2F) / 16F)
                .rotateZYX(0F, headYaw * DEG, headPitch * DEG);
        write(head, 0, local, light, overlay);
        write(beak, 1, local, light, overlay);
        local.identity().translate(0F, 17F / 16F, 0F).rotateX(-Mth.PI / 4F);
        write(body, 2, local, light, overlay, !fat);
        write(bodyFat, 3, local, light, overlay, fat);
        local.identity()
                .translate(1F / 16F, 20F / 16F, -1F / 16F)
                .rotateZYX(0F, 0F, leftLegRotation);
        write(leftLeg, 4, local, light, overlay);
        local.identity()
                .translate(-1F / 16F, 20F / 16F, -1F / 16F)
                .rotateZYX(0F, 0F, rightLegRotation);
        write(rightLeg, 5, local, light, overlay);

        local.identity().translate(0F, 20F / 16F, (fat ? 5F : 4F) / 16F).rotateX(-Mth.PI / 4F);
        write(ass, 6, local, light, overlay);
        local.identity()
                .translate(0F, 21.5F / 16F, (fat ? 8.5F : 7.5F) / 16F)
                .rotateX(-Mth.PI / 8F);
        write(feathers, 7, local, light, overlay);
        local.identity()
                .translate(0F, 17F / 16F, 0F)
                .rotateX(-Mth.PI / 4F)
                .translate((fat ? 4F : 3F) / 16F, -2F / 16F, 0F)
                .rotateZ(-flap);
        write(leftWing, 8, local, light, overlay, !fat);
        write(leftWingFat, 10, local, light, overlay, fat);
        local.identity()
                .translate(0F, 17F / 16F, 0F)
                .rotateX(-Mth.PI / 4F)
                .translate((fat ? -4F : -3F) / 16F, -2F / 16F, 0F)
                .rotateZ(flap);
        write(rightWing, 9, local, light, overlay, !fat);
        write(rightWingFat, 11, local, light, overlay, fat);
        lastPose.set(pose);
        lastHeadPitch = headPitch;
        lastHeadYaw = headYaw;
        lastFat = fat;
        lastLeftLeg = leftLegRotation;
        lastRightLeg = rightLegRotation;
        lastFlap = flap;
        lastLight = light;
        lastOverlay = overlay;
        visible = true;
    }

    private void livingPose(Matrix4f pose, float partialTick) {
        float bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float age = entity.tickCount + partialTick;
        if (entity.isFullyFrozen())
            bodyRot += (float) (Math.cos(Mth.floor(age) * 3.25F) * Math.PI * 0.4F);
        if (entity.getPose() != Pose.SLEEPING) pose.rotateY((180F - bodyRot) * DEG);
        if (entity.deathTime > 0F) {
            float fall = Mth.sqrt((entity.deathTime + partialTick - 1F) / 20F * 1.6F);
            pose.rotateZ(Math.min(fall, 1F) * 90F * DEG);
        } else if (upsideDown()) {
            pose.translate(0F, (entity.getBbHeight() + 0.1F) / entity.getScale(), 0F);
            pose.rotateZ(Mth.PI);
        }
    }

    private boolean upsideDown() {
        var name = entity.getCustomName();
        return name != null
                && ("Dinnerbone".equals(name.getString()) || "Grumm".equals(name.getString()));
    }

    private void write(
            TransformedInstance instance,
            int slot,
            Matrix4f localTransform,
            int light,
            int overlay) {
        write(instance, slot, localTransform, light, overlay, true);
    }

    private void write(
            TransformedInstance instance,
            int slot,
            Matrix4f localTransform,
            int light,
            int overlay,
            boolean visible) {
        if (!visible) {
            setVisible(instance, slot, false);
            return;
        }
        setVisible(instance, slot, true);
        world.set(pose).mul(localTransform);
        instance.setTransform(world);
        instance.overlay(overlay);
        instance.light(light);
        instance.setChanged();
    }

    private void setVisible(TransformedInstance instance, int slot, boolean visible) {
        byte state = (byte) (visible ? 2 : 1);
        if (visibility[slot] == state) return;
        instance.setVisible(visible);
        visibility[slot] = state;
    }

    @Override
    protected void _delete() {
        head.delete();
        beak.delete();
        body.delete();
        bodyFat.delete();
        leftLeg.delete();
        rightLeg.delete();
        ass.delete();
        feathers.delete();
        leftWing.delete();
        rightWing.delete();
        leftWingFat.delete();
        rightWingFat.delete();
        nameTag.delete();
        shadow.delete();
    }

    private static final class Models {
        private final Model head;
        private final Model beak;
        private final Model body;
        private final Model bodyFat;
        private final Model leg;
        private final Model ass;
        private final Model feathers;
        private final Model leftWing;
        private final Model rightWing;

        private Models(
                Model head,
                Model beak,
                Model body,
                Model bodyFat,
                Model leg,
                Model ass,
                Model feathers,
                Model leftWing,
                Model rightWing) {
            this.head = head;
            this.beak = beak;
            this.body = body;
            this.bodyFat = bodyFat;
            this.leg = leg;
            this.ass = ass;
            this.feathers = feathers;
            this.leftWing = leftWing;
            this.rightWing = rightWing;
        }

        private static Models create() {
            Material material =
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(ResourceManager.pigeon_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .backfaceCulling(false)
                            .build();
            return new Models(
                    cube(material, 0F, 0F, -2F, -6F, -2F, 4F, 6F, 4F, 0F),
                    cube(material, 14F, 0F, -1F, -4F, -4F, 2F, 2F, 2F, 0F),
                    cube(material, 0F, 10F, -3F, -3F, -4F, 6F, 6F, 8F, 0F),
                    cube(material, 0F, 10F, -3F, -3F, -4F, 6F, 6F, 8F, 1F),
                    cube(material, 20F, 0F, -1F, 0F, 0F, 2F, 4F, 2F, 0F),
                    cube(material, 0F, 24F, -2F, -2F, -2F, 4F, 4F, 4F, 0F),
                    cube(material, 16F, 24F, -1F, -.5F, -2F, 2F, 1F, 4F, 0F),
                    cube(material, 28F, 0F, 0F, 0F, -3F, 1F, 4F, 6F, 0F),
                    cube(material, 28F, 10F, -1F, 0F, -3F, 1F, 4F, 6F, 0F));
        }
    }
}
