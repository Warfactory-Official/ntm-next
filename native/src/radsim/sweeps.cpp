// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include <thread>
#include "radsim/internal.hpp"
#include "radsim/sweeps_simd.hpp"

#include <cassert>
#include <span>

#if HBM_RADSIM_X86_SIMD
#include <immintrin.h>
#endif

namespace hbm::radsim::detail {

    bool exchange_uni_exact(double &a, double &b, double uniform_exchange, double diffusion_dt, double diff_a,
                            double diff_b, bool use_diff_transport) {
        if (!use_diff_transport)
            return exchange_uni_exact_pair(a, b, uniform_exchange);
        return exchange_uni_exact_pair_diffusive(a, b, uniform_exchange, diffusion_dt, diff_a, diff_b);
    }

    double single_inv_volume(const SectionTable &sections, int32_t section_id) {
        if (!section_id_in_range(sections, section_id))
            return 1.0;
        return sections.single_inv_volume[static_cast<size_t>(section_id)];
    }

    double single_face_dist(const SectionTable &sections, int32_t section_id, int32_t face) {
        if (!section_id_in_range(sections, section_id) || face < 0 || face >= 6)
            return 0.0;
        return static_cast<double>(
                sections.single_face_distance[static_cast<size_t>(section_id)][static_cast<size_t>(face)]);
    }

    double multi_face_dist(const SectionTable &sections, int32_t section_id, uint16_t pocket, int32_t face) {
        if (!section_id_in_range(sections, section_id) || face < 0 || face >= 6)
            return 0.0;
        const auto &face_dist = section_multi_state(sections, section_id).face_distance;
        const size_t idx = static_cast<size_t>(pocket) * 6u + static_cast<size_t>(face);
        if (idx >= face_dist.size())
            return 0.0;
        return static_cast<double>(face_dist[idx]);
    }

    double multi_inv_volume(const SectionTable &sections, int32_t section_id, uint16_t pocket) {
        if (!section_id_in_range(sections, section_id))
            return 1.0;
        const auto &inv_volume = section_multi_state(sections, section_id).pocket_inv_volume;
        const size_t idx = static_cast<size_t>(pocket);
        if (idx >= inv_volume.size())
            return 1.0;
        return inv_volume[idx];
    }

    bool exchange_single_single(RadWorld &world, int32_t section_a, int32_t face_a, int32_t section_b, int32_t face_b) {
        SectionTable &sections = world.sections;
        const auto idx_a = static_cast<size_t>(section_a);
        const auto idx_b = static_cast<size_t>(section_b);
        const auto face_a_idx = static_cast<size_t>(face_a);
        const auto face_b_idx = static_cast<size_t>(face_b);
        const int32_t area = static_cast<int32_t>(sections.single_conn_counts[idx_a][face_a_idx]);
        if (area <= 0)
            return false;

        double &ra = sections.uniform_density[idx_a];
        double &rb = sections.uniform_density[idx_b];
        if (ra == rb)
            return false;

        const double inv_va = sections.single_inv_volume[idx_a];
        const double inv_vb = sections.single_inv_volume[idx_b];
        const double dist_a = sections.single_face_distance[idx_a][face_a_idx];
        const double dist_b = sections.single_face_distance[idx_b][face_b_idx];
        const double dist_sum = java_strict_add(dist_a, dist_b);
        if (dist_sum <= 0.0)
            return false;

        double diffusion_dt = world.diffusion_dt;
        if (world.diffusion_transport_enabled != 0u) {
            const double d_eff = edge_diffusivity(dist_a, sections.single_diffusivity[idx_a], dist_b,
                                                  sections.single_diffusivity[idx_b]);
            if (!(d_eff > 0.0))
                return false;
            diffusion_dt = java_strict_mul(diffusion_dt, d_eff);
        }
        exchange_exact_pair(ra, rb, inv_va, inv_vb, area, dist_sum, diffusion_dt);
        mark_section_active_during_sweep(world, section_a);
        mark_section_active_during_sweep(world, section_b);
        mark_section_owner_dirty(world, section_a);
        mark_section_owner_dirty(world, section_b);
        return true;
    }

    bool exchange_single_uniform(RadWorld &world, int32_t uniform_section, int32_t single_section,
                                 int32_t face_single) {
        SectionTable &sections = world.sections;
        const auto single_idx = static_cast<size_t>(single_section);
        const auto face_idx = static_cast<size_t>(face_single);
        const int32_t area = static_cast<int32_t>(sections.single_face_open_counts[single_idx][face_idx]);
        if (area <= 0) [[unlikely]]
            return false;

        double &ra = sections.uniform_density[static_cast<size_t>(uniform_section)];
        double &rb = sections.uniform_density[single_idx];
        if (ra == rb)
            return false;

        const double dist_a = 8.0;
        const double dist_b = sections.single_face_distance[single_idx][face_idx];
        const double dist_sum = java_strict_add(dist_a, dist_b);
        if (dist_sum <= 0.0) [[unlikely]]
            return false;

        const double inv_vb = sections.single_inv_volume[single_idx];
        double diffusion_dt = world.diffusion_dt;
        if (world.diffusion_transport_enabled != 0u) {
            const double d_eff =
                    edge_diffusivity(dist_a, sections.uniform_diffusivity[static_cast<size_t>(uniform_section)], dist_b,
                                     sections.single_diffusivity[single_idx]);
            if (!(d_eff > 0.0))
                return false;
            diffusion_dt = java_strict_mul(diffusion_dt, d_eff);
        }
        exchange_exact_pair(ra, rb, kUniformInvVolume, inv_vb, area, dist_sum, diffusion_dt);
        mark_section_active_during_sweep(world, uniform_section);
        mark_section_active_during_sweep(world, single_section);
        mark_section_owner_dirty(world, uniform_section);
        mark_section_owner_dirty(world, single_section);
        return true;
    }

