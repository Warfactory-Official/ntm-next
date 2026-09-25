// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/internal.hpp"
#include "radsim/cpu_profile.hpp"
#include "simd/avx512_target.h"

#include <cstdlib>

#define HBM_RADSIM_DECAY_USE_GCC_SIMD (HBM_RADSIM_X86_SIMD)

namespace hbm::radsim::detail {
    namespace {
        struct DecayEventBuffers {
            std::vector<int64_t> fog;
            std::vector<int64_t> destroy;

            void merge_from(DecayEventBuffers &&other) {
                if (!other.fog.empty()) {
                    fog.insert(fog.end(), std::make_move_iterator(other.fog.begin()),
                               std::make_move_iterator(other.fog.end()));
                }
                if (!other.destroy.empty()) {
                    destroy.insert(destroy.end(), std::make_move_iterator(other.destroy.begin()),
                                   std::make_move_iterator(other.destroy.end()));
                }
            }
        };

        struct DecayMultiSummary {
            bool dirty = false;
            bool active = false;
            double total_mass = 0.0;
            int64_t total_open = 0;
        };

        inline DecayMultiSummary decay_multi_no_events_scalar_fast(double *__restrict__ density_ptr,
                                                                   size_t density_count,
                                                                   const int32_t *__restrict__ volume_ptr,
                                                                   size_t weighted_count, double retention_dt,
                                                                   double min_bound, double eps) noexcept {
            DecayMultiSummary summary;
            for (size_t p = 0; p < density_count; p++) {
                const double prev = density_ptr[p];
                const double next = sanitize_density_raw(prev * retention_dt, min_bound, eps);
                summary.dirty |= (next != prev);
                summary.active |= (next != 0.0);
                density_ptr[p] = next;
                if (p < weighted_count) {
                    const int32_t vol = volume_ptr[p];
                    if (vol > 0) {
                        summary.total_mass += next * static_cast<double>(vol);
                        summary.total_open += vol;
                    }
                }
            }
            return summary;
        }

#if HBM_RADSIM_DECAY_USE_GCC_SIMD
        __attribute__((target("avx")))
        DecayMultiSummary decay_multi_no_events_avx(double *__restrict__ density_ptr, size_t density_count,
                                                    const int32_t *__restrict__ volume_ptr, size_t weighted_count,
                                                    double retention_dt, double min_bound, double eps) noexcept {
            constexpr size_t kVecWidth = 4u;
            const __m256d retention_v = _mm256_set1_pd(retention_dt);
            const __m256d zero_v = _mm256_setzero_pd();
            const __m256d min_v = _mm256_set1_pd(min_bound);
            const __m256d max_v = _mm256_set1_pd(kSanitizedDensityMax);
            const __m256d eps_v = _mm256_set1_pd(eps);
            const __m256d neg_eps_v = _mm256_set1_pd(-eps);
            const __m256d signbit_v = _mm256_set1_pd(-0.0);

            DecayMultiSummary summary;
            __m256d mass_sum_v = _mm256_setzero_pd();

            __m128i open_sum_v = _mm_setzero_si128();
            int dirty_mask = 0;
            int active_mask = 0;
            size_t p = 0;
            const size_t weighted_vec_end = weighted_count & ~(kVecWidth - 1u);

            for (; p < weighted_vec_end; p += kVecWidth) {
                const __m256d prev_v = _mm256_loadu_pd(density_ptr + p);
                const __m256d decayed_v = _mm256_mul_pd(prev_v, retention_v);
                const __m256d abs_v = _mm256_andnot_pd(signbit_v, decayed_v);
                const __m256d finite_mask = _mm256_cmp_pd(abs_v, max_v, _CMP_LE_OQ);
                const __m256d nan_mask = _mm256_cmp_pd(decayed_v, decayed_v, _CMP_UNORD_Q);
                const __m256d neg_mask = _mm256_cmp_pd(decayed_v, zero_v, _CMP_LT_OQ);

                const __m256d clamped_lo_v = _mm256_max_pd(decayed_v, min_v);
                const __m256d clamped_v = _mm256_min_pd(clamped_lo_v, max_v);
                const __m256d near_zero_mask =
                        _mm256_and_pd(_mm256_cmp_pd(clamped_v, min_v, _CMP_GT_OQ),
                                      _mm256_and_pd(_mm256_cmp_pd(clamped_v, eps_v, _CMP_LT_OQ),
                                                    _mm256_cmp_pd(clamped_v, neg_eps_v, _CMP_GT_OQ)));
                const __m256d sanitized_finite_v = _mm256_blendv_pd(clamped_v, zero_v, near_zero_mask);
                const __m256d inf_replacement_v = _mm256_blendv_pd(max_v, min_v, neg_mask);
                const __m256d nonfinite_replacement_v = _mm256_blendv_pd(inf_replacement_v, zero_v, nan_mask);
                const __m256d next_v = _mm256_blendv_pd(nonfinite_replacement_v, sanitized_finite_v, finite_mask);

                _mm256_storeu_pd(density_ptr + p, next_v);
                dirty_mask |= _mm256_movemask_pd(_mm256_cmp_pd(next_v, prev_v, _CMP_NEQ_UQ));
                active_mask |= _mm256_movemask_pd(_mm256_cmp_pd(next_v, zero_v, _CMP_NEQ_OQ));

                const __m128i volume_i = _mm_loadu_si128(reinterpret_cast<const __m128i *>(volume_ptr + p));
                const __m128i clamped_volume_i = _mm_max_epi32(volume_i, _mm_setzero_si128());
                open_sum_v = _mm_add_epi32(open_sum_v, clamped_volume_i);

                const __m256d volume_v = _mm256_cvtepi32_pd(clamped_volume_i);
                mass_sum_v = _mm256_add_pd(mass_sum_v, _mm256_mul_pd(next_v, volume_v));
            }

            alignas(32) double mass_lane[4];
            _mm256_store_pd(mass_lane, mass_sum_v);
            summary.total_mass = mass_lane[0] + mass_lane[1] + mass_lane[2] + mass_lane[3];
            alignas(16) int32_t open_lane[4];
            _mm_store_si128(reinterpret_cast<__m128i *>(open_lane), open_sum_v);
            summary.total_open += static_cast<int64_t>(open_lane[0]) + open_lane[1] + open_lane[2] + open_lane[3];

            for (; p < weighted_count; p++) {
                const double prev = density_ptr[p];
                const double next = sanitize_density_raw(prev * retention_dt, min_bound, eps);
                summary.dirty |= (next != prev);
                summary.active |= (next != 0.0);
                density_ptr[p] = next;
                const int32_t vol = volume_ptr[p];
                if (vol > 0) {
                    summary.total_mass += next * static_cast<double>(vol);
                    summary.total_open += vol;
                }
            }

            for (; p < density_count; p++) {
                const double prev = density_ptr[p];
                const double next = sanitize_density_raw(prev * retention_dt, min_bound, eps);
                summary.dirty |= (next != prev);
                summary.active |= (next != 0.0);
                density_ptr[p] = next;
            }

            summary.dirty |= (dirty_mask != 0);
            summary.active |= (active_mask != 0);
            return summary;
        }

