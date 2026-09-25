#include "hbm:instance/particle_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    hbm_particleVertex(i.previousPosition, i.position, i.previousRoll, i.roll, i.color, i.overlay, vec2(i.light), i.uvRegion, i.size * (i.age + flw_partialTick), clamp(1. - (i.age + flw_partialTick) / i.lifetime, 0., 1.));
}