    bool exchange_single_multi(RadWorld &world, int32_t single_section, int32_t face_single, int32_t multi_section,
                               int32_t face_multi) {
        SectionTable &sections = world.sections;
        auto edges = multi_face_cached_edges(sections, multi_section, face_multi);
        if (edges.empty())
            return false;
        auto *multi_state = section_multi_state_ptr(sections, multi_section);
        if (multi_state == nullptr)
            return false;

        bool changed = false;
        const auto single_idx = static_cast<size_t>(single_section);
        const auto face_idx = static_cast<size_t>(face_single);
        double ra = sections.uniform_density[single_idx];
        auto &multi_density = multi_state->pocket_density;
        double *__restrict__ density_ptr = multi_density.data();
        const double inv_va = sections.single_inv_volume[single_idx];
        const size_t edge_count = edges.size();
        const double single_face = sections.single_face_distance[single_idx][face_idx];
        const double single_diff = sections.single_diffusivity[single_idx];
        const size_t density_count = multi_density.size();
        for (size_t i = 0; i < edge_count; i++) {
            const size_t idx_b = static_cast<size_t>(edges.pocket[i]);
            if (idx_b >= density_count)
                continue;
            const double rb = density_ptr[idx_b];
            if (ra == rb)
                continue;

            const double inv_vb = edges.inv_volume[i];
            const double denom_inv = inv_va + inv_vb;
            const double dist_sum = single_face + static_cast<double>(edges.face_distance[i]);
            if (dist_sum <= 0.0)
                continue;

            double diffusion_dt = world.diffusion_dt;
            if (world.diffusion_transport_enabled != 0u) {
                const double d_eff = edge_diffusivity(
                        single_face, single_diff, static_cast<double>(edges.face_distance[i]), edges.diffusivity_at(i));
                if (!(d_eff > 0.0))
                    continue;
                diffusion_dt *= d_eff;
            }
            const double e =
                    transport_exp(-((static_cast<double>(edges.area[i]) / dist_sum) * denom_inv * diffusion_dt));
            const double r_star = (ra * inv_vb + rb * inv_va) / denom_inv;
            ra = r_star + (ra - r_star) * e;
            double rb_after = r_star + (rb - r_star) * e;
            density_ptr[idx_b] = rb_after;
            changed = true;
        }

        if (changed) {
            sections.uniform_density[static_cast<size_t>(single_section)] = ra;
            mark_section_active_during_sweep(world, single_section);
            mark_section_active_during_sweep(world, multi_section);
            mark_section_owner_dirty(world, single_section);
            mark_section_owner_dirty(world, multi_section);
        }
        return changed;
    }

