void flw_instanceVertex(in FlwInstance i) {
    float t = flw_vertexPos.y;
    float side = flw_vertexPos.x;
    flw_vertexPos.xyz = mix(i.position, i.previousPosition, t)
                        + vec3(i.uvRegion.x * side, 0., i.uvRegion.y * side);
    flw_vertexColor.a *= mix(i.uvRegion.z, i.uvRegion.w, t);
    flw_vertexNormal = vec3(0., 1., 0.);
    flw_vertexLight = max((vec2(i.light) + 8.) / 256., flw_vertexLight);
}
