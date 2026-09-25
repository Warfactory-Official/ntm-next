// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "radsim/abi.h"
#include <algorithm>
#include <array>
#include <bit>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <limits>
#include <memory>
#include <string>
#include <type_traits>
#include <unordered_map>
#include <utility>
#include <vector>

#include "radsim/world_state.hpp"
#include "radsim/runtime_support.hpp"

namespace hbm::radsim::detail {
    int32_t section_id_for_chunk_sy(const ChunkTable &chunks, int32_t chunk_id, int32_t sy);
    void relink_note_chunk_unloading(RadWorld &world, int32_t chunk_id);

#if defined(__GNUC__) && defined(__x86_64__)
#define HBM_RADSIM_X86_SIMD 1
#include <immintrin.h>
#else
#define HBM_RADSIM_X86_SIMD 0
#endif

    double multi_face_dist(const SectionTable &sections, int32_t section_id, uint16_t pocket, int32_t face);

    uint64_t checksum_mask_words(const uint64_t *words);

    int32_t flood_fill_pockets(const std::array<uint64_t, kMaskWordsPerSection> &words,
                               std::array<uint16_t, kSectionVoxelCount> &pocket_map,
                               std::vector<int32_t> &pocket_volume, std::vector<int64_t> &pocket_sum_xyz);

    void mark_chunk_loaded(RadWorld &world, int64_t ck);

    void mark_chunk_unloaded(RadWorld &world, int32_t id);

    void mark_chunk_removed(RadWorld &world, int64_t ck);

    void mark_section_topology_dirty(RadWorld &world, int32_t section_id);

    void rebuild_section_topology(RadWorld &world, int32_t chunk_id, int32_t sy, int32_t section_id);

    void apply_section_diffusivity(RadWorld &world, int32_t section_id, int32_t value_count, const float *values);

    void queue_section_edit(RadWorld &world, int32_t section_id, const PendingLocalEdit &edit);

    void queue_section_edit_fields(RadWorld &world, int32_t section_id, uint16_t local, double add_value, bool has_set,
                                   double set_value, uint64_t set_seq);

    struct EmissionAccumulator {
        double weight_sum = 0.0;
        double numerator = 0.0;

        void add(double emission, double saturation, double c) noexcept {
            if (emission == 0.0 || saturation == 0.0)
                return;
            if (!(emission * (saturation - c) > 0.0))
                return;
            const double magnitude = (emission < 0.0) ? -emission : emission;
            const double scale = (saturation < 0.0) ? -saturation : saturation;
            weight_sum += magnitude / scale;
            numerator += (saturation < 0.0) ? -magnitude : magnitude;
        }

        [[nodiscard]] double relax(double c) const noexcept {
            if (weight_sum <= 0.0)
                return c;

            if (weight_sum > 1.0)
                return numerator / weight_sum;
            return c + (numerator - c * weight_sum);
        }
    };

    struct PendingEditScratch {
        std::vector<double> add_acc;
        std::vector<double> set_val;
        std::vector<uint64_t> best_seq;
        std::vector<uint8_t> has_set;
        std::vector<EmissionAccumulator> emission;

        void reset(size_t n) {
            add_acc.assign(n, 0.0);
            set_val.assign(n, 0.0);
            best_seq.assign(n, 0u);
            has_set.assign(n, 0u);
            emission.assign(n, EmissionAccumulator{});
        }
    };

    inline void resolve_pending_edits(const std::vector<PendingLocalEdit> &edits, const SectionPocketMap *pocket_of,
                                      size_t pocket_count, double *density, PendingEditScratch &scratch) {
        if (pocket_count == 0u)
            return;
        scratch.reset(pocket_count);

        for (const PendingLocalEdit &e : edits) {
            if ((e.flags & kEditFlagHasSet) == 0u)
                continue;
            size_t p = 0u;
            if (pocket_of != nullptr) {
                const uint16_t pocket = (*pocket_of)[static_cast<size_t>(e.local & 0x0FFFu)];
                if (pocket == kNoPocket || pocket >= pocket_count)
                    continue;
                p = static_cast<size_t>(pocket);
            }
            if (e.set_seq > scratch.best_seq[p]) {
                scratch.best_seq[p] = e.set_seq;
                scratch.set_val[p] = e.set_value;
                scratch.has_set[p] = 1u;
            }
        }

        for (const PendingLocalEdit &e : edits) {
            size_t p = 0u;
            if (pocket_of != nullptr) {
                const uint16_t pocket = (*pocket_of)[static_cast<size_t>(e.local & 0x0FFFu)];
                if (pocket == kNoPocket || pocket >= pocket_count)
                    continue;
                p = static_cast<size_t>(pocket);
            }
            if ((e.flags & kEditFlagHasSaturation) != 0u) {
                scratch.emission[p].add(e.add_value, e.saturation,
                                        (scratch.has_set[p] != 0u) ? scratch.set_val[p] : density[p]);
            } else {
                scratch.add_acc[p] += e.add_value;
            }
        }

        for (size_t p = 0u; p < pocket_count; p++) {
            const double base = (scratch.has_set[p] != 0u) ? scratch.set_val[p] : density[p];
            density[p] = scratch.emission[p].relax(base) + scratch.add_acc[p];
        }
    }

    int64_t pocket_key(int64_t section_key, uint16_t pocket_index);

    double java_compat_exp(double x);

    inline int32_t sign_extend_bits(uint32_t value, int bits) {
        const uint32_t sign = 1u << static_cast<uint32_t>(bits - 1);
        return static_cast<int32_t>((value ^ sign) - sign);
    }

