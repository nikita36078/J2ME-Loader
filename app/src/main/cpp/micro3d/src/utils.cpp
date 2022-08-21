//
// Created by woesss on 11.07.2020.
//

#include "utils.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_mascotcapsule_micro3d_v3_Utils_fillBuffer
        (JNIEnv *env, jclass /*clazz*/,
         jobject buffer, jobject vertices, jintArray indices) {
    auto bufPtr = static_cast<Vec3f *>(env->GetDirectBufferAddress(buffer));
    jsize len = env->GetArrayLength(indices);
    auto indexPtr = env->GetIntArrayElements(indices, nullptr);
    auto vertPtr = static_cast<Vec3f *>(env->GetDirectBufferAddress(vertices));
    for (int i = 0; i < len; ++i) {
        auto src = &vertPtr[indexPtr[i]];
        auto dst = bufPtr++;
        dst->x = src->x;
        dst->y = src->y;
        dst->z = src->z;
    }
    env->ReleaseIntArrayElements(indices, indexPtr, 0);
}

JNIEXPORT void JNICALL Java_com_mascotcapsule_micro3d_v3_Utils_glReadPixels
        (JNIEnv *env, jclass /*clazz*/,
         jint x, jint y, jint width, jint height, jobject bitmap_buffer) {
    int ret;
    AndroidBitmapInfo info;
    ret = AndroidBitmap_getInfo(env, bitmap_buffer, &info);
    if (ret < 0) {
        LOGE("AndroidBitmap_getInfo() failed! error=%d", ret)
        MICRO3D_RAISE_EXCEPTION(env, "java/lang/IllegalStateException",
                                "AndroidBitmap_getInfo() failed!")
        return;
    }
    void *pixels;
    ret = AndroidBitmap_lockPixels(env, bitmap_buffer, &pixels);
    if (ret < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret)
        MICRO3D_RAISE_EXCEPTION(env, "java/lang/IllegalStateException",
                                "AndroidBitmap_lockPixels() failed!")
        return;
    }
    const uint32_t bw = info.width;
    const uint32_t bs = info.stride;
    if (x == 0 && width == bw) {
        pixels = ((uint8_t *) pixels) + bs * y;
        glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    } else {
        pixels = ((uint8_t *) pixels) + x * 4 /*RGBA*/ + bs * y;
        for (int i = 0; i < height; ++i) {
            glReadPixels(x, y + i, width, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            pixels = ((uint8_t *) pixels) + bs;
        }
    }
    ret = AndroidBitmap_unlockPixels(env, bitmap_buffer);
    pixels = nullptr;
    if (ret < 0) {
        LOGE("AndroidBitmap_unlockPixels() failed! error=%d", ret)
        MICRO3D_RAISE_EXCEPTION(env, "java/lang/IllegalStateException",
                                "AndroidBitmap_unlockPixels() failed!")
    }
}

