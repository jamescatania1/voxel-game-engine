#version 440 core

#define BOTTOM 0
#define TOP 1
#define LEFT 2
#define RIGHT 3
#define BACK 4
#define FRONT 5
#define NORMAL_BOTTOM vec3(0.0, -1.0, 0.0);
#define NORMAL_TOP vec3(0.0, 1.0, 0.0);
#define NORMAL_LEFT vec3(-1.0, 0.0, 0.0);
#define NORMAL_RIGHT vec3(1.0, 0.0, 0.0);
#define NORMAL_BACK vec3(0.0, 0.0, -1.0);
#define NORMAL_FRONT vec3(0.0, 0.0, 1.0);

layout (location = 0) in vec3 vertexPosition;
layout (location = 1) in float vertexData; // color (8 bits) | faceDirection (3 bits) |  ...
layout (location = 2) in float instanceData; // chunk x (9 bits) | chunk z (9 bits) | ...

layout(std140, binding = 0) uniform windowInfo{
    int windowX;
    int windowY;
};

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

layout(std140, binding = 3) uniform colorPalette{
    vec4[256] palette;
};

out float depth;
out vec4 color;
out float ambientOcclusion;
out vec3 position;
out vec4 lightSpacePosition;
out vec2 screenPosition;

flat out vec3 normal;
flat out int faceDirection;

uniform sampler2D windField;

void main()
{
    //retrieve integer bits of attributes
    int _vertexData = floatBitsToInt(vertexData); 
    int _instanceData = floatBitsToInt(instanceData);

    vec3 chunkPos = vec3(
        float(int((_instanceData >> 23u) & 0x1FFu)), 0.0,
        float(int((_instanceData >> 14u) & 0x1FFu))
    );

    position = vertexPosition + chunkPos;
    float windVal = 0.0185 *  texture(windField, vec2(chunkPos.x / 512.0, chunkPos.z / 512.0)).r * (clamp(position.y, 0.4, 1.5) - 0.4) / 1.1;
    position += vec3(windVal, 0.0, -windVal);

    gl_Position = viewProjMatrix * vec4(position, 1.0);

    screenPosition = vec2(gl_Position.x * 0.5 + 0.5, gl_Position.y * 0.5 + 0.5);

    lightSpacePosition = sunViewProjMatrix * vec4(position, 1.0);

    depth = gl_Position.z;

    gl_Position.z = gl_Position.z / 2.0 + 0.5;
    
    color = palette[int((_vertexData >> 24u) & 0xFFu)];

    //face direction and normals
    faceDirection = int((_vertexData >> 21u) & 0x7u);
    if(faceDirection == BOTTOM) normal = NORMAL_BOTTOM;
    if(faceDirection == TOP) normal = NORMAL_TOP;
    if(faceDirection == LEFT) normal = NORMAL_LEFT;
    if(faceDirection == RIGHT) normal = NORMAL_RIGHT;
    if(faceDirection == BACK) normal = NORMAL_BACK;
    if(faceDirection == FRONT) normal = NORMAL_FRONT;

    ambientOcclusion = float(int((_vertexData >> 19u) & 0x3u)) / 3.0;
}