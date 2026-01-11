#version 330 core

out vec4 fragColor;
uniform vec3 outlineColor;

void main() {
    fragColor = vec4(outlineColor, 1.0);
}