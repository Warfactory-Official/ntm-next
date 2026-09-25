// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric;

import com.hbm.NuclearTech;
import com.hbm.capability.NtmCapabilities;
import com.hbm.command.ModCommands;
import com.hbm.config.FabricHbmConfig;
import com.hbm.config.ForeignModChange;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.fabric.compat.CreateMountedCrates;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.BobmazonSignHandler;
import com.hbm.handler.CoalGasHandler;
import com.hbm.handler.FuelHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.FluidPropertyReloadListener;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.recipes.ingredient.HbmFluidContentIngredient;
import com.hbm.inventory.recipes.ingredient.HbmMatchersIngredient;
import com.hbm.inventory.recipes.ingredient.HbmNarrowedIngredient;
import com.hbm.items.armor.ItemModTwoKick;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemSettingsTool;
import com.hbm.lib.Library;
import com.hbm.packet.PacketDispatcher;
import com.hbm.platform.FabricCapabilityService;
import com.hbm.platform.FabricNetworkService;
import com.hbm.platform.Services;
import com.hbm.qmaw.QMAWLoader;
import com.hbm.util.ContagionUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class NuclearTechFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NuclearTech.init();
        ((FabricHbmConfig) Services.CONFIG).load();
        NtmCapabilities.declareAll();
        ((FabricCapabilityService) Services.CAPS).flush();
        NuclearTech.commonSetup();
        NuclearTech.registerFlammability();
        ArmorModChestLootFabric.init();

        PacketDispatcher.register();
        ((FabricNetworkService) Services.NETWORK).flush();

        AttackEntityCallback.EVENT.register(
                (player, level, hand, entity, hit) -> {
                    ItemModTwoKick.onPunch(player);
                    return ItemBoltgun.handleAttack(player, entity)
                            ? InteractionResult.FAIL
                            : InteractionResult.PASS;
                });

        UseBlockCallback.EVENT.register(
                (player, level, hand, hit) -> {
                    var stack = player.getItemInHand(hand);
                    if (!(stack.getItem() instanceof ItemSettingsTool))
                        return InteractionResult.PASS;
                    return ItemSettingsTool.useFirst(stack, new UseOnContext(player, hand, hit));
                });
        UseBlockCallback.EVENT.register(
                (player, level, hand, hit) ->
                        BobmazonSignHandler.onSignUsed(level, hit.getBlockPos())
                                ? InteractionResult.SUCCESS
                                : InteractionResult.PASS);

        PlayerBlockBreakEvents.AFTER.register(
                (level, player, pos, state, blockEntity) -> {
                    CoalGasHandler.onBlockBroken(level, pos, state);
                    PollutionHandler.onBlockBroken(player, pos);
                });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) ->
                        HbmPlayerProps.getData(player).grenadeDeployment = 0);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> amount > 0F);
        ResourceLoader.get(PackType.SERVER_DATA)
                .registerReloadListener(
                        FluidPropertyReloadListener.ID, new FluidPropertyReloadListener());
        DataResourceLoader.get().registerReloadListener(QMAWLoader.ID, QMAWLoader::new);
        for (ForeignModChange change : ForeignModChange.values()) {
            if (!FabricLoader.getInstance().isModLoaded(change.modId) || !change.enabled())
                continue;
            if (!ResourceLoader.registerBuiltinPack(
                    Library.id(change.pack),
                    FabricLoader.getInstance().getModContainer(NuclearTech.MOD_ID).orElseThrow(),
                    Component.translatable(change.titleKey()),
                    PackActivationType.ALWAYS_ENABLED)) {
                throw new IllegalStateException("Missing builtin pack: " + change.pack);
            }
        }
        if (FabricLoader.getInstance().isModLoaded("create")) CreateMountedCrates.register();

        CommonLifecycleEvents.TAGS_LOADED.register(
                (registries, client) -> {
                    HazardSystem.clearCaches();
                    NTMFluidProperties.bindEquivalents();
                });

        ServerLivingEntityEvents.ALLOW_DEATH.register(
                (entity, source, amount) -> !ArmorModHandler.tryRevive(entity));
        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, source) -> {
                    if (source.getEntity() instanceof ServerPlayer killer
                            && !(killer instanceof FakePlayer)) {
                        ArmorModHandler.onKilledByPlayer(entity);
                    }
                    ContagionUtil.onDeath(entity);
                });
        LootTableEvents.MODIFY_DROPS.register(
                (holder, context, drops) -> {
                    if (!ContagionUtil.isContagious(
                            context.getOptionalParameter(LootContextParams.THIS_ENTITY))) return;
                    for (ItemStack drop : drops) ContagionUtil.taint(drop);
                });

        FuelValueEvents.BUILD.register(
                (builder, context) -> FuelHandler.forEachUniformFuel(builder::add));

        ResourceConditions.register(FlagResourceCondition.TYPE);

        CustomIngredientSerializer.register(HbmMatchersIngredient.SERIALIZER);
        CustomIngredientSerializer.register(HbmFluidContentIngredient.SERIALIZER);
        CustomIngredientSerializer.register(HbmNarrowedIngredient.SERIALIZER);

        ServerLifecycleEvents.SERVER_STARTING.register(NuclearTech::serverAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NuclearTech.serverStopped());
        ServerLifecycleEvents.SERVER_STARTED.register(
                com.hbm.world.ore.OreGeneration::serverStarted);

        EntitySleepEvents.ALLOW_SLEEPING.register(
                (player, pos) -> {
                    PollutionHandler.rampantTargetSetter(pos);
                    return null;
                });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, access, env) -> ModCommands.register(dispatcher));
    }
}
