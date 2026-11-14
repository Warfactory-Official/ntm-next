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

package com.hbm.packet.layout;

public final class PacketLayoutRegistry {
    private PacketLayoutRegistry() {}

    public static void registerAll() {
        PacketFamilyBulkCopy.register();
        PacketFamilyBulkDownload.register();
        PacketFamilyBulkPaste.register();
        PacketFamilyBulkSession.register();
        PacketFamilyClientEvents.register();
        PacketFamilyControls.register();
        PacketFamilyDatapackQmaw.register();
        PacketFamilyDatapackRecipes.register();
        PacketFamilyEffects.register();
        PacketFamilyPrejoinBlueprintPools.register();
        PacketFamilyPrejoinFluidProperties.register();
        PacketFamilyPrejoinOreGeneration.register();
        PacketFamilyServerActions.register();
        PacketFamilySyncBodies.register();
        PacketFamilySyncRegistry.register();
        PacketFamilySyncValues.register();
    }
}
