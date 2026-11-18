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

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.KeybindType;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.BaseAbility;
import com.hbm.handler.ability.ToolAreaAbility;
import com.hbm.handler.ability.ToolDig;
import com.hbm.handler.ability.ToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.items.KeybindHandler;
import com.hbm.items.ModDataComponents;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import com.hbm.platform.Platform;
import com.hbm.sound.EntitySounds;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
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
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ToolAbilityItem extends Item implements KeybindHandler {
    public static @Nullable BooleanSupplier CYCLE_FROM_USE;

    static final Identifier MOVEMENT_MODIFIER_ID = RegistryUtil.id("tool_movement");

    private final AvailableAbilities abilities;
    private final ToolType type;
    private final boolean rockBreaker;
    private final boolean shears;

    public ToolAbilityItem(
            Properties properties, AvailableAbilities abilities, boolean rockBreaker) {
        this(properties, abilities, rockBreaker, false);
    }

    public ToolAbilityItem(
            Properties properties,
            AvailableAbilities abilities,
            boolean rockBreaker,
            boolean shears) {
        super(properties);
        this.abilities = abilities;
        this.type = ((ToolProperties) properties).type;
        this.rockBreaker = rockBreaker;
        this.shears = shears;
    }

    public static TagKey<Item> repairTag(String item) {
        return TagKey.create(Registries.ITEM, RegistryUtil.id("tool_repair/" + item));
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
        if (type == ToolType.PICKAXE || type == ToolType.MINER) {
            rules.add(
                    Tool.Rule.minesAndDrops(
                            HolderSet.direct(Blocks.GLOWSTONE.builtInRegistryHolder()),
                            tier.speed()));
        }

        Properties properties =
                new ToolProperties(type)
                        .stacksTo(1)
                        .component(DataComponents.TOOL, new Tool(rules, 1.0F, 1, true))
                        .component(
                                DataComponents.WEAPON,
                                new Weapon(
                                        1,
                                        type == ToolType.AXE
                                                ? Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS
                                                : 0.0F));
        if (tier.enchantmentValue() > 0) properties.enchantable(tier.enchantmentValue());
        if (tier.durability() > 0) properties.durability(tier.durability());
        if (repairItems != null) properties.repairable(repairItems);

        switch (type) {
            case AXE ->
                    properties.delayedComponent(
                            DataComponents.BLOCK_TRANSFORMER,
                            lookup -> lookup.getOrThrow(BlockTransformers.AXE));
            case SHOVEL, MINER ->
                    properties.delayedComponent(
                            DataComponents.BLOCK_TRANSFORMER,
                            lookup -> lookup.getOrThrow(BlockTransformers.SHOVEL));
            case PICKAXE -> {}
        }

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

    public static boolean harvest(ToolDig dig, BlockPos pos, boolean skipDefaultDrops) {
        ServerLevel level = dig.level();
        ServerPlayer player = dig.player();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        level.levelEvent(player, 2001, pos, Block.getId(state));
        boolean drops =
                !skipDefaultDrops
                        && !player.getAbilities().instabuild
                        && Platform.canHarvestBlock(level, pos, state, player);
        BlockState adjusted = state.getBlock().playerWillDestroy(level, pos, state, player);
        if (!level.removeBlock(pos, false)) return false;
        state.getBlock().destroy(level, pos, state);
        PlayerBlockBreakEvents.AFTER
                .invoker()
                .afterBlockBreak(level, player, pos, adjusted, blockEntity);

        if (player.getAbilities().instabuild) return true;
        if (drops) {
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
            player.causeFoodExhaustion(0.005F);
            Platform.dropResources(
                    level, pos, dig.reference(), adjusted, blockEntity, player, dig.harvestTool());
        }
        ((ToolAbilityItem) dig.held().getItem()).spendPerBlock(dig.held(), player);
        return true;
    }

    public static void breakExtraBlock(ToolDig dig, BlockPos candidate) {
        ServerLevel level = dig.level();
        if (level.isEmptyBlock(candidate) || dig.held().isEmpty()) return;

        BlockPos pos = candidate.immutable();
        BlockState state = level.getBlockState(pos);
        if (!canHarvest(dig, state)) return;
        float strength = state.getDestroyProgress(dig.player(), level, pos);
        if (state.getDestroySpeed(level, pos) == -1.0F && strength == 0.0F) return;
        if (state.is(ModBlocks.STONE_KEYHOLE.get())) return;
        if (!Platform.canHarvestBlock(level, pos, state, dig.player())) return;

        BlockState reference = level.getBlockState(dig.reference());
        float referenceStrength =
                reference.getDestroyProgress(dig.player(), level, dig.reference());
        if (referenceStrength < 0 || referenceStrength / strength > 10F) return;

        ServerPlayer player = dig.player();
        if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer()))
            return;
        if (!Platform.allowPlayerBreak(level, player, pos, state, level.getBlockEntity(pos)))
            return;

        dig.preset().harvest().harvestBlock(dig, pos, state);
    }

    private static boolean canHarvest(ToolDig dig, BlockState state) {
        if (dig.preset().harvest() == ToolHarvestAbility.SILK_TOUCH) return true;
        if (dig.held().getItem() instanceof ToolAbilityItem tool && tool.shears) {
            List<Tool.Rule> rules = Items.SHEARS.components().get(DataComponents.TOOL).rules();
            for (int i = 0, n = rules.size(); i < n; i++) {
                if (state.is(rules.get(i).blocks())) return true;
            }
        }
        return dig.held().getDestroySpeed(state) > 1.0F;
    }

    public AvailableAbilities abilities() {
        return abilities;
    }

    public List<ToolPreset> presets(ItemStack stack) {
        List<ToolPreset> stored = stack.get(ModDataComponents.TOOL_PRESETS.get());
        boolean defaults = stored == null || stored.isEmpty();
        if (defaults) stored = abilities.defaultPresets();

        List<ToolPreset> restricted = new ArrayList<>(stored.size());
        for (ToolPreset preset : stored) {
            ToolPreset available = restrictPreset(stack, preset);
            if (!defaults || !available.isNone() || preset.isNone()) restricted.add(available);
        }
        return restricted;
    }

    public boolean canSelectAbility(ItemStack stack, BaseAbility ability) {
        if (ability == ToolAreaAbility.NONE || ability == ToolHarvestAbility.NONE) return true;
        if (!abilities.supports(ability)) return false;

        boolean silk = false;
        boolean fortune = false;
        for (var entry :
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .entrySet()) {
            if (entry.getIntValue() <= 0) continue;
            silk |= entry.getKey().is(Enchantments.SILK_TOUCH);
            fortune |= entry.getKey().is(Enchantments.FORTUNE);
        }
        if (!silk && !fortune) return true;
        if (ability == ToolAreaAbility.EXPLOSION) return false;
        return !(ability instanceof ToolHarvestAbility)
                || ability == (silk ? ToolHarvestAbility.SILK_TOUCH : ToolHarvestAbility.FORTUNE);
    }

    private ToolPreset restrictPreset(ItemStack stack, ToolPreset preset) {
        ToolPreset restricted = preset.restrictTo(abilities);
        ToolAreaAbility area =
                canSelectAbility(stack, restricted.area())
                        ? restricted.area()
                        : ToolAreaAbility.NONE;
        ToolHarvestAbility harvest =
                canSelectAbility(stack, restricted.harvest())
                        ? restricted.harvest()
                        : ToolHarvestAbility.NONE;
        return new ToolPreset(
                area,
                area == restricted.area() ? restricted.areaLevel() : 0,
                harvest,
                harvest == restricted.harvest() ? restricted.harvestLevel() : 0);
    }

    public int currentPreset(ItemStack stack) {
        return currentPreset(stack, presets(stack));
    }

    private static int currentPreset(ItemStack stack, List<ToolPreset> presets) {
        return Math.clamp(
                stack.getOrDefault(ModDataComponents.TOOL_PRESET_INDEX.get(), 0),
                0,
                presets.size() - 1);
    }

    public ToolPreset activePreset(ItemStack stack) {
        List<ToolPreset> presets = presets(stack);
        return presets.get(currentPreset(stack, presets));
    }

    public void setPresets(ItemStack stack, List<ToolPreset> presets, int current) {
        List<ToolPreset> restricted = new ArrayList<>(presets.size());
        for (ToolPreset preset : presets) restricted.add(restrictPreset(stack, preset));

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

    protected void spendToolAction(ItemStack stack, LivingEntity user) {}

    public final void finishToolAction(ItemStack stack, Player player) {
        if (!player.level().isClientSide() && !player.hasInfiniteMaterials())
            spendToolAction(stack, player);
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
        dig = dig.withHarvestTool(preset.harvest().lootTool(dig, preset.harvestLevel()));
        if (!canHarvest(dig, state)) return true;

        boolean skipReference = preset.area().dig(dig, preset.areaLevel());
        if (!skipReference) breakExtraBlock(dig, pos);
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !canOperate(stack)) return InteractionResult.PASS;

        InteractionResult result = super.useOn(context);
        if (result != InteractionResult.PASS) {
            if (result.consumesAction()) finishToolAction(stack, player);
            return result;
        }
        if (!context.getLevel().isClientSide() || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return cycleFromUse(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        return cycleFromUse(player.getItemInHand(hand));
    }

    private InteractionResult cycleFromUse(ItemStack stack) {
        if (!canOperate(stack)) return InteractionResult.PASS;
        if (presets(stack).size() < 2) return InteractionResult.PASS;
        return CYCLE_FROM_USE.getAsBoolean() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private void cycle(ServerPlayer player, ItemStack stack) {
        if (!canOperate(stack)) return;
        List<ToolPreset> presets = presets(stack);
        if (presets.size() < 2) return;

        int next =
                player.isShiftKeyDown() ? 0 : (currentPreset(stack, presets) + 1) % presets.size();
        setPresets(stack, presets, next);

        ToolPreset active = presets.get(next);
        PacketWire.sendTo(
                PlayerInformLayout.encode(
                        active.message(),
                        ClientEventConstants.PLAYER_INFORM_ID_TOOL_ABILITY,
                        ClientEventConstants.PLAYER_INFORM_DEFAULT_MILLIS),
                player);
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.25F,
                active.isNone() ? 0.75F : 1.25F);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level) || !(attacker instanceof Player player))
            return;
        if (!canOperate(stack)) return;

        var hits = abilities.hits();
        for (int i = 0, n = hits.size(); i < n; i++) {
            var hit = hits.get(i);
            hit.ability().hit(level, hit.maxLevel(), player, victim);
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
        abilities.appendTooltip(adder, ability -> canSelectAbility(stack, ability));

        if (rockBreaker) {
            adder.accept(Component.empty());
            adder.accept(
                    Component.translatable("desc.item.toolAbility.canBreakDepth")
                            .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean canHandleKeybind(ServerPlayer player, ItemStack stack, KeybindType keybind) {
        return keybind == KeybindType.ABILITY_CYCLE || keybind == KeybindType.ABILITY_CONFIGURE;
    }

    @Override
    public void onKeyChanged(
            ServerPlayer player, ItemStack stack, KeybindType keybind, boolean state) {
        if (keybind == KeybindType.ABILITY_CYCLE && state) cycle(player, stack);
        if (keybind == KeybindType.ABILITY_CONFIGURE && state) {
            HeldItemMenu.open(
                    player,
                    InteractionHand.MAIN_HAND,
                    ModMenus.TOOL_PRESETS,
                    CommonComponents.EMPTY);
        }
    }

    private static final class ToolProperties extends Properties {

        private final ToolType type;

        private ToolProperties(ToolType type) {
            this.type = type;
        }
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
