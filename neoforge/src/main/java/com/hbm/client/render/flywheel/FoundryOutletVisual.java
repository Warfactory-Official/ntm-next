// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.FoundryOutlet;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.tileentity.machine.BlockEntityFoundryOutlet;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FoundryOutletVisual extends HbmDynamicBlockEntityVisual<BlockEntityFoundryOutlet>
        implements ShaderLightVisual {
    private static final float MAX_AGE = 20F;
    private static final Identifier FILTER_TEXTURE =
            Library.id("textures/block/foundry_outlet_filter.png");
    private static final Identifier LOCK_TEXTURE =
            Library.id("textures/block/foundry_outlet_lock.png");

    private static final MeshPart[] PLATES = {
        plate(FILTER_TEXTURE, .96875F), plate(LOCK_TEXTURE, .9375F)
    };
    private final TransformedInstance[] plates = new TransformedInstance[PLATES.length];
    private final AABB bodyBounds;
    private final ArrayList<PourVisual> pours = new ArrayList<>();
    private final Direction facing;
    private final Matrix4f platePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastPourLength = Float.NaN;
    private boolean lastFilter;
    private boolean lastClosed;
    private boolean initialized;

    public FoundryOutletVisual(
            VisualizationContext context, BlockEntityFoundryOutlet blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockState.getValue(FoundryOutlet.FACING);
        platePose
                .translation(.5F, 0F, .5F)
                .rotateY(Facing.yaw(facing, 0) * Mth.DEG_TO_RAD)
                .translate(-.5F, 0F, -.5F);
        bodyBounds = new AABB(pos).inflate(1);
        for (int i = 0; i < plates.length; i++)
            plates[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PLATES[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static void includeStreamBounds(
            double[] bounds, BlockPos anchor, Direction direction, float length) {
        double x = anchor.getX() + .5D - direction.getStepX() * .125D;
        double z = anchor.getZ() + .5D - direction.getStepZ() * .125D;
        double y = anchor.getY() + .125D;
        double drop = Math.max(length, 0F);
        bounds[0] = Math.min(bounds[0], x - .625D - 1D);
        bounds[1] = Math.min(bounds[1], y - drop - .125D - 1D);
        bounds[2] = Math.min(bounds[2], z - .625D - 1D);
        bounds[3] = Math.max(bounds[3], x + .625D + 1D);
        bounds[4] = Math.max(bounds[4], y + .125D + 1D);
        bounds[5] = Math.max(bounds[5], z + .625D + 1D);
    }

    private static MeshPart plate(Identifier texture, float z) {
        float[] data = {
            .375F, .5F, z, .375F, .5F, 0F, 0F, 1F,
            .375F, .0625F, z, .375F, .9375F, 0F, 0F, 1F,
            .625F, .0625F, z, .625F, .9375F, 0F, 0F, 1F,
            .625F, .5F, z, .625F, .5F, 0F, 0F, 1F,
            .375F, .5F, z, .625F, .5F, 0F, 0F, -1F,
            .625F, .5F, z, .375F, .5F, 0F, 0F, -1F,
            .625F, .0625F, z, .375F, .9375F, 0F, 0F, -1F,
            .375F, .0625F, z, .625F, .9375F, 0F, 0F, -1F
        };
        int[] colors = {-1, -1, -1, -1, -1, -1, -1, -1};
        int[] light = new int[8];
        var mesh = PackedQuadMesh.of(data, colors, light);
        Material material =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(texture)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .build();
        return MeshPart.create(mesh, material);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean filter = blockEntity.filter != null;
        boolean closed = blockEntity.isClosed();
        boolean filterChanged = !initialized || filter != lastFilter;
        boolean closedChanged = !initialized || closed != lastClosed;
        if (filterChanged) writePlate(0, filter);
        if (closedChanged) writePlate(1, closed);

        int used = 0;
        float pourLength = -1F;
        long now = GameTime.now();
        for (PourStream stream : blockEntity.streams) {
            float age = (now - stream.birth()) + partialTick;
            if (age >= MAX_AGE) continue;
            if (used == pours.size()) pours.add(new PourVisual(visualizationContext, level, pos));
            pours.get(used++)
                    .update(
                            stream.color(),
                            facing,
                            stream.len(),
                            age,
                            0F,
                            .375F,
                            .5F - facing.getStepX() * .125F,
                            .125F,
                            .5F - facing.getStepZ() * .125F);
            pourLength = Math.max(pourLength, stream.len());
        }
        while (pours.size() > used) pours.removeLast().delete();

        if (filterChanged || closedChanged || pourLength != lastPourLength) {
            LightBounds.resetBounds(lightBounds, bodyBounds);
            if (filter)
                LightBounds.includeLightBounds(lightBounds, PLATES[0].model(), platePose, pos);
            if (closed)
                LightBounds.includeLightBounds(lightBounds, PLATES[1].model(), platePose, pos);
            if (used > 0) includeStreamBounds(lightBounds, pos, facing, pourLength);
            lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
            lastPourLength = pourLength;
        }
        lastFilter = filter;
        lastClosed = closed;
        initialized = true;
    }

    private void writePlate(int index, boolean visible) {
        TransformedInstance plate = plates[index];
        plate.setVisible(visible);
        if (!visible) return;
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(platePose);
        plate.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(16);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance plate : plates) consumer.accept(plate);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance plate : plates) plate.delete();
        for (PourVisual pour : pours) pour.delete();
        pours.clear();
    }
}
