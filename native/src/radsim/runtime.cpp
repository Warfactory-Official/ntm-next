// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/internal.hpp"

namespace hbm::radsim::detail {
    void mark_chunk_loaded(RadWorld &world, int64_t ck) {
        ChunkTable &table = world.chunks;
        const int32_t id = ensure_chunk_record(table, ck);
        const auto idx = static_cast<size_t>(id);
        if (!is_loaded_chunk_id(table, id)) {
            table.loaded_by_id[idx] = 1u;
            add_to_parity_bucket(table, id, parity_bucket_for_chunk(ck));
            world.chunk_load_count++;
        }
        relink_neighbors_for_loaded_chunk(table, id);
        mark_relink_column(world, id);
        world.links_dirty = 1u;
        world.pair_lists_dirty = 1u;
    }

    void mark_chunk_unloaded(RadWorld &world, int32_t id) {
        ChunkTable &table = world.chunks;
        if (!is_loaded_chunk_id(table, id))
            return;

        relink_note_chunk_unloading(world, id);
        unlink_neighbors_for_loaded_chunk(table, id);
        remove_from_parity_bucket(table, id);
        const auto idx = static_cast<size_t>(id);
        table.loaded_by_id[idx] = 0u;
        table.east_neighbor_id[idx] = kNoNeighbor;
        table.south_neighbor_id[idx] = kNoNeighbor;

        for (int32_t sy = 0; sy < sections_per_chunk(table); sy++) {
            const int32_t section_id = section_id_for_chunk_sy(table, id, sy);
            if (section_id_in_range(world.sections, section_id)) {
                world.sections.active_by_id[static_cast<size_t>(section_id)] = 0u;
            }
            set_chunk_section_active(table, id, sy, false);
        }

        world.chunk_unload_count++;
        world.links_dirty = 1u;
        world.pair_lists_dirty = 1u;
    }

    void mark_chunk_removed(RadWorld &world, int64_t ck) {
        ChunkTable &table = world.chunks;
        const int32_t id = find_chunk_id(table, ck);
        if (id < 0)
            return;
        if (is_loaded_chunk_id(table, id)) {
            mark_chunk_unloaded(world, id);
        }
        remove_sections_for_chunk(world, id);
        table.coord_to_id.erase(ck);
        clear_chunk_slot(table, id);
        table.free_ids.push_back(id);
        world.chunk_remove_count++;

        world.links_full_dirty = 1u;
        world.links_dirty = 1u;
        world.pair_lists_dirty = 1u;
    }

    bool validate_chunk_dump_mapping(const RadWorld &world, int32_t chunk_id, std::string &reason) {
        const ChunkTable &chunks = world.chunks;
        const SectionTable &sections = world.sections;
        if (!chunk_id_in_range(chunks, chunk_id)) {
            reason = "invalid chunk id";
            return false;
        }

        const auto cidx = static_cast<size_t>(chunk_id);
        const int64_t ck = chunks.ck_by_id[cidx];
        std::vector<int32_t> seen_section_ids(static_cast<size_t>(sections_per_chunk(chunks)), kNoIndex);

        for (int32_t sy = 0; sy < sections_per_chunk(chunks); sy++) {
            const size_t metadata_index = chunk_word_index(chunks, chunk_id, sy);
            const uint32_t shift = (static_cast<uint32_t>(sy) & 31u) << 1u;
            const uint8_t slot_kind = static_cast<uint8_t>((chunks.kinds_by_id[metadata_index] >> shift) & 0x3u);
            const bool slot_active =
                    (chunks.active_mask_by_id[metadata_index] & (1u << (static_cast<uint32_t>(sy) & 31u))) != 0u;
            const int32_t section_id = section_id_for_chunk_sy(chunks, chunk_id, sy);
            if (section_id < 0) {
                if (slot_kind != kKindNone || slot_active) {
                    reason = "missing section slot with non-empty chunk bits at sy=" + std::to_string(sy);
                    return false;
                }
                continue;
            }
            if (!section_id_in_range(sections, section_id)) {
                reason = "section slot out of range at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id);
                return false;
            }
            for (int32_t other_sy = 0; other_sy < sy; other_sy++) {
                if (seen_section_ids[static_cast<size_t>(other_sy)] == section_id) {
                    reason = "duplicate section id reused across sy=" + std::to_string(other_sy) +
                             " and sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id);
                    return false;
                }
            }
            seen_section_ids[static_cast<size_t>(sy)] = section_id;

            const auto sidx = static_cast<size_t>(section_id);
            if (sections.key_by_id[sidx] == kInvalidSectionKey) {
                reason = "invalid section key at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id);
                return false;
            }
            if (sections.owner_chunk_id[sidx] != chunk_id) {
                reason = "owner chunk mismatch at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id);
                return false;
            }
            if (static_cast<int32_t>(sections.sy_by_id[sidx]) != sy) {
                reason = "section sy mismatch at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id) +
                         " actualSy=" + std::to_string(static_cast<int32_t>(sections.sy_by_id[sidx]));
                return false;
            }
            if (section_to_chunk_key(sections.key_by_id[sidx]) != ck) {
                reason =
                        "section chunk key mismatch at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id);
                return false;
            }
            if (sections.kind_by_id[sidx] != slot_kind) {
                reason = "section kind mismatch at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id) +
                         " slotKind=" + std::string(kind_name(slot_kind)) +
                         " actualKind=" + std::string(kind_name(sections.kind_by_id[sidx]));
                return false;
            }
            if ((sections.active_by_id[sidx] != 0u) != slot_active) {
                reason = "section active mismatch at sy=" + std::to_string(sy) + " sid=" + std::to_string(section_id);
                return false;
            }
        }

        return true;
    }

