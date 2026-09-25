// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import com.hbm.NuclearTech;
import com.hbm.packet.SyncWire;
import com.hbm.registration.IRegistrar;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public abstract class GenericRecipes<T extends GenericRecipe, I> extends SerializableRecipe
        implements RecipeType<T> {

    public static final RandomSource RNG = RandomSource.create();

    public static final String POOL_PREFIX_ALT = "alt.";
    public static final String POOL_PREFIX_DISCOVER = "discover.";
    public static final String POOL_PREFIX_SECRET = "secret.";
    public static final String POOL_PREFIX_528 = "528.";
    private static Pools pools = new Pools(RecipeSource.NEVER_FILLED, Map.of(), Map.of());

    private static volatile int tagEpoch;
    private Generation<T, I> generation = Generation.empty();
    private Identifier typeId;

    public static Map<String, List<String>> pools() {
        return livePools().byPool();
    }

    public static @Nullable GenericRecipe pooled(String name) {
        return livePools().byName().get(name);
    }

    public static Collection<String> poolNames() {
        Map<String, List<String>> live = pools();
        return live.isEmpty() ? RecipeSource.clientPoolNames() : live.keySet();
    }

    private static Pools livePools() {
        RecipeSource.Token token = RecipeSource.token();
        Pools held = pools;
        if (token == null || held.token().equals(token)) return held;
        return rebuildPools(token);
    }

    private static synchronized Pools rebuildPools(RecipeSource.Token token) {
        if (pools.token().equals(token)) return pools;
        Map<String, List<String>> byPool = new HashMap<>();
        Map<String, GenericRecipe> byName = new HashMap<>();
        for (SerializableRecipe table : SerializableRecipe.recipeHandlers) {
            table.ensureFilled();
            if (!(table instanceof GenericRecipes<?, ?> generic)) continue;
            for (GenericRecipe recipe : generic.generation.rows()) {
                if (!recipe.isPooled()) continue;
                byName.put(recipe.getInternalName(), recipe);
                for (String pool : recipe.getPools()) {
                    byPool.computeIfAbsent(pool, k -> new ArrayList<>())
                            .add(recipe.getInternalName());
                }
            }
        }
        Map<String, List<String>> frozen = new HashMap<>(byPool.size());
        byPool.forEach((pool, members) -> frozen.put(pool, List.copyOf(members)));
        return pools = new Pools(token, Map.copyOf(frozen), Map.copyOf(byName));
    }

    public static void invalidateIndexes() {
        tagEpoch++;
        SyncWire.invalidateRecipeInputs();
    }

    protected abstract String registryName();

    protected abstract RecipeSerializer<? extends T> serializer();

    @Override
    public final void registerType(IRegistrar registrar) {
        this.typeId = registrar.registerRecipeType(registryName(), this).id();
        registrar.registerRecipeSerializer(registryName(), serializer());
    }

    public final RecipeType<T> type() {
        return this;
    }

    @Override
    public final Identifier datapackTypeId() {
        return typeId;
    }

    @Override
    public final RecipeType<?> datapackType() {
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
        int epoch = tagEpoch;
        Generation<T, I> held = generation;
        if (held.token().equals(token) && held.tagEpoch() == epoch) return;
        synchronized (this) {
            if (generation.token().equals(token) && generation.tagEpoch() == epoch) return;
            fill(RecipeSource.recipes(), token, epoch);
        }
    }

    private Generation<T, I> live() {
        ensureFilled();
        return generation;
    }

    @SuppressWarnings("unchecked")
    private void fill(Collection<RecipeHolder<?>> loaded, RecipeSource.Token token, int epoch) {
        List<T> found = new ArrayList<>();
        for (RecipeHolder<?> holder : loaded) {
            if (holder.value().getType() == this) {
                T recipe = (T) holder.value();
                recipe.id = holder.id();
                found.add(recipe);
            }
        }
        found.sort(
                Comparator.comparingInt((T recipe) -> recipe.order)
                        .thenComparing(GenericRecipe::getInternalName));

        for (T recipe : found) recipe.materialize();

        Map<String, T> byName = new HashMap<>(found.size());
        Map<String, List<GenericRecipe>> groups = new HashMap<>();
        for (T recipe : found) {
            if (byName.putIfAbsent(recipe.getInternalName(), recipe) != null) continue;
            if (recipe.autoSwitchGroup != null) {
                groups.computeIfAbsent(recipe.autoSwitchGroup, k -> new ArrayList<>()).add(recipe);
            }
        }

        List<T> rows = List.copyOf(found);
        Map<String, List<GenericRecipe>> frozenGroups = new HashMap<>(groups.size());
        groups.forEach((group, members) -> frozenGroups.put(group, List.copyOf(members)));

        RecipeIndex<Fluid, T> byFluid =
                RecipeIndex.of(
                        rows,
                        recipe ->
                                recipe.inputFluid != null && recipe.inputFluid.length > 0
                                        ? recipe.inputFluid[0].type()
                                        : null);

        RecipeIndex<Item, T> byItem =
                RecipeIndex.of(
                        rows,
                        (recipe, file) -> {
                            if (recipe.inputItem == null || recipe.inputItem.length == 0) return;
                            int[] filed = {0};
                            recipe.inputItem[0]
                                    .ingredient()
                                    .items()
                                    .forEach(
                                            item -> {
                                                file.accept(item.value());
                                                filed[0]++;
                                            });

                            if (filed[0] == 0) {
                                NuclearTech.LOGGER.warn(
                                        "{}: recipe {} has an input ingredient matching no item, so nothing can "
                                                + "reach it",
                                        datapackTypeId(),
                                        recipe.getInternalName());
                            }
                        });

        this.generation =
                new Generation<>(
                        token,
                        epoch,
                        rows,
                        Map.copyOf(byName),
                        Map.copyOf(frozenGroups),
                        byItem,
                        byFluid,
                        indexRows(rows));
    }

    protected abstract @Nullable I indexRows(List<T> rows);

    protected final I index() {
        I index = live().index();
        return index != null ? index : indexRows(List.of());
    }

    public List<T> recipes() {
        return live().rows();
    }

    public @Nullable T getRecipe(String name) {
        if (name == null || name.isEmpty()) return null;
        return live().byName().get(name);
    }

    public List<GenericRecipe> autoSwitchGroup(@Nullable String group) {
        if (group == null) return List.of();
        List<GenericRecipe> rows = live().groups().get(group);
        return rows == null ? List.of() : rows;
    }

    public RecipeIndex<Item, T> byInputItem() {
        return live().byInputItem();
    }

    public RecipeIndex<Fluid, T> byInputFluid() {
        return live().byInputFluid();
    }

    public @Nullable T findByItem(ItemStack stack, Predicate<T> accepts) {
        if (stack.isEmpty()) return null;
        return byInputItem().find(stack.getItem(), accepts);
    }

    private record Generation<T, I>(
            RecipeSource.Token token,
            int tagEpoch,
            List<T> rows,
            Map<String, T> byName,
            Map<String, List<GenericRecipe>> groups,
            RecipeIndex<Item, T> byInputItem,
            RecipeIndex<Fluid, T> byInputFluid,
            @Nullable I index) {

        static <T, I> Generation<T, I> empty() {
            return new Generation<>(
                    RecipeSource.NEVER_FILLED,
                    0,
                    List.of(),
                    Map.of(),
                    Map.of(),
                    RecipeIndex.empty(),
                    RecipeIndex.empty(),
                    null);
        }
    }

    private record Pools(
            RecipeSource.Token token,
            Map<String, List<String>> byPool,
            Map<String, GenericRecipe> byName) {}
}
