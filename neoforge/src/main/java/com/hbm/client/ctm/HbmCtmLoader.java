// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.ctm;

import java.util.Properties;
import java.util.function.Predicate;
import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.api.client.CtmLoader;
import me.pepperbell.continuity.api.client.CtmLoaderRegistry;
import me.pepperbell.continuity.api.client.CtmProperties;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.BaseCachingPredicates;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.simple.CtmSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.properties.OrientedConnectingCtmProperties;
import me.pepperbell.continuity.client.properties.PropertiesParsingHelper;
import me.pepperbell.continuity.client.properties.TileAmountValidator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;

public final class HbmCtmLoader {

    public static final String METHOD = "hbm_ctm";

    private HbmCtmLoader() {}

    public static void register() {
        CtmProperties.Factory<Properties17> factory =
                TileAmountValidator.wrapFactory(
                        BaseCtmProperties.wrapFactory(Properties17::new),
                        new TileAmountValidator.AtLeast<>(47));
        QuadProcessor.Factory<Properties17> processor =
                new SimpleQuadProcessor.Factory<>(new CtmSpriteProvider.Factory());
        CachingPredicates.Factory<Properties17> predicates =
                new BaseCachingPredicates.Factory<>(true);
        CtmLoaderRegistry.get()
                .registerLoader(
                        METHOD,
                        new CtmLoader<Properties17>() {

                            @Override
                            public CtmProperties.Factory<Properties17> getPropertiesFactory() {
                                return factory;
                            }

                            @Override
                            public QuadProcessor.Factory<Properties17> getProcessorFactory() {
                                return processor;
                            }

                            @Override
                            public CachingPredicates.Factory<Properties17> getPredicatesFactory() {
                                return predicates;
                            }
                        });
    }

    public static class Properties17 extends OrientedConnectingCtmProperties {

        public Properties17(
                Properties properties,
                Identifier resourceId,
                PackResources pack,
                int packPriority,
                ResourceManager resourceManager,
                String method) {
            super(properties, resourceId, pack, packPriority, resourceManager, method);
        }

        @Override
        public void init() {
            super.init();
            Predicate<BlockState> join =
                    PropertiesParsingHelper.parseBlockStates(
                            properties, "connectBlocks", resourceId, packId);
            if (join == null || join == PropertiesParsingHelper.EMPTY_BLOCK_STATE_PREDICATE) {
                return;
            }
            ConnectionPredicate base = connectionPredicate;
            if (base == null) {
                return;
            }
            connectionPredicate =
                    (level,
                            pos,
                            appearance,
                            state,
                            otherPos,
                            otherAppearance,
                            otherState,
                            face,
                            sprite) ->
                            base.shouldConnect(
                                            level,
                                            pos,
                                            appearance,
                                            state,
                                            otherPos,
                                            otherAppearance,
                                            otherState,
                                            face,
                                            sprite)
                                    || join.test(otherAppearance);
        }
    }
}
