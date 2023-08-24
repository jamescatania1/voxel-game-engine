#version 440 core

layout (location = 0) in int vertexData;

out vec2 texCoord;

void main()
{
    if(vertexData == 0) {
        gl_Position = vec4(-1.0, 1.0, 0.0, 1.0);
        texCoord = vec2(0.0, 1.0);
    }
    if(vertexData == 1) {
        gl_Position = vec4(-1.0, -1.0, 0.0, 1.0);
        texCoord = vec2(0.0, 0.0);
    }
    if(vertexData == 2) {
        gl_Position = vec4(1.0, 1.0, 0.0, 1.0);
        texCoord = vec2(1.0, 1.0);
    }
    if(vertexData == 3) {
        gl_Position = vec4(1.0, -1.0, 0.0, 1.0);
        texCoord = vec2(1.0, 0.0);
    }
}