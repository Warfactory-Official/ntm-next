/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.special;

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.Materials;
import com.hbm.inventory.material.SmeltingBehavior;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.MaterialShapeItem;
import com.hbm.registration.OpenRegistry;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import static com.hbm.items.special.BedrockOreNewItem.ProcessingTrait.*;

public class BedrockOreNewItem extends Item {

    public static final int none = 0xFFFFFF;
    public static final int roasted = 0xCFCFCF;
    public static final int arc = 0xC3A2A2;
    public static final int washed = 0xDBE2CB;

    public BedrockOreNewItem(Properties properties) {
        super(properties);
    }

    public static BedrockOreOutput o(Material mat, int amount) {
        return new BedrockOreOutput(mat, amount);
    }

    public static MaterialStack toFluid(BedrockOreOutput o, double amount) {
        if (o.mat != null && o.mat.behaviour() == SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(
                    o.mat, (int) Math.ceil(MaterialShape.FRAGMENT.amount(o.amount) * amount));
        }
        return null;
    }

    public static ItemStack extract(BedrockOreOutput o, double amount) {
        MaterialShapeItem fragment = Autogen.find(MaterialShape.FRAGMENT, o.mat);
        return new ItemStack(
                fragment,
                Math.min((int) Math.ceil(o.amount * amount), fragment.getDefaultMaxStackSize()));
    }

    public static ItemStack make(BedrockOreGrade grade, BedrockOreKind type) {
        return make(grade, type, 1);
    }

    public static ItemStack make(BedrockOreGrade grade, BedrockOreKind type, int amount) {
        ItemStack stack = new ItemStack(ModItems.BEDROCK_ORE.get(), amount);
        stack.set(ModDataComponents.BEDROCK_ORE.get(), new Ore(grade, type));
        return stack;
    }

