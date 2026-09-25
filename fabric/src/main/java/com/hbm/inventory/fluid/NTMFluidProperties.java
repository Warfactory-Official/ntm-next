// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.util.RegistryUtil;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class NTMFluidProperties {

    private static volatile Reference2ObjectOpenHashMap<Fluid, NTMFluidProperty> OWN =
            new Reference2ObjectOpenHashMap<>();
    private static volatile Reference2ObjectOpenHashMap<Fluid, Component> NAMES =
            new Reference2ObjectOpenHashMap<>();

    private static volatile Reference2ReferenceOpenHashMap<Fluid, Fluid> KINDS =
            new Reference2ReferenceOpenHashMap<>();
    private static volatile Reference2ObjectOpenHashMap<Fluid, Fluid[]> EQUIVALENTS =
            new Reference2ObjectOpenHashMap<>();
    private static final Fluid[] NO_FLUIDS = new Fluid[0];
    private static final Component EMPTY_NAME = Component.translatable("hbmfluid.none");

    private static volatile boolean resyncPending;
    private static volatile boolean syncOwned;

    private NTMFluidProperties() {}

    public static void register(Fluid fluid, NTMFluidProperty property) {
        put(OWN, NAMES, fluid, property);
    }

    private static void put(
            Map<Fluid, NTMFluidProperty> own,
            Map<Fluid, Component> names,
            Fluid fluid,
            NTMFluidProperty property) {
        own.put(fluid, property);
        Identifier id = RegistryUtil.keyOf(fluid);
        names.put(fluid, Component.translatable(nameKey(id)));
    }

    public static void onClientDisconnect() {
        if (!syncOwned) return;
        syncOwned = false;
        clear();
    }

    public static void markResyncPending() {
        resyncPending = true;
    }

    public static boolean takeResyncPending() {
        if (!resyncPending) return false;
        resyncPending = false;
        return true;
    }

    public static Map<Fluid, NTMFluidProperty> snapshot() {
        return new Reference2ObjectOpenHashMap<>(OWN);
    }

    public static void acceptSync(Map<Fluid, NTMFluidProperty> properties) {
        replace(properties);
        syncOwned = true;
    }

    public static void replace(Map<Fluid, NTMFluidProperty> properties) {
        Reference2ObjectOpenHashMap<Fluid, NTMFluidProperty> own =
                new Reference2ObjectOpenHashMap<>(properties.size());
        Reference2ObjectOpenHashMap<Fluid, Component> names =
                new Reference2ObjectOpenHashMap<>(properties.size());
        properties.forEach((fluid, property) -> put(own, names, fluid, property));
        Equivalence replacement = compileEquivalents(own);
        NAMES = names;
        OWN = own;
        EQUIVALENTS = replacement.equivalents();
        KINDS = replacement.kinds();
    }

    public static @Nullable NTMFluidProperty get(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        NTMFluidProperty own = OWN.get(fluid);
        return own != null ? own : OWN.get(kindOf(fluid));
    }

    public static Fluid kindOf(Fluid fluid) {
        Fluid kind = KINDS.get(fluid);
        return kind != null ? kind : fluid;
    }

    public static Fluid[] equivalents(Fluid kind) {
        Fluid[] out = EQUIVALENTS.get(kind);
        return out != null ? out : NO_FLUIDS;
    }

    public static void bindEquivalents() {
        Equivalence replacement = compileEquivalents(OWN);
        EQUIVALENTS = replacement.equivalents();
        KINDS = replacement.kinds();
    }

    private static Equivalence compileEquivalents(Map<Fluid, NTMFluidProperty> own) {
        Reference2ReferenceOpenHashMap<Fluid, Fluid> kinds = new Reference2ReferenceOpenHashMap<>();
        Reference2ObjectOpenHashMap<Fluid, Fluid[]> equivalents =
                new Reference2ObjectOpenHashMap<>();
        for (Fluid ntm : NTMFluids.displayOrder()) {
            NTMFluidProperty property = own.get(ntm);
            if (property == null) continue;
            List<Fluid> sources = new ArrayList<>(0);
            for (TagKey<Fluid> tag : property.shares()) {
                for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
                    Fluid member = holder.value();
                    if (!isForeign(member)) continue;
                    Fluid prior = kinds.putIfAbsent(member, ntm);
                    if (prior != null && prior != ntm) continue;

                    if (member.isSource(member.defaultFluidState()) && !sources.contains(member))
                        sources.add(member);
                }
            }
            if (!sources.isEmpty()) equivalents.put(ntm, sources.toArray(NO_FLUIDS));
        }
        return new Equivalence(equivalents, kinds);
    }

    private record Equivalence(
            Reference2ObjectOpenHashMap<Fluid, Fluid[]> equivalents,
            Reference2ReferenceOpenHashMap<Fluid, Fluid> kinds) {}

    public static boolean isForeign(Fluid fluid) {
        return fluid != Fluids.EMPTY
                && NTMFluids.legacyName(fluid) == null
                && !"hbm".equals(BuiltInRegistries.FLUID.getKey(fluid).getNamespace());
    }

    public static boolean isOwn(@Nullable Fluid fluid) {
        return fluid != null && OWN.containsKey(fluid);
    }

    public static boolean hasTrait(@Nullable Fluid fluid, Class<? extends FluidTrait> trait) {
        NTMFluidProperty p = get(fluid);
        return p != null && p.hasTrait(trait);
    }

    public static boolean isDispersable(@Nullable Fluid fluid) {
        NTMFluidProperty p = get(fluid);
        return p != null && p.isDispersable();
    }

    public static <T extends FluidTrait> @Nullable T getTrait(
            @Nullable Fluid fluid, Class<? extends T> trait) {
        NTMFluidProperty p = get(fluid);
        return p == null ? null : p.getTrait(trait);
    }

    public static void clear() {
        OWN = new Reference2ObjectOpenHashMap<>();
        NAMES = new Reference2ObjectOpenHashMap<>();
        EQUIVALENTS = new Reference2ObjectOpenHashMap<>();
        KINDS = new Reference2ReferenceOpenHashMap<>();
    }

    public static Component getDisplayName(@Nullable Fluid fluid) {

        if (fluid == null || fluid == Fluids.EMPTY) return EMPTY_NAME;
        Component own = NAMES.get(fluid);
        if (own != null) return own;
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        String key = nameKey(id);

        if (Language.getInstance().has(key)) return Component.translatable(key);
        return Component.literal(prettify(id.getPath()));
    }

    public static String nameKey(Identifier id) {
        return Util.makeDescriptionId("block", id);
    }

    public static String clientName(@Nullable Fluid fluid) {
        return getDisplayName(fluid).getString();
    }

    private static String prettify(String path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path.toLowerCase(Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
