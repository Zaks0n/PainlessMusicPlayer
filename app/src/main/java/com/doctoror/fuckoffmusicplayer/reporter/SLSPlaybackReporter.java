package com.doctoror.fuckoffmusicplayer.reporter;

import com.doctoror.commons.playback.PlaybackState;
import com.doctoror.fuckoffmusicplayer.R;
import com.doctoror.fuckoffmusicplayer.playback.PlaybackService;
import com.doctoror.fuckoffmusicplayer.playlist.CurrentPlaylist;
import com.doctoror.fuckoffmusicplayer.playlist.Media;
import com.doctoror.fuckoffmusicplayer.settings.Settings;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Simple Last.fm Scrobbler playback reporter
 */
final class SLSPlaybackReporter implements PlaybackReporter {

    private static final String ACTION = "com.adam.aslfms.notify.playstatechanged";

    private static final int STATE_START = 0;
    private static final int STATE_RESUME = 1;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_COMPLETE = 3;

    private static final String APP_NAME = "app-name";
    private static final String APP_PACKAGE = "app-package";

    private static final String STATE = "state";
    private static final String ARTIST = "artist";
    private static final String ALBUM = "album";
    private static final String TRACK = "track";
    private static final String DURATION = "duration";

    @NonNull
    private final Context mContext;

    @NonNull
    private final Settings mSettings;

    private Media mMedia;

    @PlaybackState.State
    private int mState;

    SLSPlaybackReporter(@NonNull final Context context) {
        mContext = context;
        mSettings = Settings.getInstance(context);
        mMedia = CurrentPlaylist.getInstance(context).getMedia();
        mState = PlaybackService.getLastKnownState();
    }

    @Override
    public void reportTrackChanged(@NonNull final Media media, final int positionInPlaylist) {
        mMedia = media;
        report(media, mState, mState);
    }

    @Override
    public void reportPlaybackStateChanged(@PlaybackState.State final int state,
            @Nullable final CharSequence errorMessage) {
        report(mMedia, mState, state);
        mState = state;
    }

    private void report(@Nullable final Media media,
            @PlaybackState.State final int prevState,
            @PlaybackState.State final int state) {
        if (mSettings.isScrobbleEnabled() && media != null) {
            final Intent intent = new Intent(ACTION);
            intent.putExtra(APP_NAME, mContext.getString(R.string.app_name));
            intent.putExtra(APP_PACKAGE, mContext.getPackageName());

            intent.putExtra(ARTIST, media.getArtist());
            intent.putExtra(TRACK, media.getTitle());
            intent.putExtra(DURATION, (int) (media.getDuration() / 1000L));
            intent.putExtra(ALBUM, media.getAlbum());

            intent.putExtra(STATE, toSlsState(prevState, state));
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void reportPositionChanged(final long mediaId, final long position) {
        // Not supported
    }

    @Override
    public void reportPlaylistChanged(@Nullable final List<Media> playlist) {
        // Not supported
    }

    private static int toSlsState(@PlaybackState.State final int prevState,
            @PlaybackState.State final int playbackState) {
        switch (playbackState) {
            case PlaybackState.STATE_IDLE:
                return STATE_PAUSE;

            case PlaybackState.STATE_LOADING:
                // Loading after playing means playback completed for prev track
                return prevState == PlaybackState.STATE_PLAYING
                        ? STATE_COMPLETE
                        : STATE_PAUSE;

            case PlaybackState.STATE_PLAYING:
                // Playing after loading means new track is started
                return prevState == PlaybackState.STATE_LOADING
                        ? STATE_START
                        : STATE_RESUME;

            case PlaybackState.STATE_PAUSED:
                return STATE_PAUSE;

            case PlaybackState.STATE_ERROR:
                return STATE_PAUSE;


            default:
                return STATE_PAUSE;
        }
    }
}