    bool validate_chunk_dump_entries(const RadWorld &world, int32_t chunk_id, const std::vector<uint32_t> &sypi_entries,
                                     const std::vector<double> &density_entries, std::string &reason) {
        const ChunkTable &chunks = world.chunks;
        const SectionTable &sections = world.sections;
        if (sypi_entries.size() != density_entries.size()) {
            reason = "sypi/density entry count mismatch";
            return false;
        }
        if (!chunk_id_in_range(chunks, chunk_id)) {
            reason = "invalid chunk id for dump entry validation";
            return false;
        }

        std::unordered_map<uint32_t, size_t> first_index_by_sypi;
        first_index_by_sypi.reserve(sypi_entries.size());

        for (size_t i = 0; i < sypi_entries.size(); i++) {
            const uint32_t sypi = sypi_entries[i];
            const int32_t sy = static_cast<int32_t>(sypi >> 11u);
            const uint16_t pocket = static_cast<uint16_t>(sypi & 0x07FFu);
            const double value = density_entries[i];
            if (!sy_in_range(chunks, sy)) {
                reason = "dump emitted section outside dimension sy=" + std::to_string(sy);
                return false;
            }

            const auto seen = first_index_by_sypi.find(sypi);
            if (seen != first_index_by_sypi.end()) {
                reason = "duplicate sypi in dump sy=" + std::to_string(sy) +
                         " pi=" + std::to_string(static_cast<int32_t>(pocket)) +
                         " firstIndex=" + std::to_string(seen->second) + " dupIndex=" + std::to_string(i);
                return false;
            }
            first_index_by_sypi.emplace(sypi, i);

            if (!std::isfinite(value) || value == 0.0) {
                reason = "non-finite or zero density emitted for sy=" + std::to_string(sy) +
                         " pi=" + std::to_string(static_cast<int32_t>(pocket));
                return false;
            }

            const size_t metadata_index = chunk_word_index(chunks, chunk_id, sy);
            const uint32_t shift = (static_cast<uint32_t>(sy) & 31u) << 1u;
            const uint8_t slot_kind = static_cast<uint8_t>((chunks.kinds_by_id[metadata_index] >> shift) & 0x3u);
            const int32_t section_id = section_id_for_chunk_sy(chunks, chunk_id, sy);
            if (section_id < 0 || !section_id_in_range(sections, section_id)) {
                reason = "dump emitted entry for missing section sy=" + std::to_string(sy);
                return false;
            }

            if ((slot_kind == kKindUni || slot_kind == kKindSingle) && pocket != 0u) {
                reason = "non-zero pocket emitted for " + std::string(kind_name(slot_kind)) +
                         " section sy=" + std::to_string(sy) + " pi=" + std::to_string(static_cast<int32_t>(pocket));
                return false;
            }
            if (slot_kind == kKindMulti) {
                const uint16_t pocket_count = sections.pocket_count[static_cast<size_t>(section_id)];
                if (pocket >= pocket_count) {
                    reason = "multi section emitted out-of-range pocket sy=" + std::to_string(sy) +
                             " pi=" + std::to_string(static_cast<int32_t>(pocket)) +
                             " pocketCount=" + std::to_string(static_cast<int32_t>(pocket_count));
                    return false;
                }
            }
        }

        return true;
    }