    bool exchange_multi_uniform(RadWorld &world, int32_t multi_section, int32_t face_multi, int32_t uniform_section) {
        SectionTable &sections = world.sections;
        auto *multi_state = section_multi_state_ptr(sections, multi_section);
        if (multi_state == nullptr)
            return false;

        const bool use_cached = (world.bypass_coefficient_cache == 0u) && world.diffusion_transport_enabled == 0u;
        auto edges = multi_face_cached_edges(sections, multi_section, face_multi);
        if (edges.empty()) [[unlikely]]
            return false;

        bool changed = false;
        double ra = sections.uniform_density[static_cast<size_t>(uniform_section)];
        auto &multi_density = multi_state->pocket_density;
        double *__restrict__ density_ptr = multi_density.data();
        const size_t density_count = multi_density.size();
        const size_t edge_count = edges.size();
        const double uniform_diff = sections.uniform_diffusivity[static_cast<size_t>(uniform_section)];

        if (!use_cached || edges.uni_share == nullptr) {
            alignas(64) double derived_uni[kFaceCellCount];
            alignas(64) double derived_pocket[kFaceCellCount];
            if (edge_count > kFaceCellCount)
                return false;
            for (size_t i = 0; i < edge_count; i++) {
                const double inv_vb = edges.inv_volume[i];
                const double face_dist = static_cast<double>(edges.face_distance[i]);
                const double dist_sum = 8.0 + face_dist;
                double diffusion_dt = world.diffusion_dt;
                bool open = dist_sum > 0.0;

                if (open && world.diffusion_transport_enabled != 0u) {
                    const double d_eff = edge_diffusivity(8.0, uniform_diff, face_dist, edges.diffusivity_at(i));
                    if (d_eff > 0.0)
                        diffusion_dt *= d_eff;
                    else
                        open = false;
                }
                double uni_share = 0.0;
                double pocket_share = 0.0;
                if (open) {
                    const double e = transport_exp(-((static_cast<double>(edges.area[i]) / dist_sum) *
                                                     (kUniformInvVolume + inv_vb) * diffusion_dt));
                    const double inv_s = 1.0 / (kUniformInvVolume + inv_vb);
                    const double one_minus_e = 1.0 - e;
                    uni_share = (kUniformInvVolume * one_minus_e) * inv_s;
                    pocket_share = (inv_vb * one_minus_e) * inv_s;
                }
                derived_uni[i] = uni_share;
                derived_pocket[i] = pocket_share;
            }
            const double ra_before_face = ra;
            ra = exchange_uni_multi_fused(ra, edges.pocket, derived_uni, derived_pocket, density_ptr, edge_count,
                                          density_count);
            changed = (ra != ra_before_face);
            if (changed) {
                sections.uniform_density[static_cast<size_t>(uniform_section)] = ra;
                mark_section_active_during_sweep(world, uniform_section);
                mark_section_active_during_sweep(world, multi_section);
                mark_section_owner_dirty(world, uniform_section);
                mark_section_owner_dirty(world, multi_section);
            }
            return changed;
        }
        assert(edges.uni_share != nullptr && edges.pocket_share != nullptr);
        {
            const double ra_before_face = ra;
            const bool force_twin = (world.column_path_flags & kMultiUniformForceScalar) != 0u;
            ra = force_twin ? exchange_uni_multi_scalar(ra, edges.pocket, edges.uni_share, edges.pocket_share,
                                                        density_ptr, edge_count, density_count)
                            : exchange_uni_multi_fused(ra, edges.pocket, edges.uni_share, edges.pocket_share,
                                                       density_ptr, edge_count, density_count);
            changed = (ra != ra_before_face);
            if (changed) {
                sections.uniform_density[static_cast<size_t>(uniform_section)] = ra;
                mark_section_active_during_sweep(world, uniform_section);
                mark_section_active_during_sweep(world, multi_section);
                mark_section_owner_dirty(world, uniform_section);
                mark_section_owner_dirty(world, multi_section);
            }
            return changed;
        }

        if (changed) {
            sections.uniform_density[static_cast<size_t>(uniform_section)] = ra;
            mark_section_active_during_sweep(world, uniform_section);
            mark_section_active_during_sweep(world, multi_section);
            mark_section_owner_dirty(world, uniform_section);
            mark_section_owner_dirty(world, multi_section);
        }
        return changed;
    }

    bool exchange_multi_multi(RadWorld &world, int32_t section_a, int32_t face_a, int32_t section_b, int32_t face_b) {
        SectionTable &sections = world.sections;
        auto edges = multi_face_cached_edges(sections, section_a, face_a);
        if (edges.empty())
            return false;
        auto *multi_a = section_multi_state_ptr(sections, section_a);
        auto *multi_b = section_multi_state_ptr(sections, section_b);
        if (multi_a == nullptr || multi_b == nullptr)
            return false;

        bool changed = false;
        auto &density_a = multi_a->pocket_density;
        auto &density_b = multi_b->pocket_density;
        const auto &inv_volume_b = multi_b->pocket_inv_volume;
        const auto &face_distance_b = multi_b->face_distance;
        double *__restrict__ density_a_ptr = density_a.data();
        double *__restrict__ density_b_ptr = density_b.data();
        const size_t density_a_count = density_a.size();
        const size_t density_b_count = density_b.size();
        const double *__restrict__ inv_volume_b_ptr = inv_volume_b.data();
        const float *__restrict__ face_b_ptr = face_distance_b.data();
        const float *__restrict__ diff_b_ptr = multi_b->pocket_diffusivity.data();
        const size_t edge_count = edges.size();
        [[maybe_unused]] const auto face_a_idx = static_cast<size_t>(face_a);
        const auto face_b_idx = static_cast<size_t>(face_b);
        for (size_t i = 0; i < edge_count;) {
            const size_t idx_a = static_cast<size_t>(edges.pocket[i]);
            const size_t idx_b = static_cast<size_t>(edges.nei_pocket[i]);
            if (idx_a >= density_a_count || idx_b >= density_b_count) {
                i++;
                continue;
            }

            const double ra_before = density_a_ptr[idx_a];
            const double rb_before = density_b_ptr[idx_b];
            if (ra_before == rb_before) {
                i++;
                continue;
            }

            const double inv_va = edges.inv_volume[i];
            const double inv_vb = inv_volume_b_ptr[idx_b];
            const double dist_a = static_cast<double>(edges.face_distance[i]);
            const double dist_b = static_cast<double>(face_b_ptr[idx_b * 6u + face_b_idx]);
            const double dist_sum = dist_a + dist_b;
            if (dist_sum <= 0.0) {
                i++;
                continue;
            }

            const double denom_inv = inv_va + inv_vb;
            double diffusion_dt = world.diffusion_dt;
            if (world.diffusion_transport_enabled != 0u) {
                const double diff_a = edges.diffusivity_at(i);
                const double diff_b =
                        (idx_b < multi_b->pocket_diffusivity.size()) ? static_cast<double>(diff_b_ptr[idx_b]) : 1.0;
                const double d_eff = edge_diffusivity(dist_a, diff_a, dist_b, diff_b);
                if (!(d_eff > 0.0)) {
                    i++;
                    continue;
                }
                diffusion_dt *= d_eff;
            }

            assert(i + 1u >= edge_count || edges.pocket[i + 1u] != edges.pocket[i] ||
                   edges.nei_pocket[i + 1u] != edges.nei_pocket[i]);
            const double e =
                    transport_exp(-((static_cast<double>(edges.area[i]) / dist_sum) * denom_inv * diffusion_dt));
            const double r_star = (ra_before * inv_vb + rb_before * inv_va) / denom_inv;
            double ra_after = r_star + (ra_before - r_star) * e;
            double rb_after = r_star + (rb_before - r_star) * e;
            density_a_ptr[idx_a] = ra_after;
            density_b_ptr[idx_b] = rb_after;
            changed = true;
            i++;
        }

        if (changed) {
            mark_section_active_during_sweep(world, section_a);
            mark_section_active_during_sweep(world, section_b);
            mark_section_owner_dirty(world, section_a);
            mark_section_owner_dirty(world, section_b);
        }
        return changed;
    }

