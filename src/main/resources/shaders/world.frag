#version 440 core


#define BOTTOM 0
#define TOP 1
#define LEFT 2
#define RIGHT 3
#define BACK 4
#define FRONT 5

#define LIGHT_BOTTOM 0.6
#define LIGHT_TOP 1.0
#define LIGHT_LEFT 0.8
#define LIGHT_RIGHT 0.7
#define LIGHT_BACK 0.9
#define LIGHT_FRONT 0.7

#define SUN_COLOR vec3(1.0, 0.952, 0.788)
#define MOON_COLOR vec3(0.701, 0.831, 1.0)

layout(std140, binding = 2) uniform cameraInfo{
    mat4 viewMatrix;
    mat4 projMatrix;
    mat4 viewProjMatrix;
    mat4 sunViewProjMatrix;
    float zNear;
    float zFar;
    float cameraDistance;
    float aspectRatio;
};

layout(std140, binding = 4) uniform lightingInfo{
    vec3 sunDirection;
    vec3 moonDirection;
    vec3 activeLightDirection;
    vec3 sunColor;
    vec3 moonColor;
    float dayNightCycle;
    float activeShadowStrength;
    float activeShadowBackLeftStrength;
    float activeShadowOffset;
    float activeShadowBackLeftOffset;
    float lightExposure;
    float lightColorSaturation;
};

layout(std140, binding = 5) uniform shadowmapData{
    int kernelShellCount;
    int poissonSampleCount;
    float kernelShellSize;
    vec4 poissonSamples[40];
};

out vec4 FragColor;

in float depth;
in vec4 color;
in float ambientOcclusion;
in vec3 position;
in vec4 lightSpacePosition;
in vec2 screenPosition;

flat in vec3 normal;
flat in int faceDirection;

uniform sampler2D shadowmap;

/**
 * Pesudorandom generator used in shadow sampling.
 * Extended to 3-Dimensions from: https://thebookofshaders.com/10/
 */
float random( vec3 p )
{
    vec3 K1 = vec3(
        23.14069263277926, // e^pi (Gelfond's constant)
        0.12345678910111, // Champernowne constant
        2.665144142690225 // 2^sqrt(2) (Gelfondâ€“Schneider constant)
    );
    return fract( cos( dot(p,K1) ) * 12345.6789 );
}

float calculateShadow(){
    float bias = -0.00001;

    vec3 projCoords = lightSpacePosition.xyz;
    projCoords = projCoords * 0.5 + 0.5;

    //if(projCoords.x < 0.0 || projCoords.x > 1.0 || projCoords.y < 0.0 || projCoords.y > 1.0) return 2.0;
    //else return 2.0;

    float texelSize = 1.0 / textureSize(shadowmap, 0).x;
    float result = 0.0;

    for(int i = kernelShellCount - 1; i >= 0; i--){
        float shellResult = 0.0;
        for(int k = 0; k < poissonSampleCount; k++){
            int m = (i * poissonSampleCount + k);
            vec2 s = m % 2 == 0 ? poissonSamples[m / 2].xy : poissonSamples[m / 2].zw;
            vec2 offset = s * texelSize * kernelShellSize * float(i + 1);

            float sampleDepth = texture(shadowmap, projCoords.xy + offset).r;

            result += (projCoords.z - bias > sampleDepth) ? 1.0 : 0.0;

            //if( s.x == 0.0 && s.y == 0.0) return 2.0;
            //s += (vec2(random(position + vec3(k)), random(position - vec3(k))) - vec2(1.0)) / 2.0;
        }
        if(i == kernelShellCount - 1 && result == 0.0) return 0.0;
        if(i == kernelShellCount - 1 && result == float(poissonSampleCount)) return 1.0;
    }
    result /= float(kernelShellCount * poissonSampleCount);

    return result;
}

void main()
{
    float sunVisibility = clamp(clamp(sunDirection.y + 0.15, 0.0, 1.0) * 4.0 + 0.0, 0.0, 1.0);
    float moonVisibility = clamp(clamp(moonDirection.y + 0.15, 0.0, 1.0) * 4.0 + 0.0, 0.0, 1.0);

    float sunDiffuse = max(dot(normal, sunDirection), 0.0);
    sunDiffuse = (-1.0 / pow(sunDiffuse + 1.0, 3.0)) + 1.0;
    sunDiffuse *= sunVisibility * 2.0;

    float moonDiffuse = max(dot(normal, moonDirection), 0.0);
    moonDiffuse = (-1.0 / pow(moonDiffuse + 1.0, 3.0)) + 1.0;
    moonDiffuse *= moonVisibility * 0.65;

    float sunLuminance = dot(SUN_COLOR, vec3(0.2126, 0.7152, 0.0722));
    vec3 adjSunColor = mix(vec3(sunLuminance), SUN_COLOR, lightColorSaturation);
    float moonLuminance = dot(MOON_COLOR, vec3(0.2126, 0.7152, 0.0722));
    vec3 adjMoonColor = mix(vec3(moonLuminance), MOON_COLOR, lightColorSaturation);

    vec3 skyDiffuse = adjMoonColor * moonDiffuse + adjSunColor * sunDiffuse;
    skyDiffuse *= 3.4;
    skyDiffuse = skyDiffuse + vec3(0.02);
    
    float occlusionFactor = clamp(0.5 * sin(3.14159 * 0.5 * ambientOcclusion - 3.14159 * 0.5) + 0.5, 0.0, 1.0);
    float occlusion = 1.0 - 0.95 * occlusionFactor;

    float shadowFactor = (dot(normal, activeLightDirection) >= 0.0 ? calculateShadow() : 1.0);
    //float shadowFactor = calculateShadow();
    //shadowFactor = (dot(normal, activeLightDirection) >= 0.0 ? shadowFactor : 1.0);
    
    float shadowStrength = activeShadowStrength;
    float shadowOffset = activeShadowOffset;
    if(faceDirection == LEFT || faceDirection == BACK) {
        shadowStrength *= activeShadowBackLeftStrength;
        shadowOffset += activeShadowBackLeftOffset;
    }

    float shadow = 1.0 - 1.0 * (clamp(shadowFactor + shadowOffset, 0.0, 1.0)) * shadowStrength;
    
    //skyDiffuse = vec3(1.0);
    FragColor = vec4((color.xyz * skyDiffuse) * occlusion * shadow, color.w);
    if(shadowFactor > 1.5) FragColor = vec4(1.25, 0.0, 0.0, 1.0);
    if(shadowFactor < -1.5) FragColor = vec4(0.0, 1.25, 0.0, 1.0);

    //if(faceDirection == LEFT) FragColor = vec4(1.0, 0.0, 0.0, 1.0);
}