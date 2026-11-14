void flw_instanceVertex(in FlwInstance i) {
    flw_vertexPos = i.pose * flw_vertexPos;
    flw_vertexNormal = vec3(0.0, 1.0, 0.0);
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    flw_vertexTexCoord = i.uvRegion.xy + flw_vertexTexCoord * i.uvRegion.zw + flw_vertexTexCoord.yx * i.uvShear;
}