    bool exchange_face_exact(RadWorld &world, int32_t section_a, uint8_t kind_a, int32_t face_a, int32_t section_b,
                             uint8_t kind_b, int32_t face_b) {
        SectionTable &sections = world.sections;
        if (kind_a == kKindNone || kind_b == kKindNone)
            return false;
        if (!section_id_in_range(sections, section_a) || !section_id_in_range(sections, section_b))
            return false;
        switch (static_cast<uint8_t>((kind_a << 2u) | kind_b)) {
        case static_cast<uint8_t>((kKindUni << 2u) | kKindUni): {
            double &ra = sections.uniform_density[static_cast<size_t>(section_a)];
            double &rb = sections.uniform_density[static_cast<size_t>(section_b)];
            const double diff_a = sections.uniform_diffusivity[static_cast<size_t>(section_a)];
            const double diff_b = sections.uniform_diffusivity[static_cast<size_t>(section_b)];
            const bool use_diff_transport = world.diffusion_transport_enabled != 0u;
            const bool changed = exchange_uni_exact(ra, rb, world.uniform_exchange, world.diffusion_dt, diff_a, diff_b,
                                                    use_diff_transport);
            if (changed) {
                mark_section_active_during_sweep(world, section_a);
                mark_section_active_during_sweep(world, section_b);
                mark_section_owner_dirty(world, section_a);
                mark_section_owner_dirty(world, section_b);
            }
            return changed;
        }
        case static_cast<uint8_t>((kKindUni << 2u) | kKindSingle):
            return exchange_single_uniform(world, section_a, section_b, face_b);
        case static_cast<uint8_t>((kKindUni << 2u) | kKindMulti):
            return exchange_multi_uniform(world, section_b, face_b, section_a);
        case static_cast<uint8_t>((kKindSingle << 2u) | kKindUni):
            return exchange_single_uniform(world, section_b, section_a, face_a);
        case static_cast<uint8_t>((kKindMulti << 2u) | kKindUni):
            return exchange_multi_uniform(world, section_a, face_a, section_b);
        case static_cast<uint8_t>((kKindSingle << 2u) | kKindSingle):
            return exchange_single_single(world, section_a, face_a, section_b, face_b);
        case static_cast<uint8_t>((kKindSingle << 2u) | kKindMulti):
            return exchange_single_multi(world, section_a, face_a, section_b, face_b);
        case static_cast<uint8_t>((kKindMulti << 2u) | kKindSingle):
            return exchange_single_multi(world, section_b, face_b, section_a, face_a);
        case static_cast<uint8_t>((kKindMulti << 2u) | kKindMulti):
            return exchange_multi_multi(world, section_a, face_a, section_b, face_b);
        default:
            return false;
        }
    }

    inline uint32_t exchange_uni_y(double *d, int32_t off, int32_t n, double uniform_exchange, uint32_t lanes,
                                   bool force_scalar, bool force_256, bool force_avx) noexcept {
#if HBM_RADSIM_X86_SIMD
        if (!force_scalar && !force_256 && !force_avx && n - off >= 8 && exchange_uni_column_use_avx512()) {
            return exchange_uni_y_avx512(d, off, n, uniform_exchange, lanes);
        }
        if (!force_scalar && !force_avx && n - off >= 4 && exchange_uni_column_use_avx2()) {
            return exchange_uni_y_avx2(d, off, n, uniform_exchange, lanes);
        }
        if (!force_scalar && n - off >= 4 && exchange_uni_column_use_avx()) {
            return exchange_uni_y_avx(d, off, n, uniform_exchange, lanes);
        }
#else
        (void)force_scalar;
        (void)force_256;
        (void)force_avx;
#endif
        return exchange_uni_y_scalar(d, off, n, uniform_exchange, lanes);
    }

    inline uint32_t exchange_uni_column(double *ra, double *rb, int32_t n, double uniform_exchange, uint32_t lanes,
                                        bool force_scalar, bool force_256, bool force_avx) noexcept {
#if HBM_RADSIM_X86_SIMD

        if (!force_scalar && !force_256 && !force_avx && n >= 8 && exchange_uni_column_use_avx512()) {
            return exchange_uni_column_avx512(ra, rb, n, uniform_exchange, lanes);
        }
        if (!force_scalar && !force_avx && n >= 4 && exchange_uni_column_use_avx2()) {
            return exchange_uni_column_avx2(ra, rb, n, uniform_exchange, lanes);
        }
        if (!force_scalar && n >= 4 && exchange_uni_column_use_avx()) {
            return exchange_uni_column_avx(ra, rb, n, uniform_exchange, lanes);
        }
#else
        (void)force_scalar;
        (void)force_256;
        (void)force_avx;
#endif
        return exchange_uni_column_scalar(ra, rb, 0, n, uniform_exchange, lanes);
    }

