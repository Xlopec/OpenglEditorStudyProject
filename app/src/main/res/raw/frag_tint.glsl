#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform vec4 tint;
in vec2 vTexPosition;
out vec4 outColor;

// see https://en.wikipedia.org/wiki/Alpha_compositing
vec4 rgbOver(vec4 foreground, vec4 background)
{
    float a = foreground.a + background.a * (1.0f - foreground.a);
    return vec4(
    (foreground.r * foreground.a  + background.r * background.a * (1.0f - foreground.a)) / a,
    (foreground.g * foreground.a  + background.g * background.a * (1.0f - foreground.a)) / a,
    (foreground.b * foreground.a  + background.b * background.a * (1.0f - foreground.a)) / a,
    a
    );
}

void main() {
    outColor = rgbOver(tint, texture(uTexture, vTexPosition));
}