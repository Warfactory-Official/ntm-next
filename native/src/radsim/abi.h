// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include <cstddef>
#include <cstdint>

#include "export.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RAD_BUILD_REPRODUCE_JAVA_MATH = 1 << 0,
    RAD_BUILD_VALIDATE_EVERY_STEP = 1 << 1,

    RAD_BUILD_DEBUG_DUMP = 1 << 2,
    RAD_BUILD_DIFFUSIVITY_TRANSPORT = 1 << 3
};

NTM_NATIVE_EXPORT int32_t rad_build_flags(void);

NTM_NATIVE_EXPORT void rad_configure_runtime(int32_t use_tbb, int32_t threads);

NTM_NATIVE_EXPORT uint64_t rad_world_create(int32_t dim, int64_t seed, double min_bound, int32_t sections_per_chunk);

NTM_NATIVE_EXPORT void rad_submit_section_sources(uint64_t handle, int32_t section_count, const int64_t *packed_keys,
                                                  const int32_t *entry_counts, const uint16_t *pockets,
                                                  const int32_t *multiplicities, const double *emissions,
                                                  const double *saturations);

NTM_NATIVE_EXPORT void rad_submit_section_diffusivity(uint64_t handle, int32_t section_count,
                                                      const int64_t *packed_keys, const int32_t *pocket_counts,
                                                      const float *values);

NTM_NATIVE_EXPORT void rad_world_destroy(uint64_t handle);

NTM_NATIVE_EXPORT void rad_world_set_params(uint64_t handle, double diffusion_dt, double uniform_exchange,
                                            double retention_dt, uint64_t fog_prob_u64, uint64_t destroy_prob_u64,
                                            double fog_threshold, double eps, double max_value);

NTM_NATIVE_EXPORT void rad_world_set_feature_flags(uint64_t handle, int32_t flags);

NTM_NATIVE_EXPORT void rad_chunk_loaded(uint64_t handle, int64_t ck);

NTM_NATIVE_EXPORT void rad_chunk_unloaded(uint64_t handle, int64_t ck);

NTM_NATIVE_EXPORT void rad_chunk_removed(uint64_t handle, int64_t ck);

NTM_NATIVE_EXPORT void rad_ack_chunk_dirty(uint64_t handle, int64_t ck);

NTM_NATIVE_EXPORT void rad_submit_dirty_sections(uint64_t handle, int32_t count, const int64_t *packed_keys,
                                                 const uint64_t *masks_words);

NTM_NATIVE_EXPORT void rad_submit_edits(uint64_t handle, int32_t count, const void *edits_buffer,
                                        size_t edits_buffer_bytes);

NTM_NATIVE_EXPORT void rad_load_pending_entries(uint64_t handle, int64_t ck, int32_t entry_count,
                                                const uint32_t *sypi_entries, const double *density_entries);

NTM_NATIVE_EXPORT int32_t rad_step(uint64_t handle, uint64_t work_epoch_salt, int32_t work_epoch, int32_t perm_bits,
                                   void *out_events_buffer, size_t out_events_capacity_bytes);

NTM_NATIVE_EXPORT int32_t rad_dump_chunk_entries(uint64_t handle, int64_t ck, void *out_sypi_buffer,
                                                 size_t out_sypi_capacity_bytes, void *out_density_buffer,
                                                 size_t out_density_capacity_bytes);

enum {
    RAD_BUF_SECTION_UNIFORM_DENSITY = 0,
    RAD_BUF_SECTION_KIND = 1,
    RAD_BUF_SECTION_ACTIVE = 2,
    RAD_BUF_CHUNK_SECTION_ID_BY_SY = 3,
    RAD_BUF_CHUNK_KINDS = 4,
    RAD_BUF_CHUNK_ACTIVE_MASK = 5
};

NTM_NATIVE_EXPORT int32_t rad_chunk_id(uint64_t handle, int64_t ck);

NTM_NATIVE_EXPORT uint64_t rad_buffer_ptr(uint64_t handle, int32_t which);

NTM_NATIVE_EXPORT int64_t rad_buffer_count(uint64_t handle, int32_t which);

NTM_NATIVE_EXPORT int32_t rad_buffer_stride(uint64_t handle, int32_t which);

NTM_NATIVE_EXPORT int32_t rad_buffer_generation(uint64_t handle);

NTM_NATIVE_EXPORT int32_t rad_sections_per_chunk(void);

NTM_NATIVE_EXPORT double rad_query_local_density(uint64_t handle, int64_t section_key, int32_t local);

NTM_NATIVE_EXPORT int32_t rad_world_sections_per_chunk(uint64_t handle);

#ifdef __cplusplus
}
#endif