    template <bool Wide>
    void run_xz_pairs_range_impl(RadWorld &world, const PairSpan &pairs, int32_t face_a, int32_t face_b, int32_t start,
                                 int32_t end) {
        ChunkTable &chunks = world.chunks;
        SectionTable &sections = world.sections;
        if (!(world.diffusion_dt > 0.0) || !std::isfinite(world.diffusion_dt))
            return;
        const int32_t pair_count = pairs.count();
        const int32_t hi = std::min(end, pair_count);
        const int32_t *__restrict__ pair_a_ptr = pairs.a.data();
        const int32_t *__restrict__ pair_b_ptr = pairs.b.data();
        const int32_t n = sections_per_chunk(chunks);
        for (int32_t i = start; i < hi; i++) {
            const int32_t a_id = pair_a_ptr[i];
            const int32_t b_id = pair_b_ptr[i];
            if (!is_loaded_chunk_id(chunks, a_id) || !is_loaded_chunk_id(chunks, b_id))
                continue;

            const size_t metadata_a =
                    Wide ? static_cast<size_t>(a_id) * chunks.words_per_chunk : static_cast<size_t>(a_id);
            const size_t metadata_b =
                    Wide ? static_cast<size_t>(b_id) * chunks.words_per_chunk : static_cast<size_t>(b_id);
            const int32_t word_count = Wide ? chunks.words_per_chunk : 1;
            for (int32_t word = 0; word < word_count; word++) {
                const int32_t first = word * kChunkWordSections;
                const int32_t count = Wide ? std::min(kChunkWordSections, n - first) : n;
                const size_t a_idx = metadata_a + static_cast<size_t>(word);
                const size_t b_idx = metadata_b + static_cast<size_t>(word);
                const auto kinds_a = chunks.kinds_by_id[a_idx];
                const auto kinds_b = chunks.kinds_by_id[b_idx];
                const auto active_a = chunks.active_mask_by_id[a_idx];
                const auto active_b = chunks.active_mask_by_id[b_idx];
                if ((active_a | active_b) == 0u)
                    continue;

                const int32_t *const section_ids_a = section_ids_of(chunks, a_id) + first;
                const int32_t *const section_ids_b = section_ids_of(chunks, b_id) + first;

                const uint32_t uni_lanes = uni_lane_mask(kinds_a) & uni_lane_mask(kinds_b) & lane_span_mask(count);
                uint32_t handled = 0u;

                if (uni_lanes != 0u && (world.column_path_flags & kColumnPathDisabled) == 0u &&
                    world.diffusion_transport_enabled == 0u) {
                    const int32_t base_a = a_id * n + first;
                    const int32_t base_b = b_id * n + first;
                    if (section_id_in_range(sections, base_a + count - 1) &&
                        section_id_in_range(sections, base_b + count - 1)) {
                        const uint32_t changed = exchange_uni_column(
                                sections.uniform_density.data() + base_a, sections.uniform_density.data() + base_b,
                                count, world.uniform_exchange, uni_lanes,
                                (world.column_path_flags & kColumnPathForceScalar) != 0u,
                                (world.column_path_flags & kColumnPathForce256) != 0u,
                                (world.column_path_flags & kColumnPathForceAvx) != 0u);
                        handled = uni_lanes;
                        if (changed != 0u) {
                            for (uint32_t m = changed; m != 0u; m &= (m - 1u)) {
                                const int32_t sy = std::countr_zero(m);
                                sections.active_by_id[static_cast<size_t>(base_a + sy)] = 1u;
                                sections.active_by_id[static_cast<size_t>(base_b + sy)] = 1u;
                            }
                            chunks.active_mask_by_id[a_idx] |= changed;
                            chunks.active_mask_by_id[b_idx] |= changed;
                            mark_chunk_dirty(world, a_id);
                            mark_chunk_dirty(world, b_id);
                        }
                    }
                }

                uint32_t active_sections = (active_a | active_b) & ~handled;
                while (active_sections != 0u) {
                    const int32_t sy = std::countr_zero(active_sections);
                    active_sections &= (active_sections - 1u);

                    const uint32_t shift = static_cast<uint32_t>(sy) << 1u;
                    const uint8_t kind_a = static_cast<uint8_t>((kinds_a >> shift) & 0x3u);
                    const uint8_t kind_b = static_cast<uint8_t>((kinds_b >> shift) & 0x3u);

                    const int32_t section_a = section_ids_a[static_cast<size_t>(sy)];
                    const int32_t section_b = section_ids_b[static_cast<size_t>(sy)];
                    if (!section_id_in_range(sections, section_a) || !section_id_in_range(sections, section_b))
                        continue;
                    exchange_face_exact(world, section_a, kind_a, face_a, section_b, kind_b, face_b);
                }
            }
        }
    }

