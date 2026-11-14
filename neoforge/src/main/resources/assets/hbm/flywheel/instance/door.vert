#include "hbm:instance/door_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    float first, second;
    vec3 origin;
    int millis = clamp(int(flw_gameMillis - i.timing.x), 0, int(i.timing.y));
    hbm_doorChannels(i.timing, i.shapes, i.rest, i.ends, i.values, millis, flw_partialTick, first, second, origin);
    mat4 local;
    vec3 slide;
    vec4 plane;
    hbm_doorPose(i.timing.z & 0xFFu, i.timing.z >> 8u, first, second, origin, local, slide, plane);
    vec3 slid = flw_vertexPos.xyz + slide;
    mat4 pose = i.pose * local;
    flw_vertexPos = pose * vec4(slid, flw_vertexPos.w);
    flw_vertexNormal = transpose(inverse(mat3(pose))) * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    _flw_clipData = vec2(dot(plane.xyz, slid), plane.w);
}
