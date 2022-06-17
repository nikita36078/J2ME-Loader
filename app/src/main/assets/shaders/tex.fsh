precision mediump float;
uniform sampler2D uTextureUnit;
uniform sampler2D uSphereUnit;
uniform vec2 uTexSize;
uniform vec3 uColorKey;
uniform vec2 uSphereSize;
uniform vec3 uLightDir;
uniform float uDirIntensity;
uniform float uToonThreshold;
uniform float uToonHigh;
uniform float uToonLow;
varying vec2 vTexture;
varying vec3 vNormal;
varying float vIsTransparency;
varying float vIsReflect;
varying float vAmbIntensity;

const vec3 COLORKEY_ERROR = vec3(0.5 / 255.0);

void main() {
    vec2 tex = (floor(vTexture) + 0.5);
    vec4 color = texture2D(uTextureUnit, tex / uTexSize);
    if (vIsTransparency != 0.0 && all(lessThan(abs(color.rgb - uColorKey), COLORKEY_ERROR)))
            discard;
    if (vAmbIntensity == -1.0) {
        gl_FragColor = vec4(color.rgb, 1.0);
        return;
    }
    vec4 spec = uSphereSize.x == -1.0 || vIsReflect == 0.0 ?
        vec4(0.0) : texture2D(uSphereUnit, (normalize(vNormal).xy + 1.0) * 0.5 * uSphereSize);
    float lambert_factor = max(dot(normalize(vNormal), uLightDir), 0.0);
    float light = min(vAmbIntensity + uDirIntensity * lambert_factor, 1.0);
    if (uToonThreshold != -1.0) {
        light = light < uToonThreshold ? uToonLow : uToonHigh;
    }
    gl_FragColor = vec4(color.rgb * light + spec.rgb, 1.0);
}