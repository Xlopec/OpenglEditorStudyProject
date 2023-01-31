#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform float tint;
in vec2 vTexPosition;
out vec4 outColor;

const mat3 RGBtoYIQ = mat3(0.299f, 0.587f, 0.114f, 0.5959f, -0.2746f, -0.3213f, 0.2115f, -0.5227f, 0.3112f);
const mat3 YIQtoRGB = mat3(1.0f, 0.956f, 0.619f, 1.0f, -0.272f, -0.647f, 1.0f, -1.106f, 1.703f);

// https://en.wikipedia.org/wiki/YIQ
void main() {
    vec4 textureColor = texture(uTexture, vTexPosition);
    vec3 yiq = RGBtoYIQ * textureColor.rgb;
    // +- max 10%
    yiq.b = clamp(yiq.b + tint * 0.5226f * 0.1f, -0.5226f, 0.5226f);

    outColor = vec4(YIQtoRGB * yiq, textureColor.a);
}