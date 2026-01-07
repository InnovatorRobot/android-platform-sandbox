#include "playback_state.h"
#include <stdexcept>

namespace core_engine {

PlaybackStateMachine::PlaybackStateMachine()
    : currentState_(PlaybackState::Idle) {}

bool PlaybackStateMachine::transitionTo(PlaybackState newState) {
  std::lock_guard<std::mutex> lock(mutex_);

  if (!isValidTransition(currentState_, newState)) {
    return false;
  }

  PlaybackState oldState = currentState_;
  currentState_ = newState;
  notifyStateChange(oldState, newState);
  return true;
}

bool PlaybackStateMachine::play() {
  return transitionTo(PlaybackState::Playing);
}

bool PlaybackStateMachine::pause() {
  return transitionTo(PlaybackState::Paused);
}

bool PlaybackStateMachine::stop() { return transitionTo(PlaybackState::Idle); }

bool PlaybackStateMachine::buffer() {
  return transitionTo(PlaybackState::Buffering);
}

PlaybackState PlaybackStateMachine::getCurrentState() const {
  std::lock_guard<std::mutex> lock(mutex_);
  return currentState_;
}

std::string PlaybackStateMachine::getStateName() const {
  std::lock_guard<std::mutex> lock(mutex_);

  switch (currentState_) {
  case PlaybackState::Idle:
    return "Idle";
  case PlaybackState::Buffering:
    return "Buffering";
  case PlaybackState::Playing:
    return "Playing";
  case PlaybackState::Paused:
    return "Paused";
  case PlaybackState::Error:
    return "Error";
  default:
    return "Unknown";
  }
}

void PlaybackStateMachine::setStateChangeCallback(
    StateChangeCallback callback) {
  std::lock_guard<std::mutex> lock(mutex_);
  stateChangeCallback_ = callback;
}

bool PlaybackStateMachine::isValidTransition(PlaybackState from,
                                             PlaybackState to) const {
  // Define valid state transitions
  switch (from) {
  case PlaybackState::Idle:
    return to == PlaybackState::Buffering || to == PlaybackState::Error;

  case PlaybackState::Buffering:
    return to == PlaybackState::Playing || to == PlaybackState::Idle ||
           to == PlaybackState::Error;

  case PlaybackState::Playing:
    return to == PlaybackState::Paused || to == PlaybackState::Idle ||
           to == PlaybackState::Buffering || to == PlaybackState::Error;

  case PlaybackState::Paused:
    return to == PlaybackState::Playing || to == PlaybackState::Idle ||
           to == PlaybackState::Error;

  case PlaybackState::Error:
    return to == PlaybackState::Idle;

  default:
    return false;
  }
}

void PlaybackStateMachine::notifyStateChange(PlaybackState oldState,
                                             PlaybackState newState) {
  if (stateChangeCallback_) {
    stateChangeCallback_(oldState, newState);
  }
}

} // namespace core_engine
