//
// Created by User on 09.12.2019.
//

#ifndef __M3G_SHADER_H__
#define __M3G_SHADER_H__

void m3gInitShaders();
void m3gLoadVertices(GLint size, GLenum type, GLsizei stride, const void *pointer);
void m3gDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices);
void m3gLoadProjectionMatrix(M3Gfloat* matr);
void m3gLoadModelMatrix(M3Gfloat* matr);

void m3gIdentityProjectionMatrix();
void m3gIdentityModelMatrix();
void m3gMultProjectionMatrix(M3Gfloat* matr);
void m3gMultModelMatrix(M3Gfloat* matr);

#endif //__M3G_SHADER_H__
