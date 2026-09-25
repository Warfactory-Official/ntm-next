// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include <chrono>
#include <cstdio>
#include <cstdlib>
#include "radsim/cpu_profile.hpp"
#include "radsim/internal.hpp"
#include "radsim/world_registry.hpp"

using namespace hbm::radsim::detail;

extern "C" void rad_submit_section_sources(uint64_t handle, int32_t section_count, const int64_t *packed_keys,
                                           const int32_t *entry_counts, const uint16_t *pockets,
                                           const int32_t *multiplicities, const double *emissions,
                                           const double *saturations) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;
    if (section_count <= 0 || packed_keys == nullptr || entry_counts == nullptr)
        return;

    size_t cursor = 0u;
    for (int32_t i = 0; i < section_count; i++) {
        const int64_t section_key = packed_keys[static_cast<size_t>(i)];
        const int32_t count = entry_counts[static_cast<size_t>(i)];
        if (count < 0)
            return;

        const int32_t sy = decode_section_y(section_key);

        if (sy < 0 || sy >= world.chunks.sections_per_chunk) {
            cursor += static_cast<size_t>(count);
            continue;
        }
        const int64_t ck = section_to_chunk_key(section_key);
        const int32_t chunk_id = ensure_chunk_record(world.chunks, ck);
        const int32_t section_id = ensure_section_record(world, section_key, chunk_id, sy);

        std::vector<SectionSourceEntry> entries;
        entries.reserve(static_cast<size_t>(count));
        for (int32_t e = 0; e < count; e++) {
            const size_t at = cursor + static_cast<size_t>(e);
            SectionSourceEntry entry;
            entry.pocket = pockets == nullptr ? 0u : pockets[at];
            entry.count = multiplicities == nullptr ? 1 : multiplicities[at];
            entry.emission = emissions == nullptr ? 0.0 : emissions[at];
            entry.saturation = saturations == nullptr ? 0.0 : saturations[at];

            if (entry.count <= 0 || entry.emission == 0.0)
                continue;
            entries.push_back(entry);
        }
        cursor += static_cast<size_t>(count);

        const bool any = !entries.empty();
        set_section_sources(world.sections, section_id, std::move(entries));
        const auto cidx = chunk_word_index(world.chunks, chunk_id, sy);
        const uint32_t bit = 1u << (static_cast<uint32_t>(sy) & 31u);
        if (any)
            world.chunks.source_mask_by_id[cidx] |= bit;
        else
            world.chunks.source_mask_by_id[cidx] &= ~bit;
    }
}

extern "C" void rad_submit_section_diffusivity(uint64_t handle, int32_t section_count, const int64_t *packed_keys,
                                               const int32_t *pocket_counts, const float *values) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;
    if (section_count <= 0 || packed_keys == nullptr || pocket_counts == nullptr)
        return;

    size_t cursor = 0u;
    for (int32_t i = 0; i < section_count; i++) {
        const int64_t section_key = packed_keys[static_cast<size_t>(i)];
        const int32_t count = pocket_counts[static_cast<size_t>(i)];
        if (count < 0)
            return;

        const int32_t sy = decode_section_y(section_key);

        if (sy < 0 || sy >= world.chunks.sections_per_chunk) {
            cursor += static_cast<size_t>(count);
            continue;
        }
        const int64_t ck = section_to_chunk_key(section_key);
        const int32_t chunk_id = ensure_chunk_record(world.chunks, ck);
        const int32_t section_id = ensure_section_record(world, section_key, chunk_id, sy);

        const float *entries = (count > 0 && values != nullptr) ? (values + cursor) : nullptr;
        apply_section_diffusivity(world, section_id, entries != nullptr ? count : 0, entries);
        cursor += static_cast<size_t>(count);
    }
}

extern "C" int32_t rad_build_flags(void) {
    int32_t flags = 0;
    flags |= RAD_BUILD_DIFFUSIVITY_TRANSPORT;
    return flags;
}

extern "C" void rad_configure_runtime(int32_t use_tbb, int32_t threads) {
    hbm::radsim::detail::g_runtime_config_explicit = true;
    apply_runtime_config(use_tbb != 0, threads);
}

