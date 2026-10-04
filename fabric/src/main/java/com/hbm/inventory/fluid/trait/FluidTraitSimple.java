// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class FluidTraitSimple {

    private FluidTraitSimple() {}

    public static final class FT_Gaseous extends FluidTrait {

        public static final FT_Gaseous INSTANCE = new FT_Gaseous();

        private FT_Gaseous() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.gaseous")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_Gaseous_ART extends FluidTrait {

        public static final FT_Gaseous_ART INSTANCE = new FT_Gaseous_ART();

        private FT_Gaseous_ART() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.gaseousRoom")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_Liquid extends FluidTrait {

        public static final FT_Liquid INSTANCE = new FT_Liquid();

        private FT_Liquid() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.liquid")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_Viscous extends FluidTrait {

        public static final FT_Viscous INSTANCE = new FT_Viscous();

        private FT_Viscous() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.viscous")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    @Deprecated
    public static final class FT_Plasma extends FluidTrait {

        public static final FT_Plasma INSTANCE = new FT_Plasma();

        private FT_Plasma() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(
                                    Component.literal("[Plasma]")
                                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    public static final class FT_Amat extends FluidTrait {

        public static final FT_Amat INSTANCE = new FT_Amat();

        private FT_Amat() {}

        @Override
        public void addInfo(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.DARK_RED))
                            .append(
                                    Component.translatable("hbmfluid.trait.antimatter")
                                            .withStyle(ChatFormatting.DARK_RED))
                            .append(Component.literal("]").withStyle(ChatFormatting.DARK_RED)));
        }
    }

    public static final class FT_LeadContainer extends FluidTrait {

        public static final FT_LeadContainer INSTANCE = new FT_LeadContainer();

        private FT_LeadContainer() {}

        @Override
        public void addInfo(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.DARK_RED))
                            .append(
                                    Component.translatable("hbmfluid.trait.leadContainer")
                                            .withStyle(ChatFormatting.DARK_RED))
                            .append(Component.literal("]").withStyle(ChatFormatting.DARK_RED)));
        }
    }

    public static final class FT_Delicious extends FluidTrait {

        public static final FT_Delicious INSTANCE = new FT_Delicious();

        private FT_Delicious() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GREEN))
                            .append(
                                    Component.translatable("hbmfluid.trait.delicious")
                                            .withStyle(ChatFormatting.DARK_GREEN))
                            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GREEN)));
        }
    }

    public static final class FT_Unsiphonable extends FluidTrait {

        public static final FT_Unsiphonable INSTANCE = new FT_Unsiphonable();

        private FT_Unsiphonable() {}

        @Override
        public void addInfoHidden(List<Component> info) {
            info.add(
                    Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.BLUE))
                            .append(
                                    Component.translatable("hbmfluid.trait.unsiphonable")
                                            .withStyle(ChatFormatting.BLUE))
                            .append(Component.literal("]").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static final class FT_NoID extends FluidTrait {

        public static final FT_NoID INSTANCE = new FT_NoID();

        private FT_NoID() {}
    }

    public static final class FT_NoContainer extends FluidTrait {

        public static final FT_NoContainer INSTANCE = new FT_NoContainer();

        private FT_NoContainer() {}
    }
}
