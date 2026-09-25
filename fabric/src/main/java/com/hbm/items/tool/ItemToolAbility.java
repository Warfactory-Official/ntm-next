// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.BaseAbility;
import com.hbm.handler.ability.ToolDig;
import com.hbm.handler.ability.ToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.handler.ability.WeaponAbility;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import mov.movblock.tenon.strip.api.DropSafe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ItemToolAbility extends Item implements IKeybindReceiver {

    public static @Nullable Consumer<ItemStack> OPEN_SCREEN;

    static final Identifier MOVEMENT_MODIFIER_ID = Library.id("tool_movement");

    private final AvailableAbilities abilities;
    private final boolean rockBreaker;
    private final boolean shears;

    public ItemToolAbility(
            Properties properties, AvailableAbilities abilities, boolean rockBreaker) {
        this(properties, abilities, rockBreaker, false);
    }

    public ItemToolAbility(
            Properties properties,
            AvailableAbilities abilities,
            boolean rockBreaker,
            boolean shears) {
        super(properties);
        this.abilities = abilities;
        this.rockBreaker = rockBreaker;
        this.shears = shears;
    }

    @DropSafe
    public static TagKey<Item> repairTag(String item) {
        return TagKey.create(Registries.ITEM, Library.id("tool_repair/" + item));
    }

    public static Properties toolProperties(
            ToolTier tier,
            ToolType type,
            float attackDamage,
            double movement,
            @Nullable TagKey<Item> repairItems) {
        HolderGetter<Block> blocks =
                BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);

        List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(blocks.getOrThrow(tier.incorrectBlocksForDrops())));
        for (TagKey<Block> mineable : type.mineable()) {
            rules.add(Tool.Rule.minesAndDrops(blocks.getOrThrow(mineable), tier.speed()));
        }

        Properties properties =
                new Properties()
                        .stacksTo(1)
                        .component(DataComponents.TOOL, new Tool(rules, 1.0F, 1, true))
                        .component(DataComponents.WEAPON, new Weapon(1));

        if (tier.enchantmentValue() > 0) properties.enchantable(tier.enchantmentValue());
        if (tier.durability() > 0) properties.durability(tier.durability());
        if (repairItems != null) properties.repairable(repairItems);

        return properties.attributes(attributes(attackDamage, attackSpeed(tier, type), movement));
    }

    private static float attackSpeed(ToolTier tier, ToolType type) {
        return switch (type) {
            case PICKAXE, MINER -> -2.8F;
            case SHOVEL -> -3.0F;
            case AXE -> {
                TagKey<Block> incorrect = tier.incorrectBlocksForDrops();
                if (incorrect.equals(BlockTags.INCORRECT_FOR_IRON_TOOL)) yield -3.1F;
                if (incorrect.equals(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                        || incorrect.equals(BlockTags.INCORRECT_FOR_STONE_TOOL)
                        || incorrect.equals(BlockTags.INCORRECT_FOR_COPPER_TOOL)) yield -3.2F;
                yield -3.0F;
            }
        };
    }

    public static float hoeSpeed(ToolTier tier) {
        TagKey<Block> incorrect = tier.incorrectBlocksForDrops();
        if (incorrect.equals(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                || incorrect.equals(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) return 0.0F;
        if (incorrect.equals(BlockTags.INCORRECT_FOR_IRON_TOOL)) return -1.0F;
        if (incorrect.equals(BlockTags.INCORRECT_FOR_STONE_TOOL)
                || incorrect.equals(BlockTags.INCORRECT_FOR_COPPER_TOOL)) return -2.0F;
        return -3.0F;
    }

    static ItemAttributeModifiers attributes(float attackDamage, double movement) {
        return attributes(attackDamage, null, movement);
    }

    static ItemAttributeModifiers attributes(
            float attackDamage, @Nullable Float attackSpeed, double movement) {
        ItemAttributeModifiers.Builder attributes =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        BASE_ATTACK_DAMAGE_ID,
                                        attackDamage,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND);
        if (attackSpeed != null) {
            attributes.add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                            BASE_ATTACK_SPEED_ID,
                            attackSpeed,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        if (movement != 0.0D) {
            attributes.add(
                    Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            MOVEMENT_MODIFIER_ID,
                            movement,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return attributes.build();
    }

    public static void dropAtReference(ToolDig dig, ItemStack stack) {
        if (stack.isEmpty()) return;

        BlockPos reference = dig.reference();
        dig.level()
                .addFreshEntity(
                        new ItemEntity(
                                dig.level(),
                                reference.getX() + 0.5D,
                                reference.getY() + 0.5D,
                                reference.getZ() + 0.5D,
                                stack));
    }

    public static void harvest(ToolDig dig, BlockPos pos, boolean skipDefaultDrops) {
        ServerLevel level = dig.level();
        ServerPlayer player = dig.player();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        level.levelEvent(player, 2001, pos, Block.getId(state));
        boolean drops =
                !skipDefaultDrops
                        && !player.getAbilities().instabuild
                        && Services.PLATFORM.canHarvestBlock(level, pos, state, player);
        BlockState adjusted = state.getBlock().playerWillDestroy(level, pos, state, player);
        if (!level.removeBlock(pos, false)) return;
        state.getBlock().destroy(level, pos, state);
        Services.PLATFORM.afterPlayerBreak(level, player, pos, adjusted, blockEntity);

        if (player.getAbilities().instabuild) return;
        if (drops) {

            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
            player.causeFoodExhaustion(0.005F);
            Services.PLATFORM.dropResources(
                    level, pos, dig.reference(), adjusted, blockEntity, player, dig.harvestTool());
        }
        ((ItemToolAbility) dig.held().getItem()).spendPerBlock(dig.held(), player);
    }

    public static void breakExtraBlock(ToolDig dig, BlockPos pos) {
        ServerLevel level = dig.level();
        if (level.isEmptyBlock(pos) || dig.held().isEmpty()) return;

        BlockState state = level.getBlockState(pos);
        if (!canHarvest(dig, state)) return;
        float strength = state.getDestroyProgress(dig.player(), level, pos);
        if (state.getDestroySpeed(level, pos) == -1.0F && strength == 0.0F) return;

        if (state.is(ModBlocks.STONE_KEYHOLE.get())) return;
        if (!Services.PLATFORM.canHarvestBlock(level, pos, state, dig.player())) return;

        BlockState reference = level.getBlockState(dig.reference());
        float referenceStrength =
                reference.getDestroyProgress(dig.player(), level, dig.reference());
        if (referenceStrength < 0 || referenceStrength / strength > 10F) return;

        ServerPlayer player = dig.player();
        if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer()))
            return;
        if (!Services.PLATFORM.allowPlayerBreak(
                level, player, pos, state, level.getBlockEntity(pos))) return;

        dig.preset().harvest().onHarvestBlock(dig, dig.preset().harvestLevel(), pos, state);
    }

    private static boolean canHarvest(ToolDig dig, BlockState state) {
        if (dig.preset().harvest() == ToolHarvestAbility.SILK) return true;
        if (dig.held().getItem() instanceof ItemToolAbility tool
                && tool.shears
                && Items.SHEARS.components().get(DataComponents.TOOL).rules().stream()
                        .anyMatch(rule -> state.is(rule.blocks()))) {
            return true;
        }
        return dig.held().getDestroySpeed(state) > 1.0F;
    }

    public AvailableAbilities abilities() {
        return abilities;
    }

    public List<ToolPreset> presets(ItemStack stack) {
        List<ToolPreset> stored = stack.get(ModDataComponents.TOOL_PRESETS.get());
        if (stored == null || stored.isEmpty()) return abilities.defaultPresets();

        List<ToolPreset> restricted = new ArrayList<>(stored.size());
        for (ToolPreset preset : stored) restricted.add(preset.restrictTo(abilities));
        return restricted;
    }

    public int currentPreset(ItemStack stack) {
        return Math.clamp(
                stack.getOrDefault(ModDataComponents.TOOL_PRESET_INDEX.get(), 0),
                0,
                presets(stack).size() - 1);
    }

    public ToolPreset activePreset(ItemStack stack) {
        return presets(stack).get(currentPreset(stack));
    }

    public void setPresets(ItemStack stack, List<ToolPreset> presets, int current) {
        List<ToolPreset> restricted = new ArrayList<>(presets.size());
        for (ToolPreset preset : presets) restricted.add(preset.restrictTo(abilities));

        stack.set(ModDataComponents.TOOL_PRESETS.get(), List.copyOf(restricted));
        stack.set(
                ModDataComponents.TOOL_PRESET_INDEX.get(),
                Math.clamp(current, 0, restricted.size() - 1));
    }

    public boolean canOperate(ItemStack stack) {
        return true;
    }

    protected void spendPerBlock(ItemStack stack, LivingEntity user) {
        stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
    }

    public boolean canBreakDepthRock(ItemStack stack) {
        return rockBreaker && canOperate(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return canOperate(stack) ? super.getDestroySpeed(stack, state) : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return canOperate(stack) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean canDestroyBlock(
            ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        if (!(level instanceof ServerLevel server) || !(user instanceof ServerPlayer player))
            return true;

        if (state.is(ModBlocks.STONE_KEYHOLE.get())
                || state.is(ModBlocks.STONE_KEYHOLE_META.get())) {
            return true;
        }
        if (!canOperate(stack)) return true;

        ToolPreset preset = activePreset(stack);
        if (preset.isNone()) return true;

        ToolDig dig = new ToolDig(server, player, stack, stack, pos, preset);
        dig = dig.withHarvestTool(preset.harvest().harvestTool(dig, preset.harvestLevel()));
        if (!canHarvest(dig, state)) return true;

        boolean skipReference = preset.area().onDig(dig, preset.areaLevel());
        if (!skipReference) breakExtraBlock(dig, pos);
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return cycle(context.getLevel(), player, context.getItemInHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return cycle(level, player, player.getItemInHand(hand));
    }

    private InteractionResult cycle(Level level, Player player, ItemStack stack) {
        if (!canOperate(stack)) return InteractionResult.PASS;

        List<ToolPreset> presets = presets(stack);
        if (presets.size() < 2) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        int next = player.isShiftKeyDown() ? 0 : (currentPreset(stack) + 1) % presets.size();
        setPresets(stack, presets, next);

        ToolPreset active = presets.get(next);
        Services.NETWORK.sendTo(
                new PlayerInformPayload(
                        active.message(),
                        PlayerInformPayload.ID_TOOL_ABILITY,
                        PlayerInformPayload.DEFAULT_MILLIS),
                serverPlayer);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.25F,
                active.isNone() ? 0.75F : 1.25F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level) || !(attacker instanceof Player player))
            return;
        if (!canOperate(stack)) return;

        for (Map.Entry<BaseAbility, Integer> entry : abilities.weapon()) {
            ((WeaponAbility) entry.getKey()).onHit(level, entry.getValue(), player, victim);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !activePreset(stack).isNone();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        abilities.appendTooltip(adder);

        if (rockBreaker) {
            adder.accept(Component.empty());
            adder.accept(
                    Component.translatable("desc.item.toolAbility.canBreakDepth")
                            .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean canHandleKeybind(Player player, ItemStack stack, EnumKeybind keybind) {
        return keybind == EnumKeybind.ABILITY_ALT && player.level().isClientSide();
    }

    @Override
    public void handleKeybind(Player player, ItemStack stack, EnumKeybind keybind, boolean state) {}

    @Override
    public void handleKeybindClient(
            Player player, ItemStack stack, EnumKeybind keybind, boolean state) {
        if (state && OPEN_SCREEN != null) OPEN_SCREEN.accept(stack);
    }

    public enum ToolType {
        PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
        AXE(BlockTags.MINEABLE_WITH_AXE),
        SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL),
        MINER(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL);

        private final List<TagKey<Block>> mineable;

        @SafeVarargs
        ToolType(TagKey<Block>... mineable) {
            this.mineable = List.of(mineable);
        }

        public List<TagKey<Block>> mineable() {
            return mineable;
        }
    }
}
