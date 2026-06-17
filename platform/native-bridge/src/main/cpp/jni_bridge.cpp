#include "audio_processor.h"
#include "image_filter.h"

#include <android/log.h>
#include <jni.h>
#include <vector>

#define LOG_TAG "NativeBridge"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// ── ImageProcessorEngine
// ──────────────────────────────────────────────────────

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

JNIEXPORT void JNICALL
Java_com_mediaplatform_nativebridge_ImageProcessorEngine_nativeApplyFilter(
    JNIEnv *env, jobject /*thiz*/, jlong ptr, jintArray pixels, jint width,
    jint height, jint filterType) {

  auto *filter = reinterpret_cast<image_filter::ImageFilter *>(ptr);

  // Operate directly on the caller's array (mode 0 commits changes back),
  // avoiding a second large allocation and copy every frame.
  jint *data = env->GetIntArrayElements(pixels, nullptr);
  if (data == nullptr) {
    LOGE("Failed to access pixel array (%dx%d)", width, height);
    return;
  }
  filter->apply(reinterpret_cast<int32_t *>(data), static_cast<int32_t>(width),
                static_cast<int32_t>(height),
                static_cast<image_filter::FilterType>(filterType));
  env->ReleaseIntArrayElements(pixels, data, 0);
}

// ── AudioProcessorEngine
// ──────────────────────────────────────────────────────

JNIEXPORT jlong JNICALL
Java_com_mediaplatform_nativebridge_AudioProcessorEngine_nativeCreate(
    JNIEnv * /*env*/, jobject /*thiz*/) {
  return reinterpret_cast<jlong>(new audio_proc::AudioProcessor());
}

JNIEXPORT void JNICALL
Java_com_mediaplatform_nativebridge_AudioProcessorEngine_nativeDestroy(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong ptr) {
  delete reinterpret_cast<audio_proc::AudioProcessor *>(ptr);
}

JNIEXPORT jfloat JNICALL
Java_com_mediaplatform_nativebridge_AudioProcessorEngine_nativeComputeDb(
    JNIEnv *env, jobject /*thiz*/, jlong ptr, jshortArray samples) {
  auto *proc = reinterpret_cast<audio_proc::AudioProcessor *>(ptr);
  const jsize count = env->GetArrayLength(samples);
  jshort *data = env->GetShortArrayElements(samples, nullptr);
  const float db = proc->computeDb(reinterpret_cast<const int16_t *>(data),
                                   static_cast<int32_t>(count));
  env->ReleaseShortArrayElements(samples, data, JNI_ABORT);
  return static_cast<jfloat>(db);
}

JNIEXPORT jint JNICALL
Java_com_mediaplatform_nativebridge_AudioProcessorEngine_nativeFilterForDb(
    JNIEnv * /*env*/, jobject /*thiz*/, jlong ptr, jfloat db) {
  auto *proc = reinterpret_cast<audio_proc::AudioProcessor *>(ptr);
  return static_cast<jint>(proc->filterForDb(static_cast<float>(db)));
}

JNIEXPORT jfloatArray JNICALL
Java_com_mediaplatform_nativebridge_AudioProcessorEngine_nativeComputeBands(
    JNIEnv *env, jobject /*thiz*/, jlong ptr, jshortArray samples,
    jint bandCount) {
  auto *proc = reinterpret_cast<audio_proc::AudioProcessor *>(ptr);
  const jsize count = env->GetArrayLength(samples);
  jshort *data = env->GetShortArrayElements(samples, nullptr);

  const std::vector<float> bands = proc->computeBands(
      reinterpret_cast<const int16_t *>(data), static_cast<int32_t>(count),
      static_cast<int32_t>(bandCount));
  env->ReleaseShortArrayElements(samples, data, JNI_ABORT);

  jfloatArray result = env->NewFloatArray(bandCount);
  if (result != nullptr) {
    env->SetFloatArrayRegion(result, 0, bandCount, bands.data());
  }
  return result;
}

} // extern "C"
