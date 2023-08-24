#version 440 core
layout (location = 0) in vec2 vertexPosition;
layout (location = 1) in uint vertexColor; // red (7 bits) | green (7 bits) | blue (7 bits) | alpha (11 bits)
layout (location = 2) in vec2 textureCoords;
layout (location = 3) in uint textData; 
// clipRectsEnabled (1 bit) | cull (1 bit) | cullRect_1 (6 bits) | cullRect_2 (6 bits) | cullRect_3 (6 bits)
// | cullRectInside_1 (1 bit) | cullRectInside_2 (1 bit) | cullRectInside_3 (1 bit) | layer (7 bits) | 

out vec4 color;
out vec2 texCoord;
out vec2 scPos;
flat out int cull;
flat out int clipRectsEnabled;
flat out ivec3 cullRectIndicies;
flat out ivec3 cullRectInside;

void main()
{
    gl_Position = vec4(vertexPosition, 0.0 - float(int((textData >> 1u) & 0xFFu)) / 128.0, 1.0);
    
    color = vec4(
        float(int((vertexColor >> 25u) & 0x7Fu)) / 128.0, 
        float(int((vertexColor >> 18u) & 0x7Fu)) / 128.0, 
        float(int((vertexColor >> 11u) & 0x7Fu)) / 128.0,
        float(int((vertexColor & 0x7FFu))) / 2048.0);

    texCoord = textureCoords;

    scPos = vec2(vertexPosition);

    cull = int((textData >> 30u) & 0x1u);

    clipRectsEnabled = int((textData >> 31u) & 0x1u);
    if(clipRectsEnabled == 1) {
        cullRectIndicies = ivec3(
            int((textData >> 24u) & 0x3Fu),
            int((textData >> 18u) & 0x3Fu),
            int((textData >> 12u) & 0x3Fu));
        
        cullRectInside = ivec3(
            int((textData >> 11u) & 0x1u),
            int((textData >> 10u) & 0x1u),
            int((textData >> 9u) & 0x1u));
    }
}