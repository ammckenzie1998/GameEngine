#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec2 aTexCoord;

out vec2 fTexCoord;

uniform mat4 model;
uniform mat4 projection;
uniform vec2 uTexOffset;
uniform vec2 uTexScale;

void main() {
    gl_Position = projection * model * vec4(aPos.xy, 0.0, 1.0);
    fTexCoord = aTexCoord * uTexScale + uTexOffset;
}