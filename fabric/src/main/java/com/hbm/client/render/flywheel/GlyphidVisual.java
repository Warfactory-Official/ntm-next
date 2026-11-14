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

import com.hbm.client.VanishedEntities;
import com.hbm.client.render.GlyphidAnimationData;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GlyphidVisual extends HbmDynamicEntityVisual<GlyphidEntity> {
    private static final int ROOT = 1;
    private static final int JOINTS = 2;
    private static final int LIGHT = 4;
    private static final int DAMAGE = 8;
    private static final int ALPHA = 16;
    private static final int LIT_FIELDS = ROOT | JOINTS | LIGHT | DAMAGE;
    private static final int FLASH_FIELDS = ROOT | JOINTS | ALPHA;
    private static final int ALL_FIELDS = LIT_FIELDS | FLASH_FIELDS;

    private static final int[] PART_GROUPS = new int[GlyphidAnimationData.PART_COUNT];
    private static final PackedQuadMesh[] PART_MESHES = prepareMeshes();
    private static final int[] ANGLE_INDICES = prepareAngleIndices();
    private static final int[] ARMOR_MASKS = prepareArmorMasks();

    private static final Material OVERLAY_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.glyphid_infestation_tex)
                    .mipmap(false)
                    .backfaceCulling(false)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .build();
    private static final Model[] INFECTION_MODELS = buildSkin(OVERLAY_MATERIAL);

    static final Model[] GLYPHID = buildSkin(cutout(ResourceManager.glyphid_tex));
    static final Model[] GLYPHID_SCOUT = buildSkin(cutout(ResourceManager.glyphid_scout_tex));
    static final Model[] GLYPHID_BLASTER = buildSkin(cutout(ResourceManager.glyphid_blaster_tex));
    static final Model[] GLYPHID_BOMBARDIER =
            buildSkin(cutout(ResourceManager.glyphid_bombardier_tex));
    static final Model[] GLYPHID_BRAWLER = buildSkin(cutout(ResourceManager.glyphid_brawler_tex));
    static final Model[] GLYPHID_BRENDA = buildSkin(cutout(ResourceManager.glyphid_brenda_tex));
    static final Model[] GLYPHID_DIGGER = buildSkin(cutout(ResourceManager.glyphid_digger_tex));
    static final Model[] GLYPHID_BEHEMOTH = buildSkin(cutout(ResourceManager.glyphid_behemoth_tex));

    private final boolean nuclear;
    private final GlyphidInstance[] base;
    private final GlyphidInstance[] layers;
    private final boolean[] baseVisible;
    private final boolean[] layerVisible;
    private final float[] bank = new float[GlyphidAnimationData.BANK_SIZE];
    private final float[] consumed;
    private final Matrix4f root = new Matrix4f();
    private final Vector3f position = new Vector3f();
    private final float[] rootValues = new float[16];

    private boolean initialized;
    private int walkingBits;
    private int swingBits;
    private int positionXBits;
    private int positionYBits;
    private int positionZBits;
    private int yawBits;
    private int scaleBits;
    private int deathAgeBits;
    private int lastPackedLight;
    private int lastDamageOverlay;
    private int lastFlashAlpha;

    private static PackedQuadMesh[] prepareMeshes() {
        var mesh = ResourceManager.glyphid;
        var parts = new PackedQuadMesh[mesh.groups.length];
        for (int part = 0; part < PART_GROUPS.length; part++) {
            int group = mesh.partId(GlyphidAnimationData.group(part));
            PART_GROUPS[part] = group;
            if (parts[group] == null) {
                parts[group] = PackedQuadMesh.of(mesh, group);
            }
        }
        return parts;
    }

    private static int[] prepareAngleIndices() {
        var indices =
                new int
                        [GlyphidAnimationData.TOTAL_PART_COUNT
                                * GlyphidAnimationData.ANGLES_PER_PART];
        for (int part = 0; part < GlyphidAnimationData.TOTAL_PART_COUNT; part++) {
            int offset = part * GlyphidAnimationData.ANGLES_PER_PART;
            for (int component = 0; component < GlyphidAnimationData.ANGLES_PER_PART; component++) {
                indices[offset + component] = GlyphidAnimationData.angleIndex(part, component);
            }
        }
        return indices;
    }

    private static int[] prepareArmorMasks() {
        var masks = new int[GlyphidAnimationData.TOTAL_PART_COUNT];
        for (int part = 0; part < masks.length; part++) {
            int bit = GlyphidAnimationData.armorBit(part);
            masks[part] = bit < 0 ? 0 : 1 << bit;
        }
        return masks;
    }

    static Material cutout(Identifier texture) {
        return SimpleMaterial.builder()
                .texture(texture)
                .mipmap(false)
                .cutout(CutoutShaders.ONE_TENTH)
                .backfaceCulling(false)
                .build();
    }

    static Model[] buildSkin(Material material) {
        var models = new Model[PART_MESHES.length];
        for (int group : PART_GROUPS) {
            if (models[group] == null) {
                models[group] =
                        new SimpleModel(
                                List.of(new Model.ConfiguredMesh(material, PART_MESHES[group])));
            }
        }
        return models;
    }

    public static void initModels() {}

    public GlyphidVisual(
            VisualizationContext context,
            GlyphidEntity glyphid,
            float partialTick,
            Model[] models) {
        this(context, glyphid, partialTick, models, INFECTION_MODELS, false);
    }

    protected GlyphidVisual(
            VisualizationContext context,
            GlyphidEntity glyphid,
            float partialTick,
            Model[] models,
            Model[] layerModels,
            boolean nuclear) {
        super(context, glyphid, partialTick);
        this.nuclear = nuclear;
        int count =
                nuclear ? GlyphidAnimationData.TOTAL_PART_COUNT : GlyphidAnimationData.PART_COUNT;
        base = createInstances(models, count);
        layers = createInstances(layerModels, count);
        baseVisible = new boolean[count];
        layerVisible = new boolean[count];
        consumed = new float[count * GlyphidAnimationData.ANGLES_PER_PART];
        if (!VanishedEntities.isVanished(glyphid)) {
            refresh(partialTick);
        }
    }

    private GlyphidInstance[] createInstances(Model[] models, int count) {
        var instances = new GlyphidInstance[count];
        for (int part = 0; part < count; part++) {
            int group =
                    part == GlyphidAnimationData.PAYLOAD_PART
                            ? models.length - 1
                            : PART_GROUPS[part];
            var instance =
                    instancerProvider()
                            .instancer(GlyphidInstance.TYPE, models[group])
                            .createInstance();
            instance.part(part);
            instance.joints(0.0F, 0.0F, 0.0F);
            instance.setVisible(false);
            instances[part] = instance;
        }
        return instances;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        if (VanishedEntities.isVanished(entity)) {
            hideAll();
            return false;
        }
        return super.isVisible(frustum);
    }

    @Override
    protected void frame(DynamicVisual.Context context) {
        refresh(context.partialTick());
    }

    protected float deathAge(float partialTick) {
        return 0.0F;
    }

    private void refresh(float partialTick) {
        float walking = entity.walkAnimation.position(partialTick);
        float swing = ((LivingEntity) entity).getSwingAnimation(partialTick);
        int nextWalkingBits = Float.floatToRawIntBits(walking);
        int nextSwingBits = Float.floatToRawIntBits(swing);
        if (!initialized || nextWalkingBits != walkingBits) {
            GlyphidAnimationData.evaluateWalking(walking, bank);
            walkingBits = nextWalkingBits;
        }
        if (!initialized || nextSwingBits != swingBits) {
            GlyphidAnimationData.evaluateSwing(swing, bank);
            swingBits = nextSwingBits;
        }

        float age = deathAge(partialTick);
        int nextDeathAgeBits = Float.floatToRawIntBits(age);
        int alpha = 255;
        if (nuclear) {
            alpha = lastFlashAlpha;
            if (!initialized || nextDeathAgeBits != deathAgeBits) {
                alpha = GlyphidAnimationData.nuclearFlashAlpha(age);
            }
        }
        boolean rootChanged = prepareRoot(partialTick, age);
        int packedLight = computePackedLight(partialTick);
        int damageOverlay = ObjEntityVisual.damageOverlay(entity);
        int changes = 0;
        if (rootChanged) {
            changes |= ROOT;
        }
        if (!initialized || packedLight != lastPackedLight) {
            changes |= LIGHT;
        }
        if (!initialized || damageOverlay != lastDamageOverlay) {
            changes |= DAMAGE;
        }
        if (!initialized || alpha != lastFlashAlpha) {
            changes |= ALPHA;
        }

        byte armor = entity.armor();
        boolean showLayers = nuclear ? alpha > 0 : entity.subtype() == GlyphidEntity.TYPE_INFECTED;
        for (int part = 0; part < base.length; part++) {
            int offset = part * GlyphidAnimationData.ANGLES_PER_PART;
            float first = angle(ANGLE_INDICES[offset]);
            float second = angle(ANGLE_INDICES[offset + 1]);
            float third = angle(ANGLE_INDICES[offset + 2]);
            boolean jointsChanged =
                    !initialized
                            || Float.floatToRawIntBits(first)
                                    != Float.floatToRawIntBits(consumed[offset])
                            || Float.floatToRawIntBits(second)
                                    != Float.floatToRawIntBits(consumed[offset + 1])
                            || Float.floatToRawIntBits(third)
                                    != Float.floatToRawIntBits(consumed[offset + 2]);
            int partChanges = changes;
            if (jointsChanged) {
                consumed[offset] = first;
                consumed[offset + 1] = second;
                consumed[offset + 2] = third;
                partChanges |= JOINTS;
            }

            boolean drawn = ARMOR_MASKS[part] == 0 || (armor & ARMOR_MASKS[part]) != 0;
            boolean hadBase = baseVisible[part];
            if (drawn != hadBase) {
                base[part].setVisible(drawn);
                baseVisible[part] = drawn;
            }
            if (drawn) {
                writeInstance(
                        base[part],
                        hadBase ? partChanges : ALL_FIELDS,
                        first,
                        second,
                        third,
                        packedLight,
                        damageOverlay,
                        alpha,
                        false);
            }

            boolean showLayer = drawn && showLayers;
            boolean hadLayer = layerVisible[part];
            if (showLayer != hadLayer) {
                layers[part].setVisible(showLayer);
                layerVisible[part] = showLayer;
            }
            if (showLayer) {
                writeInstance(
                        layers[part],
                        hadLayer ? partChanges : ALL_FIELDS,
                        first,
                        second,
                        third,
                        packedLight,
                        damageOverlay,
                        alpha,
                        nuclear);
            }
        }
        lastPackedLight = packedLight;
        lastDamageOverlay = damageOverlay;
        lastFlashAlpha = alpha;
        deathAgeBits = nextDeathAgeBits;
        initialized = true;
    }

    private float angle(int index) {
        return index < 0 ? 0.0F : bank[index];
    }

    private boolean prepareRoot(float partialTick, float age) {
        getVisualPosition(partialTick, position);
        float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double entityScale = entity.getGlyphidScale();
        int nextXBits = Float.floatToRawIntBits(position.x);
        int nextYBits = Float.floatToRawIntBits(position.y);
        int nextZBits = Float.floatToRawIntBits(position.z);
        int nextYawBits = Float.floatToRawIntBits(yaw);
        int nextScaleBits = Float.floatToRawIntBits((float) entityScale);
        int nextAgeBits = Float.floatToRawIntBits(age);
        if (initialized
                && nextXBits == positionXBits
                && nextYBits == positionYBits
                && nextZBits == positionZBits
                && nextYawBits == yawBits
                && nextScaleBits == scaleBits
                && nextAgeBits == deathAgeBits) {
            return false;
        }

        positionXBits = nextXBits;
        positionYBits = nextYBits;
        positionZBits = nextZBits;
        yawBits = nextYawBits;
        scaleBits = nextScaleBits;
        root.translation(position);
        root.rotateY((float) Math.toRadians(-yaw));
        if (nuclear) {
            GlyphidAnimationData.appendNuclearScale(root, age, entityScale);
        } else {
            root.scale((float) entityScale);
        }
        return rememberRoot();
    }

    private boolean rememberRoot() {
        boolean changed = false;
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                int index = column * 4 + row;
                float value = root.get(column, row);
                if (Float.floatToRawIntBits(value) != Float.floatToRawIntBits(rootValues[index])) {
                    changed = true;
                }
                rootValues[index] = value;
            }
        }
        return changed;
    }

    private void writeInstance(
            GlyphidInstance instance,
            int changes,
            float first,
            float second,
            float third,
            int packedLight,
            int damageOverlay,
            int alpha,
            boolean flash) {
        int fields = changes & (flash ? FLASH_FIELDS : LIT_FIELDS);
        if (fields == 0) {
            return;
        }
        if ((fields & ROOT) != 0) {
            instance.setTransform(root);
        }
        if ((fields & JOINTS) != 0) {
            instance.joints(first, second, third);
        }
        if ((fields & LIGHT) != 0) {
            instance.light(packedLight);
        }
        if ((fields & DAMAGE) != 0) {
            instance.overlay(damageOverlay);
        }
        if ((fields & ALPHA) != 0) {
            instance.colorArgb((alpha << 24) | 0x00FFFFFF);
        }
        instance.setChanged();
    }

    private void hideAll() {
        for (int part = 0; part < base.length; part++) {
            if (baseVisible[part]) {
                base[part].setVisible(false);
                baseVisible[part] = false;
            }
            if (layerVisible[part]) {
                layers[part].setVisible(false);
                layerVisible[part] = false;
            }
        }
    }

    @Override
    protected void _delete() {
        for (var instance : base) {
            instance.delete();
        }
        for (var instance : layers) {
            instance.delete();
        }
    }
}
