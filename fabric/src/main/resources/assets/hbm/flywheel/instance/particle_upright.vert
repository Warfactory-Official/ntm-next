#include "hbm:instance/particle_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    float worldHeight = flw_vertexPos.y * i.size;
    hbm_particleVertex(i.previousPosition, i.position, i.previousRoll, i.roll, i.color,
                       i.overlay, vec2(i.light), i.uvRegion, i.size, 1.);
    flw_vertexPos.y = mix(i.previousPosition.y, i.position.y, flw_partialTick) + worldHeight;
}