    void collect_pending_dirty_chunk_events(RadWorld &world) {
        world.dirty_chunk_events.clear();
        ChunkTable &chunks = world.chunks;
        for (size_t idx = 0; idx < chunks.ck_by_id.size(); idx++) {
            if (chunks.dirty_event_pending_by_id[idx] == 0u)
                continue;
            if (chunks.ck_by_id[idx] == kInvalidChunkKey)
                continue;
            world.dirty_chunk_events.push_back(chunks.ck_by_id[idx]);
        }
    }

    void clear_pending_dirty_chunk_events(RadWorld &world) {
        ChunkTable &chunks = world.chunks;
        for (size_t idx = 0; idx < chunks.ck_by_id.size(); idx++) {
            chunks.dirty_event_pending_by_id[idx] = 0u;
        }
    }

    bool is_section_topology_dirty(const SectionTable &table, const int32_t section_id) {
        return section_id_in_range(table, section_id) && table.topology_dirty[static_cast<size_t>(section_id)] != 0u;
    }

    void mark_section_topology_dirty(RadWorld &world, const int32_t section_id) {
        SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto idx = static_cast<size_t>(section_id);
        if (sections.topology_dirty[idx] != 0u)
            return;
        sections.topology_dirty[idx] = 1u;
        sections.topology_dirty_section_ids.push_back(section_id);
    }

    int32_t serialize_step_events(RadWorld &world, void *out_events_buffer, size_t out_events_capacity_bytes) {
        collect_pending_dirty_chunk_events(world);
        const size_t fog_count = world.fog_events.size();
        const size_t destroy_count = world.destroy_events.size();
        const size_t dirty_count = world.dirty_chunk_events.size();
        const size_t total_count = fog_count + destroy_count + dirty_count;
        const size_t needed_bytes = kEventHeaderBytes + (total_count * sizeof(int64_t));

        if (out_events_buffer == nullptr || out_events_capacity_bytes < needed_bytes) {
            return (needed_bytes > static_cast<size_t>(std::numeric_limits<int32_t>::max()))
                           ? std::numeric_limits<int32_t>::min() + 1
                           : -static_cast<int32_t>(needed_bytes);
        }

        auto *out = static_cast<uint8_t *>(out_events_buffer);
        write_u32_le(out, static_cast<uint32_t>(fog_count));
        write_u32_le(out + 4u, static_cast<uint32_t>(destroy_count));
        write_u32_le(out + 8u, static_cast<uint32_t>(dirty_count));
        write_u32_le(out + 12u, 0u);
        out += kEventHeaderBytes;

        for (const int64_t key : world.fog_events) {
            write_u64_le(out, static_cast<uint64_t>(key));
            out += sizeof(int64_t);
        }
        for (const int64_t key : world.destroy_events) {
            write_u64_le(out, static_cast<uint64_t>(key));
            out += sizeof(int64_t);
        }
        for (const int64_t key : world.dirty_chunk_events) {
            write_u64_le(out, static_cast<uint64_t>(key));
            out += sizeof(int64_t);
        }

        clear_pending_dirty_chunk_events(world);

        return static_cast<int32_t>(needed_bytes);
    }