    void run_xz_pairs_range(RadWorld &world, const PairSpan &pairs, int32_t face_a, int32_t face_b, int32_t start,
                            int32_t end) {
        if (world.chunks.words_per_chunk == 1) [[likely]] {
            run_xz_pairs_range_impl<false>(world, pairs, face_a, face_b, start, end);
        } else {
            run_xz_pairs_range_impl<true>(world, pairs, face_a, face_b, start, end);
        }
    }

    constexpr uint32_t y_pair_start_mask(int32_t parity, int32_t n) {
        uint32_t mask = 0u;
        for (int32_t sy = parity; sy + 1 < n; sy += 2) {
            mask |= (1u << static_cast<uint32_t>(sy));
        }
        return mask;
    }

    template <bool Wide>
    void run_y_chunks_range_impl(RadWorld &world, const std::span<const int32_t> &chunk_ids, int32_t parity,
                                 int32_t start, int32_t end) {
        ChunkTable &chunks = world.chunks;
        SectionTable &sections = world.sections;
        if (!(world.diffusion_dt > 0.0) || !std::isfinite(world.diffusion_dt))
            return;
        const int32_t chunk_count = static_cast<int32_t>(chunk_ids.size());
        const int32_t *__restrict__ chunk_ptr = chunk_ids.data();
        const int32_t hi = std::min(end, chunk_count);
        for (int32_t i = start; i < hi; i++) {
            if (i + 1 < hi)
                prefetch_chunk_metadata<Wide>(chunks, chunk_ptr[i + 1]);
            const int32_t chunk_id = chunk_ptr[i];
            if (!is_loaded_chunk_id(chunks, chunk_id))
                continue;
            const int32_t n = sections_per_chunk(chunks);
            const size_t metadata_base =
                    Wide ? static_cast<size_t>(chunk_id) * chunks.words_per_chunk : static_cast<size_t>(chunk_id);
            const int32_t word_count = Wide ? chunks.words_per_chunk : 1;
            for (int32_t word = 0; word < word_count; word++) {
                const int32_t first = word * kChunkWordSections;
                const int32_t count = Wide ? std::min(kChunkWordSections, n - first) : n;
                const size_t cidx = metadata_base + static_cast<size_t>(word);
                const auto kinds = chunks.kinds_by_id[cidx];
                auto &active_mask = chunks.active_mask_by_id[cidx];
                const int32_t *const section_ids = section_ids_of(chunks, chunk_id) + first;
                if constexpr (Wide) {

                    if (word != 0 && parity == 1 &&
                        ((chunks.active_mask_by_id[cidx - 1] >> 31u) | (active_mask & 1u)) != 0u) {
                        const auto lower_kind = static_cast<uint8_t>((chunks.kinds_by_id[cidx - 1] >> 62u) & 3u);
                        const auto upper_kind = static_cast<uint8_t>(kinds & 3u);
                        exchange_face_exact(world, section_ids[-1], lower_kind, 1, section_ids[0], upper_kind, 0);
                    }
                }
                const uint32_t pair_start_mask = y_pair_start_mask(parity, count);

                const uint32_t uni = uni_lane_mask(kinds) & lane_span_mask(count);
                const uint32_t uni_starts = uni & (uni >> 1u) & pair_start_mask;
                uint32_t handled_starts = 0u;
                if (uni_starts != 0u && world.diffusion_transport_enabled == 0u) {
                    const int32_t base = chunk_id * n + first;
                    if (section_id_in_range(sections, base) && section_id_in_range(sections, base + count - 1)) {
                        const uint32_t changed =
                                exchange_uni_y(sections.uniform_density.data() + base, parity, count,
                                               world.uniform_exchange, uni_starts | (uni_starts << 1u),
                                               (world.column_path_flags & kColumnPathForceScalar) != 0u,
                                               (world.column_path_flags & kColumnPathForce256) != 0u,
                                               (world.column_path_flags & kColumnPathForceAvx) != 0u);
                        handled_starts = uni_starts;
                        if (changed != 0u) {
                            for (uint32_t m = changed; m != 0u; m &= (m - 1u)) {
                                sections.active_by_id[static_cast<size_t>(base + std::countr_zero(m))] = 1u;
                            }
                            active_mask |= changed;
                            mark_chunk_dirty(world, chunk_id);
                        }
                    }
                }

                uint32_t active_starts =
                        (active_mask | static_cast<uint32_t>(active_mask >> 1u)) & pair_start_mask & ~handled_starts;
                while (active_starts != 0u) {
                    const int32_t sy = std::countr_zero(static_cast<unsigned int>(active_starts));
                    active_starts = static_cast<uint32_t>(active_starts & (active_starts - 1u));

                    const uint32_t shift = static_cast<uint32_t>(sy) << 1u;
                    const uint8_t kind_a = static_cast<uint8_t>((kinds >> shift) & 0x3u);
                    const uint8_t kind_b = static_cast<uint8_t>((kinds >> (shift + 2u)) & 0x3u);

                    const int32_t section_a = section_ids[static_cast<size_t>(sy)];
                    const int32_t section_b = section_ids[static_cast<size_t>(sy + 1)];
                    if (!section_id_in_range(sections, section_a) || !section_id_in_range(sections, section_b))
                        continue;
                    exchange_face_exact(world, section_a, kind_a, 1, section_b, kind_b, 0);
                }
            }
        }
    }

