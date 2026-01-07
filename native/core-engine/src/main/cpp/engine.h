#ifndef CORE_ENGINE_ENGINE_H
#define CORE_ENGINE_ENGINE_H

#include "playback_state.h"
#include <memory>
#include <string>

namespace core_engine {

/**
 * Core playback engine.
 *
 * This demonstrates:
 * - Platform-agnostic C++ core logic
 * - Thread-safe operations
 * - Clean separation from platform-specific code
 */
class Engine {
public:
  Engine();
  ~Engine();

  // Engine lifecycle
  bool initialize();
  void shutdown();

  // Playback control
  bool loadTrack(const std::string &trackId);
  bool play();
  bool pause();
  bool stop();

  // State queries
  PlaybackState getState() const;
  std::string getCurrentTrackId() const;

private:
  std::unique_ptr<PlaybackStateMachine> stateMachine_;
  std::string currentTrackId_;
  bool initialized_;
};

} // namespace core_engine

#endif // CORE_ENGINE_ENGINE_H
