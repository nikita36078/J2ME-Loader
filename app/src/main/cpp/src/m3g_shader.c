#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_shader.h"

static const char *gTVertexShader =
        "attribute vec4 aPosition;\n"
        "uniform mat4 uMVPMatrix;\n"
        "uniform mat4 uTexMatrix;\n"
        "attribute vec2 aTexture;\n"
        "varying vec2 vTexture;\n"
        "void main() {\n"
        "  gl_Position = uMVPMatrix * aPosition;\n"
        "  vec4 texCoord = vec4(aTexture, 0.0, 1.0);\n"
        "  vec4 res = uTexMatrix * texCoord;\n"
        "  vTexture = vec2(res.x, res.y);\n"
        "}\n";

static const char *gTFragmentShader =
        "precision mediump float;\n"
        "uniform sampler2D uTextureUnit;\n"
        "varying vec2 vTexture;\n"
        "void main() {\n"
        "  gl_FragColor = texture2D(uTextureUnit, vTexture);\n"
        "}\n";

static const char *gCVertexShader =
        "attribute vec4 aPosition;\n"
        "uniform mat4 uMVPMatrix;\n"
        "attribute vec4 aColor;\n"
        "varying vec4 vColor;\n"
        "void main() {\n"
        "  gl_Position = uMVPMatrix * aPosition;\n"
        "  vColor = aColor;\n"
        "}\n";

static const char *gCFragmentShader =
        "precision mediump float;\n"
        "varying vec4 vColor;\n"
        "void main() {\n"
        "  gl_FragColor = vColor;\n"
        "}\n";

const GLfloat identityMatrix[16] =
        {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
        };

const GLfloat identityTextureMatrix[16] =
        {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
        };

GLfloat projMatrix[16];
GLfloat mvpMatrix[16];
GLfloat modelMatrix[16];
GLfloat textureMatrix[16];

GLuint glTProgram;
GLuint glTPositionHandle;
GLuint glTTextureHandle;
GLuint glTTextureUnitHandle;
GLuint glTMVPHandle;
GLuint glTTexMHandle;

GLuint glCProgram;
GLuint glCPositionHandle;
GLuint glCColorHandle;
GLuint glCMVPHandle;

M3Gbool inited = M3G_FALSE;
M3Genum shaderMode = M3G_SHADER_TEXTURE;

static void printGLString(const char *name, GLenum s) {
    const char *v = (const char *) glGetString(s);
    M3G_LOG2(M3G_LOG_USER_ERRORS, "GL %s = %s\n", name, v);
}

static void checkGlError(const char* op) {
    for (GLint error = glGetError(); error; error = glGetError()) {
        M3G_LOG2(M3G_LOG_USER_ERRORS, "after %s() glError (0x%x)\n", op, error);
    }
}

GLuint loadShader(GLenum shaderType, const char* pSource) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    M3G_LOG2(M3G_LOG_USER_ERRORS, "Could not compile shader %d:\n%s\n", shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}

