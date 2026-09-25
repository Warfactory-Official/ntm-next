// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/internal.hpp"

namespace hbm::radsim::detail {
    thread_local std::vector<uint16_t> g_face_pocket_counts;
    thread_local std::vector<uint16_t> g_face_touched_pockets;

    namespace {
        thread_local std::vector<double> g_edit_add_acc;
        thread_local std::vector<double> g_edit_set_val;
        thread_local std::vector<uint64_t> g_edit_best_seq;
        thread_local std::vector<uint8_t> g_edit_has_set;

        inline void reset_edit_scratch(size_t n) {
            if (g_edit_add_acc.size() < n)
                g_edit_add_acc.resize(n);
            if (g_edit_set_val.size() < n)
                g_edit_set_val.resize(n);
            if (g_edit_best_seq.size() < n)
                g_edit_best_seq.resize(n);
            if (g_edit_has_set.size() < n)
                g_edit_has_set.resize(n);
            std::fill_n(g_edit_add_acc.data(), n, 0.0);
            std::fill_n(g_edit_set_val.data(), n, 0.0);
            std::fill_n(g_edit_best_seq.data(), n, 0ull);
            std::fill_n(g_edit_has_set.data(), n, uint8_t{0});
        }
    }

    uint64_t checksum_mask_words(const uint64_t *words) {
        uint64_t h = 0x9E37'79B9'7F4A'7C15ull;
        for (int i = 0; i < kMaskWordsPerSection; i++) {
            h ^= words[i] + 0x9E37'79B9'7F4A'7C15ull + (h << 6u) + (h >> 2u);
        }
        return h;
    }

    bool is_resistant_local(const std::array<uint64_t, kMaskWordsPerSection> &words, int32_t local) {
        const uint32_t bit = static_cast<uint32_t>(local) & 63u;
        const uint32_t word_idx = static_cast<uint32_t>(local) >> 6u;
        return (words[word_idx] & (1ull << bit)) != 0ull;
    }

    void set_resistant_local(std::array<uint16_t, kSectionVoxelCount> &pocket_map, int32_t local) {
        pocket_map[static_cast<size_t>(local)] = kNoPocket;
    }

    int32_t local_index_for_face_row_col(int32_t face, int32_t row, int32_t col) {
        switch (face) {
        case 0:
            return (row << 4) | col;
        case 1:
            return (15 << 8) | (row << 4) | col;
        case 2:
            return (row << 8) | col;
        case 3:
            return (row << 8) | (15 << 4) | col;
        case 4:
            return (row << 8) | (col << 4);
        case 5:
            return (row << 8) | (col << 4) | 15;
        default:
            return 0;
        }
    }

    int32_t flood_fill_pockets(const std::array<uint64_t, kMaskWordsPerSection> &words,
                               std::array<uint16_t, kSectionVoxelCount> &pocket_map,
                               std::vector<int32_t> &pocket_volume, std::vector<int64_t> &pocket_sum_xyz) {
        pocket_map.fill(kNoPocket);
        pocket_volume.clear();
        pocket_sum_xyz.clear();

        bool all_open = true;
        bool all_blocked = true;
        for (const uint64_t word : words) {
            all_open &= (word == 0u);
            all_blocked &= (word == ~0ull);
        }
        if (all_open) {
            pocket_map.fill(0u);
            pocket_volume.push_back(kSectionVoxelCount);
            pocket_sum_xyz.push_back(kSectionAxisCoordSum);
            pocket_sum_xyz.push_back(kSectionAxisCoordSum);
            pocket_sum_xyz.push_back(kSectionAxisCoordSum);
            return 1;
        }
        if (all_blocked) {
            return 0;
        }

        std::array<int32_t, kSectionVoxelCount> queue{};
        int32_t pocket_count = 0;

        for (int32_t local = 0; local < kSectionVoxelCount; local++) {
            if (is_resistant_local(words, local)) {
                set_resistant_local(pocket_map, local);
                continue;
            }
            if (pocket_map[static_cast<size_t>(local)] != kNoPocket)
                continue;
            if (pocket_count >= static_cast<int32_t>(std::numeric_limits<uint16_t>::max()))
                break;

            const uint16_t pocket_id = static_cast<uint16_t>(pocket_count);
            int32_t qh = 0;
            int32_t qt = 0;
            queue[qt++] = local;
            pocket_map[static_cast<size_t>(local)] = pocket_id;
            int32_t volume = 0;
            int64_t sum_x = 0;
            int64_t sum_y = 0;
            int64_t sum_z = 0;

            while (qh < qt) {
                const int32_t idx = queue[qh++];
                volume++;
                const int32_t x = idx & 15;
                const int32_t z = (idx >> 4) & 15;
                const int32_t y = (idx >> 8) & 15;
                sum_x += x;
                sum_y += y;
                sum_z += z;

                auto try_push = [&](int32_t nx, int32_t ny, int32_t nz) {
                    if (nx < 0 || nx >= 16 || ny < 0 || ny >= 16 || nz < 0 || nz >= 16)
                        return;
                    const int32_t ni = (ny << 8) | (nz << 4) | nx;
                    auto &dst = pocket_map[static_cast<size_t>(ni)];
                    if (dst != kNoPocket)
                        return;
                    if (is_resistant_local(words, ni)) {
                        dst = kNoPocket;
                        return;
                    }
                    dst = pocket_id;
                    queue[qt++] = ni;
                };

                try_push(x, y - 1, z);
                try_push(x, y + 1, z);
                try_push(x, y, z - 1);
                try_push(x, y, z + 1);
                try_push(x - 1, y, z);
                try_push(x + 1, y, z);
            }

            pocket_volume.push_back(volume);
            pocket_sum_xyz.push_back(sum_x);
            pocket_sum_xyz.push_back(sum_y);
            pocket_sum_xyz.push_back(sum_z);
            pocket_count++;
        }

        return pocket_count;
    }

    void apply_pending_edits_during_rebuild(RadWorld &world, int32_t section_id, uint8_t rebuilt_kind,
                                            const std::array<uint16_t, kSectionVoxelCount> &rebuilt_pocket_map,
                                            std::vector<double> &rebuilt_density) {
        SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;

        auto *pending = section_pending_state_ptr(sections, section_id);
        if (pending == nullptr || pending->local_edits.empty()) {
            if (pending != nullptr) {
                pending->touched = 0u;
                maybe_release_section_pending_state(sections, section_id);
            }
            return;
        }
        auto &edits = pending->local_edits;

        if (rebuilt_kind == kKindNone || rebuilt_density.empty()) {
            edits.clear();
            pending->touched = 0u;
            maybe_release_section_pending_state(sections, section_id);
            return;
        }

        const size_t scratch_count = rebuilt_density.size();
        reset_edit_scratch(scratch_count);
        double *const add_acc = g_edit_add_acc.data();
        double *const set_val = g_edit_set_val.data();
        uint64_t *const best_seq = g_edit_best_seq.data();
        uint8_t *const has_set = g_edit_has_set.data();

        for (const PendingLocalEdit &edit : edits) {
            const uint16_t local = static_cast<uint16_t>(edit.local & 0x0FFFu);

            size_t pocket_index = 0u;
            if (rebuilt_kind != kKindUni) {
                const uint16_t pocket = rebuilt_pocket_map[static_cast<size_t>(local)];
                if (pocket == kNoPocket)
                    continue;
                pocket_index = static_cast<size_t>(pocket);
                if (pocket_index >= scratch_count)
                    continue;
            }

            add_acc[pocket_index] += edit.add_value;
            if ((edit.flags & kEditFlagHasSet) != 0u && edit.set_seq > best_seq[pocket_index]) {
                best_seq[pocket_index] = edit.set_seq;
                set_val[pocket_index] = edit.set_value;
                has_set[pocket_index] = 1u;
            }
        }

        for (size_t p = 0; p < rebuilt_density.size(); p++) {
            const double base = (has_set[p] != 0u) ? set_val[p] : rebuilt_density[p];
            rebuilt_density[p] = sanitize_density(world, base + add_acc[p]);
        }

        edits.clear();
        pending->touched = 0u;
        maybe_release_section_pending_state(sections, section_id);
    }

