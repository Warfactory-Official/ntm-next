#include "hbm:instance/particle_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    hbm_particleVertex(i.previousPosition, i.position, i.previousRoll, i.roll, i.color, i.overlay, vec2(i.light), i.uvRegion, i.size, 1.);
}
