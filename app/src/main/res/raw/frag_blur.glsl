#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform int radius;
uniform int sigma;
// [0f..1f]
in vec2 vTexPosition;
out vec4 outColor;

#define M_PI 3.1415926535897932384626433832795
#define M_E 2.718281828459045

vec3 pixelAt(const int x, const int y, in vec2 fragCoord, in ivec2 texSize)
{
    vec2 xy = fragCoord.xy + vec2(float(x) / float(texSize.x), float(y) / float(texSize.y));

    return texture(uTexture, xy).rgb;
}

float gaussian(int sigma, int x, int y)
{
    return (1.0f / (2.0f * M_PI * pow(float(sigma), 2.0f))) * pow(M_E, -(pow(float(x), 2.0f) + pow(float(y), 2.0f)) / (2.0f * pow(float(sigma), 2.0f)));
}

// https://en.wikipedia.org/wiki/Gaussian_blur
void main() {
    if (radius == 0 || sigma == 0) {
        outColor = texture(uTexture, vTexPosition);
    } else {
        float kernelSum = 0.0f;
        vec3 outRgb = vec3(0.0f);
        ivec2 texSize = textureSize(uTexture, 2);

        for (int x = -radius; x < radius + 1; ++x)
        {
            for (int y = -radius; y < radius + 1; ++y)
            {
                float G = gaussian(sigma, x, y);
                kernelSum += G;
                outRgb += G * pixelAt(x, y, vTexPosition, texSize);
            }
        }
        // normalize result by kernelSum
        outRgb = outRgb / kernelSum;
        outColor = vec4(outRgb, texture(uTexture, vTexPosition).a);
    }
}