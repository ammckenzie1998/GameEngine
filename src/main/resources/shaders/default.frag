#version 330 core

in vec3 fColor;
in vec2 fTexCoord;

uniform sampler2D textureSampler;

out vec4 FragColor;

void main(){
    vec4 textureColor = texture(textureSampler, fTexCoord);

    FragColor = textureColor * vec4(fColor, 1.0);
}