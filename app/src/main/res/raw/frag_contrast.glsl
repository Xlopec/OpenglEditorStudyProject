#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform float contrast;
in vec2 vTexPosition;
out vec4 outColor;

#define M_SHIFT 259.0 / 255.0

// see https://ie.nitk.ac.in/blog/2020/01/19/algorithms-for-adjusting-brightness-and-contrast-of-an-image/
void main()
{
    vec4 textureColor = texture(uTexture, vTexPosition);
    float f = (M_SHIFT * (contrast + 1.0f)) / (1.0f * (M_SHIFT - contrast));
    float r = clamp(f * (textureColor.r - 0.5f) + 0.5f, 0.0f, 1.0f);
    float g = clamp(f * (textureColor.g - 0.5f) + 0.5f, 0.0f, 1.0f);
    float b = clamp(f * (textureColor.b - 0.5f) + 0.5f, 0.0f, 1.0f);
    outColor = vec4(r, g, b, textureColor.a);
}