        __attribute__((target(NTM_TARGET_AVX512F))) DecayMultiSummary decay_multi_no_events_avx512(
                double *__restrict__ density_ptr, size_t density_count, const int32_t *__restrict__ volume_ptr,
                size_t weighted_count, double retention_dt, double min_bound, double eps) noexcept {
            constexpr size_t kVecWidth = 8u;
            const __m512d retention_v = _mm512_set1_pd(retention_dt);
            const __m512d zero_v = _mm512_setzero_pd();
            const __m512d min_v = _mm512_set1_pd(min_bound);
            const __m512d max_v = _mm512_set1_pd(kSanitizedDensityMax);
            const __m512d eps_v = _mm512_set1_pd(eps);
            const __m512d neg_eps_v = _mm512_set1_pd(-eps);

            DecayMultiSummary summary;
            __m512d mass_sum_v = _mm512_setzero_pd();
            __m256i open_sum_v = _mm256_setzero_si256();
            __mmask8 dirty_k = 0;
            __mmask8 active_k = 0;
            size_t p = 0;
            const size_t weighted_vec_end = weighted_count & ~(kVecWidth - 1u);

            for (; p < weighted_vec_end; p += kVecWidth) {
                const __m512d prev_v = _mm512_loadu_pd(density_ptr + p);
                const __m512d decayed_v = _mm512_mul_pd(prev_v, retention_v);
                const __m512d abs_v = _mm512_abs_pd(decayed_v);
                const __mmask8 finite_k = _mm512_cmp_pd_mask(abs_v, max_v, _CMP_LE_OQ);
                const __mmask8 nan_k = _mm512_cmp_pd_mask(decayed_v, decayed_v, _CMP_UNORD_Q);
                const __mmask8 neg_k = _mm512_cmp_pd_mask(decayed_v, zero_v, _CMP_LT_OQ);

                const __m512d clamped_v = _mm512_min_pd(_mm512_max_pd(decayed_v, min_v), max_v);
                const __mmask8 near_zero_k =
                        static_cast<__mmask8>(_mm512_cmp_pd_mask(clamped_v, min_v, _CMP_GT_OQ) &
                                              _mm512_cmp_pd_mask(clamped_v, eps_v, _CMP_LT_OQ) &
                                              _mm512_cmp_pd_mask(clamped_v, neg_eps_v, _CMP_GT_OQ));

                const __m512d sanitized_finite_v = _mm512_mask_blend_pd(near_zero_k, clamped_v, zero_v);
                const __m512d inf_replacement_v = _mm512_mask_blend_pd(neg_k, max_v, min_v);
                const __m512d nonfinite_replacement_v = _mm512_mask_blend_pd(nan_k, inf_replacement_v, zero_v);
                const __m512d next_v = _mm512_mask_blend_pd(finite_k, nonfinite_replacement_v, sanitized_finite_v);

                _mm512_storeu_pd(density_ptr + p, next_v);
                dirty_k = static_cast<__mmask8>(dirty_k | _mm512_cmp_pd_mask(next_v, prev_v, _CMP_NEQ_UQ));
                active_k = static_cast<__mmask8>(active_k | _mm512_cmp_pd_mask(next_v, zero_v, _CMP_NEQ_OQ));

                const __m256i volume_i = _mm256_loadu_si256(reinterpret_cast<const __m256i *>(volume_ptr + p));
                const __m256i clamped_volume_i = _mm256_max_epi32(volume_i, _mm256_setzero_si256());
                open_sum_v = _mm256_add_epi32(open_sum_v, clamped_volume_i);
                const __m512d volume_v = _mm512_cvtepi32_pd(clamped_volume_i);
                mass_sum_v = _mm512_add_pd(mass_sum_v, _mm512_mul_pd(next_v, volume_v));
            }

            alignas(64) double mass_lane[8];
            _mm512_store_pd(mass_lane, mass_sum_v);
            summary.total_mass = mass_lane[0] + mass_lane[1] + mass_lane[2] + mass_lane[3] + mass_lane[4] +
                                 mass_lane[5] + mass_lane[6] + mass_lane[7];
            alignas(32) int32_t open_lane[8];
            _mm256_store_si256(reinterpret_cast<__m256i *>(open_lane), open_sum_v);
            for (int l = 0; l < 8; l++)
                summary.total_open += open_lane[l];

            for (; p < weighted_count; p++) {
                const double prev = density_ptr[p];
                const double next = sanitize_density_raw(prev * retention_dt, min_bound, eps);
                summary.dirty |= (next != prev);
                summary.active |= (next != 0.0);
                density_ptr[p] = next;
                const int32_t vol = volume_ptr[p];
                if (vol > 0) {
                    summary.total_mass += next * static_cast<double>(vol);
                    summary.total_open += vol;
                }
            }
            for (; p < density_count; p++) {
                const double prev = density_ptr[p];
                const double next = sanitize_density_raw(prev * retention_dt, min_bound, eps);
                summary.dirty |= (next != prev);
                summary.active |= (next != 0.0);
                density_ptr[p] = next;
            }
            summary.dirty |= (dirty_k != 0);
            summary.active |= (active_k != 0);
            return summary;
        }

