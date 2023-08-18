#version 440 core

layout (location = 0) in vec2 vertexPosition;
layout (location = 1) in uint vertexColor; // red (7 bits) | green (7 bits) | blue (7 bits) | alpha (11 bits)
layout (location = 2) in ivec3 vertexData; 
layout (location = 3) in uint rectSize; // width (16 bits) | height (16 bits)

// vertexData:
// [0]: texCornerIndex (2 bits) | texImage (8 bits) | clipOutsideRect (1 bit) | cullRect_1 (6 bits) | cullRect_2 (6 bits) 
//      | cullRect_3 (6 bits) | cullRectInOut_1 (1 bit) | cullRectInOut_2 (1 bit) | cullRectInOut_3 (1 bit)
// [1]: cornerRadius_1 (6 bits) | cornerRadius_2 (6 bits) | cornerRadius_3 (6 bits) | cornerRadius_4 (6 bits) | layer (7 bits) | cull (1 bit)
// [2]: borderInnerRadius (6 bits) | borderOuterRadius (6 bits) | borderColor(20 bits) (RGBA_4448)

layout(std140) uniform windowInfo{
    int windowX;
    int windowY;
};

out vec2 screenPosition;
out vec2 texCoord;
out vec4 color;
flat out int texImage;
flat out int cull;

flat out int clipRectsEnabled;
flat out ivec3 cullRectIndicies;
flat out ivec3 cullRectInside;

flat out int cornersEnabled;
flat out int bordersEnabled;
flat out int cornerRadii[4];

flat out ivec2 pixelCornerPosition;
flat out ivec2 pixelRect;
flat out int borderInnerRadius;
flat out int borderOuterRadius;
flat out vec4 borderColor;

void main()
{
    cull = int(vertexData[1] & 0x1u);

    int cornerIndex = int((vertexData[0] >> 30u) & 0x3u);

    borderInnerRadius = int((vertexData[2] >> 26u) & 0x3Fu);
    borderOuterRadius = int((vertexData[2] >> 20u) & 0x3Fu);
    pixelRect = ivec2(int((rectSize >> 16u) & 0xFFFFu), int(rectSize & 0xFFFFu)) + ivec2(2 * borderOuterRadius);

    //position
    vec2 texelSize = vec2(2.0 / float(windowX), 2.0 / float(windowY));
    vec2 borderSize = texelSize * float(borderOuterRadius);
    screenPosition = vec2(ivec2((vertexPosition + vec2(1.0)) / texelSize)) * texelSize - vec2(1.0);
    if(cornerIndex == 0) {
        screenPosition += vec2(1.0, -1.0) * borderSize;
        texCoord = vec2(1.0, 0.0);
    }
    else if(cornerIndex == 1) {
        screenPosition += vec2(-1.0, -1.0) * borderSize; 
        texCoord = vec2(0.0, 0.0);
    }
    else if(cornerIndex == 2) {
        screenPosition += vec2(-1.0, 1.0) * borderSize; 
        texCoord = vec2(0.0, 1.0);
    }
    else if(cornerIndex == 3) {
        screenPosition += vec2(1.0, 1.0) * borderSize;
        texCoord = vec2(1.0, 1.0);
    }
    gl_Position = vec4(screenPosition, 0.0 - float(int((vertexData[1] >> 1u) & 0x7Fu)) / 256.0, 1.0);

    pixelCornerPosition = ivec2(
        int((screenPosition.x + 1.0) * 0.5 * windowX),
        int((screenPosition.y + 1.0) * 0.5 * windowY));
    if(cornerIndex == 0) pixelCornerPosition.x -= pixelRect.x;
    else if(cornerIndex == 2) pixelCornerPosition.y -= pixelRect.y;
    else if(cornerIndex == 3) pixelCornerPosition -= pixelRect;
    
    //image index
    texImage = int((vertexData[0] >> 22u) & 0xFFu);

    //color
    color = vec4(
        float(int((vertexColor >> 25u) & 0x7Fu)) / 128.0, 
        float(int((vertexColor >> 18u) & 0x7Fu)) / 128.0, 
        float(int((vertexColor >> 11u) & 0x7Fu)) / 128.0,
        float(int((vertexColor & 0x7FFu))) / 2048.0);

    borderColor = vec4(
        float(int((vertexData[2] >> 16u) & 0xFu)) / 16.0, 
        float(int((vertexData[2] >> 12u) & 0xFu)) / 16.0, 
        float(int((vertexData[2] >> 8u) & 0xFu)) / 16.0,
        float(int((vertexData[2] & 0xFFu))) / 256.0);

    //clip rects
    clipRectsEnabled = int((vertexData[0] >> 21u) & 0x1u);
    if(clipRectsEnabled == 1) {
        cullRectIndicies = ivec3(
            int((vertexData[0] >> 15u) & 0x3Fu),
            int((vertexData[0] >> 9u) & 0x3Fu),
            int((vertexData[0] >> 3u) & 0x3Fu));
        
        cullRectInside = ivec3(
            int((vertexData[0] >> 2) & 0x1u),
            int((vertexData[0] >> 1) & 0x1u),
            int(vertexData[0] & 0x1u));
    }

    //corner radii
    cornerRadii[0] = int((vertexData[1] >> 26u) & 0x3Fu) * 2 - 2;
    cornerRadii[1] = int((vertexData[1] >> 20u) & 0x3Fu) * 2 - 2;
    cornerRadii[2] = int((vertexData[1] >> 14u) & 0x3Fu) * 2 - 2;
    cornerRadii[3] = int((vertexData[1] >> 8u) & 0x3Fu) * 2 - 2;
    
    cornersEnabled = !((cornerRadii[0] == 0) && (cornerRadii[1] == 0) && (cornerRadii[2] == 0) && (cornerRadii[3] == 0)) ? 1 : 0;
    bordersEnabled = borderOuterRadius == 0 ? 0 : 1;
}