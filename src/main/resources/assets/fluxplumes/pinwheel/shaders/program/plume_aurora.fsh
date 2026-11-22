#include veil:fog
#include fluxnautics:noise

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float time;
uniform float uPlumeLength;
uniform float uPlumePower;
uniform float uLayerFade;
uniform float uPlumeRadius;
uniform float uFPOpacity;
uniform float uPlumeSeed;
uniform vec3 uFPCore;
uniform vec3 uFPMid;
uniform vec3 uFPOuter;
uniform vec3 uFPAccent;

in float vertexDistance;
in vec2 lengthData;
in vec2 vLateral;
out vec4 fragColor;

// A soft, wide plasma curtain: slow braided bands, colour cycling and a diffuse edge.
void main() {
    float lp = min(lengthData.y, 1.0);
    float opacity = uFPOpacity > 0.0 ? uFPOpacity : 1.0;
    // Never make the colour mask wider than the flared rocket mesh. A mask wider than its mesh
    // leaves every face opaque and exposes the original rectangular wall tube.
    float radius = max(uPlumeRadius * mix(1.0, 1.7, smoothstep(0.0, 0.85, lp)), 0.05);
    float r = length(vLateral) / radius;
    float edge = 1.0 - smoothstep(1.02, 1.48, r);
    float t = mod(time, 1000.0);
    float curl = plumeFbm3(vec3(vLateral * 0.9 + uPlumeSeed * 31.0, lp * 4.0 - t * 0.38), vec3(48.0, 48.0, 900.0));
    float band = 0.5 + 0.5 * sin(lp * 25.0 + atan(vLateral.y, vLateral.x) * 3.0 - t * 1.1 + curl * 5.0);
    float veil = pow(band, 1.8) * (0.45 + 0.75 * curl);
    float colourShift = 0.5 + 0.5 * sin(lp * 9.0 - t * 0.45 + curl * 4.0);
    vec3 first = mix(uFPMid, uFPAccent, colourShift);
    vec3 second = mix(uFPOuter, uFPCore, 0.35 + 0.4 * band);
    vec3 rgb = mix(first, second, smoothstep(0.45, 1.15, r)) * (0.45 + veil * 1.15);
    float alpha = edge * (0.18 + veil * 0.65) * (0.6 + 0.4 * (1.0 - lp)) * opacity;
    fragColor = vec4(rgb, alpha * 2.8 * max(1.0 - max(lengthData.y - 1.0, 0.0), 0.0)) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
