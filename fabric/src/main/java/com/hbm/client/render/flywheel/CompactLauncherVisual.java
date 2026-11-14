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

import com.hbm.blockentity.ordnance.launch.CompactLauncherBlockEntity;
import com.hbm.client.render.MissilePronter;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.CustomMissilePartItem.PartType;
import com.hbm.items.weapon.CustomMissilePartItem;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractDynamicExtentBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class CompactLauncherVisual
        extends AbstractDynamicExtentBlockEntityVisual<CompactLauncherBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final float MISSILE_Y = 1.0625F;
    private static final int BODY_RADIUS = 2;
    private static final int BODY_HEIGHT = 8;
    private static final Map<PartKey, PartModels> PART_MODELS = new ConcurrentHashMap<>();

    private final Slot[] slots = new Slot[4];
    private final CustomMissilePartItem[] keys = new CustomMissilePartItem[4];
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private @Nullable AABB lastLightBounds;
    private float missileTop = BODY_HEIGHT;
    private MissileStruct lastMissile;

    public CompactLauncherVisual(
            VisualizationContext context,
            CompactLauncherBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static CustomMissilePartItem valid(CustomMissilePartItem part, PartType type) {
        return part != null && part.type == type && part.mesh != null && part.skin != null
                ? part
                : null;
    }

    private static PartModels models(PartKey key) {
        HFRWavefrontObject mesh = MissilePronter.partMesh(key.mesh);
        return new PartModels(
                MeshPart.objParts(mesh, MeshPart.litCutout(key.skin)), mesh.getExtents()[4]);
    }

    @Override
    public void beforeFrame() {
        if (blockEntity.observedAssembly != lastMissile) updateMovingParts(0F);
    }

    @Override
    public void frame(Context context) {}

    public void updateMovingParts(float partialTick) {
        MissileStruct missile = blockEntity.observedAssembly;
        lastMissile = missile;
        boolean changed = sync(0, valid(missile.thruster(), PartType.THRUSTER));
        changed |=
                sync(
                        1,
                        valid(missile.fins(), PartType.FINS) != null
                                        && valid(missile.fuselage(), PartType.FUSELAGE) != null
                                ? missile.fins()
                                : null);
        changed |= sync(2, valid(missile.fuselage(), PartType.FUSELAGE));
        changed |= sync(3, valid(missile.warhead(), PartType.WARHEAD));

        float y = MISSILE_Y;
        pose.identity().translate(.5F, y, .5F);
        write(slots[0], pose);
        float top = slots[0] == null ? 0F : y + slots[0].maxY;
        CustomMissilePartItem thruster = missile.thruster();
        if (valid(thruster, PartType.THRUSTER) != null) y += thruster.height;

        pose.identity().translate(.5F, y, .5F);
        CustomMissilePartItem fuselage = missile.fuselage();
        if (valid(fuselage, PartType.FUSELAGE) != null) {
            write(slots[1], pose);
            write(slots[2], pose);
            if (slots[1] != null) top = Math.max(top, y + slots[1].maxY);
            if (slots[2] != null) top = Math.max(top, y + slots[2].maxY);
            y += fuselage.height;
        }

        pose.identity().translate(.5F, y, .5F);
        write(slots[3], pose);
        if (slots[3] != null) top = Math.max(top, y + slots[3].maxY);
        top = Math.max(BODY_HEIGHT, top);
        if (changed && top != missileTop) {
            missileTop = top;
            lastLightBounds =
                    LightBounds.sections(lightSections, getRenderBoundingBox(), lastLightBounds);
            refreshVisibleBounds();
        }
    }

    private boolean sync(int index, CustomMissilePartItem part) {
        if (keys[index] == part) return false;
        if (slots[index] != null) slots[index].delete();
        keys[index] = part;
        slots[index] = part == null ? null : createSlot(part);
        return true;
    }

    private Slot createSlot(CustomMissilePartItem part) {
        PartModels models =
                PART_MODELS.computeIfAbsent(
                        new PartKey(part.mesh, part.skin), CompactLauncherVisual::models);
        TransformedInstance[] instances = new TransformedInstance[models.parts.length];
        for (int i = 0; i < instances.length; i++) {
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, models.parts[i].model())
                            .createInstance();
        }
        return new Slot(instances, models.maxY);
    }

    private void write(Slot slot, Matrix4f local) {
        if (slot == null) return;
        if (slot.written && slot.local.equals(local)) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        for (TransformedInstance instance : slot.instances)
            instance.setTransform(world).light(0).setChanged();
        slot.local.set(local);
        slot.written = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - BODY_RADIUS,
                        pos.getY(),
                        pos.getZ() - BODY_RADIUS,
                        pos.getX() + BODY_RADIUS + 1,
                        pos.getY() + missileTop,
                        pos.getZ() + BODY_RADIUS + 1)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (Slot slot : slots)
            if (slot != null)
                for (TransformedInstance instance : slot.instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (Slot slot : slots) if (slot != null) slot.delete();
    }

    private record PartKey(Identifier mesh, Identifier skin) {}

    private record PartModels(MeshPart[] parts, float maxY) {}

    private static final class Slot {
        private final TransformedInstance[] instances;
        private final float maxY;
        private final Matrix4f local = new Matrix4f();
        private boolean written;

        private Slot(TransformedInstance[] instances, float maxY) {
            this.instances = instances;
            this.maxY = maxY;
        }

        private void delete() {
            for (TransformedInstance instance : instances) instance.delete();
        }
    }
}
