package com.mediaplatform.filters

import com.mediaplatform.services.PlatformService

/**
 * Manages which image filter is currently selected.
 * Isolated feature — no dependency on camera or native code.
 */
class FiltersFeature : PlatformService {

    var currentFilter: FilterType = FilterType.NONE
        private set

    override fun start()  { /* no-op — stateless initialisation */ }
    override fun stop()   { currentFilter = FilterType.NONE }

    fun selectFilter(filter: FilterType) {
        currentFilter = filter
    }
}
