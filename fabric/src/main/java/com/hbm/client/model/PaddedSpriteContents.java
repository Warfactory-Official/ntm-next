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

package com.hbm.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public final class PaddedSpriteContents extends SpriteContents {

    private final float uScale;
    private final float vScale;

    public PaddedSpriteContents(
            Identifier name,
            FrameSize frameSize,
            NativeImage image,
            Optional<AnimationMetadataSection> animationInfo,
            List<MetadataSectionType.WithValue<?>> additionalMetadata,
            Optional<TextureMetadataSection> textureInfo,
            float uScale,
            float vScale) {
        super(name, frameSize, image, animationInfo, additionalMetadata, textureInfo);
        this.uScale = uScale;
        this.vScale = vScale;
    }

    public float uScale() {
        return uScale;
    }

    public float vScale() {
        return vScale;
    }
}
