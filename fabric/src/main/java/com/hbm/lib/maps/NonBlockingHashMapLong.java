// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.maps;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongFunction;

import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.fieldOffset;

public class NonBlockingHashMapLong<TypeV> extends AbstractLong2ObjectMap<TypeV>
        implements ConcurrentMap<Long, TypeV>, Cloneable, Serializable {

    private static final long serialVersionUID = 1234123412341234124L;

    private static final int REPROBE_LIMIT = 10;

    private static final long _Obase = U.arrayBaseOffset(Object[].class);
    private static final int _Oscale = U.arrayIndexScale(Object[].class);
    private static final long _Lbase = U.arrayBaseOffset(long[].class);
    private static final int _Lscale = U.arrayIndexScale(long[].class);
    private static final long _chm_offset = fieldOffset(NonBlockingHashMapLong.class, "_chm");
    private static final long _val_1_offset = fieldOffset(NonBlockingHashMapLong.class, "_val_1");
    private static final int MIN_SIZE_LOG = 4;
    private static final int MIN_SIZE = (1 << MIN_SIZE_LOG);
    private static final Object NO_MATCH_OLD = new Object();
    private static final Object MATCH_ANY = new Object();
    private static final Object TOMBSTONE = new Object();
    private static final Prime TOMBPRIME = new Prime(TOMBSTONE);

    private static final long NO_KEY = 0L;
    private final boolean _opt_for_space;
    private transient volatile CHM _chm;

    private transient volatile Object _val_1;
    private transient long _last_resize_milli;
    private transient ObjectCollection<TypeV> _values = null;
    private transient LongSet _keySet = null;
    private transient Long2ObjectMap.FastEntrySet<TypeV> _entrySet = null;

    public NonBlockingHashMapLong() {
        this(MIN_SIZE, true);
    }

    public NonBlockingHashMapLong(final int initial_sz) {
        this(initial_sz, true);
    }

    public NonBlockingHashMapLong(final boolean opt_for_space) {
        this(1, opt_for_space);
    }

    public NonBlockingHashMapLong(final int initial_sz, final boolean opt_for_space) {
        _opt_for_space = opt_for_space;
        initialize(initial_sz);
    }

    private static long rawIndex(final Object[] ary, final int idx) {
        assert idx >= 0 && idx < ary.length;
        return _Obase + ((long) idx * _Oscale);
    }

    private static long rawIndex(final long[] ary, final int idx) {
        assert idx >= 0 && idx < ary.length;
        return _Lbase + ((long) idx * _Lscale);
    }

    private static void print_impl(final int i, final long K, final Object V) {
        String p = (V instanceof Prime) ? "prime_" : "";
        Object V2 = Prime.unbox(V);
        String VS = (V2 == TOMBSTONE) ? "tombstone" : V2.toString();
        System.out.println("[" + i + "]=(" + K + "," + p + VS + ")");
    }

    private static void print2_impl(final int i, final long K, final Object V) {
        if (V != null && Prime.unbox(V) != TOMBSTONE) print_impl(i, K, V);
    }

    private static int reprobe_limit(int len) {
        return REPROBE_LIMIT + (len >> 4);
    }

    private static int hash(long value) {
        value ^= (value >>> 20) ^ (value >>> 12);
        value ^= (value >>> 7) ^ (value >>> 4);
        value += value << 7;
        return (int) value;
    }

    public static NBHMLKeySet newKeySet() {
        return new NBHMLKeySet(new NonBlockingHashMapLong<>());
    }

    public static NBHMLKeySet newKeySet(int initial_capacity) {
        return new NBHMLKeySet(new NonBlockingHashMapLong<>(initial_capacity));
    }

    private final boolean CAS(final long offset, final Object old, final Object nnn) {
        return U.compareAndSetReference(this, offset, old, nnn);
    }

    private final Object CAE(final long offset, final Object expected, final Object value) {
        return U.compareAndExchangeReference(this, offset, expected, value);
    }

    public final void print() {
        System.out.println("=========");
        print_impl(-99, NO_KEY, _val_1);
        _chm.print();
        System.out.println("=========");
    }

    private void print2() {
        System.out.println("=========");
        print2_impl(-99, NO_KEY, _val_1);
        _chm.print();
        System.out.println("=========");
    }

    private void initialize(final int initial_sz) {
        if (initial_sz < 0 || initial_sz > (1 << 30)) {
            throw new IllegalArgumentException(
                    "initial_sz: " + initial_sz + " (expected: 0.." + (1 << 30) + ")");
        }
        int i;
        for (i = MIN_SIZE_LOG; (1 << i) < initial_sz; i++) {}
        _chm = new CHM(this, new ConcurrentAutoTable(), i);
        _val_1 = TOMBSTONE;
        _last_resize_milli = System.currentTimeMillis();
    }

    public int size() {
        return (_val_1 == TOMBSTONE ? 0 : 1) + _chm.size();
    }

    @Override
    public void defaultReturnValue(TypeV rv) {
        if (rv != null) {
            throw new IllegalArgumentException(
                    "NonBlockingHashMapLong requires a null default return value");
        }
        super.defaultReturnValue(null);
    }

    public boolean containsKey(long key) {
        return get(key) != null;
    }

    public boolean contains(Object val) {
        return containsValue(val);
    }

    public TypeV put(long key, TypeV val) {
        return putIfMatch(key, val, NO_MATCH_OLD);
    }

    public TypeV putIfAbsent(long key, TypeV val) {
        return putIfMatch(key, val, TOMBSTONE);
    }

    @Deprecated
    public TypeV putIfAbsentLong(long key, TypeV val) {
        return putIfMatch(key, val, TOMBSTONE);
    }

    public TypeV remove(long key) {
        return putIfMatch(key, TOMBSTONE, NO_MATCH_OLD);
    }

    public boolean remove(long key, Object val) {
        return val.equals(putIfMatch(key, TOMBSTONE, val));
    }

    public TypeV replace(long key, TypeV val) {
        return putIfMatch(key, val, MATCH_ANY);
    }

    public boolean replace(long key, TypeV oldValue, TypeV newValue) {
        return oldValue.equals(putIfMatch(key, newValue, oldValue));
    }

    @SuppressWarnings("unchecked")
    private TypeV putIfMatch(long key, Object newVal, Object oldVal) {
        if (oldVal == null || newVal == null) throw new NullPointerException();
        if (key == NO_KEY) {
            Object curVal = _val_1;
            while (true) {
                if (oldVal != NO_MATCH_OLD
                        && curVal != oldVal
                        && (oldVal != MATCH_ANY || curVal == TOMBSTONE)
                        && !oldVal.equals(curVal)) {
                    return curVal == TOMBSTONE ? null : (TypeV) curVal;
                }
                Object witness = CAE(_val_1_offset, curVal, newVal);
                if (witness == curVal) {
                    return curVal == TOMBSTONE ? null : (TypeV) curVal;
                }
                curVal = witness;
            }
        }
        final Object res = _chm.putIfMatch(key, newVal, oldVal);
        assert !(res instanceof Prime);
        assert res != null;
        return res == TOMBSTONE ? null : (TypeV) res;
    }

    public void clear() {
        CHM newchm = new CHM(this, new ConcurrentAutoTable(), MIN_SIZE_LOG);
        while (!CAS(_chm_offset, _chm, newchm)) {}
        CAS(_val_1_offset, _val_1, TOMBSTONE);
    }

    public boolean containsValue(Object val) {
        if (val == null) return false;
        if (val == _val_1) return true;
        for (TypeV V : values()) if (V == val || V.equals(val)) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    public final TypeV get(long key) {
        if (key == NO_KEY) {
            final Object V = _val_1;
            return V == TOMBSTONE ? null : (TypeV) V;
        }
        final Object V = _chm.get_impl(key);
        assert !(V instanceof Prime);
        assert V != TOMBSTONE;
        return (TypeV) V;
    }

    @Deprecated
    public TypeV get(Object key) {
        return (key instanceof Long) ? get(((Long) key).longValue()) : null;
    }

    @Deprecated
    public TypeV remove(Object key) {
        return (key instanceof Long) ? remove(((Long) key).longValue()) : null;
    }

    @Deprecated
    public boolean remove(Object key, Object Val) {
        return (key instanceof Long) && remove(((Long) key).longValue(), Val);
    }

    @Deprecated
    public boolean containsKey(Object key) {
        return (key instanceof Long) && containsKey(((Long) key).longValue());
    }

    @Deprecated
    public TypeV putIfAbsent(Long key, TypeV val) {
        return putIfAbsent(key.longValue(), val);
    }

    @Deprecated
    public TypeV replace(Long key, TypeV Val) {
        return replace(key.longValue(), Val);
    }

    @Deprecated
    public TypeV put(Long key, TypeV val) {
        return put(key.longValue(), val);
    }

    @Deprecated
    public boolean replace(Long key, TypeV oldValue, TypeV newValue) {
        return replace(key.longValue(), oldValue, newValue);
    }

    private void help_copy() {
        CHM topchm = _chm;
        if (topchm._newchm == null) return;
        topchm.help_copy_impl(false);
    }

    public Enumeration<TypeV> elements() {
        return new SnapshotV();
    }

    public ObjectCollection<TypeV> values() {
        if (_values == null)
            _values =
                    new AbstractObjectCollection<TypeV>() {
                        public void clear() {
                            NonBlockingHashMapLong.this.clear();
                        }

                        public int size() {
                            return NonBlockingHashMapLong.this.size();
                        }

                        public boolean contains(Object v) {
                            return NonBlockingHashMapLong.this.containsValue(v);
                        }

                        public ObjectIterator<TypeV> iterator() {
                            return new SnapshotV();
                        }
                    };
        return _values;
    }

    public Enumeration<Long> keys() {
        return new IteratorLong();
    }

    public LongSet keySet() {
        if (_keySet == null) _keySet = new KeySet();
        return _keySet;
    }

    @SuppressWarnings("unchecked")
    public long[] keySetLong() {
        long[] dom = new long[size()];
        IteratorLong i = (IteratorLong) keySet().iterator();
        int j = 0;
        while (j < dom.length && i.hasNext()) dom[j++] = i.nextLong();
        return dom;
    }

    public Long2ObjectMap.FastEntrySet<TypeV> long2ObjectEntrySet() {
        if (_entrySet == null) _entrySet = new EntrySet();
        return _entrySet;
    }

    public void forEach(LongObjectConsumer<? super TypeV> action) {
        if (action == null) throw new NullPointerException();
        TypeV val1 = get(NO_KEY);
        if (val1 != null) {
            action.accept(NO_KEY, val1);
        }
        CHM topchm;
        while (true) {
            topchm = _chm;
            if (topchm._newchm == null) {
                break;
            }
            topchm.help_copy_impl(true);
        }
        for (int i = 0; i < topchm._keys.length; i++) {
            long k = topchm.keyAcquire(i);
            if (k != NO_KEY) {
                TypeV v = get(k);
                if (v != null) {
                    action.accept(k, v);
                }
            }
        }
    }

    public void forEachFast(LongObjectConsumer<? super TypeV> action) {
        if (action == null) throw new NullPointerException();
        Object v0 = _val_1;
        if (v0 != TOMBSTONE) {

            TypeV vv0 = (TypeV) v0;
            action.accept(NO_KEY, vv0);
        }

        CHM top = _chm;
        if (top._newchm != null) {
            top.help_copy_impl(true);
            top = _chm;
        }

        final long[] keys = top._keys;
        final Object[] vals = top._vals;

        for (int i = 0, len = keys.length; i < len; i++) {
            long k = U.getLongAcquire(keys, rawIndex(keys, i));
            if (k == NO_KEY) continue;

            Object v = U.getReferenceAcquire(vals, rawIndex(vals, i));
            if (v == null || v == TOMBSTONE) continue;

            if (v instanceof Prime) {
                TypeV gv = get(k);
                if (gv != null) action.accept(k, gv);
                continue;
            }

            TypeV vv = (TypeV) v;
            action.accept(k, vv);
        }
    }

    public <R> void forEachFastRef(R ref, LongObjectRefConsumer<? super TypeV, ? super R> action) {
        if (action == null) throw new NullPointerException();
        Object v0 = _val_1;
        if (v0 != TOMBSTONE) {

            TypeV vv0 = (TypeV) v0;
            action.accept(NO_KEY, vv0, ref);
        }

        CHM top = _chm;
        if (top._newchm != null) {
            top.help_copy_impl(true);
            top = _chm;
        }

        final long[] keys = top._keys;
        final Object[] vals = top._vals;

        for (int i = 0, len = keys.length; i < len; i++) {
            long k = U.getLongAcquire(keys, rawIndex(keys, i));
            if (k == NO_KEY) continue;

            Object v = U.getReferenceAcquire(vals, rawIndex(vals, i));
            if (v == null || v == TOMBSTONE) continue;

            if (v instanceof Prime) {
                TypeV gv = get(k);
                if (gv != null) action.accept(k, gv, ref);
                continue;
            }

            TypeV vv = (TypeV) v;
            action.accept(k, vv, ref);
        }
    }

    @Deprecated
    @Override
    public void forEach(BiConsumer<? super Long, ? super TypeV> action) {
        if (action == null) throw new NullPointerException();
        forEach((LongObjectConsumer<? super TypeV>) action::accept);
    }

    public TypeV getOrDefault(long key, TypeV defaultValue) {
        TypeV v = get(key);
        return (v != null) ? v : defaultValue;
    }

    @Deprecated
    @Override
    public TypeV getOrDefault(Object key, TypeV defaultValue) {
        return (key instanceof Long)
                ? getOrDefault(((Long) key).longValue(), defaultValue)
                : defaultValue;
    }

    public void replaceAll(LongObjectBiFunction<? super TypeV, ? extends TypeV> function) {
        if (function == null) throw new NullPointerException();

        if (_val_1 != TOMBSTONE) {
            Object oldVal;
            do {
                oldVal = _val_1;
                if (oldVal == TOMBSTONE) break;
                @SuppressWarnings("unchecked")
                TypeV newVal = function.apply(NO_KEY, (TypeV) oldVal);
                if (newVal == null) throw new NullPointerException();
                if (CAS(_val_1_offset, oldVal, newVal)) break;
            } while (true);
        }

        CHM topchm;
        while (true) {
            topchm = _chm;
            if (topchm._newchm == null) {
                break;
            }
            topchm.help_copy_impl(true);
        }
        for (int i = 0; i < topchm._keys.length; i++) {
            long k = topchm.keyAcquire(i);
            if (k != NO_KEY) {
                while (true) {
                    TypeV oldVal = get(k);
                    if (oldVal == null) {
                        break;
                    }
                    TypeV newVal = function.apply(k, oldVal);
                    if (newVal == null) throw new NullPointerException();
                    if (replace(k, oldVal, newVal)) {
                        break;
                    }
                }
            }
        }
    }

    @Deprecated
    @Override
    public void replaceAll(BiFunction<? super Long, ? super TypeV, ? extends TypeV> function) {
        if (function == null) throw new NullPointerException();
        replaceAll((LongObjectBiFunction<? super TypeV, ? extends TypeV>) function::apply);
    }

    @Override
    public TypeV computeIfAbsent(long key, LongFunction<? extends TypeV> mappingFunction) {
        if (mappingFunction == null) throw new NullPointerException();
        TypeV v = get(key);
        if (v != null) {
            return v;
        }
        TypeV newValue = mappingFunction.apply(key);
        if (newValue == null) {
            return null;
        }
        TypeV concurrentValue = putIfAbsent(key, newValue);
        return (concurrentValue != null) ? concurrentValue : newValue;
    }

    @Override
    public TypeV computeIfAbsent(long key, Long2ObjectFunction<? extends TypeV> mappingFunction) {
        if (mappingFunction == null) throw new NullPointerException();
        TypeV v = get(key);
        if (v != null) {
            return v;
        }
        if (!mappingFunction.containsKey(key)) {
            return defaultReturnValue();
        }
        TypeV newValue = mappingFunction.get(key);
        if (newValue == null) {
            throw new NullPointerException();
        }
        TypeV concurrentValue = putIfAbsent(key, newValue);
        return (concurrentValue != null) ? concurrentValue : newValue;
    }

    public TypeV computeIfPresent(
            long key, LongObjectBiFunction<? super TypeV, ? extends TypeV> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        while (true) {
            TypeV oldVal = get(key);
            if (oldVal == null) {
                return null;
            }
            TypeV newVal = remappingFunction.apply(key, oldVal);
            if (newVal != null) {
                if (replace(key, oldVal, newVal)) {
                    return newVal;
                }
            } else {
                if (remove(key, oldVal)) {
                    return null;
                }
            }
        }
    }

    @Override
    public TypeV computeIfPresent(
            long key, BiFunction<? super Long, ? super TypeV, ? extends TypeV> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        LongObjectBiFunction<? super TypeV, ? extends TypeV> primitive = remappingFunction::apply;
        return computeIfPresent(key, primitive);
    }

    @Deprecated
    @Override
    public TypeV computeIfAbsent(
            Long key, Function<? super Long, ? extends TypeV> mappingFunction) {
        if (mappingFunction == null) throw new NullPointerException();
        return computeIfAbsent(key.longValue(), mappingFunction::apply);
    }

    @Deprecated
    @Override
    public TypeV computeIfPresent(
            Long key, BiFunction<? super Long, ? super TypeV, ? extends TypeV> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        LongObjectBiFunction<? super TypeV, ? extends TypeV> primitive = remappingFunction::apply;
        return computeIfPresent(key.longValue(), primitive);
    }

    public TypeV compute(
            long key, LongObjectBiFunction<? super TypeV, ? extends TypeV> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        while (true) {
            TypeV oldVal = get(key);
            TypeV newVal = remappingFunction.apply(key, oldVal);
            if (newVal == null) {
                if (oldVal != null) {
                    if (remove(key, oldVal)) {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                if (oldVal != null) {
                    if (replace(key, oldVal, newVal)) {
                        return newVal;
                    }
                } else {
                    if (putIfAbsent(key, newVal) == null) {
                        return newVal;
                    }
                }
            }
        }
    }

    @Override
    public TypeV compute(
            long key, BiFunction<? super Long, ? super TypeV, ? extends TypeV> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        LongObjectBiFunction<? super TypeV, ? extends TypeV> primitive = remappingFunction::apply;
        return compute(key, primitive);
    }

    @Deprecated
    @Override
    public TypeV compute(
            Long key, BiFunction<? super Long, ? super TypeV, ? extends TypeV> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        LongObjectBiFunction<? super TypeV, ? extends TypeV> primitive = remappingFunction::apply;
        return compute(key.longValue(), primitive);
    }

    public TypeV merge(
            long key,
            TypeV value,
            BiFunction<? super TypeV, ? super TypeV, ? extends TypeV> remappingFunction) {
        if (value == null || remappingFunction == null) throw new NullPointerException();
        while (true) {
            TypeV oldVal = get(key);
            if (oldVal == null) {
                if (putIfAbsent(key, value) == null) {
                    return value;
                }
            } else {
                TypeV newVal = remappingFunction.apply(oldVal, value);
                if (newVal != null) {
                    if (replace(key, oldVal, newVal)) {
                        return newVal;
                    }
                } else {
                    if (remove(key, oldVal)) {
                        return null;
                    }
                }
            }
        }
    }

    @Deprecated
    @Override
    public TypeV merge(
            Long key,
            TypeV value,
            BiFunction<? super TypeV, ? super TypeV, ? extends TypeV> remappingFunction) {
        if (value == null || remappingFunction == null) throw new NullPointerException();
        return merge(key.longValue(), value, remappingFunction);
    }

    public void putAll(final Long2ObjectMap<? extends TypeV> m) {
        if (m == null) throw new NullPointerException();
        if (m == this || m.isEmpty()) return;
        final ObjectSet<? extends Long2ObjectMap.Entry<? extends TypeV>> it =
                m.long2ObjectEntrySet();
        for (Long2ObjectMap.Entry<? extends TypeV> e : it) {
            final long k = e.getLongKey();
            final TypeV v = e.getValue();
            if (v == null) throw new NullPointerException();
            put(k, v);
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        for (long K : keySet()) {
            final Object V = get(K);
            s.writeLong(K);
            s.writeObject(V);
        }
        s.writeLong(NO_KEY);
        s.writeObject(null);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        initialize(MIN_SIZE);
        for (; ; ) {
            final long K = s.readLong();
            final TypeV V = (TypeV) s.readObject();
            if (K == NO_KEY && V == null) break;
            put(K, V);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public NonBlockingHashMapLong<TypeV> clone() {
        try {
            NonBlockingHashMapLong<TypeV> t = (NonBlockingHashMapLong<TypeV>) super.clone();
            t._values = null;
            t._keySet = null;
            t._entrySet = null;
            t.clear();
            for (long K : keySetLong()) t.put(K, get(K));
            return t;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    private record Prime(Object _V) {

        static Object unbox(Object V) {
            return V instanceof Prime ? ((Prime) V)._V : V;
        }
    }

    private static final class CHM implements Serializable {
        private static final long _newchmOffset = fieldOffset(CHM.class, "_newchm");
        private static final long _resizersOffset = fieldOffset(CHM.class, "_resizers");
        private static final long _copyIdxOffset = fieldOffset(CHM.class, "_copyIdx");
        private static final long _copyDoneOffset = fieldOffset(CHM.class, "_copyDone");
        final NonBlockingHashMapLong _nbhml;
        final long[] _keys;
        final Object[] _vals;
        volatile CHM _newchm;
        volatile long _resizers;
        volatile long _copyIdx = 0;
        volatile long _copyDone = 0;
        private ConcurrentAutoTable _size;
        private ConcurrentAutoTable _slots;

        CHM(final NonBlockingHashMapLong nbhml, ConcurrentAutoTable size, final int logsize) {
            _nbhml = nbhml;
            _size = size;
            _slots = new ConcurrentAutoTable();
            _keys = new long[1 << logsize];
            _vals = new Object[1 << logsize];
        }

        public int size() {
            return (int) _size.get();
        }

        public int slots() {
            return (int) _slots.get();
        }

        boolean CAS_newchm(CHM newchm) {
            return U.compareAndSetReference(this, _newchmOffset, null, newchm);
        }

        private long CAE_key(int idx, long expected, long key) {
            return U.compareAndExchangeLong(_keys, rawIndex(_keys, idx), expected, key);
        }

        private Object CAE_val(int idx, Object expected, Object val) {
            return U.compareAndExchangeReference(_vals, rawIndex(_vals, idx), expected, val);
        }

        private long keyAcquire(int idx) {
            return U.getLongAcquire(_keys, rawIndex(_keys, idx));
        }

        private Object valAcquire(int idx) {
            return U.getReferenceAcquire(_vals, rawIndex(_vals, idx));
        }

        void clear() {
            _size = new ConcurrentAutoTable();
            _slots = new ConcurrentAutoTable();
            Arrays.fill(_keys, 0);
            Arrays.fill(_vals, null);
        }

        private void print() {
            for (int i = 0; i < _keys.length; i++) {
                long K = keyAcquire(i);
                if (K != NO_KEY) print_impl(i, K, valAcquire(i));
            }
            CHM newchm = _newchm;
            if (newchm != null) {
                System.out.println("----");
                newchm.print();
            }
        }

        private void print2() {
            for (int i = 0; i < _keys.length; i++) {
                long K = keyAcquire(i);
                if (K != NO_KEY) print2_impl(i, K, valAcquire(i));
            }
            CHM newchm = _newchm;
            if (newchm != null) {
                System.out.println("----");
                newchm.print2();
            }
        }

        private Object get_impl(final long key) {
            final int hash = hash(key);
            final int len = _keys.length;
            int idx = (hash & (len - 1));

            int reprobe_cnt = 0;
            while (true) {
                long K = _keys[idx];
                if (K == NO_KEY && (K = keyAcquire(idx)) == NO_KEY) return null;

                if (key == K) {
                    final Object V = valAcquire(idx);
                    if (!(V instanceof Prime)) {
                        if (V == TOMBSTONE) return null;
                        return V;
                    }
                    return copy_slot_and_check(idx, key).get_impl(key);
                }
                if (++reprobe_cnt >= reprobe_limit(len))
                    return _newchm == null ? null : copy_slot_and_check(idx, key).get_impl(key);

                idx = (idx + 1) & (len - 1);
            }
        }

        private Object putIfMatch(final long key, final Object putval, final Object expVal) {
            final int hash = hash(key);
            assert putval != null;
            assert !(putval instanceof Prime);
            assert !(expVal instanceof Prime);
            final int len = _keys.length;
            int idx = (hash & (len - 1));

            int reprobe_cnt = 0;
            long K;
            Object V;
            while (true) {
                K = _keys[idx];
                if (K == NO_KEY) K = keyAcquire(idx);
                if (K == NO_KEY) {
                    if (putval == TOMBSTONE) return TOMBSTONE;
                    if (expVal == MATCH_ANY) return TOMBSTONE;
                    long witness = CAE_key(idx, NO_KEY, key);
                    if (witness == NO_KEY) {
                        _slots.add(1);
                        V = valAcquire(idx);
                        break;
                    }
                    K = witness;
                    assert K != NO_KEY;
                }
                if (K == key) {
                    V = valAcquire(idx);
                    break;
                }

                if (++reprobe_cnt >= reprobe_limit(len)) {
                    final CHM newchm = resize();
                    if (expVal != null) _nbhml.help_copy();
                    return newchm.putIfMatch(key, putval, expVal);
                }

                idx = (idx + 1) & (len - 1);
            }

            while (true) {
                if (putval == V) return V;

                if ((V == null && tableFull(reprobe_cnt, len)) || V instanceof Prime) {
                    resize();
                    return copy_slot_and_check(idx, expVal).putIfMatch(key, putval, expVal);
                }

                if (expVal != NO_MATCH_OLD
                        && V != expVal
                        && (expVal != MATCH_ANY || V == TOMBSTONE || V == null)
                        && !(V == null && expVal == TOMBSTONE)
                        && (expVal == null || !expVal.equals(V)))
                    return (V == null) ? TOMBSTONE : V;

                Object witness = CAE_val(idx, V, putval);
                if (witness == V) break;

                V = witness;

                if (V instanceof Prime)
                    return copy_slot_and_check(idx, expVal).putIfMatch(key, putval, expVal);
            }

            if (expVal != null) {
                if ((V == null || V == TOMBSTONE) && putval != TOMBSTONE) _size.add(1);
                if (!(V == null || V == TOMBSTONE) && putval == TOMBSTONE) _size.add(-1);
            }

            return (V == null && expVal != null) ? TOMBSTONE : V;
        }

        private boolean tableFull(int reprobe_cnt, int len) {
            return reprobe_cnt >= REPROBE_LIMIT
                    && (reprobe_cnt >= reprobe_limit(len) || _slots.estimate_get() >= (len >> 1));
        }

        private CHM resize() {
            CHM newchm = _newchm;
            if (newchm != null) return newchm;

            int oldlen = _keys.length;
            int sz = size();
            int newsz = sz;

            if (_nbhml._opt_for_space) {
                if (sz >= (oldlen >> 1)) newsz = oldlen << 1;
            } else {
                if (sz >= (oldlen >> 2)) {
                    newsz = oldlen << 1;
                    if (sz >= (oldlen >> 1)) newsz = oldlen << 2;
                }
            }

            long tm = System.currentTimeMillis();
            if (newsz <= oldlen && tm <= _nbhml._last_resize_milli + 10000) newsz = oldlen << 1;

            if (newsz < oldlen) newsz = oldlen;

            int log2;
            for (log2 = MIN_SIZE_LOG; (1 << log2) < newsz; log2++)
                ;
            long len = ((1L << log2) << 1) + 2;
            if ((int) len != len) {
                log2 = 30;
                len = (1L << log2) + 2;
                if (sz > ((len >> 2) + (len >> 1))) throw new RuntimeException("Table is full.");
            }

            long r = U.getAndAddLong(this, _resizersOffset, 1);
            long megs = ((((1L << log2) << 1) + 8) << 3) >> 20;
            if (r >= 2 && megs > 0) {
                newchm = _newchm;
                if (newchm != null) return newchm;
                try {
                    Thread.sleep(megs);
                } catch (Exception e) {
                }
            }
            newchm = _newchm;
            if (newchm != null) return newchm;

            newchm = new CHM(_nbhml, _size, log2);

            if (_newchm != null) return _newchm;

            if (CAS_newchm(newchm)) {
            } else newchm = _newchm;
            return newchm;
        }

        private void help_copy_impl(final boolean copy_all) {
            final CHM newchm = _newchm;
            assert newchm != null;
            int oldlen = _keys.length;
            final int MIN_COPY_WORK = Math.min(oldlen, 1024);

            int panic_start = -1;
            int copyidx = -9999;
            while (_copyDone < oldlen) {
                if (panic_start == -1) {
                    copyidx = (int) U.getAndAddLong(this, _copyIdxOffset, MIN_COPY_WORK);
                    if (!(copyidx < (oldlen << 1))) panic_start = copyidx;
                }

                int workdone = 0;
                for (int i = 0; i < MIN_COPY_WORK; i++)
                    if (copy_slot((copyidx + i) & (oldlen - 1))) workdone++;
                if (workdone > 0) copy_check_and_promote(workdone);

                copyidx += MIN_COPY_WORK;
                if (!copy_all && panic_start == -1) return;
            }
            copy_check_and_promote(0);
        }

        private CHM copy_slot_and_check(int idx, Object should_help) {
            assert _newchm != null;
            if (copy_slot(idx)) copy_check_and_promote(1);
            if (should_help != null) _nbhml.help_copy();
            return _newchm;
        }

        private void copy_check_and_promote(int workdone) {
            int oldlen = _keys.length;
            long nowDone = U.getAndAddLong(this, _copyDoneOffset, workdone) + workdone;

            if (nowDone == oldlen
                    && _nbhml._chm == this
                    && _nbhml.CAS(_chm_offset, this, _newchm)) {
                _nbhml._last_resize_milli = System.currentTimeMillis();
            }
        }

        private boolean copy_slot(int idx) {
            long key = keyAcquire(idx);
            if (key == NO_KEY) {
                long filler = idx + _keys.length;
                long witness = CAE_key(idx, NO_KEY, filler);
                key = witness == NO_KEY ? filler : witness;
            }

            Object oldval = valAcquire(idx);
            while (!(oldval instanceof Prime)) {
                final Prime box =
                        (oldval == null || oldval == TOMBSTONE) ? TOMBPRIME : new Prime(oldval);
                Object witness = CAE_val(idx, oldval, box);
                if (witness == oldval) {
                    if (box == TOMBPRIME) return true;
                    oldval = box;
                    break;
                }
                oldval = witness;
            }
            if (oldval == TOMBPRIME) return false;

            Object old_unboxed = ((Prime) oldval)._V;
            assert old_unboxed != TOMBSTONE;
            boolean copied_into_new = (_newchm.putIfMatch(key, old_unboxed, null) == null);

            while (oldval != TOMBPRIME) {
                Object witness = CAE_val(idx, oldval, TOMBPRIME);
                if (witness == oldval) break;
                oldval = witness;
            }

            return copied_into_new;
        }
    }

    public static final class NBHMLKeySet extends AbstractLongSet implements LongSet, Serializable {
        private static final long serialVersionUID = 1L;
        private static final Object PRESENT = new Object();

        private final NonBlockingHashMapLong<Object> map;

        private NBHMLKeySet(NonBlockingHashMapLong<Object> map) {
            if (map == null) throw new NullPointerException("map");
            this.map = map;
        }

        @Override
        public LongIterator iterator() {
            return map.new IteratorLong();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean contains(long k) {
            return map.containsKey(k);
        }

        @Override
        public boolean add(long k) {
            return map.putIfAbsent(k, PRESENT) == null;
        }

        @Override
        public boolean remove(long k) {
            return map.remove(k) != null;
        }

        @Override
        public void clear() {
            map.clear();
        }
    }

    private class SnapshotV implements ObjectIterator<TypeV>, Enumeration<TypeV> {
        final CHM _sschm;
        private int _idx;
        private long _nextK, _prevK;
        private TypeV _nextV, _prevV;

        public SnapshotV() {
            CHM topchm;
            while (true) {
                topchm = _chm;
                if (topchm._newchm == null) break;
                topchm.help_copy_impl(true);
            }
            _sschm = topchm;
            _idx = -1;
            next();
        }

        int length() {
            return _sschm._keys.length;
        }

        long key(final int idx) {
            return _sschm.keyAcquire(idx);
        }

        public boolean hasNext() {
            return _nextV != null;
        }

        public TypeV next() {
            if (_idx != -1 && _nextV == null) throw new NoSuchElementException();
            _prevK = _nextK;
            _prevV = _nextV;
            _nextV = null;
            if (_idx == -1) {
                _idx = 0;
                _nextK = NO_KEY;
                if ((_nextV = get(_nextK)) != null) return _prevV;
            }
            while (_idx < length()) {
                _nextK = key(_idx++);
                if (_nextK != NO_KEY && (_nextV = get(_nextK)) != null) break;
            }
            return _prevV;
        }

        public void removeKey() {
            if (_prevV == null) throw new IllegalStateException();
            NonBlockingHashMapLong.this.putIfMatch(_prevK, TOMBSTONE, NO_MATCH_OLD);
            _prevV = null;
        }

        @Override
        public void remove() {
            removeKey();
        }

        public TypeV nextElement() {
            return next();
        }

        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public int skip(int n) {
            int i = n;
            while (i-- > 0 && hasNext()) {
                next();
            }
            return n - (i + 1);
        }
    }

    public class IteratorLong implements LongIterator, Enumeration<Long> {
        private final SnapshotV _ss;

        public IteratorLong() {
            _ss = new SnapshotV();
        }

        public void remove() {
            _ss.removeKey();
        }

        public Long next() {
            _ss.next();
            return _ss._prevK;
        }

        public long nextLong() {
            _ss.next();
            return _ss._prevK;
        }

        public boolean hasNext() {
            return _ss.hasNext();
        }

        public Long nextElement() {
            return next();
        }

        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public int skip(int n) {
            return _ss.skip(n);
        }
    }

    private final class KeySet extends AbstractLongSet {
        @Override
        public LongIterator iterator() {
            return new IteratorLong();
        }

        @Override
        public int size() {
            return NonBlockingHashMapLong.this.size();
        }

        @Override
        public boolean contains(long k) {
            return NonBlockingHashMapLong.this.containsKey(k);
        }

        @Override
        public boolean remove(long k) {
            return NonBlockingHashMapLong.this.remove(k) != null;
        }

        @Override
        public void clear() {
            NonBlockingHashMapLong.this.clear();
        }
    }

    private final class NBHMLEntry implements Long2ObjectMap.Entry<TypeV> {
        long _k;
        TypeV _v;

        NBHMLEntry() {}

        NBHMLEntry(long k, TypeV v) {
            _k = k;
            _v = v;
        }

        @Override
        public long getLongKey() {
            return _k;
        }

        @Override
        @Deprecated
        public Long getKey() {
            return _k;
        }

        @Override
        public TypeV getValue() {
            return _v;
        }

        @Override
        public TypeV setValue(TypeV val) {
            if (val == null) throw new NullPointerException();
            _v = val;
            return put(_k, val);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            final long k;
            if (e instanceof Long2ObjectMap.Entry<?> le) k = le.getLongKey();
            else if (e.getKey() instanceof Long l) k = l;
            else return false;
            return k == _k && Objects.equals(_v, e.getValue());
        }

        @Override
        public int hashCode() {
            return Long.hashCode(_k) ^ Objects.hashCode(_v);
        }

        @Override
        public String toString() {
            return _k + "=" + _v;
        }
    }

    private class SnapshotE implements ObjectIterator<Long2ObjectMap.Entry<TypeV>> {
        final SnapshotV _ss;

        public SnapshotE() {
            _ss = new SnapshotV();
        }

        public void remove() {
            _ss.removeKey();
        }

        public Long2ObjectMap.Entry<TypeV> next() {
            _ss.next();
            return new NBHMLEntry(_ss._prevK, _ss._prevV);
        }

        public boolean hasNext() {
            return _ss.hasNext();
        }

        @Override
        public int skip(int n) {
            return _ss.skip(n);
        }
    }

    private class FastSnapshotE implements ObjectIterator<Long2ObjectMap.Entry<TypeV>> {
        final SnapshotV _ss;
        final NBHMLEntry _entry;

        public FastSnapshotE() {
            _ss = new SnapshotV();
            _entry = new NBHMLEntry();
        }

        public void remove() {
            _ss.removeKey();
        }

        public Long2ObjectMap.Entry<TypeV> next() {
            _ss.next();
            _entry._k = _ss._prevK;
            _entry._v = _ss._prevV;
            return _entry;
        }

        public boolean hasNext() {
            return _ss.hasNext();
        }

        @Override
        public int skip(int n) {
            return _ss.skip(n);
        }
    }

    private final class EntrySet extends AbstractObjectSet<Long2ObjectMap.Entry<TypeV>>
            implements Long2ObjectMap.FastEntrySet<TypeV> {

        @Override
        public ObjectIterator<Long2ObjectMap.Entry<TypeV>> iterator() {
            return new SnapshotE();
        }

        @Override
        public ObjectIterator<Long2ObjectMap.Entry<TypeV>> fastIterator() {
            return new FastSnapshotE();
        }

        @Override
        public int size() {
            return NonBlockingHashMapLong.this.size();
        }

        @Override
        public void clear() {
            NonBlockingHashMapLong.this.clear();
        }

        @Override
        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            final long k;
            if (e instanceof Long2ObjectMap.Entry<?> le) {
                k = le.getLongKey();
            } else if (e.getKey() instanceof Long l) {
                k = l;
            } else return false;

            final TypeV v = get(k);
            return v != null && v.equals(e.getValue());
        }

        @Override
        public boolean remove(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            final long k;
            if (e instanceof Long2ObjectMap.Entry<?> le) {
                k = le.getLongKey();
            } else if (e.getKey() instanceof Long l) {
                k = l;
            } else return false;
            return NonBlockingHashMapLong.this.remove(k, e.getValue());
        }
    }
}
