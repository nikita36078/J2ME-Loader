#ifndef M3G_CORE_INCLUDE
#   error included by m3g_core.c; do not compile separately.
#endif

#include "m3g_shader.h"

static const char *gVertexShader =
"attribute vec4 vPosition;\n"
"uniform mat4 uMVPMatrix;\n"
"void main() {\n"
"  gl_Position = vPosition;\n"
"}\n";

static const char *gFragmentShader =
"precision mediump float;\n"
"void main() {\n"
"  gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n"
"}\n";

const GLfloat identityMatrix[16] =
        {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
        };

GLfloat projMatrix[16];
GLfloat mvpMatrix[16];
GLfloat modelMatrix[16];

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

GLuint glProgram;
GLuint glPositionHandle;
GLuint glMVPHandle;

void m3gInitShaders() {
    glProgram = createProgram(gVertexShader, gFragmentShader);
    if (!glProgram) {
        M3G_LOG(M3G_LOG_USER_ERRORS, "Could not create program.");
        return;
    }
    glPositionHandle = (GLuint) glGetAttribLocation(glProgram, "vPosition");
    checkGlError("glGetAttribLocation");
    glMVPHandle = (GLuint) glGetUniformLocation(glProgram, "uMVPMatrix");
    checkGlError("glGetUniformLocation");
}

void m3gLoadVertices(GLint size, GLenum type, GLsizei stride, const void *pointer) {
    glUseProgram(glProgram);
    checkGlError("glUseProgram");
    glVertexAttribPointer(glPositionHandle, size, type, GL_FALSE, stride, pointer);
    checkGlError("glVertexAttribPointer");
    glEnableVertexAttribArray(glPositionHandle);
    checkGlError("glEnableVertexAttribArray");
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
    glUniformMatrix4fv(glMVPHandle, 1, GL_FALSE, mvpMatrix);
    checkGlError("glUniformMatrix4fv");
    glDrawElements(mode, count, type, indices);
    checkGlError("glDrawElements");
}

void m3gLoadProjectionMatrix(M3Gfloat* matr) {
    m3gCopy(projMatrix, matr, sizeof(projMatrix));
}

void m3gLoadModelMatrix(M3Gfloat* matr) {
    m3gCopy(modelMatrix, matr, sizeof(modelMatrix));
}

void m3gIdentityProjectionMatrix() {
    m3gCopy(projMatrix, identityMatrix, sizeof(projMatrix));
}

void m3gIdentityModelMatrix() {
    m3gCopy(modelMatrix, identityMatrix, sizeof(modelMatrix));
}

void m3gMultProjectionMatrix(M3Gfloat* matr) {
    multiplyMatrix(projMatrix, matr, projMatrix);
}

void m3gMultModelMatrix(M3Gfloat* matr) {
    multiplyMatrix(modelMatrix, matr, modelMatrix);
}
