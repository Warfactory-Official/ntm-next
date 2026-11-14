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

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blockentity.Synced;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class RBMKPanelPeripheral<BE extends BlockEntity & Synced, U>
        extends SnapshotPeripheral<BE> implements IDynamicPeripheral {

    private final Panel<BE, U> panel;

    RBMKPanelPeripheral(BE machine, Panel<BE, U> panel) {
        super(machine, panel.type, panel.count * panel.longStride, panel.count * panel.refStride);
        this.panel = panel;
    }

    @Override
    protected void capture(BE machine) {
        var units = panel.units.apply(machine);
        var fields = panel.fields;
        for (var u = 0; u < panel.count; u++) {
            var unit = units[u];
            for (var f = 0; f < fields.length; f++) {
                fields[f].capture(
                        this,
                        unit,
                        u * panel.longStride + panel.longSlots[f],
                        u * panel.refStride + panel.refSlots[f]);
            }
        }
    }

    @Override
    public String[] getMethodNames() {
        return panel.names;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MethodResult callMethod(
            IComputerAccess computer, ILuaContext context, int method, IArguments args)
            throws LuaException {
        var call = panel.methods[method];
        var unit = args.getInt(0) - 1;
        var known = unit >= 0 && unit < panel.count;
        if (call instanceof Read reader) {
            return MethodResult.of(
                    known
                            ? read(reader.perUnit[unit])
                            : new Object[] {reader.invalid, panel.invalidIndex});
        }
        var write = (Write<BE, U>) call;
        args.escapes();
        return context.executeMainThreadTask(
                () -> {
                    if (!known) return new Object[] {false, panel.invalidIndex};
                    var machine = machine();
                    return write.task.run(machine, panel.units.apply(machine)[unit], args);
                });
    }

    @FunctionalInterface
    public interface FlagSet<U> {
        void set(U unit, boolean value);
    }

    @FunctionalInterface
    public interface WholeSet<U> {
        void set(U unit, long value);
    }

    @FunctionalInterface
    public interface TextSet<U> {
        void set(U unit, String value);
    }

    @FunctionalInterface
    public interface UnitTask<BE, U> {
        Object[] run(BE machine, U unit, IArguments args) throws LuaException;
    }

    @FunctionalInterface
    public interface UnitReadout {
        Object[] read(SnapshotPeripheral<?> published, int firstNumber);
    }

    private sealed interface Field<U> {
        String key();

        default int numbers() {
            return 1;
        }

        default int texts() {
            return 0;
        }

        default @Nullable String setter() {
            return null;
        }

        void capture(RBMKPanelPeripheral<?, U> peripheral, U unit, int number, int text);

        Object value(SnapshotPeripheral<?> published, int number, int text);

        default boolean write(U unit, IArguments args) throws LuaException {
            throw new IllegalStateException(key() + " has no setter");
        }
    }

    private record Flag<U>(
            String key, Predicate<U> get, @Nullable String setter, @Nullable FlagSet<U> set)
            implements Field<U> {
        @Override
        public void capture(RBMKPanelPeripheral<?, U> peripheral, U unit, int number, int text) {
            peripheral.put(number, get.test(unit));
        }

        @Override
        public Object value(SnapshotPeripheral<?> published, int number, int text) {
            return published.booleanAt(number);
        }

        @Override
        public boolean write(U unit, IArguments args) throws LuaException {
            var value = args.getBoolean(1);
            if (get.test(unit) == value) return false;
            set.set(unit, value);
            return true;
        }
    }

    private record Whole<U>(
            String key,
            ToLongFunction<U> get,
            boolean wide,
            @Nullable String setter,
            @Nullable WholeSet<U> set,
            long min,
            long max,
            @Nullable String refusal)
            implements Field<U> {
        @Override
        public void capture(RBMKPanelPeripheral<?, U> peripheral, U unit, int number, int text) {
            peripheral.put(number, get.applyAsLong(unit));
        }

        @Override
        public Object value(SnapshotPeripheral<?> published, int number, int text) {
            return wide ? (Object) published.longAt(number) : (Object) published.intAt(number);
        }

        @Override
        public boolean write(U unit, IArguments args) throws LuaException {
            long value = args.getInt(1);
            if (value < min || value > max) {
                if (refusal != null) throw new LuaException(refusal);
                value = Math.clamp(value, min, max);
            }
            if (get.applyAsLong(unit) == value) return false;
            set.set(unit, value);
            return true;
        }
    }

    private record Text<U>(
            String key, Function<U, String> get, @Nullable String setter, @Nullable TextSet<U> set)
            implements Field<U> {
        @Override
        public int numbers() {
            return 0;
        }

        @Override
        public int texts() {
            return 1;
        }

        @Override
        public void capture(RBMKPanelPeripheral<?, U> peripheral, U unit, int number, int text) {
            peripheral.putRef(text, get.apply(unit));
        }

        @Override
        public Object value(SnapshotPeripheral<?> published, int number, int text) {
            return published.refAt(text);
        }

        @Override
        public boolean write(U unit, IArguments args) throws LuaException {
            var value = args.getString(1);
            if (value.equals(get.apply(unit))) return false;
            set.set(unit, value);
            return true;
        }
    }

    private record Fraction<U>(String key, ToDoubleFunction<U> get) implements Field<U> {
        @Override
        public void capture(RBMKPanelPeripheral<?, U> peripheral, U unit, int number, int text) {
            peripheral.put(number, get.applyAsDouble(unit));
        }

        @Override
        public Object value(SnapshotPeripheral<?> published, int number, int text) {
            return published.doubleAt(number);
        }
    }

    private record Series<U>(String key, Function<U, long[]> get, int length) implements Field<U> {
        @Override
        public int numbers() {
            return length;
        }

        @Override
        public void capture(RBMKPanelPeripheral<?, U> peripheral, U unit, int number, int text) {
            var samples = get.apply(unit);
            for (var i = 0; i < length; i++) peripheral.put(number + i, samples[i]);
        }

        @Override
        public Object value(SnapshotPeripheral<?> published, int number, int text) {
            var samples = new Long[length];
            for (var i = 0; i < length; i++) samples[i] = published.longAt(number + i);
            return samples;
        }
    }

    private sealed interface Method permits Read, Write {}

    private record Read(Object invalid, Readout[] perUnit) implements Method {}

    private record Write<BE, U>(UnitTask<BE, U> task) implements Method {}

    public static final class Panel<BE extends BlockEntity & Synced, U> {

        private final String type;
        private final int count;
        private final Function<BE, U[]> units;
        private final String invalidIndex;
        private final Field<U>[] fields;
        private final int[] longSlots;
        private final int[] refSlots;
        private final int longStride;
        private final int refStride;
        private final String[] names;
        private final Method[] methods;

        private Panel(Builder<BE, U> builder) {
            type = builder.type;
            count = builder.count;
            units = builder.units;
            invalidIndex = "Invalid index (1-" + count + ")";
            @SuppressWarnings("unchecked")
            Field<U>[] declared = builder.fields.toArray(new Field[0]);
            fields = declared;
            longSlots = new int[fields.length];
            refSlots = new int[fields.length];
            int numbers = 0, texts = 0;
            for (var f = 0; f < fields.length; f++) {
                longSlots[f] = numbers;
                refSlots[f] = texts;
                numbers += fields[f].numbers();
                texts += fields[f].texts();
            }
            longStride = numbers;
            refStride = texts;

            List<String> names = new ArrayList<>();
            List<Method> methods = new ArrayList<>();
            names.add(builder.info);
            methods.add(new Read(null, readouts(this::info)));
            for (var f = 0; f < fields.length; f++) {
                var field = fields[f];
                if (field.setter() == null) continue;
                names.add(field.setter());
                methods.add(
                        new Write<BE, U>(
                                (machine, unit, args) -> {
                                    if (field.write(unit, args)) machine.markChanged();
                                    return new Object[] {true};
                                }));
            }
            for (var extra : builder.reads) {
                names.add(extra.name);
                var reader = extra.reader;
                methods.add(
                        new Read(
                                extra.invalid,
                                readouts(
                                        (published, unit) ->
                                                reader.read(published, unit * longStride))));
            }
            for (var extra : builder.writes) {
                names.add(extra.name);
                methods.add(new Write<>(extra.task));
            }
            this.names = names.toArray(String[]::new);
            this.methods = methods.toArray(Method[]::new);
            if (new HashSet<>(names).size() != names.size()) {
                throw new IllegalStateException(type + " declares a method twice");
            }
        }

        @FunctionalInterface
        private interface UnitRead {
            Object[] read(SnapshotPeripheral<?> published, int unit);
        }

        private Readout[] readouts(UnitRead read) {
            var perUnit = new Readout[count];
            for (var u = 0; u < count; u++) {
                var unit = u;
                perUnit[u] = published -> read.read(published, unit);
            }
            return perUnit;
        }

        private Object[] info(SnapshotPeripheral<?> published, int unit) {
            Map<String, Object> table = new HashMap<>(2 * fields.length);
            for (var f = 0; f < fields.length; f++) {
                table.put(
                        fields[f].key(),
                        fields[f].value(
                                published,
                                unit * longStride + longSlots[f],
                                unit * refStride + refSlots[f]));
            }
            return new Object[] {table};
        }

        public static <BE extends BlockEntity & Synced, U> Builder<BE, U> builder(
                String type, String info, int count, Function<BE, U[]> units) {
            return new Builder<>(type, info, count, units);
        }
    }

    public static final class Builder<BE extends BlockEntity & Synced, U> {

        private static final long COLOR_MAX = 0xFFFFFF;

        private final String type;
        private final String info;
        private final int count;
        private final Function<BE, U[]> units;
        private final List<Field<U>> fields = new ArrayList<>();
        private final List<ExtraRead> reads = new ArrayList<>();
        private final List<ExtraWrite<BE, U>> writes = new ArrayList<>();

        private record ExtraRead(String name, Object invalid, UnitReadout reader) {}

        private record ExtraWrite<BE, U>(String name, UnitTask<BE, U> task) {}

        private Builder(String type, String info, int count, Function<BE, U[]> units) {
            this.type = type;
            this.info = info;
            this.count = count;
            this.units = units;
        }

        public Builder<BE, U> flag(String key, Predicate<U> get) {
            fields.add(new Flag<>(key, get, null, null));
            return this;
        }

        public Builder<BE, U> flag(String key, Predicate<U> get, String setter, FlagSet<U> set) {
            fields.add(new Flag<>(key, get, setter, set));
            return this;
        }

        public Builder<BE, U> color(
                String key, ToIntFunction<U> get, String setter, WholeSet<U> set) {
            fields.add(new Whole<>(key, get::applyAsInt, false, setter, set, 0, COLOR_MAX, null));
            return this;
        }

        public Builder<BE, U> number(
                String key,
                ToLongFunction<U> get,
                @Nullable String setter,
                @Nullable WholeSet<U> set) {
            fields.add(
                    new Whole<>(
                            key,
                            get,
                            true,
                            setter,
                            set,
                            Integer.MIN_VALUE,
                            Integer.MAX_VALUE,
                            null));
            return this;
        }

        public Builder<BE, U> bounded(
                String key,
                ToLongFunction<U> get,
                String setter,
                WholeSet<U> set,
                int min,
                int max) {
            fields.add(
                    new Whole<>(
                            key,
                            get,
                            true,
                            setter,
                            set,
                            min,
                            max,
                            "Invalid value (" + min + "-" + max + ")"));
            return this;
        }

        public Builder<BE, U> text(String key, Function<U, String> get) {
            fields.add(new Text<>(key, get, null, null));
            return this;
        }

        public Builder<BE, U> text(
                String key, Function<U, String> get, String setter, TextSet<U> set) {
            fields.add(new Text<>(key, get, setter, set));
            return this;
        }

        public Builder<BE, U> fraction(String key, ToDoubleFunction<U> get) {
            fields.add(new Fraction<>(key, get));
            return this;
        }

        public Builder<BE, U> series(String key, Function<U, long[]> get, int length) {
            fields.add(new Series<>(key, get, length));
            return this;
        }

        public int numberOf(String key) {
            var at = 0;
            for (var field : fields) {
                if (field.key().equals(key)) return at;
                at += field.numbers();
            }
            throw new IllegalArgumentException(type + " declares no " + key);
        }

        public Builder<BE, U> read(String name, Object invalid, UnitReadout reader) {
            reads.add(new ExtraRead(name, invalid, reader));
            return this;
        }

        public Builder<BE, U> write(String name, UnitTask<BE, U> task) {
            writes.add(new ExtraWrite<>(name, task));
            return this;
        }

        public Panel<BE, U> build() {
            return new Panel<>(this);
        }
    }
}