    int32_t resolve_pending_edits_for_section(const RadWorld &world, size_t sidx, std::vector<double> &out_density) {
        const SectionTable &sections = world.sections;
        const auto *pending = section_pending_state_ptr(sections, static_cast<int32_t>(sidx));
        if (pending == nullptr)
            return 0;
        const auto &edits = pending->local_edits;
        if (edits.empty())
            return 0;

        const uint8_t kind = sections.kind_by_id[sidx];
        if (kind == kKindNone)
            return 0;

        if (kind == kKindMulti) {
            const int32_t pocket_count = sections.pocket_count[sidx];
            if (pocket_count <= 0)
                return 0;
            out_density.resize(static_cast<size_t>(pocket_count));
            const auto &density = section_multi_state(sections, static_cast<int32_t>(sidx)).pocket_density;
            for (int32_t p = 0; p < pocket_count; p++) {
                out_density[p] = (p < static_cast<int32_t>(density.size())) ? density[p] : 0.0;
            }

            PendingEditScratch scratch;
            resolve_pending_edits(edits, &section_pocket_map(sections, static_cast<int32_t>(sidx)),
                                  static_cast<size_t>(pocket_count), out_density.data(), scratch);
            for (int32_t p = 0; p < pocket_count; p++) {
                const size_t pi = static_cast<size_t>(p);
                out_density[pi] = sanitize_density(world, out_density[pi]);
            }
            return pocket_count;
        } else {
            out_density.resize(1);
            out_density[0] = sections.uniform_density[sidx];
            const SectionPocketMap *pocket_of =
                    (kind == kKindSingle) ? &section_pocket_map(sections, static_cast<int32_t>(sidx)) : nullptr;
            PendingEditScratch scratch;
            resolve_pending_edits(edits, pocket_of, 1u, out_density.data(), scratch);
            out_density[0] = sanitize_density(world, out_density[0]);
            return 1;
        }
    }

    bool mask_words_all_zero(const std::array<uint64_t, kMaskWordsPerSection> &words) {
        for (const uint64_t word : words) {
            if (word != 0u)
                return false;
        }
        return true;
    }

    void compute_snapshot_mapping_from_mask(const std::array<uint64_t, kMaskWordsPerSection> &words, uint8_t &out_kind,
                                            int32_t &out_pocket_count,
                                            std::array<uint16_t, kSectionVoxelCount> &out_pocket_map,
                                            std::vector<int32_t> &out_volume, std::vector<int64_t> &out_sum_xyz) {
        out_pocket_map.fill(kNoPocket);
        out_volume.clear();
        out_sum_xyz.clear();
        out_kind = kKindNone;
        out_pocket_count = 0;

        if (mask_words_all_zero(words)) {
            out_kind = kKindUni;
            out_pocket_count = 1;
            out_volume.assign(1, kSectionVoxelCount);
            return;
        }

        out_volume.reserve(256);
        out_sum_xyz.reserve(256 * 3u);
        out_pocket_count = flood_fill_pockets(words, out_pocket_map, out_volume, out_sum_xyz);
        if (out_pocket_count <= 0) {
            out_kind = kKindNone;
            out_pocket_count = 0;
            out_volume.clear();
            out_sum_xyz.clear();
            return;
        }

        if (out_pocket_count == 1) {
            const bool fully_open = (!out_volume.empty() && out_volume.front() == kSectionVoxelCount);
            out_kind = fully_open ? kKindUni : kKindSingle;
        } else {
            out_kind = kKindMulti;
        }
    }

    void overlay_pending_density_bits_snapshot(const RadWorld &world, int32_t section_id, uint8_t kind,
                                               int32_t pocket_count, std::vector<double> &density) {
        const SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto *pending_state = section_pending_state_ptr(sections, section_id);
        if (pending_state == nullptr || pending_state->density_bits.empty())
            return;
        const auto &pending = pending_state->density_bits;

        if (kind == kKindNone || density.empty())
            return;

        if (kind == kKindUni || kind == kKindSingle) {
            const auto it = pending.find(0u);
            if (it != pending.end()) {
                double value = 0.0;
                std::memcpy(&value, &it->second, sizeof(value));
                density[0] = sanitize_density(world, value);
            }
            return;
        }

        for (int32_t p = 0; p < pocket_count; p++) {
            const auto it = pending.find(static_cast<uint16_t>(p));
            if (it == pending.end())
                continue;
            double value = 0.0;
            std::memcpy(&value, &it->second, sizeof(value));
            density[static_cast<size_t>(p)] = sanitize_density(world, value);
        }
    }