        inline bool decay_multi_no_events_use_avx512() noexcept { return cpu_profile().has_avx512f; }

        inline bool decay_multi_no_events_use_avx() noexcept { return cpu_profile().has_avx; }
#endif

        [[gnu::always_inline]] inline void decay_multi_no_events_dispatch(DecayMultiSummary &summary,
                                                                          double *__restrict__ density_ptr,
                                                                          size_t density_count,
                                                                          const int32_t *__restrict__ volume_ptr,
                                                                          size_t weighted_count, double retention_dt,
                                                                          double min_bound, double eps) noexcept {
#if HBM_RADSIM_DECAY_USE_GCC_SIMD
            if (density_count >= 16u && decay_multi_no_events_use_avx512()) {
                summary = decay_multi_no_events_avx512(density_ptr, density_count, volume_ptr, weighted_count,
                                                       retention_dt, min_bound, eps);
            } else if (density_count >= 8u && decay_multi_no_events_use_avx()) {
                summary = decay_multi_no_events_avx(density_ptr, density_count, volume_ptr, weighted_count,
                                                    retention_dt, min_bound, eps);
            } else {
                summary = decay_multi_no_events_scalar_fast(density_ptr, density_count, volume_ptr, weighted_count,
                                                            retention_dt, min_bound, eps);
            }
#else
            summary = decay_multi_no_events_scalar_fast(density_ptr, density_count, volume_ptr, weighted_count,
                                                        retention_dt, min_bound, eps);
#endif
        }

