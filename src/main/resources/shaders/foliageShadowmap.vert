#version 440 core
layout (location = 0) in vec3 vertexPosition;
layout (location = 1) in float vertexData; // color (8 bits) | faceDirection (3 bits) |  ...
layout (location = 2) in float instanceData; // chunk x (9 bits) | chunk z (9 bits) | ...

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

    vec3 position = vertexPosition + chunkPos;
    float windVal = 0.0175 *  texture(windField, vec2(chunkPos.x / 512.0, chunkPos.z / 512.0)).r * (clamp(position.y, 0.3, 1.5) - 0.3) / 1.2;
    position += vec3(windVal, 0.0, -windVal);

    gl_Position = sunViewProjMatrix * vec4(position, 1.0);
}  