    void apply_pending_edits_snapshot(const RadWorld &world, int32_t section_id, uint8_t kind,
                                      const std::array<uint16_t, kSectionVoxelCount> &pocket_map,
                                      std::vector<double> &density) {
        const SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto *pending_state = section_pending_state_ptr(sections, section_id);
        if (pending_state == nullptr || pending_state->local_edits.empty())
            return;
        if (kind == kKindNone || density.empty())
            return;

        PendingEditScratch scratch;
        resolve_pending_edits(pending_state->local_edits, (kind == kKindUni) ? nullptr : &pocket_map, density.size(),
                              density.data(), scratch);
        for (double &value : density)
            value = sanitize_density(world, value);
    }

    void compute_snapshot_density_after_rebuild(const RadWorld &world, int32_t section_id, uint8_t rebuilt_kind,
                                                int32_t rebuilt_pocket_count,
                                                const std::array<uint16_t, kSectionVoxelCount> &rebuilt_pocket_map,
                                                const std::vector<int32_t> &rebuilt_volume,
                                                std::vector<double> &rebuilt_density) {
        const SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto sidx = static_cast<size_t>(section_id);

        rebuilt_density.clear();
        if (rebuilt_kind == kKindNone || rebuilt_pocket_count <= 0)
            return;

        rebuilt_density.assign(static_cast<size_t>(std::max(1, rebuilt_pocket_count)), 0.0);

        const uint8_t old_kind = sections.kind_by_id[sidx];
        const double old_uniform = sections.uniform_density[sidx];
        const uint16_t old_pocket_count = sections.pocket_count[sidx];
        const auto &old_map = section_pocket_map(sections, section_id);
        const auto &old_multi_state = section_multi_state(sections, section_id);
        const auto &old_volume = old_multi_state.pocket_volume;
        const auto &old_multi = old_multi_state.pocket_density;
        const int32_t old_single_volume = std::max(1, sections.single_volume[sidx]);

        std::vector<double> out_new_mass(static_cast<size_t>(std::max(1, rebuilt_pocket_count)), 0.0);

        if (old_kind != kKindNone) {
            if (rebuilt_kind == kKindUni) {
                double total_mass = 0.0;
                if (old_kind == kKindUni) {
                    if (std::abs(old_uniform) > world.eps) {
                        total_mass = mul_clamp(old_uniform, kSectionVoxelCount);
                    }
                } else if (old_pocket_count > 0) {
                    if (old_kind == kKindSingle) {
                        if (std::abs(old_uniform) > world.eps) {
                            total_mass = mul_clamp(old_uniform, old_single_volume);
                        }
                    } else if (old_kind == kKindMulti) {
                        const int32_t old_cnt = old_pocket_count;
                        for (int32_t p = 0; p < old_cnt; p++) {
                            if (p >= static_cast<int32_t>(old_multi.size()) ||
                                p >= static_cast<int32_t>(old_volume.size()))
                                continue;
                            const double dp = old_multi[static_cast<size_t>(p)];
                            if (std::abs(dp) > world.eps) {
                                total_mass = add_clamp(total_mass,
                                                       mul_clamp(dp, std::max(1, old_volume[static_cast<size_t>(p)])));
                            }
                        }
                    }
                }
                out_new_mass[0] = total_mass;
            } else if (old_kind == kKindUni) {
                if (std::abs(old_uniform) > world.eps) {
                    const double old_mass = mul_clamp(old_uniform, kSectionVoxelCount);
                    int64_t total_new_air = 0;
                    for (int32_t p = 0; p < rebuilt_pocket_count; p++) {
                        total_new_air += std::max(1, rebuilt_volume[static_cast<size_t>(p)]);
                    }
                    if (total_new_air > 0) {
                        const double mass_per_block = old_mass / static_cast<double>(total_new_air);
                        for (int32_t p = 0; p < rebuilt_pocket_count; p++) {
                            out_new_mass[static_cast<size_t>(p)] =
                                    mul_clamp(mass_per_block, std::max(1, rebuilt_volume[static_cast<size_t>(p)]));
                        }
                    }
                }
            } else if (old_pocket_count > 0) {
                const int32_t old_cnt = old_pocket_count;
                std::vector<int32_t> old_totals(static_cast<size_t>(old_cnt), 0);
                std::vector<uint32_t> overlaps;
                overlaps.reserve(kSectionVoxelCount);

                for (int32_t local = 0; local < kSectionVoxelCount; local++) {
                    const uint16_t n_idx = rebuilt_pocket_map[static_cast<size_t>(local)];
                    if (n_idx == kNoPocket || n_idx >= static_cast<uint16_t>(rebuilt_pocket_count))
                        continue;

                    const uint16_t o_idx = old_map[static_cast<size_t>(local)];
                    if (o_idx == kNoPocket || o_idx >= static_cast<uint16_t>(old_cnt))
                        continue;

                    overlaps.push_back((static_cast<uint32_t>(o_idx) << 16u) | static_cast<uint32_t>(n_idx));
                    old_totals[static_cast<size_t>(o_idx)]++;
                }

                if (!overlaps.empty()) {
                    std::sort(overlaps.begin(), overlaps.end());

                    std::vector<double> old_mass(static_cast<size_t>(old_cnt), 0.0);
                    if (old_kind == kKindSingle) {
                        if (std::abs(old_uniform) > world.eps) {
                            old_mass[0] = mul_clamp(old_uniform, old_single_volume);
                        }
                    } else if (old_kind == kKindMulti) {
                        for (int32_t p = 0; p < old_cnt; p++) {
                            if (p >= static_cast<int32_t>(old_multi.size()) ||
                                p >= static_cast<int32_t>(old_volume.size()))
                                continue;
                            const double dp = old_multi[static_cast<size_t>(p)];
                            if (std::abs(dp) > world.eps) {
                                old_mass[static_cast<size_t>(p)] =
                                        mul_clamp(dp, std::max(1, old_volume[static_cast<size_t>(p)]));
                            }
                        }
                    }

                    for (size_t i = 0; i < overlaps.size();) {
                        const uint32_t key = overlaps[i];
                        size_t j = i + 1;
                        while (j < overlaps.size() && overlaps[j] == key)
                            j++;

                        const uint32_t o = key >> 16u;
                        const double mass = old_mass[static_cast<size_t>(o)];
                        if (std::abs(mass) > world.eps) {
                            const int32_t total = old_totals[static_cast<size_t>(o)];
                            if (total > 0) {
                                const uint32_t n = key & 0xFFFFu;
                                const int32_t c = static_cast<int32_t>(j - i);
                                out_new_mass[static_cast<size_t>(n)] =
                                        add_clamp(out_new_mass[static_cast<size_t>(n)],
                                                  mul_clamp(mass / static_cast<double>(total), c));
                            }
                        }
                        i = j;
                    }
                }
            }
        }

        if (rebuilt_kind == kKindMulti) {
            for (int32_t p = 0; p < rebuilt_pocket_count; p++) {
                const int32_t vol = std::max(1, rebuilt_volume[static_cast<size_t>(p)]);
                rebuilt_density[static_cast<size_t>(p)] = sanitize_density(
                        world, java_strict_div(out_new_mass[static_cast<size_t>(p)], static_cast<double>(vol)));
            }
        } else {
            const int32_t vol = (rebuilt_kind == kKindUni || rebuilt_volume.empty()) ? kSectionVoxelCount
                                                                                     : std::max(1, rebuilt_volume[0]);
            rebuilt_density[0] = sanitize_density(world, java_strict_div(out_new_mass[0], static_cast<double>(vol)));
        }

        overlay_pending_density_bits_snapshot(world, section_id, rebuilt_kind, rebuilt_pocket_count, rebuilt_density);
        apply_pending_edits_snapshot(world, section_id, rebuilt_kind, rebuilt_pocket_map, rebuilt_density);
    }

