#version 440 core
out vec4 FragColor;

layout(std140, binding = 0) uniform windowInfo{
    int windowX;
    int windowY;
};

layout(std140, binding = 1) uniform cullRects{
    vec4 rects[64];
};

in vec4 color;  
in vec2 texCoord;
in vec2 scPos;
flat in int cull;
flat in int clipRectsEnabled;
flat in ivec3 cullRectIndicies;
flat in ivec3 cullRectInside;

uniform sampler2D atlas;
uniform float glyphWidth;
uniform float glyphEdge;

bool culled(int rectIndex, int cullInside);

void main()
{
    if(cull == 1) { // is null character '\0' or has transform hidden
        FragColor = vec4(0.0);
        return;
    }

    if(clipRectsEnabled == 1){
        if(cullRectIndicies.x > 0 && culled(cullRectIndicies.x, cullRectInside.x)) {
            FragColor = vec4(0.0);
            return;
        }
        if(cullRectIndicies.y > 0 && culled(cullRectIndicies.y, cullRectInside.y)) {
            FragColor = vec4(0.0);
            return;
        }
        if(cullRectIndicies.z > 0 && culled(cullRectIndicies.z, cullRectInside.z)) {
            FragColor = vec4(0.0);
            return;
        }
    }

    float dist = 1.0 - texture(atlas, texCoord).a;
    FragColor = color * vec4(1.0, 1.0, 1.0, 1 - smoothstep(glyphWidth, glyphWidth + glyphEdge, dist));
}

bool culled(int rectIndex, int cullInside){
    if(scPos.x > rects[rectIndex].y || scPos.x < rects[rectIndex].w || scPos.y > rects[rectIndex].x || scPos.y < rects[rectIndex].z){
        if(cullInside == 0) {
            return true;
        }
    }
    else if(cullInside == 1) {
        return true;    
    }
    return false;
}