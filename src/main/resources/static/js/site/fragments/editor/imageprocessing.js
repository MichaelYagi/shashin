// Usage examples:
//   // pass an element
//   setupImageAdjustments(imgEl, document.getElementById('glCanvas'), ...);
//   // or pass an id (string)
//   setupImageAdjustments(imgEl, 'glCanvas', ...);
//   // make sure to call after DOMContentLoaded or place script after elements.

const vertexShaderSource = `
    attribute vec2 a_position;
    attribute vec2 a_texCoord;
    varying vec2 v_texCoord;
    void main() {
        gl_Position = vec4(a_position, 0, 1);
        v_texCoord = a_texCoord;
    }
`;

const fragmentShaderSource = `
    precision mediump float;
    uniform sampler2D u_image;
    uniform float u_brightness;
    uniform float u_contrast;
    uniform float u_saturation;
    uniform float u_sharpness;
    uniform vec2 u_resolution;
    varying vec2 v_texCoord;

    void main() {
        vec4 center = texture2D(u_image, v_texCoord);
        vec4 color = center;

        float sharpenWeight = max((u_sharpness - 1.0) / 4.5, 0.0);
        if (sharpenWeight > 0.0) {
            vec2 onePixel = vec2(1.0) / u_resolution;
            float scale = clamp(sqrt(u_resolution.x * u_resolution.y) / 512.0, 0.5, 2.0);
            vec2 offset = onePixel * scale;

            vec4 sum = center * (1.0 + 4.0 * sharpenWeight);
            sum -= texture2D(u_image, v_texCoord + vec2(-offset.x, 0.0)) * sharpenWeight;
            sum -= texture2D(u_image, v_texCoord + vec2(offset.x, 0.0)) * sharpenWeight;
            sum -= texture2D(u_image, v_texCoord + vec2(0.0, -offset.y)) * sharpenWeight;
            sum -= texture2D(u_image, v_texCoord + vec2(0.0, offset.y)) * sharpenWeight;
            color = sum;
        }

        color.rgb = ((color.rgb * u_brightness - 0.5) * u_contrast) + 0.5;

        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        vec3 delta = color.rgb - vec3(gray);
        vec3 saturated = vec3(gray) + delta * u_saturation;

        if (u_saturation > 1.0) {
            float redFactor = 1.0 - 0.10 * (u_saturation - 1.0);
            saturated.r = gray + (color.r - gray) * u_saturation * redFactor;
        }

        saturated += vec3(max((u_saturation - 1.0) * 0.006, 0.0));

        gl_FragColor = vec4(clamp(saturated, 0.0, 1.0), color.a);
    }
`;

function createShader(gl, type, source) {
    const shader = gl.createShader(type);
    if (!shader) throw new Error('Unable to create shader');
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    const ok = gl.getShaderParameter(shader, gl.COMPILE_STATUS);
    if (!ok) {
        const log = gl.getShaderInfoLog(shader);
        gl.deleteShader(shader);
        throw new Error('Shader compile error: ' + log);
    }
    return shader;
}

function createProgram(gl, vsSource, fsSource) {
    const vs = createShader(gl, gl.VERTEX_SHADER, vsSource);
    const fs = createShader(gl, gl.FRAGMENT_SHADER, fsSource);
    const program = gl.createProgram();
    if (!program) {
        gl.deleteShader(vs);
        gl.deleteShader(fs);
        throw new Error('Unable to create program');
    }
    gl.attachShader(program, vs);
    gl.attachShader(program, fs);
    gl.linkProgram(program);
    const ok = gl.getProgramParameter(program, gl.LINK_STATUS);
    if (!ok) {
        const log = gl.getProgramInfoLog(program);
        gl.deleteProgram(program);
        gl.deleteShader(vs);
        gl.deleteShader(fs);
        throw new Error('Program link error: ' + log);
    }
    // shaders can be deleted after linking
    gl.deleteShader(vs);
    gl.deleteShader(fs);
    return program;
}

