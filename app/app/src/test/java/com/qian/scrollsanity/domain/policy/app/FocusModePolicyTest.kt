//package com.qian.scrollsanity.domain.policy.app
//
//import org.junit.Assert.assertFalse
//import org.junit.Assert.assertTrue
//import org.junit.Before
//import org.junit.Test
//
//class FocusModePolicyTest {
//
//    private lateinit var policy: FocusModePolicy
//
//    @Before
//    fun setup() {
//        policy = FocusModePolicy()
//    }
//
//    @Test
//    fun shouldBlockDuringFocus_returns_true_when_strict_mode_is_on_and_focus_is_active() {
//        val result = policy.shouldBlockDuringFocus(
//            strictMode = true,
//            isFocusActive = true
//        )
//
//        assertTrue(result)
//    }
//
//    @Test
//    fun shouldBlockDuringFocus_returns_false_when_strict_mode_is_on_but_focus_is_not_active() {
//        val result = policy.shouldBlockDuringFocus(
//            strictMode = true,
//            isFocusActive = false
//        )
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun shouldBlockDuringFocus_returns_false_when_strict_mode_is_off_but_focus_is_active() {
//        val result = policy.shouldBlockDuringFocus(
//            strictMode = false,
//            isFocusActive = true
//        )
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun shouldBlockDuringFocus_returns_false_when_strict_mode_is_off_and_focus_is_not_active() {
//        val result = policy.shouldBlockDuringFocus(
//            strictMode = false,
//            isFocusActive = false
//        )
//
//        assertFalse(result)
//    }
//}