#version 330 core

in vec2 fTexCoord;

out vec4 FragColor;

uniform vec3 uColor;
uniform float uAlpha;
uniform sampler2D textureSampler;

void main() {
    float alpha = texture(textureSampler, fTexCoord).r;
    FragColor = vec4(uColor, alpha * uAlpha);
}