        template <bool Wide>
        DecayEventBuffers post_sweep_decay_range_no_events_impl(RadWorld &world, const std::vector<int32_t> &chunk_ids,
                                                                int32_t lo, int32_t hi) {
            DecayEventBuffers out;
            ChunkTable &chunks = world.chunks;
            SectionTable &sections = world.sections;
            const double retention_dt = world.retention_dt;
            const double min_bound = world.min_bound;
            const double eps = world.eps;

            for (int32_t i = lo; i < hi; i++) {
                const int32_t chunk_id = chunk_ids[static_cast<size_t>(i)];
                if (!is_loaded_chunk_id(chunks, chunk_id))
                    continue;
                bool dirty = false;
                const int32_t column_n = sections_per_chunk(chunks);
                const size_t metadata_base =
                        Wide ? static_cast<size_t>(chunk_id) * chunks.words_per_chunk : static_cast<size_t>(chunk_id);
                const int32_t word_count = Wide ? chunks.words_per_chunk : 1;
                for (int32_t word = 0; word < word_count; word++) {
                    const int32_t first = word * kChunkWordSections;
                    const int32_t n = Wide ? std::min(kChunkWordSections, column_n - first) : column_n;
                    const size_t cidx = metadata_base + static_cast<size_t>(word);
                    auto &active_mask = chunks.active_mask_by_id[cidx];
                    const int32_t *const section_ids = section_ids_of(chunks, chunk_id) + first;
                    const uint32_t source_mask = chunks.source_mask_by_id[cidx];
                    for (int32_t sy = 0; sy < n; sy++) {
                        const uint32_t bit = (1u << static_cast<uint32_t>(sy));
                        if ((active_mask & bit) == 0u && (source_mask & bit) == 0u)
                            continue;

                        const int32_t section_id = section_ids[static_cast<size_t>(sy)];
                        if (!section_id_in_range(sections, section_id)) {
                            active_mask = static_cast<uint32_t>(active_mask & ~bit);
                            continue;
                        }

                        const auto sidx = static_cast<size_t>(section_id);
                        const SectionSourceState *const src =
                                ((source_mask & bit) != 0u) ? section_source_state_ptr(sections, section_id) : nullptr;
                        const uint8_t kind = sections.kind_by_id[sidx];
                        bool active = false;

                        if (kind == kKindUni || kind == kKindSingle) {
                            const double prev = sections.uniform_density[sidx];

                            const double emitted = relax_section_sources(src, 0, prev);
                            const double next =
                                    sanitize_density_raw(java_strict_mul(emitted, retention_dt), min_bound, eps);
                            if (next != prev)
                                dirty = true;
                            sections.uniform_density[sidx] = next;
                            active = (next != 0.0);
                        } else if (kind == kKindMulti) {
                            auto &multi = ensure_section_multi_state(sections, section_id);
                            auto &density = multi.pocket_density;
                            const auto &volume = multi.pocket_volume;
                            const size_t density_count = density.size();
                            const size_t weighted_count = std::min(density_count, volume.size());
                            double *const density_ptr = density.data();
                            const int32_t *const volume_ptr = volume.data();

                            if (src != nullptr) {
                                for (size_t p = 0; p < density_count; p++) {
                                    const double before = density_ptr[p];
                                    const double after = relax_section_sources(src, static_cast<int32_t>(p), before);
                                    if (after != before) {
                                        density_ptr[p] = after;
                                        dirty = true;
                                    }
                                }
                            }
                            DecayMultiSummary summary;
                            decay_multi_no_events_dispatch(summary, density_ptr, density_count, volume_ptr,
                                                           weighted_count, retention_dt, min_bound, eps);
                            dirty |= summary.dirty;
                            active = summary.active;
                            sections.uniform_density[sidx] =
                                    (summary.total_open > 0)
                                            ? sanitize_density_raw(summary.total_mass /
                                                                           static_cast<double>(summary.total_open),
                                                                   min_bound, eps)
                                            : 0.0;
                        } else {
                            const auto *multi = section_multi_state_ptr(sections, section_id);
                            if (sections.uniform_density[sidx] != 0.0 ||
                                (multi != nullptr && !multi->pocket_density.empty()))
                                dirty = true;
                            sections.uniform_density[sidx] = 0.0;
                            release_section_multi_state(sections, section_id);
                            active = false;
                        }

                        const bool was_active = (active_mask & bit) != 0u;
                        if ((sections.active_by_id[sidx] != 0u) != active) {
                            sections.active_by_id[sidx] = active ? 1u : 0u;
                        }
                        if (was_active != active) {
                            if (active)
                                active_mask = static_cast<uint32_t>(active_mask | bit);
                            else
                                active_mask = static_cast<uint32_t>(active_mask & ~bit);
                            dirty = true;
                        }
                    }
                }
                if (dirty)
                    mark_chunk_dirty(world, chunk_id);
            }
            return out;
        }

