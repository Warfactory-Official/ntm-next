// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.client.ClientEffects;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.items.special.ItemLootCrate;
import com.hbm.lib.Library;
import com.hbm.registration.OpenRegistry;
import com.hbm.registration.OverridePoint;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemCustomMissilePart extends Item {

    public PartType type;
    public PartSize top;
    public PartSize bottom;
    public Rarity rarity;
    public float health;

    public float height;
    public float guiHeight;
    public @Nullable Identifier mesh;
    public @Nullable Identifier skin;

    private String title;
    private String author;
    private String witty;

    public Object[] attributes;

    public ItemCustomMissilePart(Properties properties) {
        super(properties);
    }

    public enum PartType {
        CHIP,
        WARHEAD,
        FUSELAGE,
        FINS,
        THRUSTER
    }

    public static final class PartSize {

        private static final OpenRegistry<PartSize> REGISTRY =
                new OpenRegistry<>("missile part size", PartSize::name);

        public static final PartSize ANY =
                register("ANY", () -> Component.translatable("item.hbm.missile.part.size.any"));
        public static final PartSize NONE =
                register("NONE", () -> Component.translatable("item.hbm.missile.part.size.none"));
        public static final PartSize SIZE_10 = register("SIZE_10", () -> Component.literal("1.0m"));
        public static final PartSize SIZE_15 = register("SIZE_15", () -> Component.literal("1.5m"));
        public static final PartSize SIZE_20 = register("SIZE_20", () -> Component.literal("2.0m"));

        private final String name;
        private final Supplier<Component> display;

        private PartSize(String name, Supplier<Component> display) {
            this.name = name;
            this.display = display;
        }

        public static PartSize register(String name, Supplier<Component> display) {
            return REGISTRY.add(new PartSize(name, display));
        }

        public static List<PartSize> values() {
            return REGISTRY.values();
        }

        public static PartSize byName(String name, PartSize fallback) {
            return REGISTRY.byName(name, fallback);
        }

        public String name() {
            return name;
        }

        public int ordinal() {
            return REGISTRY.index(this);
        }

        public Component getDisplay() {
            return display.get();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class WarheadType {

        public static final WarheadType HE =
                new WarheadType("HE", () -> key("he", ChatFormatting.YELLOW));
        public static final WarheadType INC =
                new WarheadType("INC", () -> key("incendiary", ChatFormatting.GOLD));
        public static final WarheadType BUSTER =
                new WarheadType("BUSTER", () -> key("bunker_buster", ChatFormatting.WHITE));
        public static final WarheadType CLUSTER =
                new WarheadType("CLUSTER", () -> key("cluster", ChatFormatting.GRAY));
        public static final WarheadType NUCLEAR =
                new WarheadType("NUCLEAR", () -> key("nuclear", ChatFormatting.DARK_GREEN));
        public static final WarheadType TX =
                new WarheadType("TX", () -> key("thermonuclear", ChatFormatting.DARK_PURPLE));
        public static final WarheadType N2 =
                new WarheadType("N2", () -> key("n2", ChatFormatting.RED));
        public static final WarheadType BALEFIRE =
                new WarheadType("BALEFIRE", () -> key("balefire", ChatFormatting.GREEN));
        public static final WarheadType SCHRAB =
                new WarheadType("SCHRAB", () -> key("schrabidium", ChatFormatting.AQUA));
        public static final WarheadType TAINT =
                new WarheadType("TAINT", () -> key("taint", ChatFormatting.DARK_PURPLE));
        public static final WarheadType CLOUD =
                new WarheadType("CLOUD", () -> key("cloud", ChatFormatting.LIGHT_PURPLE));
        public static final WarheadType TURBINE =
                new WarheadType(
                        "TURBINE",
                        () ->
                                key(
                                        "turbine",
                                        System.currentTimeMillis() % 1000 < 500
                                                ? ChatFormatting.RED
                                                : ChatFormatting.LIGHT_PURPLE));

        public static final WarheadType CUSTOM0 = custom("CUSTOM0");
        public static final WarheadType CUSTOM1 = custom("CUSTOM1");
        public static final WarheadType CUSTOM2 = custom("CUSTOM2");
        public static final WarheadType CUSTOM3 = custom("CUSTOM3");
        public static final WarheadType CUSTOM4 = custom("CUSTOM4");
        public static final WarheadType CUSTOM5 = custom("CUSTOM5");
        public static final WarheadType CUSTOM6 = custom("CUSTOM6");
        public static final WarheadType CUSTOM7 = custom("CUSTOM7");
        public static final WarheadType CUSTOM8 = custom("CUSTOM8");
        public static final WarheadType CUSTOM9 = custom("CUSTOM9");

        public Consumer<EntityMissileCustom> impactCustom = null;
        public Consumer<EntityMissileCustom> updateCustom = null;
        public String labelCustom = null;

        private final String name;
        private final Supplier<Component> display;

        public WarheadType(String name, Supplier<Component> display) {
            this.name = name;
            this.display = display;
        }

        private static WarheadType custom(String name) {
            return new WarheadType(
                    name,
                    () -> Component.translatable("general.na").withStyle(ChatFormatting.BOLD));
        }

        public String name() {
            return name;
        }

        public Component getDisplay() {
            if (this.labelCustom != null) return Component.literal(this.labelCustom);
            return display.get();
        }

        private static Component key(String name, ChatFormatting color) {
            return Component.translatable("item.hbm.warhead.desc." + name).withStyle(color);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class FuelType {

        public static final FuelType KEROSENE =
                new FuelType(
                        "KEROSENE",
                        "item.hbm.missile.fuel.kerosene_peroxide",
                        ChatFormatting.LIGHT_PURPLE,
                        ClientEffects.Contrail.KEROSENE);
        public static final FuelType SOLID =
                new FuelType(
                        "SOLID",
                        "item.hbm.missile.fuel.solid",
                        ChatFormatting.GOLD,
                        ClientEffects.Contrail.SOLID);
        public static final FuelType HYDROGEN =
                new FuelType(
                        "HYDROGEN",
                        "item.hbm.missile.fuel.hydrogen",
                        ChatFormatting.DARK_AQUA,
                        ClientEffects.Contrail.HYDROGEN);
        public static final FuelType XENON =
                new FuelType(
                        "XENON", "item.hbm.missile.fuel.xenon", ChatFormatting.DARK_PURPLE, null);
        public static final FuelType BALEFIRE =
                new FuelType(
                        "BALEFIRE",
                        "item.hbm.missile.fuel.balefire",
                        ChatFormatting.GREEN,
                        ClientEffects.Contrail.BALEFIRE);

        private final String name;
        private final String key;
        private final ChatFormatting color;
        public final ClientEffects.@Nullable Contrail contrail;

        public FuelType(
                String name,
                String key,
                ChatFormatting color,
                ClientEffects.@Nullable Contrail contrail) {
            this.name = name;
            this.key = key;
            this.color = color;
            this.contrail = contrail;
        }

        public String name() {
            return name;
        }

        public Component getDisplay() {
            return Component.translatable(key).withStyle(color);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Rarity {
        COMMON("common", ChatFormatting.GRAY),
        UNCOMMON("uncommon", ChatFormatting.YELLOW),
        RARE("rare", ChatFormatting.AQUA),
        EPIC("epic", ChatFormatting.LIGHT_PURPLE),
        LEGENDARY("legendary", ChatFormatting.DARK_GREEN),
        STRANGE("strange", ChatFormatting.DARK_AQUA);

        private final String key;
        private final ChatFormatting color;

        Rarity(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component getDisplay() {
            return Component.translatable("item.hbm.missile.part.rarity." + key).withStyle(color);
        }
    }

    public ItemCustomMissilePart makeChip(float inaccuracy) {
        this.type = PartType.CHIP;
        this.top = PartSize.ANY;
        this.bottom = PartSize.ANY;
        this.attributes = new Object[] {inaccuracy};
        return this;
    }

    public ItemCustomMissilePart makeWarhead(
            WarheadType type, float punch, float weight, PartSize size) {
        this.type = PartType.WARHEAD;
        this.top = PartSize.NONE;
        this.bottom = size;
        this.attributes = new Object[] {type, punch, weight};
        return this;
    }

    public ItemCustomMissilePart makeFuselage(
            FuelType type, float fuel, PartSize top, PartSize bottom) {
        this.type = PartType.FUSELAGE;
        this.top = top;
        this.bottom = bottom;
        this.attributes = new Object[] {type, fuel};
        return this;
    }

    public ItemCustomMissilePart makeStability(float inaccuracy, PartSize size) {
        this.type = PartType.FINS;
        this.top = size;
        this.bottom = size;
        this.attributes = new Object[] {inaccuracy};
        return this;
    }

    public ItemCustomMissilePart makeThruster(
            FuelType type, float consumption, float lift, PartSize size) {
        this.type = PartType.THRUSTER;
        this.top = size;
        this.bottom = PartSize.NONE;
        this.attributes = new Object[] {type, consumption, lift};
        return this;
    }

    public ItemCustomMissilePart setAssembly(
            float height, float guiHeight, Identifier mesh, Identifier skin) {
        this.height = height;
        this.guiHeight = guiHeight;
        this.mesh = mesh;
        this.skin = skin;
        return this;
    }

    public ItemCustomMissilePart setAuthor(String author) {
        this.author = author;
        return this;
    }

    public ItemCustomMissilePart setTitle(String title) {
        this.title = title;
        return this;
    }

    public ItemCustomMissilePart setWittyText(String witty) {
        this.witty = witty;
        return this;
    }

    public ItemCustomMissilePart setHealth(float health) {
        this.health = health;
        return this;
    }

    public ItemCustomMissilePart setRarity(Rarity rarity) {
        this.rarity = rarity;

        if (this.type == PartType.FUSELAGE) {
            if (this.top == PartSize.SIZE_10) ItemLootCrate.LIST_10.add(this);
            if (this.top == PartSize.SIZE_15) ItemLootCrate.LIST_15.add(this);
        } else {
            ItemLootCrate.LIST_MISC.add(this);
        }

        return this;
    }

    public WarheadType warheadType() {
        return this.type == PartType.WARHEAD ? (WarheadType) this.attributes[0] : null;
    }

    public float warheadStrength() {
        return this.type == PartType.WARHEAD ? (Float) this.attributes[1] : 0F;
    }

    public FuelType fuelType() {
        return this.type == PartType.FUSELAGE || this.type == PartType.THRUSTER
                ? (FuelType) this.attributes[0]
                : null;
    }

    public float fuelAmount() {
        return this.type == PartType.FUSELAGE ? (Float) this.attributes[1] : 0F;
    }

    public float consumption() {
        return this.type == PartType.THRUSTER ? (Float) this.attributes[1] : 0F;
    }

    public float inaccuracy() {
        return this.type == PartType.CHIP || this.type == PartType.FINS
                ? (Float) this.attributes[0]
                : 0F;
    }

    public static ItemCustomMissilePart part(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCustomMissilePart part ? part : null;
    }

    public static final OverridePoint<Set<Identifier>> VERTEX_NORMAL_MESHES =
            OverridePoint.of(Library.id("missile_part_vertex_normal_meshes"), Set.of());

    public static final OverridePoint<Describer> DESCRIBE =
            OverridePoint.of(Library.id("missile_part_describe"), ItemCustomMissilePart::describe);

    @FunctionalInterface
    public interface Describer {
        void describe(ItemCustomMissilePart part, Consumer<Component> adder);
    }

    public @Nullable String title() {
        return title;
    }

    public @Nullable String author() {
        return author;
    }

    public @Nullable String witty() {
        return witty;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        DESCRIBE.get().describe(this, adder);
    }

    public void describe(Consumer<Component> adder) {
        if (this.title != null) {
            adder.accept(
                    Component.literal("\"" + this.title + "\"")
                            .withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (this.type != null && this.attributes != null) {
            switch (this.type) {
                case CHIP:
                    adder.accept(stat("inaccuracy", Component.literal(inaccuracy() * 100 + "%")));
                    break;
                case WARHEAD:
                    adder.accept(stat("size", this.bottom.getDisplay()));
                    adder.accept(stat("type", warheadType().getDisplay()));
                    adder.accept(
                            stat("strength", Component.literal(String.valueOf(warheadStrength()))));
                    adder.accept(stat("weight", Component.literal(this.attributes[2] + "t")));
                    break;
                case FUSELAGE:
                    adder.accept(stat("topSize", this.top.getDisplay()));
                    adder.accept(stat("bottomSize", this.bottom.getDisplay()));
                    adder.accept(stat("fuelType", fuelType().getDisplay()));
                    adder.accept(stat("fuelAmount", Component.literal(fuelAmount() + "l")));
                    break;
                case FINS:
                    adder.accept(stat("size", this.top.getDisplay()));
                    adder.accept(stat("inaccuracy", Component.literal(inaccuracy() * 100 + "%")));
                    break;
                case THRUSTER:
                    adder.accept(stat("size", this.top.getDisplay()));
                    adder.accept(stat("fuelType", fuelType().getDisplay()));
                    adder.accept(
                            stat("fuelConsumption", Component.literal(consumption() + "l/tick")));
                    adder.accept(stat("maxPayload", Component.literal(this.attributes[2] + "t")));
                    break;
            }
        }

        if (this.type != PartType.CHIP) {
            adder.accept(stat("health", Component.literal(this.health + "HP")));
        }
        if (this.rarity != null) {
            adder.accept(stat("rarity", this.rarity.getDisplay()));
        }
        if (this.author != null) {
            adder.accept(
                    Component.translatable("item.hbm.missile.part.by")
                            .append(" " + this.author)
                            .withStyle(ChatFormatting.WHITE));
        }
        if (this.witty != null) {
            adder.accept(
                    Component.literal("\"" + this.witty + "\"")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    public static Component stat(String key, Component value) {
        return Component.translatable("item.hbm.missile.part." + key)
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value);
    }
}
