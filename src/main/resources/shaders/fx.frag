#version 440 core
#define VIGNETTE_DISTANCE 0.1
#define VIGNETTE_FALLOFF 3.8
#define VIGNETTE_ALPHA 1.0

layout(std140) uniform windowInfo{
    int windowX;
    int windowY;
};

in vec2 texCoord;
out vec4 FragColor;

uniform sampler2D renderColor;
uniform sampler2D blurColor;

uniform bool blurActive;
uniform float blurAmount;

void main()
{
    vec3 baseColor;
    if(blurActive) {
        //sample blur texture with a simple 3x3 tent filter to reduce upscale artifacts
        float x = texCoord.x;
        float y = texCoord.y;
        float dx = 2.0 / windowX;
        float dy = 2.0 / windowY;

        vec3 a_11 = texture(blurColor, vec2(x - dx, y + dy)).rgb;
        vec3 a_12 = texture(blurColor, vec2(x, y + dy)).rgb;
        vec3 a_13 = texture(blurColor, vec2(x + dx, y + dy)).rgb;
        vec3 a_21 = texture(blurColor, vec2(x - dx, y)).rgb;
        vec3 a_22 = texture(blurColor, vec2(x, y)).rgb;
        vec3 a_23 = texture(blurColor, vec2(x + dx, y)).rgb;
        vec3 a_31 = texture(blurColor, vec2(x - dx, y - dy)).rgb;
        vec3 a_32 = texture(blurColor, vec2(x, y - dy)).rgb;
        vec3 a_33 = texture(blurColor, vec2(x + dx, y - dy)).rgb;

        vec3 blur = (a_11 + a_13 + a_31 + a_33) * 0.0625 + (a_12 + a_21 + a_23 + a_32) * 0.125 + a_22 * 0.25;
        baseColor = mix(texture(renderColor, texCoord).rgb, blur, blurAmount);
    }
    else baseColor = texture(renderColor, texCoord).rgb;

    //gamma correction
    baseColor = pow(baseColor, vec3(1.5));

    //vignette
    float aspect = float(windowX) / float(windowY);
    float vx = min(VIGNETTE_DISTANCE, VIGNETTE_DISTANCE / aspect);
    float vy = min(VIGNETTE_DISTANCE, VIGNETTE_DISTANCE * aspect);
    float xAmt = max(0.0, max(vx - texCoord.x, texCoord.x - 1.0 + vx)) * max(1.0, aspect);
    float yAmt = max(0.0, max(vy - texCoord.y, texCoord.y - 1.0 + vy)) * max(1.0, 1.0 / aspect);
    baseColor *= 1.0 - VIGNETTE_ALPHA * pow(clamp(VIGNETTE_FALLOFF * length(vec2(xAmt, yAmt)), 0.0, 1.0), 3.0);

    FragColor = vec4(baseColor, 1.0);
}