    void append_section_dump_entries(const RadWorld &world, int32_t section_id, int32_t sy,
                                     std::vector<uint32_t> &out_sypi, std::vector<double> &out_density) {
        const SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto sidx = static_cast<size_t>(section_id);

        const uint8_t kind = sections.kind_by_id[sidx];
        const bool dirty = is_section_topology_dirty(sections, section_id);
        const auto *pending = section_pending_state_ptr(sections, section_id);
        const bool touched = pending != nullptr && !pending->local_edits.empty();
        const bool has_pending_bits = pending != nullptr && !pending->density_bits.empty();

        auto emit_density_vector = [&](int32_t emit_sy, const std::vector<double> &density) {
            for (size_t p = 0; p < density.size(); p++) {
                const double value = sanitize_density(world, density[p]);
                if (value == 0.0)
                    continue;
                const uint32_t sypi = (static_cast<uint32_t>(emit_sy) << 11u) | static_cast<uint32_t>(p);
                out_sypi.push_back(sypi);
                out_density.push_back(value);
            }
        };

        if (!touched && has_pending_bits) {
            std::vector<uint16_t> pockets;
            pockets.reserve(pending->density_bits.size());
            for (const auto &entry : pending->density_bits)
                pockets.push_back(entry.first);
            std::sort(pockets.begin(), pockets.end());

            for (const uint16_t pocket : pockets) {
                const auto it = pending->density_bits.find(pocket);
                if (it == pending->density_bits.end())
                    continue;
                double value = 0.0;
                std::memcpy(&value, &it->second, sizeof(value));
                value = sanitize_density(world, value);
                if (value == 0.0)
                    continue;
                const uint32_t sypi = (static_cast<uint32_t>(sy) << 11u) | pocket;
                out_sypi.push_back(sypi);
                out_density.push_back(value);
            }
            return;
        }

        if (!touched && kind == kKindNone)
            return;

        if (dirty || kind == kKindNone) {
            uint8_t rebuilt_kind = kKindNone;
            int32_t rebuilt_pocket_count = 0;
            std::array<uint16_t, kSectionVoxelCount> rebuilt_pocket_map{};
            std::vector<int32_t> rebuilt_volume;
            std::vector<int64_t> rebuilt_sum_xyz;
            compute_snapshot_mapping_from_mask(section_resistant_mask(sections, section_id), rebuilt_kind,
                                               rebuilt_pocket_count, rebuilt_pocket_map, rebuilt_volume,
                                               rebuilt_sum_xyz);

            std::vector<double> rebuilt_density;
            compute_snapshot_density_after_rebuild(world, section_id, rebuilt_kind, rebuilt_pocket_count,
                                                   rebuilt_pocket_map, rebuilt_volume, rebuilt_density);

            emit_density_vector(sy, rebuilt_density);
            return;
        }

        if (!touched) {
            if (kind == kKindUni || kind == kKindSingle) {
                const double value = sanitize_density(world, sections.uniform_density[sidx]);
                if (value != 0.0) {
                    out_sypi.push_back(static_cast<uint32_t>(sy) << 11u);
                    out_density.push_back(value);
                }
                return;
            }

            emit_density_vector(sy, section_multi_state(sections, section_id).pocket_density);
            return;
        }

        std::vector<double> density_snapshot;
        if (kind == kKindUni || kind == kKindSingle) {
            density_snapshot.assign(1u, sanitize_density(world, sections.uniform_density[sidx]));
        } else {
            density_snapshot = section_multi_state(sections, section_id).pocket_density;
        }

        overlay_pending_density_bits_snapshot(world, section_id, kind, static_cast<int32_t>(density_snapshot.size()),
                                              density_snapshot);
        apply_pending_edits_snapshot(world, section_id, kind, section_pocket_map(sections, section_id),
                                     density_snapshot);
        emit_density_vector(sy, density_snapshot);
    }

