#include veil:fog
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float uFPOpacity;
uniform vec3 uFPCore;
uniform vec3 uFPMid;
uniform vec3 uFPOuter;
in float vertexDistance;
in vec2 lengthData;
in vec2 vLateral;
out vec4 fragColor;
void main() {
    float t = clamp(lengthData.y, 0.0, 1.0);
    // Long luminous centre transitions into a narrower coloured tail.
    vec3 rgb = mix(uFPCore, uFPMid, smoothstep(0.12, 0.55, t));
    rgb = mix(rgb, uFPOuter, smoothstep(0.48, 0.95, t));
    float fade = (1.0 - smoothstep(0.78, 1.0, t)) * clamp(uFPOpacity, 0.0, 1.0);
    fragColor = vec4(rgb, fade * 0.38) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
