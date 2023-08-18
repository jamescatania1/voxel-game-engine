#version 440 core

flat in int faceDirection;

out vec4 FragColor;

void main()
{
    FragColor = vec4(float(faceDirection) / 6.0, 0.0, 0.0, 1.0);
}  