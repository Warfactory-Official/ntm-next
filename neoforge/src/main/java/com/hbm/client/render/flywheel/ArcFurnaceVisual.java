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

import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.ArcFurnaceAnimationData;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector4fc;

import static com.hbm.client.render.ArcFurnaceAnimationData.*;

public final class ArcFurnaceVisual extends AbstractBoundedBlockEntityVisual<ArcFurnaceBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static volatile PreparedModels preparedModels;

    private final PreparedModels models;
    private final ChainInstance[] parts = new ChainInstance[PART_COUNT];
    private final double[] samples = new double[SCALAR_COUNT];
    private final float[] channels = new float[CHANNEL_COUNT];
    private final float[] publishedChannels = new float[CHANNEL_COUNT];
    private final ArrayList<PourVisual> pours = new ArrayList<>();
    private final Direction facing;
    private final AABB bounds;
    private final float outletX;
    private final float outletY;
    private final float outletZ;
    private boolean initialized;
    private int visibleParts;
    private int lastAmount;
    private int activePours;
    private float sampledPhase;
    private float sampledPartial;
    private float sampledPreviousLift;
    private float sampledCurrentLift;
    private boolean sampledProgressing;

    public static void initModels() {
        var object = ResourceManager.arc_furnace;
        boolean smoothing = object.smoothing();
        var lit = MeshPart.litCutout(ResourceManager.arc_furnace_tex);
        var hot =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(ResourceManager.arc_furnace_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.NONE)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(false)
                        .build();
        var partModels = new Model[PART_COUNT];
        var sweeps = new Vector4fc[PART_COUNT];
        var masks = new int[PART_COUNT];
        var litParts = new int[PART_COUNT];
        int litCount = 0;
        int hotParts = 0;
        for (int part = 0; part < PART_COUNT; part++) {
            boolean isHot = part == HOT_CONTENTS || part >= USED_START && part < CABLE_START;
            var group = object.groups[object.partId(ArcFurnaceAnimationData.group(part))];
            var mesh = MeshPart.obj(group, smoothing, isHot ? hot : lit);
            if (part == HOT_CONTENTS) {
                mesh = FoundryTankVisual.molten(mesh);
            }
            partModels[part] = mesh.model();
            sweeps[part] = ArcFurnaceAnimationData.sweep(part, partModels[part]);
            masks[part] = ArcFurnaceAnimationData.channelMask(part);
            if (isHot) {
                hotParts |= 1 << part;
            } else {
                litParts[litCount++] = part;
            }
        }
        preparedModels =
                new PreparedModels(
                        partModels,
                        ArcFurnaceAnimationData.chains(),
                        sweeps,
                        masks,
                        Arrays.copyOf(litParts, litCount),
                        hotParts);
    }

    public ArcFurnaceVisual(
            VisualizationContext context, ArcFurnaceBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        models = preparedModels;
        facing = MultiblockCoreBlock.coreFacing(blockState);
        var localPose = ArcFurnaceAnimationData.rootPose(new Matrix4f(), facing);
        var visualPose =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);
        for (int part = 0; part < PART_COUNT; part++) {
            var instance =
                    instancerProvider()
                            .instancer(ChainInstance.TYPE, models.parts[part])
                            .createInstance();
            instance.chain(models.chains[part]);
            instance.setTransform(visualPose);
            instance.light((models.hotParts & 1 << part) != 0 ? LightCoordsUtil.FULL_BRIGHT : 0);
            instance.setVisible(false);
            instance.setChanged();
            parts[part] = instance;
        }
        var limits = new double[6];
        LightBounds.resetBounds(limits, ArcFurnaceAnimationData.visualMinimumBounds(pos));
        LightBounds.includeLightBounds(
                limits, LightBounds.of(ResourceManager.arc_furnace, HULL_GROUP, localPose, pos));
        for (int part = 0; part < PART_COUNT; part++) {
            LightBounds.includeLightBounds(limits, models.sweeps[part], localPose, pos);
        }
        bounds = new AABB(limits[0], limits[1], limits[2], limits[3], limits[4], limits[5]);
        outletX = (float) (ROOT_X + facing.getStepX() * OUTLET_REACH);
        outletY = (float) OUTLET_Y;
        outletZ = (float) (ROOT_Z + facing.getStepZ() * OUTLET_REACH);
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateMovingParts(context.partialTick());
    }

    @Override
    public boolean doDistanceLimitThisFrame(DynamicVisual.Context context) {
        return initialized && super.doDistanceLimitThisFrame(context);
    }

    public void updateMovingParts(float partialTick) {
        long gameTick = blockEntity.getLevel().getGameTime();
        float previousLift = blockEntity.prevLid;
        float currentLift = blockEntity.lid;
        boolean progressing = blockEntity.isProgressing;
        float phase = ArcFurnaceAnimationData.phase(gameTick, partialTick);
        boolean evaluate =
                !initialized
                        || different(previousLift, sampledPreviousLift)
                        || different(currentLift, sampledCurrentLift)
                        || progressing != sampledProgressing
                        || progressing && different(phase, sampledPhase)
                        || previousLift != currentLift && different(partialTick, sampledPartial);
        if (evaluate) {
            ArcFurnaceAnimationData.evaluate(
                    gameTick, partialTick, previousLift, currentLift, progressing, samples);
            sampledPhase = phase;
            sampledPartial = partialTick;
            sampledPreviousLift = previousLift;
            sampledCurrentLift = currentLift;
            sampledProgressing = progressing;
        }
        boolean liquidsEmpty = blockEntity.liquids.isEmpty();
        int amount = ArcFurnaceBlockEntity.getStackAmount(blockEntity.liquids);
        if (evaluate || !initialized || amount != lastAmount) {
            ArcFurnaceAnimationData.writeChannels(
                    samples, amount, ArcFurnaceBlockEntity.MAX_LIQUID, channels);
        }
        int changedChannels = 0;
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (!initialized || different(channels[channel], publishedChannels[channel])) {
                changedChannels |= 1 << channel;
                publishedChannels[channel] = channels[channel];
            }
        }
        int nextVisible = 1 << LID;
        if (!liquidsEmpty) {
            nextVisible |= 1 << HOT_CONTENTS;
        } else if (blockEntity.hasMaterial) {
            nextVisible |= 1 << COLD_CONTENTS;
        }
        for (int electrode = 0; electrode < ELECTRODE_COUNT; electrode++) {
            int code = blockEntity.electrodes[electrode];
            if (code != 0) {
                nextVisible |= 1 << (RING_START + electrode);
                nextVisible |= 1 << (CABLE_START + electrode);
            }
            switch (code) {
                case 1 -> nextVisible |= 1 << (FRESH_START + electrode);
                case 2 -> nextVisible |= 1 << (USED_START + electrode);
                case 3 -> nextVisible |= 1 << (SHORT_START + electrode);
                default -> {}
            }
        }
        int changedVisibility = nextVisible ^ visibleParts;
        if (!initialized || changedChannels != 0 || changedVisibility != 0) {
            for (int part = 0; part < PART_COUNT; part++) {
                int bit = 1 << part;
                boolean visible = (nextVisible & bit) != 0;
                boolean visibilityChanged = (changedVisibility & bit) != 0;
                if (visibilityChanged) {
                    parts[part].setVisible(visible);
                }
                int mask = models.channelMasks[part];
                if (visible
                        && mask != 0
                        && (!initialized || visibilityChanged || (changedChannels & mask) != 0)) {
                    parts[part].channels(channels, channels);
                    parts[part].setChanged();
                }
            }
        }
        visibleParts = nextVisible;
        lastAmount = amount;
        updatePours(gameTick, partialTick);
        initialized = true;
    }

    private void updatePours(long gameTick, float partialTick) {
        int active = 0;
        var streams = blockEntity.streams;
        int count = streams.size();
        for (int index = 0; index < count; index++) {
            var stream = streams.get(index);
            float age = (float) (gameTick - stream.birth()) + partialTick;
            if (age < POUR_LIFETIME) {
                if (active == pours.size()) {
                    pours.add(new PourVisual(visualizationContext, level, pos));
                }
                pours.get(active)
                        .update(
                                stream.color(),
                                facing,
                                stream.len(),
                                age,
                                POUR_BASE,
                                POUR_OFFSET,
                                outletX,
                                outletY,
                                outletZ);
                active++;
            }
        }
        for (int index = active; index < activePours; index++) {
            pours.get(index).hide();
        }
        activePours = active;
    }

    private static boolean different(float first, float second) {
        return Float.floatToRawIntBits(first) != Float.floatToRawIntBits(second);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int part : models.litParts) {
            consumer.accept(parts[part]);
        }
    }

    @Override
    protected void _delete() {
        for (var instance : parts) {
            instance.delete();
        }
        for (int index = 0; index < pours.size(); index++) {
            pours.get(index).delete();
        }
    }

    private record PreparedModels(
            Model[] parts,
            ChainInstance.Chain[] chains,
            Vector4fc[] sweeps,
            int[] channelMasks,
            int[] litParts,
            int hotParts) {}
}
