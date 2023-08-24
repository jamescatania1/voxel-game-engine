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

void main()
{
    //retrieve integer bits of attributes
    int _vertexData = floatBitsToInt(vertexData); 
    int _instanceData = floatBitsToInt(instanceData);

    vec3 chunkPos = vec3(
        float(int((_instanceData >> 23u) & 0x1FFu)), 0.0,
        float(int((_instanceData >> 14u) & 0x1FFu))
    );

    gl_Position = sunViewProjMatrix * vec4(vertexPosition + chunkPos, 1.0);
}  