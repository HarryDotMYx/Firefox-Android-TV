package org.mozilla.tv.firefox.ext

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule

class LiveDataDoOnEachTest {

    @get:Rule
    val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var liveData: MutableLiveData<Int>
    private var uninitializedValue: Int? = null

    @Before
    fun setup() {
        liveData = MutableLiveData()
    }

    @Test
    fun `side effects should be executed`() {
        var callCount = 0

        liveData.doOnEach { uninitializedValue = it }
            .observeForever(Observer { callCount++ })

        liveData.value = 1
        assertNotNull(uninitializedValue)
        assertEquals(1, callCount)
    }

    @Test
    fun `passed value should not be changed`() {
        var callCount = 0

        liveData.doOnEach { uninitializedValue = it!! * 5 }
            .observeForever(Observer {
                callCount++
                assertEquals(1, it)
            })

        liveData.value = 1
        assertEquals(1, callCount)
    }
}
