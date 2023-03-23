#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform float contrast;
in vec2 vTexPosition;
out vec4 outColor;

// see https://ie.nitk.ac.in/blog/2020/01/19/algorithms-for-adjusting-brightness-and-contrast-of-an-image/
void main()
{
    vec4 textureColor = texture(uTexture, vTexPosition);
    float f = (259.0f * (contrast + 255.0f)) / (255.0f * (259.0f - contrast));
    float r = clamp(f * (textureColor.r - 128.0f) + 128.0f, 0.0f, 255.0f);
    float g = clamp(f * (textureColor.g - 128.0f) + 128.0f, 0.0f, 255.0f);
    float b = clamp(f * (textureColor.b - 128.0f) + 128.0f, 0.0f, 255.0f);
    outColor = vec4(r, g, b, textureColor.a);
}