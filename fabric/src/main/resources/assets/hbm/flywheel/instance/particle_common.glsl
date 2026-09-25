void hbm_particleVertex(vec3 previousPosition, vec3 position, float previousRoll, float currentRoll,
                        vec4 color, ivec2 overlay, vec2 light, vec4 uvRegion, float size, float alpha) {
    float roll = mix(previousRoll, currentRoll, flw_partialTick);
    float c = cos(roll), s = sin(roll);
    vec3 point = flw_vertexPos.xyz * size;
    point.xy = mat2(c, s, -s, c) * point.xy;
    mat3 billboard = transpose(mat3(flw_view));
    flw_vertexPos.xyz = mix(previousPosition, position, flw_partialTick) + billboard * point;
    flw_vertexNormal = billboard * flw_vertexNormal;
    flw_vertexColor *= color * vec4(1., 1., 1., alpha);
    flw_vertexOverlay = overlay;
    flw_vertexLight = max((light + 8.) / 256., flw_vertexLight);
    flw_vertexTexCoord = uvRegion.xy + flw_vertexTexCoord * uvRegion.zw;
}
