/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.blockentity.machine.FoundryTankBlockEntity;
import com.hbm.blocks.machine.FoundryOutletBlock;
import com.hbm.blocks.machine.FoundryTankBlock;
import com.hbm.client.render.FoundryFaces;
import com.hbm.main.ResourceManager;
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
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public final class FoundryTankVisual extends AbstractBlockEntityVisual<FoundryTankBlockEntity>
        implements CulledDynamicVisual {
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
            VisualizationContext context, FoundryTankBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        bodyMask = FoundryTankBlock.mask(level, pos);
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
                || !(before.getBlock() instanceof FoundryTankBlock
                        || after.getBlock() instanceof FoundryTankBlock
                        || before.getBlock() instanceof FoundryOutletBlock
                        || after.getBlock() instanceof FoundryOutletBlock)) return;
        var manager = VisualizationManager.get(level);
        if (manager == null) return;
        for (Direction direction : Direction.VALUES) {
            var neighbor = ChunkUtil.blockEntityIfLoaded(level, pos.relative(direction));
            if (neighbor instanceof FoundryTankBlockEntity)
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
                if (entry.getValue() instanceof FoundryTankBlockEntity
                        && ((target.getX() - direction.getStepX()) >> 4) == chunkX
                        && ((target.getZ() - direction.getStepZ()) >> 4) == chunkZ)
                    manager.blockEntities().queueUpdate(entry.getValue());
            }
        }
    }

    @Override
    public void frame(Context context) {
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
        int color = present ? FoundryFaces.brightenMolten(blockEntity.type.moltenColor()) : -1;
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
        int currentMask = FoundryTankBlock.mask(level, pos);
        if (currentMask != bodyMask) {
            bodyMask = currentMask;
            initialized = false;
        }
        updateMovingParts(partialTick);
    }
}
