/*
 * Copyright (C) 2016 Yaroslav Mytkalyk
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
package com.doctoror.fuckoffmusicplayer.playlist;

import com.doctoror.commons.util.ProtoUtils;
import com.doctoror.fuckoffmusicplayer.playlist.nano.RecentPlaylists;
import com.doctoror.fuckoffmusicplayer.util.CollectionUtils;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.Collection;
import java.util.Queue;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Used for storing recent playlists
 */
public final class RecentPlaylistsManagerImpl implements RecentPlaylistsManager {

    private static final int MAX_LENGTH = 10;

    private static final String FILE_NAME_RECENT_PLAYLISTS = "recent_playlists_albums";


    // Is not a leak since it's an application context
    @SuppressLint("StaticFieldLeak")
    private static volatile RecentPlaylistsManagerImpl sInstance;

    @NonNull
    public static RecentPlaylistsManagerImpl getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            synchronized (RecentPlaylistsManagerImpl.class) {
                if (sInstance == null) {
                    sInstance = new RecentPlaylistsManagerImpl(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    @NonNull
    private final Object mLock = new Object();

    @NonNull
    private final Object mLockIO = new Object();

    @NonNull
    private final Context mContext;

    @NonNull
    private final Queue<Long> mRecentAlbums = new CircularFifoQueue<>(MAX_LENGTH);

    private RecentPlaylistsManagerImpl(@NonNull final Context context) {
        mContext = context;
        read();
    }

    private void read() {
        final RecentPlaylists.Albums albums = ProtoUtils
                .readFromFile(mContext, FILE_NAME_RECENT_PLAYLISTS, new RecentPlaylists.Albums());
        if (albums != null) {
            final long[] ids = albums.ids;
            if (ids != null) {
                synchronized (mLock) {
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0; i < ids.length; i++) {
                        mRecentAlbums.add(ids[i]);
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized (mLock) {
            mRecentAlbums.clear();
        }
        persist();
    }

    @Override
    public void storeAlbum(final long albumId) {
        if (albumId > 0) {
            storeAlbumInternal(albumId, true);
        }
    }

    @Override
    public void storeAlbums(@NonNull final Collection<Long> albumIds) {
        boolean result = false;
        for (final Long albumId : albumIds) {
            if (albumId > 0) {
                result |= storeAlbumInternal(albumId, false);
            }
        }
        if (result) {
            persist();
        }
    }

    // For testing
    void storeAlbumsSync(@NonNull final Collection<Long> albumIds) {
        storeAlbums(albumIds);
        persistBlocking();
    }

    private boolean storeAlbumInternal(final long albumId, boolean persist) {
        boolean result = false;
        synchronized (mLock) {
            // If already contains
            final long[] array = CollectionUtils.toLongArray(mRecentAlbums);
            if (array.length == 0 || array[array.length - 1] != albumId) {
                final Long albumIdLong = albumId;
                if (mRecentAlbums.contains(albumIdLong)) {
                    // Remove duplicate
                    if (mRecentAlbums.remove(albumIdLong)) {
                        // Add to head
                        result = mRecentAlbums.add(albumIdLong);
                    }
                } else {
                    // Does not contain. Add.
                    result = mRecentAlbums.add(albumIdLong);
                }
            }
        }
        if (persist && result) {
            persist();
        }
        return result;
    }

    @NonNull
    @Override
    public long[] getRecentAlbums() {
        synchronized (mLock) {
            return CollectionUtils.toReverseLongArray(mRecentAlbums);
        }
    }

    private void persist() {
        Observable.create(s -> persistBlocking()).subscribeOn(Schedulers.io()).subscribe();
    }

    @WorkerThread
    private void persistBlocking() {
        final RecentPlaylists.Albums albums = new RecentPlaylists.Albums();
        synchronized (mLock) {
            albums.ids = CollectionUtils.toLongArray(mRecentAlbums);
        }
        synchronized (mLockIO) {
            ProtoUtils.writeToFile(mContext, FILE_NAME_RECENT_PLAYLISTS, albums);
        }
    }
}