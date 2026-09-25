// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import com.hbm.client.model.Meshes;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class MissilePronter {

    private final Supplier<Table> table = Suppliers.memoize(MissilePronter::load);

    public void pront(
            MissileStruct missile, PoseStack pose, SubmitNodeCollector collector, int light) {
        visit(
                missile,
                (mesh, skin, y) -> {
                    pose.pushPose();
                    pose.translate(0F, y, 0F);
                    collector.submitCustomGeometry(
                            pose,
                            RenderTypes.entityCutoutCull(skin),
                            (p, buffer) -> mesh.render(p, buffer, light, -1));
                    pose.popPose();
                });
    }

    public void visit(MissileStruct missile, PartVisitor visitor) {
        Map<Item, Drawn> parts = table.get().parts();
        float y = 0F;

        ItemCustomMissilePart thruster = missile.thruster();
        if (thruster != null && thruster.type == PartType.THRUSTER) {
            parts.get(thruster).visit(visitor, y);
            y += thruster.height;
        }

        ItemCustomMissilePart fuselage = missile.fuselage();
        if (fuselage != null && fuselage.type == PartType.FUSELAGE) {

            ItemCustomMissilePart fins = missile.fins();
            if (fins != null && fins.type == PartType.FINS) parts.get(fins).visit(visitor, y);
            parts.get(fuselage).visit(visitor, y);
            y += fuselage.height;
        }

        ItemCustomMissilePart warhead = missile.warhead();
        if (warhead != null && warhead.type == PartType.WARHEAD)
            parts.get(warhead).visit(visitor, y);
    }

    public float[] bounds() {
        return table.get().bounds();
    }

    private static Table load() {
        Map<Identifier, HFRWavefrontObject> meshes = new HashMap<>();
        Map<Item, Drawn> parts = new HashMap<>();
        float[] box = null;
        float thruster = 0F;
        float fuselage = 0F;

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemCustomMissilePart part) || part.mesh == null) continue;
            HFRWavefrontObject mesh = meshes.computeIfAbsent(part.mesh, Meshes::faceNormals);
            parts.put(item, new Drawn(mesh, Objects.requireNonNull(part.skin)));
            box = union(box, mesh.getExtents());
            if (part.type == PartType.THRUSTER) thruster = Math.max(thruster, part.height);
            if (part.type == PartType.FUSELAGE) fuselage = Math.max(fuselage, part.height);
        }

        box[4] += thruster + fuselage;
        return new Table(Map.copyOf(parts), box);
    }

    private static float[] union(float @Nullable [] box, float[] add) {
        if (box == null) return add.clone();
        for (int axis = 0; axis < 3; axis++) {
            box[axis] = Math.min(box[axis], add[axis]);
            box[axis + 3] = Math.max(box[axis + 3], add[axis + 3]);
        }
        return box;
    }

    private record Drawn(HFRWavefrontObject mesh, Identifier skin) {
        void visit(PartVisitor visitor, float y) {
            visitor.accept(mesh, skin, y);
        }
    }

    @FunctionalInterface
    public interface PartVisitor {
        void accept(HFRWavefrontObject mesh, Identifier skin, float y);
    }

    private record Table(Map<Item, Drawn> parts, float[] bounds) {}
}
