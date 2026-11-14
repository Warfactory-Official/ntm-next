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

package com.hbm.inventory.material;

import com.hbm.NuclearTech;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class Material {

    private static final int SHAPES = MaterialShape.ALL.size();

    private final int id;
    private final String name;
    private final String namespace;
    private final MaterialTags frame;
    private final SmeltingBehavior behaviour;
    private final int lightColor;
    private final int darkColor;
    private final int moltenColor;
    private final Material smeltsInto;
    private final int conversionIn;
    private final int conversionOut;
    private final Hazards hazards;
    private final String translationKey;
    private final Component displayName;
    private final TagKey<Item>[] tags;
    private final SequencedSet<MaterialShape> autoShapes = new LinkedHashSet<>(4);
    private final SequencedSet<MaterialShape> memberShapes = new LinkedHashSet<>(4);
    private final String[] namedIds = new String[SHAPES];
    private long declared;

    @SuppressWarnings("unchecked")
    private Material(Declaration d) {
        this.id = d.id;
        this.name = d.frame.segment();
        this.namespace = d.namespace;
        this.frame = d.frame;
        this.behaviour = d.behaviour;
        this.lightColor = d.lightColor;
        this.darkColor = d.darkColor;
        this.moltenColor = d.moltenColor;
        this.smeltsInto = d.smeltsInto == null ? this : d.smeltsInto;
        this.conversionIn = d.conversionIn;
        this.conversionOut = d.conversionOut;
        this.hazards = new Hazards(d.radiation, d.hot, d.blinding, d.asbestos, d.coal);
        this.translationKey = "hbmmat." + name;
        this.displayName = Component.translatable(translationKey);
        this.tags = new TagKey[SHAPES];
        for (MaterialShape shape : MaterialShape.ALL) {
            if (shape.hasTags()) tags[shape.ordinal()] = shape.tag(name);
        }
    }

    public static Declaration define(int id, MaterialTags frame) {
        return new Declaration(
                id, frame, SmeltingBehavior.NOT_SMELTABLE, 0xFF4A00, 0x802000, 0xFF4A00);
    }

    public static Declaration define(
            int id,
            MaterialTags frame,
            SmeltingBehavior behaviour,
            int lightColor,
            int darkColor,
            int moltenColor) {
        return new Declaration(id, frame, behaviour, lightColor, darkColor, moltenColor);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String namespace() {
        return namespace;
    }

    public MaterialTags frame() {
        return frame;
    }

    public SmeltingBehavior behaviour() {
        return behaviour;
    }

    public int lightColor() {
        return lightColor;
    }

    public int darkColor() {
        return darkColor;
    }

    public int moltenColor() {
        return moltenColor;
    }

    public Material smeltsInto() {
        return smeltsInto;
    }

    public int conversionIn() {
        return conversionIn;
    }

    public int conversionOut() {
        return conversionOut;
    }

    public int convert(int amount) {
        return (int) ((long) amount * conversionOut / conversionIn);
    }

    public Hazards hazards() {
        return hazards;
    }

    public String translationKey() {
        return translationKey;
    }

    public Component displayName() {
        return displayName;
    }

    public TagKey<Item> tag(MaterialShape shape) {
        return tags[shape.ordinal()];
    }

    public SequencedSet<MaterialShape> autoShapes() {
        return Collections.unmodifiableSequencedSet(autoShapes);
    }

    public SequencedSet<MaterialShape> memberShapes() {
        return Collections.unmodifiableSequencedSet(memberShapes);
    }

    public boolean isAuto(MaterialShape shape) {
        return autoShapes.contains(shape);
    }

    public boolean declares(MaterialShape shape) {
        return (declared & 1L << shape.ordinal()) != 0;
    }

    public @Nullable String namedId(MaterialShape shape) {
        return namedIds[shape.ordinal()];
    }

    public Material addAutoShapes(MaterialShape... shapes) {
        for (MaterialShape shape : shapes) {
            autoShapes.add(shape);
            declared |= 1L << shape.ordinal();
        }
        return this;
    }

    public Material addMemberShapes(MaterialShape... shapes) {
        for (MaterialShape shape : shapes) {
            memberShapes.add(shape);
            declared |= 1L << shape.ordinal();
        }
        return this;
    }

    public Material addNamedId(MaterialShape shape, String id) {
        namedIds[shape.ordinal()] = id;
        return this;
    }

    public Material addNamedId(MaterialShape shape, String id, String art) {
        addNamedId(shape, id);

        return this;
    }

    @Override
    public String toString() {
        return name;
    }

    public record Hazards(
            double radiation, double hot, double blinding, double asbestos, double coal) {}

    public static final class Declaration {
        private final int id;
        private final MaterialTags frame;
        private final SmeltingBehavior behaviour;
        private final int lightColor;
        private final int darkColor;
        private final int moltenColor;
        private String namespace = NuclearTech.MOD_ID;
        private @Nullable Material anchor;
        private @Nullable Material smeltsInto;
        private int conversionIn = 1;
        private int conversionOut = 1;
        private double radiation;
        private double hot;
        private double blinding;
        private double asbestos;
        private double coal;
        private MaterialShape[] autoShapes = {};
        private MaterialShape[] memberShapes = {};
        private final List<Named> named = new ArrayList<>(0);

        private Declaration(
                int id,
                MaterialTags frame,
                SmeltingBehavior behaviour,
                int lightColor,
                int darkColor,
                int moltenColor) {
            this.id = id;
            this.frame = frame;
            this.behaviour = behaviour;
            this.lightColor = lightColor;
            this.darkColor = darkColor;
            this.moltenColor = moltenColor;
        }

        public Declaration owner(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Declaration after(Material anchor) {
            this.anchor = anchor;
            return this;
        }

        public Declaration convertsInto(Material target, int in, int out) {
            this.smeltsInto = target;
            this.conversionIn = in;
            this.conversionOut = out;
            return this;
        }

        public Declaration radiation(double value) {
            this.radiation = value;
            return this;
        }

        public Declaration hot(double value) {
            this.hot = value;
            return this;
        }

        public Declaration blinding(double value) {
            this.blinding = value;
            return this;
        }

        public Declaration asbestos(double value) {
            this.asbestos = value;
            return this;
        }

        public Declaration coal(double value) {
            this.coal = value;
            return this;
        }

        public Declaration autoShapes(MaterialShape... shapes) {
            this.autoShapes = shapes;
            return this;
        }

        public Declaration memberShapes(MaterialShape... shapes) {
            this.memberShapes = shapes;
            return this;
        }

        public Declaration namedId(MaterialShape shape, String id) {
            named.add(new Named(shape, id, null));
            return this;
        }

        public Declaration namedId(MaterialShape shape, String id, String art) {
            named.add(new Named(shape, id, art));
            return this;
        }

        public Material register() {
            Material material = new Material(this);
            MaterialRegistry.add(material, anchor);
            material.addAutoShapes(autoShapes).addMemberShapes(memberShapes);
            for (Named n : named) {
                if (n.art == null) material.addNamedId(n.shape, n.id);
                else material.addNamedId(n.shape, n.id, n.art);
            }
            return material;
        }

        private record Named(MaterialShape shape, String id, @Nullable String art) {}
    }
}