extern "C" uint64_t rad_world_create(int32_t dim, int64_t seed, double min_bound, int32_t sections_per_chunk) {
    if (sections_per_chunk <= 0 || sections_per_chunk > kMaxChunkSections)
        return 0u;

    RadWorld world;
    world.dim = dim;
    world.seed = seed;
    world.min_bound = min_bound;
    world.chunks.sections_per_chunk = sections_per_chunk;
    world.chunks.words_per_chunk = (sections_per_chunk + 31) >> 5;

    world.chunks.coord_to_id.reserve(4096u);
    world.chunks.free_ids.reserve(2048u);
    for (auto &ids : world.chunks.parity_bucket_ids)
        ids.reserve(2048u);

    world.sections.key_to_id.reserve(65536u);
    world.sections.touched_section_ids.reserve(8192u);
    world.sections.topology_dirty_section_ids.reserve(8192u);
    world.fog_events.reserve(4096u);
    world.destroy_events.reserve(1024u);
    world.dirty_chunk_events.reserve(4096u);
    for (int i = 0; i < 4; i++) {
        world.x_pair_a_by_bucket[static_cast<size_t>(i)].reserve(2048u);
        world.x_pair_b_by_bucket[static_cast<size_t>(i)].reserve(2048u);
        world.z_pair_a_by_bucket[static_cast<size_t>(i)].reserve(2048u);
        world.z_pair_b_by_bucket[static_cast<size_t>(i)].reserve(2048u);
    }

    return install_world(std::make_unique<RadWorld>(std::move(world)));
}

extern "C" void rad_world_destroy(uint64_t handle) { destroy_world(handle); }

extern "C" void rad_world_set_params(uint64_t handle, double diffusion_dt, double uniform_exchange, double retention_dt,
                                     uint64_t fog_prob_u64, uint64_t destroy_prob_u64, double fog_threshold, double eps,
                                     double max_value) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;
    world.diffusion_dt = diffusion_dt;
    world.uniform_exchange = uniform_exchange;
    world.retention_dt = retention_dt;
    world.fog_prob_u64 = fog_prob_u64;
    world.destroy_prob_u64 = destroy_prob_u64;
    world.fog_threshold = fog_threshold;
    world.eps = eps;
    world.max_value = max_value;
}

extern "C" void rad_world_set_feature_flags(uint64_t handle, int32_t flags) {
    auto *world = retain_world(handle);
    if (!world)
        return;
    world->diffusion_transport_enabled = ((flags & 0x1) != 0) ? 1u : 0u;
}

extern "C" void rad_chunk_loaded(uint64_t handle, int64_t ck) {
    auto *world = retain_world(handle);
    if (!world)
        return;
    mark_chunk_loaded(*world, ck);
}

extern "C" void rad_chunk_unloaded(uint64_t handle, int64_t ck) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;
    const int32_t id = find_chunk_id(world.chunks, ck);
    if (id < 0)
        return;
    mark_chunk_unloaded(world, id);
}

extern "C" void rad_chunk_removed(uint64_t handle, int64_t ck) {
    auto *world = retain_world(handle);
    if (!world)
        return;
    mark_chunk_removed(*world, ck);
}

extern "C" void rad_ack_chunk_dirty(uint64_t handle, int64_t ck) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;
    const int32_t chunk_id = find_chunk_id(world.chunks, ck);
    if (chunk_id < 0)
        return;
    const auto idx = static_cast<size_t>(chunk_id);
    world.chunks.dirty_by_id[idx] = 0u;
    world.chunks.dirty_event_pending_by_id[idx] = 0u;
}

extern "C" void rad_submit_dirty_sections(uint64_t handle, int32_t count, const int64_t *packed_keys,
                                          const uint64_t *masks_words) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;
    if (count <= 0 || packed_keys == nullptr || masks_words == nullptr)
        return;

    world.dirty_submit_count += count;

    for (int32_t i = 0; i < count; i++) {
        const int64_t section_key = packed_keys[i];
        const int32_t sy = decode_section_y(section_key);
        if (!sy_in_range(world.chunks, sy)) {
            world.dirty_decode_failures++;
            continue;
        }

        const int64_t ck = section_to_chunk_key(section_key);
        const int32_t chunk_id = ensure_chunk_record(world.chunks, ck);
        const int32_t section_id = ensure_section_record(world, section_key, chunk_id, sy);
        const auto sec_idx = static_cast<size_t>(section_id);

        const uint64_t *words = masks_words + (static_cast<size_t>(i) * kMaskWordsPerSection);
        auto &dst = ensure_section_resistant_mask(world.sections, section_id);
        std::copy(words, words + kMaskWordsPerSection, dst.begin());
        world.sections.has_resistant_mask[sec_idx] = 1u;
        world.sections.mask_checksum[sec_idx] = checksum_mask_words(words);

        mark_section_topology_dirty(world, section_id);
        mark_chunk_dirty(world, chunk_id);
        world.links_dirty = 1u;
    }
}

