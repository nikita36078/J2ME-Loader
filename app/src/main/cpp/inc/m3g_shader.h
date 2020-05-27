//
// Created by User on 09.12.2019.
//

#ifndef __M3G_SHADER_H__
#define __M3G_SHADER_H__

void m3gInitShaders();
void m3gSetShaderMode(M3Genum mode);
void m3gLoadVertices(GLint size, GLenum type, GLsizei stride, const void *pointer);
void m3gLoadTexCoords(GLint size, GLenum type, GLsizei stride, const void *pointer);
void m3gLoadColors(GLint size, GLenum type, GLsizei stride, const void *pointer);
void m3gDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices);
void m3gDrawArrays(GLenum mode, GLsizei count, GLenum type);
void m3gLoadProjectionMatrix(M3Gfloat* matr);
void m3gLoadModelMatrix(M3Gfloat* matr);
void m3gLoadTextureMatrix(M3Gfloat* matr);

void m3gIdentityProjectionMatrix();
void m3gIdentityModelMatrix();
void m3gIdentityTextureMatrix();
void m3gMultProjectionMatrix(M3Gfloat* matr);
void m3gMultModelMatrix(M3Gfloat* matr);
void m3gOrtho(M3Gfloat left, M3Gfloat right, M3Gfloat bottom, M3Gfloat top,
        M3Gfloat near, M3Gfloat far);
void m3gScaleModel(M3Gfloat x, M3Gfloat y, M3Gfloat z);
void m3gTranslateModel(M3Gfloat x, M3Gfloat y, M3Gfloat z);
void m3gScaleTexture(M3Gfloat x, M3Gfloat y, M3Gfloat z);
void m3gTranslateTexture(M3Gfloat x, M3Gfloat y, M3Gfloat z);

#endif //__M3G_SHADER_H__