    void build_chunk_dump_entries(const RadWorld &world, int32_t chunk_id, std::vector<uint32_t> &out_sypi,
                                  std::vector<double> &out_density) {
        out_sypi.clear();
        out_density.clear();

        const ChunkTable &chunks = world.chunks;
        if (!chunk_id_in_range(chunks, chunk_id))
            return;

        out_sypi.reserve(64);
        out_density.reserve(64);

        for (int32_t sy = 0; sy < sections_per_chunk(chunks); sy++) {
            const int32_t section_id = section_id_for_chunk_sy(chunks, chunk_id, sy);
            if (section_id < 0)
                continue;
            append_section_dump_entries(world, section_id, sy, out_sypi, out_density);
        }
    }

    void clear_section_topology_dirty(SectionTable &sections, int32_t section_id) {
        if (!section_id_in_range(sections, section_id))
            return;
        sections.topology_dirty[static_cast<size_t>(section_id)] = 0u;
    }

    template <bool Wide> struct DirtyChunkWork {
        int32_t chunk_id = kNoIndex;
        std::array<uint32_t, Wide ? (kMaxChunkSections + 31) / 32 : 1> sy_masks{};
    };

    template <bool Wide>
    void rebuild_dirty_chunk_batch(RadWorld &world, const std::vector<DirtyChunkWork<Wide>> &work_items, int32_t lo,
                                   int32_t hi) {
        SectionTable &sections = world.sections;
        for (int32_t i = lo; i < hi; i++) {
            const DirtyChunkWork<Wide> &work = work_items[static_cast<size_t>(i)];
            if (!chunk_id_in_range(world.chunks, work.chunk_id))
                continue;

            const int32_t word_count = Wide ? world.chunks.words_per_chunk : 1;
            for (int32_t word = 0; word < word_count; word++) {
                auto mask = work.sy_masks[static_cast<size_t>(word)];
                while (mask != 0u) {
                    const int32_t sy = word * kChunkWordSections + std::countr_zero(mask);
                    mask = mask & (mask - 1u);

                    const int32_t section_id = section_id_for_chunk_sy(world.chunks, work.chunk_id, sy);
                    if (!section_id_in_range(sections, section_id))
                        continue;
                    const auto sidx = static_cast<size_t>(section_id);
                    if (sections.key_by_id[sidx] == kInvalidSectionKey)
                        continue;
                    rebuild_section_topology(world, work.chunk_id, sy, section_id);
                }
            }
            mark_chunk_dirty(world, work.chunk_id);
        }
    }

