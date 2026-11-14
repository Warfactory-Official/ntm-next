#include "hbm:instance/scroll_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    flw_vertexPos = i.pose * flw_vertexPos;
    flw_vertexNormal = transpose(inverse(mat3(i.pose))) * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    float value = hbm_scrollValue(i.scroll.xy, i.clock, flw_partialTick, flw_gameMillis);
    flw_vertexTexCoord = i.uvRegion.xy + i.scroll.zw * value + flw_vertexTexCoord * i.uvRegion.zw;
}
