/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.example.kiosk.client

import android.app.Application
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import android.widget.ImageView
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.google.example.kiosk.client.KioskViewModel.Vis
import com.google.kgax.grpc.CallResult
import com.google.kgax.grpc.FutureCall
import com.google.kgax.grpc.ResponseMetadata
import com.google.kgax.grpc.ResponseStream
import com.google.kgax.grpc.ServerStreamingCall
import com.google.type.LatLng
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kiosk.DisplayClient
import kiosk.GetSignIdResponse
import kiosk.Kiosk
import kiosk.ScreenSize
import kiosk.Sign
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.Executor

/**
 * Basic unit tests for the [KioskViewModel].
 */
@RunWith(MockitoJUnitRunner::class)
class KioskViewModelTest {

    @get:Rule
    var rule: TestRule = object : InstantTaskExecutorRule() {
        override fun starting(description: Description?) {
            super.starting(description)
            MainThreadExecutor.delegate = MoreExecutors.directExecutor()
        }

        override fun finished(description: Description?) {
            super.finished(description)
            MainThreadExecutor.delegate = null
        }
    }

    @Mock
    lateinit var app: Application

    @Mock
    lateinit var client: DisplayClient

    lateinit var model: KioskViewModel

    @Before
    fun before() {
        model = KioskViewModel(app, client)
        whenever(app.getString(any())).doReturn("a string!")
    }

    @Test
    fun canRegisterAKiosk() {
        val kiosk = Kiosk {}
        val future = future(kiosk)
        whenever(client.createKiosk(any())).doReturn(future)

        val location = LatLng {
            latitude = 4.2
            longitude = 5.6
        }
        val screenSize = ScreenSize {
            width = 100
            height = 200
        }

        var result: Kiosk? = null
        model.registerKiosk("my kiosk", location, screenSize) { result = it.body }

        assertThat(result).isEqualTo(kiosk)
        assertThat(model.connected.value).isFalse()
        assertThat(model.kiosk.value).isNull()
        assertThat(model.sign.value).isNull()
        assertThat(model.errorMessage.value).isNull()
        assertThat(model.errorStacktrace.value).isNull()

        verify(client).createKiosk(check {
            assertThat(it.name).isEqualTo("my kiosk")
            assertThat(it.location).isEqualTo(location)
            assertThat(it.size).isEqualTo(screenSize)
            assertThat(it.createTime).isNotNull()
        })
    }

    @Test
    fun canFailToRegisterAKiosk() {
        val future = futureError<Kiosk>(RuntimeException())
        whenever(client.createKiosk(any())).doReturn(future)

        model.registerKiosk("a kiosk", LatLng {}, ScreenSize {}) {}

        assertThat(model.connected.value).isFalse()
        assertThat(model.kiosk.value).isNull()
        assertThat(model.sign.value).isNull()
        assertThat(model.errorMessage.value).isNotNull()
        assertThat(model.errorStacktrace.value).isNotNull()
    }

    @Test
    fun canSwitchToKiosk() {
        val kiosk = Kiosk {}
        val sign = Sign { id = 10 }
        val future = future(kiosk)
        whenever(client.getKiosk(any())).doReturn(future)

        val signStream: ServerStreamingCall<GetSignIdResponse> = mock()
        whenever(client.getSignIdsForKioskId(any())).doReturn(signStream)

        val signFuture = future(sign)
        whenever(client.getSign(any())).doReturn(signFuture)

        whenever(signStream.start(any())).thenAnswer {
            val fakeStream = FakeResponseStream<GetSignIdResponse>().apply(
                    responseStreamFromInvocation(it))
            fakeStream.onNext(GetSignIdResponse { signId = 10 })
        }

        model.switchToKiosk(5)

        verify(client).getKiosk(check { assertThat(it.id).isEqualTo(5) })
        verify(client).getSign(check { assertThat(it.id).isEqualTo(10) })

        assertThat(model.connected.value).isTrue()
        assertThat(model.kiosk.value).isEqualTo(kiosk)
        assertThat(model.sign.value).isEqualTo(sign)
        assertThat(model.errorMessage.value).isNull()
        assertThat(model.errorStacktrace.value).isNull()
    }

    @Test
    fun canFailToSwitchToKiosk() {
        val future = futureError<Kiosk>(RuntimeException())
        whenever(client.getKiosk(any())).doReturn(future)

        model.switchToKiosk(15)

        verify(client).getKiosk(check { assertThat(it.id).isEqualTo(15) })

        assertThat(model.connected.value).isFalse()
        assertThat(model.kiosk.value).isNull()
        assertThat(model.sign.value).isNull()
        assertThat(model.errorMessage.value).isNotNull()
        assertThat(model.errorStacktrace.value).isNotNull()
    }

    @Test
    fun canFailToSubscribeToSignChanges() {
        val kiosk = Kiosk {}
        val future = future(kiosk)
        whenever(client.getKiosk(any())).doReturn(future)

        val signStream: ServerStreamingCall<GetSignIdResponse> = mock()
        whenever(client.getSignIdsForKioskId(any())).doReturn(signStream)

        whenever(signStream.start(any())).thenAnswer {
            val fakeStream = FakeResponseStream<GetSignIdResponse>().apply(
                    responseStreamFromInvocation(it))
            fakeStream.onError(RuntimeException())
        }

        model.switchToKiosk(55)

        verify(client).getKiosk(check { assertThat(it.id).isEqualTo(55) })

        assertThat(model.connected.value).isFalse()
        assertThat(model.kiosk.value).isEqualTo(kiosk)
        assertThat(model.sign.value).isNull()
        assertThat(model.errorMessage.value).isNotNull()
        assertThat(model.errorStacktrace.value).isNotNull()
    }

