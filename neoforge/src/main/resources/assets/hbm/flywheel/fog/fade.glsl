
vec4 flw_fogFilter(vec4 color, float sphericalDistance, float cylindricalDistance) {
    return color * (1.0 - total_fog_value(sphericalDistance, cylindricalDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd));
}