    inline ChunkCoord decode_chunk_key(int64_t ck) {
        const uint64_t raw = static_cast<uint64_t>(ck);
        ChunkCoord coord;
        coord.x = static_cast<int32_t>(raw & 0xFFFF'FFFFu);
        coord.z = static_cast<int32_t>(raw >> 32u);
        return coord;
    }

    inline int64_t encode_chunk_key(int32_t x, int32_t z) {
        const uint64_t raw = static_cast<uint64_t>(static_cast<uint32_t>(x)) |
                             (static_cast<uint64_t>(static_cast<uint32_t>(z)) << 32u);
        return static_cast<int64_t>(raw);
    }

    inline int32_t decode_section_x(int64_t section_key) {
        const uint32_t raw = static_cast<uint32_t>((static_cast<uint64_t>(section_key) >> 42u) & 0x3F'FFFFu);
        return sign_extend_bits(raw, 22);
    }

    inline int32_t decode_section_y(int64_t section_key) {
        const uint32_t raw = static_cast<uint32_t>(static_cast<uint64_t>(section_key) & 0x0F'FFFFu);
        return sign_extend_bits(raw, 20);
    }

    inline int32_t decode_section_z(int64_t section_key) {
        const uint32_t raw = static_cast<uint32_t>((static_cast<uint64_t>(section_key) >> 20u) & 0x3F'FFFFu);
        return sign_extend_bits(raw, 22);
    }

    inline int64_t section_to_chunk_key(int64_t section_key) {
        return encode_chunk_key(decode_section_x(section_key), decode_section_z(section_key));
    }

    static_assert(std::endian::native == std::endian::little,
                  "hbm_radsim currently assumes native little-endian layout");

    template <typename T> inline T read_native_le(const uint8_t *ptr) {
        static_assert(std::is_trivially_copyable_v<T>);
        std::array<std::byte, sizeof(T)> bytes{};
        std::memcpy(bytes.data(), ptr, sizeof(T));
        return std::bit_cast<T>(bytes);
    }

    template <typename T> inline void write_native_le(uint8_t *ptr, T value) {
        static_assert(std::is_trivially_copyable_v<T>);
        const auto bytes = std::bit_cast<std::array<std::byte, sizeof(T)>>(value);
        std::memcpy(ptr, bytes.data(), sizeof(T));
    }

    inline uint64_t read_u64_le(const uint8_t *ptr) { return read_native_le<uint64_t>(ptr); }

    inline uint16_t read_u16_le(const uint8_t *ptr) { return read_native_le<uint16_t>(ptr); }

    inline double read_f64_le(const uint8_t *ptr) { return read_native_le<double>(ptr); }

    inline int64_t encode_section_key(int32_t x, int32_t y, int32_t z) {
        const uint64_t raw = ((static_cast<uint64_t>(static_cast<uint32_t>(x)) & 0x3F'FFFFu) << 42u) |
                             ((static_cast<uint64_t>(static_cast<uint32_t>(z)) & 0x3F'FFFFu) << 20u) |
                             (static_cast<uint64_t>(static_cast<uint32_t>(y)) & 0x0F'FFFFu);
        return static_cast<int64_t>(raw);
    }

    inline void write_u32_le(uint8_t *ptr, uint32_t value) { write_native_le<uint32_t>(ptr, value); }

    inline void write_u16_le(uint8_t *ptr, uint16_t value) { write_native_le<uint16_t>(ptr, value); }

    inline void write_u64_le(uint8_t *ptr, uint64_t value) { write_native_le<uint64_t>(ptr, value); }

    inline void write_f64_le(uint8_t *ptr, double value) { write_native_le<double>(ptr, value); }

    inline uint8_t parity_bucket_for_chunk(int64_t ck) {
        const ChunkCoord coord = decode_chunk_key(ck);
        const int bx = coord.x & 1;
        const int bz = coord.z & 1;
        return static_cast<uint8_t>(bx | (bz << 1));
    }

    inline bool chunk_id_in_range(const ChunkTable &table, int32_t id) {
        return id >= 0 && static_cast<size_t>(id) < table.ck_by_id.size();
    }

    inline int32_t sections_per_chunk(const ChunkTable &table) { return table.sections_per_chunk; }

    inline size_t chunk_word_base(const ChunkTable &table, int32_t chunk_id) {
        return table.words_per_chunk == 1 ? static_cast<size_t>(chunk_id)
                                          : static_cast<size_t>(chunk_id) * static_cast<size_t>(table.words_per_chunk);
    }

    inline size_t chunk_word_index(const ChunkTable &table, int32_t chunk_id, int32_t sy) {
        return chunk_word_base(table, chunk_id) + (static_cast<uint32_t>(sy) >> 5u);
    }

    inline bool sy_in_range(const ChunkTable &table, int32_t sy) { return sy >= 0 && sy < table.sections_per_chunk; }

    inline int32_t *section_ids_of(ChunkTable &table, int32_t chunk_id) {
        return table.section_id_by_sy.data() +
               static_cast<size_t>(chunk_id) * static_cast<size_t>(table.sections_per_chunk);
    }

    inline const int32_t *section_ids_of(const ChunkTable &table, int32_t chunk_id) {
        return table.section_id_by_sy.data() +
               static_cast<size_t>(chunk_id) * static_cast<size_t>(table.sections_per_chunk);
    }

    inline void ensure_chunk_slot(ChunkTable &table, int32_t id) {
        const size_t need = static_cast<size_t>(id) + 1u;
        if (table.ck_by_id.size() < need) {
            table.buffer_generation++;
            table.ck_by_id.resize(need, kInvalidChunkKey);
        }
        if (table.east_neighbor_id.size() < need)
            table.east_neighbor_id.resize(need, kNoNeighbor);
        if (table.south_neighbor_id.size() < need)
            table.south_neighbor_id.resize(need, kNoNeighbor);
        const size_t metadata_need = need * static_cast<size_t>(table.words_per_chunk);
        if (table.kinds_by_id.size() < metadata_need)
            table.kinds_by_id.resize(metadata_need, 0u);
        if (table.active_mask_by_id.size() < metadata_need)
            table.active_mask_by_id.resize(metadata_need, 0u);
        if (table.source_mask_by_id.size() < metadata_need)
            table.source_mask_by_id.resize(metadata_need, 0u);
        if (table.dirty_by_id.size() < need)
            table.dirty_by_id.resize(need, 0u);
        if (table.dirty_event_pending_by_id.size() < need)
            table.dirty_event_pending_by_id.resize(need, 0u);
        if (table.loaded_by_id.size() < need)
            table.loaded_by_id.resize(need, 0u);
        if (table.bucket_by_id.size() < need)
            table.bucket_by_id.resize(need, kNoBucket);
        if (table.bucket_index_by_id.size() < need)
            table.bucket_index_by_id.resize(need, kNoIndex);
        const size_t stride = static_cast<size_t>(table.sections_per_chunk);
        if (table.section_id_by_sy.size() < need * stride) {
            table.section_id_by_sy.resize(need * stride, kNoIndex);
        }
    }

    inline int32_t find_chunk_id(const ChunkTable &table, int64_t ck) {
        const auto it = table.coord_to_id.find(ck);
        return it == table.coord_to_id.end() ? kNoNeighbor : it->second;
    }

    inline bool is_loaded_chunk_id(const ChunkTable &table, int32_t id) {
        return chunk_id_in_range(table, id) && table.loaded_by_id[static_cast<size_t>(id)] != 0u;
    }

    inline int32_t find_loaded_chunk_id(const ChunkTable &table, int64_t ck) {
        const int32_t id = find_chunk_id(table, ck);
        if (id < 0)
            return kNoNeighbor;
        return is_loaded_chunk_id(table, id) ? id : kNoNeighbor;
    }

    inline void clear_chunk_slot(ChunkTable &table, int32_t id) {
        if (!chunk_id_in_range(table, id))
            return;
        const auto idx = static_cast<size_t>(id);
        table.ck_by_id[idx] = kInvalidChunkKey;
        table.east_neighbor_id[idx] = kNoNeighbor;
        table.south_neighbor_id[idx] = kNoNeighbor;
        const size_t metadata_base = chunk_word_base(table, id);
        std::fill_n(table.kinds_by_id.data() + metadata_base, table.words_per_chunk, 0u);
        std::fill_n(table.active_mask_by_id.data() + metadata_base, table.words_per_chunk, 0u);
        std::fill_n(table.source_mask_by_id.data() + metadata_base, table.words_per_chunk, 0u);
        table.dirty_by_id[idx] = 0u;
        table.dirty_event_pending_by_id[idx] = 0u;
        table.loaded_by_id[idx] = 0u;
        table.bucket_by_id[idx] = kNoBucket;
        table.bucket_index_by_id[idx] = kNoIndex;
        std::fill_n(section_ids_of(table, id), table.sections_per_chunk, kNoIndex);
    }

    inline int32_t ensure_chunk_record(ChunkTable &table, int64_t ck) {
        if (const int32_t existing = find_chunk_id(table, ck); existing >= 0) {
            ensure_chunk_slot(table, existing);
            return existing;
        }

        int32_t id = kNoNeighbor;
        if (!table.free_ids.empty()) {
            id = table.free_ids.back();
            table.free_ids.pop_back();
        } else {
            id = static_cast<int32_t>(table.ck_by_id.size());
        }

        ensure_chunk_slot(table, id);
        clear_chunk_slot(table, id);
        table.ck_by_id[static_cast<size_t>(id)] = ck;
        table.coord_to_id[ck] = id;
        return id;
    }

    inline void add_to_parity_bucket(ChunkTable &table, int32_t id, uint8_t bucket) {
        if (!chunk_id_in_range(table, id) || bucket >= table.parity_bucket_ids.size())
            return;
        auto &ids = table.parity_bucket_ids[bucket];
        const int32_t index = static_cast<int32_t>(ids.size());
        ids.push_back(id);
        const auto idx = static_cast<size_t>(id);
        table.bucket_by_id[idx] = bucket;
        table.bucket_index_by_id[idx] = index;
    }

    inline void remove_from_parity_bucket(ChunkTable &table, int32_t id) {
        if (!chunk_id_in_range(table, id))
            return;
        const auto idx = static_cast<size_t>(id);
        const uint8_t bucket = table.bucket_by_id[idx];
        const int32_t index = table.bucket_index_by_id[idx];
        if (bucket == kNoBucket || bucket >= table.parity_bucket_ids.size()) {
            table.bucket_by_id[idx] = kNoBucket;
            table.bucket_index_by_id[idx] = kNoIndex;
            return;
        }

        auto &ids = table.parity_bucket_ids[bucket];
        if (index < 0 || static_cast<size_t>(index) >= ids.size()) {
            table.bucket_by_id[idx] = kNoBucket;
            table.bucket_index_by_id[idx] = kNoIndex;
            return;
        }

        const int32_t tail_id = ids.back();
        ids[static_cast<size_t>(index)] = tail_id;
        ids.pop_back();
        if (tail_id != id && chunk_id_in_range(table, tail_id)) {
            table.bucket_index_by_id[static_cast<size_t>(tail_id)] = index;
        }
        table.bucket_by_id[idx] = kNoBucket;
        table.bucket_index_by_id[idx] = kNoIndex;
    }

    inline void relink_neighbors_for_loaded_chunk(ChunkTable &table, int32_t id) {
        if (!is_loaded_chunk_id(table, id))
            return;
        const size_t idx = static_cast<size_t>(id);
        const ChunkCoord coord = decode_chunk_key(table.ck_by_id[idx]);

        const int32_t east_id = find_loaded_chunk_id(table, encode_chunk_key(coord.x + 1, coord.z));
        const int32_t south_id = find_loaded_chunk_id(table, encode_chunk_key(coord.x, coord.z + 1));
        const int32_t west_id = find_loaded_chunk_id(table, encode_chunk_key(coord.x - 1, coord.z));
        const int32_t north_id = find_loaded_chunk_id(table, encode_chunk_key(coord.x, coord.z - 1));

        table.east_neighbor_id[idx] = east_id;
        table.south_neighbor_id[idx] = south_id;

        if (west_id >= 0 && chunk_id_in_range(table, west_id)) {
            table.east_neighbor_id[static_cast<size_t>(west_id)] = id;
        }
        if (north_id >= 0 && chunk_id_in_range(table, north_id)) {
            table.south_neighbor_id[static_cast<size_t>(north_id)] = id;
        }
    }

    inline void unlink_neighbors_for_loaded_chunk(ChunkTable &table, int32_t id) {
        if (!chunk_id_in_range(table, id))
            return;
        const size_t idx = static_cast<size_t>(id);
        const int64_t ck = table.ck_by_id[idx];
        if (ck == kInvalidChunkKey)
            return;
        const ChunkCoord coord = decode_chunk_key(ck);

        const int32_t west_id = find_loaded_chunk_id(table, encode_chunk_key(coord.x - 1, coord.z));
        const int32_t north_id = find_loaded_chunk_id(table, encode_chunk_key(coord.x, coord.z - 1));
        if (west_id >= 0 && chunk_id_in_range(table, west_id)) {
            auto &west_east = table.east_neighbor_id[static_cast<size_t>(west_id)];
            if (west_east == id)
                west_east = kNoNeighbor;
        }
        if (north_id >= 0 && chunk_id_in_range(table, north_id)) {
            auto &north_south = table.south_neighbor_id[static_cast<size_t>(north_id)];
            if (north_south == id)
                north_south = kNoNeighbor;
        }

        table.east_neighbor_id[idx] = kNoNeighbor;
        table.south_neighbor_id[idx] = kNoNeighbor;
    }

    inline void mark_relink_slot(RadWorld &world, int32_t chunk_id, int32_t sy, uint8_t bits) {
        if (bits == 0u)
            return;
        const ChunkTable &chunks = world.chunks;
        if (!chunk_id_in_range(chunks, chunk_id) || !sy_in_range(chunks, sy))
            return;
        const auto slot = static_cast<size_t>(chunk_id) * static_cast<size_t>(chunks.sections_per_chunk) +
                          static_cast<size_t>(sy);
        if (slot >= world.relink_flags.size())
            world.relink_flags.resize(slot + 1u, 0u);
        if (world.relink_flags[slot] == 0u)
            world.relink_slots.push_back(static_cast<int32_t>(slot));
        world.relink_flags[slot] = static_cast<uint8_t>(world.relink_flags[slot] | bits);
    }

    inline void mark_relink_section(RadWorld &world, int32_t chunk_id, int32_t sy) {
        ChunkTable &chunks = world.chunks;
        if (!chunk_id_in_range(chunks, chunk_id) || !sy_in_range(chunks, sy))
            return;
        const int64_t ck = chunks.ck_by_id[static_cast<size_t>(chunk_id)];
        if (ck == kInvalidChunkKey)
            return;
        const ChunkCoord coord = decode_chunk_key(ck);
        const int32_t west = find_loaded_chunk_id(chunks, encode_chunk_key(coord.x - 1, coord.z));
        const int32_t north = find_loaded_chunk_id(chunks, encode_chunk_key(coord.x, coord.z - 1));
        const int32_t east = chunks.east_neighbor_id[static_cast<size_t>(chunk_id)];
        const int32_t south = chunks.south_neighbor_id[static_cast<size_t>(chunk_id)];

        mark_relink_slot(world, chunk_id, sy, kRelinkFaceAll);
        mark_relink_slot(world, west, sy, kRelinkFaceEast);
        mark_relink_slot(world, north, sy, kRelinkFaceSouth);
        if (sy > 0)
            mark_relink_slot(world, chunk_id, sy - 1, kRelinkFaceUp);

        mark_relink_slot(world, chunk_id, sy, kRelinkRepack);
        mark_relink_slot(world, east, sy, kRelinkRepack);
        mark_relink_slot(world, south, sy, kRelinkRepack);
        mark_relink_slot(world, west, sy, kRelinkRepack);
        mark_relink_slot(world, north, sy, kRelinkRepack);
        if (sy > 0)
            mark_relink_slot(world, chunk_id, sy - 1, kRelinkRepack);
        mark_relink_slot(world, chunk_id, sy + 1, kRelinkRepack);
    }

    inline void mark_relink_column(RadWorld &world, int32_t chunk_id) {
        const ChunkTable &chunks = world.chunks;
        for (int32_t sy = 0; sy < chunks.sections_per_chunk; sy++) {
            mark_relink_section(world, chunk_id, sy);
        }
    }

    inline void set_chunk_section_kind(ChunkTable &table, int32_t chunk_id, int32_t sy, uint8_t kind) {
        if (!chunk_id_in_range(table, chunk_id))
            return;
        if (!sy_in_range(table, sy))
            return;
        auto &kinds = table.kinds_by_id[chunk_word_index(table, chunk_id, sy)];
        const uint32_t shift = (static_cast<uint32_t>(sy) & 31u) << 1u;
        kinds = (kinds & ~(0x3ull << shift)) | ((static_cast<uint64_t>(kind) & 0x3ull) << shift);
    }

    inline void set_chunk_section_active(ChunkTable &table, int32_t chunk_id, int32_t sy, bool active) {
        if (!chunk_id_in_range(table, chunk_id))
            return;
        if (!sy_in_range(table, sy))
            return;
        auto &active_mask = table.active_mask_by_id[chunk_word_index(table, chunk_id, sy)];
        const uint32_t bit = 1u << (static_cast<uint32_t>(sy) & 31u);
        const bool already_active = (active_mask & bit) != 0u;
        if (already_active == active)
            return;
        if (active)
            active_mask = active_mask | bit;
        else
            active_mask = active_mask & ~bit;
    }

    inline void mark_chunk_dirty(RadWorld &world, int32_t chunk_id) {
        if (!chunk_id_in_range(world.chunks, chunk_id))
            return;
        const auto idx = static_cast<size_t>(chunk_id);
        if (world.chunks.dirty_by_id[idx] != 0u)
            return;
        world.chunks.dirty_event_pending_by_id[idx] = 1u;
        world.chunks.dirty_by_id[idx] = 1u;
    }

    inline void mark_section_owner_dirty(RadWorld &world, int32_t section_id) {
        if (section_id < 0 || static_cast<size_t>(section_id) >= world.sections.owner_chunk_id.size())
            return;
        const int32_t owner = world.sections.owner_chunk_id[static_cast<size_t>(section_id)];
        mark_chunk_dirty(world, owner);
    }

    inline void mark_section_active_during_sweep(RadWorld &world, int32_t section_id) {
        SectionTable &sections = world.sections;
        ChunkTable &chunks = world.chunks;
        if (section_id < 0 || static_cast<size_t>(section_id) >= sections.active_by_id.size())
            return;
        const auto sidx = static_cast<size_t>(section_id);

        if (sections.active_by_id[sidx] != 0u) {
            return;
        }
        sections.active_by_id[sidx] = 1u;
        const int32_t owner = sections.owner_chunk_id[sidx];
        const int32_t sy = static_cast<int32_t>(sections.sy_by_id[sidx]);
        set_chunk_section_active(chunks, owner, sy, true);
    }

    inline uint64_t hash_mix_u64(uint64_t z) {
        z ^= z >> 33u;
        z *= 0xff51afd7ed558ccdULL;
        z ^= z >> 33u;
        z *= 0xc4ceb9fe1a85ec53ULL;
        z ^= z >> 33u;
        return z;
    }

    inline uint64_t decay_event_draw(int64_t pocket_key_value, uint64_t epoch_salt, uint64_t kind) {
        return hash_mix_u64(static_cast<uint64_t>(pocket_key_value) ^ epoch_salt ^ kind);
    }

    constexpr uint64_t kDecayDrawFog = 0xD1B54A32D192ED03ULL;
    constexpr uint64_t kDecayDrawDestroy = 0x9E3779B97F4A7C15ULL;

    inline bool section_id_in_range(const SectionTable &table, int32_t id) {
        return id >= 0 && static_cast<size_t>(id) < table.key_by_id.size();
    }

    inline void ensure_section_slot(SectionTable &table, int32_t id);
    inline const MultiSectionState &section_multi_state(const SectionTable &table, int32_t id);

    inline FaceEdges multi_face_cached_edges(const SectionTable &table, int32_t section_id, int32_t face) {
        if (!section_id_in_range(table, section_id) || face < 0 || face >= 6)
            return {};
        const auto &multi = section_multi_state(table, section_id);
        const size_t offset = multi.edge_offset[static_cast<size_t>(face)];
        const size_t count = multi.edge_counts[static_cast<size_t>(face)];
        const size_t total = multi.edge_pocket.size();
        if (offset > total || count > (total - offset))
            return {};
        FaceEdges out;
        out.face_distance = multi.edge_face_distance.data() + offset;
        out.inv_volume = multi.edge_inv_volume.data() + offset;
        out.pocket = multi.edge_pocket.data() + offset;
        out.nei_pocket = multi.edge_nei_pocket.data() + offset;
        out.area = multi.edge_area.data() + offset;
        out.exchange_e =
                multi.edge_exchange_e.size() >= offset + count ? multi.edge_exchange_e.data() + offset : nullptr;
        const bool shares_ok = multi.edge_uni_share.size() >= offset + count;
        out.uni_share = shares_ok ? multi.edge_uni_share.data() + offset : nullptr;
        out.pocket_share = shares_ok ? multi.edge_pocket_share.data() + offset : nullptr;
        out.diffusivity =
                multi.edge_diffusivity.size() >= offset + count ? multi.edge_diffusivity.data() + offset : nullptr;
        out.count = count;
        return out;
    }

    inline const SectionMaskWords &empty_section_mask_words() {
        static const SectionMaskWords empty{};
        return empty;
    }

    inline const SectionPocketMap &empty_section_pocket_map() {
        static const SectionPocketMap empty = [] {
            SectionPocketMap pocket_map{};
            pocket_map.fill(kNoPocket);
            return pocket_map;
        }();
        return empty;
    }

    template <typename T> inline void release_vector_memory(std::vector<T> &values) { std::vector<T>().swap(values); }

    inline SectionMaskWords &ensure_section_resistant_mask(SectionTable &table, int32_t id) {
        ensure_section_slot(table, id);
        const auto idx = static_cast<size_t>(id);
        auto &mask = table.resistant_masks[idx];
        if (!mask) {
            mask = std::make_unique<SectionMaskWords>();
            mask->fill(0u);
        }
        return *mask;
    }

    inline const SectionMaskWords &section_resistant_mask(const SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return empty_section_mask_words();
        const auto &mask = table.resistant_masks[static_cast<size_t>(id)];
        return mask ? *mask : empty_section_mask_words();
    }

    inline void release_section_resistant_mask(SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return;
        const auto idx = static_cast<size_t>(id);
        table.resistant_masks[idx].reset();
        table.has_resistant_mask[idx] = 0u;
    }

    inline SectionPocketMap &ensure_section_pocket_map(SectionTable &table, int32_t id) {
        ensure_section_slot(table, id);
        const auto idx = static_cast<size_t>(id);
        auto &pocket_map = table.pocket_by_local[idx];
        if (!pocket_map) {
            pocket_map = std::make_unique<SectionPocketMap>();
            pocket_map->fill(kNoPocket);
        }
        return *pocket_map;
    }

    inline const SectionPocketMap &section_pocket_map(const SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return empty_section_pocket_map();
        const auto &pocket_map = table.pocket_by_local[static_cast<size_t>(id)];
        return pocket_map ? *pocket_map : empty_section_pocket_map();
    }

    inline void release_section_pocket_map(SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return;
        table.pocket_by_local[static_cast<size_t>(id)].reset();
    }

    inline MultiSectionState &ensure_section_multi_state(SectionTable &table, int32_t id) {
        ensure_section_slot(table, id);
        const auto idx = static_cast<size_t>(id);
        auto &state = table.multi_state[idx];
        if (!state)
            state = std::make_unique<MultiSectionState>();
        return *state;
    }

    inline const MultiSectionState *section_multi_state_ptr(const SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return nullptr;
        return table.multi_state[static_cast<size_t>(id)].get();
    }

    inline MultiSectionState *section_multi_state_ptr(SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return nullptr;
        return table.multi_state[static_cast<size_t>(id)].get();
    }

    inline const MultiSectionState &section_multi_state(const SectionTable &table, int32_t id) {
        static const MultiSectionState empty{};
        const auto *state = section_multi_state_ptr(table, id);
        return state != nullptr ? *state : empty;
    }

    inline void release_section_multi_state(SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return;
        table.multi_state[static_cast<size_t>(id)].reset();
    }

    inline const SectionSourceState *section_source_state_ptr(const SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return nullptr;
        return table.source_state[static_cast<size_t>(id)].get();
    }

    inline void set_section_sources(SectionTable &table, int32_t id, std::vector<SectionSourceEntry> entries) {
        ensure_section_slot(table, id);
        auto &slot = table.source_state[static_cast<size_t>(id)];
        if (entries.empty()) {
            slot.reset();
            return;
        }
        if (!slot)
            slot = std::make_unique<SectionSourceState>();
        slot->entries = std::move(entries);
    }

    inline double relax_section_sources(const SectionSourceState *sources, int32_t pocket, double c) noexcept {
        if (sources == nullptr)
            return c;
        EmissionAccumulator acc;
        double add = 0.0;
        for (const SectionSourceEntry &e : sources->entries) {
            if (static_cast<int32_t>(e.pocket) != pocket)
                continue;
            if (e.emission == 0.0)
                continue;
            if (e.saturation == 0.0) {
                add += e.count * e.emission;
                continue;
            }
            if (!(e.emission * (e.saturation - c) > 0.0))
                continue;
            const double magnitude = (e.emission < 0.0) ? -e.emission : e.emission;
            const double scale = (e.saturation < 0.0) ? -e.saturation : e.saturation;
            acc.weight_sum += e.count * (magnitude / scale);
            acc.numerator += e.count * ((e.saturation < 0.0) ? -magnitude : magnitude);
        }
        return acc.relax(c) + add;
    }

    inline PendingSectionState &ensure_section_pending_state(SectionTable &table, int32_t id) {
        ensure_section_slot(table, id);
        const auto idx = static_cast<size_t>(id);
        auto &state = table.pending_state[idx];
        if (!state)
            state = std::make_unique<PendingSectionState>();
        return *state;
    }

    inline const PendingSectionState *section_pending_state_ptr(const SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return nullptr;
        return table.pending_state[static_cast<size_t>(id)].get();
    }

    inline PendingSectionState *section_pending_state_ptr(SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return nullptr;
        return table.pending_state[static_cast<size_t>(id)].get();
    }

    inline void maybe_release_section_pending_state(SectionTable &table, int32_t id) {
        auto *state = section_pending_state_ptr(table, id);
        if (state == nullptr)
            return;
        if (!state->density_bits.empty())
            return;
        if (!state->local_edits.empty())
            return;
        if (!state->pocket_diffusivity.empty())
            return;
        if (state->touched != 0u)
            return;
        table.pending_state[static_cast<size_t>(id)].reset();
    }

    inline void ensure_section_slot(SectionTable &table, int32_t id) {
        const size_t need = static_cast<size_t>(id) + 1u;
        if (table.key_by_id.size() < need) {
            table.buffer_generation++;
            table.key_by_id.resize(need, kInvalidSectionKey);
        }
        if (table.owner_chunk_id.size() < need)
            table.owner_chunk_id.resize(need, kNoNeighbor);
        if (table.sy_by_id.size() < need)
            table.sy_by_id.resize(need, 0u);
        if (table.kind_by_id.size() < need)
            table.kind_by_id.resize(need, kKindNone);
        if (table.active_by_id.size() < need)
            table.active_by_id.resize(need, 0u);
        if (table.uniform_density.size() < need)
            table.uniform_density.resize(need, 0.0);
        if (table.uniform_diffusivity.size() < need)
            table.uniform_diffusivity.resize(need, 1.0f);
        if (table.mask_checksum.size() < need)
            table.mask_checksum.resize(need, 0u);
        if (table.resistant_masks.size() < need)
            table.resistant_masks.resize(need);
        if (table.has_resistant_mask.size() < need)
            table.has_resistant_mask.resize(need, 0u);
        if (table.pocket_count.size() < need)
            table.pocket_count.resize(need, 0u);
        if (table.pocket_by_local.size() < need)
            table.pocket_by_local.resize(need);
        if (table.single_volume.size() < need)
            table.single_volume.resize(need, 1);
        if (table.single_face_open_counts.size() < need)
            table.single_face_open_counts.resize(need);
        if (table.single_face_distance.size() < need)
            table.single_face_distance.resize(need);
        if (table.single_diffusivity.size() < need)
            table.single_diffusivity.resize(need, 1.0f);
        if (table.single_inv_volume.size() < need)
            table.single_inv_volume.resize(need, 1.0);
        if (table.single_conn_counts.size() < need)
            table.single_conn_counts.resize(need);
        if (table.multi_state.size() < need)
            table.multi_state.resize(need);
        if (table.source_state.size() < need)
            table.source_state.resize(need);
        if (table.pending_state.size() < need)
            table.pending_state.resize(need);
        if (table.topology_dirty.size() < need)
            table.topology_dirty.resize(need, 0u);
    }

    inline void clear_section_slot(SectionTable &table, int32_t id) {
        if (!section_id_in_range(table, id))
            return;
        const auto idx = static_cast<size_t>(id);
        table.key_by_id[idx] = kInvalidSectionKey;
        table.owner_chunk_id[idx] = kNoNeighbor;
        table.sy_by_id[idx] = 0u;
        table.kind_by_id[idx] = kKindNone;
        table.active_by_id[idx] = 0u;
        table.uniform_density[idx] = 0.0;
        table.uniform_diffusivity[idx] = 1.0f;
        table.mask_checksum[idx] = 0u;
        release_section_resistant_mask(table, id);
        table.pocket_count[idx] = 0u;
        release_section_pocket_map(table, id);
        table.single_volume[idx] = 1;
        table.single_face_open_counts[idx].fill(0u);
        table.single_face_distance[idx].fill(0.0);
        table.single_diffusivity[idx] = 1.0f;
        table.single_inv_volume[idx] = 1.0;
        table.single_conn_counts[idx].fill(0u);
        release_section_multi_state(table, id);
        table.source_state[idx].reset();
        if (auto *pending = section_pending_state_ptr(table, id)) {
            pending->pocket_diffusivity.clear();
            pending->touched = 0u;
        }
        table.pending_state[idx].reset();
        table.topology_dirty[idx] = 0u;
    }

    inline int32_t ensure_section_record(RadWorld &world, int64_t section_key, int32_t chunk_id, int32_t sy) {
        SectionTable &sections = world.sections;
        ChunkTable &chunks = world.chunks;

        ensure_chunk_slot(chunks, chunk_id);
        const int32_t section_id = chunk_id * sections_per_chunk(chunks) + sy;
        ensure_section_slot(sections, section_id);
        const auto sec_idx = static_cast<size_t>(section_id);

        if (sections.key_by_id[sec_idx] == section_key) {

            sections.owner_chunk_id[sec_idx] = chunk_id;
            sections.sy_by_id[sec_idx] = static_cast<uint8_t>(sy);
            if (sy_in_range(chunks, sy))
                section_ids_of(chunks, chunk_id)[sy] = section_id;
            return section_id;
        }

        const int64_t stale = sections.key_by_id[sec_idx];
        if (stale != kInvalidSectionKey)
            sections.key_to_id.erase(stale);

        clear_section_slot(sections, section_id);
        sections.key_by_id[sec_idx] = section_key;
        sections.owner_chunk_id[sec_idx] = chunk_id;
        sections.sy_by_id[sec_idx] = static_cast<uint8_t>(sy);
        sections.key_to_id[section_key] = section_id;

        if (sy_in_range(chunks, sy)) {
            section_ids_of(chunks, chunk_id)[sy] = section_id;
        }
        return section_id;
    }

    inline void remove_section_record(RadWorld &world, int32_t section_id) {
        SectionTable &sections = world.sections;
        ChunkTable &chunks = world.chunks;
        if (!section_id_in_range(sections, section_id))
            return;

        const auto sec_idx = static_cast<size_t>(section_id);
        const int64_t key = sections.key_by_id[sec_idx];
        if (key == kInvalidSectionKey)
            return;

        const int32_t owner = sections.owner_chunk_id[sec_idx];
        const int32_t sy = static_cast<int32_t>(sections.sy_by_id[sec_idx]);
        if (chunk_id_in_range(chunks, owner) && sy_in_range(chunks, sy)) {
            auto &section_idx = section_ids_of(chunks, owner)[sy];
            if (section_idx == section_id)
                section_idx = kNoIndex;
            set_chunk_section_kind(chunks, owner, sy, kKindNone);
            set_chunk_section_active(chunks, owner, sy, false);
        }

        sections.key_to_id.erase(key);
        clear_section_slot(sections, section_id);
    }

    inline void remove_sections_for_chunk(RadWorld &world, int32_t chunk_id) {
        ChunkTable &chunks = world.chunks;
        if (!chunk_id_in_range(chunks, chunk_id))
            return;
        const int32_t n = sections_per_chunk(chunks);
        const std::vector<int32_t> snapshot(section_ids_of(chunks, chunk_id), section_ids_of(chunks, chunk_id) + n);
        for (int32_t sy = 0; sy < n; sy++) {
            if (snapshot[static_cast<size_t>(sy)] >= 0) {
                remove_section_record(world, snapshot[static_cast<size_t>(sy)]);
            }
        }
        std::fill_n(section_ids_of(chunks, chunk_id), n, kNoIndex);
    }

    inline constexpr double kSanitizedDensityMax = std::numeric_limits<double>::max() * 0.5;

    inline uint32_t compress_even_bits(uint64_t x) {
        x &= 0x5555555555555555ull;
        x = (x | (x >> 1)) & 0x3333333333333333ull;
        x = (x | (x >> 2)) & 0x0F0F0F0F0F0F0F0Full;
        x = (x | (x >> 4)) & 0x00FF00FF00FF00FFull;
        x = (x | (x >> 8)) & 0x0000FFFF0000FFFFull;
        x = (x | (x >> 16)) & 0x00000000FFFFFFFFull;
        return static_cast<uint32_t>(x);
    }

    inline uint32_t uni_lane_mask(uint64_t kinds) {
        const uint64_t lo = kinds & 0x5555555555555555ull;
        const uint64_t hi = (kinds >> 1) & 0x5555555555555555ull;
        return compress_even_bits(lo & ~hi);
    }

    inline uint32_t lane_span_mask(int32_t n) {
        return (n >= 32) ? 0xFFFFFFFFu : static_cast<uint32_t>((1u << n) - 1u);
    }

    inline double sanitize_density_raw(double value, double min_bound, double eps) noexcept {
        if (std::isfinite(value)) [[likely]] {
            if (value == 0.0) [[unlikely]]
                return 0.0;
            if (value < min_bound) [[unlikely]]
                value = min_bound;
            else if (value > kSanitizedDensityMax) [[unlikely]]
                value = kSanitizedDensityMax;
            if (value > min_bound && value < eps && value > -eps) [[unlikely]]
                return 0.0;
            return value;
        }

        if (std::isnan(value))
            return 0.0;
        return std::signbit(value) ? min_bound : kSanitizedDensityMax;
    }

    inline double sanitize_density(const RadWorld &world, double value) noexcept {
        return sanitize_density_raw(value, world.min_bound, world.eps);
    }

    inline double weighted_average_density(const RadWorld &world, const std::vector<double> &density,
                                           const std::vector<int32_t> &volume) {
        const size_t count = std::min(density.size(), volume.size());
        double total_mass = 0.0;
        int64_t total_open = 0;
        for (size_t i = 0; i < count; i++) {
            const int32_t vol = volume[i];
            if (vol <= 0)
                continue;
            total_mass += density[i] * static_cast<double>(vol);
            total_open += vol;
        }
        return (total_open > 0) ? sanitize_density(world, total_mass / static_cast<double>(total_open)) : 0.0;
    }

    inline double mul_clamp(double a, int32_t b) {
        if (a == 0.0)
            return 0.0;
        if (!std::isfinite(a))
            return std::copysign(std::numeric_limits<double>::max(), a);
        const double limit = std::numeric_limits<double>::max() / static_cast<double>(b);
        if (std::abs(a) >= limit)
            return std::copysign(std::numeric_limits<double>::max(), a);
        return a * static_cast<double>(b);
    }

    inline double add_clamp(double a, double b) {
        const double sum = a + b;
        if (sum == std::numeric_limits<double>::infinity())
            return std::numeric_limits<double>::max();
        if (sum == -std::numeric_limits<double>::infinity())
            return -std::numeric_limits<double>::max();
        return std::isnan(sum) ? 0.0 : sum;
    }

    inline const char *kind_name(uint8_t kind) {
        switch (kind) {
        case kKindNone:
            return "NONE";
        case kKindUni:
            return "UNI";
        case kKindSingle:
            return "SINGLE";
        case kKindMulti:
            return "MULTI";
        default:
            return "UNKNOWN";
        }
    }

    inline double transport_exp(double value) { return std::exp(value); }

    inline float transport_quantize_multi_face_distance_storage(double value) { return static_cast<float>(value); }

    inline double java_strict_add(double a, double b) { return a + b; }

    inline double java_strict_sub(double a, double b) { return a - b; }

    inline double java_strict_mul(double a, double b) { return a * b; }

    inline double java_strict_div(double a, double b) { return a / b; }

    inline double java_strict_fma(double a, double b, double c) { return std::fma(a, b, c); }

    inline float clamp_diffusivity(float value) {
        if (!std::isfinite(value))
            return 0.01f;
        return std::clamp(value, 0.01f, 1.50f);
    }

    inline double edge_diffusivity(double len_a, double diff_a, double len_b, double diff_b) {
        if (!(diff_a > 0.0) || !(diff_b > 0.0))
            return 0.0;
        const double denom = (len_a / diff_a) + (len_b / diff_b);
        const double len_sum = len_a + len_b;
        if (!(denom > 0.0) || !(len_sum > 0.0) || !std::isfinite(denom) || !std::isfinite(len_sum))
            return 0.0;
        return len_sum / denom;
    }

    inline double exchange_decay(double k) {
        if (!(k > 0.0) || !std::isfinite(k))
            return 1.0;
        if (k >= 700.0)
            return 0.0;
        return transport_exp(-k);
    }

    inline bool exchange_uni_exact_pair(double &a, double &b, double uniform_exchange) {
        if (a == b)
            return false;
        const double avg = 0.5 * (a + b);
        const double delta = 0.5 * (a - b) * uniform_exchange;
        a = avg + delta;
        b = avg - delta;
        return true;
    }

    inline bool exchange_uni_exact_pair_diffusive(double &a, double &b, double uniform_exchange, double diffusion_dt,
                                                  double diff_a, double diff_b) {
        const double d_eff = edge_diffusivity(8.0, diff_a, 8.0, diff_b);
        if (!(d_eff > 0.0))
            return false;
        const double k = java_strict_mul(java_strict_div(diffusion_dt, 128.0), d_eff);
        const double e = (d_eff == 1.0) ? uniform_exchange : exchange_decay(k);
        return exchange_uni_exact_pair(a, b, e);
    }

    inline void exchange_exact_pair(double &ra, double &rb, double inv_va, double inv_vb, int32_t area, double dist_sum,
                                    double diffusion_dt) {
        const double denom_inv = inv_va + inv_vb;
        const double e = transport_exp(-((static_cast<double>(area) / dist_sum) * denom_inv * diffusion_dt));
        const double r_star = (ra * inv_vb + rb * inv_va) / denom_inv;
        ra = r_star + (ra - r_star) * e;
        rb = r_star + (rb - r_star) * e;
    }

    void rebuild_dirty_sections(RadWorld &world);

    void rebuild_face_links(RadWorld &world);

    void rebuild_pair_lists_if_needed(RadWorld &world);

    void run_exact_exchange_sweeps(RadWorld &world);

    void post_sweep_decay(RadWorld &world);

    void apply_pending_edits(RadWorld &world);

    int32_t serialize_step_events(RadWorld &world, void *out_events_buffer, size_t out_events_capacity_bytes);

    bool validate_chunk_dump_mapping(const RadWorld &world, int32_t chunk_id, std::string &reason);

    bool validate_chunk_dump_entries(const RadWorld &world, int32_t chunk_id, const std::vector<uint32_t> &sypi_entries,
                                     const std::vector<double> &density_entries, std::string &reason);

    void build_chunk_dump_entries(const RadWorld &world, int32_t chunk_id, std::vector<uint32_t> &out_sypi,
                                  std::vector<double> &out_density);

    void reset_chunk_payload_state(RadWorld &world, int32_t chunk_id);
}
