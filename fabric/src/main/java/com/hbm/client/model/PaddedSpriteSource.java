// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;

public record PaddedSpriteSource(Identifier resourceId, Optional<Identifier> spriteId)
        implements SpriteSource {

    public static final MapCodec<PaddedSpriteSource> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Identifier.CODEC
                                                    .fieldOf("resource")
                                                    .forGetter(PaddedSpriteSource::resourceId),
                                            Identifier.CODEC
                                                    .optionalFieldOf("sprite")
                                                    .forGetter(PaddedSpriteSource::spriteId))
                                    .apply(instance, PaddedSpriteSource::new));
    private static final Logger LOGGER = LogUtils.getLogger();

    private static PaddedSpriteContents load(
            Identifier spriteLocation,
            Identifier resourceLocation,
            Resource resource,
            Set<MetadataSectionType<?>> additionalMetadata) {
        try {
            ResourceMetadata metadata = resource.metadata();
            Optional<AnimationMetadataSection> animationInfo =
                    metadata.getSection(AnimationMetadataSection.TYPE);
            if (animationInfo.isPresent()) {
                throw new IOException(
                        "Animated padded atlas sprites are unsupported: " + resourceLocation);
            }

            Optional<TextureMetadataSection> textureInfo =
                    metadata.getSection(TextureMetadataSection.TYPE);
            List<MetadataSectionType.WithValue<?>> extraMetadata =
                    metadata.getTypedSections(additionalMetadata);
            try (NativeImage source = readImage(resource)) {
                int width = source.getWidth();
                int height = source.getHeight();
                int mipLevels = Minecraft.getInstance().options.mipmapLevels().get();
                int requiredMultiple = mipLevels <= 0 ? 1 : 1 << mipLevels;
                int size = Math.max(width, height);
                int remainder = size % requiredMultiple;
                if (remainder != 0) size += requiredMultiple - remainder;

                NativeImage padded = new NativeImage(NativeImage.Format.RGBA, size, size, false);
                for (int y = 0; y < size; y++) {
                    int sourceY = Math.min(y, height - 1);
                    for (int x = 0; x < size; x++) {
                        padded.setPixel(x, y, source.getPixel(Math.min(x, width - 1), sourceY));
                    }
                }

                return new PaddedSpriteContents(
                        spriteLocation,
                        new FrameSize(size, size),
                        padded,
                        Optional.empty(),
                        extraMetadata,
                        textureInfo,
                        width / (float) size,
                        height / (float) size);
            }
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Unable to load padded sprite {}", resourceLocation, e);
            return null;
        }
    }

    private static NativeImage readImage(Resource resource) throws IOException {
        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        }
    }

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        run(resourceManager, output, Set.of());
    }

    public void run(
            ResourceManager resourceManager,
            Output output,
            Set<MetadataSectionType<?>> additionalMetadata) {
        Identifier resourceLocation = TEXTURE_ID_CONVERTER.idToFile(resourceId);
        Optional<Resource> resource = resourceManager.getResource(resourceLocation);
        if (resource.isEmpty()) {
            LOGGER.warn("Missing padded sprite: {}", resourceLocation);
            return;
        }

        Identifier spriteLocation = spriteId.orElse(resourceId);
        output.add(
                spriteLocation,
                loader ->
                        load(spriteLocation, resourceLocation, resource.get(), additionalMetadata));
    }

    @Override
    public MapCodec<PaddedSpriteSource> codec() {
        return MAP_CODEC;
    }
}