/**
 * Applies image adjustments and updates #editShashinImage src.
 * - image: HTMLImageElement/HTMLCanvasElement/HTMLVideoElement
 * - canvasOrId: either a <canvas> element or its id string
 * - brightnessInput, contrastInput, saturationInput, sharpnessInput: numbers
 *
 * Returns a Promise that resolves with the created object URL (or null on failure).
 */
function setupImageAdjustments(
    image,
    canvasOrId,
    brightnessInput = 1.0,
    contrastInput = 1.0,
    saturationInput = 1.0,
    sharpnessInput = 1.0
) {
    return new Promise((resolve, reject) => {
        const startTime = performance.now();

        // Resolve canvas element if id was passed
        let canvas = null;
        if (typeof canvasOrId === 'string') {
            canvas = document.getElementById(canvasOrId);
        } else {
            canvas = canvasOrId;
        }

        if (!canvas) {
            const msg = 'Canvas element not found. Pass a valid canvas element or id, and call after DOM ready.';
            shashin.printMessageToConsole(msg,{tag:"editor", consoleType: shashin.consoleTypes.error});
            reject(new Error(msg));
            return;
        }

        // Make sure canvas has correct size for the image
        // Some image elements may not have width/height filled until loaded; try to use naturalWidth
        const imgWidth = image.width || image.naturalWidth || image.videoWidth;
        const imgHeight = image.height || image.naturalHeight || image.videoHeight;
        if (!imgWidth || !imgHeight) {
            const msg = 'Image has no dimensions. Ensure image is loaded before calling setupImageAdjustments.';
            shashin.printMessageToConsole(msg,{tag:"editor", consoleType: shashin.consoleTypes.error});
            reject(new Error(msg));
            return;
        }

        if (canvas.width !== imgWidth || canvas.height !== imgHeight) {
            canvas.width = imgWidth;
            canvas.height = imgHeight;
        }

        const gl = canvas.getContext('webgl', { preserveDrawingBuffer: false });
        if (!gl) {
            const msg = 'WebGL not supported or failed to initialize.';
            shashin.printMessageToConsole(msg,{tag:"editor", consoleType: shashin.consoleTypes.error});
            reject(new Error(msg));
            return;
        }

        let program;
        try {
            program = createProgram(gl, vertexShaderSource, fragmentShaderSource);
        } catch (err) {
            shashin.printMessageToConsole(err,{tag:"editor", consoleType: shashin.consoleTypes.error});
            reject(err);
            return;
        }
        gl.useProgram(program);
        gl.viewport(0, 0, canvas.width, canvas.height);

        // Geometry (fullscreen quad)
        const positionBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
            -1, -1, 1, -1, -1, 1,
            1, -1, 1, 1, -1, 1
        ]), gl.STATIC_DRAW);

        const aPosition = gl.getAttribLocation(program, 'a_position');
        if (aPosition >= 0) {
            gl.enableVertexAttribArray(aPosition);
            gl.vertexAttribPointer(aPosition, 2, gl.FLOAT, false, 0, 0);
        }

        const texCoordBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
            0, 0, 1, 0, 0, 1,
            1, 0, 1, 1, 0, 1
        ]), gl.STATIC_DRAW);

        const aTexCoord = gl.getAttribLocation(program, 'a_texCoord');
        if (aTexCoord >= 0) {
            gl.enableVertexAttribArray(aTexCoord);
            gl.vertexAttribPointer(aTexCoord, 2, gl.FLOAT, false, 0, 0);
        }

        // Texture
        const texture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
        // Upload image
        try {
            gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
        } catch (err) {
            shashin.printMessageToConsole(`'texImage2D failed - image may not be ready:', : ${err}`,{tag:"editor", consoleType: shashin.consoleTypes.error});
            reject(err);
            return;
        }
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);

        // Uniforms
        const uniforms = {
            brightness: gl.getUniformLocation(program, 'u_brightness'),
            contrast: gl.getUniformLocation(program, 'u_contrast'),
            saturation: gl.getUniformLocation(program, 'u_saturation'),
            sharpness: gl.getUniformLocation(program, 'u_sharpness'),
            resolution: gl.getUniformLocation(program, 'u_resolution'),
            image: gl.getUniformLocation(program, 'u_image')
        };

        if (uniforms.brightness) gl.uniform1f(uniforms.brightness, brightnessInput);
        if (uniforms.contrast) gl.uniform1f(uniforms.contrast, contrastInput);
        if (uniforms.saturation) gl.uniform1f(uniforms.saturation, saturationInput);
        if (uniforms.sharpness) gl.uniform1f(uniforms.sharpness, sharpnessInput);
        if (uniforms.resolution) gl.uniform2f(uniforms.resolution, canvas.width, canvas.height);
        if (uniforms.image) gl.uniform1i(uniforms.image, 0);

        // Draw
        gl.drawArrays(gl.TRIANGLES, 0, 6);

        // Use toBlob + createObjectURL (non-blocking) and set #editShashinImage.src
        canvas.toBlob((blob) => {
            if (!blob) {
                resolve(null);
                return;
            }
            const url = URL.createObjectURL(blob);
            const imgEl = document.getElementById('editShashinImage');
            if (imgEl) {
                imgEl.src = url;
            }
            // Note: caller should revoke URL when no longer needed.
            resolve(url);
        }, 'image/jpeg', 0.2);

        const endTime = performance.now();
        shashin.printMessageToConsole(`Call to setupImageAdjustments took ${Math.trunc(endTime - startTime)} milliseconds`,{tag:"editor"});
    });
}