        DecayEventBuffers post_sweep_decay_range_no_events(RadWorld &world, const std::vector<int32_t> &chunk_ids,
                                                           int32_t lo, int32_t hi) {
            if (world.chunks.words_per_chunk == 1) [[likely]] {
                return post_sweep_decay_range_no_events_impl<false>(world, chunk_ids, lo, hi);
            }
            return post_sweep_decay_range_no_events_impl<true>(world, chunk_ids, lo, hi);
        }

        template <bool Wide>
        DecayEventBuffers post_sweep_decay_range_with_events_impl(RadWorld &world,
                                                                  const std::vector<int32_t> &chunk_ids, int32_t lo,
                                                                  int32_t hi) {
            DecayEventBuffers out;
            ChunkTable &chunks = world.chunks;
            SectionTable &sections = world.sections;
            const bool fog_enabled = world.fog_prob_u64 != 0u;
            const bool destroy_enabled = world.destroy_prob_u64 != 0u;
            const double retention_dt = world.retention_dt;
            const double min_bound = world.min_bound;
            const double eps = world.eps;

            for (int32_t i = lo; i < hi; i++) {
                const int32_t chunk_id = chunk_ids[static_cast<size_t>(i)];
                if (!is_loaded_chunk_id(chunks, chunk_id))
                    continue;
                bool dirty = false;
                const int32_t column_n = sections_per_chunk(chunks);
                const size_t metadata_base =
                        Wide ? static_cast<size_t>(chunk_id) * chunks.words_per_chunk : static_cast<size_t>(chunk_id);
                const int32_t word_count = Wide ? chunks.words_per_chunk : 1;
                for (int32_t word = 0; word < word_count; word++) {
                    const int32_t first = word * kChunkWordSections;
                    const int32_t n = Wide ? std::min(kChunkWordSections, column_n - first) : column_n;
                    const size_t cidx = metadata_base + static_cast<size_t>(word);
                    auto &active_mask = chunks.active_mask_by_id[cidx];
                    const int32_t *const section_ids = section_ids_of(chunks, chunk_id) + first;
                    const uint32_t source_mask = chunks.source_mask_by_id[cidx];
                    for (int32_t sy = 0; sy < n; sy++) {
                        const uint32_t bit = (1u << static_cast<uint32_t>(sy));
                        if ((active_mask & bit) == 0u && (source_mask & bit) == 0u)
                            continue;

                        const int32_t section_id = section_ids[static_cast<size_t>(sy)];
                        if (!section_id_in_range(sections, section_id)) {
                            active_mask = static_cast<uint32_t>(active_mask & ~bit);
                            continue;
                        }

                        const auto sidx = static_cast<size_t>(section_id);
                        const SectionSourceState *const src =
                                ((source_mask & bit) != 0u) ? section_source_state_ptr(sections, section_id) : nullptr;
                        const uint8_t kind = sections.kind_by_id[sidx];
                        const int64_t section_key = sections.key_by_id[sidx];
                        bool active = false;

                        if (kind == kKindUni || kind == kKindSingle) {
                            const double prev = sections.uniform_density[sidx];

                            const double emitted = relax_section_sources(src, 0, prev);
                            const double next =
                                    sanitize_density_raw(java_strict_mul(emitted, retention_dt), min_bound, eps);
                            if (next != prev)
                                dirty = true;
                            sections.uniform_density[sidx] = next;
                            active = (next != 0.0);
                            if (active) {

                                const int64_t pk = pocket_key(section_key, 0u);
                                if (fog_enabled && next > world.fog_threshold &&
                                    decay_event_draw(pk, world.last_work_epoch_salt, kDecayDrawFog) <
                                            world.fog_prob_u64) {
                                    out.fog.push_back(pk);
                                }
                                if (destroy_enabled && next >= kDestroyThreshold &&
                                    decay_event_draw(pk, world.last_work_epoch_salt, kDecayDrawDestroy) <
                                            world.destroy_prob_u64) {
                                    out.destroy.push_back(pk);
                                }
                            }
                        } else if (kind == kKindMulti) {
                            auto &multi = ensure_section_multi_state(sections, section_id);
                            auto &density = multi.pocket_density;
                            const auto &volume = multi.pocket_volume;
                            const size_t density_count = density.size();
                            const size_t weighted_count = std::min(density_count, volume.size());
                            double *const density_ptr = density.data();
                            const int32_t *const volume_ptr = volume.data();

                            if (src != nullptr) {
                                for (size_t p = 0; p < density_count; p++) {
                                    const double before = density_ptr[p];
                                    const double after = relax_section_sources(src, static_cast<int32_t>(p), before);
                                    if (after != before) {
                                        density_ptr[p] = after;
                                        dirty = true;
                                    }
                                }
                            }
                            DecayMultiSummary summary;
                            decay_multi_no_events_dispatch(summary, density_ptr, density_count, volume_ptr,
                                                           weighted_count, retention_dt, min_bound, eps);
                            dirty |= summary.dirty;
                            active = summary.active;
                            for (size_t p = 0; p < density_count; p++) {
                                const double next = density_ptr[p];
                                if (next == 0.0)
                                    continue;
                                if (fog_enabled && next > world.fog_threshold &&
                                    decay_event_draw(pocket_key(section_key, static_cast<uint16_t>(p)),
                                                     world.last_work_epoch_salt, kDecayDrawFog) < world.fog_prob_u64) {
                                    out.fog.push_back(pocket_key(section_key, static_cast<uint16_t>(p)));
                                }
                                if (destroy_enabled && next >= kDestroyThreshold &&
                                    decay_event_draw(pocket_key(section_key, static_cast<uint16_t>(p)),
                                                     world.last_work_epoch_salt,
                                                     kDecayDrawDestroy) < world.destroy_prob_u64) {
                                    out.destroy.push_back(pocket_key(section_key, static_cast<uint16_t>(p)));
                                }
                            }
                            sections.uniform_density[sidx] =
                                    (summary.total_open > 0)
                                            ? sanitize_density_raw(summary.total_mass /
                                                                           static_cast<double>(summary.total_open),
                                                                   min_bound, eps)
                                            : 0.0;
                        } else {
                            const auto *multi = section_multi_state_ptr(sections, section_id);
                            if (sections.uniform_density[sidx] != 0.0 ||
                                (multi != nullptr && !multi->pocket_density.empty()))
                                dirty = true;
                            sections.uniform_density[sidx] = 0.0;
                            release_section_multi_state(sections, section_id);
                            active = false;
                        }

                        const bool was_active = (active_mask & bit) != 0u;
                        if ((sections.active_by_id[sidx] != 0u) != active) {
                            sections.active_by_id[sidx] = active ? 1u : 0u;
                        }
                        if (was_active != active) {
                            if (active)
                                active_mask = static_cast<uint32_t>(active_mask | bit);
                            else
                                active_mask = static_cast<uint32_t>(active_mask & ~bit);
                            dirty = true;
                        }
                    }
                }
                if (dirty)
                    mark_chunk_dirty(world, chunk_id);
            }
            return out;
        }

