#include veil:fog

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float time;
uniform float uPlumeLength;
uniform float uPlumePower;
uniform float uLayerFade;
uniform float uPlumeRadius;
uniform float uFPOpacity;
uniform vec3 uFPCore;
uniform vec3 uFPMid;
uniform vec3 uFPOuter;
uniform vec3 uFPAccent;

in float vertexDistance;
in vec2 lengthData;
in vec2 vLateral;
out vec4 fragColor;

// A narrow electromagnetic beam: concentric charge rings travel outwards while
// a cool sheath clamps the edge. This deliberately has no flame turbulence.
void main() {
    float lp = min(lengthData.y, 1.0);
    float plumeLen = max(uPlumeLength, 0.5);
    float opacity = uFPOpacity > 0.0 ? uFPOpacity : 1.0;
    // Matches the pinched SR-71 tessellation profile forced for Ion in the renderer.
    float radius = max(uPlumeRadius * mix(1.0, 0.2, pow(smoothstep(0.0, 1.0, lp), 0.9)), 0.05);
    float r = length(vLateral) / radius;
    float edge = 1.0 - smoothstep(1.05, 1.48, r);
    float pulse = 0.5 + 0.5 * sin((lp * plumeLen * 15.0 - mod(time, 1000.0) * 8.0) * 6.2831853);
    float rings = pow(pulse, 12.0) * (0.35 + 0.65 * smoothstep(0.05, 0.8, lp));
    float core = exp(-r * r * 1.2);
    float flicker = 0.88 + 0.12 * sin(time * 19.0 + lp * 71.0);
    vec3 rgb = uFPCore * core * 2.2 + uFPMid * rings * 1.45 + uFPAccent * edge * 0.36;
    // Keep a visible core at low thrust/short lengths; the old all-ring treatment could disappear.
    float alpha = (core * 1.25 + rings * 0.9 + edge * 0.12) * edge * flicker * opacity;
    fragColor = vec4(rgb, alpha * 3.4 * max(1.0 - max(lengthData.y - 1.0, 0.0), 0.0)) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
