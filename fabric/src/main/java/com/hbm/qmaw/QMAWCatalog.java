// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.qmaw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class QMAWCatalog {
    public static final QMAWCatalog EMPTY = new QMAWCatalog(Map.of());
    private static volatile QMAWCatalog server = EMPTY;
    private static volatile QMAWCatalog remote = EMPTY;
    private final Map<Identifier, QuickManualAndWiki> pages;
    private final Map<String, Identifier> names;
    private final Entry[] ordered;

    public QMAWCatalog(Map<Identifier, QuickManualAndWiki> pages) {
        this.pages = Map.copyOf(pages);
        Map<String, Identifier> names = new HashMap<>();
        pages.forEach(
                (id, page) -> {
                    alias(names, id, page.name());
                    for (String alias : page.aliases()) alias(names, id, alias);
                });
        this.names = Map.copyOf(names);
        ordered =
                pages.entrySet().stream()
                        .map(e -> new Entry(e.getKey(), e.getValue()))
                        .sorted(
                                Comparator.<Entry>comparingInt(e -> e.page.priority())
                                        .reversed()
                                        .thenComparing(e -> e.id.toString()))
                        .toArray(Entry[]::new);
    }

    private static void alias(Map<String, Identifier> names, Identifier id, String name) {
        Identifier previous = names.putIfAbsent(id.getNamespace() + ":" + name, id);
        if (previous != null && !previous.equals(id)) {
            throw new IllegalArgumentException(
                    "QMAW name '" + name + "' is shared by " + previous + " and " + id);
        }
    }

    public static QMAWCatalog server() {
        return server;
    }

    public static QMAWCatalog remote() {
        return remote;
    }

    public static void publishServer(QMAWCatalog catalog) {
        server = catalog;
    }

    public static void publishRemote(QMAWCatalog catalog) {
        remote = catalog;
    }

    public static void clearServer() {
        server = EMPTY;
    }

    public static void clearRemote() {
        remote = EMPTY;
    }

    public Map<Identifier, QuickManualAndWiki> pages() {
        return pages;
    }

    public @Nullable QuickManualAndWiki page(Identifier id) {
        return pages.get(id);
    }

    public @Nullable Identifier link(Identifier from, String target) {
        if (target.indexOf(':') >= 0) {
            Identifier id = Identifier.tryParse(target);
            return id != null && pages.containsKey(id) ? id : null;
        }
        return names.get(from.getNamespace() + ":" + target);
    }

    public String compose(Identifier from, String language, String content) {
        return compose(from, language, content, new ArrayList<>());
    }

    private String compose(
            Identifier from, String language, String content, List<Identifier> active) {
        if (active.contains(from) || active.size() == 100) {
            throw new IllegalArgumentException(
                    "QMAW composition cycle or depth limit at " + from + " through " + active);
        }
        active.add(from);
        StringBuilder result = new StringBuilder();
        int at = 0;
        while (at < content.length()) {
            int begin = content.indexOf("{{", at);
            if (begin < 0) break;
            int end = content.indexOf("}}", begin + 2);
            if (end < 0) break;
            result.append(content, at, begin);
            String name = content.substring(begin + 2, end);
            Identifier target = link(from, name);
            if (target == null) {
                result.append(name);
            } else {
                QuickManualAndWiki page = pages.get(target);
                String replacement =
                        page.content()
                                .getOrDefault(language, page.content().getOrDefault("en_us", name));
                result.append(compose(target, language, replacement, active));
            }
            at = end + 2;
        }
        result.append(content, at, content.length());
        active.removeLast();
        return result.toString();
    }

    public @Nullable Identifier forStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Entry best = null;
        boolean specific = false;
        for (Entry entry : ordered) {
            if (best != null && entry.page.priority() < best.page.priority()) break;
            for (var trigger : entry.page.triggers()) {
                if (!trigger.test(stack)) continue;
                boolean exact = !trigger.components().isEmpty();
                if (best == null || exact && !specific) {
                    best = entry;
                    specific = exact;
                }
            }
        }
        return best == null ? null : best.id;
    }

    private record Entry(Identifier id, QuickManualAndWiki page) {}
}