extern "C" void rad_submit_edits(uint64_t handle, int32_t count, const void *edits_buffer, size_t edits_buffer_bytes) {
    if (count <= 0 || edits_buffer == nullptr || edits_buffer_bytes == 0u)
        return;

    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;

    const auto *bytes = static_cast<const uint8_t *>(edits_buffer);
    const size_t max_entries_by_size = edits_buffer_bytes / kEditWireBytes;
    const int32_t entry_count = static_cast<int32_t>(std::min(static_cast<size_t>(count), max_entries_by_size));
    if (entry_count <= 0) {
        world.edit_decode_failures++;
        return;
    }
    world.edit_submit_count += entry_count;

    for (int32_t i = 0; i < entry_count; i++) {
        const uint8_t *ptr = bytes + (static_cast<size_t>(i) * kEditWireBytes);
        const int64_t section_key = static_cast<int64_t>(read_u64_le(ptr));
        const double add_value = read_f64_le(ptr + 8);
        const double set_value = read_f64_le(ptr + 16);
        const uint64_t set_seq = read_u64_le(ptr + 24);
        const uint16_t local = static_cast<uint16_t>(read_u16_le(ptr + 32) & 0x0FFFu);
        const uint8_t flags = ptr[34];
        const double saturation = ((flags & kEditFlagHasSaturation) != 0u) ? read_f64_le(ptr + 40) : 0.0;

        const int32_t sy = decode_section_y(section_key);
        if (!sy_in_range(world.chunks, sy)) {
            world.edit_decode_failures++;
            continue;
        }

        const int64_t ck = section_to_chunk_key(section_key);
        const int32_t chunk_id = ensure_chunk_record(world.chunks, ck);
        const int32_t section_id = ensure_section_record(world, section_key, chunk_id, sy);

        PendingLocalEdit entry;
        entry.local = local;
        entry.flags = flags;
        entry.add_value = add_value;
        entry.set_value = set_value;
        entry.set_seq = set_seq;
        entry.saturation = saturation;

        queue_section_edit(world, section_id, entry);
    }
}

extern "C" void rad_load_pending_entries(uint64_t handle, int64_t ck, int32_t entry_count, const uint32_t *sypi_entries,
                                         const double *density_entries) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return;
    RadWorld &world = *world_ptr;

    const int32_t chunk_id = ensure_chunk_record(world.chunks, ck);
    reset_chunk_payload_state(world, chunk_id);

    if (entry_count <= 0)
        return;
    if (sypi_entries == nullptr || density_entries == nullptr)
        return;

    const ChunkCoord coord = decode_chunk_key(ck);
    for (int32_t i = 0; i < entry_count; i++) {
        const uint32_t sypi = sypi_entries[static_cast<size_t>(i)];
        const int32_t sy = static_cast<int32_t>(sypi >> 11u);
        if (!sy_in_range(world.chunks, sy)) {
            world.load_decode_failures++;
            continue;
        }
        const uint16_t pocket = static_cast<uint16_t>(sypi & 0x07FFu);
        if (pocket >= 2048u)
            continue;

        const double value = sanitize_density(world, density_entries[static_cast<size_t>(i)]);
        if (value == 0.0)
            continue;

        const int64_t section_key = encode_section_key(coord.x, sy, coord.z);
        const int32_t section_id = ensure_section_record(world, section_key, chunk_id, sy);
        const auto sec_idx = static_cast<size_t>(section_id);
        uint64_t bits = 0u;
        std::memcpy(&bits, &value, sizeof(bits));
        ensure_section_pending_state(world.sections, section_id).density_bits[pocket] = bits;
        world.sections.kind_by_id[sec_idx] = kKindNone;
        world.sections.uniform_density[sec_idx] = 0.0;
        world.sections.active_by_id[sec_idx] = 0u;
        set_chunk_section_kind(world.chunks, chunk_id, sy, kKindNone);
        set_chunk_section_active(world.chunks, chunk_id, sy, false);
    }
}

