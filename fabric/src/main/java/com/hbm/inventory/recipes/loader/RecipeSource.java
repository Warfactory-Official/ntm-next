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

package com.hbm.inventory.recipes.loader;

import com.hbm.platform.ServerEvents;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jspecify.annotations.Nullable;

public final class RecipeSource {

    public static final Token NEVER_FILLED = new Token(new Object());
    private static volatile @Nullable Latch client;
    private static @Nullable Token serverToken;
    private static volatile List<String> clientPoolNames = List.of();

    private RecipeSource() {}

    public static void publishClient(Object source, Collection<RecipeHolder<?>> recipes) {
        client = new Latch(new Token(source), recipes);
    }

    public static void clearClient() {
        client = null;
        clientPoolNames = List.of();
    }

    public static void publishClientPoolNames(List<String> names) {
        clientPoolNames = names;
    }

    public static List<String> clientPoolNames() {
        return clientPoolNames;
    }

    public static @Nullable Token token() {
        RecipeManager server = serverRecipes();
        if (server != null) {
            Token held = serverToken;
            if (held != null && held.source() == server) return held;
            return serverToken = new Token(server);
        }
        Latch latched = client;
        return latched == null ? null : latched.token();
    }

    public static Collection<RecipeHolder<?>> recipes() {
        RecipeManager server = serverRecipes();
        if (server != null) return server.getRecipes();
        Latch latched = client;
        return latched == null ? List.of() : latched.recipes();
    }

    private static @Nullable RecipeManager serverRecipes() {
        MinecraftServer server = ServerEvents.getCurrentServer();
        return server == null ? null : server.getRecipeManager();
    }

    public record Token(Object source) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Token(Object theirs) && theirs == this.source;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(source);
        }

        @Override
        public String toString() {
            return "Token["
                    + source.getClass().getSimpleName()
                    + "@"
                    + Integer.toHexString(System.identityHashCode(source))
                    + "]";
        }
    }

    private record Latch(Token token, Collection<RecipeHolder<?>> recipes) {}
}
