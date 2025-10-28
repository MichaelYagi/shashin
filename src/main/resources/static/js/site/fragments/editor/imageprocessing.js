// Optimized setupImageAdjustments.js
// - Caches WebGL resources per-canvas (program, buffers, texture, locations) to avoid
//   recompiling/linking shaders and recreating buffers/textures on repeated calls.
// - Uses texSubImage2D when possible to avoid reallocating texture storage.
// - Minimizes GL calls where safe and updates only when inputs change.
// - Keeps the same function names and signatures as requested.

// Usage: same as original. Make sure to call after DOMContentLoaded or place script after elements.

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

// Per-canvas cache of GL resources. WeakMap ensures canvas GC is not prevented.
const canvasGLCache = new WeakMap();

function initResourcesForCanvas(canvas) {
    // Returns an object with gl, program, buffers, locations, texture, etc.
    const gl = canvas.getContext('webgl', { preserveDrawingBuffer: false });
    if (!gl) return null;

    const program = createProgram(gl, vertexShaderSource, fragmentShaderSource);
    gl.useProgram(program);

    // Create and populate static buffers (positions & texcoords)
    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    // Fullscreen triangle pair (6 verts)
    const positions = new Float32Array([
        -1, -1, 1, -1, -1, 1,
        1, -1, 1,  1, -1, 1
    ]);
    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW);

    const aPosition = gl.getAttribLocation(program, 'a_position');
    if (aPosition >= 0) {
        gl.enableVertexAttribArray(aPosition);
        // vertexAttribPointer binds to the currently bound ARRAY_BUFFER (positionBuffer)
        gl.vertexAttribPointer(aPosition, 2, gl.FLOAT, false, 0, 0);
    }

    const texCoordBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
    const texcoords = new Float32Array([
        0, 0, 1, 0, 0, 1,
        1, 0, 1, 1, 0, 1
    ]);
    gl.bufferData(gl.ARRAY_BUFFER, texcoords, gl.STATIC_DRAW);

    const aTexCoord = gl.getAttribLocation(program, 'a_texCoord');
    if (aTexCoord >= 0) {
        gl.enableVertexAttribArray(aTexCoord);
        gl.vertexAttribPointer(aTexCoord, 2, gl.FLOAT, false, 0, 0);
    }

    // Create texture (but do not upload image yet)
    const texture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, texture);
    // Default parameters (safe for NPOT textures)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);

    // Cache uniform/attribute locations
    const uniforms = {
        brightness: gl.getUniformLocation(program, 'u_brightness'),
        contrast: gl.getUniformLocation(program, 'u_contrast'),
        saturation: gl.getUniformLocation(program, 'u_saturation'),
        sharpness: gl.getUniformLocation(program, 'u_sharpness'),
        resolution: gl.getUniformLocation(program, 'u_resolution'),
        image: gl.getUniformLocation(program, 'u_image')
    };

    // Initial state
    const resources = {
        gl,
        program,
        positionBuffer,
        texCoordBuffer,
        texture,
        uniforms,
        lastImageWidth: 0,
        lastImageHeight: 0,
        lastParams: {
            brightness: null,
            contrast: null,
            saturation: null,
            sharpness: null,
            width: null,
            height: null
        }
    };

    // Store in cache
    canvasGLCache.set(canvas, resources);
    return resources;
}

