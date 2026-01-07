#ifndef CORE_ENGINE_PLAYBACK_STATE_H
#define CORE_ENGINE_PLAYBACK_STATE_H

#include <functional>
#include <mutex>
#include <string>

namespace core_engine {

/**
 * Playback state machine states.
 * This demonstrates platform-agnostic C++ logic that could be shared
 * across Android and iOS (or other platforms).
 */
enum class PlaybackState { Idle, Buffering, Playing, Paused, Error };

/**
 * Thread-safe playback state machine.
 *
 * This shows:
 * - Pure C++ logic (no platform dependencies)
 * - Thread safety
 * - State machine pattern
 * - Callback mechanism for state changes
 */
class PlaybackStateMachine {
public:
  using StateChangeCallback = std::function<void(PlaybackState, PlaybackState)>;

  PlaybackStateMachine();
  ~PlaybackStateMachine() = default;

  // State transitions
  bool transitionTo(PlaybackState newState);
  bool play();
  bool pause();
  bool stop();
  bool buffer();

  // State queries
  PlaybackState getCurrentState() const;
  std::string getStateName() const;

  // Observer pattern
  void setStateChangeCallback(StateChangeCallback callback);

private:
  mutable std::mutex mutex_;
  PlaybackState currentState_;
  StateChangeCallback stateChangeCallback_;

  bool isValidTransition(PlaybackState from, PlaybackState to) const;
  void notifyStateChange(PlaybackState oldState, PlaybackState newState);
};

} // namespace core_engine

#endif // CORE_ENGINE_PLAYBACK_STATE_H
