#include "hbm:instance/chain_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    float values[6];
    hbm_chainValues(i.previous, i.current, i.clock, flw_partialTick, flw_gameMillis, values);
    vec3 normal = flw_vertexNormal;
    vec3 position = hbm_chainApply(values, i.steps, i.codes, flw_vertexPos.xyz, normal);
    flw_vertexPos = i.pose * vec4(position, flw_vertexPos.w);
    flw_vertexNormal = transpose(inverse(mat3(i.pose))) * normal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
}
