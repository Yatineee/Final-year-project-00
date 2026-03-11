package com.qian.scrollsanity.domain.policy

class FocusModePolicy {
    /**
     * Block ALL tracked apps during focus mode when strict mode is enabled
     * Note: This is only called for apps that are already confirmed to be tracked
     */
    fun shouldBlockDuringFocus(
        strictMode: Boolean,
        isFocusActive: Boolean
    ): Boolean {
        // When focus mode is active and strict mode is on, block all tracked apps
        return strictMode && isFocusActive
    }
}
