#version 330 core

layout (location = 0) in vec3 position;
layout (location = 3) in vec3 normal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform float outlineWidth;

void main() {
    vec3 newPos = position + (normal * outlineWidth);
    gl_Position = projection * view * model * vec4(newPos, 1.0);
}