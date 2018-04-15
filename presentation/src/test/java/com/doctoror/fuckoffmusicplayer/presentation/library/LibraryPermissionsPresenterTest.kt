/*
 * Copyright (C) 2018 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.fuckoffmusicplayer.presentation.library

import com.doctoror.commons.reactivex.SchedulersProvider
import com.doctoror.fuckoffmusicplayer.reactivex.TestSchedulersProvider
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPermissionsPresenterTest {

    private val libraryPermissionProvider: LibraryPermissionsProvider = mock()

    private val underTest = Impl(
            libraryPermissionProvider,
            TestSchedulersProvider())

    private fun givenPermissionDenied() {
        whenever(libraryPermissionProvider.permissionsGranted())
                .thenReturn(false)

        whenever(libraryPermissionProvider.requestPermission())
                .thenReturn(Observable.just(false))
    }

    private fun givenPermissionGranted() {
        whenever(libraryPermissionProvider.permissionsGranted())
                .thenReturn(true)
    }

    private fun givenPermissionDeniedButGrantedOnRequest() {
        whenever(libraryPermissionProvider.permissionsGranted())
                .thenReturn(false)

        whenever(libraryPermissionProvider.requestPermission())
                .thenReturn(Observable.just(true))
    }

    @Test
    fun requestsPermissionOnStart() {
        // Given
        givenPermissionDenied()

        // When
        underTest.onStart()

        // Then
        verify(libraryPermissionProvider).requestPermission()
    }

    @Test
    fun deliversPermissionDenied() {
        // Given
        givenPermissionDenied()

        // When
        underTest.onStart()

        // Then
        assertEquals(1, underTest.permissionDeniedInvocationCount)
        assertEquals(0, underTest.permissionGrantedInvocationCount)
    }

    @Test
    fun deliversPermissionDeniedEveryTime() {
        // Given
        givenPermissionDenied()

        // When
        underTest.onStart()
        underTest.onStart()

        // Then
        assertEquals(2, underTest.permissionDeniedInvocationCount)
    }

    @Test
    fun deliversPermissionGranted() {
        // Given
        givenPermissionGranted()

        // When
        underTest.onStart()

        // Then
        assertEquals(0, underTest.permissionDeniedInvocationCount)
        assertEquals(1, underTest.permissionGrantedInvocationCount)
    }

    @Test
    fun deliversPermissionGrantedOnRequest() {
        // Given
        givenPermissionDeniedButGrantedOnRequest()

        // When
        underTest.onStart()

        // Then
        assertEquals(0, underTest.permissionDeniedInvocationCount)
        assertEquals(1, underTest.permissionGrantedInvocationCount)
    }

    @Test
    fun requestsPermissionOnlyOnce() {
        // Given
        givenPermissionDenied()

        // When
        underTest.onStart()
        underTest.onStart()

        // Then
        verify(libraryPermissionProvider, times(1)).requestPermission()
        assertEquals(2, underTest.permissionDeniedInvocationCount)
        assertEquals(0, underTest.permissionGrantedInvocationCount)
    }

    internal class Impl(
            libraryPermissionProvider: LibraryPermissionsProvider,
            schedulersProvider: SchedulersProvider)
        : LibraryPermissionsPresenter(libraryPermissionProvider, schedulersProvider) {

        var permissionDeniedInvocationCount = 0
        var permissionGrantedInvocationCount = 0

        override fun onPermissionDenied() {
            permissionDeniedInvocationCount++
        }

        override fun onPermissionGranted() {
            permissionGrantedInvocationCount++
        }
    }
}
