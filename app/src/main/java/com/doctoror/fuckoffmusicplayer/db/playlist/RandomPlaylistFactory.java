package com.doctoror.fuckoffmusicplayer.db.playlist;

import com.doctoror.fuckoffmusicplayer.playlist.Media;

import java.util.List;

/**
 * Created by Yaroslav Mytkalyk on 06.01.17.
 */

public interface RandomPlaylistFactory {

    List<Media> randomPlaylist();

}
