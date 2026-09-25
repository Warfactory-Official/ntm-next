// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_NoID;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.FluidIdControlPayload;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ScreenFluidIdentifier extends Screen {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_fluid.png");

    private final int xSize = 176;
    private final int ySize = 54;
    private final Player player;
    private final Fluid[] searchArray = new Fluid[9];
    private int guiLeft;
    private int guiTop;
    private EditBox search;
    private Fluid primary = Fluids.EMPTY;
    private Fluid secondary = Fluids.EMPTY;

    public ScreenFluidIdentifier(Player player) {
        super(Component.translatable("item.hbm.fluid_identifier_multi"));
        this.player = player;

        ItemStack held = heldIdentifier(player);
        if (!held.isEmpty()) {
            FluidIdentifierData data =
                    held.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            this.primary = data.primary();
            this.secondary = data.secondary();
        }
    }

    private static String fluidId(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    private static ItemStack heldIdentifier(Player player) {
        if (isSelectorItem(player.getMainHandItem())) return player.getMainHandItem();
        return ItemStack.EMPTY;
    }

    private static boolean isSelectorItem(ItemStack stack) {
        return stack.getItem() instanceof FluidIdentifierItem;
    }

    private static void playClick() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.search = new EditBox(this.font, guiLeft + 46, guiTop + 11, 86, 12, Component.empty());
        this.search.setTextColor(CommonColors.WHITE);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setResponder(term -> updateSearch());
        addRenderableWidget(this.search);

        updateSearch();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.search);
    }

    private void updateSearch() {
        Arrays.fill(this.searchArray, null);

        int next = 0;
        String subs = this.search.getValue().toLowerCase(Locale.US);

        for (Fluid fluid : NTMFluids.displayOrder()) {
            if (NTMFluidProperties.hasTrait(fluid, FT_NoID.class)) continue;
            if (matches(fluid, subs)) {
                this.searchArray[next] = fluid;
                next++;
                if (next >= 9) return;
            }
        }

        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (NTMFluids.legacyName(fluid) != null
                    || !fluid.isSource(fluid.defaultFluidState())
                    || "hbm".equals(BuiltInRegistries.FLUID.getKey(fluid).getNamespace())) continue;
            if (matches(fluid, subs)) {
                this.searchArray[next] = fluid;
                next++;
                if (next >= 9) return;
            }
        }
    }

    private static boolean matches(Fluid fluid, String subs) {
        return NTMFluidProperties.clientName(fluid).toLowerCase(Locale.US).contains(subs);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                guiLeft,
                guiTop,
                0.0F,
                0.0F,
                xSize,
                ySize,
                256,
                256);

        if (this.search.isFocused()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + 43,
                    guiTop + 7,
                    166.0F,
                    54.0F,
                    90,
                    18,
                    256,
                    256);
        }

        for (int k = 0; k < this.searchArray.length; k++) {
            Fluid type = this.searchArray[k];
            if (type == null) return;

            NTMFluidProperty prop = NTMFluidProperties.get(type);
            int color = prop != null ? prop.colorARGB() : CommonColors.WHITE;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + 12 + k * 18,
                    guiTop + 31,
                    (float) (12 + k * 18),
                    56.0F,
                    8,
                    14,
                    256,
                    256,
                    color);

            if (type == this.primary && type == this.secondary) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        guiLeft + 7 + k * 18,
                        guiTop + 29,
                        176.0F,
                        36.0F,
                        18,
                        18,
                        256,
                        256);
            } else if (type == this.primary) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        guiLeft + 7 + k * 18,
                        guiTop + 29,
                        176.0F,
                        0.0F,
                        18,
                        18,
                        256,
                        256);
            } else if (type == this.secondary) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        guiLeft + 7 + k * 18,
                        guiTop + 29,
                        176.0F,
                        18.0F,
                        18,
                        18,
                        256,
                        256);
            }
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        for (int k = 0; k < this.searchArray.length; k++) {
            Fluid type = this.searchArray[k];
            if (type == null) return;

            if (isOverSwatch(mouseX, mouseY, k)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(NTMFluidProperties.getDisplayName(type));
                FluidTraitTooltip.addInfo(type, tooltip::add);
                graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int i = (int) event.x(), j = (int) event.y();

        for (int k = 0; k < this.searchArray.length; k++) {
            Fluid type = this.searchArray[k];
            if (type == null) return false;

            if (isOverSwatch(i, j, k)) {
                if (event.button() == 0) {
                    playClick();
                    this.primary = type;
                    Services.NETWORK.sendToServer(new FluidIdControlPayload(true, fluidId(type)));
                    return true;
                } else if (event.button() == 1) {
                    playClick();
                    this.secondary = type;
                    Services.NETWORK.sendToServer(new FluidIdControlPayload(false, fluidId(type)));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (heldIdentifier(this.player).isEmpty()) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isOverSwatch(int mx, int my, int k) {
        int x = guiLeft + 7 + k * 18, y = guiTop + 29;
        return mx >= x && mx < x + 18 && my >= y && my < y + 18;
    }
}
