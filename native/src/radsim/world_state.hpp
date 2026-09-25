// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <memory>
#include <span>
#include <string>
#include <unordered_map>
#include <vector>

namespace hbm::radsim::detail {
    constexpr int kMaskWordsPerSection = 64;
    constexpr int32_t kChunkWordSections = 32;

    constexpr int32_t kMaxChunkSections = 254;

    constexpr int32_t kSectionVoxelCount = 4096;
    constexpr int32_t kNoNeighbor = -1;
    constexpr int32_t kNoIndex = -1;
    constexpr uint8_t kNoBucket = 0xFFu;
    constexpr uint16_t kNoPocket = std::numeric_limits<uint16_t>::max();
    constexpr uint16_t kSentinelPocket = std::numeric_limits<uint16_t>::max();
    constexpr int64_t kInvalidChunkKey = std::numeric_limits<int64_t>::min();
    constexpr int64_t kInvalidSectionKey = std::numeric_limits<int64_t>::min();
    constexpr double kDestroyThreshold = 5.0;
    constexpr double kUniformInvVolume = 1.0 / kSectionVoxelCount;

    constexpr size_t kEventHeaderBytes = 16u;

    constexpr uint8_t kRelinkFaceEast = 1u;
    constexpr uint8_t kRelinkFaceSouth = 2u;
    constexpr uint8_t kRelinkFaceUp = 4u;
    constexpr uint8_t kRelinkFaceAll = 7u;

    constexpr uint8_t kRelinkRepack = 8u;

    constexpr int32_t kMinTaskGrain = 64;

    constexpr int32_t kSplitTaskGrain = 256;

    constexpr uint8_t kColumnPathForceScalar = 1u;
    constexpr uint8_t kColumnPathDisabled = 2u;

    constexpr uint8_t kColumnPathForce256 = 4u;

    constexpr uint8_t kColumnPathForceAvx = 8u;

    constexpr uint8_t kMultiUniformForceScalar = 16u;

    constexpr uint8_t kPathFlagMask = kColumnPathForceScalar | kColumnPathDisabled | kColumnPathForce256 |
                                      kColumnPathForceAvx | kMultiUniformForceScalar;

    constexpr uint8_t kKindNone = 0u;
    constexpr uint8_t kKindUni = 1u;
    constexpr uint8_t kKindSingle = 2u;
    constexpr uint8_t kKindMulti = 3u;

    using SectionMaskWords = std::array<uint64_t, kMaskWordsPerSection>;
    using SectionPocketMap = std::array<uint16_t, kSectionVoxelCount>;

    constexpr uint8_t kEditFlagHasSet = 1u;
    constexpr uint8_t kEditFlagHasSaturation = 2u;

    constexpr size_t kEditWireBytes = 48u;
    constexpr int32_t kPermOverrideFlag = 1 << 31;
    constexpr int32_t kPermMaskFlipX = 1 << 0;
    constexpr int32_t kPermMaskFlipZ = 1 << 1;
    constexpr int32_t kPermMaskYParity = 1 << 2;
    constexpr int32_t kPermShiftOrder = 3;
    constexpr int32_t kPermMaskOrder = 0x7 << kPermShiftOrder;
    constexpr size_t kFaceCellCount = 256u;
    constexpr int32_t kSectionAxisCoordSum = 30720;
    constexpr size_t kStepProfilePhaseCount = 8u;
    constexpr size_t kStepProfileDirtyRebuild = 0u;
    constexpr size_t kStepProfileApplyEdits = 1u;
    constexpr size_t kStepProfileRelink = 2u;
    constexpr size_t kStepProfileTransport = 3u;
    constexpr size_t kStepProfileDecay = 4u;
    constexpr size_t kStepProfileValidate = 5u;
    constexpr size_t kStepProfileSerialize = 6u;
    constexpr size_t kStepProfileTotal = 7u;

    struct ChunkCoord {
        int32_t x = 0;
        int32_t z = 0;
    };

    consteval std::array<std::array<uint16_t, kFaceCellCount>, 6> make_face_local_table() {
        std::array<std::array<uint16_t, kFaceCellCount>, 6> table{};
        for (int face = 0; face < 6; face++) {
            int index = 0;
            for (int row = 0; row < 16; row++) {
                for (int col = 0; col < 16; col++) {
                    uint16_t local = 0;
                    switch (face) {
                    case 0:
                        local = static_cast<uint16_t>((row << 4) | col);
                        break;
                    case 1:
                        local = static_cast<uint16_t>((15 << 8) | (row << 4) | col);
                        break;
                    case 2:
                        local = static_cast<uint16_t>((row << 8) | col);
                        break;
                    case 3:
                        local = static_cast<uint16_t>((row << 8) | (15 << 4) | col);
                        break;
                    case 4:
                        local = static_cast<uint16_t>((row << 8) | (col << 4));
                        break;
                    case 5:
                        local = static_cast<uint16_t>((row << 8) | (col << 4) | 15);
                        break;
                    default:
                        break;
                    }
                    table[static_cast<size_t>(face)][static_cast<size_t>(index++)] = local;
                }
            }
        }
        return table;
    }

