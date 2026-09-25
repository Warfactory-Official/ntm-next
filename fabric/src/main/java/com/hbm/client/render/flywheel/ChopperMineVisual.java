// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.projectile.EntityChopperMine;
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
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ChopperMineVisual extends HbmDynamicEntityVisual<EntityChopperMine> {
    private static final Model MODEL =
            new SingleMeshModel(
                    mesh(),
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(ResourceManager.chopper_bomb_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance instance;
    private final Matrix4f pose = new Matrix4f();
    private final NameTagComponent nameTag;
    private final ShadowComponent shadow;
    private float lastX, lastY, lastZ;
    private int lastLight;
    private boolean written;

    public ChopperMineVisual(
            VisualizationContext context, EntityChopperMine entity, float partialTick) {
        super(context, entity, partialTick);
        instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        nameTag = new NameTagComponent(context, entity);
        shadow = new ShadowComponent(context, entity).radius(0F).strength(1F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static PackedQuadMesh mesh() {
        return new CuboidMesh(32F, 16F)
                .box(new Matrix4f(), 0F, 0F, -4F, -4F, -4F, 8F, 8F, 8F, 0F)
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
        int light = computePackedLight(partialTick);
        if (written
                && lastX == visualPos.x
                && lastY == visualPos.y
                && lastZ == visualPos.z
                && lastLight == light) return;
        pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(1.5F).rotateX(Mth.PI);
        instance.setTransform(pose).light(light).setChanged();
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastLight = light;
        written = true;
    }

    @Override
    protected void _delete() {
        instance.delete();
        nameTag.delete();
        shadow.delete();
    }
}
