// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/internal.hpp"

namespace hbm::radsim::detail {
    void queue_section_edit_fields(RadWorld &world, int32_t section_id, uint16_t local, double add_value, bool has_set,
                                   double set_value, uint64_t set_seq) {
        PendingLocalEdit entry;
        entry.local = local;
        entry.add_value = add_value;
        entry.flags = has_set ? kEditFlagHasSet : 0u;
        entry.set_value = set_value;
        entry.set_seq = set_seq;
        queue_section_edit(world, section_id, entry);
    }

    void queue_section_edit(RadWorld &world, int32_t section_id, const PendingLocalEdit &entry) {
        SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;

        auto &pending = ensure_section_pending_state(sections, section_id);
        pending.local_edits.push_back(entry);

        if (pending.touched == 0u) {
            pending.touched = 1u;
            sections.touched_section_ids.push_back(section_id);
        }
    }

    void apply_pending_edits(RadWorld &world) {
        SectionTable &sections = world.sections;
        ChunkTable &chunks = world.chunks;
        PendingEditScratch scratch;
        for (const int32_t section_id : sections.touched_section_ids) {
            if (!section_id_in_range(sections, section_id))
                continue;
            auto *pending = section_pending_state_ptr(sections, section_id);
            if (pending == nullptr)
                continue;
            pending->touched = 0u;
            const auto idx = static_cast<size_t>(section_id);
            if (sections.key_by_id[idx] == kInvalidSectionKey)
                continue;
            if (pending->local_edits.empty()) {
                maybe_release_section_pending_state(sections, section_id);
                continue;
            }

            const uint8_t kind = sections.kind_by_id[idx];
            auto &edits = pending->local_edits;
            bool active = false;

            if (kind == kKindNone) {
                sections.uniform_density[idx] = 0.0;
                const auto *multi = section_multi_state_ptr(sections, section_id);
                if (multi != nullptr && !multi->pocket_density.empty()) {
                    ensure_section_multi_state(sections, section_id).pocket_density.clear();
                }
                active = false;
            } else if (kind == kKindMulti) {
                const int32_t pocket_count = static_cast<int32_t>(sections.pocket_count[idx]);
                auto &multi = ensure_section_multi_state(sections, section_id);
                if (pocket_count <= 0) {
                    sections.uniform_density[idx] = 0.0;
                    multi.pocket_density.clear();
                    active = false;
                } else {
                    auto &density = multi.pocket_density;
                    if (density.size() != static_cast<size_t>(pocket_count)) {
                        density.assign(static_cast<size_t>(pocket_count),
                                       sanitize_density(world, sections.uniform_density[idx]));
                    }

                    resolve_pending_edits(edits, &section_pocket_map(sections, section_id),
                                          static_cast<size_t>(pocket_count), density.data(), scratch);

                    const auto &volume = multi.pocket_volume;
                    for (int32_t p = 0; p < pocket_count; p++) {
                        const size_t pi = static_cast<size_t>(p);
                        density[pi] = sanitize_density(world, density[pi]);
                        active |= (density[pi] != 0.0);
                    }
                    sections.uniform_density[idx] = weighted_average_density(world, density, volume);
                }
            } else {

                const SectionPocketMap *pocket_of =
                        (kind == kKindSingle) ? &section_pocket_map(sections, section_id) : nullptr;
                resolve_pending_edits(edits, pocket_of, 1u, &sections.uniform_density[idx], scratch);
                sections.uniform_density[idx] = sanitize_density(world, sections.uniform_density[idx]);
                active = sections.uniform_density[idx] != 0.0;
            }

            sections.active_by_id[idx] = active ? 1u : 0u;
            mark_section_owner_dirty(world, section_id);

            const int32_t owner = sections.owner_chunk_id[idx];
            const int32_t sy = static_cast<int32_t>(sections.sy_by_id[idx]);
            if (chunk_id_in_range(chunks, owner) && sy_in_range(chunks, sy)) {
                set_chunk_section_active(chunks, owner, sy, active);
            }

            edits.clear();
            maybe_release_section_pending_state(sections, section_id);
        }
        sections.touched_section_ids.clear();
    }
}
