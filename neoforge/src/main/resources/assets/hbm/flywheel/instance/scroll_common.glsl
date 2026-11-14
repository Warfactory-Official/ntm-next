

float hbm_scrollValue(vec2 scroll, uvec3 clock, float t, uint millis) {
    if (clock.z == 0u) return scroll.x + t * (scroll.y - scroll.x);
    uint elapsed = millis - clock.x;
    if (clock.y != 0u) elapsed %= clock.y;
    return scroll.x + scroll.y * float(elapsed);
}
