#version 440 core
//#define CORNER_SMOOTHING_PIXELS 0.0
#define CORNER_SMOOTHING_PIXELS 2.0

out vec4 FragColor;

layout(std140, binding = 0) uniform windowInfo{
    int windowX;
    int windowY;
};

layout(std140, binding = 1) uniform cullRects{
    vec4 rects[64];
};

in vec4 color;
flat in int texImage;
flat in int cull;

flat in int clipRectsEnabled;
flat in ivec3 cullRectIndicies;
flat in ivec3 cullRectInside;

flat in int cornersEnabled;
flat in int bordersEnabled;
flat in int cornerRadii[4];

in vec2 texCoord;
in vec2 screenPosition;
flat in ivec2 pixelCornerPosition;
flat in ivec2 pixelRect;
flat in int borderInnerRadius;
flat in int borderOuterRadius;
flat in vec4 borderColor;

uniform sampler2DArray atlas;

bool culled(int rectIndex, int cullInside);

void main()
{
    if(cull == 1) { //transform is hidden
        FragColor = vec4(0.0);
        return;
    }

    //calculate if inside any clipping rects
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

    ivec2 pixelPosition = ivec2(int((screenPosition.x + 1.0) * 0.5 * windowX), int((screenPosition.y + 1.0) * 0.5 * windowY)) - pixelCornerPosition;
    float edgeFactor = 1.0;
    float borderFactor = 0.0;
    vec4 adjBorderColor = borderColor;
    
    if(bordersEnabled == 1 || cornersEnabled == 1){
        if(pixelPosition.x + 1 < borderOuterRadius + cornerRadii[0] && pixelPosition.y + 1 < borderOuterRadius + cornerRadii[0]){
            //bottom left corner
            float cornerDistance = distance(pixelPosition + ivec2(1, 1), ivec2(borderOuterRadius + cornerRadii[0]));

            edgeFactor = 1.0 - smoothstep(float(cornerRadii[0]), float(cornerRadii[0]) + CORNER_SMOOTHING_PIXELS, cornerDistance);
            borderFactor = 1.0 - smoothstep(float(cornerRadii[0] + borderInnerRadius), float(borderOuterRadius + cornerRadii[0]), cornerDistance);
        }
        else if(pixelPosition.x + 1 < borderOuterRadius + cornerRadii[1] && pixelPosition.y - 1 > pixelRect.y - borderOuterRadius - cornerRadii[1]){
            //top left corner
            float cornerDistance = distance(pixelPosition + ivec2(1, -1), ivec2(borderOuterRadius + cornerRadii[1], pixelRect.y - borderOuterRadius - cornerRadii[1]));

            edgeFactor = 1.0 - smoothstep(float(cornerRadii[1]), float(cornerRadii[1] + CORNER_SMOOTHING_PIXELS), cornerDistance);
            borderFactor = 1.0 - smoothstep(float(cornerRadii[1] + borderInnerRadius), float(borderOuterRadius + cornerRadii[1]), cornerDistance);
        }
        else if(pixelPosition.x - 1 > pixelRect.x - borderOuterRadius - cornerRadii[2] && pixelPosition.y - 1 > pixelRect.y - borderOuterRadius - cornerRadii[2]){
            //top right corner
            float cornerDistance = distance(pixelPosition + ivec2(-1, -1), ivec2(pixelRect.x - borderOuterRadius - cornerRadii[2], pixelRect.y - borderOuterRadius - cornerRadii[2]));

            edgeFactor = 1.0 - smoothstep(float(cornerRadii[2]), float(cornerRadii[2] + CORNER_SMOOTHING_PIXELS), cornerDistance);
            borderFactor = 1.0 - smoothstep(float(cornerRadii[2] + borderInnerRadius), float(borderOuterRadius + cornerRadii[2]), cornerDistance);
        }
        else if(pixelPosition.x - 1 > pixelRect.x - borderOuterRadius - cornerRadii[3] && pixelPosition.y + 1 < borderOuterRadius + cornerRadii[3]){
            //bottom right corner
            float cornerDistance = distance(pixelPosition + ivec2(-1, 1), ivec2(pixelRect.x - borderOuterRadius - cornerRadii[3], borderOuterRadius + cornerRadii[3]));

            edgeFactor = 1.0 - smoothstep(float(cornerRadii[3]), float(cornerRadii[3] + CORNER_SMOOTHING_PIXELS), cornerDistance);
            borderFactor = 1.0 - smoothstep(float(cornerRadii[3] + borderInnerRadius), float(borderOuterRadius + cornerRadii[3]), cornerDistance);
        }
        else if(pixelPosition.x + 1 < borderOuterRadius || pixelPosition.x - 1 > pixelRect.x - borderOuterRadius || pixelPosition.y + 1 < borderOuterRadius || pixelPosition.y - 1 > pixelRect.y - borderOuterRadius){
            float cornerDistance = float(max(borderOuterRadius - pixelPosition.x - 1, max(pixelPosition.x - 1 - pixelRect.x + borderOuterRadius, max(borderOuterRadius - pixelPosition.y - 1, pixelPosition.y - 1 - pixelRect.y + borderOuterRadius))));

            edgeFactor = 1.0 - smoothstep(0.0, CORNER_SMOOTHING_PIXELS, cornerDistance);
            borderFactor = (1.0 - smoothstep(borderInnerRadius, float(borderOuterRadius), cornerDistance));
        }
    }
    
    if(texImage == 255) {
        FragColor = color * vec4(1.0, 1.0, 1.0, 1.0);
    }
    else{
        FragColor = color * texture(atlas, vec3(texCoord, texImage));
    }
    
    if(cornersEnabled == 1 && bordersEnabled == 0){
        adjBorderColor = vec4(FragColor.rgb, 0.0);
        borderFactor = 0.0;
    }

    FragColor = mix(adjBorderColor, FragColor, edgeFactor);
    FragColor.w *= max(edgeFactor, borderFactor);
} 

bool culled(int rectIndex, int cullInside){
    vec2 texelSize = vec2(2.0) / vec2(windowX, windowY);
    if(screenPosition.x > rects[rectIndex].y + texelSize.x || screenPosition.x < rects[rectIndex].w || screenPosition.y > rects[rectIndex].x + texelSize.y || screenPosition.y < rects[rectIndex].z){
        if(cullInside == 0) {
            return true;
        }
    }
    else if(cullInside == 1) {
        return true;    
    }
    return false;
}