/**
 * Applies image adjustments and updates #editShashinImage src.
 * - image: HTMLImageElement/HTMLCanvasElement/HTMLVideoElement
 * - canvasOrId: either a <canvas> element or its id string
 * - brightnessInput, contrastInput, saturationInput, sharpnessInput: numbers
 *
 * Returns a Promise that resolves with true/false depending on success.
 * (Preserves original signature and general behavior.)
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
            if (typeof shashin !== 'undefined' && shashin.printMessageToConsole) {
                shashin.printMessageToConsole(msg, { tag: "editor", consoleType: shashin.consoleTypes.error });
            }
            reject(new Error(msg));
            return;
        }

        // Ensure image has dimensions
        const imgWidth = image.width || image.naturalWidth || image.videoWidth;
        const imgHeight = image.height || image.naturalHeight || image.videoHeight;
        if (!imgWidth || !imgHeight) {
            const msg = 'Image has no dimensions. Ensure image is loaded before calling setupImageAdjustments.';
            if (typeof shashin !== 'undefined' && shashin.printMessageToConsole) {
                shashin.printMessageToConsole(msg, { tag: "editor", consoleType: shashin.consoleTypes.error });
            }
            reject(new Error(msg));
            return;
        }

        // Resize canvas only if necessary
        if (canvas.width !== imgWidth || canvas.height !== imgHeight) {
            canvas.width = imgWidth;
            canvas.height = imgHeight;
        }

        // Get or initialize cached resources for this canvas
        let resources = canvasGLCache.get(canvas);
        if (!resources) {
            resources = initResourcesForCanvas(canvas);
            if (!resources) {
                const msg = 'WebGL not supported or failed to initialize.';
                if (typeof shashin !== 'undefined' && shashin.printMessageToConsole) {
                    shashin.printMessageToConsole(msg, { tag: "editor", consoleType: shashin.consoleTypes.error });
                }
                reject(new Error(msg));
                return;
            }
        }

        const gl = resources.gl;
        const program = resources.program;
        gl.useProgram(program);

        // Update viewport & resolution uniform if changed
        gl.viewport(0, 0, canvas.width, canvas.height);
        if (resources.lastParams.width !== canvas.width || resources.lastParams.height !== canvas.height) {
            if (resources.uniforms.resolution) {
                gl.uniform2f(resources.uniforms.resolution, canvas.width, canvas.height);
            }
            resources.lastParams.width = canvas.width;
            resources.lastParams.height = canvas.height;
        }

        // Update uniforms only if changed (reduces gl calls)
        if (resources.lastParams.brightness !== brightnessInput && resources.uniforms.brightness) {
            gl.uniform1f(resources.uniforms.brightness, brightnessInput);
            resources.lastParams.brightness = brightnessInput;
        }
        if (resources.lastParams.contrast !== contrastInput && resources.uniforms.contrast) {
            gl.uniform1f(resources.uniforms.contrast, contrastInput);
            resources.lastParams.contrast = contrastInput;
        }
        if (resources.lastParams.saturation !== saturationInput && resources.uniforms.saturation) {
            gl.uniform1f(resources.uniforms.saturation, saturationInput);
            resources.lastParams.saturation = saturationInput;
        }
        if (resources.lastParams.sharpness !== sharpnessInput && resources.uniforms.sharpness) {
            gl.uniform1f(resources.uniforms.sharpness, sharpnessInput);
            resources.lastParams.sharpness = sharpnessInput;
        }
        if (resources.uniforms.image) {
            // texture unit 0
            gl.uniform1i(resources.uniforms.image, 0);
        }

        // Bind buffers just in case (cheap if same)
        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        const aPos = gl.getAttribLocation(program, 'a_position');
        if (aPos >= 0) {
            // Ensure pointer remains correct (binding to same buffer)
            gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);
        }
        gl.bindBuffer(gl.ARRAY_BUFFER, resources.texCoordBuffer);
        const aTex = gl.getAttribLocation(program, 'a_texCoord');
        if (aTex >= 0) {
            gl.vertexAttribPointer(aTex, 2, gl.FLOAT, false, 0, 0);
        }

        // Upload image to texture.
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, resources.texture);
        // Flip Y so image orientation matches canvas
        gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);

        // If texture size matches previous, use texSubImage2D (avoids realloc)
        const sameSize = resources.lastImageWidth === imgWidth && resources.lastImageHeight === imgHeight;
        try {
            if (sameSize && resources.lastImageWidth > 0) {
                // texSubImage2D can be faster because it does not reallocate texture storage
                gl.texSubImage2D(gl.TEXTURE_2D, 0, 0, 0, gl.RGBA, gl.UNSIGNED_BYTE, image);
            } else {
                // Allocate/replace texture
                gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
                resources.lastImageWidth = imgWidth;
                resources.lastImageHeight = imgHeight;
            }
        } catch (err) {
            if (typeof shashin !== 'undefined' && shashin.printMessageToConsole) {
                shashin.printMessageToConsole(`'texImage2D/texSubImage2D failed - image may not be ready: ${err}`, { tag: "editor", consoleType: shashin.consoleTypes.error });
            }
            reject(err);
            return;
        }

        // Draw fullscreen triangles
        gl.drawArrays(gl.TRIANGLES, 0, 6);

        // Use toBlob + createObjectURL (async)
        canvas.toBlob((blob) => {
            if (!blob) {
                resolve(false);
                return;
            }
            const url = URL.createObjectURL(blob);
            const imgEl = $("#editShashinImage");
            if (imgEl) {
                imgEl.off("load").on("load", () => {
                    resolve(true);
                });
                imgEl.attr("src", url);
            } else {
                resolve(false);
                return;
            }
            const endTime = performance.now();
            if (typeof shashin !== 'undefined' && shashin.printMessageToConsole) {
                shashin.printMessageToConsole(`Call to setupImageAdjustments took ${Math.trunc(endTime - startTime)} milliseconds`, { tag: "editor" });
            }
            resolve(true);
        }, 'image/jpeg', 0.2);
    });
}

// Export for module usage (uncomment if using modules)
// export { setupImageAdjustments };