    template <bool Wide> void rebuild_dirty_sections_impl(RadWorld &world) {
        SectionTable &sections = world.sections;
        if (sections.topology_dirty_section_ids.empty())
            return;

        std::vector<int32_t> dirty_ids = std::move(sections.topology_dirty_section_ids);
        sections.topology_dirty_section_ids.clear();

        std::vector<DirtyChunkWork<Wide>> work_items;
        const size_t dirty_chunk_bound = std::min(dirty_ids.size(), world.chunks.ck_by_id.size());
        work_items.reserve(dirty_chunk_bound);
        std::unordered_map<int32_t, size_t> chunk_to_work_index;
        chunk_to_work_index.reserve(dirty_chunk_bound);

        for (const int32_t section_id : dirty_ids) {
            if (!section_id_in_range(sections, section_id))
                continue;
            const auto sidx = static_cast<size_t>(section_id);
            if (sections.topology_dirty[sidx] == 0u)
                continue;
            sections.topology_dirty[sidx] = 0u;

            if (sections.key_by_id[sidx] == kInvalidSectionKey)
                continue;
            const int32_t owner = sections.owner_chunk_id[sidx];
            const int32_t sy = sections.sy_by_id[sidx];
            if (!chunk_id_in_range(world.chunks, owner))
                continue;

            mark_relink_section(world, owner, sy);

            const size_t word = Wide ? static_cast<uint32_t>(sy) >> 5u : 0u;
            const uint32_t bit = 1u << (static_cast<uint32_t>(sy) & 31u);
            if (const auto it = chunk_to_work_index.find(owner); it != chunk_to_work_index.end()) {
                work_items[it->second].sy_masks[word] |= bit;
                continue;
            }

            chunk_to_work_index.emplace(owner, work_items.size());
            DirtyChunkWork<Wide> work;
            work.chunk_id = owner;
            work.sy_masks[word] = bit;
            work_items.push_back(work);
        }

        const int32_t work_count = static_cast<int32_t>(work_items.size());
        const int32_t threshold = get_task_threshold(work_count, 8);
        parallel_split_range(world, 0, work_count, threshold, [&](int32_t lo, int32_t hi) {
            rebuild_dirty_chunk_batch<Wide>(world, work_items, lo, hi);
        });
    }

    void rebuild_dirty_sections(RadWorld &world) {
        if (world.chunks.words_per_chunk == 1) [[likely]] {
            rebuild_dirty_sections_impl<false>(world);
        } else {
            rebuild_dirty_sections_impl<true>(world);
        }
    }
}
