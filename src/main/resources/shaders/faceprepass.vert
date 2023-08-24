#version 440 core
layout (location = 0) in vec3 vertexPosition;
layout (location = 1) in float vertexData; // color (8 bits) | faceDirection (3 bits) |  ...
layout (location = 2) in float instanceData; // chunk x (8 bits) | chunk z (8 bits) | ...

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

flat out int faceDirection;

void main()
{
    //retrieve integer bits of attributes
    int _vertexData = floatBitsToInt(vertexData); 
    int _instanceData = floatBitsToInt(instanceData);

    vec3 chunkPos = vec3(
        float(int((_instanceData >> 24u) & 0xFFu)), 0.0,
        float(int((_instanceData >> 16u) & 0xFFu))
    );

    vec3 position = vertexPosition + chunkPos;

    gl_Position = viewProjMatrix * vec4(position, 1.0);
    gl_Position.z = gl_Position.z / 2.0 + 0.5;

    faceDirection = int((_vertexData >> 21u) & 0x7u);
}  