#version 330



#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

#ifndef ALPHA_CUTOUT
#define ALPHA_CUTOUT 0.1
#endif

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 0.0 || color.a < ALPHA_CUTOUT) {
        discard;
    }
    fragColor = color;
}