JNIEXPORT void JNICALL
Java_com_mascotcapsule_micro3d_v3_Utils_transform(JNIEnv *env, jclass /*clazz*/,
                                                  jobject src_vertices,
                                                  jobject dst_vertices,
                                                  jobject src_normals,
                                                  jobject dst_normals,
                                                  jobject aBones,
                                                  jfloatArray action_matrices) {
    auto srcVert = static_cast<Vec3f *>(env->GetDirectBufferAddress(src_vertices));
    auto dstVert = static_cast<Vec3f *>(env->GetDirectBufferAddress(dst_vertices));
    Vec3f *srcNorm;
    Vec3f *dstNorm;
    if (src_normals == nullptr) {
        srcNorm = nullptr;
        dstNorm = nullptr;
    } else {
        srcNorm = static_cast<Vec3f *>(env->GetDirectBufferAddress(src_normals));
        dstNorm = static_cast<Vec3f *>(env->GetDirectBufferAddress(dst_normals));
    }
    auto bones = static_cast<Bone *>(env->GetDirectBufferAddress(aBones));
    jlong bonesLen = env->GetDirectBufferCapacity(aBones) / sizeof(Bone);
    jsize actionsLen = 0;
    float *actionsPtr = nullptr;
    Matrix *actions = nullptr;
    if (action_matrices != nullptr) {
        actionsPtr = env->GetFloatArrayElements(action_matrices, nullptr);
        actionsLen = env->GetArrayLength(action_matrices) / 12;
        actions = reinterpret_cast<Matrix *>(actionsPtr);
    }
    auto tmp = new Matrix[bonesLen];
    for (int i = 0; i < bonesLen; ++i) {
        Bone *bone = &bones[i];
        int parent = bone->parent;
        Matrix *matrix = &tmp[i];
        if (parent == -1) {
            memcpy(matrix, &bone->matrix, sizeof(Matrix));
        } else {
            multiplyMM(matrix, &tmp[parent], &bone->matrix);
        }
        if (i < actionsLen) {
            multiplyMM(matrix, matrix, actions++);
        }
        auto boneLen = bone->length;
        for (int j = 0; j < boneLen; ++j) {
            multiplyMV(dstVert++, srcVert++, matrix);

            if (srcNorm != nullptr) {
                multiplyMN(dstNorm++, srcNorm++, matrix);
            }
        }
    }
    delete[] tmp;
    if (action_matrices != nullptr) {
        env->ReleaseFloatArrayElements(action_matrices, actionsPtr, 0);
    }
}

static void multiplyMM(Matrix *m, Matrix *lm, Matrix *rm) {
    float l00 = lm->m00;
    float l01 = lm->m01;
    float l02 = lm->m02;
    float l10 = lm->m10;
    float l11 = lm->m11;
    float l12 = lm->m12;
    float l20 = lm->m20;
    float l21 = lm->m21;
    float l22 = lm->m22;
    float r00 = rm->m00;
    float r01 = rm->m01;
    float r02 = rm->m02;
    float r03 = rm->m03;
    float r10 = rm->m10;
    float r11 = rm->m11;
    float r12 = rm->m12;
    float r13 = rm->m13;
    float r20 = rm->m20;
    float r21 = rm->m21;
    float r22 = rm->m22;
    float r23 = rm->m23;

    m->m00 = l00 * r00 + l01 * r10 + l02 * r20;
    m->m01 = l00 * r01 + l01 * r11 + l02 * r21;
    m->m02 = l00 * r02 + l01 * r12 + l02 * r22;
    m->m03 = l00 * r03 + l01 * r13 + l02 * r23 + lm->m03;
    m->m10 = l10 * r00 + l11 * r10 + l12 * r20;
    m->m11 = l10 * r01 + l11 * r11 + l12 * r21;
    m->m12 = l10 * r02 + l11 * r12 + l12 * r22;
    m->m13 = l10 * r03 + l11 * r13 + l12 * r23 + lm->m13;
    m->m20 = l20 * r00 + l21 * r10 + l22 * r20;
    m->m21 = l20 * r01 + l21 * r11 + l22 * r21;
    m->m22 = l20 * r02 + l21 * r12 + l22 * r22;
    m->m23 = l20 * r03 + l21 * r13 + l22 * r23 + lm->m23;
}

static void multiplyMV(Vec3f *dst, Vec3f *src, Matrix *matrix) {
    float x = src->x;
    float y = src->y;
    float z = src->z;
    dst->x = x * matrix->m00 + y * matrix->m01 + z * matrix->m02 + matrix->m03;
    dst->y = x * matrix->m10 + y * matrix->m11 + z * matrix->m12 + matrix->m13;
    dst->z = x * matrix->m20 + y * matrix->m21 + z * matrix->m22 + matrix->m23;
}

static void multiplyMN(Vec3f *dst, Vec3f *src, Matrix *matrix) {
    float x = src->x;
    float y = src->y;
    float z = src->z;
    dst->x = x * matrix->m00 + y * matrix->m01 + z * matrix->m02;
    dst->y = x * matrix->m10 + y * matrix->m11 + z * matrix->m12;
    dst->z = x * matrix->m20 + y * matrix->m21 + z * matrix->m22;
}

#ifdef __cplusplus
}
#endif
