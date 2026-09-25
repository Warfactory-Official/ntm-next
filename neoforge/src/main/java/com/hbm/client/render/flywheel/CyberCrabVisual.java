// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
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

public final class CyberCrabVisual extends HbmDynamicEntityVisual<EntityCyberCrab> {
    private static final float[] LEG_YAW = {0.78539816F, -0.78539816F, -2.35619449F, 2.35619449F};
    private static final float[] FANG_YAW = {-0.6981317F, 0.87266463F, -2.26892803F, 2.44346095F};
    private static final int[] LEG_SWING = {1, -1, -1, 1};
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final Models MODELS = Models.create();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance shell;
    private final TransformedInstance[] legs = new TransformedInstance[4];
    private final TransformedInstance[] feet = new TransformedInstance[4];
    private final TransformedInstance[] fangs = new TransformedInstance[4];
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f lastPose = new Matrix4f();
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private float lastSwing;
    private int lastLight, lastOverlay;
    private boolean visible;

    public CyberCrabVisual(
            VisualizationContext context, EntityCyberCrab entity, float partialTick) {
        super(context, entity, partialTick);
        shell = instance(MODELS.shell);
        for (int i = 0; i < 4; i++) {
            legs[i] = instance(MODELS.legs[i]);
            feet[i] = instance(MODELS.feet[i]);
            fangs[i] = instance(MODELS.fangs[i]);
        }
        shell.setVisible(false);
        for (int i = 0; i < 4; i++) {
            legs[i].setVisible(false);
            feet[i].setVisible(false);
            fangs[i].setVisible(false);
        }
        nameTag = new NameTagComponent(context, entity).shouldShow(() -> visibleForTag());
        shadow = new ShadowComponent(context, entity).radius(1F).strength(0F);
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
            float depth) {
        return model(
                new CuboidMesh(64F, 32F)
                        .box(new Matrix4f(), u, v, x, y, z, width, height, depth, 0F),
                material);
    }

    private TransformedInstance instance(Model model) {
        return instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
    }

    @Override
    protected void frame(DynamicVisual.Context context) {
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
        shadow.radius(drawn ? entity.getScale() : 0F);
        if (!drawn) {
            if (!visible) return;
            shell.setVisible(false);
            for (int i = 0; i < 4; i++) {
                legs[i].setVisible(false);
                feet[i].setVisible(false);
                fangs[i].setVisible(false);
            }
            visible = false;
            return;
        }

        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(entity.getScale());
        livingPose(pose, partialTick);
        pose.scale(-1F, -1F, 1F);
        pose.translate(0F, 1.5F, 0F).rotateY(-Mth.HALF_PI).translate(0F, -1.501F, 0F);
        int light = computePackedLight(partialTick);
        int overlay = OverlayTexture.pack(0F, entity.hurtTime > 0 || entity.deathTime > 0);

        float walkPosition =
                entity.isPassenger() || !entity.isAlive()
                        ? 0F
                        : entity.walkAnimation.position(partialTick);
        float walkSpeed =
                entity.isPassenger() || !entity.isAlive()
                        ? 0F
                        : entity.walkAnimation.speed(partialTick);

        float swing = -(Mth.cos(walkPosition * 0.6662F * 2F) * 0.4F) * walkSpeed * 1.5F;
        if (visible
                && lastPose.equals(pose)
                && lastSwing == swing
                && lastLight == light
                && lastOverlay == overlay) return;
        write(shell, local.identity(), pose, light, overlay);
        for (int i = 0; i < 4; i++) {
            local.identity()
                    .translate(0F, -3F / 16F, 0F)
                    .rotateZYX(0F, LEG_YAW[i] + swing * LEG_SWING[i], -0.17453293F);
            write(legs[i], local, pose, light, overlay);
            local.identity()
                    .translate(0F, -3F / 16F, 0F)
                    .rotateZYX(0F, LEG_YAW[i] + swing * LEG_SWING[i], 0.17453293F);
            write(feet[i], local, pose, light, overlay);
            local.identity().translate(0F, -3F / 16F, 0F).rotateZYX(0F, FANG_YAW[i], -0.43633231F);
            write(fangs[i], local, pose, light, overlay);
        }
        lastPose.set(pose);
        lastSwing = swing;
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
            Matrix4f localTransform,
            Matrix4f base,
            int light,
            int overlay) {
        world.set(base).mul(localTransform);
        if (!visible) instance.setVisible(true);
        instance.setTransform(world);
        instance.overlay(overlay);
        instance.light(light);
        instance.setChanged();
    }

    @Override
    protected void _delete() {
        shell.delete();
        for (int i = 0; i < 4; i++) {
            legs[i].delete();
            feet[i].delete();
            fangs[i].delete();
        }
        nameTag.delete();
        shadow.delete();
    }

    private static final class Models {
        private final Model shell;
        private final Model[] legs;
        private final Model[] feet;
        private final Model[] fangs;

        private Models(Model shell, Model[] legs, Model[] feet, Model[] fangs) {
            this.shell = shell;
            this.legs = legs;
            this.feet = feet;
            this.fangs = fangs;
        }

        private static Models create() {
            Material material =
                    SimpleMaterial.builder()
                            .texture(ResourceManager.crab_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .backfaceCulling(false)
                            .build();
            CuboidMesh shellMesh =
                    new CuboidMesh(64F, 32F)
                            .box(new Matrix4f(), 1F, 1F, -2F, -3F, -2F, 4F, 1F, 4F, 0F)
                            .box(new Matrix4f(), 17F, 1F, -2F, -4F, -3F, 4F, 1F, 6F, 0F)
                            .box(new Matrix4f(), 33F, 1F, -1.5F, -5F, -1.5F, 3F, 1F, 3F, 0F)
                            .box(new Matrix4f(), 49F, 1F, -2F, -4.5F, -1F, 4F, 1F, 2F, 0F)
                            .box(new Matrix4f(), 1F, 9F, -3F, -4F, -2F, 6F, 1F, 4F, 0F)
                            .box(new Matrix4f(), 1F, 25F, -1F, -4.5F, -2F, 2F, 1F, 4F, 0F)
                            .box(new Matrix4f(), 17F, 25F, -2.5F, -3.5F, -1.5F, 5F, 1F, 3F, 0F)
                            .box(new Matrix4f(), 33F, 25F, -1.5F, -3.5F, -2.5F, 3F, 1F, 5F, 0F);
            int[][] legUv = {{25, 9}, {41, 9}, {1, 17}, {17, 17}};
            int[][] footUv = {{33, 17}, {57, 9}, {41, 17}, {49, 17}};
            int[][] fangUv = {{17, 1}, {33, 9}, {49, 9}, {9, 17}};
            Model[] legs = new Model[4], feet = new Model[4], fangs = new Model[4];
            for (int i = 0; i < 4; i++) {
                legs[i] = cube(material, legUv[i][0], legUv[i][1], -.5F, 0F, 2F, 1F, 1F, 3F);
                feet[i] = cube(material, footUv[i][0], footUv[i][1], -.5F, 1F, 4F, 1F, 3F, 1F);
                fangs[i] = cube(material, fangUv[i][0], fangUv[i][1], -.5F, 0F, 1.5F, 1F, 1F, 1F);
            }
            return new Models(model(shellMesh, material), legs, feet, fangs);
        }
    }
}