    inline constexpr auto kFaceLocalTable = make_face_local_table();

    struct ChunkTable {

        int32_t buffer_generation = 0;
        std::vector<int64_t> ck_by_id;
        std::vector<int32_t> east_neighbor_id;
        std::vector<int32_t> south_neighbor_id;
        std::vector<uint64_t> kinds_by_id;
        std::vector<uint32_t> active_mask_by_id;

        std::vector<uint32_t> source_mask_by_id;
        std::vector<uint8_t> dirty_by_id;
        std::vector<uint8_t> dirty_event_pending_by_id;
        std::vector<uint8_t> loaded_by_id;
        std::vector<uint8_t> bucket_by_id;
        std::vector<int32_t> bucket_index_by_id;

        std::vector<int32_t> section_id_by_sy;

        int32_t sections_per_chunk = kChunkWordSections;
        int32_t words_per_chunk = 1;
        std::vector<int32_t> free_ids;
        std::unordered_map<int64_t, int32_t> coord_to_id;
        std::array<std::vector<int32_t>, 4> parity_bucket_ids{};
    };

    struct PendingLocalEdit {
        uint16_t local = 0;
        uint8_t flags = 0;
        double add_value = 0.0;
        double set_value = 0.0;
        uint64_t set_seq = 0u;

        double saturation = 0.0;
    };

    struct SectionSourceEntry {
        uint16_t pocket = 0u;
        int32_t count = 0;
        double emission = 0.0;
        double saturation = 0.0;
    };

    struct SectionSourceState {
        std::vector<SectionSourceEntry> entries;
    };

    struct EdgeLink {
        uint16_t my_pocket = 0u;
        uint16_t nei_pocket = 0u;
        uint16_t area = 0u;
    };

    struct FaceEdges {
        const float *face_distance = nullptr;
        const double *inv_volume = nullptr;
        const uint16_t *pocket = nullptr;
        const uint16_t *nei_pocket = nullptr;
        const uint16_t *area = nullptr;
        const double *exchange_e = nullptr;
        const double *uni_share = nullptr;
        const double *pocket_share = nullptr;
        const float *diffusivity = nullptr;
        size_t count = 0u;

        [[nodiscard]] bool empty() const noexcept { return count == 0u; }
        [[nodiscard]] size_t size() const noexcept { return count; }

        [[nodiscard]] double diffusivity_at(size_t i) const noexcept {
            return diffusivity != nullptr ? static_cast<double>(diffusivity[i]) : 1.0;
        }
    };

    struct MultiSectionState {
        std::vector<int32_t> pocket_volume;
        std::vector<double> pocket_density;
        std::array<uint16_t, 6> edge_counts{};
        std::array<std::vector<EdgeLink>, 6> edges_by_face;
        std::vector<float> edge_face_distance;
        std::vector<double> edge_inv_volume;
        std::vector<uint16_t> edge_pocket;
        std::vector<uint16_t> edge_nei_pocket;
        std::vector<uint16_t> edge_area;

        std::vector<double> edge_exchange_e;

        std::vector<double> edge_uni_share;
        std::vector<double> edge_pocket_share;
        std::vector<float> edge_diffusivity;

        [[gnu::always_inline]] void clear_edge_streams() noexcept {
            edge_face_distance.clear();
            edge_exchange_e.clear();
            edge_uni_share.clear();
            edge_pocket_share.clear();
            edge_inv_volume.clear();
            edge_pocket.clear();
            edge_nei_pocket.clear();
            edge_area.clear();
            edge_diffusivity.clear();
        }

        std::array<uint32_t, 6> edge_offset{};
        std::vector<float> face_distance;
        std::vector<float> pocket_diffusivity;
        std::vector<double> pocket_inv_volume;
    };

    struct PendingSectionState {
        std::unordered_map<uint16_t, uint64_t> density_bits;
        std::vector<PendingLocalEdit> local_edits;
        std::vector<float> pocket_diffusivity;
        uint8_t touched = 0u;
    };

