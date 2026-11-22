// Full-screen heat haze: samples the already-rendered frame and distorts the UVs near each
// plume (where the plume appeared on screen) with an animated ripple, like hot air behind a jet.
uniform sampler2D DiffuseSampler0;

uniform float uHazeTime;
uniform int uHazeCount;
uniform float uHazeData[32]; // up to 8 plumes, vec4 each: x,y screen pos, z screen radius, w strength

in vec2 texCoord;

out vec4 fragColor;

float hnoise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    vec2 off = vec2(0.0);
    float t = uHazeTime * 0.05;
    for (int i = 0; i < 8; i++) {
        if (i >= uHazeCount) {
            break;
        }
        float x = uHazeData[i * 4 + 0];
        float y = uHazeData[i * 4 + 1];
        float r = max(uHazeData[i * 4 + 2], 0.001);
        float w = uHazeData[i * 4 + 3];
        vec2 d = uv - vec2(x, y);
        float dist = length(d);
        // Widen the radius of influence so the shimmer is visible over a larger area around the plume.
        float fall = exp(-(dist * dist) / (r * r * 1.2));
        if (fall < 0.001) {
            continue;
        }
        vec2 dir = dist > 0.0001 ? d / dist : vec2(0.0, 1.0);
        vec2 perp = vec2(-dir.y, dir.x);
        // Boiling swirl (perpendicular wobble) plus a radial pulse, strongest at the core.
        float n1 = hnoise(uv * 160.0 + vec2(t * 30.0, 0.0));
        float n2 = hnoise(uv * 90.0 + vec2(0.0, t * 22.0));
        float swirl = (n1 - 0.5) + (n2 - 0.5);
        off += perp * swirl * fall * w * 0.30;
        off += dir * sin(dist * 40.0 - t * 25.0) * fall * w * 0.08;
        off += -d * fall * fall * w * 0.10;
    }
    fragColor = texture(DiffuseSampler0, clamp(uv + off, 0.001, 0.999));
}
