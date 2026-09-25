// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.items.tool.ItemGuideBook.BookType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

final class GuideBookContents {
    private static final List<GuidePage> TEST = List.copyOf(statFacTest());
    private static final List<GuidePage> RBMK = List.copyOf(statFacRBMK());
    private static final List<GuidePage> HADRON = List.copyOf(statFacHadron());
    private static final List<GuidePage> STARTER = List.copyOf(statFacStarter());

    static List<GuidePage> pages(BookType type) {
        return switch (type) {
            case TEST -> TEST;
            case RBMK -> RBMK;
            case HADRON -> HADRON;
            case STARTER -> STARTER;
        };
    }

    public static List<GuidePage> statFacTest() {

        List<GuidePage> pages = new ArrayList<>();

        pages.add(
                new GuidePage()
                        .addTitle("desc.gui.guide.testTitle", 0x800000, 1F)
                        .addText("book.test.page1", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/smileman.png"),
                                100,
                                40,
                                40));
        pages.add(
                new GuidePage()
                        .addTitle("desc.gui.guide.testSecondTitle", 0x800000, 0.5F)
                        .addText("book.test.page1", 1.75F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/smileman.png"),
                                100,
                                40,
                                40));
        pages.add(new GuidePage().addText("desc.gui.guide.testText"));
        pages.add(new GuidePage().addText("desc.gui.guide.testLongText"));
        pages.add(new GuidePage().addText("desc.gui.guide.testText"));
        pages.add(new GuidePage().addText("desc.gui.guide.testLongText"));
        pages.add(new GuidePage().addText("desc.gui.guide.testText"));

        return pages;
    }

