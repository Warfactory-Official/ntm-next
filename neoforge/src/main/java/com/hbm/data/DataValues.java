// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.mojang.serialization.Dynamic;
import java.util.Map;

public interface DataValues {

    Map<String, Dynamic<?>> values();
}
