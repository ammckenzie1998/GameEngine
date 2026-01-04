#version 330 core

layout (location=0) in vec3 pos;
layout (location=1) in vec3 color;
layout (location=2) in vec2 texCoord;
layout (location=3) in vec3 normal;

out vec3 fColor;
out vec2 fTexCoord;
out vec3 fNormal;
out vec3 fPos;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main(){
    fColor = color;
    fTexCoord = texCoord;

    fNormal = mat3(transpose(inverse(model))) * normal;

    vec4 worldPos = model * vec4(pos, 1.0);
    fPos = worldPos.xyz;

    gl_Position = projection * view * worldPos;
}