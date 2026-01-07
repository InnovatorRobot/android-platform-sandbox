#include "engine.h"
#include <stdexcept>

namespace core_engine {

Engine::Engine() : initialized_(false) {
  stateMachine_ = std::make_unique<PlaybackStateMachine>();
}

Engine::~Engine() { shutdown(); }

bool Engine::initialize() {
  if (initialized_) {
    return true;
  }

  initialized_ = true;
  return true;
}

void Engine::shutdown() {
  if (initialized_) {
    stop();
    initialized_ = false;
  }
}

bool Engine::loadTrack(const std::string &trackId) {
  if (!initialized_) {
    return false;
  }

  currentTrackId_ = trackId;
  return stateMachine_->buffer();
}

bool Engine::play() {
  if (!initialized_) {
    return false;
  }
  return stateMachine_->play();
}

bool Engine::pause() {
  if (!initialized_) {
    return false;
  }
  return stateMachine_->pause();
}

bool Engine::stop() {
  if (!initialized_) {
    return false;
  }
  return stateMachine_->stop();
}

PlaybackState Engine::getState() const {
  if (!initialized_) {
    return PlaybackState::Error;
  }
  return stateMachine_->getCurrentState();
}

std::string Engine::getCurrentTrackId() const { return currentTrackId_; }

} // namespace core_engine