        DecayEventBuffers post_sweep_decay_range_with_events(RadWorld &world, const std::vector<int32_t> &chunk_ids,
                                                             int32_t lo, int32_t hi) {
            if (world.chunks.words_per_chunk == 1) [[likely]] {
                return post_sweep_decay_range_with_events_impl<false>(world, chunk_ids, lo, hi);
            }
            return post_sweep_decay_range_with_events_impl<true>(world, chunk_ids, lo, hi);
        }

        DecayEventBuffers post_sweep_decay_range(RadWorld &world, const std::vector<int32_t> &chunk_ids, int32_t lo,
                                                 int32_t hi, int32_t threshold) {
            if (world_parallel_enabled(world) && hi - lo > threshold) {
                const int32_t mid = (lo + hi) >> 1;
                DecayEventBuffers left;
                DecayEventBuffers right;
                pool::parallel_invoke([&] { left = post_sweep_decay_range(world, chunk_ids, lo, mid, threshold); },
                                      [&] { right = post_sweep_decay_range(world, chunk_ids, mid, hi, threshold); });
                left.merge_from(std::move(right));
                return left;
            }

            const bool fog_enabled = world.fog_prob_u64 != 0u;
            const bool destroy_enabled = world.destroy_prob_u64 != 0u;
            if (!fog_enabled && !destroy_enabled) {
                return post_sweep_decay_range_no_events(world, chunk_ids, lo, hi);
            }
            return post_sweep_decay_range_with_events(world, chunk_ids, lo, hi);
        }
    }

