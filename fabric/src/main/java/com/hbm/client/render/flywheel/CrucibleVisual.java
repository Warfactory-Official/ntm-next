// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.data.MachineData;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityCrucible;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class CrucibleVisual extends HbmDynamicBlockEntityVisual<BlockEntityCrucible> {
    private static final int POUR_REACH = 6;
    private static final MeshPart MELT_MODEL =
            FoundryTankVisual.molten(
                    MeshPart.face(
                            Direction.UP,
                            SimpleMaterial.builderOf(FoundryTankVisual.MOLTEN)
                                    .texture(ResourceManager.foundry_lava_tex)
                                    .build()));

    private final TransformedInstance melt;
    private final ArrayList<PourVisual> pours = new ArrayList<>();
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private final Direction facing;
    private final Matrix4f basePose = new Matrix4f();
    private int lastMass = Integer.MIN_VALUE;

    public CrucibleVisual(
            VisualizationContext context, BlockEntityCrucible blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        facing = BlockMultiblockCore.coreFacing(blockState);
        basePose.translation(.5F, 0F, .5F).rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        melt =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MELT_MODEL.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int
                capacity =
                        MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                                + MachineData.CRUCIBLE_WASTE_CAPACITY.get(),
                mass = 0;
        for (var stack : blockEntity.recipeStack) mass += stack.amount;
        for (var stack : blockEntity.wasteStack) mass += stack.amount;
        if (mass != lastMass) {
            melt.setVisible(mass > 0);
            if (mass > 0) {
                float height = (float) (.5D + (double) mass / capacity * .875D);
                local.set(basePose)
                        .translate(0F, height, 0F)
                        .translate(-1F, -1F, -1F)
                        .scale(2F, 1F, 2F);
                pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
                melt.setTransform(pose).light(LightCoordsUtil.FULL_BRIGHT).setChanged();
            }
            lastMass = mass;
        }
        long now = level.getGameTime();
        int used = 0;
        for (var stream : blockEntity.streams) {
            float age = now - stream.birth() + partialTick;
            if (age >= 20F) continue;
            if (used == pours.size()) pours.add(new PourVisual(visualizationContext, level, pos));
            Direction direction = stream.waste() ? facing.getOpposite() : facing;
            pours.get(used++)
                    .update(
                            stream.color(),
                            direction,
                            stream.len(),
                            age,
                            .625F,
                            .625F,
                            (float) (.5D + direction.getStepX() * 1.875D),
                            0,
                            (float) (.5D + direction.getStepZ() * 1.875D));
        }
        while (pours.size() > used) pours.removeLast().delete();
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos).inflate(2).expandTowards(0, -POUR_REACH, 0);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        melt.delete();
        for (var pour : pours) pour.delete();
        pours.clear();
    }
}