GLuint createProgram(const char* pVertexSource, const char* pFragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, pFragmentSource);
    if (!pixelShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        glLinkProgram(program);
        checkGlError("glLinkProgram");
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        checkGlError("glGetProgramiv");
        if (linkStatus != GL_TRUE) {
            M3G_LOG(M3G_LOG_USER_ERRORS, "Could not link program");
            GLint bufLength = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(program, bufLength, NULL, buf);
                    M3G_LOG1(M3G_LOG_USER_ERRORS, "Could not link program:\n%s\n", buf);
                    free(buf);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

void m3gInitShaders() {
    if (!inited) {
        glTProgram = createProgram(gTVertexShader, gTFragmentShader);
        if (!glTProgram) {
            M3G_LOG(M3G_LOG_USER_ERRORS, "Could not create program.");
            return;
        }
        glTPositionHandle = (GLuint) glGetAttribLocation(glTProgram, "aPosition");
        checkGlError("glGetAttribLocation");
        glTTextureHandle = (GLuint) glGetAttribLocation(glTProgram, "aTexture");
        checkGlError("glGetAttribLocation");
        glTTextureUnitHandle = (GLuint) glGetUniformLocation(glTProgram, "uTextureUnit");
        checkGlError("glGetUniformLocation");
        glTMVPHandle = (GLuint) glGetUniformLocation(glTProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation");
        glTTexMHandle = (GLuint) glGetUniformLocation(glTProgram, "uTexMatrix");
        checkGlError("glGetUniformLocation");

        glCProgram = createProgram(gCVertexShader, gCFragmentShader);
        if (!glCProgram) {
            M3G_LOG(M3G_LOG_USER_ERRORS, "Could not create program.");
            return;
        }
        glCPositionHandle = (GLuint) glGetAttribLocation(glCProgram, "aPosition");
        checkGlError("glGetAttribLocation");
        glCColorHandle = (GLuint) glGetAttribLocation(glCProgram, "aColor");
        checkGlError("glGetAttribLocation");
        glCMVPHandle = (GLuint) glGetUniformLocation(glCProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation");
        inited = M3G_TRUE;
    }
}

void m3gSetShaderMode(M3Genum mode) {
    shaderMode = mode;
    if (shaderMode == M3G_SHADER_TEXTURE) {
        glUseProgram(glTProgram);
        checkGlError("glUseProgram");
    } else {
        glUseProgram(glCProgram);
        checkGlError("glUseProgram");
    }
}

void m3gLoadVertices(GLint size, GLenum type, GLsizei stride, const void *pointer) {
    if (shaderMode == M3G_SHADER_TEXTURE) {
        glVertexAttribPointer(glTPositionHandle, size, type, GL_FALSE, stride, pointer);
        checkGlError("glVertexAttribPointer");
        glEnableVertexAttribArray(glTPositionHandle);
        checkGlError("glEnableVertexAttribArray");
    } else {
        glVertexAttribPointer(glCPositionHandle, size, type, GL_FALSE, stride, pointer);
        checkGlError("glVertexAttribPointer");
        glEnableVertexAttribArray(glCPositionHandle);
        checkGlError("glEnableVertexAttribArray");
    }
}

void m3gLoadTexCoords(GLint size, GLenum type, GLsizei stride, const void *pointer) {
    if (shaderMode == M3G_SHADER_TEXTURE) {
        glVertexAttribPointer(glTTextureHandle, size, type, GL_FALSE, stride, pointer);
        checkGlError("glVertexAttribPointer");
        glEnableVertexAttribArray(glTTextureHandle);
        checkGlError("glEnableVertexAttribArray");
        glUniform1i(glTTextureUnitHandle, 0);
        checkGlError("glUniform1i");
    }
}

void m3gLoadColors(GLint size, GLenum type, GLsizei stride, const void *pointer) {
    if (shaderMode == M3G_SHADER_COLOR) {
        glVertexAttribPointer(glCColorHandle, size, type, GL_FALSE, stride, pointer);
        checkGlError("glVertexAttribPointer");
        glEnableVertexAttribArray(glCColorHandle);
        checkGlError("glEnableVertexAttribArray");
    }
}

M3Gfloat* multiplyMatrix(M3Gfloat *mat, M3Gfloat *mat2, M3Gfloat *dest) {
    if (!dest) { dest = mat; }

    // Cache the matrix values (makes for huge speed increases!)
    M3Gfloat a00 = mat[0], a01 = mat[1], a02 = mat[2], a03 = mat[3],
            a10 = mat[4], a11 = mat[5], a12 = mat[6], a13 = mat[7],
            a20 = mat[8], a21 = mat[9], a22 = mat[10], a23 = mat[11],
            a30 = mat[12], a31 = mat[13], a32 = mat[14], a33 = mat[15],

            b00 = mat2[0], b01 = mat2[1], b02 = mat2[2], b03 = mat2[3],
            b10 = mat2[4], b11 = mat2[5], b12 = mat2[6], b13 = mat2[7],
            b20 = mat2[8], b21 = mat2[9], b22 = mat2[10], b23 = mat2[11],
            b30 = mat2[12], b31 = mat2[13], b32 = mat2[14], b33 = mat2[15];

    dest[0] = b00 * a00 + b01 * a10 + b02 * a20 + b03 * a30;
    dest[1] = b00 * a01 + b01 * a11 + b02 * a21 + b03 * a31;
    dest[2] = b00 * a02 + b01 * a12 + b02 * a22 + b03 * a32;
    dest[3] = b00 * a03 + b01 * a13 + b02 * a23 + b03 * a33;
    dest[4] = b10 * a00 + b11 * a10 + b12 * a20 + b13 * a30;
    dest[5] = b10 * a01 + b11 * a11 + b12 * a21 + b13 * a31;
    dest[6] = b10 * a02 + b11 * a12 + b12 * a22 + b13 * a32;
    dest[7] = b10 * a03 + b11 * a13 + b12 * a23 + b13 * a33;
    dest[8] = b20 * a00 + b21 * a10 + b22 * a20 + b23 * a30;
    dest[9] = b20 * a01 + b21 * a11 + b22 * a21 + b23 * a31;
    dest[10] = b20 * a02 + b21 * a12 + b22 * a22 + b23 * a32;
    dest[11] = b20 * a03 + b21 * a13 + b22 * a23 + b23 * a33;
    dest[12] = b30 * a00 + b31 * a10 + b32 * a20 + b33 * a30;
    dest[13] = b30 * a01 + b31 * a11 + b32 * a21 + b33 * a31;
    dest[14] = b30 * a02 + b31 * a12 + b32 * a22 + b33 * a32;
    dest[15] = b30 * a03 + b31 * a13 + b32 * a23 + b33 * a33;

    return dest;
}

void m3gDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices) {
    multiplyMatrix(projMatrix, modelMatrix, mvpMatrix);
    if (shaderMode == M3G_SHADER_TEXTURE) {
        glUniformMatrix4fv(glTMVPHandle, 1, GL_FALSE, mvpMatrix);
        checkGlError("glUniformMatrix4fv");
        glUniformMatrix4fv(glTTexMHandle, 1, GL_FALSE, textureMatrix);
        checkGlError("glUniformMatrix4fv");
    } else {
        glUniformMatrix4fv(glCMVPHandle, 1, GL_FALSE, mvpMatrix);
        checkGlError("glUniformMatrix4fv");
    }
    glDrawElements(mode, count, type, indices);
    checkGlError("glDrawElements");
}

void m3gDrawArrays(GLenum mode, GLsizei count, GLenum type) {
    multiplyMatrix(projMatrix, modelMatrix, mvpMatrix);

    glUniformMatrix4fv(glTMVPHandle, 1, GL_FALSE, mvpMatrix);
    checkGlError("glUniformMatrix4fv");
    glUniformMatrix4fv(glTTexMHandle, 1, GL_FALSE, textureMatrix);
    checkGlError("glUniformMatrix4fv");

    glDrawArrays(mode, count, type);
    checkGlError("glDrawElements");
}

void m3gLoadProjectionMatrix(M3Gfloat* matr) {
    m3gCopy(projMatrix, matr, sizeof(projMatrix));
}

void m3gLoadModelMatrix(M3Gfloat* matr) {
    m3gCopy(modelMatrix, matr, sizeof(modelMatrix));
}

void m3gLoadTextureMatrix(M3Gfloat* matr) {
    m3gCopy(textureMatrix, matr, sizeof(textureMatrix));
}

void m3gIdentityProjectionMatrix() {
    m3gCopy(projMatrix, identityMatrix, sizeof(projMatrix));
}

void m3gIdentityModelMatrix() {
    m3gCopy(modelMatrix, identityMatrix, sizeof(modelMatrix));
}

void m3gIdentityTextureMatrix() {
    m3gCopy(textureMatrix, identityTextureMatrix, sizeof(textureMatrix));
}

void m3gMultProjectionMatrix(M3Gfloat* matr) {
    multiplyMatrix(projMatrix, matr, projMatrix);
}

void m3gMultModelMatrix(M3Gfloat* matr) {
    multiplyMatrix(modelMatrix, matr, modelMatrix);
}

void m3gOrtho(M3Gfloat left, M3Gfloat right, M3Gfloat bottom, M3Gfloat top,
              M3Gfloat near, M3Gfloat far) {
    M3Gfloat r_width  = 1.0f / (right - left);
    M3Gfloat r_height = 1.0f / (top - bottom);
    M3Gfloat r_depth  = 1.0f / (far - near);
    M3Gfloat x =  2.0f * (r_width);
    M3Gfloat y =  2.0f * (r_height);
    M3Gfloat z = -2.0f * (r_depth);
    M3Gfloat tx = -(right + left) * r_width;
    M3Gfloat ty = -(top + bottom) * r_height;
    M3Gfloat tz = -(far + near) * r_depth;
    projMatrix[0] = x;
    projMatrix[5] = y;
    projMatrix[10] = z;
    projMatrix[12] = tx;
    projMatrix[13] = ty;
    projMatrix[14] = tz;
    projMatrix[15] = 1.0f;
    projMatrix[1] = 0.0f;
    projMatrix[2] = 0.0f;
    projMatrix[3] = 0.0f;
    projMatrix[4] = 0.0f;
    projMatrix[6] = 0.0f;
    projMatrix[7] = 0.0f;
    projMatrix[8] = 0.0f;
    projMatrix[9] = 0.0f;
    projMatrix[11] = 0.0f;
}

void m3gScaleModel(M3Gfloat x, M3Gfloat y, M3Gfloat z) {
    for (int i=0 ; i<4 ; i++) {
        modelMatrix[i] *= x;
        modelMatrix[4 + i] *= y;
        modelMatrix[8 + i] *= z;
    }
}

void m3gTranslateModel(M3Gfloat x, M3Gfloat y, M3Gfloat z) {
    for (int i=0 ; i<4 ; i++) {
        modelMatrix[12 + i] += modelMatrix[i] * x + modelMatrix[4 + i] * y + modelMatrix[8 + i] * z;
    }
}

void m3gScaleTexture(M3Gfloat x, M3Gfloat y, M3Gfloat z) {
    for (int i=0 ; i<4 ; i++) {
        textureMatrix[i] *= x;
        textureMatrix[4 + i] *= y;
        textureMatrix[8 + i] *= z;
    }
}

void m3gTranslateTexture(M3Gfloat x, M3Gfloat y, M3Gfloat z) {
    for (int i=0 ; i<4 ; i++) {
        textureMatrix[12 + i] += textureMatrix[i] * x + textureMatrix[4 + i] * y + textureMatrix[8 + i] * z;
    }
}
