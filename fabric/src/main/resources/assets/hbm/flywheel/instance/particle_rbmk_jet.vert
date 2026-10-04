void flw_instanceVertex(in FlwInstance i) {
    mat3 camera = transpose(mat3(flw_view));
    vec3 right = -camera[0];
    float x = mix(i.previousRoll, i.roll, flw_vertexPos.x + .5);
    float y = mix(i.age, i.lifetime, flw_vertexPos.y + .5);
    flw_vertexPos.xyz = i.position + vec3(right.x * (1. + x), camera[1].y + y,
                                          right.z * (1. + x));
    flw_vertexNormal = vec3(0., 1., 0.);
    flw_vertexColor *= i.color;
    flw_vertexLight = max((vec2(i.light) + 8.) / 256., flw_vertexLight);
    flw_vertexTexCoord = i.uvRegion.xy + flw_vertexTexCoord * i.uvRegion.zw;
}
