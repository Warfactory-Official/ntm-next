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

public final class NeoForgeHbmConfig {
    public static final Map<ConfigEntry<?>, ModConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
    public static final ModConfigSpec CONTENT_SPEC = build(Domain.CONTENT);
    public static final ModConfigSpec RUNTIME_SPEC = build(Domain.RUNTIME);
    public static final ModConfigSpec CLIENT_SPEC = build(Domain.CLIENT);

    public static final ConfigStore STORE =
            new ConfigStore() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(ConfigEntry<T> entry) {
                    ModConfigSpec.ConfigValue<?> value = VALUES.get(entry);
                    if (value == null) {
                        throw new IllegalStateException(
                                "config entry not in the spec: " + entry.key());
                    }
                    return (T) value.get();
                }
            };

    public static final VarHandle EXTERNAL_EDIT;
    public static volatile boolean externalEdit;

    static {
        try {
            EXTERNAL_EDIT =
                    MethodHandles.lookup()
                            .findStaticVarHandle(
                                    NeoForgeHbmConfig.class, "externalEdit", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final ConfigStore.Content CONTENT = new ConfigStore.Content(STORE);
    public static final ConfigStore.Runtime RUNTIME = new ConfigStore.Runtime(STORE);

    public static ModConfigSpec build(Domain domain) {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        for (ConfigEntry<?> entry : ConfigSchema.of(domain)) {
            if (!entry.comment().isEmpty()) {
                b.comment(entry.comment());
            }
            List<String> path = List.of(entry.section(), entry.key());
            VALUES.put(
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

    public static ModConfigSpec spec(Domain domain) {
        return switch (domain) {
            case CONTENT -> CONTENT_SPEC;
            case RUNTIME -> RUNTIME_SPEC;
            case CLIENT -> CLIENT_SPEC;
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> void set(ConfigEntry<T> entry, T value) {
        ModConfigSpec.ConfigValue<T> config = (ModConfigSpec.ConfigValue<T>) VALUES.get(entry);
        if (config == null) {
            throw new IllegalStateException("config entry not in the spec: " + entry.key());
        }
        config.set(value);
        spec(entry.domain()).save();
        NuclearTech.deriveConfigFacades();
        externalEdit = false;
    }

    public static void reload() {
        NuclearTech.deriveConfigFacades();
        externalEdit = false;
    }

    public static ContentConfig content() {
        return CONTENT;
    }

    public static RuntimeConfig runtime() {
        return RUNTIME;
    }

    public static void registerSpecs(ModContainer container) {
        container.registerConfig(
                ModConfig.Type.COMMON, CONTENT_SPEC, ConfigFiles.neoForgeFile(Domain.CONTENT));
        container.registerConfig(
                ModConfig.Type.COMMON, RUNTIME_SPEC, ConfigFiles.neoForgeFile(Domain.RUNTIME));
        container.registerConfig(
                ModConfig.Type.CLIENT, CLIENT_SPEC, ConfigFiles.neoForgeFile(Domain.CLIENT));
    }
}
