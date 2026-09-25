// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.special.ScrapType;
import com.hbm.tileentity.BlockEntityBobble;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockBobble extends Block implements EntityBlock {

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    private static final VoxelShape SHAPE = Block.box(5.5, 0, 5.5, 10.5, 10, 10.5);

    public static Consumer<BlockEntityBobble> OPEN_GUI = be -> {};

    public BlockBobble(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityBobble(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(ROTATION, RotationSegment.convertToSegment(ctx.getRotation() + 180.0F));
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BlockEntityBobble bobble) {
            bobble.type = BlockEntityBobble.typeOf(stack);
            bobble.setChanged();

            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof BlockEntityBobble bobble) {
            return BlockEntityBobble.stackOf(bobble.type);
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.getAbilities().instabuild
                && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityBobble bobble) {
            ItemEntity item =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            BlockEntityBobble.stackOf(bobble.type));

            item.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityBobble bobble) {
            OPEN_GUI.accept(bobble);
        }
        return InteractionResult.SUCCESS;
    }

    public enum BobbleType {
        NONE("null", "null", null, null, false, ScrapType.BOARD_BLANK),
        STRENGTH(
                "Strength",
                "Strength",
                null,
                "It's essential to give your arguments impact.",
                false,
                ScrapType.BRIDGE_BIOS),
        PERCEPTION(
                "Perception",
                "Perception",
                null,
                "Only through observation will you perceive weakness.",
                false,
                ScrapType.BRIDGE_NORTH),
        ENDURANCE(
                "Endurance",
                "Endurance",
                null,
                "Always be ready to take one for the team.",
                false,
                ScrapType.BRIDGE_SOUTH),
        CHARISMA(
                "Charisma",
                "Charisma",
                null,
                "Nothing says pizzaz like a winning smile.",
                false,
                ScrapType.BRIDGE_IO),
        INTELLIGENCE(
                "Intelligence",
                "Intelligence",
                null,
                "It takes the smartest individuals to realize$there's always more to learn.",
                false,
                ScrapType.BRIDGE_BUS),
        AGILITY(
                "Agility",
                "Agility",
                null,
                "Never be afraid to dodge the sensitive issues.",
                false,
                ScrapType.BRIDGE_CHIPSET),
        LUCK(
                "Luck",
                "Luck",
                null,
                "There's only one way to give 110%.",
                false,
                ScrapType.BRIDGE_CMOS),
        BOB(
                "Robert \"The Bobcat\" Katzinsky",
                "HbMinecraft",
                "Hbm's Nuclear Tech Mod",
                "I know where you live, " + System.getProperty("user.name"),
                false,
                ScrapType.CPU_SOCKET),
        FRIZZLE("Frooz", "Frooz", "Weapon models", "BLOOD IS FUEL", true, ScrapType.CPU_CLOCK),
        PU238(
                "Pu-238",
                "Pu-238",
                "Improved Tom impact mechanics",
                null,
                false,
                ScrapType.CPU_REGISTER),
        VT(
                "VT-6/24",
                "VT-6/24",
                "Balefire warhead model and general texturework",
                "You cannot unfuck a horse.",
                true,
                ScrapType.CPU_EXT),
        DOC(
                "The Doctor",
                "Doctor17PH",
                "Russian localization, lunar miner",
                "Perhaps the moon rocks were too expensive",
                true,
                ScrapType.CPU_CACHE),
        BLUEHAT(
                "The Blue Hat",
                "The Blue Hat",
                "Textures",
                "payday 2's deagle freeaim champ of the year 2022",
                true,
                ScrapType.MEM_16K_A),
        PHEO(
                "Pheo",
                "Pheonix",
                "Deuterium machines, tantalium textures, Reliant Rocket",
                "RUN TO THE BEDROOM, ON THE SUITCASE ON THE LEFT,$YOU'LL FIND MY FAVORITE AXE",
                true,
                ScrapType.MEM_16K_B),
        ADAM29(
                "Adam29",
                "Adam29",
                "Ethanol, liquid petroleum gas",
                "You know, nukes are really quite beatiful.$It's like watching a star be born for a split second.",
                true,
                ScrapType.MEM_16K_C),
        UFFR(
                "UFFR",
                "UFFR",
                "All sorts of things from his PR",
                "fried shrimp",
                false,
                ScrapType.MEM_SOCKET),
        VAER(
                "vaer",
                "vaer",
                "ZIRNOX",
                "taken de family out to the weekend cigarette festival",
                true,
                ScrapType.MEM_16K_D),
        NOS(
                "Dr Nostalgia",
                "Dr Nostalgia",
                "SSG and Vortex models",
                "Take a picture, I'ma pose, paparazzi$I've been drinking, moving like a zombie",
                true,
                ScrapType.BOARD_TRANSISTOR),
        DRILLGON("Drillgon200", "Drillgon200", "1.12 Port", null, false, ScrapType.CPU_LOGIC),
        CIRNO(
                "Cirno",
                "Cirno",
                "the only multi layered skin i had",
                "No brain. Head empty.",
                true,
                ScrapType.BOARD_BLANK),
        MICROWAVE(
                "Microwave",
                "Microwave",
                "OC Compatibility and massive RBMK/packet optimizations",
                "they call me the food heater$john optimization",
                true,
                ScrapType.BOARD_CONVERTER),
        PEEP(
                "Peep",
                "LePeeperSauvage",
                "Coilgun, Leadburster and Congo Lake models, BDCL QC",
                "Fluffy ears can't hide in ash, nor snow.",
                true,
                ScrapType.CARD_BOARD),
        MELLOW(
                "MELLOWARPEGGIATION",
                "Mellow",
                "NBT Structures, industrial lighting, animation tools",
                "Make something cool now, ask for permission later.",
                true,
                ScrapType.CARD_PROCESSOR),
        ABEL(
                "Abel1502",
                "Abel1502",
                "Abilities GUI, optimizations and many QoL improvements",
                "NANTO SUBARASHII",
                true,
                ScrapType.CPU_REGISTER);

        private static final BobbleType[] VALUES = values();
        public final String name;
        public final String label;
        public final @Nullable String contribution;
        public final @Nullable String inscription;
        public final boolean skinLayers;
        public final ScrapType scrap;

        BobbleType(
                String name,
                String label,
                @Nullable String contribution,
                @Nullable String inscription,
                boolean layers,
                ScrapType scrap) {
            this.name = name;
            this.label = label;
            this.contribution = contribution;
            this.inscription = inscription;
            this.skinLayers = layers;
            this.scrap = scrap;
        }

        public static BobbleType byOrdinal(int raw) {
            return VALUES[Math.abs(raw) % VALUES.length];
        }

        public static int count() {
            return VALUES.length;
        }
    }
}