extern "C" int32_t rad_step(uint64_t handle, uint64_t work_epoch_salt, int32_t work_epoch, int32_t perm_bits,
                            void *out_events_buffer, size_t out_events_capacity_bytes) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return -1;
    RadWorld &world = *world_ptr;
    using profile_clock = std::chrono::steady_clock;
    const auto total_start = profile_clock::now();

    const StepWidthScope step_width{world};

    world.step_count++;
    world.last_work_epoch_salt = work_epoch_salt;
    world.last_work_epoch = work_epoch;
    world.last_perm_bits = perm_bits;
    world.last_step_pending_sections = static_cast<uint32_t>(
            std::min<size_t>(world.sections.topology_dirty_section_ids.size(), std::numeric_limits<uint32_t>::max()));
    size_t pending_edit_total = 0u;
    for (const int32_t section_id : world.sections.touched_section_ids) {
        if (!section_id_in_range(world.sections, section_id))
            continue;
        const auto *pending = section_pending_state_ptr(world.sections, section_id);
        if (pending != nullptr)
            pending_edit_total += pending->local_edits.size();
    }
    world.last_step_pending_edits = (pending_edit_total > std::numeric_limits<uint32_t>::max())
                                            ? std::numeric_limits<uint32_t>::max()
                                            : static_cast<uint32_t>(pending_edit_total);
    world.last_step_links_dirty_before = world.links_dirty;
    world.last_step_links_rebuilt = 0u;
    world.last_step_pair_lists_dirty_before = world.pair_lists_dirty;
    world.last_step_pair_lists_rebuilt = 0u;
    world.last_step_profile_nanos.fill(0u);

    auto phase_start = profile_clock::now();
    rebuild_dirty_sections(world);
    auto phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileDirtyRebuild] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());
    phase_start = phase_end;
    apply_pending_edits(world);
    phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileApplyEdits] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());

    phase_start = phase_end;
    if (world.links_dirty != 0u) {
        rebuild_face_links(world);
        world.last_step_links_rebuilt = 1u;
    }
    phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileRelink] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());

    phase_start = phase_end;
    post_sweep_decay(world);
    phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileDecay] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());

    phase_start = phase_end;
    run_exact_exchange_sweeps(world);
    phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileTransport] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());

    phase_start = phase_end;
    phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileValidate] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());

    phase_start = phase_end;
    const int32_t result = serialize_step_events(world, out_events_buffer, out_events_capacity_bytes);
    phase_end = profile_clock::now();
    world.last_step_profile_nanos[kStepProfileSerialize] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - phase_start).count());
    world.last_step_profile_nanos[kStepProfileTotal] = static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(phase_end - total_start).count());
    return result;
}

extern "C" int32_t rad_dump_chunk_entries(uint64_t handle, int64_t ck, void *out_sypi_buffer,
                                          size_t out_sypi_capacity_bytes, void *out_density_buffer,
                                          size_t out_density_capacity_bytes) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return -1;
    RadWorld &world = *world_ptr;

    const int32_t chunk_id = find_chunk_id(world.chunks, ck);
    if (chunk_id < 0)
        return 0;

    std::vector<uint32_t> sypi_entries;
    std::vector<double> density_entries;
    build_chunk_dump_entries(world, chunk_id, sypi_entries, density_entries);
    world.last_dump_snapshot_ck = ck;
    world.last_dump_snapshot_sypi = sypi_entries;
    world.last_dump_snapshot_density = density_entries;

    std::string dump_error;
    if (!validate_chunk_dump_mapping(world, chunk_id, dump_error) ||
        !validate_chunk_dump_entries(world, chunk_id, sypi_entries, density_entries, dump_error)) {
        world.dump_validation_failures++;
        world.last_dump_invalid_ck = ck;
        world.last_dump_validation_error = dump_error;
    }

    const int32_t entry_count = static_cast<int32_t>(sypi_entries.size());
    if (entry_count <= 0)
        return 0;

    const size_t needed_sypi_bytes = static_cast<size_t>(entry_count) * sizeof(uint32_t);
    const size_t needed_density_bytes = static_cast<size_t>(entry_count) * sizeof(double);
    if (out_sypi_buffer == nullptr || out_density_buffer == nullptr || out_sypi_capacity_bytes < needed_sypi_bytes ||
        out_density_capacity_bytes < needed_density_bytes) {
        return (needed_density_bytes > static_cast<size_t>(std::numeric_limits<int32_t>::max()))
                       ? std::numeric_limits<int32_t>::min() + 1
                       : -static_cast<int32_t>(needed_density_bytes);
    }

    auto *sypi = static_cast<uint8_t *>(out_sypi_buffer);
    auto *density = static_cast<uint8_t *>(out_density_buffer);
    for (int32_t i = 0; i < entry_count; i++) {
        write_u32_le(sypi, sypi_entries[static_cast<size_t>(i)]);
        write_f64_le(density, density_entries[static_cast<size_t>(i)]);
        sypi += sizeof(uint32_t);
        density += sizeof(double);
    }

    return entry_count;
}

