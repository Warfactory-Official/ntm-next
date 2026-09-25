// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.render.MeshItemRenderer;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.container.MenuMachineMissileAssembly;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineMissileAssembly;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class ScreenMachineMissileAssembly extends ScreenInfoContainer<MenuMachineMissileAssembly> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/gui_missile_assembly.png");

    private static final int LAMP_W = 6, LAMP_H = 8, LAMP_Y = 23;
    private static final int LAMP_LIT_U = 194, LAMP_BAD_U = 200;
    private static final int BUILD_X = 115, BUILD_Y = 35, BUILD_SIZE = 18, BUILD_U = 176;

    private static final MeshItemRenderer.Spin CLOCK =
            new MeshItemRenderer.Spin(10L, 360L, 1F, Optional.empty());

    private static final int PREVIEW_CX = 88, PREVIEW_CY = 98, PREVIEW_HALF = 72;
    private static final float PREVIEW_SIZE = 8 * 18F, PREVIEW_MIN_HEIGHT = 6F;

    private @Nullable EntityMissileCustom previewEntity;

    public ScreenMachineMissileAssembly(
            MenuMachineMissileAssembly menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        BlockEntityMachineMissileAssembly be = menu.blockEntity();

        if (be.fuselageState() == 1) lamp(graphics, 49, LAMP_LIT_U);
        if (be.warheadState() == 1) lamp(graphics, 31, LAMP_LIT_U);
        if (be.chipState() == 1) lamp(graphics, 13, LAMP_LIT_U);
        if (be.stabilityState() == 1) lamp(graphics, 67, LAMP_LIT_U);
        if (be.stabilityState() == 0) lamp(graphics, 67, LAMP_BAD_U);
        if (be.thrusterState() == 1) lamp(graphics, 85, LAMP_LIT_U);

        if (be.canBuild()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BUILD_X,
                    BUILD_Y,
                    BUILD_U,
                    0,
                    BUILD_SIZE,
                    BUILD_SIZE,
                    256,
                    256);
        }

        drawMissilePreview(graphics);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawMissilePreview(GuiGraphicsExtractor graphics) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        ItemStack warhead = menu.getSlot(BlockEntityMachineMissileAssembly.SLOT_WARHEAD).getItem();
        ItemStack fuselage =
                menu.getSlot(BlockEntityMachineMissileAssembly.SLOT_FUSELAGE).getItem();
        ItemStack fins = menu.getSlot(BlockEntityMachineMissileAssembly.SLOT_FINS).getItem();
        ItemStack thruster =
                menu.getSlot(BlockEntityMachineMissileAssembly.SLOT_THRUSTER).getItem();

        if (previewEntity == null) {
            previewEntity =
                    new EntityMissileCustom(ModEntities.MISSILE_CUSTOM.get(), this.minecraft.level);
        }
        previewEntity.setParts(warhead, fuselage, fins, thruster);

        MissileStruct parts = previewEntity.getStruct();
        float height = parts.height();

        if (height == 0F && parts.fins() == null) return;

        float scale = PREVIEW_SIZE / Math.max(height, PREVIEW_MIN_HEIGHT);
        float spin = (float) Math.toRadians(CLOCK.degrees());

        Vector3f translation =
                new Vector3f(height / 2F * Mth.cos(spin), 0F, -height / 2F * Mth.sin(spin));
        Quaternionf rotation =
                new Quaternionf()
                        .rotateZ(Mth.PI)
                        .rotateY(-spin)
                        .rotateX(Mth.HALF_PI)
                        .rotateZ(-Mth.HALF_PI);

        EntityRenderState rs =
                this.minecraft
                        .getEntityRenderDispatcher()
                        .getRenderer(previewEntity)
                        .createRenderState(previewEntity, 0F);
        rs.shadowPieces.clear();

        submitEntity(
                graphics,
                rs,
                scale,
                translation,
                rotation,
                PREVIEW_CX - PREVIEW_HALF,
                PREVIEW_CY - PREVIEW_HALF,
                PREVIEW_HALF * 2,
                PREVIEW_HALF * 2);
    }

    private void lamp(GuiGraphicsExtractor graphics, int x, int u) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, LAMP_Y, u, 0, LAMP_W, LAMP_H, 256, 256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        if (event.button() == 0
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        BUILD_X,
                        BUILD_Y,
                        BUILD_SIZE,
                        BUILD_SIZE)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("build", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
