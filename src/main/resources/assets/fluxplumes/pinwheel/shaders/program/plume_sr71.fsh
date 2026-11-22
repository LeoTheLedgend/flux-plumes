#include veil:fog
#include fluxnautics:util
#include fluxnautics:noise

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float time; // game ticks

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
uniform float uFPOpacity; // 0 = not set, treated as 1
uniform float uFPHeat;    // 0 = just lit (rocket flame), 1 = fully hot (afterburner with diamonds)

in float vertexDistance;
in vec2 lengthData;
in vec4 vertexColor;
in vec2 vLateral;

out vec4 fragColor;

// #veil:normal
const vec3 normal = vec3(0.0, 0.0, -1.0);

// Keep in sync with plume_sr71.tesh
float fpTaper(float lp, float heat) {
    float tip = mix(1.7, 0.2, heat);
    return mix(1.0, tip, pow(smoothstep(0.0, 1.0, lp), 0.9));
}

void main() {
    // #veil:albedo
    float endTaper = max(lengthData.y - 1.0, 0.0);
    float lengthPos = min(lengthData.y, 1.0);
    float plumeLen = max(uPlumeLength > 0.0 ? uPlumeLength : uPlumePower * max(uPlumeSpan, 1.0), 0.5);
    float fade = uLayerFade > 0.0 ? min(uLayerFade, 1.0) : 1.0;
    float power = min(clamp(uPlumePower, 0.0, 4.0), 2.0);
    float opacity = uFPOpacity > 0.0 ? uFPOpacity : 1.0;
    float heat = clamp(uFPHeat, 0.0, 1.0);
    float h = smoothstep(0.0, 1.0, heat);   // flame colours -> palette
    float dh = smoothstep(0.55, 1.0, heat); // diamonds only appear once it is properly hot
    float dist = lengthPos * plumeLen;      // blocks from the nozzle
    float t = mod(time, 1000.0);

    // Colours blend from a rocket flame to the chosen palette as it heats up.
    vec3 core = mix(uFPCore * 0.8, uFPCore, h);
    vec3 mid = mix(vec3(1.0, 0.6, 0.15), uFPMid, h);
    vec3 outer = mix(vec3(0.9, 0.2, 0.05), uFPOuter, h);
    vec3 accent = mix(vec3(1.0, 0.8, 0.5), uFPAccent, h);

    // Soft tube that narrows to a point; corners stay faintly visible so there is no gap.
    float R = max(uPlumeRadius * fpTaper(lengthPos, heat), 0.05);
    float rn = length(vLateral) / R;
    float roundMask = 1.0 - 0.6 * smoothstep(1.02, 1.45, rn);

    // Turbulence: fast streaks stretched along the plume and a slower shimmer.
    float streak = plumeFbm3(vec3(vLateral * 2.2 + uPlumeSeed * 60.0, lengthPos * 0.15 * plumeLen - t * 3.2),
        vec3(64.0, 64.0, 3200.0));
    float shimmer = plumeFbm3(vec3(vLateral * 0.8 + 11.0, lengthPos * 0.6 * plumeLen - t * 1.4),
        vec3(64.0, 64.0, 1400.0));

    // Just lit: ragged billowing edge like a rocket flame. Hot: clean laser-like edge.
    float edgeNoise = (streak - 0.5) * 0.9;
    float edgeMask = 1.0 - (1.0 - h) * smoothstep(0.9, 1.4, rn + edgeNoise);

    // The whole plume breathes with the throttle, and the diamonds slide a little along the axis.
    float breathe = 1.0 + 0.10 * sin(t * 1.9 + uPlumeSeed * 40.0) + 0.06 * sin(t * 5.3);
    float nozzleD = max(uPlumeRadius * 2.0, 0.6);
    float spacing = nozzleD * (1.25 + 0.4 * power) * breathe;
    float slide = (0.10 * sin(t * 1.3) + 0.05 * sin(t * 3.7 + 1.7)) * spacing;

    // Shock diamonds: hot bright cores with a wide soft halo, closer together toward the tip.
    float comp = 1.0 + 0.9 * lengthPos;
    float u = (dist + slide) / spacing * comp;
    float cell = 0.5 + 0.5 * cos(6.2831853 * u);
    float dcore = pow(cell, 6.0 + 2.0 * power);
    float halo = pow(cell, 1.6);
    float idx = floor(u + 0.5); // changes at the dark gaps, so each diamond keeps its own flicker
    float flick = 0.65 + 0.35 * sin(t * (2.3 + 0.37 * mod(idx, 5.0)) + idx * 1.9);
    float onset = smoothstep(0.2 * spacing, 0.8 * spacing, dist);
    float decay = exp(-dist / (spacing * 8.0));
    float diamonds = (dcore + 0.35 * halo) * onset * decay * flick * (0.85 + 0.3 * shimmer)
        * (1.0 - 0.35 * smoothstep(1.0, 1.35, rn)) * dh;

    // Hot body and a bright flare right at the nozzle exit. A fresh flame fades out sooner than a hot one.
    float body = pow(1.0 - lengthPos, mix(1.3, 0.7, h));
    float bodyTex = 0.55 + 0.9 * streak;
    float exitGlow = exp(-dist / (nozzleD * 0.9)) * (0.6 + 0.6 * h);

    vec3 hot = mix(core, mid, smoothstep(0.0, 0.25, lengthPos));
    hot = mix(hot, outer, smoothstep(0.2, 0.9, lengthPos));
    vec3 bodyRgb = hot * body * bodyTex * breathe;

    // Diamonds take over the colour (mix, not add) and glow white-hot at their centres.
    vec3 diaCol = mix(accent, vec3(1.0), 0.55 * dcore);
    vec3 rgb = mix(bodyRgb, diaCol * 1.5, clamp(diamonds * 1.4, 0.0, 1.0));
    rgb += core * exitGlow * 1.2;

    float alphaShape = max(1.0 - endTaper, 0.0);
    float a = (0.5 * body * bodyTex + 1.1 * diamonds + 0.9 * exitGlow) * (0.6 + 0.4 * power)
        * fade * alphaShape * roundMask * edgeMask * opacity;

    fragColor = vec4(rgb, a * 3.5) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}

