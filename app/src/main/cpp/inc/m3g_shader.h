//
// Created by User on 09.12.2019.
//

#ifndef __M3G_SHADER_H__
#define __M3G_SHADER_H__

void m3gInitShaders();
void m3gLoadVertices(GLint size, GLenum type, GLsizei stride, const void *pointer);
void m3gDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices);

#endif //__M3G_SHADER_H__
