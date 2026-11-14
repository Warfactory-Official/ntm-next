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

package com.hbm.extprop;

import com.hbm.platform.EntityData;
import com.hbm.platform.PlayerLevelChange;
import com.hbm.platform.ServerEvents;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

public final class ModEntityData {

    public static Supplier<AttachmentType<PlayerState>> PLAYER_PROPS;
    public static Supplier<AttachmentType<LivingHazards>> LIVING_PROPS;

    private ModEntityData() {}

    public static void register() {
        PLAYER_PROPS =
                EntityData.register(
                        RegistryUtil.id("player_props"), PlayerState::new, PlayerState.CODEC, true);
        LIVING_PROPS =
                EntityData.register(
                        RegistryUtil.id("living_props"),
                        LivingHazards::new,
                        LivingHazards.CODEC,
                        false);
        final Consumer<ServerPlayer> callback = PlayerState::respawned;
        ServerEvents.RESPAWN.add(callback);
        final PlayerLevelChange callback2 =
                (player, _, _) ->
                        PlayerState.getOrCreate(player).changedLevel();
        ServerEvents.LEVEL_CHANGE.add(callback2);
    }

    public static <T> Supplier<AttachmentType<T>> addonKey(
            Identifier id, Supplier<T> defaults, MapCodec<T> codec, boolean copyOnDeath) {
        return EntityData.register(id, defaults, codec, copyOnDeath);
    }

    public static <T> @Nullable T getOrNull(Entity entity, Supplier<AttachmentType<T>> key) {
        return entity.getExistingDataOrNull(key.get());
    }

    public static <T> void set(Entity entity, Supplier<AttachmentType<T>> key, T value) {
        entity.setData(key.get(), value);
    }
}
