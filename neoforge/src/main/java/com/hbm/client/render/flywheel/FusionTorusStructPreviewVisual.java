// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.StructGhost;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorusStruct;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class FusionTorusStructPreviewVisual
        extends HbmBlockEntityVisual<BlockEntityFusionTorusStruct> implements ShaderLightVisual {
    private final List<UvTransformedInstance> ghosts = new ArrayList<>();

    public FusionTorusStructPreviewVisual(
            VisualizationContext context,
            BlockEntityFusionTorusStruct blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var pose = new Matrix4f();
        BlockEntityFusionTorusStruct.forEachRequirement(
                (block, dx, dy, dz) -> {
                    var sprite = StructGhost.sprite(block);
                    var instance =
                            instancerProvider()
                                    .instancer(InstanceTypes.UV_TRANSFORMED, PreviewCube.MODEL)
                                    .createInstance();
                    instance.uvRegion(
                            sprite.getU0(),
                            sprite.getV0(),
                            sprite.getU1() - sprite.getU0(),
                            sprite.getV1() - sprite.getV0());
                    VisualTextures.animated(sprite);
                    pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                            .translate(dx, dy, dz);
                    instance.setTransform(pose);
                    instance.setChanged();
                    ghosts.add(instance);
                });
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                pos.getX() - 8,
                pos.getY() - 1,
                pos.getZ() - 8,
                pos.getX() + 9,
                pos.getY() + 6,
                pos.getZ() + 9);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (UvTransformedInstance ghost : ghosts) ghost.delete();
    }
}
