#include "hbm:instance/beam_common.glsl"

void flw_instanceVertex(in FlwInstance i) {
    int segment = int(flw_vertexTexCoord.y + 0.5);
    flw_vertexTexCoord.y = 0.;
    if (segment >= i.count) {
        flw_vertexPos = i.pose * vec4(0., 0., 0., 1.);
        flw_vertexColor = vec4(0.);
        return;
    }
    int joint = int(flw_vertexPos.y + 0.5);
    vec3 point = hbm_beamJoint(joint, i.length, i.size, i.count, i.phase, i.wave);
    point.xz += flw_vertexPos.xz * i.thickness;
    flw_vertexPos = i.pose * vec4(point, 1.0);
    flw_vertexColor.rgb *= hbm_beamColor(i.outerColor.rgb, i.innerColor.rgb, flw_vertexTexCoord.x);
    flw_vertexColor.a *= i.outerColor.a;
}