    void post_sweep_decay(RadWorld &world) {
        world.fog_events.clear();
        world.destroy_events.clear();
        if (!(world.retention_dt > 0.0) || !std::isfinite(world.retention_dt))
            return;

        std::array<DecayEventBuffers, 4> bucket_events{};
        maybe_parallel_invoke(
                world,
                [&] {
                    const auto &bucket = world.chunks.parity_bucket_ids[0];
                    bucket_events[0] = post_sweep_decay_range(
                            world, bucket, 0, static_cast<int32_t>(bucket.size()),
                            get_task_threshold(static_cast<int32_t>(bucket.size()), kSplitTaskGrain));
                },
                [&] {
                    const auto &bucket = world.chunks.parity_bucket_ids[1];
                    bucket_events[1] = post_sweep_decay_range(
                            world, bucket, 0, static_cast<int32_t>(bucket.size()),
                            get_task_threshold(static_cast<int32_t>(bucket.size()), kSplitTaskGrain));
                },
                [&] {
                    const auto &bucket = world.chunks.parity_bucket_ids[2];
                    bucket_events[2] = post_sweep_decay_range(
                            world, bucket, 0, static_cast<int32_t>(bucket.size()),
                            get_task_threshold(static_cast<int32_t>(bucket.size()), kSplitTaskGrain));
                },
                [&] {
                    const auto &bucket = world.chunks.parity_bucket_ids[3];
                    bucket_events[3] = post_sweep_decay_range(
                            world, bucket, 0, static_cast<int32_t>(bucket.size()),
                            get_task_threshold(static_cast<int32_t>(bucket.size()), kSplitTaskGrain));
                });

        for (auto &bucket : bucket_events) {
            if (!bucket.fog.empty()) {
                world.fog_events.insert(world.fog_events.end(), std::make_move_iterator(bucket.fog.begin()),
                                        std::make_move_iterator(bucket.fog.end()));
            }
            if (!bucket.destroy.empty()) {
                world.destroy_events.insert(world.destroy_events.end(), std::make_move_iterator(bucket.destroy.begin()),
                                            std::make_move_iterator(bucket.destroy.end()));
            }
        }
    }
}

#undef HBM_RADSIM_DECAY_USE_GCC_SIMD
