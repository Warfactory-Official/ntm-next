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

package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mods.GunAttribute;
import com.hbm.items.weapon.sedna.mods.InstalledWeaponMods;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class GunConfig {

    public static final GunAttribute.Ref<List<Receiver>> RECEIVERS = new GunAttribute.Ref<>();
    public static final GunAttribute.OfFloat DURABILITY = new GunAttribute.OfFloat();
    public static final GunAttribute.OfInt DRAW_DURATION = new GunAttribute.OfInt();
    public static final GunAttribute.OfInt INSPECT_DURATION = new GunAttribute.OfInt();
    public static final GunAttribute.OfBool INSPECT_CANCEL = new GunAttribute.OfBool();
    public static final GunAttribute.Ref<Crosshair> CROSSHAIR = new GunAttribute.Ref<>();
    public static final GunAttribute.OfBool HIDE_CROSSHAIR = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool THERMAL_SIGHTS = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool RELOAD_REQUIRES_TYPE_CHANGE = new GunAttribute.OfBool();
    public static final GunAttribute.OfBool RELOAD_ANIMATIONS_SEQUENTIAL =
            new GunAttribute.OfBool();
    public static final GunAttribute.Ref<Identifier> SCOPE_TEXTURE = new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> SMOKE =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> ORCHESTRA =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> PRESS_PRIMARY =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> PRESS_SECONDARY =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> PRESS_TERTIARY =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> PRESS_RELOAD =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> RELEASE_PRIMARY =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> RELEASE_SECONDARY =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> RELEASE_TERTIARY =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> RELEASE_RELOAD =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiConsumer<ItemStack, LambdaContext>> DECIDER =
            new GunAttribute.Ref<>();
    public static final GunAttribute.Ref<BiFunction<ItemStack, GunAnimation, BusAnimation>>
            ANIMATIONS = new GunAttribute.Ref<>();

    private final int index;
    private final Values declared;

    private GunConfig(int index, Values declared) {
        this.index = index;
        this.declared = declared;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int index() {
        return index;
    }

    public List<Receiver> getReceivers(ItemStack stack) {
        return resolved(stack).receivers();
    }

    public float getDurability(ItemStack stack) {
        return resolved(stack).durability();
    }

    public int getDrawDuration(ItemStack stack) {
        return resolved(stack).drawDuration();
    }

    public int getInspectDuration(ItemStack stack) {
        return resolved(stack).inspectDuration();
    }

    public boolean getInspectCancel(ItemStack stack) {
        return resolved(stack).inspectCancel();
    }

    public Crosshair getCrosshair(ItemStack stack) {
        return resolved(stack).crosshair();
    }

    public boolean getHideCrosshair(ItemStack stack) {
        return resolved(stack).hideCrosshair();
    }

    public boolean hasThermalSights(ItemStack stack) {
        return resolved(stack).thermalSights();
    }

    public boolean getReloadChangesType(ItemStack stack) {
        return resolved(stack).reloadChangesType();
    }

    public boolean getReloadAnimSequential(ItemStack stack) {
        return resolved(stack).reloadSequential();
    }

    public Identifier getScopeTexture(ItemStack stack) {
        return resolved(stack).scopeTexture();
    }

    public BiConsumer<ItemStack, LambdaContext> getSmokeHandler(ItemStack stack) {
        return resolved(stack).smoke();
    }

    public BiConsumer<ItemStack, LambdaContext> getOrchestra(ItemStack stack) {
        return resolved(stack).orchestra();
    }

    public BiConsumer<ItemStack, LambdaContext> getPressPrimary(ItemStack stack) {
        return resolved(stack).pressPrimary();
    }

    public BiConsumer<ItemStack, LambdaContext> getPressSecondary(ItemStack stack) {
        return resolved(stack).pressSecondary();
    }

    public BiConsumer<ItemStack, LambdaContext> getPressTertiary(ItemStack stack) {
        return resolved(stack).pressTertiary();
    }

    public BiConsumer<ItemStack, LambdaContext> getPressReload(ItemStack stack) {
        return resolved(stack).pressReload();
    }

    public BiConsumer<ItemStack, LambdaContext> getReleasePrimary(ItemStack stack) {
        return resolved(stack).releasePrimary();
    }

    public BiConsumer<ItemStack, LambdaContext> getReleaseSecondary(ItemStack stack) {
        return resolved(stack).releaseSecondary();
    }

    public BiConsumer<ItemStack, LambdaContext> getReleaseTertiary(ItemStack stack) {
        return resolved(stack).releaseTertiary();
    }

    public BiConsumer<ItemStack, LambdaContext> getReleaseReload(ItemStack stack) {
        return resolved(stack).releaseReload();
    }

    public BiConsumer<ItemStack, LambdaContext> getDecider(ItemStack stack) {
        return resolved(stack).decider();
    }

    public BiFunction<ItemStack, GunAnimation, BusAnimation> getAnims(ItemStack stack) {
        return resolved(stack).animations();
    }

    private Values resolved(ItemStack stack) {
        return InstalledWeaponMods.of(stack, index).resolved(this, GunConfig::resolve);
    }

    private static Values resolve(GunConfig spec, InstalledWeaponMods mods) {
        var d = spec.declared;
        return new Values(
                RECEIVERS.resolve(d.receivers, mods),
                DURABILITY.resolve(d.durability, mods),
                DRAW_DURATION.resolve(d.drawDuration, mods),
                INSPECT_DURATION.resolve(d.inspectDuration, mods),
                INSPECT_CANCEL.resolve(d.inspectCancel, mods),
                CROSSHAIR.resolve(d.crosshair, mods),
                HIDE_CROSSHAIR.resolve(d.hideCrosshair, mods),
                THERMAL_SIGHTS.resolve(d.thermalSights, mods),
                RELOAD_REQUIRES_TYPE_CHANGE.resolve(d.reloadChangesType, mods),
                RELOAD_ANIMATIONS_SEQUENTIAL.resolve(d.reloadSequential, mods),
                SCOPE_TEXTURE.resolve(d.scopeTexture, mods),
                SMOKE.resolve(d.smoke, mods),
                ORCHESTRA.resolve(d.orchestra, mods),
                PRESS_PRIMARY.resolve(d.pressPrimary, mods),
                PRESS_SECONDARY.resolve(d.pressSecondary, mods),
                PRESS_TERTIARY.resolve(d.pressTertiary, mods),
                PRESS_RELOAD.resolve(d.pressReload, mods),
                RELEASE_PRIMARY.resolve(d.releasePrimary, mods),
                RELEASE_SECONDARY.resolve(d.releaseSecondary, mods),
                RELEASE_TERTIARY.resolve(d.releaseTertiary, mods),
                RELEASE_RELOAD.resolve(d.releaseReload, mods),
                DECIDER.resolve(d.decider, mods),
                ANIMATIONS.resolve(d.animations, mods));
    }

    private record Values(
            List<Receiver> receivers,
            float durability,
            int drawDuration,
            int inspectDuration,
            boolean inspectCancel,
            Crosshair crosshair,
            boolean hideCrosshair,
            boolean thermalSights,
            boolean reloadChangesType,
            boolean reloadSequential,
            Identifier scopeTexture,
            BiConsumer<ItemStack, LambdaContext> smoke,
            BiConsumer<ItemStack, LambdaContext> orchestra,
            BiConsumer<ItemStack, LambdaContext> pressPrimary,
            BiConsumer<ItemStack, LambdaContext> pressSecondary,
            BiConsumer<ItemStack, LambdaContext> pressTertiary,
            BiConsumer<ItemStack, LambdaContext> pressReload,
            BiConsumer<ItemStack, LambdaContext> releasePrimary,
            BiConsumer<ItemStack, LambdaContext> releaseSecondary,
            BiConsumer<ItemStack, LambdaContext> releaseTertiary,
            BiConsumer<ItemStack, LambdaContext> releaseReload,
            BiConsumer<ItemStack, LambdaContext> decider,
            BiFunction<ItemStack, GunAnimation, BusAnimation> animations) {}

    public static final class Builder {
        private List<Receiver.Builder> receivers = List.of();
        private float durability;
        private int drawDuration;
        private int inspectDuration;
        private boolean inspectCancel = true;
        private Crosshair crosshair;
        private boolean hideCrosshair = true;
        private boolean thermalSights;
        private boolean reloadChangesType;
        private boolean reloadSequential;
        private Identifier scopeTexture;
        private BiConsumer<ItemStack, LambdaContext> smoke;
        private BiConsumer<ItemStack, LambdaContext> orchestra;
        private BiConsumer<ItemStack, LambdaContext> pressPrimary;
        private BiConsumer<ItemStack, LambdaContext> pressSecondary;
        private BiConsumer<ItemStack, LambdaContext> pressTertiary;
        private BiConsumer<ItemStack, LambdaContext> pressReload;
        private BiConsumer<ItemStack, LambdaContext> releasePrimary;
        private BiConsumer<ItemStack, LambdaContext> releaseSecondary;
        private BiConsumer<ItemStack, LambdaContext> releaseTertiary;
        private BiConsumer<ItemStack, LambdaContext> releaseReload;
        private BiConsumer<ItemStack, LambdaContext> decider;
        private BiFunction<ItemStack, GunAnimation, BusAnimation> animations;

        private Builder() {}

        public Builder rec(Receiver.Builder... receivers) {
            this.receivers = List.of(receivers);
            return this;
        }

        public Builder dura(float durability) {
            this.durability = durability;
            return this;
        }

        public Builder draw(int ticks) {
            this.drawDuration = ticks;
            return this;
        }

        public Builder inspect(int ticks) {
            this.inspectDuration = ticks;
            return this;
        }

        public Builder inspectCancel(boolean cancel) {
            this.inspectCancel = cancel;
            return this;
        }

        public Builder crosshair(Crosshair crosshair) {
            this.crosshair = crosshair;
            return this;
        }

        public Builder hideCrosshair(boolean hide) {
            this.hideCrosshair = hide;
            return this;
        }

        public Builder thermalSights(boolean thermal) {
            this.thermalSights = thermal;
            return this;
        }

        public Builder reloadChangeType(boolean change) {
            this.reloadChangesType = change;
            return this;
        }

        public Builder reloadSequential(boolean sequential) {
            this.reloadSequential = sequential;
            return this;
        }

        public Builder scopeTexture(Identifier texture) {
            this.scopeTexture = texture;
            return this;
        }

        public Builder smoke(BiConsumer<ItemStack, LambdaContext> hook) {
            this.smoke = hook;
            return this;
        }

        public Builder orchestra(BiConsumer<ItemStack, LambdaContext> hook) {
            this.orchestra = hook;
            return this;
        }

        public Builder pp(BiConsumer<ItemStack, LambdaContext> hook) {
            this.pressPrimary = hook;
            return this;
        }

        public Builder ps(BiConsumer<ItemStack, LambdaContext> hook) {
            this.pressSecondary = hook;
            return this;
        }

        public Builder pt(BiConsumer<ItemStack, LambdaContext> hook) {
            this.pressTertiary = hook;
            return this;
        }

        public Builder pr(BiConsumer<ItemStack, LambdaContext> hook) {
            this.pressReload = hook;
            return this;
        }

        public Builder rp(BiConsumer<ItemStack, LambdaContext> hook) {
            this.releasePrimary = hook;
            return this;
        }

        public Builder rs(BiConsumer<ItemStack, LambdaContext> hook) {
            this.releaseSecondary = hook;
            return this;
        }

        public Builder rt(BiConsumer<ItemStack, LambdaContext> hook) {
            this.releaseTertiary = hook;
            return this;
        }

        public Builder rr(BiConsumer<ItemStack, LambdaContext> hook) {
            this.releaseReload = hook;
            return this;
        }

        public Builder decider(BiConsumer<ItemStack, LambdaContext> hook) {
            this.decider = hook;
            return this;
        }

        public Builder anim(BiFunction<ItemStack, GunAnimation, BusAnimation> animations) {
            this.animations = animations;
            return this;
        }

        public Builder setupStandardConfiguration() {
            return pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                    .pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .pt(Lego.LAMBDA_TOGGLE_AIM)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER);
        }

        public GunConfig build(int index) {
            var built = new Receiver[receivers.size()];
            for (int i = 0; i < built.length; i++) built[i] = receivers.get(i).build(index);
            return new GunConfig(
                    index,
                    new Values(
                            List.of(built),
                            durability,
                            drawDuration,
                            inspectDuration,
                            inspectCancel,
                            crosshair,
                            hideCrosshair,
                            thermalSights,
                            reloadChangesType,
                            reloadSequential,
                            scopeTexture,
                            smoke,
                            orchestra,
                            pressPrimary,
                            pressSecondary,
                            pressTertiary,
                            pressReload,
                            releasePrimary,
                            releaseSecondary,
                            releaseTertiary,
                            releaseReload,
                            decider,
                            animations));
        }
    }
}
