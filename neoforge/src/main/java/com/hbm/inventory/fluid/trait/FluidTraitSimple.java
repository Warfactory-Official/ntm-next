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

package com.hbm.inventory.fluid.trait;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class FluidTraitSimple {

    private FluidTraitSimple() {}

    public static final class FT_Gaseous implements FluidTrait {

        public static final FT_Gaseous INSTANCE = new FT_Gaseous();

        private FT_Gaseous() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.gaseous")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_Gaseous_ART implements FluidTrait {

        public static final FT_Gaseous_ART INSTANCE = new FT_Gaseous_ART();

        private FT_Gaseous_ART() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.gaseousRoom")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_Liquid implements FluidTrait {

        public static final FT_Liquid INSTANCE = new FT_Liquid();

        private FT_Liquid() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.liquid")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_Viscous implements FluidTrait {

        public static final FT_Viscous INSTANCE = new FT_Viscous();

        private FT_Viscous() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.viscous")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    @Deprecated
    public static final class FT_Plasma implements FluidTrait {

        public static final FT_Plasma INSTANCE = new FT_Plasma();

        private FT_Plasma() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(
                                    Component.literal("[Plasma]")
                                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    public static final class FT_Amat implements FluidTrait {

        public static final FT_Amat INSTANCE = new FT_Amat();

        private FT_Amat() {}

        @Override
        public void describe(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.DARK_RED))
                            .append(
                                    Component.translatable("hbmfluid.trait.antimatter")
                                            .withStyle(ChatFormatting.DARK_RED))
                            .append(Component.literal("]").withStyle(ChatFormatting.DARK_RED)));
        }
    }

    public static final class FT_LeadContainer implements FluidTrait {

        public static final FT_LeadContainer INSTANCE = new FT_LeadContainer();

        private FT_LeadContainer() {}

        @Override
        public void describe(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.DARK_RED))
                            .append(
                                    Component.translatable("hbmfluid.trait.leadContainer")
                                            .withStyle(ChatFormatting.DARK_RED))
                            .append(Component.literal("]").withStyle(ChatFormatting.DARK_RED)));
        }
    }

    public static final class FT_Delicious implements FluidTrait {

        public static final FT_Delicious INSTANCE = new FT_Delicious();

        private FT_Delicious() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GREEN))
                            .append(
                                    Component.translatable("hbmfluid.trait.delicious")
                                            .withStyle(ChatFormatting.DARK_GREEN))
                            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GREEN)));
        }
    }

    public static final class FT_Unsiphonable implements FluidTrait {

        public static final FT_Unsiphonable INSTANCE = new FT_Unsiphonable();

        private FT_Unsiphonable() {}

        @Override
        public void describeDetails(Consumer<Component> info) {
            info.accept(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.unsiphonable")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_NoID implements FluidTrait {

        public static final FT_NoID INSTANCE = new FT_NoID();

        private FT_NoID() {}
    }

    public static final class FT_NoContainer implements FluidTrait {

        public static final FT_NoContainer INSTANCE = new FT_NoContainer();

        private FT_NoContainer() {}
    }
}
