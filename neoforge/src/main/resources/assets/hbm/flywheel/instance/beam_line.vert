#include "hbm:instance/beam_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    int segment = int(flw_vertexPos.x + 0.5);
    bool centerLine = flw_vertexPos.z > 0.5;
    if (!centerLine && segment >= i.count) {
        flw_vertexPos = i.pose * vec4(0., 0., 0., 1.);
        flw_vertexNormal = vec3(0., 1., 0.);
        flw_vertexColor = vec4(0.);
        return;
    }
    vec3 from = centerLine ? vec3(0.0) : hbm_beamJoint(segment, i.length, i.size, i.count, i.phase, i.wave);
    vec3 to = centerLine ? vec3(0.0, i.length, 0.0)
                         : hbm_beamJoint(segment + 1, i.length, i.size, i.count, i.phase, i.wave);
    vec3 start = (i.pose * vec4(from, 1.0)).xyz;
    vec3 end = (i.pose * vec4(to, 1.0)).xyz;
    vec3 delta = end - start;
    float distance = length(delta);
    if (distance < 1e-6) end = start + vec3(0.0, 1.0, 0.0);
    flw_vertexPos = vec4(flw_vertexPos.y < 0.5 ? start : end, 1.0);
    flw_vertexNormal = normalize(end - start);
    flw_vertexColor *= centerLine ? i.innerColor : i.outerColor;
    if (distance < 1e-6) flw_vertexColor.a = 0.0;
}
