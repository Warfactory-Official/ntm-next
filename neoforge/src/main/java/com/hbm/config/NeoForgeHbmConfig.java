// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import com.hbm.NuclearTech;
import com.hbm.config.ConfigEntry.Domain;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeHbmConfig implements HbmConfig {

    private final Map<ConfigEntry<?>, ModConfigSpec.ConfigValue<?>> values = new HashMap<>();
    private final ModConfigSpec contentSpec;
    private final ModConfigSpec runtimeSpec;
    private final ModConfigSpec clientSpec;

    private final ConfigStore store =
            new ConfigStore() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(ConfigEntry<T> entry) {
                    ModConfigSpec.ConfigValue<?> value = values.get(entry);
                    if (value == null) {
                        throw new IllegalStateException(
                                "config entry not in the spec: " + entry.key());
                    }
                    return (T) value.get();
                }
            };

    private static final VarHandle EXTERNAL_EDIT;
    private volatile boolean externalEdit;

    static {
        try {
            EXTERNAL_EDIT =
                    MethodHandles.lookup()
                            .findVarHandle(NeoForgeHbmConfig.class, "externalEdit", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ConfigStore.Content content = new ConfigStore.Content(store);
    private final ConfigStore.Runtime runtime = new ConfigStore.Runtime(store);

    public NeoForgeHbmConfig() {
        contentSpec = build(Domain.CONTENT);
        runtimeSpec = build(Domain.RUNTIME);
        clientSpec = build(Domain.CLIENT);
    }

    private ModConfigSpec build(Domain domain) {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        for (ConfigEntry<?> entry : ConfigSchema.of(domain)) {
            if (!entry.comment().isEmpty()) {
                b.comment(entry.comment());
            }

            List<String> path = List.of(entry.section(), entry.key());
            values.put(
                    entry,
                    switch (entry.kind()) {
                        case BOOL -> b.define(path, (boolean) (Boolean) entry.defaultValue());
                        case INT ->
                                b.defineInRange(
                                        path,
                                        (int) (Integer) entry.defaultValue(),
                                        entry.intMin(),
                                        entry.intMax());
                        case DOUBLE ->
                                b.defineInRange(
                                        path,
                                        (double) (Double) entry.defaultValue(),
                                        entry.min(),
                                        entry.max());
                        case STRING -> b.define(path, (String) entry.defaultValue());
                    });
        }
        return b.build();
    }

    public void raiseExternalEdit() {
        externalEdit = true;
    }

    @Override
    public boolean pollForExternalEdit() {
        return EXTERNAL_EDIT.compareAndSet(this, true, false);
    }

    public ModConfigSpec spec(Domain domain) {
        return switch (domain) {
            case CONTENT -> contentSpec;
            case RUNTIME -> runtimeSpec;
            case CLIENT -> clientSpec;
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void set(ConfigEntry<T> entry, T value) {
        ModConfigSpec.ConfigValue<T> config = (ModConfigSpec.ConfigValue<T>) values.get(entry);
        if (config == null) {
            throw new IllegalStateException("config entry not in the spec: " + entry.key());
        }
        config.set(value);
        spec(entry.domain()).save();
        NuclearTech.deriveConfigFacades();

        externalEdit = false;
    }

    @Override
    public void reload() {
        NuclearTech.deriveConfigFacades();
        externalEdit = false;
    }

    @Override
    public ContentConfig content() {
        return content;
    }

    @Override
    public RuntimeConfig runtime() {
        return runtime;
    }

    @Override
    public ConfigStore clientStore() {
        return store;
    }

    public void registerSpecs(ModContainer container) {
        container.registerConfig(
                ModConfig.Type.COMMON, contentSpec, ConfigFiles.neoForgeFile(Domain.CONTENT));
        container.registerConfig(
                ModConfig.Type.COMMON, runtimeSpec, ConfigFiles.neoForgeFile(Domain.RUNTIME));
        container.registerConfig(
                ModConfig.Type.CLIENT, clientSpec, ConfigFiles.neoForgeFile(Domain.CLIENT));
    }
}
