/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import org.junit.Assert.fail

fun <T> LiveData<T>.assertThat(vararg predicates: (T) -> Boolean, pushValues: () -> Unit) {
    val actualValues = collectEmissions(this, pushValues)

    if (actualValues.size > predicates.size) fail("LiveData emitted more values than expected\nExpected: ${predicates.size}\nActual  : $actualValues")
    if (actualValues.size < predicates.size) fail("LiveData emitted fewer values than expected\nExpected: ${predicates.size}\nActual  : $actualValues")

    predicates.zip(actualValues).forEachIndexed { i, (predicate, actual) ->
        if (!predicate.invoke(actual)) fail("Value emitted at index $i does satisfy predicate.\nExpected: true\nActual: false")
    }

}

fun <T> LiveData<T>.assertValues(vararg expectedRaw: T, pushValues: () -> Unit) {
    // Arrays do not print prettily, so convert them to a list
    val expectedValues = List(expectedRaw.size) { expectedRaw[it] }

    val actualValues = collectEmissions(this, pushValues)

    if (actualValues.size > expectedValues.size) fail("LiveData emitted more values than expected\nExpected: $expectedValues\nActual  : $actualValues")
    if (actualValues.size < expectedValues.size) fail("LiveData emitted fewer values than expected\nExpected: $expectedValues\nActual  : $actualValues")

    expectedValues.zip(actualValues).forEachIndexed { i, (expect, actual) ->
        if (expect != actual) fail("Values emitted at index $i do not match\nExpected: $expectedValues\nActual  : $actualValues")
    }

}

private fun <T> collectEmissions(liveData: LiveData<T>, pushValues: () -> Unit): List<T> {
    val actualValues = mutableListOf<T>()

    val observer = Observer<T> {
        it ?: return@Observer
        actualValues += it
    }

    liveData.observeForever(observer)
    pushValues.invoke()
    liveData.removeObserver(observer)
    return actualValues
}

fun <T> MutableLiveData<T>.assertValuesWithReceiver(vararg expectedRaw: T, pushValues: MutableLiveData<T>.() -> Unit) {
    this.assertValues(*expectedRaw) { this.pushValues() }
}
