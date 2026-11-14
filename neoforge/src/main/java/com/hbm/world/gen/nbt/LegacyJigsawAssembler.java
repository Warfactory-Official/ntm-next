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

package com.hbm.world.gen.nbt;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SequencedPriorityIterator;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class LegacyJigsawAssembler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AABB outerBounds;
    private final JigsawFreeSpace outerFree;
    private final @Nullable LegacyStartPoolElement legacy;
    private final BlockPos start;
    private final IdentityHashMap<PoolElementStructurePiece, BlockPos> incoming =
            new IdentityHashMap<>();

    public static void assemble(
            RandomState randomState,
            int maxDepth,
            boolean expansion,
            ChunkGenerator generator,
            StructureTemplateManager templates,
            LevelHeightAccessor height,
            RandomSource random,
            Registry<StructureTemplatePool> pools,
            PoolElementStructurePiece start,
            List<PoolElementStructurePiece> pieces,
            AABB bounds,
            PoolAliasLookup aliases,
            LiquidSettings liquid) {
        LegacyJigsawAssembler assembler =
                new LegacyJigsawAssembler(
                        pools, maxDepth, generator, templates, pieces, random, bounds, start);
        assembler.tryPlacingChildren(
                start, assembler.outerFree, 0, expansion, height, randomState, aliases, liquid);
        while (assembler.placing.hasNext()) {
            PieceState next = assembler.placing.next();
            assembler.tryPlacingChildren(
                    next.piece,
                    next.free,
                    next.depth,
                    expansion,
                    height,
                    randomState,
                    aliases,
                    liquid);
        }
    }

    private record PieceState(PoolElementStructurePiece piece, JigsawFreeSpace free, int depth) {}

    private final Registry<StructureTemplatePool> pools;
    private final int maxDepth;
    private final ChunkGenerator chunkGenerator;
    private final StructureTemplateManager structureTemplateManager;
    private final List<PoolElementStructurePiece> pieces;
    private final RandomSource random;
    private final SequencedPriorityIterator<PieceState> placing = new SequencedPriorityIterator<>();

    private LegacyJigsawAssembler(
            final Registry<StructureTemplatePool> pools,
            final int maxDepth,
            final ChunkGenerator chunkGenerator,
            final StructureTemplateManager structureTemplateManager,
            final List<PoolElementStructurePiece> pieces,
            final RandomSource random,
            final AABB bounds,
            final PoolElementStructurePiece startPiece) {
        this.pools = pools;
        this.maxDepth = maxDepth;
        this.chunkGenerator = chunkGenerator;
        this.structureTemplateManager = structureTemplateManager;
        this.pieces = pieces;
        this.random = random;
        this.outerBounds = bounds;
        this.outerFree = JigsawFreeSpace.of(bounds);
        for (PoolElementStructurePiece piece : pieces) outerFree.subtract(piece.getBoundingBox());
        this.legacy =
                startPiece.getElement() instanceof LegacyStartPoolElement element ? element : null;
        this.start = startPiece.getPosition();
    }

    private void tryPlacingChildren(
            final PoolElementStructurePiece sourcePiece,
            final JigsawFreeSpace contextFree,
            final int depth,
            final boolean doExpansionHack,
            final LevelHeightAccessor heightAccessor,
            final RandomState randomState,
            final PoolAliasLookup poolAliasLookup,
            final LiquidSettings liquidSettings) {
        StructurePoolElement sourceElement = sourcePiece.getElement();
        BlockPos sourceBoxPosition = sourcePiece.getPosition();
        Rotation sourceRotation = sourcePiece.getRotation();
        StructureTemplatePool.Projection sourceProjection = sourceElement.getProjection();
        boolean sourceRigid = sourceProjection == StructureTemplatePool.Projection.RIGID;
        JigsawFreeSpace sourceFree = null;
        BoundingBox sourceBB = sourcePiece.getBoundingBox();
        int sourceBoxY = sourceBB.minY();
        BlockPos center = sourceBB.getCenter();
        int distance =
                Math.max(
                        Math.abs(center.getX() - start.getX()),
                        Math.abs(center.getZ() - start.getZ()));
        boolean fallbacksOnly =
                legacy != null
                        && (pieces.size() >= legacy.sizeLimit() || distance >= legacy.rangeLimit());

        nextConnector:
        for (StructureTemplate.JigsawBlockInfo sourceJigsaw :
                sourceElement.getShuffledJigsawBlocks(
                        this.structureTemplateManager,
                        sourceBoxPosition,
                        sourceRotation,
                        this.random)) {
            Direction sourceDirection = JigsawBlock.getFrontFacing(sourceJigsaw.state());
            BlockPos sourceJigsawPos = sourceJigsaw.pos();
            BlockPos targetJigsawPos = sourceJigsawPos.relative(sourceDirection);
            int sourceJigsawLocalY = sourceJigsawPos.getY() - sourceBoxY;
            int sourceJigsawBaseHeight = Integer.MIN_VALUE;
            ResourceKey<StructureTemplatePool> poolName =
                    poolAliasLookup.lookup(sourceJigsaw.pool());
            Optional<? extends Holder<StructureTemplatePool>> maybeTargetPool =
                    this.pools.get(poolName);
            if (maybeTargetPool.isEmpty()) {
                LOGGER.warn("Empty or non-existent pool: {}", poolName.identifier());
            } else {
                Holder<StructureTemplatePool> targetPool =
                        (Holder<StructureTemplatePool>) maybeTargetPool.get();
                if (targetPool.value().size() == 0 && !targetPool.is(Pools.EMPTY)) {
                    LOGGER.warn("Empty or non-existent pool: {}", poolName.identifier());
                } else {
                    Holder<StructureTemplatePool> fallback = targetPool.value().getFallback();
                    if (fallback.value().size() == 0 && !fallback.is(Pools.EMPTY)) {
                        LOGGER.warn(
                                "Empty or non-existent fallback pool: {}",
                                fallback.unwrapKey()
                                        .map(e -> e.identifier().toString())
                                        .orElse("<unregistered>"));
                    } else {
                        boolean attachInsideSource = sourceBB.isInside(targetJigsawPos);
                        JigsawFreeSpace childrenFree;
                        if (attachInsideSource) {
                            if (sourceFree == null)
                                sourceFree = JigsawFreeSpace.of(AABB.of(sourceBB));
                            childrenFree = sourceFree;
                        } else {
                            childrenFree = contextFree;
                        }

                        List<StructurePoolElement> targetPieces = new ArrayList<>();
                        if ((legacy != null || depth != this.maxDepth) && !fallbacksOnly) {
                            targetPieces.addAll(
                                    targetPool.value().getShuffledTemplates(this.random));
                        }

                        int mainCandidates = targetPieces.size();
                        targetPieces.addAll(fallback.value().getShuffledTemplates(this.random));
                        int candidate = 0;
                        int placementPriority = sourceJigsaw.placementPriority();

                        for (StructurePoolElement targetElement : targetPieces) {
                            if (targetElement == EmptyPoolElement.INSTANCE) {
                                break;
                            }

                            boolean isFallback = ++candidate > mainCandidates;
                            for (Rotation targetRotation : Rotation.getShuffled(this.random)) {
                                List<StructureTemplate.JigsawBlockInfo> targetJigsaws =
                                        targetElement.getShuffledJigsawBlocks(
                                                this.structureTemplateManager,
                                                BlockPos.ZERO,
                                                targetRotation,
                                                this.random);
                                BoundingBox hackBox =
                                        targetElement.getBoundingBox(
                                                this.structureTemplateManager,
                                                BlockPos.ZERO,
                                                targetRotation);
                                int expandTo;
                                if (doExpansionHack && hackBox.getYSpan() <= 16) {
                                    expandTo =
                                            targetJigsaws.stream()
                                                    .mapToInt(
                                                            targetJigsawx -> {
                                                                if (!hackBox.isInside(
                                                                        targetJigsawx
                                                                                .pos()
                                                                                .relative(
                                                                                        JigsawBlock
                                                                                                .getFrontFacing(
                                                                                                        targetJigsawx
                                                                                                                .state())))) {
                                                                    return 0;
                                                                }

                                                                ResourceKey<StructureTemplatePool>
                                                                        childPoolName =
                                                                                poolAliasLookup
                                                                                        .lookup(
                                                                                                targetJigsawx
                                                                                                        .pool());
                                                                Optional<
                                                                                ? extends
                                                                                        Holder<
                                                                                                StructureTemplatePool>>
                                                                        childPool =
                                                                                this.pools.get(
                                                                                        childPoolName);
                                                                Optional<
                                                                                Holder<
                                                                                        StructureTemplatePool>>
                                                                        childFallbackPool =
                                                                                childPool.map(
                                                                                        p ->
                                                                                                p.value()
                                                                                                        .getFallback());
                                                                int childPoolSize =
                                                                        childPool
                                                                                .<Integer>map(
                                                                                        p ->
                                                                                                p.value()
                                                                                                        .getMaxSize(
                                                                                                                this
                                                                                                                        .structureTemplateManager))
                                                                                .orElse(0);
                                                                int childFallbackSize =
                                                                        childFallbackPool
                                                                                .<Integer>map(
                                                                                        p ->
                                                                                                p.value()
                                                                                                        .getMaxSize(
                                                                                                                this
                                                                                                                        .structureTemplateManager))
                                                                                .orElse(0);
                                                                return Math.max(
                                                                        childPoolSize,
                                                                        childFallbackSize);
                                                            })
                                                    .max()
                                                    .orElse(0);
                                } else {
                                    expandTo = 0;
                                }

                                for (StructureTemplate.JigsawBlockInfo targetJigsaw :
                                        targetJigsaws) {
                                    if (JigsawBlock.canAttach(sourceJigsaw, targetJigsaw)) {
                                        BlockPos targetJigsawLocalPos = targetJigsaw.pos();
                                        BlockPos rawTargetBoxPos =
                                                targetJigsawPos.subtract(targetJigsawLocalPos);
                                        BoundingBox rawTargetBB =
                                                targetElement.getBoundingBox(
                                                        this.structureTemplateManager,
                                                        rawTargetBoxPos,
                                                        targetRotation);
                                        int rawTargetY = rawTargetBB.minY();
                                        StructureTemplatePool.Projection targetProjection =
                                                targetElement.getProjection();
                                        boolean targetRigid =
                                                targetProjection
                                                        == StructureTemplatePool.Projection.RIGID;
                                        int targetJigsawLocalY = targetJigsawLocalPos.getY();
                                        int deltaY =
                                                sourceJigsawLocalY
                                                        - targetJigsawLocalY
                                                        + JigsawBlock.getFrontFacing(
                                                                        sourceJigsaw.state())
                                                                .getStepY();
                                        int targetBoxY;
                                        if (sourceRigid && targetRigid) {
                                            targetBoxY = sourceBoxY + deltaY;
                                        } else {
                                            if (sourceJigsawBaseHeight == Integer.MIN_VALUE) {
                                                sourceJigsawBaseHeight =
                                                        this.chunkGenerator.getFirstFreeHeight(
                                                                sourceJigsawPos.getX(),
                                                                sourceJigsawPos.getZ(),
                                                                Heightmap.Types.WORLD_SURFACE_WG,
                                                                heightAccessor,
                                                                randomState);
                                            }

                                            targetBoxY =
                                                    sourceJigsawBaseHeight - targetJigsawLocalY;
                                        }

                                        int yOffset = targetBoxY - rawTargetY;
                                        BoundingBox targetBB = rawTargetBB.moved(0, yOffset, 0);
                                        BlockPos targetBoxPosition =
                                                rawTargetBoxPos.offset(0, yOffset, 0);
                                        if (expandTo > 0) {
                                            int newSize =
                                                    Math.max(
                                                            expandTo + 1,
                                                            targetBB.maxY() - targetBB.minY());
                                            targetBB.encapsulate(
                                                    new BlockPos(
                                                            targetBB.minX(),
                                                            targetBB.minY() + newSize,
                                                            targetBB.minZ()));
                                        }

                                        boolean accepted;
                                        if (legacy != null && isFallback) {
                                            accepted =
                                                    !outsideOuterBounds(targetBB)
                                                            && (fallbacksOnly
                                                                    ? !sourceJigsawPos.equals(
                                                                            incoming.get(
                                                                                    sourcePiece))
                                                                    : !otherPieceContains(
                                                                            sourcePiece,
                                                                            targetJigsawPos));
                                        } else if (sourceElement
                                                        instanceof ParentOverlapPoolElement element
                                                && element.allowsParentOverlap(
                                                        sourceJigsaw.name())) {
                                            accepted =
                                                    !outsideOuterBounds(targetBB)
                                                            && !otherPieceIntersects(
                                                                    sourcePiece, targetBB);
                                            if (accepted) childrenFree = outerFree;
                                        } else {
                                            accepted = childrenFree.contains(targetBB);
                                        }
                                        if (accepted) {
                                            childrenFree.subtract(targetBB);
                                            int sourceGroundLevelDelta =
                                                    sourcePiece.getGroundLevelDelta();
                                            int targetGroundLevelDelta;
                                            if (targetRigid) {
                                                targetGroundLevelDelta =
                                                        sourceGroundLevelDelta - deltaY;
                                            } else {
                                                targetGroundLevelDelta =
                                                        targetElement.getGroundLevelDelta();
                                            }

                                            PoolElementStructurePiece targetPiece =
                                                    new PoolElementStructurePiece(
                                                            this.structureTemplateManager,
                                                            targetElement,
                                                            targetBoxPosition,
                                                            targetGroundLevelDelta,
                                                            targetRotation,
                                                            targetBB,
                                                            liquidSettings);
                                            int junctionY;
                                            if (sourceRigid) {
                                                junctionY = sourceBoxY + sourceJigsawLocalY;
                                            } else if (targetRigid) {
                                                junctionY = targetBoxY + targetJigsawLocalY;
                                            } else {
                                                if (sourceJigsawBaseHeight == Integer.MIN_VALUE) {
                                                    sourceJigsawBaseHeight =
                                                            this.chunkGenerator.getFirstFreeHeight(
                                                                    sourceJigsawPos.getX(),
                                                                    sourceJigsawPos.getZ(),
                                                                    Heightmap.Types
                                                                            .WORLD_SURFACE_WG,
                                                                    heightAccessor,
                                                                    randomState);
                                                }

                                                junctionY = sourceJigsawBaseHeight + deltaY / 2;
                                            }

                                            sourcePiece.addJunction(
                                                    new JigsawJunction(
                                                            targetJigsawPos.getX(),
                                                            junctionY
                                                                    - sourceJigsawLocalY
                                                                    + sourceGroundLevelDelta,
                                                            targetJigsawPos.getZ(),
                                                            deltaY,
                                                            targetProjection));
                                            targetPiece.addJunction(
                                                    new JigsawJunction(
                                                            sourceJigsawPos.getX(),
                                                            junctionY
                                                                    - targetJigsawLocalY
                                                                    + targetGroundLevelDelta,
                                                            sourceJigsawPos.getZ(),
                                                            -deltaY,
                                                            sourceProjection));
                                            this.pieces.add(targetPiece);
                                            if (legacy != null)
                                                incoming.put(targetPiece, targetJigsawPos);
                                            if (legacy != null || depth + 1 <= this.maxDepth) {
                                                PieceState state =
                                                        new PieceState(
                                                                targetPiece,
                                                                childrenFree,
                                                                depth + 1);
                                                this.placing.add(state, placementPriority);
                                            }
                                            continue nextConnector;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean outsideOuterBounds(BoundingBox box) {
        return box.minX() < outerBounds.minX
                || box.minY() < outerBounds.minY
                || box.minZ() < outerBounds.minZ
                || box.maxX() + 1 > outerBounds.maxX
                || box.maxY() + 1 > outerBounds.maxY
                || box.maxZ() + 1 > outerBounds.maxZ;
    }

    private boolean otherPieceContains(PoolElementStructurePiece source, BlockPos target) {
        for (PoolElementStructurePiece piece : pieces) {
            if (piece != source && piece.getBoundingBox().isInside(target)) return true;
        }
        return false;
    }

    private boolean otherPieceIntersects(PoolElementStructurePiece source, BoundingBox candidate) {
        for (PoolElementStructurePiece piece : pieces) {
            if (piece != source && piece.getBoundingBox().intersects(candidate)) return true;
        }
        return false;
    }
}
