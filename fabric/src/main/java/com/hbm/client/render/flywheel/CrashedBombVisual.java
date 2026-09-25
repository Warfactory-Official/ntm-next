// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.bomb.BlockCrashedBomb;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class CrashedBombVisual extends HbmBlockEntityVisual<TileEntityCrashedBomb>
        implements ShaderLightVisual {
    private static final MeshPart[][] DUD_PARTS = {
        parts(ResourceManager.dud_balefire, ResourceManager.dud_balefire_tex),
        parts(ResourceManager.dud_conventional, ResourceManager.dud_conventional_tex),
        parts(ResourceManager.dud_nuke, ResourceManager.dud_nuke_tex),
        parts(ResourceManager.dud_salted, ResourceManager.dud_salted_tex)
    };
    private final TransformedInstance[] parts;

    public CrashedBombVisual(
            VisualizationContext context, TileEntityCrashedBomb blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var type = ((BlockCrashedBomb) blockState.getBlock()).type;
        var random = new Random((pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX());
        float yaw = (float) (random.nextDouble() * 360D);
        float pitch = (float) (random.nextDouble() * 45D + 45D);
        float roll = (float) (random.nextDouble() * 360D);
        float shift =
                -(float) (random.nextDouble() * 2D - 1D)
                        + switch (type) {
                            case NUKE -> 1.25F;
                            case SALTED -> .5F;
                            default -> 0F;
                        };
        var local =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(yaw * Mth.DEG_TO_RAD)
                        .rotateX(pitch * Mth.DEG_TO_RAD)
                        .rotateZ(roll * Mth.DEG_TO_RAD)
                        .translate(0, 0, shift);
        var pose =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(local);
        var models = DUD_PARTS[type.ordinal()];
        parts = new TransformedInstance[models.length];
        for (int i = 0; i < models.length; i++) {
            parts[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, models[i].model())
                            .createInstance();
            parts[i].setTransform(pose).light(0).setChanged();
        }
    }

    public static void initModels() {}

    private static MeshPart[] parts(HFRWavefrontObject source, Identifier texture) {
        return MeshPart.objParts(source, MeshPart.litCutout(texture));
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(7);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var part : parts) consumer.accept(part);
    }

    @Override
    protected void _delete() {
        for (var part : parts) part.delete();
    }
}
