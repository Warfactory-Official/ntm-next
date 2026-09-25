// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.BlockEntityTesla;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TeslaVisual extends HbmDynamicBlockEntityVisual<BlockEntityTesla> {
    private static final double ARC_REACH = 12D;
    private final ArrayList<BeamVisual> arcs = new ArrayList<>();
    private final Matrix4f local =
            new Matrix4f().translation(.5F, (float) BlockEntityTesla.OFFSET, .5F);
    private Vec3[] targetVectors = new Vec3[0];
    private int lastCount = -1;
    private int lastPhase = Integer.MIN_VALUE;

    public TeslaVisual(
            VisualizationContext context, BlockEntityTesla blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int count = blockEntity.targets.size();
        if (count == 0 && lastCount == 0) return;
        int phase = (int) (level.getGameTime() % 1000L) + 1;
        boolean targetsChanged = count != lastCount || targetVectors.length != count;
        if (!targetsChanged) {
            for (int i = 0; i < count; i++) {
                Vec3 target = blockEntity.targets.get(i);
                Vec3 cached = targetVectors[i];
                if (target.x != cached.x + pos.getX() + .5D
                        || target.y != cached.y + pos.getY() + BlockEntityTesla.OFFSET
                        || target.z != cached.z + pos.getZ() + .5D) {
                    targetsChanged = true;
                    break;
                }
            }
        }
        if (!targetsChanged && phase == lastPhase) return;
        while (arcs.size() < count)
            arcs.add(new BeamVisual(visualizationContext, level, pos, false, 2, 1F));
        while (arcs.size() > count) arcs.removeLast().delete();
        if (targetVectors.length != count) targetVectors = new Vec3[count];
        for (int i = 0; i < count; i++) {
            Vec3 target = blockEntity.targets.get(i);
            Vec3 delta = targetVectors[i];
            if (targetsChanged || delta == null) {
                delta =
                        target.subtract(
                                pos.getX() + .5D,
                                pos.getY() + BlockEntityTesla.OFFSET,
                                pos.getZ() + .5D);
                targetVectors[i] = delta;
            }
            arcs.get(i)
                    .update(
                            local,
                            delta,
                            EnumWaveType.RANDOM,
                            phase,
                            (int) (delta.length() * 5),
                            .125F,
                            .03125F,
                            0x404040,
                            0x404040);
        }
        lastCount = count;
        lastPhase = phase;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos);
    }

    @Override
    protected AABB visibleBounds() {
        return new AABB(pos).inflate(ARC_REACH);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var arc : arcs) arc.delete();
        arcs.clear();
    }
}
