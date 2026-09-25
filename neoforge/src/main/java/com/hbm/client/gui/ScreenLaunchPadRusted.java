// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.entity.missile.EntityMissileTier4.EntityMissileDoomsdayRusted;
import com.hbm.inventory.container.MenuLaunchPadRusted;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadRusted;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ScreenLaunchPadRusted extends ScreenInfoContainer<MenuLaunchPadRusted> {
    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/gui_launch_pad_rusted.png");
    private static final int RELEASE_X = 26, RELEASE_Y = 36, RELEASE_SIZE = 16;

    public ScreenLaunchPadRusted(MenuLaunchPadRusted menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 236);
        titleLabelY = 4;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
        BlockEntityLaunchPadRusted pad = pad();
        if (pad != null) {
            boolean codes =
                    menu.getSlot(BlockEntityLaunchPadRusted.SLOT_CODE)
                            .getItem()
                            .is(ModItems.LAUNCH_CODE.get());
            boolean key =
                    menu.getSlot(BlockEntityLaunchPadRusted.SLOT_KEY)
                            .getItem()
                            .is(ModItems.LAUNCH_KEY.get());
            if (codes)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 121, 32, 192, 0, 6, 8, 256, 256);
            if (key)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 139, 32, 192, 0, 6, 8, 256, 256);
            if (codes && key && pad.missileLoaded) {

                int launchCode =
                        new Random(pad.getBlockPos().getX() * 131_071 + pad.getBlockPos().getZ())
                                .nextInt(100_000_000);
                for (int i = 0; i < 8; i++) {
                    int magnitude = (int) Math.pow(10, i);
                    int digit = launchCode % (magnitude * 10) / magnitude;
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            109 + 6 * i,
                            85,
                            192 + 6 * digit,
                            8,
                            6,
                            8,
                            256,
                            256);
                }
            }
            if (pad.missileLoaded) drawMissilePreview(graphics);
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                RELEASE_X,
                RELEASE_Y,
                RELEASE_SIZE,
                RELEASE_SIZE,
                List.of(
                        Component.translatable("desc.gui.launchPadRusted.releaseMissile"),
                        Component.translatable("desc.gui.launchPadRusted.missileIsLockedIn"),
                        Component.translatable("desc.gui.launchPadRusted.releasingMayCauseDamage"),
                        Component.translatable("desc.gui.launchPadRusted.damagedMissileCanNot"),
                        Component.translatable("desc.gui.launchPadRusted.intoLaunchingPosition")));
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityLaunchPadRusted pad() {
        return minecraft != null
                        && minecraft.level != null
                        && minecraft.level.getBlockEntity(menu.blockEntity().getBlockPos())
                                instanceof BlockEntityLaunchPadRusted pad
                ? pad
                : null;
    }

    private void drawMissilePreview(GuiGraphicsExtractor graphics) {
        if (minecraft == null || minecraft.level == null) return;
        EntityMissileDoomsdayRusted missile = new EntityMissileDoomsdayRusted(minecraft.level);
        EntityRenderState state =
                minecraft
                        .getEntityRenderDispatcher()
                        .getRenderer(missile)
                        .createRenderState(missile, 0F);
        state.shadowPieces.clear();

        submitEntity(
                graphics,
                state,
                7F,
                new Vector3f(),
                new Quaternionf().rotateZ(Mth.PI).rotateY((float) Math.toRadians(90)),
                34,
                32,
                72,
                176);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick(
                (int) event.x(),
                (int) event.y(),
                RELEASE_X,
                RELEASE_Y,
                RELEASE_SIZE,
                RELEASE_SIZE)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("release", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            if (minecraft != null)
                minecraft
                        .getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
