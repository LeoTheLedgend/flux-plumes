#include veil:fog
#include fluxnautics:util
#include fluxnautics:noise

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float time;

uniform float uPlumeSpan;
uniform float uPlumePower;
uniform float uPlumeLength;
uniform float uLayerFade;
uniform float uPlumeSeed;
uniform float uPlumeRadius;

uniform vec3 uFPCore;
uniform vec3 uFPMid;
uniform vec3 uFPOuter;
uniform vec3 uFPAccent;
uniform float uFPGround;  // distance to the ground in blocks, <= 0 when unknown
uniform float uFPOpacity; // 0 = not set, treated as 1

in float vertexDistance;
in vec2 lengthData;
in vec4 vertexColor;
in vec2 vLateral;

out vec4 fragColor;

// #veil:normal
const vec3 normal = vec3(0.0, 0.0, -1.0);

// Keep in sync with plume_rocket.tesh
float fpTaper(float lp) {
    return mix(1.0, 1.7, smoothstep(0.0, 0.85, lp));
}

// The smoke is made of particles (see ExhaustSmokeParticle); this shader is the flame, with ragged
// soft edges so it blends into that smoke instead of looking like a block.
void main() {
    // #veil:albedo
    float endTaper = max(lengthData.y - 1.0, 0.0);
    float lengthPos = min(lengthData.y, 1.0);
    float plumeLen = max(uPlumeLength > 0.0 ? uPlumeLength : uPlumePower * max(uPlumeSpan, 1.0), 0.5);
    float fade = uLayerFade > 0.0 ? min(uLayerFade, 1.0) : 1.0;
    float power = clamp(uPlumePower, 0.0, 4.0);
    float opacity = uFPOpacity > 0.0 ? uFPOpacity : 1.0;
    float dist = lengthPos * plumeLen;

    float tWrap = mod(time, 1000.0);
    float turb = plumeFbm3(vec3(vLateral * 1.1 + uPlumeSeed * 64.0, lengthPos * 0.35 * plumeLen - tWrap * 3.0),
        vec3(64.0, 64.0, 3000.0));
    float turb2 = plumeFbm3(vec3(vLateral * 2.7 + uPlumeSeed * 31.0, lengthPos * 0.9 * plumeLen - tWrap * 4.5),
        vec3(64.0, 64.0, 3000.0));
    float body = pow(1.0 - lengthPos, 1.1);
    float rough = 0.55 + 0.9 * turb;

    // Ragged, rounded edge: erode the outline with noise so it reads as billowing gas, not a box.
    float R = max(uPlumeRadius * fpTaper(lengthPos), 0.05);
    float rn = length(vLateral) / R;
    float edgeNoise = (turb - 0.5) * 0.7 + (turb2 - 0.5) * 0.35;
    float edgeMask = 1.0 - smoothstep(0.9, 1.4, rn + edgeNoise * (0.4 + lengthPos));

    // When the exhaust hits the ground the flame stops there and glows where it lands.
    float cut = 1.0;
    float impact = 0.0;
    if (uFPGround > 0.0 && uFPGround < plumeLen) {
        cut = 1.0 - smoothstep(uFPGround - 0.15, uFPGround + 0.35, dist);
        impact = exp(-abs(dist - uFPGround) * 1.5);
    }

    vec3 flame = mix(uFPCore, uFPMid, smoothstep(0.0, 0.2, lengthPos));
    flame = mix(flame, uFPOuter, smoothstep(0.15, 0.6, lengthPos));
    vec3 rgb = flame * body * rough + uFPOuter * impact * 0.8;

    float alphaShape = max(1.0 - endTaper, 0.0);
    float a = (0.65 * body * rough + 0.7 * impact) * edgeMask * cut * (0.7 + 0.3 * min(power, 2.0)) * fade * alphaShape * opacity;

    fragColor = vec4(rgb, a * 3.0) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
