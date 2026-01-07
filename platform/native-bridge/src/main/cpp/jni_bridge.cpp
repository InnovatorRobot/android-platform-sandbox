#include "jni_bridge.h"
#include "engine.h"
#include "playback_state.h"
#include <android/log.h>

#define LOG_TAG "JniBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JniBridge::JniBridge() : stateChangeListener_(nullptr), jvm_(nullptr) {
  engine_ = std::make_unique<core_engine::Engine>();
}

JniBridge::~JniBridge() { shutdown(); }

bool JniBridge::initialize(JNIEnv *env, jobject javaObject) {
  if (jvm_ != nullptr) {
    return true; // Already initialized
  }

  if (env->GetJavaVM(&jvm_) != JNI_OK) {
    LOGE("Failed to get JavaVM");
    return false;
  }

  return engine_->initialize();
}

void JniBridge::shutdown() {
  if (engine_) {
    engine_->shutdown();
  }
  stateChangeListener_ = nullptr;
  jvm_ = nullptr;
}

bool JniBridge::loadTrack(JNIEnv *env, const char *trackId) {
  if (!engine_) {
    return false;
  }
  return engine_->loadTrack(std::string(trackId));
}

bool JniBridge::play() {
  if (!engine_) {
    return false;
  }
  return engine_->play();
}

bool JniBridge::pause() {
  if (!engine_) {
    return false;
  }
  return engine_->pause();
}

bool JniBridge::stop() {
  if (!engine_) {
    return false;
  }
  return engine_->stop();
}

int JniBridge::getState() {
  if (!engine_) {
    return static_cast<int>(core_engine::PlaybackState::Error);
  }
  return static_cast<int>(engine_->getState());
}

const char *JniBridge::getCurrentTrackId() {
  if (!engine_) {
    return "";
  }
  // Note: In production, you'd need to manage string lifetime properly
  static std::string trackId;
  trackId = engine_->getCurrentTrackId();
  return trackId.c_str();
}

void JniBridge::setStateChangeListener(JNIEnv *env, jobject listener) {
  if (stateChangeListener_ != nullptr) {
    env->DeleteGlobalRef(stateChangeListener_);
  }
  stateChangeListener_ = env->NewGlobalRef(listener);
}

JNIEnv *JniBridge::getJniEnv() {
  if (jvm_ == nullptr) {
    return nullptr;
  }
  JNIEnv *env = nullptr;
  jint result = jvm_->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
  if (result != JNI_OK) {
    LOGE("Failed to get JNI environment");
    return nullptr;
  }
  return env;
}

void JniBridge::notifyStateChange(core_engine::PlaybackState oldState,
                                  core_engine::PlaybackState newState) {
  if (stateChangeListener_ == nullptr) {
    return;
  }

  JNIEnv *env = getJniEnv();
  if (env == nullptr) {
    return;
  }

  // Find the listener class and method
  jclass listenerClass = env->GetObjectClass(stateChangeListener_);
  if (listenerClass == nullptr) {
    return;
  }

  jmethodID methodId =
      env->GetMethodID(listenerClass, "onStateChanged", "(II)V");

  if (methodId != nullptr) {
    env->CallVoidMethod(stateChangeListener_, methodId,
                        static_cast<jint>(oldState),
                        static_cast<jint>(newState));
  }

  env->DeleteLocalRef(listenerClass);
}

// JNI function implementations
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeCreate(JNIEnv *env,
                                                              jobject thiz) {
  auto *bridge = new JniBridge();
  return reinterpret_cast<jlong>(bridge);
}

JNIEXPORT void JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeDestroy(
    JNIEnv *env, jobject thiz, jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  delete bridge;
}

JNIEXPORT jboolean JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeInitialize(
    JNIEnv *env, jobject thiz, jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  return bridge->initialize(env, thiz) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeShutdown(
    JNIEnv *env, jobject thiz, jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  bridge->shutdown();
}

JNIEXPORT jboolean JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeLoadTrack(
    JNIEnv *env, jobject thiz, jlong nativePtr, jstring trackId) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  const char *trackIdStr = env->GetStringUTFChars(trackId, nullptr);
  bool result = bridge->loadTrack(env, trackIdStr);
  env->ReleaseStringUTFChars(trackId, trackIdStr);
  return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativePlay(JNIEnv *env,
                                                            jobject thiz,
                                                            jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  return bridge->play() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativePause(JNIEnv *env,
                                                             jobject thiz,
                                                             jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  return bridge->pause() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeStop(JNIEnv *env,
                                                            jobject thiz,
                                                            jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  return bridge->stop() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_mediaplatform_nativebridge_NativeEngine_nativeGetState(
    JNIEnv *env, jobject thiz, jlong nativePtr) {
  auto *bridge = reinterpret_cast<JniBridge *>(nativePtr);
  return bridge->getState();
}

} // extern "C"
