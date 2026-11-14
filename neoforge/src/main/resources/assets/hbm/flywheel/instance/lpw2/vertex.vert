#include "hbm:instance/lpw2/motion.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    vec3 position = flw_vertexPos.xyz;
    vec3 normal = flw_vertexNormal;
    lpw2Transform(instance.base, instance.phase0, instance.phase1, instance.meta,
        instance.data0, instance.data1, instance.law0, instance.law1, position, normal);
    flw_vertexPos = vec4(position, flw_vertexPos.w);
    flw_vertexNormal = normalize(normal);
    flw_vertexColor = vec4(1.0);
    flw_vertexTexCoord.y += instance.phase1.z * instance.meta.w;
}
