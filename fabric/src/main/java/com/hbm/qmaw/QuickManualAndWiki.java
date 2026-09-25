// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.qmaw;

import com.hbm.util.DataCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;

public record QuickManualAndWiki(
        String name,
        Optional<ItemStackTemplate> icon,
        Map<String, String> title,
        Map<String, String> content,
        List<ItemPredicate> triggers,
        List<String> aliases,
        int priority,
        boolean noIndex) {
    private static final Codec<String> NONBLANK =
            Codec.STRING.validate(
                    value ->
                            value.isBlank()
                                    ? DataResult.error(() -> "Text must not be blank")
                                    : DataResult.success(value));
    private static final Codec<String> LINK_NAME =
            NONBLANK.validate(
                    value ->
                            value.indexOf(':') >= 0
                                    ? DataResult.error(
                                            () ->
                                                    "Manual names and aliases must not contain ':'; links containing ':' name page IDs")
                                    : DataResult.success(value));
    private static final Codec<String> LOCALE =
            NONBLANK.validate(
                    value ->
                            value.equals(value.toLowerCase(Locale.ROOT))
                                    ? DataResult.success(value)
                                    : DataResult.error(
                                            () -> "Locale keys must be lowercase: " + value));
    private static final Codec<Map<String, String>> TEXT =
            Codec.unboundedMap(LOCALE, NONBLANK)
                    .validate(
                            value ->
                                    value.isEmpty()
                                            ? DataResult.error(
                                                    () ->
                                                            "Localized text must contain at least one locale")
                                            : DataResult.success(value));

    private static final Codec<ItemStackTemplate> ICON =
            Codec.either(
                            Item.CODEC,
                            DataCodecs.strict(
                                            RecordCodecBuilder.<ItemStackTemplate>mapCodec(
                                                    i ->
                                                            i.group(
                                                                            Item.CODEC
                                                                                    .fieldOf("id")
                                                                                    .forGetter(
                                                                                            ItemStackTemplate
                                                                                                    ::item),
                                                                            DataCodecs.intRange(
                                                                                            1, 99)
                                                                                    .optionalFieldOf(
                                                                                            "count",
                                                                                            1)
                                                                                    .forGetter(
                                                                                            ItemStackTemplate
                                                                                                    ::count),
                                                                            DataComponentPatch.CODEC
                                                                                    .optionalFieldOf(
                                                                                            "components",
                                                                                            DataComponentPatch
                                                                                                    .EMPTY)
                                                                                    .forGetter(
                                                                                            ItemStackTemplate
                                                                                                    ::components))
                                                                    .apply(
                                                                            i,
                                                                            ItemStackTemplate
                                                                                    ::new)))
                                    .codec())
                    .xmap(
                            value ->
                                    value.map(
                                            item ->
                                                    new ItemStackTemplate(
                                                            item, 1, DataComponentPatch.EMPTY),
                                            stack -> stack),
                            stack ->
                                    stack.count() == 1 && stack.components().isEmpty()
                                            ? Either.left(stack.item())
                                            : Either.right(stack));
    private static final Codec<Integer> COUNT_VALUE = DataCodecs.intRange(0, Integer.MAX_VALUE);
    private static final Codec<MinMaxBounds.Bounds<Integer>> COUNT_RANGE =
            DataCodecs.strict(
                            RecordCodecBuilder.<MinMaxBounds.Bounds<Integer>>mapCodec(
                                    i ->
                                            i.group(
                                                            COUNT_VALUE
                                                                    .optionalFieldOf("min")
                                                                    .forGetter(
                                                                            MinMaxBounds.Bounds
                                                                                    ::min),
                                                            COUNT_VALUE
                                                                    .optionalFieldOf("max")
                                                                    .forGetter(
                                                                            MinMaxBounds.Bounds
                                                                                    ::max))
                                                    .apply(i, MinMaxBounds.Bounds::new)))
                    .codec()
                    .validate(MinMaxBounds.Bounds::validateSwappedBoundsInCodec);
    private static final Codec<MinMaxBounds.Ints> COUNT =
            Codec.either(COUNT_VALUE, COUNT_RANGE)
                    .xmap(
                            value ->
                                    value.map(
                                            MinMaxBounds.Ints::exactly, QuickManualAndWiki::bounds),
                            value ->
                                    value.min().isPresent() && value.min().equals(value.max())
                                            ? Either.left(value.min().get())
                                            : Either.right(value.bounds()));
    private static final Codec<ItemPredicate> TRIGGER =
            DataCodecs.strict(
                            RecordCodecBuilder.<ItemPredicate>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ITEM)
                                                                    .optionalFieldOf("items")
                                                                    .forGetter(
                                                                            ItemPredicate::items),
                                                            COUNT.optionalFieldOf(
                                                                            "count",
                                                                            MinMaxBounds.Ints.ANY)
                                                                    .forGetter(
                                                                            ItemPredicate::count),
                                                            DataComponentMatchers.CODEC.forGetter(
                                                                    ItemPredicate::components))
                                                    .apply(i, ItemPredicate::new)))
                    .codec();
    public static final Codec<QuickManualAndWiki> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<QuickManualAndWiki>mapCodec(
                                    i ->
                                            i.group(
                                                            LINK_NAME
                                                                    .fieldOf("name")
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::name),
                                                            ICON.optionalFieldOf("icon")
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::icon),
                                                            TEXT.fieldOf("title")
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::title),
                                                            TEXT.fieldOf("content")
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::content),
                                                            DataCodecs.listOrSingle(TRIGGER)
                                                                    .optionalFieldOf(
                                                                            "trigger", List.of())
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::triggers),
                                                            DataCodecs.listOrSingle(LINK_NAME)
                                                                    .optionalFieldOf(
                                                                            "aliases", List.of())
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::aliases),
                                                            DataCodecs.INT
                                                                    .optionalFieldOf("priority", 0)
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::priority),
                                                            Codec.BOOL
                                                                    .optionalFieldOf(
                                                                            "noindex", false)
                                                                    .forGetter(
                                                                            QuickManualAndWiki
                                                                                    ::noIndex))
                                                    .apply(i, QuickManualAndWiki::new)))
                    .codec()
                    .validate(
                            page -> {
                                var names = new HashSet<String>();
                                names.add(page.name());
                                for (String alias : page.aliases()) {
                                    if (!names.add(alias))
                                        return DataResult.error(
                                                () -> "Repeated manual name or alias: " + alias);
                                }
                                return DataResult.success(page);
                            });

    private static MinMaxBounds.Ints bounds(MinMaxBounds.Bounds<Integer> range) {
        if (range.min().isPresent())
            return range.max().isPresent()
                    ? MinMaxBounds.Ints.between(range.min().get(), range.max().get())
                    : MinMaxBounds.Ints.atLeast(range.min().get());
        return range.max().map(MinMaxBounds.Ints::atMost).orElse(MinMaxBounds.Ints.ANY);
    }

    public QuickManualAndWiki {
        title = Map.copyOf(title);
        content = Map.copyOf(content);
        triggers = List.copyOf(triggers);
        aliases = List.copyOf(aliases);
    }
}
