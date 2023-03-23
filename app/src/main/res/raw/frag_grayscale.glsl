#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform float grayscale;
in vec2 vTexPosition;
out vec4 grayscaledColor;

void main()
{
    vec4 textureColor = texture(uTexture, vTexPosition);
    float baseGrayscale = 0.3 * textureColor.r + 0.59 * textureColor.g + 0.11 * textureColor.b;
    float r = baseGrayscale + grayscale * (0.7 * textureColor.r - 0.59 * textureColor.g - 0.11 * textureColor.b);
    float g = baseGrayscale + grayscale * (-0.3 * textureColor.r + 0.41 * textureColor.g - 0.11 * textureColor.b);
    float b = baseGrayscale + grayscale * (-0.3 * textureColor.r - 0.59 * textureColor.g + 0.89 * textureColor.b);
    grayscaledColor = vec4(r, g, b, textureColor.a);
}