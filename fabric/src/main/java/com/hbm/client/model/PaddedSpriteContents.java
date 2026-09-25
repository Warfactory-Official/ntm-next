// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
