// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import static com.hbm.inventory.material.Mats.*;
import static com.hbm.items.special.ItemBedrockOreNew.ProcessingTrait.*;

public class ItemBedrockOreNew extends Item {

    public static final int none = 0xFFFFFF;
    public static final int roasted = 0xCFCFCF;
    public static final int arc = 0xC3A2A2;
    public static final int washed = 0xDBE2CB;

    public ItemBedrockOreNew(Properties properties) {
        super(properties);
    }

    public static BedrockOreOutput o(NTMMaterial mat, int amount) {
        return new BedrockOreOutput(mat, amount);
    }

    public static MaterialStack toFluid(BedrockOreOutput o, double amount) {
        if (o.mat != null && o.mat.smeltable == SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(
                    o.mat, (int) Math.ceil(MaterialShapes.FRAGMENT.q(o.amount) * amount));
        }
        return null;
    }

    public static ItemStack extract(BedrockOreOutput o, double amount) {
        MaterialShapeItem fragment = Autogen.find(MaterialShapes.FRAGMENT, o.mat);
        return new ItemStack(
                fragment,
                Math.min((int) Math.ceil(o.amount * amount), fragment.getDefaultMaxStackSize()));
    }

    public static ItemStack make(BedrockOreGrade grade, BedrockOreType type) {
        return make(grade, type, 1);
    }

    public static ItemStack make(BedrockOreGrade grade, BedrockOreType type, int amount) {
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
        Component type = Component.translatable(getDescriptionId() + ".type." + ore.type().suffix);
        return Component.translatable(
                getDescriptionId() + ".grade." + ore.grade().name().toLowerCase(Locale.US), type);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (ProcessingTrait trait : ore(stack).grade().traits) {
            adder.accept(
                    Component.translatable(
                            getDescriptionId() + ".trait." + trait.name().toLowerCase(Locale.US)));
        }
    }

    public record Ore(BedrockOreGrade grade, BedrockOreType type) {

        public static final Ore BASE = new Ore(BedrockOreGrade.BASE, BedrockOreType.LIGHT_METAL);
        public static final Codec<Ore> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                BedrockOreGrade.CODEC
                                                        .optionalFieldOf(
                                                                "grade", BedrockOreGrade.BASE)
                                                        .forGetter(Ore::grade),
                                                BedrockOreType.CODEC
                                                        .optionalFieldOf(
                                                                "type", BedrockOreType.LIGHT_METAL)
                                                        .forGetter(Ore::type))
                                        .apply(i, Ore::new));
        public static final StreamCodec<ByteBuf, Ore> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.idMapper(
                                i -> BedrockOreGrade.VALUES[i], BedrockOreGrade::ordinal),
                        Ore::grade,
                        ByteBufCodecs.idMapper(
                                i -> BedrockOreType.VALUES[i], BedrockOreType::ordinal),
                        Ore::type,
                        Ore::new);

        public static Ore ofLegacy(int meta) {
            return new Ore(
                    BedrockOreGrade.VALUES[
                            Mth.positiveModulo(meta >> 4, BedrockOreGrade.VALUES.length)],
                    BedrockOreType.VALUES[
                            Mth.positiveModulo(meta & 15, BedrockOreType.VALUES.length)]);
        }
    }

    public enum BedrockOreType implements StringRepresentable {
        LIGHT_METAL(
                0xFFFFFF,
                0x353535,
                "light",
                o(MAT_IRON, 9),
                o(MAT_COPPER, 9),
                o(MAT_TITANIUM, 6),
                o(MAT_BAUXITE, 9),
                o(MAT_CRYOLITE, 3),
                o(MAT_CHLOROCALCITE, 5),
                o(MAT_LITHIUM, 5),
                o(MAT_SODIUM, 3),
                o(MAT_CHLOROCALCITE, 6),
                o(MAT_LITHIUM, 6),
                o(MAT_SODIUM, 6)),
        HEAVY_METAL(
                0x868686,
                0x000000,
                "heavy",
                o(MAT_TUNGSTEN, 9),
                o(MAT_LEAD, 9),
                o(MAT_GOLD, 2),
                o(MAT_GOLD, 2),
                o(MAT_BERYLLIUM, 3),
                o(MAT_TUNGSTEN, 9),
                o(MAT_LEAD, 9),
                o(MAT_GOLD, 5),
                o(MAT_BISMUTH, 2),
                o(MAT_TANTALIUM, 2),
                o(MAT_GOLD, 6)),
        RARE_EARTH(
                0xE6E6B6,
                0x1C1C00,
                "rare",
                o(MAT_COBALT, 5),
                o(MAT_RAREEARTH, 5),
                o(MAT_BORON, 5),
                o(MAT_LANTHANIUM, 3),
                o(MAT_NIOBIUM, 4),
                o(MAT_NEODYMIUM, 3),
                o(MAT_STRONTIUM, 3),
                o(MAT_ZIRCONIUM, 3),
                o(MAT_NIOBIUM, 5),
                o(MAT_NEODYMIUM, 5),
                o(MAT_STRONTIUM, 3)),
        ACTINIDE(
                0xC1C7BD,
                0x2B3227,
                "actinide",
                o(MAT_URANIUM, 4),
                o(MAT_THORIUM, 4),
                o(MAT_RADIUM, 2),
                o(MAT_RADIUM, 2),
                o(MAT_POLONIUM, 2),
                o(MAT_RADIUM, 2),
                o(MAT_RADIUM, 2),
                o(MAT_POLONIUM, 2),
                o(MAT_TECHNETIUM, 1),
                o(MAT_TECHNETIUM, 1),
                o(MAT_U238, 1)),
        NON_METAL(
                0xAFAFAF,
                0x0F0F0F,
                "nonmetal",
                o(MAT_COAL, 9),
                o(MAT_SULFUR, 9),
                o(MAT_LIGNITE, 9),
                o(MAT_KNO, 6),
                o(MAT_FLUORITE, 6),
                o(MAT_PHOSPHORUS, 5),
                o(MAT_FLUORITE, 6),
                o(MAT_SULFUR, 6),
                o(MAT_CHLOROCALCITE, 6),
                o(MAT_SILICON, 2),
                o(MAT_SILICON, 2)),
        CRYSTALLINE(
                0xE2FFFA,
                0x1E8A77,
                "crystal",
                o(MAT_REDSTONE, 9),
                o(MAT_CINNABAR, 4),
                o(MAT_SODALITE, 9),
                o(MAT_ASBESTOS, 6),
                o(MAT_DIAMOND, 3),
                o(MAT_CINNABAR, 3),
                o(MAT_ASBESTOS, 5),
                o(MAT_EMERALD, 3),
                o(MAT_BORAX, 3),
                o(MAT_MOLYSITE, 3),
                o(MAT_SODALITE, 9));

        public static final BedrockOreType[] VALUES = values();
        public static final Codec<BedrockOreType> CODEC =
                StringRepresentable.fromEnum(BedrockOreType::values);

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
        public NTMMaterial mat;
        public int amount;

        public BedrockOreOutput(NTMMaterial mat, int amount) {
            this.mat = mat;
            this.amount = amount;
        }
    }
}
