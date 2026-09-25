// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.material;

import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.util.I18nUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import mov.movblock.tenon.strip.api.DropSafe;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class NTMMaterial {

    public static final Codec<NTMMaterial> CODEC =
            Codec.STRING.comapFlatMap(
                    name -> {
                        NTMMaterial material = Mats.matByName.get(name);
                        return material == null
                                ? DataResult.error(() -> "Unknown NTM material: " + name)
                                : DataResult.success(material);
                    },
                    material -> material.tagPath);

    public static final StreamCodec<ByteBuf, NTMMaterial> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    name -> {
                        NTMMaterial material = Mats.matByName.get(name);
                        if (material == null)
                            throw new IllegalArgumentException("Unknown NTM material: " + name);
                        return material;
                    },
                    material -> material.tagPath);

    public final int id;
    public final DictFrame dict;
    public final String tagPath;

    public final Set<MaterialShapes> autogen = new LinkedHashSet<>();
    public final Set<MaterialShapes> tagOnly = new LinkedHashSet<>();
    private final Map<MaterialShapes, String> legacyIds = new HashMap<>();
    private final Map<MaterialShapes, String> legacyArt = new HashMap<>();
    public final Set<MatTraits> traits = new LinkedHashSet<>();
    public SmeltingBehavior smeltable = SmeltingBehavior.NOT_SMELTABLE;
    public int solidColorLight = 0xFF4A00;
    public int solidColorDark = 0x802000;
    public int moltenColor = 0xFF4A00;

    public NTMMaterial smeltsInto;
    public int convIn;
    public int convOut;

    public double radiation;
    public double hot;
    public double blinding;
    public double asbestos;
    public double coal;

    public NTMMaterial(int id, DictFrame dict) {
        this.id = id;
        this.dict = dict;
        this.tagPath = dict.mat();

        this.smeltsInto = this;
        this.convIn = 1;
        this.convOut = 1;

        Mats.matByName.put(dict.mat(), this);
        Mats.orderedList.add(this);
        if (Mats.matById.put(id, this) != null) {
            throw new IllegalStateException("Duplicate material id " + id + " for " + dict.mat());
        }
    }

    public String getUnlocalizedName() {
        return "hbmmat." + tagPath;
    }

    public String getLocalizedName() {
        return I18nUtil.resolveKey(getUnlocalizedName());
    }

    public NTMMaterial setConversion(NTMMaterial mat, int in, int out) {
        this.smeltsInto = mat;
        this.convIn = in;
        this.convOut = out;
        return this;
    }

    public NTMMaterial setAutogen(MaterialShapes... shapes) {
        Collections.addAll(this.autogen, shapes);
        return this;
    }

    public NTMMaterial setTagOnly(MaterialShapes... shapes) {
        Collections.addAll(this.tagOnly, shapes);
        return this;
    }

    public NTMMaterial legacy(MaterialShapes shape, String id) {
        legacyIds.put(shape, id);
        return this;
    }

    public NTMMaterial legacy(MaterialShapes shape, String id, String art) {
        legacyIds.put(shape, id);
        legacyArt.put(shape, art);
        return this;
    }

    public String legacyId(MaterialShapes shape) {
        String id = legacyIds.get(shape);
        if (id == null) {
            throw new IllegalStateException(
                    tagPath
                            + " reaches the "
                            + shape.name()
                            + " roster with no 1.7 item named; either name it or "
                            + "stop declaring the shape - 1.7 mints no "
                            + shape.name()
                            + " for every material");
        }
        return id;
    }

    public String legacyArt(MaterialShapes shape) {
        return legacyArt.getOrDefault(shape, legacyId(shape));
    }

    public NTMMaterial setTraits(MatTraits... traits) {
        Collections.addAll(this.traits, traits);
        return this;
    }

    public NTMMaterial m() {
        this.traits.add(MatTraits.METAL);
        return this;
    }

    public NTMMaterial n() {
        this.traits.add(MatTraits.NONMETAL);
        return this;
    }

    public NTMMaterial rad(double radiation) {
        this.radiation = radiation;
        return this;
    }

    public NTMMaterial hot(double hot) {
        this.hot = hot;
        return this;
    }

    public NTMMaterial blinding(double blinding) {
        this.blinding = blinding;
        return this;
    }

    public NTMMaterial asbestos(double asbestos) {
        this.asbestos = asbestos;
        return this;
    }

    public NTMMaterial coal(double coal) {
        this.coal = coal;
        return this;
    }

    public NTMMaterial smeltable(SmeltingBehavior behavior) {
        this.smeltable = behavior;
        return this;
    }

    public NTMMaterial setSolidColor(int colorLight, int colorDark) {
        this.solidColorLight = colorLight;
        this.solidColorDark = colorDark;
        return this;
    }

    public NTMMaterial setMoltenColor(int color) {
        this.moltenColor = color;
        return this;
    }

    @DropSafe
    public TagKey<Item> tag(MaterialShapes shape) {
        return shape.tagFor(tagPath);
    }

    public enum SmeltingBehavior {
        NOT_SMELTABLE,
        VAPORIZES,
        BREAKS,
        SMELTABLE,
        ADDITIVE
    }

    public enum MatTraits {
        METAL,
        NONMETAL
    }
}
