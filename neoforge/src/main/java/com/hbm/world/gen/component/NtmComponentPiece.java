// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.DungeonProcessorLists;
import com.hbm.world.gen.nbt.WeightedOption;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class NtmComponentPiece extends StructurePiece {

    private static final int FOUNDATION_DEPTH = 16;

    protected NtmComponentPiece(StructurePieceType type, int genDepth, BoundingBox boundingBox) {
        super(type, genDepth, boundingBox);
    }

    protected NtmComponentPiece(
            StructurePieceType type, StructurePieceSerializationContext context, CompoundTag tag) {
        super(type, tag);
    }

    protected static BlockState hbmState(String path) {
        return BuiltInRegistries.BLOCK
                .getOptional(Library.id(path))
                .map(Block::defaultBlockState)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Structure piece needs hbm:"
                                                + path
                                                + ", which is not registered"));
    }

    @Override
    protected int getWorldX(int x, int z) {
        Direction orientation = this.getOrientation();
        if (orientation == null) return x;
        return switch (orientation) {
            case SOUTH -> this.boundingBox.minX() + x;
            case WEST -> this.boundingBox.maxX() - z;
            case NORTH -> this.boundingBox.maxX() - x;
            case EAST -> this.boundingBox.minX() + z;
            default -> x;
        };
    }

    @Override
    protected int getWorldZ(int x, int z) {
        Direction orientation = this.getOrientation();
        if (orientation == null) return z;
        return switch (orientation) {
            case SOUTH -> this.boundingBox.minZ() + z;
            case WEST -> this.boundingBox.minZ() + x;
            case NORTH -> this.boundingBox.maxZ() - z;
            case EAST -> this.boundingBox.maxZ() - x;
            default -> z;
        };
    }

    @Override
    public void setOrientation(@Nullable Direction orientation) {
        super.setOrientation(orientation);
        this.rotation = legacyRotation(orientation);
        this.mirror = Mirror.NONE;
    }

    protected static Rotation legacyRotation(@Nullable Direction orientation) {
        if (orientation == null) return Rotation.NONE;
        return switch (orientation) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    protected @Nullable Identifier templateId() {
        return null;
    }

    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {}

    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {}

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {
        this.buildFoundation(level, chunkBB, random);
        Identifier id = this.templateId();
        if (id == null) {
            throw new IllegalStateException(
                    getClass().getSimpleName()
                            + " declares no template and does not override postProcess, so it writes nothing");
        }
        this.placeTemplate(level, chunkBB, random, id);
        this.buildAfterTemplate(level, chunkBB, random);
    }

    private void placeTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random, Identifier id) {
        StructureTemplate template =
                level.getLevel()
                        .getServer()
                        .getStructureManager()
                        .get(id)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                getClass().getSimpleName()
                                                        + " needs template "
                                                        + id
                                                        + ", which is not registered"));

        Direction orientation = this.getOrientation();
        StructurePlaceSettings settings =
                new StructurePlaceSettings()
                        .setBoundingBox(chunkBB)
                        .setRotation(legacyRotation(orientation))
                        .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        for (StructureProcessor processor : DungeonProcessorLists.forTemplate(level, id)) {
            settings.addProcessor(processor);
        }

        BoundingBox box = this.boundingBox;
        BlockPos origin =
                switch (orientation) {
                    case WEST -> new BlockPos(box.maxX(), box.minY(), box.minZ());
                    case NORTH -> new BlockPos(box.maxX(), box.minY(), box.maxZ());
                    case EAST -> new BlockPos(box.minX(), box.minY(), box.maxZ());
                    case null, default -> new BlockPos(box.minX(), box.minY(), box.minZ());
                };
        template.placeInWorld(level, origin, origin, settings, random, 2);
    }

    protected void fillFoundationColumn(
            WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox chunkBB) {
        BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
        if (!chunkBB.isInside(pos)) return;
        for (int laid = 0; laid < FOUNDATION_DEPTH && pos.getY() > level.getMinY() + 1; laid++) {
            if (!this.isReplaceableByStructures(level.getBlockState(pos))) return;
            level.setBlock(pos, state, 2);
            pos.move(Direction.DOWN);
        }
    }

    @Override
    protected boolean isReplaceableByStructures(BlockState state) {
        return super.isReplaceableByStructures(state)
                || !state.isSolid()
                || state.is(BlockTags.LEAVES);
    }

    @Override
    protected void placeBlock(
            WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox chunkBB) {
        super.placeBlock(level, state, x, y, z, chunkBB);
        if (!derivesConnections(state)) return;
        BlockPos pos = this.getWorldPos(x, y, z);

        if (chunkBB.isInside(pos) && level.getBlockState(pos).is(state.getBlock())) {
            level.getChunk(pos).markPosForPostProcessing(pos);
        }
    }

    private static boolean derivesConnections(BlockState state) {
        return state.hasProperty(BlockStateProperties.NORTH)
                || state.hasProperty(BlockStateProperties.NORTH_WALL)
                || state.hasProperty(BlockStateProperties.STAIRS_SHAPE);
    }

    protected void generateLoreBook(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int x,
            int y,
            int z,
            int slot,
            ItemStack book) {
        BlockPos pos = this.getWorldPos(x, y, z);
        if (!chunkBB.isInside(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof Container container)
                || slot >= container.getContainerSize()) {
            return;
        }
        if (container instanceof RandomizableContainer randomizable) {
            ResourceKey<LootTable> key = randomizable.getLootTable();
            if (key != null) {
                ServerLevel server = level.getLevel();
                LootTable table = server.getServer().reloadableRegistries().getLootTable(key);
                randomizable.setLootTable(null);
                LootParams params =
                        new LootParams.Builder(server)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                                .create(LootContextParamSets.CHEST);
                table.fill(container, params, randomizable.getLootTableSeed());
            }
        }
        container.setItem(slot, book);
    }

    public static List<WeightedOption> sandstoneTable() {
        return List.of(
                new WeightedOption(Blocks.SANDSTONE.defaultBlockState(), 4),
                new WeightedOption(hbmState("reinforced_sand"), 5),
                new WeightedOption(Blocks.SAND.defaultBlockState(), 1));
    }

    public static List<WeightedOption> concreteBricksTable() {
        return List.of(
                new WeightedOption(hbmState("brick_concrete"), 4),
                new WeightedOption(hbmState("brick_concrete_mossy"), 3),
                new WeightedOption(hbmState("brick_concrete_cracked"), 2),
                new WeightedOption(hbmState("brick_concrete_broken"), 1));
    }

    public static List<WeightedOption> labTilesTable() {
        return List.of(
                new WeightedOption(hbmState("tile_lab"), 5),
                new WeightedOption(hbmState("tile_lab_cracked"), 4),
                new WeightedOption(hbmState("tile_lab_broken"), 1));
    }
}
