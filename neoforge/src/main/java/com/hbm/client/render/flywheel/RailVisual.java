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

import com.hbm.blockentity.RailBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.rail.RailSwitchBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RailVisual extends AbstractBoundedBlockEntityVisual<RailBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final MeshPart[][] SIGNS = {
        signs(ResourceManager.rail_standard_switch, ResourceManager.rail_switch_sign_tex),
        signs(
                ResourceManager.rail_standard_switch_flipped,
                ResourceManager.rail_switch_sign_flipped_tex)
    };
    private final TransformedInstance[] signs;
    private final AABB bounds;
    private final Matrix4f world;
    private int lastSelected = Integer.MIN_VALUE;

    public RailVisual(
            VisualizationContext context, RailBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        boolean flipped =
                blockEntity.getBlockState().getBlock() instanceof RailSwitchBlock rail
                        && rail.mirrored();
        HFRWavefrontObject source =
                flipped
                        ? ResourceManager.rail_standard_switch_flipped
                        : ResourceManager.rail_standard_switch;
        Direction facing = blockEntity.getBlockState().getValue(MultiblockCoreBlock.FACING);
        Matrix4f local =
                new Matrix4f()
                        .translate(
                                (float) (.5F + shiftX(facing)), 0F, (float) (.5F + shiftZ(facing)))
                        .rotateY(yawTurned(facing) * Mth.DEG_TO_RAD);
        bounds =
                LightBounds.of(source, "SignStraight", local, pos)
                        .minmax(LightBounds.of(source, "SignTurn", local, pos));
        world =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(local);
        MeshPart[] parts = SIGNS[flipped ? 1 : 0];
        signs = new TransformedInstance[parts.length];
        for (int i = 0; i < signs.length; i++)
            signs[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, parts[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] signs(HFRWavefrontObject source, Identifier texture) {
        var material = MeshPart.litCutout(texture);
        return new MeshPart[] {
            MeshPart.obj(source.groups[source.partId("SignStraight")], false, material),
            MeshPart.obj(source.groups[source.partId("SignTurn")], false, material)
        };
    }

    private static double shiftX(Direction facing) {
        return facing.getClockWise().getStepX() * .5D;
    }

    private static double shiftZ(Direction facing) {
        return facing.getClockWise().getStepZ() * .5D;
    }

    private static float yawTurned(Direction facing) {
        return switch (facing) {
            case NORTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int selected = blockEntity.isSwitched ? 1 : 0;
        if (selected == lastSelected) return;
        lastSelected = selected;
        for (int i = 0; i < signs.length; i++) {
            signs[i].setVisible(i == selected);
            if (i == selected) signs[i].setTransform(world).light(0).setChanged();
        }
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var sign : signs) consumer.accept(sign);
    }

    @Override
    protected void _delete() {
        for (var sign : signs) sign.delete();
    }
}