    std::vector<float> consume_pending_diffusivity(RadWorld &world, int32_t section_id, int32_t pocket_count) {
        SectionTable &sections = world.sections;
        std::vector<float> out;
        if (pocket_count > 0) {
            out.assign(static_cast<size_t>(pocket_count), 1.0f);
        }

        auto *pending = section_pending_state_ptr(sections, section_id);
        if (pending == nullptr || pending->pocket_diffusivity.empty())
            return out;

        if (pocket_count > 0) {
            if (pending->pocket_diffusivity.size() >= static_cast<size_t>(pocket_count)) {
                for (int32_t i = 0; i < pocket_count; i++) {
                    out[static_cast<size_t>(i)] =
                            clamp_diffusivity(pending->pocket_diffusivity[static_cast<size_t>(i)]);
                }
            } else {

                world.diffusivity_pocket_mismatches++;
            }
        }

        pending->pocket_diffusivity.clear();
        maybe_release_section_pending_state(sections, section_id);
        return out;
    }

    struct PriorSectionState {
        uint8_t kind;
        double uniform;
        uint16_t pocket_count;
        int32_t single_volume;
        const std::vector<double> &density;
        const std::vector<int32_t> &volume;
        const SectionPocketMap &map;
    };

    void redistribute_section_mass(const RadWorld &world, const PriorSectionState &prior, uint8_t kind,
                                   int32_t new_pocket_count, const std::vector<int32_t> &new_volume,
                                   const SectionPocketMap &pocket_map, std::vector<double> &out_new_mass) {
        if (prior.kind != kKindNone && new_pocket_count > 0) {
            if (kind == kKindUni) {
                double total_mass = 0.0;
                if (prior.kind == kKindUni) {
                    if (std::abs(prior.uniform) > world.eps) {
                        total_mass = prior.uniform * static_cast<double>(kSectionVoxelCount);
                    }
                } else if (prior.pocket_count > 0) {
                    if (prior.kind == kKindSingle) {
                        if (std::abs(prior.uniform) > world.eps) {
                            total_mass = prior.uniform * static_cast<double>(prior.single_volume);
                        }
                    } else if (prior.kind == kKindMulti) {
                        const int32_t old_cnt = prior.pocket_count;
                        for (int32_t p = 0; p < old_cnt; p++) {
                            if (p >= static_cast<int32_t>(prior.density.size()) ||
                                p >= static_cast<int32_t>(prior.volume.size()))
                                continue;
                            const double dp = prior.density[static_cast<size_t>(p)];
                            if (std::abs(dp) > world.eps) {
                                total_mass +=
                                        dp * static_cast<double>(std::max(1, prior.volume[static_cast<size_t>(p)]));
                            }
                        }
                    }
                }
                out_new_mass[0] = total_mass;
            } else if (prior.kind == kKindUni) {
                if (std::abs(prior.uniform) > world.eps) {
                    const double old_mass = mul_clamp(prior.uniform, kSectionVoxelCount);
                    int64_t total_new_air = 0;
                    for (int32_t p = 0; p < new_pocket_count; p++) {
                        total_new_air += std::max(1, new_volume[static_cast<size_t>(p)]);
                    }
                    if (total_new_air > 0) {
                        const double mass_per_block = old_mass / static_cast<double>(total_new_air);
                        for (int32_t p = 0; p < new_pocket_count; p++) {
                            out_new_mass[static_cast<size_t>(p)] =
                                    mul_clamp(mass_per_block, std::max(1, new_volume[static_cast<size_t>(p)]));
                        }
                    }
                }
            } else if (prior.pocket_count > 0) {
                const int32_t old_cnt = prior.pocket_count;
                std::vector<int32_t> old_totals(static_cast<size_t>(old_cnt), 0);
                std::vector<uint32_t> overlaps;
                overlaps.reserve(kSectionVoxelCount);

                for (int32_t local = 0; local < kSectionVoxelCount; local++) {
                    const uint16_t n_idx = pocket_map[static_cast<size_t>(local)];
                    if (n_idx == kNoPocket || n_idx >= static_cast<uint16_t>(new_pocket_count))
                        continue;

                    const uint16_t o_idx = prior.map[static_cast<size_t>(local)];
                    if (o_idx == kNoPocket || o_idx >= static_cast<uint16_t>(old_cnt))
                        continue;

                    overlaps.push_back((static_cast<uint32_t>(o_idx) << 16u) | static_cast<uint32_t>(n_idx));
                    old_totals[static_cast<size_t>(o_idx)]++;
                }

                if (!overlaps.empty()) {
                    std::sort(overlaps.begin(), overlaps.end());

                    std::vector<double> old_mass(static_cast<size_t>(old_cnt), 0.0);
                    if (prior.kind == kKindSingle) {
                        if (std::abs(prior.uniform) > world.eps) {
                            old_mass[0] = mul_clamp(prior.uniform, prior.single_volume);
                        }
                    } else if (prior.kind == kKindMulti) {
                        for (int32_t p = 0; p < old_cnt; p++) {
                            if (p >= static_cast<int32_t>(prior.density.size()) ||
                                p >= static_cast<int32_t>(prior.volume.size()))
                                continue;
                            const double dp = prior.density[static_cast<size_t>(p)];
                            if (std::abs(dp) > world.eps) {
                                old_mass[static_cast<size_t>(p)] =
                                        mul_clamp(dp, std::max(1, prior.volume[static_cast<size_t>(p)]));
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
                                const auto c = static_cast<int32_t>(j - i);
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
    }

    void rebuild_section_topology(RadWorld &world, int32_t chunk_id, int32_t sy, int32_t section_id) {
        SectionTable &sections = world.sections;
        ChunkTable &chunks = world.chunks;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto sec_idx = static_cast<size_t>(section_id);

        const uint8_t old_kind = sections.kind_by_id[sec_idx];
        const double old_uniform = sections.uniform_density[sec_idx];
        const uint16_t old_pocket_count = sections.pocket_count[sec_idx];
        const auto old_map = section_pocket_map(sections, section_id);
        const auto &old_multi_state = section_multi_state(sections, section_id);
        const auto &old_volume = old_multi_state.pocket_volume;
        const auto &old_multi = old_multi_state.pocket_density;
        const int32_t old_single_volume = std::max(1, sections.single_volume[sec_idx]);

        auto &pocket_map = ensure_section_pocket_map(sections, section_id);
        std::vector<int32_t> new_volume;
        std::vector<int64_t> pocket_sum_xyz;
        const auto &words = section_resistant_mask(sections, section_id);
        const int32_t new_pocket_count = flood_fill_pockets(words, pocket_map, new_volume, pocket_sum_xyz);
        sections.pocket_count[sec_idx] = static_cast<uint16_t>(new_pocket_count);

        uint8_t kind = kKindNone;
        if (new_pocket_count <= 0) {
            kind = kKindNone;
        } else if (new_pocket_count == 1) {
            const bool fully_open = (new_volume.size() == 1u && new_volume.front() == kSectionVoxelCount);
            kind = fully_open ? kKindUni : kKindSingle;
        } else {
            kind = kKindMulti;
        }

        std::vector<double> out_new_mass(static_cast<size_t>(std::max(1, new_pocket_count)), 0.0);

        redistribute_section_mass(world,
                                  PriorSectionState{old_kind, old_uniform, old_pocket_count, old_single_volume,
                                                    old_multi, old_volume, old_map},
                                  kind, new_pocket_count, new_volume, pocket_map, out_new_mass);

        std::vector<double> rebuilt_density;
        if (kind != kKindNone) {
            rebuilt_density.assign(static_cast<size_t>(std::max(1, new_pocket_count)), 0.0);
            if (kind == kKindMulti) {
                for (int32_t p = 0; p < new_pocket_count; p++) {
                    const int32_t vol = std::max(1, new_volume[static_cast<size_t>(p)]);
                    rebuilt_density[static_cast<size_t>(p)] = sanitize_density(
                            world, java_strict_div(out_new_mass[static_cast<size_t>(p)], static_cast<double>(vol)));
                }
            } else {
                const int32_t vol =
                        (kind == kKindUni || new_volume.empty()) ? kSectionVoxelCount : std::max(1, new_volume[0]);
                rebuilt_density[0] =
                        sanitize_density(world, java_strict_div(out_new_mass[0], static_cast<double>(vol)));
            }
        }

        auto *pending_state = section_pending_state_ptr(sections, section_id);
        if (pending_state != nullptr && !pending_state->density_bits.empty()) {
            auto &pending = pending_state->density_bits;
            if (kind == kKindMulti) {
                for (int32_t p = 0; p < new_pocket_count; p++) {
                    const auto it = pending.find(static_cast<uint16_t>(p));
                    if (it == pending.end())
                        continue;
                    double value = 0.0;
                    std::memcpy(&value, &it->second, sizeof(value));
                    rebuilt_density[static_cast<size_t>(p)] = sanitize_density(world, value);
                    pending.erase(it);
                }
                for (auto it = pending.begin(); it != pending.end();) {
                    if (it->first >= static_cast<uint16_t>(new_pocket_count))
                        it = pending.erase(it);
                    else
                        ++it;
                }
            } else if (kind == kKindUni || kind == kKindSingle) {
                const auto it = pending.find(0u);
                if (it != pending.end()) {
                    double value = 0.0;
                    std::memcpy(&value, &it->second, sizeof(value));
                    rebuilt_density[0] = sanitize_density(world, value);
                    pending.erase(it);
                }
                std::erase_if(pending, [](const auto &kv) { return kv.first >= 1u; });
            } else {
                pending.clear();
            }
            maybe_release_section_pending_state(sections, section_id);
        }

        apply_pending_edits_during_rebuild(world, section_id, kind, pocket_map, rebuilt_density);
        std::vector<float> rebuilt_diffusivity =
                consume_pending_diffusivity(world, section_id, (kind == kKindNone) ? 0 : std::max(1, new_pocket_count));

        if (kind == kKindMulti) {
            auto &multi = ensure_section_multi_state(sections, section_id);
            auto &density = multi.pocket_density;
            density = rebuilt_density;
            multi.pocket_volume = new_volume;
            auto &face_distance = multi.face_distance;
            auto &pocket_diffusivity = multi.pocket_diffusivity;
            auto &pocket_inv_volume = multi.pocket_inv_volume;
            face_distance.assign(static_cast<size_t>(new_pocket_count) * 6u, 0.0f);
            pocket_diffusivity = rebuilt_diffusivity;
            pocket_inv_volume.resize(static_cast<size_t>(new_pocket_count));

            bool any_active = false;
            double total_mass = 0.0;
            int64_t total_open = 0;
            for (int32_t pi = 0; pi < new_pocket_count; pi++) {
                const size_t p = static_cast<size_t>(pi);
                const double value = density[p];
                if (value != 0.0) {
                    any_active = true;
                }
                const int32_t vol = (p < new_volume.size()) ? std::max(1, new_volume[p]) : 1;
                total_mass = java_strict_add(total_mass, java_strict_mul(value, static_cast<double>(vol)));
                total_open += vol;
                const double inv = java_strict_div(1.0, static_cast<double>(vol));
                pocket_inv_volume[p] = inv;
                const size_t s_base = p * 3u;
                const double cx = java_strict_mul(static_cast<double>(pocket_sum_xyz[s_base]), inv);
                const double cy = java_strict_mul(static_cast<double>(pocket_sum_xyz[s_base + 1u]), inv);
                const double cz = java_strict_mul(static_cast<double>(pocket_sum_xyz[s_base + 2u]), inv);
                const size_t d_base = p * 6u;
                face_distance[d_base] = transport_quantize_multi_face_distance_storage(java_strict_add(cy, 0.5));
                face_distance[d_base + 1u] = transport_quantize_multi_face_distance_storage(java_strict_sub(15.5, cy));
                face_distance[d_base + 2u] = transport_quantize_multi_face_distance_storage(java_strict_add(cz, 0.5));
                face_distance[d_base + 3u] = transport_quantize_multi_face_distance_storage(java_strict_sub(15.5, cz));
                face_distance[d_base + 4u] = transport_quantize_multi_face_distance_storage(java_strict_add(cx, 0.5));
                face_distance[d_base + 5u] = transport_quantize_multi_face_distance_storage(java_strict_sub(15.5, cx));
            }
            sections.uniform_density[sec_idx] =
                    (total_open > 0)
                            ? sanitize_density(world, java_strict_div(total_mass, static_cast<double>(total_open)))
                            : 0.0;
            sections.uniform_diffusivity[sec_idx] = !rebuilt_diffusivity.empty() ? rebuilt_diffusivity[0] : 1.0f;
            sections.single_diffusivity[sec_idx] = 1.0f;
            sections.active_by_id[sec_idx] = any_active ? 1u : 0u;
        } else {
            release_section_multi_state(sections, section_id);
            if (kind == kKindNone) {
                sections.uniform_density[sec_idx] = 0.0;
                sections.uniform_diffusivity[sec_idx] = 1.0f;
                sections.single_diffusivity[sec_idx] = 1.0f;
                sections.active_by_id[sec_idx] = 0u;
            } else {
                sections.uniform_density[sec_idx] = rebuilt_density.empty() ? 0.0 : rebuilt_density[0];
                sections.uniform_diffusivity[sec_idx] =
                        (kind == kKindUni && !rebuilt_diffusivity.empty()) ? rebuilt_diffusivity[0] : 1.0f;
                sections.single_diffusivity[sec_idx] =
                        (kind == kKindSingle && !rebuilt_diffusivity.empty()) ? rebuilt_diffusivity[0] : 1.0f;
                sections.active_by_id[sec_idx] = (sections.uniform_density[sec_idx] != 0.0) ? 1u : 0u;
            }
        }

        sections.single_conn_counts[sec_idx].fill(0u);
        if (kind != kKindMulti) {
            release_section_multi_state(sections, section_id);
        } else {
            auto &multi = ensure_section_multi_state(sections, section_id);
            multi.edge_counts.fill(0u);
            for (auto &face_edges : multi.edges_by_face)
                face_edges.clear();
            multi.edge_offset.fill(0u);
            multi.clear_edge_streams();
        }

        if (kind == kKindSingle) {
            auto &face_open = sections.single_face_open_counts[sec_idx];
            face_open.fill(0u);
            for (int32_t face = 0; face < 6; face++) {
                int32_t count = 0;
                for (int32_t row = 0; row < 16; row++) {
                    for (int32_t col = 0; col < 16; col++) {
                        const int32_t local = local_index_for_face_row_col(face, row, col);
                        if (pocket_map[static_cast<size_t>(local)] != kNoPocket)
                            count++;
                    }
                }
                face_open[static_cast<size_t>(face)] = static_cast<uint16_t>(count);
            }
            auto &face_distance = sections.single_face_distance[sec_idx];
            const int32_t vol = (!new_volume.empty()) ? std::max(1, new_volume.front()) : 1;
            const double inv = java_strict_div(1.0, static_cast<double>(vol));
            const double cx = java_strict_mul(static_cast<double>(pocket_sum_xyz[0]), inv);
            const double cy = java_strict_mul(static_cast<double>(pocket_sum_xyz[1]), inv);
            const double cz = java_strict_mul(static_cast<double>(pocket_sum_xyz[2]), inv);
            face_distance[0] = java_strict_add(cy, 0.5);
            face_distance[1] = java_strict_sub(15.5, cy);
            face_distance[2] = java_strict_add(cz, 0.5);
            face_distance[3] = java_strict_sub(15.5, cz);
            face_distance[4] = java_strict_add(cx, 0.5);
            face_distance[5] = java_strict_sub(15.5, cx);
            sections.single_volume[sec_idx] = vol;
            sections.single_inv_volume[sec_idx] = inv;
        } else {
            sections.single_volume[sec_idx] = 1;
            sections.single_face_open_counts[sec_idx].fill(0u);
            sections.single_face_distance[sec_idx].fill(0.0);
            sections.single_diffusivity[sec_idx] = 1.0f;
            sections.single_inv_volume[sec_idx] = 1.0;
        }

        if (kind == kKindUni || kind == kKindNone) {
            release_section_pocket_map(sections, section_id);
            release_vector_memory(new_volume);
        }
        release_section_resistant_mask(sections, section_id);

        sections.kind_by_id[sec_idx] = kind;
        set_chunk_section_kind(chunks, chunk_id, sy, kind);
        set_chunk_section_active(chunks, chunk_id, sy, sections.active_by_id[sec_idx] != 0u);
    }

    int32_t section_id_for_chunk_sy(const ChunkTable &chunks, int32_t chunk_id, int32_t sy) {
        if (!chunk_id_in_range(chunks, chunk_id))
            return kNoIndex;
        if (!sy_in_range(chunks, sy))
            return kNoIndex;
        return section_ids_of(chunks, chunk_id)[sy];
    }

    int64_t set_section_y_raw(int64_t section_key, uint32_t y_value) {
        const uint64_t raw = static_cast<uint64_t>(section_key);
        const uint64_t with_y = (raw & ~0x0F'FFFFULL) | (static_cast<uint64_t>(y_value) & 0x0F'FFFFULL);
        return static_cast<int64_t>(with_y);
    }

    int64_t pocket_key(int64_t section_key, uint16_t pocket_index) {
        const uint32_t sy = static_cast<uint32_t>(decode_section_y(section_key));
        const uint32_t yz = (sy << 11u) | static_cast<uint32_t>(pocket_index & 0x07FFu);
        return set_section_y_raw(section_key, yz);
    }

    int32_t hi_word(double x) {
        uint64_t bits = 0u;
        std::memcpy(&bits, &x, sizeof(bits));
        return static_cast<int32_t>(bits >> 32u);
    }

    int32_t lo_word(double x) {
        uint64_t bits = 0u;
        std::memcpy(&bits, &x, sizeof(bits));
        return static_cast<int32_t>(bits & 0xFFFF'FFFFu);
    }

    double with_hi_word(double x, int32_t hi) {
        uint64_t bits = 0u;
        std::memcpy(&bits, &x, sizeof(bits));
        bits = (bits & 0x0000'0000'FFFF'FFFFull) | (static_cast<uint64_t>(static_cast<uint32_t>(hi)) << 32u);
        double out = 0.0;
        std::memcpy(&out, &bits, sizeof(out));
        return out;
    }

    double java_compat_exp(double x) {
        constexpr int32_t exp_signif_bits = 0x7fff'ffff;
        constexpr double huge = 1.0e300;
        constexpr double twom1000 = 0x1.0p-1000;
        constexpr double o_threshold = 0x1.62e42fefa39efp9;
        constexpr double u_threshold = -0x1.74910d52d3051p9;
        constexpr double ln2_hi[2] = {0x1.62e42feep-1, -0x1.62e42feep-1};
        constexpr double ln2_lo[2] = {0x1.a39ef35793c76p-33, -0x1.a39ef35793c76p-33};
        constexpr double invln2 = 0x1.71547652b82fep0;
        constexpr double half[2] = {0.5, -0.5};
        constexpr double p1 = 0x1.555555555553ep-3;
        constexpr double p2 = -0x1.6c16c16bebd93p-9;
        constexpr double p3 = 0x1.1566aaf25de2cp-14;
        constexpr double p4 = -0x1.bbd41c5d26bf1p-20;
        constexpr double p5 = 0x1.6376972bea4d0p-25;

        double hi = 0.0;
        double lo = 0.0;
        double c = 0.0;
        double t = 0.0;
        int32_t k = 0;
        int32_t hx = hi_word(x);
        const int32_t xsb = (hx >> 31) & 1;
        hx &= exp_signif_bits;

        if (hx >= 0x40862E42) {
            if (hx >= 0x7ff0'0000) {
                if (((hx & 0x000f'ffff) | lo_word(x)) != 0)
                    return x + x;
                return (xsb == 0) ? x : 0.0;
            }
            if (x > o_threshold)
                return huge * huge;
            if (x < u_threshold)
                return twom1000 * twom1000;
        }

        if (hx > 0x3fd62e42) {
            if (hx < 0x3FF0A2B2) {
                hi = x - ln2_hi[xsb];
                lo = ln2_lo[xsb];
                k = 1 - xsb - xsb;
            } else {
                k = static_cast<int32_t>(invln2 * x + half[xsb]);
                t = static_cast<double>(k);
                hi = x - t * ln2_hi[0];
                lo = t * ln2_lo[0];
            }
            x = hi - lo;
        } else if (hx < 0x3e30'0000) {
            if (huge + x > 1.0)
                return 1.0 + x;
        } else {
            k = 0;
        }

        t = x * x;
        c = x - t * (p1 + t * (p2 + t * (p3 + t * (p4 + t * p5))));
        if (k == 0) {
            return 1.0 - ((x * c) / (c - 2.0) - x);
        }

        double y = 1.0 - ((lo - (x * c) / (2.0 - c)) - hi);
        if (k >= -1021) {
            return with_hi_word(y, hi_word(y) + (k << 20));
        }
        y = with_hi_word(y, hi_word(y) + ((k + 1000) << 20));
        return y * twom1000;
    }

    void reset_chunk_payload_state(RadWorld &world, int32_t chunk_id) {
        if (!chunk_id_in_range(world.chunks, chunk_id))
            return;
        remove_sections_for_chunk(world, chunk_id);
        const auto idx = static_cast<size_t>(chunk_id);
        const size_t metadata_base = chunk_word_base(world.chunks, chunk_id);
        std::fill_n(world.chunks.kinds_by_id.data() + metadata_base, world.chunks.words_per_chunk, 0u);
        std::fill_n(world.chunks.active_mask_by_id.data() + metadata_base, world.chunks.words_per_chunk, 0u);
        std::fill_n(world.chunks.source_mask_by_id.data() + metadata_base, world.chunks.words_per_chunk, 0u);
        world.chunks.dirty_by_id[idx] = 0u;
        world.chunks.dirty_event_pending_by_id[idx] = 0u;
        mark_relink_column(world, chunk_id);
        world.links_dirty = 1u;
    }

    void push_multi_edge(SectionTable &sections, int32_t section_id, int32_t face, uint16_t my_pi, uint16_t nei_pi,
                         int32_t add_area) {
        if (!section_id_in_range(sections, section_id))
            return;
        if (face < 0 || face >= 6)
            return;
        if (add_area <= 0)
            return;
        auto &multi = ensure_section_multi_state(sections, section_id);
        auto &edges = multi.edges_by_face[static_cast<size_t>(face)];
        for (auto &edge : edges) {
            if (edge.my_pocket == my_pi && edge.nei_pocket == nei_pi) {
                const int32_t merged = static_cast<int32_t>(edge.area) + add_area;
                edge.area = (merged > static_cast<int32_t>(std::numeric_limits<uint16_t>::max()))
                                    ? std::numeric_limits<uint16_t>::max()
                                    : static_cast<uint16_t>(merged);
                return;
            }
        }
        EdgeLink edge{};
        edge.my_pocket = my_pi;
        edge.nei_pocket = nei_pi;
        edge.area = (add_area > static_cast<int32_t>(std::numeric_limits<uint16_t>::max()))
                            ? std::numeric_limits<uint16_t>::max()
                            : static_cast<uint16_t>(add_area);
        edges.push_back(edge);
        const size_t n = edges.size();
        multi.edge_counts[static_cast<size_t>(face)] = (n > static_cast<size_t>(std::numeric_limits<uint16_t>::max()))
                                                               ? std::numeric_limits<uint16_t>::max()
                                                               : static_cast<uint16_t>(n);
    }

    void canonicalize_multi_face_links(SectionTable &sections, int32_t section_id, int32_t face) {
        if (!section_id_in_range(sections, section_id) || face < 0 || face >= 6)
            return;
        auto &edges = ensure_section_multi_state(sections, section_id).edges_by_face[static_cast<size_t>(face)];
        std::sort(edges.begin(), edges.end(), [](const EdgeLink &a, const EdgeLink &b) {
            if (a.my_pocket != b.my_pocket)
                return a.my_pocket < b.my_pocket;
            if (a.nei_pocket != b.nei_pocket)
                return a.nei_pocket < b.nei_pocket;
            return a.area < b.area;
        });
    }

    void clear_all_link_metadata(RadWorld &world) {
        SectionTable &sections = world.sections;
        const ChunkTable &chunks = world.chunks;
        for (size_t cidx = 0; cidx < chunks.loaded_by_id.size(); cidx++) {
            if (chunks.loaded_by_id[cidx] == 0u)
                continue;
            const int32_t *const section_ids = section_ids_of(chunks, static_cast<int32_t>(cidx));
            for (int32_t sy = 0; sy < sections_per_chunk(chunks); sy++) {
                const int32_t section_id = section_ids[static_cast<size_t>(sy)];
                if (!section_id_in_range(sections, section_id))
                    continue;
                const auto idx = static_cast<size_t>(section_id);
                sections.single_conn_counts[idx].fill(0u);
                if (auto *multi = section_multi_state_ptr(sections, section_id)) {
                    multi->edge_counts.fill(0u);
                    for (auto &face_edges : multi->edges_by_face)
                        face_edges.clear();
                    multi->clear_edge_streams();
                    multi->edge_offset.fill(0u);
                }
            }
        }
    }

    void finalize_multi_edge_storage_one(RadWorld &world, int32_t section_id) {
        SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto idx = static_cast<size_t>(section_id);
        auto *multi = section_multi_state_ptr(sections, section_id);
        if (sections.kind_by_id[idx] != kKindMulti || multi == nullptr) {
            if (multi != nullptr) {
                multi->clear_edge_streams();
                multi->edge_offset.fill(0u);
            }
            return;
        }

        size_t total = 0;
        auto &offsets = multi->edge_offset;
        for (int face = 0; face < 6; face++) {
            offsets[static_cast<size_t>(face)] = static_cast<uint32_t>(total);
            total += multi->edges_by_face[static_cast<size_t>(face)].size();
        }

        const auto &face_distance = multi->face_distance;
        const auto &inv_volume = multi->pocket_inv_volume;
        multi->clear_edge_streams();
        multi->edge_face_distance.reserve(total);
        multi->edge_inv_volume.reserve(total);
        multi->edge_pocket.reserve(total);
        multi->edge_nei_pocket.reserve(total);
        multi->edge_area.reserve(total);
        multi->edge_exchange_e.reserve(total);
        multi->edge_uni_share.reserve(total);
        multi->edge_pocket_share.reserve(total);
        multi->edge_diffusivity.reserve(total);
        for (int face = 0; face < 6; face++) {
            auto &edges = multi->edges_by_face[static_cast<size_t>(face)];
            for (const EdgeLink &edge : edges) {
                const size_t pocket = static_cast<size_t>(edge.my_pocket);
                const size_t distance_index = pocket * 6u + static_cast<size_t>(face);
                multi->edge_pocket.push_back(edge.my_pocket);
                multi->edge_nei_pocket.push_back(edge.nei_pocket);
                multi->edge_area.push_back(edge.area);
                const float dist = (distance_index < face_distance.size()) ? face_distance[distance_index] : 0.0f;
                multi->edge_face_distance.push_back(dist);
                multi->edge_diffusivity.push_back(
                        (pocket < multi->pocket_diffusivity.size()) ? multi->pocket_diffusivity[pocket] : 1.0f);
                const double inv_v = (pocket < inv_volume.size()) ? inv_volume[pocket] : 1.0;
                multi->edge_inv_volume.push_back(inv_v);

                const double dist_sum = 8.0 + static_cast<double>(dist);
                double exchange_e = -1.0;
                if (edge.nei_pocket == kSentinelPocket && dist_sum > 0.0 && world.diffusion_transport_enabled == 0u) {
                    exchange_e = transport_exp(-((static_cast<double>(edge.area) / dist_sum) *
                                                 (kUniformInvVolume + inv_v) * world.diffusion_dt));
                }
                multi->edge_exchange_e.push_back(exchange_e);

                double uni_share = 0.0, pocket_share = 0.0;
                if (exchange_e >= 0.0) {
                    const double inv_s = 1.0 / (kUniformInvVolume + inv_v);
                    const double one_minus_e = 1.0 - exchange_e;
                    uni_share = (kUniformInvVolume * one_minus_e) * inv_s;
                    pocket_share = (inv_v * one_minus_e) * inv_s;
                }
                multi->edge_uni_share.push_back(uni_share);
                multi->edge_pocket_share.push_back(pocket_share);
            }
        }
    }

    void finalize_multi_edge_storage(RadWorld &world) {
        const ChunkTable &chunks = world.chunks;
        const auto total_chunks = static_cast<int32_t>(chunks.loaded_by_id.size());
        const int32_t threshold = get_task_threshold(total_chunks, kSplitTaskGrain);
        parallel_split_range(world, 0, total_chunks, threshold, [&](int32_t lo, int32_t hi) {
            for (int32_t ci = lo; ci < hi; ci++) {
                if (chunks.loaded_by_id[static_cast<size_t>(ci)] == 0u)
                    continue;
                const int32_t *const section_ids = section_ids_of(chunks, ci);
                for (int32_t sy = 0; sy < sections_per_chunk(chunks); sy++) {
                    finalize_multi_edge_storage_one(world, section_ids[static_cast<size_t>(sy)]);
                }
            }
        });
    }

    void apply_section_diffusivity(RadWorld &world, int32_t section_id, int32_t value_count, const float *values) {
        SectionTable &sections = world.sections;
        if (!section_id_in_range(sections, section_id))
            return;
        const auto idx = static_cast<size_t>(section_id);
        if (values == nullptr)
            value_count = 0;
        const uint8_t kind = sections.kind_by_id[idx];

        if (sections.topology_dirty[idx] != 0u || kind == kKindNone) {
            if (value_count > 0) {
                auto &state = ensure_section_pending_state(sections, section_id);
                state.pocket_diffusivity.resize(static_cast<size_t>(value_count));
                for (int32_t i = 0; i < value_count; i++) {
                    state.pocket_diffusivity[static_cast<size_t>(i)] = clamp_diffusivity(values[i]);
                }
                return;
            }
            auto *pending = section_pending_state_ptr(sections, section_id);
            if (pending != nullptr && !pending->pocket_diffusivity.empty()) {
                pending->pocket_diffusivity.clear();
                maybe_release_section_pending_state(sections, section_id);
            }
            return;
        }

        const float first = (value_count > 0) ? clamp_diffusivity(values[0]) : 1.0f;
        if (kind == kKindUni) {
            sections.uniform_diffusivity[idx] = first;
            return;
        }
        if (kind == kKindSingle) {
            sections.single_diffusivity[idx] = first;
            return;
        }
        if (kind != kKindMulti)
            return;

        auto &multi = ensure_section_multi_state(sections, section_id);
        const auto pocket_count = std::max<size_t>(1u, sections.pocket_count[idx]);
        const bool complete = static_cast<size_t>(value_count) >= pocket_count;
        if (value_count > 0 && !complete) {

            world.diffusivity_pocket_mismatches++;
        }
        multi.pocket_diffusivity.assign(pocket_count, 1.0f);
        if (complete) {
            for (size_t i = 0; i < pocket_count; i++) {
                multi.pocket_diffusivity[i] = clamp_diffusivity(values[i]);
            }
        }
        sections.uniform_diffusivity[idx] = multi.pocket_diffusivity[0];
        sections.single_diffusivity[idx] = 1.0f;
        finalize_multi_edge_storage_one(world, section_id);
    }

    void clear_single_face_links(SectionTable &sections, int32_t section_id, int32_t face) {
        if (!section_id_in_range(sections, section_id) || face < 0 || face >= 6)
            return;
        sections.single_conn_counts[static_cast<size_t>(section_id)][static_cast<size_t>(face)] = 0u;
    }

    void clear_multi_face_links(SectionTable &sections, int32_t section_id, int32_t face) {
        if (!section_id_in_range(sections, section_id) || face < 0 || face >= 6)
            return;
        auto *multi = section_multi_state_ptr(sections, section_id);
        if (multi == nullptr)
            return;
        multi->edges_by_face[static_cast<size_t>(face)].clear();
        multi->edge_counts[static_cast<size_t>(face)] = 0u;
    }

    void fill_multi_face_sentinel(SectionTable &sections, int32_t section_id, int32_t face) {
        if (!section_id_in_range(sections, section_id) || face < 0 || face >= 6)
            return;
        const auto idx = static_cast<size_t>(section_id);
        if (sections.kind_by_id[idx] != kKindMulti) {
            clear_multi_face_links(sections, section_id, face);
            return;
        }

        auto &multi = ensure_section_multi_state(sections, section_id);
        auto &edges = multi.edges_by_face[static_cast<size_t>(face)];
        edges.clear();
        multi.edge_counts[static_cast<size_t>(face)] = 0u;
        const uint16_t pocket_count = sections.pocket_count[idx];
        if (pocket_count == 0u)
            return;

        const auto *const face_locals = kFaceLocalTable[static_cast<size_t>(face)].data();
        const auto *const pocket_by_local = section_pocket_map(sections, section_id).data();
        auto &counts = g_face_pocket_counts;
        if (counts.size() < static_cast<size_t>(pocket_count)) {
            counts.resize(static_cast<size_t>(pocket_count), 0u);
        }
        auto &touched = g_face_touched_pockets;
        touched.clear();
        for (size_t cell = 0; cell < kFaceCellCount; cell++) {
            const uint16_t pocket = pocket_by_local[face_locals[cell]];
            if (pocket == kNoPocket || pocket >= pocket_count)
                continue;
            auto &area = counts[static_cast<size_t>(pocket)];
            if (area == 0u) {
                touched.push_back(pocket);
            }
            area = static_cast<uint16_t>(area + 1u);
        }
        edges.reserve(touched.size());
        for (const uint16_t pocket : touched) {
            const auto area = counts[static_cast<size_t>(pocket)];
            EdgeLink edge{};
            edge.my_pocket = pocket;
            edge.nei_pocket = kSentinelPocket;
            edge.area = area;
            edges.push_back(edge);
            counts[static_cast<size_t>(pocket)] = 0u;
        }
        const size_t edge_count = edges.size();
        multi.edge_counts[static_cast<size_t>(face)] =
                (edge_count > static_cast<size_t>(std::numeric_limits<uint16_t>::max()))
                        ? std::numeric_limits<uint16_t>::max()
                        : static_cast<uint16_t>(edge_count);
    }

    void link_single_to_single(SectionTable &sections, int32_t section_a, int32_t face_a, int32_t section_b,
                               int32_t face_b) {
        if (!section_id_in_range(sections, section_a) || !section_id_in_range(sections, section_b))
            return;
        int32_t count = 0;
        const auto *const face_a_locals = kFaceLocalTable[static_cast<size_t>(face_a)].data();
        const auto *const face_b_locals = kFaceLocalTable[static_cast<size_t>(face_b)].data();
        const auto *const pocket_a_map = section_pocket_map(sections, section_a).data();
        const auto *const pocket_b_map = section_pocket_map(sections, section_b).data();
        for (size_t cell = 0; cell < kFaceCellCount; cell++) {
            const uint16_t pocket_a = pocket_a_map[face_a_locals[cell]];
            const uint16_t pocket_b = pocket_b_map[face_b_locals[cell]];
            if (pocket_a == kNoPocket || pocket_b == kNoPocket)
                continue;
            count++;
        }
        const auto shared_area = static_cast<uint16_t>(count);
        sections.single_conn_counts[static_cast<size_t>(section_a)][static_cast<size_t>(face_a)] = shared_area;
        sections.single_conn_counts[static_cast<size_t>(section_b)][static_cast<size_t>(face_b)] = shared_area;
    }

    void link_single_to_uniform(SectionTable &sections, int32_t section_single, int32_t face_single) {
        if (!section_id_in_range(sections, section_single) || face_single < 0 || face_single >= 6)
            return;
        const auto idx = static_cast<size_t>(section_single);
        const auto face = static_cast<size_t>(face_single);
        sections.single_conn_counts[idx][face] = sections.single_face_open_counts[idx][face];
    }

    void link_multi_to_single(SectionTable &sections, int32_t section_multi, int32_t face_multi, int32_t section_single,
                              int32_t face_single) {
        if (!section_id_in_range(sections, section_multi) || !section_id_in_range(sections, section_single))
            return;
        clear_single_face_links(sections, section_single, face_single);
        auto &multi = ensure_section_multi_state(sections, section_multi);
        auto &edges = multi.edges_by_face[static_cast<size_t>(face_multi)];
        edges.clear();
        multi.edge_counts[static_cast<size_t>(face_multi)] = 0u;

        int32_t single_count = 0;
        const auto *const face_multi_locals = kFaceLocalTable[static_cast<size_t>(face_multi)].data();
        const auto *const face_single_locals = kFaceLocalTable[static_cast<size_t>(face_single)].data();
        const auto *const pocket_multi_map = section_pocket_map(sections, section_multi).data();
        const auto *const pocket_single_map = section_pocket_map(sections, section_single).data();
        const auto multi_idx = static_cast<size_t>(section_multi);
        const uint16_t pocket_count = sections.pocket_count[multi_idx];
        auto &counts = g_face_pocket_counts;
        if (counts.size() < static_cast<size_t>(pocket_count)) {
            counts.resize(static_cast<size_t>(pocket_count), 0u);
        }
        auto &touched = g_face_touched_pockets;
        touched.clear();
        for (size_t cell = 0; cell < kFaceCellCount; cell++) {
            const uint16_t pocket_multi = pocket_multi_map[face_multi_locals[cell]];
            const uint16_t pocket_single = pocket_single_map[face_single_locals[cell]];
            if (pocket_multi == kNoPocket || pocket_single == kNoPocket)
                continue;
            auto &area = counts[static_cast<size_t>(pocket_multi)];
            if (area == 0u) {
                touched.push_back(pocket_multi);
            }
            area = static_cast<uint16_t>(area + 1u);
            single_count++;
        }
        edges.reserve(touched.size());
        for (const uint16_t pocket_multi : touched) {
            const auto area = counts[static_cast<size_t>(pocket_multi)];
            EdgeLink edge{};
            edge.my_pocket = pocket_multi;
            edge.nei_pocket = 0u;
            edge.area = area;
            edges.push_back(edge);
            counts[static_cast<size_t>(pocket_multi)] = 0u;
        }
        const size_t edge_count = edges.size();
        multi.edge_counts[static_cast<size_t>(face_multi)] =
                (edge_count > static_cast<size_t>(std::numeric_limits<uint16_t>::max()))
                        ? std::numeric_limits<uint16_t>::max()
                        : static_cast<uint16_t>(edge_count);
        sections.single_conn_counts[static_cast<size_t>(section_single)][static_cast<size_t>(face_single)] =
                static_cast<uint16_t>(single_count);
    }

    void link_multi_to_multi(SectionTable &sections, int32_t section_a, int32_t face_a, int32_t section_b,
                             int32_t face_b) {
        if (!section_id_in_range(sections, section_a) || !section_id_in_range(sections, section_b))
            return;
        clear_multi_face_links(sections, section_a, face_a);
        clear_multi_face_links(sections, section_b, face_b);

        constexpr int32_t kIndexSlots = 512;
        constexpr int32_t kIndexMask = kIndexSlots - 1;
        std::array<uint16_t, static_cast<size_t>(kIndexSlots)> index{};
        std::array<uint64_t, kFaceCellCount> pair_key{};
        std::array<uint16_t, kFaceCellCount> pair_count{};
        int32_t unique_count = 0;

        const auto *const face_a_locals = kFaceLocalTable[static_cast<size_t>(face_a)].data();
        const auto *const face_b_locals = kFaceLocalTable[static_cast<size_t>(face_b)].data();
        const auto *const pocket_a_map = section_pocket_map(sections, section_a).data();
        const auto *const pocket_b_map = section_pocket_map(sections, section_b).data();
        for (size_t cell = 0; cell < kFaceCellCount; cell++) {
            const uint16_t pocket_a = pocket_a_map[face_a_locals[cell]];
            const uint16_t pocket_b = pocket_b_map[face_b_locals[cell]];
            if (pocket_a == kNoPocket || pocket_b == kNoPocket)
                continue;

            const uint64_t key = (static_cast<uint64_t>(pocket_a) << 32u) | static_cast<uint64_t>(pocket_b);
            int32_t slot = static_cast<int32_t>((pocket_a * 31u) + pocket_b) & kIndexMask;
            while (true) {
                const uint16_t held = index[static_cast<size_t>(slot)];
                if (held == 0u) {
                    pair_key[static_cast<size_t>(unique_count)] = key;
                    pair_count[static_cast<size_t>(unique_count)] = 1u;
                    index[static_cast<size_t>(slot)] = static_cast<uint16_t>(unique_count + 1);
                    unique_count++;
                    break;
                }
                const auto at = static_cast<size_t>(held - 1);
                if (pair_key[at] == key) {
                    const uint16_t prev = pair_count[at];
                    pair_count[at] =
                            (prev == std::numeric_limits<uint16_t>::max()) ? prev : static_cast<uint16_t>(prev + 1u);
                    break;
                }
                slot = (slot + 1) & kIndexMask;
            }
        }

        if (unique_count == 0)
            return;

        auto &multi_a = ensure_section_multi_state(sections, section_a);
        auto &multi_b = ensure_section_multi_state(sections, section_b);
        auto &edges_a = multi_a.edges_by_face[static_cast<size_t>(face_a)];
        auto &edges_b = multi_b.edges_by_face[static_cast<size_t>(face_b)];
        edges_a.reserve(static_cast<size_t>(unique_count));
        edges_b.reserve(static_cast<size_t>(unique_count));

        auto emit_edge = [&](uint16_t pocket_a, uint16_t pocket_b, uint16_t area) {
            EdgeLink edge_a{};
            edge_a.my_pocket = pocket_a;
            edge_a.nei_pocket = pocket_b;
            edge_a.area = area;
            edges_a.push_back(edge_a);

            EdgeLink edge_b{};
            edge_b.my_pocket = pocket_b;
            edge_b.nei_pocket = pocket_a;
            edge_b.area = area;
            edges_b.push_back(edge_b);
        };

        for (int32_t i = 0; i < unique_count; i++) {
            const uint64_t key = pair_key[static_cast<size_t>(i)];
            emit_edge(static_cast<uint16_t>(key >> 32u), static_cast<uint16_t>(key & 0xFFFF'FFFFull),
                      pair_count[static_cast<size_t>(i)]);
        }

        multi_a.edge_counts[static_cast<size_t>(face_a)] = static_cast<uint16_t>(edges_a.size());
        multi_b.edge_counts[static_cast<size_t>(face_b)] = static_cast<uint16_t>(edges_b.size());
    }

    void link_non_uni_face(SectionTable &sections, int32_t section_a, uint8_t kind_a, int32_t face_a, int32_t section_b,
                           uint8_t kind_b) {
        const int32_t face_b = face_a ^ 1;
        if (!section_id_in_range(sections, section_a))
            return;

        if (kind_b == kKindUni) {
            if (kind_a == kKindSingle) {
                link_single_to_uniform(sections, section_a, face_a);
            } else if (kind_a == kKindMulti) {
                fill_multi_face_sentinel(sections, section_a, face_a);
            }
            return;
        }

        if (kind_b == kKindNone || !section_id_in_range(sections, section_b)) {
            if (kind_a == kKindSingle) {
                clear_single_face_links(sections, section_a, face_a);
            } else if (kind_a == kKindMulti) {
                clear_multi_face_links(sections, section_a, face_a);
            }
            return;
        }

        if (kind_a == kKindSingle) {
            if (kind_b == kKindSingle) {
                link_single_to_single(sections, section_a, face_a, section_b, face_b);
            } else if (kind_b == kKindMulti) {
                link_multi_to_single(sections, section_b, face_b, section_a, face_a);
            }
            return;
        }

        if (kind_a == kKindMulti) {
            if (kind_b == kKindSingle) {
                link_multi_to_single(sections, section_a, face_a, section_b, face_b);
            } else if (kind_b == kKindMulti) {
                link_multi_to_multi(sections, section_a, face_a, section_b, face_b);
            }
        }
    }

    void link_canonical_face(RadWorld &world, int32_t section_a, uint8_t kind_a, int32_t face_a, int32_t section_b,
                             uint8_t kind_b) {
        SectionTable &sections = world.sections;
        const int32_t face_b = face_a ^ 1;

        if (kind_a == kKindNone) {
            if (kind_b == kKindSingle && section_id_in_range(sections, section_b)) {
                clear_single_face_links(sections, section_b, face_b);
            } else if (kind_b == kKindMulti && section_id_in_range(sections, section_b)) {
                fill_multi_face_sentinel(sections, section_b, face_b);
            }
            return;
        }

        if (kind_a == kKindUni) {
            if (kind_b == kKindSingle && section_id_in_range(sections, section_b)) {
                link_single_to_uniform(sections, section_b, face_b);
            } else if (kind_b == kKindMulti && section_id_in_range(sections, section_b)) {
                fill_multi_face_sentinel(sections, section_b, face_b);
            }
            return;
        }

        link_non_uni_face(sections, section_a, kind_a, face_a, section_b, kind_b);
    }

    void link_section_canonical_faces(RadWorld &world, int32_t chunk_id, int32_t sy, uint8_t face_mask) {
        ChunkTable &chunks = world.chunks;
        SectionTable &sections = world.sections;
        if (face_mask == 0u)
            return;
        if (!is_loaded_chunk_id(chunks, chunk_id))
            return;
        const int32_t section_a = section_id_for_chunk_sy(chunks, chunk_id, sy);
        if (!section_id_in_range(sections, section_a))
            return;
        const uint8_t kind_a = sections.kind_by_id[static_cast<size_t>(section_a)];
        const auto chunk_idx = static_cast<size_t>(chunk_id);

        const auto kind_of = [&](int32_t section) {
            return section_id_in_range(sections, section) ? sections.kind_by_id[static_cast<size_t>(section)]
                                                          : kKindNone;
        };

        if ((face_mask & kRelinkFaceEast) != 0u) {
            const int32_t section_e = section_id_for_chunk_sy(chunks, chunks.east_neighbor_id[chunk_idx], sy);
            link_canonical_face(world, section_a, kind_a, 5, section_e, kind_of(section_e));
        }
        if ((face_mask & kRelinkFaceSouth) != 0u) {
            const int32_t section_s = section_id_for_chunk_sy(chunks, chunks.south_neighbor_id[chunk_idx], sy);
            link_canonical_face(world, section_a, kind_a, 3, section_s, kind_of(section_s));
        }
        if ((face_mask & kRelinkFaceUp) != 0u && sy < sections_per_chunk(chunks) - 1) {
            const int32_t section_u = section_id_for_chunk_sy(chunks, chunk_id, sy + 1);
            link_canonical_face(world, section_a, kind_a, 1, section_u, kind_of(section_u));
        }
    }

    void rebuild_face_links_range(RadWorld &world, int32_t start, int32_t end) {
        const ChunkTable &chunks = world.chunks;
        for (int32_t chunk_id = start; chunk_id < end; chunk_id++) {
            if (!is_loaded_chunk_id(chunks, chunk_id))
                continue;
            for (int32_t sy = 0; sy < sections_per_chunk(chunks); sy++) {
                link_section_canonical_faces(world, chunk_id, sy, kRelinkFaceAll);
            }
        }
    }

    void relink_note_chunk_unloading(RadWorld &world, int32_t chunk_id) {
        ChunkTable &chunks = world.chunks;
        SectionTable &sections = world.sections;
        if (!chunk_id_in_range(chunks, chunk_id))
            return;
        const int64_t ck = chunks.ck_by_id[static_cast<size_t>(chunk_id)];
        if (ck == kInvalidChunkKey)
            return;
        const ChunkCoord coord = decode_chunk_key(ck);
        const int32_t west = find_loaded_chunk_id(chunks, encode_chunk_key(coord.x - 1, coord.z));
        const int32_t north = find_loaded_chunk_id(chunks, encode_chunk_key(coord.x, coord.z - 1));
        const int32_t east = chunks.east_neighbor_id[static_cast<size_t>(chunk_id)];
        const int32_t south = chunks.south_neighbor_id[static_cast<size_t>(chunk_id)];

        for (int32_t sy = 0; sy < chunks.sections_per_chunk; sy++) {
            mark_relink_slot(world, west, sy, static_cast<uint8_t>(kRelinkFaceEast | kRelinkRepack));
            mark_relink_slot(world, north, sy, static_cast<uint8_t>(kRelinkFaceSouth | kRelinkRepack));

            const int32_t far_east = section_id_for_chunk_sy(chunks, east, sy);
            if (section_id_in_range(sections, far_east)) {
                const uint8_t kind = sections.kind_by_id[static_cast<size_t>(far_east)];
                if (kind == kKindSingle)
                    clear_single_face_links(sections, far_east, 4);
                else if (kind == kKindMulti)
                    clear_multi_face_links(sections, far_east, 4);
                mark_relink_slot(world, east, sy, kRelinkRepack);
            }
            const int32_t far_south = section_id_for_chunk_sy(chunks, south, sy);
            if (section_id_in_range(sections, far_south)) {
                const uint8_t kind = sections.kind_by_id[static_cast<size_t>(far_south)];
                if (kind == kKindSingle)
                    clear_single_face_links(sections, far_south, 2);
                else if (kind == kKindMulti)
                    clear_multi_face_links(sections, far_south, 2);
                mark_relink_slot(world, south, sy, kRelinkRepack);
            }
        }
    }

    void rebuild_face_links_full(RadWorld &world) {
        clear_all_link_metadata(world);
        const auto chunk_count = static_cast<int32_t>(world.chunks.ck_by_id.size());
        const int32_t threshold = get_task_threshold(chunk_count, kSplitTaskGrain);
        parallel_split_range(world, 0, chunk_count, threshold,
                             [&](int32_t lo, int32_t hi) { rebuild_face_links_range(world, lo, hi); });
        finalize_multi_edge_storage(world);
    }

    void rebuild_face_links_incremental(RadWorld &world) {
        const ChunkTable &chunks = world.chunks;
        const int32_t spc = chunks.sections_per_chunk;
        const auto count = static_cast<int32_t>(world.relink_slots.size());
        const int32_t threshold = get_task_threshold(count, kMinTaskGrain);

        parallel_split_range(world, 0, count, threshold, [&](int32_t lo, int32_t hi) {
            for (int32_t i = lo; i < hi; i++) {
                const int32_t slot = world.relink_slots[static_cast<size_t>(i)];
                const uint8_t flags = world.relink_flags[static_cast<size_t>(slot)];
                if ((flags & kRelinkFaceAll) == 0u)
                    continue;
                link_section_canonical_faces(world, slot / spc, slot % spc,
                                             static_cast<uint8_t>(flags & kRelinkFaceAll));
            }
        });

        parallel_split_range(world, 0, count, threshold, [&](int32_t lo, int32_t hi) {
            for (int32_t i = lo; i < hi; i++) {
                const int32_t slot = world.relink_slots[static_cast<size_t>(i)];
                if ((world.relink_flags[static_cast<size_t>(slot)] & kRelinkRepack) == 0u)
                    continue;
                finalize_multi_edge_storage_one(world, section_id_for_chunk_sy(chunks, slot / spc, slot % spc));
            }
        });
    }

    void rebuild_face_links(RadWorld &world) {
        const auto slots = static_cast<uint32_t>(world.relink_slots.size());
        world.last_step_relink_slots = slots;

        const auto loaded_sections =
                world.chunks.ck_by_id.size() * static_cast<size_t>(world.chunks.sections_per_chunk);
        const bool full = world.links_full_dirty != 0u || world.relink_slots.size() * 8u > loaded_sections;
        if (full) {
            world.last_step_relink_slots = 0u;
            rebuild_face_links_full(world);
        } else {
            rebuild_face_links_incremental(world);
        }

        for (const int32_t slot : world.relink_slots)
            world.relink_flags[static_cast<size_t>(slot)] = 0u;
        world.relink_slots.clear();
        world.links_full_dirty = 0u;
        world.links_dirty = 0u;
    }

    void rebuild_pair_bucket(RadWorld &world, int32_t bucket) {
        const auto bi = static_cast<size_t>(bucket);
        const ChunkTable &chunks = world.chunks;
        auto &xA = world.x_pair_a_by_bucket[bi];
        auto &xB = world.x_pair_b_by_bucket[bi];
        auto &zA = world.z_pair_a_by_bucket[bi];
        auto &zB = world.z_pair_b_by_bucket[bi];
        xA.clear();
        xB.clear();
        zA.clear();
        zB.clear();

        const auto &ids = chunks.parity_bucket_ids[bi];
        xA.reserve(ids.size());
        xB.reserve(ids.size());
        zA.reserve(ids.size());
        zB.reserve(ids.size());

        for (const int32_t a_id : ids) {
            if (!is_loaded_chunk_id(chunks, a_id))
                continue;

            const int32_t east_id = chunks.east_neighbor_id[static_cast<size_t>(a_id)];
            if (is_loaded_chunk_id(chunks, east_id)) {
                xA.push_back(a_id);
                xB.push_back(east_id);
            }

            const int32_t south_id = chunks.south_neighbor_id[static_cast<size_t>(a_id)];
            if (is_loaded_chunk_id(chunks, south_id)) {
                zA.push_back(a_id);
                zB.push_back(south_id);
            }
        }

        world.x_pair_counts[bi] = static_cast<int32_t>(xA.size());
        world.z_pair_counts[bi] = static_cast<int32_t>(zA.size());
    }

    void rebuild_axis_runs(RadWorld &world, bool by_row) {
        const ChunkTable &chunks = world.chunks;
        const auto &src_a = by_row ? world.x_pair_a_by_bucket : world.z_pair_a_by_bucket;
        const auto &src_b = by_row ? world.x_pair_b_by_bucket : world.z_pair_b_by_bucket;
        RadWorld::AxisRuns &runs = by_row ? world.x_runs : world.z_runs;

        struct Keyed {
            int64_t key;
            int32_t a;
            int32_t b;
        };
        std::vector<Keyed> keyed;
        size_t total = 0;
        for (size_t bi = 0; bi < 4u; bi++)
            total += src_a[bi].size();
        keyed.reserve(total);

        for (size_t bi = 0; bi < 4u; bi++) {
            const auto &va = src_a[bi];
            const auto &vb = src_b[bi];
            const size_t n = std::min(va.size(), vb.size());
            for (size_t i = 0; i < n; i++) {
                const int32_t a_id = va[i];
                if (!chunk_id_in_range(chunks, a_id))
                    continue;
                const int64_t ck = chunks.ck_by_id[static_cast<size_t>(a_id)];
                if (ck == kInvalidChunkKey)
                    continue;
                const ChunkCoord coord = decode_chunk_key(ck);
                const int64_t run = by_row ? coord.z : coord.x;
                const int64_t colour = (by_row ? coord.x : coord.z) & 1;
                keyed.push_back(Keyed{(run << 1) | colour, a_id, vb[i]});
            }
        }
        std::sort(keyed.begin(), keyed.end(), [](const Keyed &l, const Keyed &r) { return l.key < r.key; });

        runs.a.clear();
        runs.b.clear();
        runs.start.clear();
        runs.mid.clear();
        runs.end.clear();
        runs.a.reserve(keyed.size());
        runs.b.reserve(keyed.size());
        for (size_t i = 0; i < keyed.size();) {
            const int64_t run = keyed[i].key >> 1;
            const auto start = static_cast<int32_t>(runs.a.size());
            int32_t mid = -1;
            size_t j = i;
            for (; j < keyed.size() && (keyed[j].key >> 1) == run; j++) {
                if (mid < 0 && (keyed[j].key & 1) != 0)
                    mid = static_cast<int32_t>(runs.a.size());
                runs.a.push_back(keyed[j].a);
                runs.b.push_back(keyed[j].b);
            }
            const auto end = static_cast<int32_t>(runs.a.size());
            runs.start.push_back(start);
            runs.mid.push_back(mid < 0 ? end : mid);
            runs.end.push_back(end);
            i = j;
        }
    }

    void rebuild_pair_lists_if_needed(RadWorld &world) {
        if (world.pair_lists_dirty == 0u)
            return;
        maybe_parallel_invoke(
                world, [&] { rebuild_pair_bucket(world, 0); }, [&] { rebuild_pair_bucket(world, 1); },
                [&] { rebuild_pair_bucket(world, 2); }, [&] { rebuild_pair_bucket(world, 3); });
        maybe_parallel_invoke(world, [&] { rebuild_axis_runs(world, true); }, [&] { rebuild_axis_runs(world, false); });
        world.pair_lists_dirty = 0u;
    }
}
