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

package com.hbm.client;

import com.hbm.NuclearTech;
import com.hbm.config.ConfigSchema;
import com.hbm.config.HbmConfig;
import com.hbm.config.InteractionConfig;
import com.hbm.util.CurseForgeUpdates;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class UpdateChecker {
    private static final List<Project> PROJECTS =
            new ArrayList<>(List.of(new Project(NuclearTech.MOD_ID, "NTM NEXT", 1699386)));
    private static boolean started;

    private UpdateChecker() {}

    public static void register(String modId, String name, int projectId) {
        if (started) throw new IllegalStateException("Update checker registration is closed");
        PROJECTS.add(new Project(modId, name, projectId));
    }

    public static void start() {
        if (started
                || (System.getProperty("fabric-api.datagen") != null)
                || !HbmConfig.STORE.get(ConfigSchema.CHECK_FOR_UPDATES)) return;
        started = true;
        var minecraft = SharedConstants.getCurrentVersion().name();
        var loader = "Fabric";
        for (var project : PROJECTS) {
            final String modId = project.modId;
            var installed =
                    FabricLoader.getInstance()
                            .getModContainer(modId)
                            .orElseThrow()
                            .getMetadata()
                            .getVersion()
                            .getFriendlyString();
            Util.nonCriticalIoPool()
                    .execute(
                            () -> {
                                try {
                                    project.update =
                                            CurseForgeUpdates.check(
                                                    project.endpoint, minecraft, loader, installed);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    NuclearTech.LOGGER.warn(
                                            "{} update check interrupted", project.name, e);
                                } catch (Exception e) {
                                    NuclearTech.LOGGER.warn(
                                            "Could not check {} updates through CFWidget",
                                            project.name,
                                            e);
                                }
                            });
        }
    }

    public static void tick() {
        if (!InteractionConfig.checkForUpdates) return;
        var client = Minecraft.getInstance();
        if (client.player == null) return;
        for (var project : PROJECTS) {
            var available = project.update;
            if (available == null) continue;
            project.update = null;
            client.gui
                    .chatListener()
                    .handleSystemMessage(
                            Component.translatable(
                                    "chat.hbm.update_available",
                                    project.name,
                                    available.version(),
                                    link(Component.translatable("chat.hbm.here"), available.url())),
                            false);
        }
    }

    public static MutableComponent link(MutableComponent label, URI url) {
        return label.withStyle(
                style ->
                        style.withColor(ChatFormatting.YELLOW)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenUrl(url)));
    }

    private static final class Project {
        private final String modId;
        private final String name;
        private final URI endpoint;
        private volatile CurseForgeUpdates.@Nullable Release update;

        private Project(String modId, String name, int projectId) {
            this.modId = modId;
            this.name = name;
            this.endpoint = URI.create("https://api.cfwidget.com/" + projectId);
        }
    }
}
