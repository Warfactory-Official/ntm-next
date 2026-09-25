// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import com.hbm.compat.scalablelux.ScalableLuxBatch;

public interface ScalableLuxBatchQueue {
    ChunkTasks hbm$enqueueLightBatch(ScalableLuxBatch batch);
}