    public static List<GuidePage> statFacRBMK() {

        List<GuidePage> pages = new ArrayList<>();
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title1", 0x800000, 1F)
                        .addText("book.rbmk.page1", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk1.png"),
                                90,
                                80,
                                60));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title2", 0x800000, 1F)
                        .addText("book.rbmk.page2", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk2.png"),
                                95,
                                52,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title3", 0x800000, 1F)
                        .addText("book.rbmk.page3", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk3.png"),
                                95,
                                88,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title4", 0x800000, 1F)
                        .addText("book.rbmk.page4", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk4.png"),
                                95,
                                88,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title5", 0x800000, 1F)
                        .addText("book.rbmk.page5", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk5.png"),
                                95,
                                80,
                                42));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title6", 0x800000, 1F)
                        .addText("book.rbmk.page6", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk6.png"),
                                90,
                                100,
                                60));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title7", 0x800000, 1F)
                        .addText("book.rbmk.page7", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk7.png"),
                                95,
                                52,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title8", 0x800000, 1F)
                        .addText("book.rbmk.page8", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk8.png"),
                                95,
                                88,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title9", 0x800000, 1F)
                        .addText("book.rbmk.page9", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk9.png"),
                                95,
                                88,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title10", 0x800000, 1F)
                        .addText("book.rbmk.page10", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk10.png"),
                                95,
                                88,
                                52));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title11", 0x800000, 1F)
                        .addText("book.rbmk.page11", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk11.png"),
                                75,
                                85,
                                72));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title12", 0x800000, 1F)
                        .addText("book.rbmk.page12", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk12.png"),
                                90,
                                80,
                                60));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title13", 0x800000, 1F)
                        .addText("book.rbmk.page13", 2F));
        pages.add(
                new GuidePage()
                        .addText("book.rbmk.page14", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk13.png"),
                                70,
                                103,
                                78));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title15", 0x800000, 1F)
                        .addText("book.rbmk.page15", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk15.png"),
                                100,
                                48,
                                48));
        pages.add(
                new GuidePage()
                        .addTitle("book.rbmk.title16", 0x800000, 1F)
                        .addText("book.rbmk.page16", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/rbmk16.png"),
                                50,
                                70,
                                100));

        return pages;
    }

    public static List<GuidePage> statFacHadron() {

        List<GuidePage> pages = new ArrayList<>();

        for (int i = 1; i <= 9; i++) {
            pages.add(
                    new GuidePage()
                            .addTitle("book.error.title" + i, 0x800000, 1F)
                            .addText("book.error.page" + i, 2F));
        }

        return pages;
    }

    public static List<GuidePage> statFacStarter() {

        List<GuidePage> pages = new ArrayList<>();

        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title1", 0x800000, 1F)
                        .addText("book.starter.page1", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter1.png"),
                                96,
                                101,
                                56));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title2", 0x800000, 1F)
                        .addText("book.starter.page2", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/mask_piss.png"),
                                85,
                                64,
                                64));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title3", 0x800000, 1F)
                        .addText("book.starter.page3", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter3.png"),
                                89,
                                100,
                                64));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title4", 0x800000, 1F)
                        .addText("book.starter.page4", 1.4F, 0, 6, 72)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/template_folder.png"),
                                72,
                                30,
                                24,
                                24)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/stamp_iron_flat.png"),
                                72,
                                60,
                                24,
                                24)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/assembly_template.png"),
                                72,
                                90,
                                24,
                                24)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/chemistry_template.png"),
                                72,
                                120,
                                24,
                                24));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title5", 0x800000, 1F)
                        .addText("book.starter.page5", 2F));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title6", 0x800000, 1F)
                        .addText("book.starter.page6a", 2F)
                        .addText("book.starter.page6b", 2f, 0, 96, 100)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter6.png"),
                                9,
                                89,
                                84,
                                36));
        pages.add(
                new GuidePage()
                        .addText("book.starter.page7a", 2F)
                        .addText("book.starter.page7b", 2F, 0, 95, 100)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter7.png"),
                                9,
                                67,
                                84,
                                58));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title8", 0x800000, 1F)
                        .addText("book.starter.page8a", 2F, 0, -1, 50)
                        .addText("book.starter.page8b", 2F, 50, 70, 50)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter8a.png"),
                                53,
                                36,
                                47,
                                61)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter8b.png"),
                                0,
                                102,
                                47,
                                61));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title9", 0x800000, 1F)
                        .addText("book.starter.page9", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/ingot_polymer.png"),
                                4,
                                106,
                                24,
                                24)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/ingot_desh.png"),
                                28,
                                130,
                                24,
                                24)
                        .addImage(
                                Identifier.parse(
                                        "hbm" + ":textures/item/solid_fuel_presto_triplet.png"),
                                52,
                                106,
                                24,
                                24)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/item/canister_gasoline.png"),
                                76,
                                130,
                                24,
                                24));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title10", 0x800000, 1F)
                        .addText("book.starter.page10", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter10.png"),
                                0,
                                115,
                                100,
                                39));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title11", 0x800000, 1F)
                        .addText("book.starter.page11", 2F, 0, -1, 60)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter11a.png"),
                                61,
                                36,
                                45,
                                57)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter11b.png"),
                                61,
                                97,
                                45,
                                57));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title12", 0xfece00, 1F)
                        .addText("book.starter.page12a", 3F)
                        .addText("book.starter.page12b", 2F, 0, 20, 100));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title13", 0x800000, 1F)
                        .addText("book.starter.page13", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter13.png"),
                                110,
                                84,
                                42));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title14", 0x800000, 1F)
                        .addText("book.starter.page14", 2F, 0, 54, 100)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter14.png"),
                                34,
                                100,
                                46));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title15", 0x800000, 1F)
                        .addText("book.starter.page15", 2F));
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title16", 0x800000, 1F)
                        .addText("book.starter.page16", 2F));
        pages.add(new GuidePage());
        pages.add(
                new GuidePage()
                        .addTitle("book.starter.title18", 0x800000, 1F)
                        .addText("book.starter.page18", 2F)
                        .addImage(
                                Identifier.parse("hbm" + ":textures/gui/book/starter18.png"),
                                10,
                                69,
                                100,
                                100));

        return pages;
    }

    public static class GuidePage {

        public String title;
        public int titleColor;
        public float titleScale;

        public List<GuideText> texts = new ArrayList<>();
        public List<GuideImage> images = new ArrayList<>();

        public GuidePage() {}

        public GuidePage addTitle(String title, int color, float scale) {
            this.title = title;
            this.titleColor = color;
            this.titleScale = scale;
            return this;
        }

        public GuidePage addText(String text) {
            texts.add(new GuideText(text));
            return this;
        }

        public GuidePage addText(String text, float scale) {
            texts.add(new GuideText(text).setScale(scale));
            return this;
        }

        public GuidePage addText(String text, int xOffset, int yOffset, int width) {
            texts.add(new GuideText(text).setSize(xOffset, yOffset, width));
            return this;
        }

        public GuidePage addText(String text, float scale, int xOffset, int yOffset, int width) {
            texts.add(new GuideText(text).setSize(xOffset, yOffset, width).setScale(scale));
            return this;
        }

        public GuidePage addImage(
                Identifier image, int xOffset, int yOffset, int sizeX, int sizeY) {
            images.add(new GuideImage(image, xOffset, yOffset, sizeX, sizeY));
            return this;
        }

        public GuidePage addImage(Identifier image, int yOffset, int sizeX, int sizeY) {
            images.add(new GuideImage(image, -1, yOffset, sizeX, sizeY));
            return this;
        }
    }

    public static class GuideText {
        public String text;
        public float scale = 1F;
        public int xOffset = 0;
        public int yOffset = -1;
        public int width = 100;

        public GuideText(String text) {
            this.text = text;
        }

        public GuideText setScale(float scale) {
            this.scale = scale;
            return this;
        }

        public GuideText setSize(int xOffset, int yOffset, int width) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.width = width;
            return this;
        }
    }

    public static class GuideImage {
        public Identifier image;
        public int x;
        public int y;
        public int sizeX;
        public int sizeY;

        public GuideImage(Identifier image, int x, int y, int sizeX, int sizeY) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
        }
    }
}
