package com.spotify.platform.state

/**
 * Observer interface for state changes.
 * Platform-level state coordination mechanism.
 */
interface StateObserver<T> {
    fun onStateChanged(state: T)
}

/**
 * Simple state holder that notifies observers of changes.
 * This demonstrates platform-level state management.
 */
class StateHolder<T>(initialState: T) {
    private val observers = mutableSetOf<StateObserver<T>>()
    private var _state: T = initialState

    val state: T
        get() = _state

    fun updateState(newState: T) {
        if (_state != newState) {
            _state = newState
            notifyObservers()
        }
    }

    fun observe(observer: StateObserver<T>) {
        observers.add(observer)
        // Immediately notify with current state
        observer.onStateChanged(_state)
    }

    fun removeObserver(observer: StateObserver<T>) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it.onStateChanged(_state) }
    }
}

