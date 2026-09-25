void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    radius = (length(center) + radius) * abs(i.size * (i.age + 1.)) + length(i.position - i.previousPosition) * .5;
    center = (i.position + i.previousPosition) * .5;
}
