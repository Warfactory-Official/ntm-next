// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.ChatFormatting;

public final class FluidTraitSimple {

    private FluidTraitSimple() {}

    public static final class FT_Gaseous extends FluidTrait {

        public static final FT_Gaseous INSTANCE = new FT_Gaseous();

        private FT_Gaseous() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(
                    ChatFormatting.BLUE
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.gaseous")
                            + "]");
        }
    }

    public static final class FT_Gaseous_ART extends FluidTrait {

        public static final FT_Gaseous_ART INSTANCE = new FT_Gaseous_ART();

        private FT_Gaseous_ART() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(
                    ChatFormatting.BLUE
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.gaseousRoom")
                            + "]");
        }
    }

    public static final class FT_Liquid extends FluidTrait {

        public static final FT_Liquid INSTANCE = new FT_Liquid();

        private FT_Liquid() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(
                    ChatFormatting.BLUE + "[" + I18nUtil.resolveKey("hbmfluid.trait.liquid") + "]");
        }
    }

    public static final class FT_Viscous extends FluidTrait {

        public static final FT_Viscous INSTANCE = new FT_Viscous();

        private FT_Viscous() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(
                    ChatFormatting.BLUE
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.viscous")
                            + "]");
        }
    }

    @Deprecated
    public static final class FT_Plasma extends FluidTrait {

        public static final FT_Plasma INSTANCE = new FT_Plasma();

        private FT_Plasma() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(ChatFormatting.LIGHT_PURPLE + "[Plasma]");
        }
    }

    public static final class FT_Amat extends FluidTrait {

        public static final FT_Amat INSTANCE = new FT_Amat();

        private FT_Amat() {}

        @Override
        public void addInfo(List<String> info) {
            info.add(
                    ChatFormatting.DARK_RED
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.antimatter")
                            + "]");
        }
    }

    public static final class FT_LeadContainer extends FluidTrait {

        public static final FT_LeadContainer INSTANCE = new FT_LeadContainer();

        private FT_LeadContainer() {}

        @Override
        public void addInfo(List<String> info) {
            info.add(
                    ChatFormatting.DARK_RED
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.leadContainer")
                            + "]");
        }
    }

    public static final class FT_Delicious extends FluidTrait {

        public static final FT_Delicious INSTANCE = new FT_Delicious();

        private FT_Delicious() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(
                    ChatFormatting.DARK_GREEN
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.delicious")
                            + "]");
        }
    }

    public static final class FT_Unsiphonable extends FluidTrait {

        public static final FT_Unsiphonable INSTANCE = new FT_Unsiphonable();

        private FT_Unsiphonable() {}

        @Override
        public void addInfoHidden(List<String> info) {
            info.add(
                    ChatFormatting.BLUE
                            + "["
                            + I18nUtil.resolveKey("hbmfluid.trait.unsiphonable")
                            + "]");
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
