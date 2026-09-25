// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.client.gui.PwrSliceRenderer;
import com.hbm.wiaj.JarSceneRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PictureInPictureRenderer<?>> hbm$printerRenderer(
            List<PictureInPictureRenderer<?>> original) {
        List<PictureInPictureRenderer<?>> renderers = new ArrayList<>(original);
        renderers.add(new PwrSliceRenderer());
        renderers.add(new JarSceneRenderer());
        return renderers;
    }
}
