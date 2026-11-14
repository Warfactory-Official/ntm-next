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

package com.hbm.handler.neutron;

import com.hbm.blockentity.machine.rbmk.RbmkRay;
import com.hbm.platform.ServerEvents;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class NeutronHandler {

    private NeutronHandler() {}

    public static void install() {
        final Consumer<MinecraftServer> callback = NeutronHandler::walk;
        ServerEvents.TICK_POST.add(callback);
    }

    public static void enqueue(ServerLevel level, NeutronRay ray) {
        ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$neutronRays().add(ray);
    }

    private static void walk(MinecraftServer server) {
        boolean dialsRead = false;
        for (ServerLevel level : server.getAllLevels()) {
            var rays =
                    ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$neutronRays();
            if (rays.isEmpty()) continue;
            if (!dialsRead) {
                RbmkRay.readDials();
                dialsRead = true;
            }
            for (int i = 0; i < rays.size(); i++) rays.get(i).travel(level);
            rays.clear();
        }
    }
}
