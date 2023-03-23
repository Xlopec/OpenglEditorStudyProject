precision mediump float;
uniform sampler2D uTexture;
uniform float grayscale;
varying vec2 vTexPosition;

void main() {
    vec4 textureColor = texture2D(uTexture, vTexPosition);
    float baseGrayscale = 0.3 * textureColor.r + 0.59 * textureColor.g + 0.11 * textureColor.b;
    float r = baseGrayscale + grayscale * (0.7 * textureColor.r - 0.59 * textureColor.g - 0.11 * textureColor.b);
    float g = baseGrayscale + grayscale * (-0.3 * textureColor.r + 0.41 * textureColor.g - 0.11 * textureColor.b);
    float b = baseGrayscale + grayscale * (-0.3 * textureColor.r - 0.59 * textureColor.g + 0.89 * textureColor.b);
    gl_FragColor = vec4(r, g, b, textureColor.a);
}