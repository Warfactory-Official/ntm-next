// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.tileentity.machine.storage.BlockEntityBarrel;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class BarrelVisual extends HbmDynamicBlockEntityVisual<BlockEntityBarrel>
        implements ShaderLightVisual {
    private final HazardDiamondVisual hazards;
    private final Matrix4f pose = new Matrix4f();
    private NTMFluidProperty lastProperty;
    private boolean initialized;

    public BarrelVisual(
            VisualizationContext context, BlockEntityBarrel blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        hazards = new HazardDiamondVisual(context, level, pos, 4);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        Fluid type = blockEntity.tank.getTankType();
        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        if (initialized && prop == lastProperty) return;
        lastProperty = prop;
        initialized = true;
        if (prop == null) {
            for (int i = 0; i < 4; i++) hazards.hide(i);
            return;
        }
        for (int side = 0; side < 4; side++) {
            pose.identity()
                    .translate(.5F, .5F, .5F)
                    .rotateY((90F * side) * Mth.DEG_TO_RAD)
                    .translate(.4F, .30F, -.24F)
                    .scale(1F, .25F, .25F);
            hazards.update(
                    side,
                    pose,
                    prop.nfpaHealth(),
                    prop.nfpaFlame(),
                    prop.nfpaReact(),
                    prop.symbol());
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    protected void _delete() {
        hazards.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}
}
