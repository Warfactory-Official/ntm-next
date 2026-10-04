void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    center = (i.position + i.previousPosition) * .5;
    radius = length(i.previousPosition - i.position) * .5 + length(i.uvRegion.xy);
}
