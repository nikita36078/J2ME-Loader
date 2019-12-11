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
    glUniformMatrix4fv(glMVPHandle, 1, GL_FALSE, identityMatrix);
    checkGlError("glUniformMatrix4fv");
}

void m3gDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices) {
    glDrawElements(mode, count, type, indices);
    checkGlError("glDrawElements");
}
