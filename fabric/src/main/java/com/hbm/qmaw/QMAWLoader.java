// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.qmaw;

import com.hbm.lib.Library;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;

public final class QMAWLoader extends SimplePreparableReloadListener<QMAWCatalog> {
    public static final Identifier ID = Library.id("qmaw");
    private static final FileToIdConverter FILES = FileToIdConverter.json("qmaw");

    private final HolderLookup.Provider registries;

    public QMAWLoader(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    protected QMAWCatalog prepare(ResourceManager manager, ProfilerFiller profiler) {

        return read(manager, registries);
    }

    public static QMAWCatalog read(ResourceManager manager, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var pages = new HashMap<Identifier, QuickManualAndWiki>();
        for (var resource : FILES.listMatchingResources(manager).entrySet()) {
            Identifier id = FILES.fileToId(resource.getKey());
            try (var reader = resource.getValue().openAsReader()) {
                QuickManualAndWiki page =
                        QuickManualAndWiki.CODEC
                                .parse(ops, StrictJsonParser.parse(reader))
                                .getOrThrow();
                pages.put(id, page);
            } catch (IOException e) {
                throw new UncheckedIOException("QMAW " + resource.getKey(), e);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "QMAW " + resource.getKey() + ": " + e.getMessage(), e);
            }
        }
        return new QMAWCatalog(pages);
    }

    @Override
    protected void apply(QMAWCatalog catalog, ResourceManager manager, ProfilerFiller profiler) {
        QMAWCatalog.publishServer(catalog);
    }
}
