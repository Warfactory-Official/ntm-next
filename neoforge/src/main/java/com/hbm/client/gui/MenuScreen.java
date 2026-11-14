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

package com.hbm.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class MenuScreen<M extends AbstractContainerMenu> extends Screen
        implements MenuAccess<M> {

    protected final M menu;

    protected MenuScreen(M menu, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public M getMenu() {
        return menu;
    }

    @Override
    public void removed() {
        if (minecraft.player != null) menu.removed(minecraft.player);
    }

    @Override
    public void onClose() {
        minecraft.player.closeContainer();
        super.onClose();
    }
}
