#version 330 core

in vec3 fColor;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fPos;

uniform sampler2D textureSampler;

out vec4 finalColor;

const vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
const vec3 lightColor = vec3(1.0, 1.0, 1.0);
const vec3 ambientColor = vec3(0.4, 0.4, 0.5);

void main(){
    vec4 textureColor = texture(textureSampler, fTexCoord);

    vec3 normal = normalize(fNormal);
    float diff = max(dot(normal, lightDir), 0.0);

    float intensity;
    if (diff > 0.95)
        intensity = 1.0;
    else if (diff > 0.5)
        intensity = 0.7;
    else if (diff > 0.25)
        intensity = 0.4;
    else
        intensity = 0.2;

    vec3 lighting = ambientColor + (lightColor * intensity);

    vec3 result = fColor * textureColor.rgb * lighting;

    finalColor = vec4(result, textureColor.a);
}