// Export for module usage (uncomment if using modules)
// export { setupImageAdjustments };


// ---------------------------------------------------------------------------------------------------------------------
// // Original version
// const vertexShaderSource = `
//     attribute vec2 a_position;
//     attribute vec2 a_texCoord;
//     varying vec2 v_texCoord;
//     void main() {
//         gl_Position = vec4(a_position, 0, 1);
//         v_texCoord = a_texCoord;
//     }
// `;
//
// const fragmentShaderSource = `
//     precision mediump float;
//     uniform sampler2D u_image;
//     uniform float u_brightness;
//     uniform float u_contrast;
//     uniform float u_saturation;
//     uniform float u_sharpness;
//     uniform vec2 u_resolution;
//     varying vec2 v_texCoord;
//
//     void main() {
//         vec4 center = texture2D(u_image, v_texCoord);
//         vec4 color = center;
//
//         float sharpenWeight = max((u_sharpness - 1.0) / 4.5, 0.0);
//         if (sharpenWeight > 0.0) {
//             vec2 onePixel = vec2(1.0) / u_resolution;
//             float scale = clamp(sqrt(u_resolution.x * u_resolution.y) / 512.0, 0.5, 2.0);
//             vec2 offset = onePixel * scale;
//
//             vec4 sum = center * (1.0 + 4.0 * sharpenWeight);
//             sum -= texture2D(u_image, v_texCoord + vec2(-offset.x, 0.0)) * sharpenWeight;
//             sum -= texture2D(u_image, v_texCoord + vec2(offset.x, 0.0)) * sharpenWeight;
//             sum -= texture2D(u_image, v_texCoord + vec2(0.0, -offset.y)) * sharpenWeight;
//             sum -= texture2D(u_image, v_texCoord + vec2(0.0, offset.y)) * sharpenWeight;
//             color = sum;
//         }
//
//         color.rgb = ((color.rgb * u_brightness - 0.5) * u_contrast) + 0.5;
//
//         float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
//         vec3 delta = color.rgb - vec3(gray);
//         vec3 saturated = vec3(gray) + delta * u_saturation;
//
//         if (u_saturation > 1.0) {
//             float redFactor = 1.0 - 0.10 * (u_saturation - 1.0);
//             saturated.r = gray + (color.r - gray) * u_saturation * redFactor;
//         }
//
//         saturated += vec3(max((u_saturation - 1.0) * 0.006, 0.0));
//
//         gl_FragColor = vec4(clamp(saturated, 0.0, 1.0), color.a);
//     }
//     `;
//
// function createShader(gl, type, source) {
//     const shader = gl.createShader(type);
//     gl.shaderSource(shader, source);
//     gl.compileShader(shader);
//     return shader;
// }
//
// function createProgram(gl, vsSource, fsSource) {
//     const program = gl.createProgram();
//     gl.attachShader(program, createShader(gl, gl.VERTEX_SHADER, vsSource));
//     gl.attachShader(program, createShader(gl, gl.FRAGMENT_SHADER, fsSource));
//     gl.linkProgram(program);
//     return program;
// }
//
// function setupImageAdjustments(image, canvas, brightnessInput = 1.0, contrastInput = 1.0, saturationInput = 1.0, sharpnessInput = 1.0) {
//     const startTime = performance.now();
//
//     const gl = canvas.getContext("webgl", { preserveDrawingBuffer: false });
//     const program = createProgram(gl, vertexShaderSource, fragmentShaderSource);
//     gl.useProgram(program);
//     gl.viewport(0, 0, canvas.width, canvas.height);
//
//     // Geometry
//     const positionBuffer = gl.createBuffer();
//     gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
//     gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
//         -1, -1, 1, -1, -1, 1,
//         1, -1, 1, 1, -1, 1
//     ]), gl.STATIC_DRAW);
//     const aPosition = gl.getAttribLocation(program, "a_position");
//     gl.enableVertexAttribArray(aPosition);
//     gl.vertexAttribPointer(aPosition, 2, gl.FLOAT, false, 0, 0);
//
//     const texCoordBuffer = gl.createBuffer();
//     gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
//     gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
//         0, 0, 1, 0, 0, 1,
//         1, 0, 1, 1, 0, 1
//     ]), gl.STATIC_DRAW);
//     const aTexCoord = gl.getAttribLocation(program, "a_texCoord");
//     gl.enableVertexAttribArray(aTexCoord);
//     gl.vertexAttribPointer(aTexCoord, 2, gl.FLOAT, false, 0, 0);
//
//     // Texture
//     const texture = gl.createTexture();
//     gl.bindTexture(gl.TEXTURE_2D, texture);
//     gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
//     gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
//     gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
//     gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
//     gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
//     gl.activeTexture(gl.TEXTURE0);
//     gl.bindTexture(gl.TEXTURE_2D, texture);
//
//     // Uniforms
//     const uniforms = {
//         brightness: gl.getUniformLocation(program, "u_brightness"),
//         contrast: gl.getUniformLocation(program, "u_contrast"),
//         saturation: gl.getUniformLocation(program, "u_saturation"),
//         sharpness: gl.getUniformLocation(program, "u_sharpness"),
//         resolution: gl.getUniformLocation(program, "u_resolution"),
//         image: gl.getUniformLocation(program, "u_image")
//     };
//
//     gl.uniform1f(uniforms.brightness, brightnessInput);
//     gl.uniform1f(uniforms.contrast, contrastInput);
//     gl.uniform1f(uniforms.saturation, saturationInput);
//     gl.uniform1f(uniforms.sharpness, sharpnessInput);
//     gl.uniform2f(uniforms.resolution, canvas.width, canvas.height);
//     gl.uniform1i(uniforms.image, 0);
//
//     // Draw
//     gl.drawArrays(gl.TRIANGLES, 0, 6);
//
//     // Show updated image - expensive
//     document.getElementById("editShashinImage").src = canvas.toDataURL("image/jpeg", 0.2);
//
//     const endTime = performance.now();
//     shashin.printMessageToConsole(`Call to setupImageAdjustments took ${Math.trunc(endTime - startTime)} milliseconds`,{tag:"editor"});
// }