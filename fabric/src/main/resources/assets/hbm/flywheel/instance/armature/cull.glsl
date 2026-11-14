void flw_transformBoundingSphere(in FlwInstance instance, inout vec3 center, inout float radius) {
    center = instance.sphere.xyz;
    radius = instance.sphere.w;
}