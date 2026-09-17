package com.pixeldual.fold.shader

/**
 * Calibrated AGSL Optical Perspective & Defocus Shader.
 *
 * Implements Envl's SoloTilt ray-projection geometry,
 * distance-dependent angular diffusion, and glass ambient/specular optics.
 */
object SoloTiltRayShader {

    /**
     * Inner Screen Ray-Traced Optical Perspective Shader.
     * Applied to the pivoting left half panel.
     */
    const val INNER_LEFT_RAY_SHADER = """
        uniform shader content;
        uniform float2 size;
        uniform float hingeAngle;
        uniform float useShaderPerspective;

        float ign(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        vec4 main(float2 fragCoord) {
            float turn = clamp((180.0 - hingeAngle) / 90.0, 0.0, 1.0);
            if (turn <= 0.0001) {
                return content.eval(fragCoord);
            }

            vec2 uv = fragCoord / size;
            float hinge = 1.0;
            float fromHinge = clamp(1.0 - uv.x, 0.0, 1.0);

            // Physical fold tilt: 0 to PI/2 (90°)
            float tilt = turn * 1.570796327;
            float cosine = max(0.0, cos(tilt));
            float sine = sin(tilt);

            // Camera ray perspective projection:
            float aspect = size.x / size.y;
            float eyeDistance = 2.4 * max(aspect, 1.0);
            float depth = fromHinge * aspect * sine;
            float perspective = eyeDistance / max(eyeDistance - depth, 0.001);

            vec2 imageUv;
            if (useShaderPerspective > 0.5) {
                imageUv.x = hinge + (uv.x - hinge) * cosine * perspective;
                imageUv.y = 0.5 + (uv.y - 0.5) * perspective;
            } else {
                imageUv = uv;
            }

            // Defocus diffusion:
            // Angle response: tilt^0.5
            // Distance spread: smoothstep(0.0, 0.70, fromHinge)^1.45
            float blurAngle = pow(smoothstep(0.0, 1.570796327, tilt), 0.5);
            float blurSpread = pow(smoothstep(0.0, 0.70, fromHinge), 1.45);
            float defocus = blurAngle * blurSpread;
            float sigma = size.x * 0.038 * defocus;

            // Trapezoid vertical boundary mask:
            float verticalMask = 1.0;
            if (useShaderPerspective > 0.5) {
                float pixelY = 1.0 / size.y;
                float marginSoftness = pixelY + 2.0 * sigma / size.y;
                verticalMask = 1.0 - smoothstep(0.5 - marginSoftness, 0.5 + marginSoftness, abs(imageUv.y - 0.5));
                if (verticalMask <= 0.0) {
                    return vec4(0.0, 0.0, 0.0, 1.0);
                }
            }

            // Sample content with 16-tap Vogel's golden angle spiral:
            vec4 baseSample;
            vec2 centerCoord = clamp(imageUv, 0.0, 1.0) * size;

            if (sigma < 0.5) {
                baseSample = content.eval(centerCoord);
            } else {
                const int SAMPLES = 16;
                const float GOLDEN_ANGLE = 2.39996323;
                float jitter = ign(fragCoord) * 6.283185;

                vec4 sum = vec4(0.0);
                float totalWeight = 0.0;

                for (int i = 0; i < 16; i++) {
                    float fi = float(i);
                    float r = sqrt((fi + 0.5) / 16.0) * sigma;
                    float theta = fi * GOLDEN_ANGLE + jitter;
                    vec2 offset = vec2(cos(theta), sin(theta)) * r;

                    float weight = 1.0 - (r / (sigma + 0.01)) * 0.5;
                    vec2 samplePos = clamp((centerCoord + offset) / size, 0.0, 1.0) * size;
                    sum += content.eval(samplePos) * weight;
                    totalWeight += weight;
                }
                baseSample = sum / totalWeight;
            }

            // Glass ambient shading:
            float glass = sine * pow(fromHinge, 1.6);
            baseSample.rgb *= 1.0 - 0.28 * glass;

            // Specular reflection band (centered around 70% width):
            float reflection = exp(-pow((fromHinge - 0.70) / 0.30, 2.0)) * sine;
            baseSample.rgb += vec3(0.82, 0.85, 0.88) * reflection * 0.025;

            // Black void exposure fade (starts past 26% crease deadband, up to 70% opacity):
            float blackFade = clamp((fromHinge - 0.26) / 0.74, 0.0, 1.0);
            baseSample.rgb *= 1.0 - 0.70 * blurAngle * blackFade;

            vec3 dark = vec3(0.0, 0.0, 0.0);
            return vec4(mix(dark, baseSample.rgb, verticalMask), 1.0);
        }
    """

    /**
     * Outer Screen (Cover Display) Inverted Perspective & Parallax Shader.
     * As the device folds from 90° down to 0°, the outer screen clarifies and becomes 100% sharp.
     */
    const val OUTER_COVER_RAY_SHADER = """
        uniform shader content;
        uniform float2 size;
        uniform float hingeAngle;

        float ign(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        vec4 main(float2 fragCoord) {
            vec2 uv = fragCoord / size;
                // The cover hinge is on the left: keep that edge anchored and fold depth to the right.
                float outerTurn = clamp(hingeAngle / 90.0, 0.0, 1.0);
                // Fade the effect out during the final closing degrees so the cover settles sharp.
                outerTurn *= smoothstep(0.0, 12.0, hingeAngle);
                float tilt = outerTurn * 1.570796327;
                float cosine = max(0.0, cos(tilt));
                float sine = sin(tilt);
                float aspect = size.x / size.y;
                float eyeDistance = 2.4 * max(aspect, 1.0);
                float fromHinge = uv.x;

                float depth = fromHinge * aspect * sine;
                float perspective = eyeDistance / max(eyeDistance - depth, 0.001);
                vec2 imageUv;
                // One continuous projective mapping. The hinge stays anchored at x=0;
                // the far edge compresses smoothly instead of switching to a second UV region.
                float projectedX = uv.x * cosine * perspective;
                imageUv.x = projectedX;
                imageUv.y = 0.5 + (uv.y - 0.5) * perspective;

                float blurAmount = pow(smoothstep(0.0, 1.570796327, tilt), 0.5) *
                    pow(smoothstep(0.0, 0.70, fromHinge), 1.45) * outerTurn;
                float sigma = size.x * 0.038 * blurAmount;

                vec2 centerCoord = clamp(imageUv, 0.0, 1.0) * size;
                vec4 result;

                if (sigma < 0.5) {
                    result = content.eval(centerCoord);
                } else {
                    const float GOLDEN_ANGLE = 2.39996323;
                    float jitter = ign(fragCoord) * 6.283185;
                    vec4 sum = vec4(0.0);
                    float totalWeight = 0.0;

                    for (int i = 0; i < 16; i++) {
                        float fi = float(i);
                        float r = sqrt((fi + 0.5) / 16.0) * sigma;
                        float theta = fi * GOLDEN_ANGLE + jitter;
                        vec2 offset = vec2(cos(theta), sin(theta)) * r;
                        float weight = 1.0 - (r / (sigma + 0.01)) * 0.5;
                        vec2 samplePos = clamp(centerCoord + offset, vec2(0.0), size);
                        sum += content.eval(samplePos) * weight;
                        totalWeight += weight;
                    }
                    result = sum / totalWeight;
                }

                float edgeSheen = exp(-pow((fromHinge - 0.72) / 0.24, 2.0)) * outerTurn;
                result.rgb += vec3(0.82, 0.87, 0.92) * edgeSheen * 0.035;
                return vec4(result.rgb, 1.0);
        }
    """
}
