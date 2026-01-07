#ifndef NATIVE_BRIDGE_JNI_BRIDGE_H
#define NATIVE_BRIDGE_JNI_BRIDGE_H

#include <jni.h>
#include <memory>

// Forward declarations
namespace core_engine {
class Engine;
enum class PlaybackState;
} // namespace core_engine

/**
 * JNI bridge for Kotlin ↔ C++ interoperability.
 *
 * This demonstrates:
 * - Clean JNI boundary
 * - Ownership and lifecycle safety
 * - Type-safe conversions between Java/Kotlin and C++
 */
class JniBridge {
public:
  JniBridge();
  ~JniBridge();

  // Engine lifecycle
  bool initialize(JNIEnv *env, jobject javaObject);
  void shutdown();

  // Playback control
  bool loadTrack(JNIEnv *env, const char *trackId);
  bool play();
  bool pause();
  bool stop();

  // State queries
  int getState();
  const char *getCurrentTrackId();

  // Callback setup
  void setStateChangeListener(JNIEnv *env, jobject listener);

private:
  std::unique_ptr<core_engine::Engine> engine_;
  jobject stateChangeListener_;
  JavaVM *jvm_;

  void notifyStateChange(core_engine::PlaybackState oldState,
                         core_engine::PlaybackState newState);
  JNIEnv *getJniEnv();
};

#endif // NATIVE_BRIDGE_JNI_BRIDGE_H
