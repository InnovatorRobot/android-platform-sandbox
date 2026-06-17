#include "image_filter.h"

#include <android/log.h>
#include <jni.h>

#define LOG_TAG "ImageProcessor"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// ── Lifecycle
// ─────────────────────────────────────────────────────────────────

JNIEXPORT jlong JNICALL
Java_com_mediaplatform_nativebridge_ImageProcessorEngine_nativeCreate(
    JNIEnv * /*env*/, jobject /*thiz*/) {
  return reinterpret_cast<jlong>(new image_filter::ImageFilter());
}

JNIEXPORT void JNICALL
Java_com_mediaplatform_nativebridge_ImageProcessorEngine_nativeDestroy(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong ptr) {
  delete reinterpret_cast<image_filter::ImageFilter *>(ptr);
}

// ── Filter
// ────────────────────────────────────────────────────────────────────

/**
 * Apply a filter to an ARGB_8888 pixel array.
 * @param pixels     Input int[] from Bitmap.getPixels()
 * @param width      Frame width
 * @param height     Frame height
 * @param filterType int matching image_filter::FilterType enum
 * @return           New int[] with filtered pixels, same dimensions
 */
JNIEXPORT jintArray JNICALL
Java_com_mediaplatform_nativebridge_ImageProcessorEngine_nativeApplyFilter(
    JNIEnv *env, jobject /*thiz*/, jlong ptr, jintArray pixels, jint width,
    jint height, jint filterType) {

  auto *filter = reinterpret_cast<image_filter::ImageFilter *>(ptr);
  const jsize count = width * height;

  // Copy input pixels into the output array
  jintArray result = env->NewIntArray(count);
  if (result == nullptr) {
    LOGE("Failed to allocate output array (%dx%d)", width, height);
    return nullptr;
  }

  jint *srcData = env->GetIntArrayElements(pixels, nullptr);
  env->SetIntArrayRegion(result, 0, count, srcData);
  env->ReleaseIntArrayElements(pixels, srcData, JNI_ABORT);

  // Apply the selected filter in-place on the output array
  jint *dstData = env->GetIntArrayElements(result, nullptr);
  filter->apply(reinterpret_cast<int32_t *>(dstData),
                static_cast<int32_t>(width), static_cast<int32_t>(height),
                static_cast<image_filter::FilterType>(filterType));
  env->ReleaseIntArrayElements(result, dstData, 0);

  return result;
}

} // extern "C"