    @Test
    fun canFailToFetchSign() {
        val kiosk = Kiosk {}
        val future = future(kiosk)
        whenever(client.getKiosk(any())).doReturn(future)

        val signStream: ServerStreamingCall<GetSignIdResponse> = mock()
        whenever(client.getSignIdsForKioskId(any())).doReturn(signStream)

        val signFuture = futureError<Sign>(RuntimeException())
        whenever(client.getSign(any())).doReturn(signFuture)

        whenever(signStream.start(any())).thenAnswer {
            val fakeStream = FakeResponseStream<GetSignIdResponse>().apply(
                    responseStreamFromInvocation(it))
            fakeStream.onNext(GetSignIdResponse { signId = 100 })
        }

        model.switchToKiosk(50)

        verify(client).getKiosk(check { assertThat(it.id).isEqualTo(50) })
        verify(client).getSign(check { assertThat(it.id).isEqualTo(100) })

        assertThat(model.connected.value).isFalse()
        assertThat(model.kiosk.value).isEqualTo(kiosk)
        assertThat(model.sign.value).isNull()
        assertThat(model.errorMessage.value).isNotNull()
        assertThat(model.errorStacktrace.value).isNotNull()
    }

    @Test
    fun canToggleScaleType() {
        val values = mutableListOf<ImageView.ScaleType?>()
        do {
            values.add(model.scaleType.value)
            model.toggleScaleType()
        } while (values.size < 3)
        assertThat(values).containsExactly(
                ImageView.ScaleType.CENTER_CROP,
                ImageView.ScaleType.CENTER_INSIDE,
                ImageView.ScaleType.FIT_CENTER
        )
    }

    @Test
    fun visibilityShowProgress() {
        assertThat(Vis.showProgress(true, "error")).isEqualTo(View.GONE)
        assertThat(Vis.showProgress(true, null)).isEqualTo(View.GONE)
        assertThat(Vis.showProgress(false, "error")).isEqualTo(View.GONE)
        assertThat(Vis.showProgress(false, null)).isEqualTo(View.VISIBLE)
        assertThat(Vis.showProgress(null, "error")).isEqualTo(View.GONE)
        assertThat(Vis.showProgress(null, null)).isEqualTo(View.GONE)
    }

    @Test
    fun visibilityShowIfError() {
        assertThat(Vis.showIfError(null)).isEqualTo(View.GONE)
        assertThat(Vis.showIfError("hello")).isEqualTo(View.VISIBLE)
    }

    @Test
    fun visibilityShowSign() {
        assertThat(Vis.showSign(true, null, Sign {})).isEqualTo(View.VISIBLE)
        assertThat(Vis.showSign(true, null, null)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(true, "err", Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showSign(true, "err", null)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(false, null, Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showSign(false, null, null)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(false, "err", Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showSign(false, "err", null)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(true, null, Sign {}, false)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(true, null, Sign {}, true)).isEqualTo(View.VISIBLE)
        assertThat(Vis.showSign(false, "err", Sign {}, isText = true)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(false, "err", Sign { text = "hi" }, isText = true)).isEqualTo(
                View.GONE)
        assertThat(Vis.showSign(null, null, Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showSign(null, null, null)).isEqualTo(View.GONE)
        assertThat(Vis.showSign(null, "err", Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showSign(null, "err", null)).isEqualTo(View.GONE)
    }

    @Test
    fun visibilityShowNoSign() {
        assertThat(Vis.showNoSign(true, null, Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(true, null, null)).isEqualTo(View.VISIBLE)
        assertThat(Vis.showNoSign(true, "err", Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(true, "err", null)).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(false, null, Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(false, null, null)).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(false, "err", Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(false, "err", null)).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(null, null, Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(null, null, null)).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(null, "err", Sign {})).isEqualTo(View.GONE)
        assertThat(Vis.showNoSign(null, "err", null)).isEqualTo(View.GONE)
    }

    @Test
    fun visibilityScaleType() {
        assertThat(Vis.scaleType(null)).isEqualTo(ImageView.ScaleType.CENTER_CROP)
        for (type in ImageView.ScaleType.values()) {
            assertThat(Vis.scaleType(type)).isEqualTo(type)
        }
    }

    private fun <T> future(response: T): FutureCall<T> {
        val future = SettableFuture.create<CallResult<T>>()
        future.set(CallResult(response, ResponseMetadata()))
        return future
    }

    private fun <T> futureError(cause: Throwable): FutureCall<T> {
        val future = SettableFuture.create<CallResult<T>>()
        future.setException(cause)
        return future
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> responseStreamFromInvocation(invocation: InvocationOnMock) =
            invocation.arguments.first() as (ResponseStream<T>.() -> kotlin.Unit)
}

private class FakeResponseStream<T>(
    override var executor: Executor? = null,
    override var ignoreCompletedIf: () -> Boolean = { false },
    override var ignoreErrorIf: (Throwable) -> Boolean = { false },
    override var ignoreIf: () -> Boolean = { false },
    override var ignoreNextIf: (T) -> Boolean = { false },
    override var onCompleted: () -> Unit = {},
    override var onError: (Throwable) -> Unit = {},
    override var onNext: (T) -> Unit = {}
) : ResponseStream<T> {
    override fun close() {}
}