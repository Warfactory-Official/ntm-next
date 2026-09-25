// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.projectile.EntityBullet;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
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
import java.util.Random;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LegacyBulletVisual extends HbmDynamicEntityVisual<EntityBullet> {
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance instance;
    private final Matrix4f pose = new Matrix4f();
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private final float roll;
    private float lastX, lastY, lastZ, lastYaw, lastPitch;
    private int lastLight;
    private boolean written;

    public LegacyBulletVisual(
            VisualizationContext context, EntityBullet entity, float partialTick) {
        super(context, entity, partialTick);
        Model model =
                entity.getIsChopper()
                        ? Models.CHOPPER
                        : entity.getIsCritical() ? Models.CRITICAL : Models.BULLET;
        instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        nameTag = new NameTagComponent(context, entity);
        shadow = new ShadowComponent(context, entity).radius(0F).strength(1F);
        roll = new Random(entity.getId()).nextInt(360);
        writeFrame(partialTick);
    }

    public static void initModels() {
        Models.init();
    }

    private static PackedQuadMesh mesh() {
        return new CuboidMesh(8F, 4F)
                .box(new Matrix4f(), 0F, 0F, 1F, -.5F, -.5F, 2F, 1F, 1F, 0F)
                .mesh();
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        nameTag.beginFrame(context);
        shadow.beginFrame(context);
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float yaw =
                (Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90F) * Mth.DEG_TO_RAD;
        float pitch =
                (Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 180F) * Mth.DEG_TO_RAD;
        int light = computePackedLight(partialTick);
        if (written
                && lastX == visualPos.x
                && lastY == visualPos.y
                && lastZ == visualPos.z
                && lastYaw == yaw
                && lastPitch == pitch
                && lastLight == light) return;
        pose.translation(visualPos.x, visualPos.y, visualPos.z)
                .rotateY(yaw)
                .rotateZ(pitch)
                .scale(1.5F)
                .rotateX(roll * Mth.DEG_TO_RAD);
        instance.setTransform(pose).light(light).setChanged();
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastYaw = yaw;
        lastPitch = pitch;
        lastLight = light;
        written = true;
    }

    @Override
    protected void _delete() {
        instance.delete();
        nameTag.delete();
        shadow.delete();
    }

    private static final class Models {
        private static final PackedQuadMesh MESH = mesh();
        private static final Model BULLET = create(ResourceManager.bullet_tex);
        private static final Model CHOPPER = create(ResourceManager.bullet_chopper_tex);
        private static final Model CRITICAL = create(ResourceManager.bullet_critical_tex);

        static void init() {}

        private static Model create(Identifier texture) {
            return new SingleMeshModel(
                    MESH,
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(texture)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .build());
        }
    }
}
