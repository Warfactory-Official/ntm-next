// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPneumoStorageAccess.ClickOp;
import com.hbm.inventory.container.MenuPneumoStorageAccess.SlotPneumo;
import com.hbm.inventory.container.MenuPneumoStorageAccess;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.PneumoAccessInputPayload;
import com.hbm.platform.Services;
import com.hbm.util.BobMathUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ScreenPneumoStorageAccess extends ScreenInfoContainer<MenuPneumoStorageAccess> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_pneumatic_access.png");
    private static final int PANEL_X = 34;
    private static final int SCROLL_BAR_X = 154;
    private static final int SCROLL_TRAVEL = 106 - 15;
    private static final int DRAG_TRAVEL = 92;

    private static int sorting = 0;
    private static boolean startFocussed = false;
    private static boolean detailedSearch = false;

    private EditBox search;
    private int scrollBounds = 1;
    private boolean draggingScroll;

    public ScreenPneumoStorageAccess(
            MenuPneumoStorageAccess menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176 + PANEL_X, 251);
        this.titleLabelY = 5;
        this.inventoryLabelX = PANEL_X + 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static Component onOff(boolean on) {
        return Component.translatable(
                on ? "desc.gui.pneumoStorageAccess.on" : "desc.gui.pneumoStorageAccess.off");
    }

    @Override
    protected void init() {
        super.init();

        this.search =
                new EditBox(
                        this.font, leftPos + PANEL_X + 45, topPos + 129, 86, 12, Component.empty());
        this.search.setBordered(false);
        this.search.setTextColor(CommonColors.WHITE);
        this.search.setMaxLength(50);
        this.search.setResponder(term -> menu.setFilter(searchFilter(term)));
        addRenderableWidget(this.search);

        if (startFocussed) setFocused(this.search);

        menu.setSorter(sorterFor(sorting));
        menu.setFilter(searchFilter(""));
    }

    private Predicate<ItemStack> searchFilter(String term) {
        if (term.isEmpty()) return stack -> true;
        String needle = term.toLowerCase(Locale.US);

        return stack -> {
            if (stack.getHoverName().getString().toLowerCase(Locale.US).contains(needle))
                return true;
            if (!detailedSearch) return false;
            for (Component line : getTooltipFromContainerItem(stack)) {
                if (line.getString().toLowerCase(Locale.US).contains(needle)) return true;
            }
            return false;
        };
    }

    private Comparator<MenuPneumoStorageAccess.Entry> sorterFor(int index) {
        return switch (index) {
            case 1 -> MenuPneumoStorageAccess.SORT_BY_ID;
            case 2 -> MenuPneumoStorageAccess.SORT_BY_LOCALIZED;
            case 3 -> MenuPneumoStorageAccess.SORT_BY_INTERNAL;
            default -> MenuPneumoStorageAccess.SORT_BY_STACK_SIZE;
        };
    }

    @Override
    protected int titleCenterX() {
        return PANEL_X + 176 / 2;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos + PANEL_X,
                topPos,
                0.0F,
                0.0F,
                176,
                imageHeight,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                176.0F,
                15.0F,
                32,
                122,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.scrollBounds = Math.max(1, (int) Math.ceil(menu.listingSize() / 8.0D - 6.0D));
        if (menu.listingStart() > scrollBounds) setScroll(scrollBounds);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                7,
                7 + sorting * 18,
                208.0F,
                0.0F,
                18,
                18,
                256,
                256);
        if (startFocussed) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 79, 208.0F, 18.0F, 18, 18, 256, 256);
        }
        if (detailedSearch) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 97, 208.0F, 18.0F, 18, 18, 256, 256);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                PANEL_X + SCROLL_BAR_X,
                scrollKnobY(),
                draggingScroll ? 188.0F : 176.0F,
                0.0F,
                12,
                15,
                256,
                256);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                7,
                7,
                18,
                18,
                List.of(Component.translatable("desc.gui.pneumoStorageAccess.sortAmount")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                7,
                25,
                18,
                18,
                List.of(Component.translatable("desc.gui.pneumoStorageAccess.sortId")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                7,
                43,
                18,
                18,
                List.of(Component.translatable("desc.gui.pneumoStorageAccess.sortName")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                7,
                61,
                18,
                18,
                List.of(Component.translatable("desc.gui.pneumoStorageAccess.sortInternal")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                7,
                79,
                18,
                18,
                List.of(
                        Component.translatable(
                                "desc.gui.pneumoStorageAccess.focusSearch", onOff(startFocussed))));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                7,
                97,
                18,
                18,
                List.of(
                        Component.translatable(
                                "desc.gui.pneumoStorageAccess.searchTooltips",
                                onOff(detailedSearch))));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);

        for (Slot slot : menu.slots) {
            if (!(slot instanceof SlotPneumo pneumo) || !pneumo.hasItem()) continue;
            String label = BobMathUtil.getShortNumber(pneumo.amount);
            graphics.text(
                    font,
                    label,
                    (pneumo.x + 16) * 2 - font.width(label),
                    (pneumo.y + 16) * 2 - 9,
                    -1,
                    true);
        }

        graphics.pose().popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty() || hoveredSlot == null || !hoveredSlot.hasItem()) return;

        ItemStack stack = hoveredSlot.getItem();
        List<Component> tooltip = getTooltipFromContainerItem(stack);
        List<Component> lines = new ArrayList<>(tooltip.size());

        for (int i = 0; i < tooltip.size(); i++) {
            lines.add(
                    i == 0
                            ? Component.empty()
                                    .append(tooltip.get(i))
                                    .withStyle(stack.getRarity().color())
                            : Component.empty()
                                    .append(tooltip.get(i))
                                    .withStyle(ChatFormatting.GRAY));
        }

        GUIElements.drawHoveringText(
                graphics,
                font,
                lines,
                mouseX,
                mouseY,
                width,
                height,
                GUIElements.STANDARD_HEADER_OFFSET,
                GUIElements.STANDARD_LINE_DIST,
                GUIElements.STANDARD_COLOR_BACKGROUND,
                GUIElements.STANDARD_COLOR_BACKGROUND,
                0xFFD57C4F,
                0xFFAB4223);
    }

    private int scrollKnobY() {
        double progress = (double) menu.listingStart() / scrollBounds;
        return 17 + (int) (Math.min(1.0D, progress) * SCROLL_TRAVEL);
    }

    private void setScroll(int scroll) {
        int clamped = Mth.clamp(scroll, 0, scrollBounds);
        if (clamped != menu.listingStart()) menu.setListingStart(clamped);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        if (checkClick(x, y, PANEL_X + 153, 16, 14, 108)) draggingScroll = true;

        if (checkClick(x, y, 7, 7, 18, 18)) sort(0);
        if (checkClick(x, y, 7, 25, 18, 18)) sort(1);
        if (checkClick(x, y, 7, 43, 18, 18)) sort(2);
        if (checkClick(x, y, 7, 61, 18, 18)) sort(3);

        if (checkClick(x, y, 7, 79, 18, 18)) {
            playClick();
            startFocussed = !startFocussed;
        }
        if (checkClick(x, y, 7, 97, 18, 18)) {
            playClick();
            detailedSearch = !detailedSearch;
            menu.setFilter(searchFilter(search.getValue()));
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void sort(int index) {
        playClick();
        sorting = index;
        menu.setSorter(sorterFor(index));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScroll) {

            int travel = Mth.clamp((int) event.y() - topPos - 24, 0, DRAG_TRAVEL);
            setScroll((int) Math.round(scrollBounds * ((double) travel / DRAG_TRAVEL)));
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && checkClick((int) mouseX, (int) mouseY, 0, 0, imageWidth, imageHeight)) {
            setScroll(menu.listingStart() - (int) Math.signum(scrollY));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void slotClicked(
            Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot == null || !(slot instanceof SlotPneumo pneumo)) {
            super.slotClicked(slot, slotId, buttonNum, containerInput);
            return;
        }

        int entry = pneumo.entryId;

        if (containerInput == ContainerInput.QUICK_MOVE) {
            send(ClickOp.SHIFT_CLICK, entry);
            return;
        }

        if (containerInput != ContainerInput.PICKUP || buttonNum > 1) return;

        boolean rightClick = buttonNum == 1;
        ItemStack carried = menu.getCarried();
        boolean sameType =
                pneumo.hasItem() && ItemStack.isSameItemSameComponents(pneumo.getItem(), carried);

        if (!carried.isEmpty() && !sameType) {
            menu.setCarried(
                    rightClick ? carried.copyWithCount(carried.getCount() - 1) : ItemStack.EMPTY);
        } else if (!carried.isEmpty()) {
            long grab = rightClick ? 1 : pneumo.amount;
            menu.setCarried(
                    carried.copyWithCount(
                            (int) Math.min(carried.getMaxStackSize(), carried.getCount() + grab)));
        } else if (pneumo.hasItem()) {
            ItemStack stack = pneumo.getItem();
            menu.setCarried(
                    stack.copyWithCount(
                            rightClick
                                    ? 1
                                    : (int) Math.min(pneumo.amount, stack.getMaxStackSize())));
        }

        send(rightClick ? ClickOp.RIGHT_CLICK : ClickOp.LEFT_CLICK, entry);
    }

    private void send(ClickOp op, int entryId) {
        Services.NETWORK.sendToServer(new PneumoAccessInputPayload(menu.containerId, op, entryId));
    }
}
