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

package com.hbm.integration.recipeviewer;

import net.minecraft.network.chat.Component;

public interface PageLayout {

    PageSlot input(int x, int y);

    PageSlot output(int x, int y);

    PageSlot catalyst(int x, int y);

    PageSlot display(int x, int y);

    default PageSlot lookupInput() {
        throw new UnsupportedOperationException(
                "Layout does not support ingredients without a drawn slot");
    }

    default PageSlot lookupOutput() {
        throw new UnsupportedOperationException(
                "Layout does not support ingredients without a drawn slot");
    }

    void arrow(int x, int y);

    void text(Component text, int x, int y, int width, int height);
}
