// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.AnnihilatorRecipe.Milestone;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.ItemSubtype;
import com.hbm.registration.Reg;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

public final class AnnihilatorRecipes extends SerializableRecipe
        implements RecipeType<AnnihilatorRecipe> {

    public static final AnnihilatorRecipes INSTANCE = new AnnihilatorRecipes();
    private static final String REGISTRY_NAME = "annihilator";
    private Identifier typeId;
    private Generation generation = Generation.empty();

    private AnnihilatorRecipes() {}

    @Override
    public void registerType(IRegistrar registrar) {
        this.typeId = registrar.registerRecipeType(REGISTRY_NAME, this).id();
        registrar.registerRecipeSerializer(REGISTRY_NAME, AnnihilatorRecipe.SERIALIZER);
    }

    @Override
    public Identifier datapackTypeId() {
        return typeId;
    }

    @Override
    public RecipeType<?> datapackType() {
        return this;
    }

    @Override
    public String toString() {
        return typeId == null ? super.toString() : typeId.toString();
    }

    @Override
    public void ensureFilled() {
        RecipeSource.Token token = RecipeSource.token();
        if (token == null) return;
        if (generation.token().equals(token)) return;
        synchronized (this) {
            if (generation.token().equals(token)) return;
            generation = Generation.of(token, RecipeSource.recipes());
        }
    }

    public static List<RecipeHolder<AnnihilatorRecipe>> rows() {
        INSTANCE.ensureFilled();
        return INSTANCE.generation.rows();
    }

    public static @Nullable ItemStack getHighestPayoutFromKey(
            Object key, @Nullable BigInteger prevAmount, BigInteger currentAmount) {
        INSTANCE.ensureFilled();
        RecipeHolder<AnnihilatorRecipe> row = INSTANCE.generation.byKey().get(key);
        return row == null
                ? null
                : getHighestPayoutFromRecipe(row.value().milestones(), prevAmount, currentAmount);
    }

    public static @Nullable ItemStack getHighestPayoutFromRecipe(
            List<Milestone> milestones, @Nullable BigInteger prevAmount, BigInteger currentAmount) {
        BigInteger highestYet = BigInteger.ZERO;
        ItemStackTemplate highestPayout = null;
        for (Milestone milestone : milestones) {
            if (prevAmount != null && prevAmount.compareTo(milestone.amount()) >= 0) continue;
            if (currentAmount.compareTo(highestYet) <= 0) continue;
            if (currentAmount.compareTo(milestone.amount()) >= 0) {
                highestYet = milestone.amount();
                highestPayout = milestone.payout();
            }
        }
        return highestPayout != null ? highestPayout.create() : null;
    }

    public record StackKey(Item item, @Nullable Object subtype, ItemStackTemplate prototype) {

        public static StackKey of(ItemStack stack) {
            ItemSubtype kind = Reg.itemSubtype(stack.getItem());
            return new StackKey(
                    stack.getItem(),
                    kind == null ? null : kind.getSubtypeData(stack),
                    ItemStackTemplate.fromNonEmptyStack(stack).withCount(1));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StackKey key
                    && key.item == item
                    && Objects.equals(key.subtype, subtype);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + Objects.hashCode(subtype);
        }
    }

    private record Generation(
            RecipeSource.Token token,
            List<RecipeHolder<AnnihilatorRecipe>> rows,
            Map<Object, RecipeHolder<AnnihilatorRecipe>> byKey) {

        static Generation empty() {
            return new Generation(RecipeSource.NEVER_FILLED, List.of(), Map.of());
        }

        static Generation of(RecipeSource.Token token, Collection<RecipeHolder<?>> loaded) {
            List<RecipeHolder<AnnihilatorRecipe>> rows = new ArrayList<>();
            for (RecipeHolder<?> holder : loaded) {
                if (holder.value() instanceof AnnihilatorRecipe row)
                    rows.add(new RecipeHolder<>(holder.id(), row));
            }
            rows.sort(Comparator.comparing(holder -> holder.id().identifier()));

            Map<Object, RecipeHolder<AnnihilatorRecipe>> byKey = new LinkedHashMap<>();
            for (RecipeHolder<AnnihilatorRecipe> row : rows)
                byKey.put(row.value().key().poolKey(), row);
            return new Generation(token, List.copyOf(byKey.values()), Map.copyOf(byKey));
        }
    }
}