    void run_y_chunks_range(RadWorld &world, const std::span<const int32_t> &chunk_ids, int32_t parity, int32_t start,
                            int32_t end) {
        if (world.chunks.words_per_chunk == 1) [[likely]] {
            run_y_chunks_range_impl<false>(world, chunk_ids, parity, start, end);
        } else {
            run_y_chunks_range_impl<true>(world, chunk_ids, parity, start, end);
        }
    }

    void run_exact_exchange_sweeps(RadWorld &world) {
        const bool pair_lists_dirty_before = world.pair_lists_dirty != 0u;
        rebuild_pair_lists_if_needed(world);
        world.last_step_pair_lists_dirty_before = pair_lists_dirty_before ? 1u : 0u;
        world.last_step_pair_lists_rebuilt = pair_lists_dirty_before ? 1u : 0u;

        bool flip_x;
        bool flip_z;
        int32_t y_parity;
        int32_t perm;
        if ((world.last_perm_bits & kPermOverrideFlag) != 0) {
            flip_x = (world.last_perm_bits & kPermMaskFlipX) != 0;
            flip_z = (world.last_perm_bits & kPermMaskFlipZ) != 0;
            y_parity = ((world.last_perm_bits & kPermMaskYParity) != 0) ? 1 : 0;
            perm = (world.last_perm_bits & kPermMaskOrder) >> kPermShiftOrder;
            perm %= 6;
        } else {
            int32_t s = world.last_work_epoch;
            flip_x = (s & 1) != 0;
            flip_z = (s & 2) != 0;
            y_parity = ((s & 4) != 0) ? 1 : 0;
            perm = s % 6;
            if (perm < 0)
                perm += 6;
        }

        auto xz_group = [&](const std::array<std::vector<int32_t>, 4> &a_by_bucket,
                            const std::array<std::vector<int32_t>, 4> &b_by_bucket, int32_t bucket0, int32_t bucket1,
                            int32_t face_a, int32_t face_b) {
            const PairSpan span0{.a = a_by_bucket[static_cast<size_t>(bucket0)],
                                 .b = b_by_bucket[static_cast<size_t>(bucket0)]};
            const PairSpan span1{.a = a_by_bucket[static_cast<size_t>(bucket1)],
                                 .b = b_by_bucket[static_cast<size_t>(bucket1)]};
            const int32_t n0 = span0.count();
            const int32_t n1 = span1.count();
            parallel_for_range(world, n0 + n1, kMinTaskGrain, [&](int32_t lo, int32_t hi) {
                if (lo < n0)
                    run_xz_pairs_range(world, span0, face_a, face_b, lo, std::min(hi, n0));
                if (hi > n0)
                    run_xz_pairs_range(world, span1, face_a, face_b, std::max(lo, n0) - n0, hi - n0);
            });
        };

        auto axis_runs_sweep = [&](const RadWorld::AxisRuns &runs, bool flip, int32_t face_a, int32_t face_b) {
            const auto count = static_cast<int32_t>(runs.start.size());
            const PairSpan span{.a = runs.a, .b = runs.b};
            parallel_for_range(world, count, 1, [&](int32_t lo, int32_t hi) {
                for (int32_t i = lo; i < hi; i++) {
                    const auto idx = static_cast<size_t>(i);
                    const int32_t s = runs.start[idx];
                    const int32_t m = runs.mid[idx];
                    const int32_t e = runs.end[idx];
                    if (flip) {
                        run_xz_pairs_range(world, span, face_a, face_b, m, e);
                        run_xz_pairs_range(world, span, face_a, face_b, s, m);
                    } else {
                        run_xz_pairs_range(world, span, face_a, face_b, s, m);
                        run_xz_pairs_range(world, span, face_a, face_b, m, e);
                    }
                }
            });
        };

        constexpr int32_t kMinPairsPerRun = 32;
        const auto runs_fill_threads = [&](const RadWorld::AxisRuns &runs) {
            const auto count = static_cast<int32_t>(runs.start.size());
            if (count < step_parallelism())
                return false;
            return static_cast<int32_t>(runs.a.size()) / count >= kMinPairsPerRun;
        };
        auto sweep_x = [&]() {
            if (runs_fill_threads(world.x_runs)) {
                axis_runs_sweep(world.x_runs, flip_x, 5, 4);
                return;
            }
            const int32_t first = flip_x ? 1 : 0;
            xz_group(world.x_pair_a_by_bucket, world.x_pair_b_by_bucket, first, first + 2, 5, 4);
            xz_group(world.x_pair_a_by_bucket, world.x_pair_b_by_bucket, 1 - first, 3 - first, 5, 4);
        };
        auto sweep_z = [&]() {
            if (runs_fill_threads(world.z_runs)) {
                axis_runs_sweep(world.z_runs, flip_z, 3, 2);
                return;
            }
            const int32_t first = flip_z ? 2 : 0;
            xz_group(world.z_pair_a_by_bucket, world.z_pair_b_by_bucket, first, first + 1, 3, 2);
            xz_group(world.z_pair_a_by_bucket, world.z_pair_b_by_bucket, 2 - first, 3 - first, 3, 2);
        };
        auto sweep_y = [&]() {
            const auto &buckets = world.chunks.parity_bucket_ids;
            int32_t total = 0;
            for (const auto &b : buckets)
                total += static_cast<int32_t>(b.size());
            parallel_for_range(world, total, kMinTaskGrain, [&](int32_t lo, int32_t hi) {
                int32_t base = 0;
                for (const auto &b : buckets) {
                    const auto n = static_cast<int32_t>(b.size());
                    const int32_t from = std::max(lo, base);
                    const int32_t to = std::min(hi, base + n);
                    if (from < to) {
                        run_y_chunks_range(world, b, y_parity, from - base, to - base);
                        run_y_chunks_range(world, b, y_parity ^ 1, from - base, to - base);
                    }
                    base += n;
                }
            });
        };

        struct PhaseWork {
            std::atomic<int32_t> cursor{0};
            std::atomic<int32_t> done{0};
            int32_t count = 0;
        };

        const auto axis_slice = [&](const RadWorld::AxisRuns &runs, bool flip, int32_t face_a, int32_t face_b,
                                    int32_t i) {
            const PairSpan span{.a = runs.a, .b = runs.b};
            const auto idx = static_cast<size_t>(i);
            const int32_t a = runs.start[idx];
            const int32_t m = runs.mid[idx];
            const int32_t e = runs.end[idx];
            if (flip) {
                run_xz_pairs_range(world, span, face_a, face_b, m, e);
                run_xz_pairs_range(world, span, face_a, face_b, a, m);
            } else {
                run_xz_pairs_range(world, span, face_a, face_b, a, m);
                run_xz_pairs_range(world, span, face_a, face_b, m, e);
            }
        };

        const auto &y_buckets = world.chunks.parity_bucket_ids;
        const auto y_slice_range = [&](int32_t lo, int32_t hi) {
            int32_t base = 0;
            for (const auto &b : y_buckets) {
                const auto n = static_cast<int32_t>(b.size());
                const int32_t from = std::max(lo, base);
                const int32_t to = std::min(hi, base + n);
                if (from < to) {
                    run_y_chunks_range(world, b, y_parity, from - base, to - base);
                    run_y_chunks_range(world, b, y_parity ^ 1, from - base, to - base);
                }
                base += n;
            }
        };

        int32_t y_total = 0;
        for (const auto &b : y_buckets)
            y_total += static_cast<int32_t>(b.size());

        const bool fused_dag = runs_fill_threads(world.x_runs) && runs_fill_threads(world.z_runs) && y_total > 0;
        if (fused_dag && world_parallel_enabled(world)) {
            PhaseWork phases[3];
            const int32_t y_grain = std::max(kMinTaskGrain, y_total / std::max(1, step_parallelism() * 8));
            const auto axis_of = [&](int32_t slot) {
                static constexpr int8_t kOrder[6][3] = {{0, 1, 2}, {0, 2, 1}, {2, 1, 0},
                                                        {2, 0, 1}, {1, 0, 2}, {1, 2, 0}};
                return static_cast<int32_t>(kOrder[perm][slot]);
            };
            for (int32_t slot = 0; slot < 3; slot++) {
                switch (axis_of(slot)) {
                case 0:
                    phases[slot].count = static_cast<int32_t>(world.x_runs.start.size());
                    break;
                case 1:
                    phases[slot].count = static_cast<int32_t>(world.z_runs.start.size());
                    break;
                default:
                    phases[slot].count = (y_total + y_grain - 1) / y_grain;
                    break;
                }
            }
            auto worker = [&] {
                for (int32_t slot = 0; slot < 3; slot++) {
                    PhaseWork &ph = phases[slot];
                    const int32_t axis = axis_of(slot);
                    for (;;) {
                        const int32_t i = ph.cursor.fetch_add(1, std::memory_order_relaxed);
                        if (i >= ph.count)
                            break;
                        if (axis == 0)
                            axis_slice(world.x_runs, flip_x, 5, 4, i);
                        else if (axis == 1)
                            axis_slice(world.z_runs, flip_z, 3, 2, i);
                        else
                            y_slice_range(i * y_grain, std::min((i + 1) * y_grain, y_total));
                        ph.done.fetch_add(1, std::memory_order_release);
                    }

                    for (int32_t spin = 0; ph.done.load(std::memory_order_acquire) < ph.count; spin++) {
                        if ((spin & 0xFF) == 0xFF)
                            std::this_thread::yield();
                        else
                            pool::Pool::relax();
                    }
                }
            };
            int32_t want = step_parallelism();
            const int32_t widest = std::max(phases[0].count, std::max(phases[1].count, phases[2].count));
            if (want > widest)
                want = widest;
            pool::Pool::instance().invoke_n(want, worker);
            return;
        }

        switch (perm) {
        case 0:
            sweep_x();
            sweep_z();
            sweep_y();
            break;
        case 1:
            sweep_x();
            sweep_y();
            sweep_z();
            break;
        case 2:
            sweep_y();
            sweep_z();
            sweep_x();
            break;
        case 3:
            sweep_y();
            sweep_x();
            sweep_z();
            break;
        case 4:
            sweep_z();
            sweep_x();
            sweep_y();
            break;
        default:
            sweep_z();
            sweep_y();
            sweep_x();
            break;
        }
    }
}