namespace {

    struct BufferView {
        const void *ptr;
        int64_t count;
        int32_t stride;
    };

    BufferView buffer_view(RadWorld &world, int32_t which) {
        switch (which) {
        case RAD_BUF_SECTION_UNIFORM_DENSITY:
            return {world.sections.uniform_density.data(), static_cast<int64_t>(world.sections.uniform_density.size()),
                    1};
        case RAD_BUF_SECTION_KIND:
            return {world.sections.kind_by_id.data(), static_cast<int64_t>(world.sections.kind_by_id.size()), 1};
        case RAD_BUF_SECTION_ACTIVE:
            return {world.sections.active_by_id.data(), static_cast<int64_t>(world.sections.active_by_id.size()), 1};
        case RAD_BUF_CHUNK_SECTION_ID_BY_SY:

            return {world.chunks.section_id_by_sy.data(), static_cast<int64_t>(world.chunks.section_id_by_sy.size()),
                    world.chunks.sections_per_chunk};
        case RAD_BUF_CHUNK_KINDS:
            return {world.chunks.kinds_by_id.data(), static_cast<int64_t>(world.chunks.kinds_by_id.size()),
                    world.chunks.words_per_chunk};
        case RAD_BUF_CHUNK_ACTIVE_MASK:
            return {world.chunks.active_mask_by_id.data(), static_cast<int64_t>(world.chunks.active_mask_by_id.size()),
                    world.chunks.words_per_chunk};
        default:
            return {nullptr, 0, 0};
        }
    }
}

extern "C" int32_t rad_chunk_id(uint64_t handle, int64_t ck) {
    auto *world = retain_world(handle);
    if (!world)
        return -1;
    const int32_t id = find_chunk_id(world->chunks, ck);
    return chunk_id_in_range(world->chunks, id) ? id : -1;
}

extern "C" uint64_t rad_buffer_ptr(uint64_t handle, int32_t which) {
    auto *world = retain_world(handle);
    if (!world)
        return 0u;
    return reinterpret_cast<uint64_t>(buffer_view(*world, which).ptr);
}

extern "C" int64_t rad_buffer_count(uint64_t handle, int32_t which) {
    auto *world = retain_world(handle);
    if (!world)
        return 0;
    return buffer_view(*world, which).count;
}

extern "C" int32_t rad_buffer_stride(uint64_t handle, int32_t which) {
    auto *world = retain_world(handle);
    if (!world)
        return 0;
    return buffer_view(*world, which).stride;
}

extern "C" int32_t rad_buffer_generation(uint64_t handle) {
    auto *world = retain_world(handle);
    if (!world)
        return -1;

    return world->chunks.buffer_generation + world->sections.buffer_generation;
}

extern "C" int32_t rad_sections_per_chunk(void) { return kMaxChunkSections; }

extern "C" int32_t rad_world_sections_per_chunk(uint64_t handle) {
    auto *world = retain_world(handle);
    return world ? world->chunks.sections_per_chunk : 0;
}

extern "C" double rad_query_local_density(uint64_t handle, int64_t section_key, int32_t local) {
    auto *world_ptr = retain_world(handle);
    if (!world_ptr)
        return 0.0;
    RadWorld &world = *world_ptr;

    if (local < 0 || local >= kSectionVoxelCount)
        return 0.0;

    const auto it = world.sections.key_to_id.find(section_key);
    if (it == world.sections.key_to_id.end())
        return 0.0;
    const int32_t section_id = it->second;
    if (!section_id_in_range(world.sections, section_id))
        return 0.0;

    const auto sidx = static_cast<size_t>(section_id);
    const uint8_t kind = world.sections.kind_by_id[sidx];
    if (kind == kKindNone)
        return 0.0;
    if (kind == kKindUni)
        return world.sections.uniform_density[sidx];

    const uint16_t pocket = section_pocket_map(world.sections, section_id)[static_cast<size_t>(local)];
    if (pocket == kNoPocket)
        return 0.0;
    if (kind == kKindSingle)
        return world.sections.uniform_density[sidx];
    const auto pidx = static_cast<size_t>(pocket);
    const auto &density = section_multi_state(world.sections, section_id).pocket_density;
    return pidx < density.size() ? density[pidx] : 0.0;
}
