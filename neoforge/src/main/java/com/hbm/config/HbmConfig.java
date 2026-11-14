/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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

public class HbmConfig {
    public static final Map<ConfigEntry<?>, ModConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
    private static final ModConfigSpec CONTENT_SPEC = build(Domain.CONTENT);
    private static final ModConfigSpec RUNTIME_SPEC = build(Domain.RUNTIME);
    private static final ModConfigSpec CLIENT_SPEC = build(Domain.CLIENT);

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

    private static final VarHandle EXTERNAL_EDIT;
    public static volatile boolean externalEdit;

    static {
        try {
            EXTERNAL_EDIT =
                    MethodHandles.lookup()
                            .findStaticVarHandle(HbmConfig.class, "externalEdit", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final ConfigStore.Content CONTENT = new ConfigStore.Content(STORE);
    public static final ConfigStore.Runtime RUNTIME = new ConfigStore.Runtime(STORE);

    public static <T> void set(ConfigEntry<T> entry, T value) {
        @SuppressWarnings("unchecked")
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

    public static boolean pollForExternalEdit() {
        return EXTERNAL_EDIT.compareAndSet(true, false);
    }

    private static ModConfigSpec build(Domain domain) {
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

    public static void registerSpecs(ModContainer container) {
        container.registerConfig(
                ModConfig.Type.LOCAL, CONTENT_SPEC, ConfigFiles.neoForgeFile(Domain.CONTENT));
        container.registerConfig(
                ModConfig.Type.LOCAL, RUNTIME_SPEC, ConfigFiles.neoForgeFile(Domain.RUNTIME));
        container.registerConfig(
                ModConfig.Type.CLIENT, CLIENT_SPEC, ConfigFiles.neoForgeFile(Domain.CLIENT));
    }
}