    public static Ore ore(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BEDROCK_ORE.get(), Ore.BASE);
    }

    @Override
    public Component getName(ItemStack stack) {
        Ore ore = ore(stack);
        MutableComponent name =
                Component.translatable(
                        getDescriptionId()
                                + ".grade."
                                + ore.grade().name().toLowerCase(Locale.ROOT),
                        ore.type().typeName());
        Component suffix = ore.type().nameSuffix();
        return suffix == null ? name : name.append(suffix);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Ore ore = ore(stack);
        for (ProcessingTrait trait : ore.grade().traits) {
            adder.accept(
                    Component.translatable(
                            getDescriptionId()
                                    + ".trait."
                                    + trait.name().toLowerCase(Locale.ROOT)));
        }
        ore.type().appendHoverText(ore.grade(), adder);
    }

    public record Ore(BedrockOreGrade grade, BedrockOreKind type) {

        public static final Ore BASE = new Ore(BedrockOreGrade.BASE, BedrockOreType.LIGHT_METAL);
        public static final Codec<Ore> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                BedrockOreGrade.CODEC
                                                        .optionalFieldOf(
                                                                "grade", BedrockOreGrade.BASE)
                                                        .forGetter(Ore::grade),
                                                BedrockOreType.KINDS
                                                        .codec()
                                                        .optionalFieldOf(
                                                                "type", BedrockOreType.LIGHT_METAL)
                                                        .forGetter(Ore::type))
                                        .apply(i, Ore::new));
        public static final StreamCodec<ByteBuf, Ore> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.idMapper(
                                i -> BedrockOreGrade.VALUES[i], BedrockOreGrade::ordinal),
                        Ore::grade,
                        BedrockOreType.KINDS.streamCodec(),
                        Ore::type,
                        Ore::new);

        public static Ore ofLegacy(int meta) {
            return new Ore(BedrockOreGrade.VALUES[meta >> 4], BedrockOreType.VALUES[meta & 15]);
        }
    }

    public enum BedrockOreType implements BedrockOreKind, StringRepresentable {
        LIGHT_METAL(
                0xFFFFFF,
                0x353535,
                "light",
                o(Materials.IRON, 9),
                o(Materials.COPPER, 9),
                o(Materials.TITANIUM, 6),
                o(Materials.BAUXITE, 9),
                o(Materials.CRYOLITE, 3),
                o(Materials.CHLOROCALCITE, 5),
                o(Materials.LITHIUM, 5),
                o(Materials.SODIUM, 3),
                o(Materials.CHLOROCALCITE, 6),
                o(Materials.LITHIUM, 6),
                o(Materials.SODIUM, 6)),
        HEAVY_METAL(
                0x868686,
                0x000000,
                "heavy",
                o(Materials.TUNGSTEN, 9),
                o(Materials.LEAD, 9),
                o(Materials.GOLD, 2),
                o(Materials.GOLD, 2),
                o(Materials.BERYLLIUM, 3),
                o(Materials.TUNGSTEN, 9),
                o(Materials.LEAD, 9),
                o(Materials.GOLD, 5),
                o(Materials.BISMUTH, 2),
                o(Materials.TANTALUM, 2),
                o(Materials.GOLD, 6)),
        RARE_EARTH(
                0xE6E6B6,
                0x1C1C00,
                "rare",
                o(Materials.COBALT, 5),
                o(Materials.RARE_EARTH, 5),
                o(Materials.BORON, 5),
                o(Materials.LANTHANUM, 3),
                o(Materials.NIOBIUM, 4),
                o(Materials.NEODYMIUM, 3),
                o(Materials.STRONTIUM, 3),
                o(Materials.ZIRCONIUM, 3),
                o(Materials.NIOBIUM, 5),
                o(Materials.NEODYMIUM, 5),
                o(Materials.STRONTIUM, 3)),
        ACTINIDE(
                0xC1C7BD,
                0x2B3227,
                "actinide",
                o(Materials.URANIUM, 4),
                o(Materials.THORIUM, 4),
                o(Materials.RADIUM_226, 2),
                o(Materials.RADIUM_226, 2),
                o(Materials.POLONIUM_210, 2),
                o(Materials.RADIUM_226, 2),
                o(Materials.RADIUM_226, 2),
                o(Materials.POLONIUM_210, 2),
                o(Materials.TECHNETIUM_99, 1),
                o(Materials.TECHNETIUM_99, 1),
                o(Materials.URANIUM_238, 1)),
        NON_METAL(
                0xAFAFAF,
                0x0F0F0F,
                "nonmetal",
                o(Materials.COAL, 9),
                o(Materials.SULFUR, 9),
                o(Materials.LIGNITE, 9),
                o(Materials.SALTPETER, 6),
                o(Materials.FLUORITE, 6),
                o(Materials.RED_PHOSPHORUS, 5),
                o(Materials.FLUORITE, 6),
                o(Materials.SULFUR, 6),
                o(Materials.CHLOROCALCITE, 6),
                o(Materials.SILICON, 2),
                o(Materials.SILICON, 2)),
        CRYSTALLINE(
                0xE2FFFA,
                0x1E8A77,
                "crystal",
                o(Materials.REDSTONE, 9),
                o(Materials.CINNABAR, 4),
                o(Materials.SODALITE, 9),
                o(Materials.ASBESTOS, 6),
                o(Materials.DIAMOND, 3),
                o(Materials.CINNABAR, 3),
                o(Materials.ASBESTOS, 5),
                o(Materials.EMERALD, 3),
                o(Materials.BORAX, 3),
                o(Materials.MOLYSITE, 3),
                o(Materials.SODALITE, 9));

        public static final BedrockOreType[] VALUES = values();
        public static final OpenRegistry<BedrockOreKind> KINDS =
                new OpenRegistry<>("bedrock ore type", BedrockOreKind::id);

        static {
            for (BedrockOreType type : VALUES) KINDS.add(type);
        }

        public final int light;
        public final int dark;
        public final String suffix;
        public final BedrockOreOutput primary1;
        public final BedrockOreOutput primary2;
        public final BedrockOreOutput byproductAcid1;
        public final BedrockOreOutput byproductAcid2;
        public final BedrockOreOutput byproductAcid3;
        public final BedrockOreOutput byproductSolvent1;
        public final BedrockOreOutput byproductSolvent2;
        public final BedrockOreOutput byproductSolvent3;
        public final BedrockOreOutput byproductRad1;
        public final BedrockOreOutput byproductRad2;
        public final BedrockOreOutput byproductRad3;

        BedrockOreType(
                int light,
                int dark,
                String suffix,
                BedrockOreOutput p1,
                BedrockOreOutput p2,
                BedrockOreOutput bA1,
                BedrockOreOutput bA2,
                BedrockOreOutput bA3,
                BedrockOreOutput bS1,
                BedrockOreOutput bS2,
                BedrockOreOutput bS3,
                BedrockOreOutput bR1,
                BedrockOreOutput bR2,
                BedrockOreOutput bR3) {
            this.light = light;
            this.dark = dark;
            this.suffix = suffix;
            this.primary1 = p1;
            this.primary2 = p2;
            this.byproductAcid1 = bA1;
            this.byproductAcid2 = bA2;
            this.byproductAcid3 = bA3;
            this.byproductSolvent1 = bS1;
            this.byproductSolvent2 = bS2;
            this.byproductSolvent3 = bS3;
            this.byproductRad1 = bR1;
            this.byproductRad2 = bR2;
            this.byproductRad3 = bR3;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String id() {
            return getSerializedName();
        }

        @Override
        public String suffix() {
            return suffix;
        }

        @Override
        public int light() {
            return light;
        }

        @Override
        public int dark() {
            return dark;
        }

        @Override
        public Identifier channel() {
            return RegistryUtil.id("bedrock_ore/" + id());
        }
    }

    public enum ProcessingTrait {
        ROASTED,
        ARC,
        WASHED,
        CENTRIFUGED,
        SULFURIC,
        SOLVENT,
        RAD;

        public static final ProcessingTrait[] VALUES = values();
    }

    public enum BedrockOreGrade implements StringRepresentable {
        BASE(none, "base"),
        BASE_ROASTED(roasted, "base", ROASTED),
        BASE_WASHED(washed, "base", WASHED),
        PRIMARY(none, "primary", CENTRIFUGED),
        PRIMARY_ROASTED(roasted, "primary", ROASTED),
        PRIMARY_SULFURIC(0xFFFFD3, "primary", SULFURIC),
        PRIMARY_NOSULFURIC(0xD3D4FF, "primary", CENTRIFUGED, SULFURIC),
        PRIMARY_SOLVENT(0xD3F0FF, "primary", SOLVENT),
        PRIMARY_NOSOLVENT(0xFFDED3, "primary", CENTRIFUGED, SOLVENT),
        PRIMARY_RAD(0xECFFD3, "primary", RAD),
        PRIMARY_NORAD(0xEBD3FF, "primary", CENTRIFUGED, RAD),
        PRIMARY_FIRST(0xFFD3D4, "primary", CENTRIFUGED),
        PRIMARY_SECOND(0xD3FFEB, "primary", CENTRIFUGED),
        CRUMBS(none, "crumbs", CENTRIFUGED),

        SULFURIC_BYPRODUCT(none, "sulfuric", CENTRIFUGED, SULFURIC),
        SULFURIC_ROASTED(roasted, "sulfuric", ROASTED, SULFURIC),
        SULFURIC_ARC(arc, "sulfuric", ARC, SULFURIC),
        SULFURIC_WASHED(washed, "sulfuric", WASHED, SULFURIC),

        SOLVENT_BYPRODUCT(none, "solvent", CENTRIFUGED, SOLVENT),
        SOLVENT_ROASTED(roasted, "solvent", ROASTED, SOLVENT),
        SOLVENT_ARC(arc, "solvent", ARC, SOLVENT),
        SOLVENT_WASHED(washed, "solvent", WASHED, SOLVENT),

        RAD_BYPRODUCT(none, "rad", CENTRIFUGED, RAD),
        RAD_ROASTED(roasted, "rad", ROASTED, RAD),
        RAD_ARC(arc, "rad", ARC, RAD),
        RAD_WASHED(washed, "rad", WASHED, RAD);

        public static final BedrockOreGrade[] VALUES = values();
        public static final Codec<BedrockOreGrade> CODEC =
                StringRepresentable.fromEnum(BedrockOreGrade::values);

        public final int tint;
        public final String prefix;
        public final ProcessingTrait[] traits;

        BedrockOreGrade(int tint, String prefix, ProcessingTrait... traits) {
            this.tint = tint;
            this.prefix = prefix;
            this.traits = traits;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class BedrockOreOutput {
        public Material mat;
        public int amount;

        public BedrockOreOutput(Material mat, int amount) {
            this.mat = mat;
            this.amount = amount;
        }
    }
}