    struct SectionTable {
        int32_t buffer_generation = 0;
        std::vector<int64_t> key_by_id;
        std::vector<int32_t> owner_chunk_id;
        std::vector<uint8_t> sy_by_id;
        std::vector<uint8_t> kind_by_id;
        std::vector<uint8_t> active_by_id;
        std::vector<double> uniform_density;
        std::vector<float> uniform_diffusivity;
        std::vector<uint64_t> mask_checksum;
        std::vector<std::unique_ptr<SectionMaskWords>> resistant_masks;
        std::vector<uint8_t> has_resistant_mask;
        std::vector<uint16_t> pocket_count;
        std::vector<std::unique_ptr<SectionPocketMap>> pocket_by_local;
        std::vector<int32_t> single_volume;
        std::vector<std::array<uint16_t, 6>> single_face_open_counts;
        std::vector<std::array<double, 6>> single_face_distance;
        std::vector<float> single_diffusivity;
        std::vector<double> single_inv_volume;
        std::vector<std::array<uint16_t, 6>> single_conn_counts;
        std::vector<std::unique_ptr<MultiSectionState>> multi_state;

        std::vector<std::unique_ptr<SectionSourceState>> source_state;

        std::vector<std::unique_ptr<PendingSectionState>> pending_state;
        std::vector<int32_t> touched_section_ids;

        std::vector<uint8_t> topology_dirty;
        std::vector<int32_t> topology_dirty_section_ids;

        std::unordered_map<int64_t, int32_t> key_to_id;
    };

    struct RadWorld {
        int32_t dim = 0;
        int64_t seed = 0;
        double min_bound = 0.0;

        double diffusion_dt = 0.0;
        double uniform_exchange = 0.0;
        double retention_dt = 0.0;
        uint64_t fog_prob_u64 = 0u;
        uint64_t destroy_prob_u64 = 0u;
        double fog_threshold = 0.0;
        double eps = 0.0;
        double max_value = 0.0;
        uint8_t diffusion_transport_enabled = 0u;

        uint8_t column_path_flags = 0u;

        uint8_t bypass_coefficient_cache = 0u;

        uint64_t last_work_epoch_salt = 0u;
        int32_t last_work_epoch = 0;
        int32_t last_perm_bits = 0;
        uint64_t dirty_submit_count = 0u;
        uint64_t edit_submit_count = 0u;
        uint64_t chunk_load_count = 0u;
        uint64_t chunk_unload_count = 0u;
        uint64_t chunk_remove_count = 0u;
        uint64_t dump_validation_failures = 0u;
        uint64_t dirty_decode_failures = 0u;
        uint64_t edit_decode_failures = 0u;

        uint64_t load_decode_failures = 0u;

        uint64_t diffusivity_pocket_mismatches = 0u;
        uint64_t step_count = 0u;
        uint32_t last_step_pending_sections = 0u;
        uint32_t last_step_pending_edits = 0u;
        uint8_t last_step_links_dirty_before = 0u;
        uint8_t last_step_links_rebuilt = 0u;
        uint8_t last_step_pair_lists_dirty_before = 0u;
        uint8_t last_step_pair_lists_rebuilt = 0u;
        uint8_t links_dirty = 1u;

        std::vector<uint8_t> relink_flags;
        std::vector<int32_t> relink_slots;

        uint8_t links_full_dirty = 1u;
        uint32_t last_step_relink_slots = 0u;
        uint8_t pair_lists_dirty = 1u;

        ChunkTable chunks{};
        SectionTable sections{};
        std::array<std::vector<int32_t>, 4> x_pair_a_by_bucket{};
        std::array<std::vector<int32_t>, 4> x_pair_b_by_bucket{};
        std::array<std::vector<int32_t>, 4> z_pair_a_by_bucket{};
        std::array<std::vector<int32_t>, 4> z_pair_b_by_bucket{};

        struct AxisRuns {
            std::vector<int32_t> a;
            std::vector<int32_t> b;
            std::vector<int32_t> start;
            std::vector<int32_t> mid;
            std::vector<int32_t> end;
        };

        AxisRuns x_runs;
        AxisRuns z_runs;

        std::array<int32_t, 4> x_pair_counts{};
        std::array<int32_t, 4> z_pair_counts{};
        std::vector<int64_t> fog_events;
        std::vector<int64_t> destroy_events;
        std::vector<int64_t> dirty_chunk_events;
        int64_t last_dump_invalid_ck = kInvalidChunkKey;
        std::string last_dump_validation_error;
        int64_t last_dump_snapshot_ck = kInvalidChunkKey;
        std::vector<uint32_t> last_dump_snapshot_sypi;
        std::vector<double> last_dump_snapshot_density;
        std::array<uint64_t, kStepProfilePhaseCount> last_step_profile_nanos{};
    };

    struct PairSpan {
        std::span<const int32_t> a;
        std::span<const int32_t> b;

        [[nodiscard]] int32_t count() const noexcept { return static_cast<int32_t>(std::min(a.size(), b.size())); }
    };
}
