#include "hbm:instance/lpw2/motion.glsl"

void flw_transformBoundingSphere(in FlwInstance instance, inout vec3 center, inout float radius) {
    vec3 normal = vec3(0.0);
    radius *= lpw2Transform(instance.base, instance.phase0, instance.phase1, instance.meta,
        instance.data0, instance.data1, instance.law0, instance.law1, center, normal);
}
