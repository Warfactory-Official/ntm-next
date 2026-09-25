// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.threading;

import net.minecraft.server.level.ServerLevel;

public record TargetPoint(ServerLevel level, double x, double y, double z, double radius) {}
