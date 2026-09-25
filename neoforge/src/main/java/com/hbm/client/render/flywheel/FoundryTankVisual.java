// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.FoundryOutlet;
import com.hbm.blocks.machine.FoundryTank;
import com.hbm.client.render.FoundryFaces;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityFoundryTank;
import com.hbm.util.ChunkUtil;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.PackIdentity;
import dev.engine_room.flywheel.lib.model.PackTaggedModel;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public final class FoundryTankVisual extends HbmDynamicBlockEntityVisual<BlockEntityFoundryTank> {
    static final Material MOLTEN =
            SimpleMaterial.builder()
                    .texture(ResourceManager.foundry_stream_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .backfaceCulling(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .useOverlay(false)
                    .light(LightShaders.NONE)
                    .build();
    static final Material TERRAIN_MOLTEN =
            SimpleMaterial.builderOf(MOLTEN)
                    .cutout(CutoutShaders.HALF)
                    .backfaceCulling(true)
                    .build();
    private static final Direction[] DIRECTIONS = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final MeshPart[] MOLTEN_FACES = buildMoltenFaces();
    private final AffineUvTransformedInstance[] faces = new AffineUvTransformedInstance[5];
    private final Matrix4f local = new Matrix4f(), pose = new Matrix4f();
    private int bodyMask;
    private int lastMask = Integer.MIN_VALUE;
    private float lastHeight = Float.NaN;
    private int lastColor = Integer.MIN_VALUE;
    private boolean lastPresent;
    private boolean initialized;

    public FoundryTankVisual(
            VisualizationContext context, BlockEntityFoundryTank blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        bodyMask = FoundryTank.mask(level, pos);
        for (int i = 0; i < faces.length; i++) {
            var model = MOLTEN_FACES[i];
            faces[i] =
                    instancerProvider()
                            .instancer(AffineUvTransformedInstance.TYPE, model.model())
                            .createInstance();
        }
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    static MeshPart molten(MeshPart part) {
        return new MeshPart(new PackTaggedModel(part.model(), PackIdentity.Effect.LAVA_GLOW));
    }

    private static MeshPart[] buildMoltenFaces() {
        var faces = new MeshPart[DIRECTIONS.length];
        for (int i = 0; i < faces.length; i++)
            faces[i] = molten(MeshPart.face(DIRECTIONS[i], TERRAIN_MOLTEN));
        return faces;
    }

    public static void blockChanged(
            ClientLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (before == after
                || !(before.getBlock() instanceof FoundryTank
                        || after.getBlock() instanceof FoundryTank
                        || before.getBlock() instanceof FoundryOutlet
                        || after.getBlock() instanceof FoundryOutlet)) return;
        var manager = VisualizationManager.get(level);
        if (manager == null) return;
        for (Direction direction : Direction.VALUES) {
            var neighbor = ChunkUtil.blockEntityIfLoaded(level, pos.relative(direction));
            if (neighbor instanceof BlockEntityFoundryTank)
                manager.blockEntities().queueUpdate(neighbor);
        }
    }

    public static void chunkChanged(ClientLevel level, int chunkX, int chunkZ) {
        var manager = VisualizationManager.get(level);
        if (manager == null) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var neighbor =
                    level.getChunkSource()
                            .getChunk(
                                    chunkX + direction.getStepX(),
                                    chunkZ + direction.getStepZ(),
                                    false);
            if (neighbor == null) continue;
            for (var entry : neighbor.getBlockEntities().entrySet()) {
                var target = entry.getKey();
                if (entry.getValue() instanceof BlockEntityFoundryTank
                        && ((target.getX() - direction.getStepX()) >> 4) == chunkX
                        && ((target.getZ() - direction.getStepZ()) >> 4) == chunkZ)
                    manager.blockEntities().queueUpdate(entry.getValue());
            }
        }
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean present = blockEntity.amount > 0 && blockEntity.type != null;
        int mask = present ? bodyMask : 0;
        float bottom = (mask & 1 << Direction.DOWN.ordinal()) != 0 ? 0 : .125F;
        double maximum =
                .75D
                        + ((mask & 1 << Direction.DOWN.ordinal()) != 0 ? .125D : 0)
                        + ((mask & 1 << Direction.UP.ordinal()) != 0 ? .125D : 0);
        float height =
                present ? (float) (blockEntity.amount * maximum / blockEntity.getCapacity()) : 0;
        int color = present ? FoundryFaces.brightenMolten(blockEntity.type.moltenColor) : -1;
        if (initialized
                && present == lastPresent
                && mask == lastMask
                && height == lastHeight
                && color == lastColor) return;
        lastPresent = present;
        lastMask = mask;
        lastHeight = height;
        lastColor = color;
        local.translation(0, bottom, 0).scale(1, height, 1);
        pose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        for (int i = 0; i < faces.length; i++) {
            Direction direction = DIRECTIONS[i];
            boolean visible = present && (i == 0 || (mask & 1 << direction.ordinal()) != 0);
            var face = faces[i];
            face.setVisible(visible);
            if (!visible) continue;
            face.setTransform(pose).colorArgb(color).light(LightCoordsUtil.pack(15, 0));
            float scaleV = i == 0 ? 1 : height, offsetV = i == 0 ? 0 : 1 - bottom - height;
            face.uv(1, 0, 0, scaleV, 0, offsetV).setChanged();
        }
        initialized = true;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var face : faces) face.delete();
    }

    @Override
    public void update(float partialTick) {
        int currentMask = FoundryTank.mask(level, pos);
        if (currentMask != bodyMask) {
            bodyMask = currentMask;
            initialized = false;
        }
        updateMovingParts(partialTick);
    }
}
