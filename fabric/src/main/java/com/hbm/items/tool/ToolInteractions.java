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

package com.hbm.items.tool;

import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ToolInteractions {

    private ToolInteractions() {}

    public static boolean useOn(UseOnContext ctx, ToolType tool) {
        Level level = ctx.getLevel();
        NtmContracts.Target<ToolInteractionHandler> target =
                NtmContracts.TOOLABLE.target(level, ctx.getClickedPos());
        return target != null
                && target.handler()
                        .onToolUse(
                                level,
                                ctx.getPlayer(),
                                target.pos(),
                                ctx.getClickedPos(),
                                ctx.getClickedFace(),
                                ctx.getClickLocation(),
                                tool);
    }
}
