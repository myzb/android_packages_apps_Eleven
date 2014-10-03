/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.cyngn.eleven.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.cyngn.eleven.Config;
import com.cyngn.eleven.R;
import com.cyngn.eleven.ui.fragments.AlbumDetailFragment;
import com.cyngn.eleven.ui.fragments.ArtistDetailFragment;
import com.cyngn.eleven.ui.fragments.ISetupActionBar;
import com.cyngn.eleven.ui.fragments.PlaylistDetailFragment;
import com.cyngn.eleven.ui.fragments.RecentFragment;
import com.cyngn.eleven.ui.fragments.phone.MusicBrowserPhoneFragment;
import com.cyngn.eleven.ui.fragments.profile.LastAddedFragment;
import com.cyngn.eleven.ui.fragments.profile.TopTracksFragment;

public class HomeActivity extends SlidingPanelActivity {
    private static final String ACTION_PREFIX = HomeActivity.class.getName();
    public static final String ACTION_VIEW_BROWSE = ACTION_PREFIX + ".view.Browse";
    public static final String ACTION_VIEW_MUSIC_PLAYER = ACTION_PREFIX + ".view.MusicPlayer";
    public static final String ACTION_VIEW_QUEUE = ACTION_PREFIX + ".view.Queue";
    public static final String ACTION_VIEW_ARTIST_DETAILS = ACTION_PREFIX + ".view.ArtistDetails";
    public static final String ACTION_VIEW_ALBUM_DETAILS = ACTION_PREFIX + ".view.AlbumDetails";
    public static final String ACTION_VIEW_PLAYLIST_DETAILS = ACTION_PREFIX + ".view.PlaylistDetails";
    public static final String ACTION_VIEW_SMART_PLAYLIST = ACTION_PREFIX + ".view.SmartPlaylist";

    private boolean mLoadedBaseFragment = false;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // if we've been launched by an intent, parse it
        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            parseIntent(launchIntent);
        }

        // if the intent didn't cause us to load a fragment, load the music browse one
        if (!mLoadedBaseFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_base_content, new MusicBrowserPhoneFragment()).commit();

            mLoadedBaseFragment = true;
        }

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment topFragment = getTopFragment();
                if (topFragment != null) {
                    ISetupActionBar setupActionBar = (ISetupActionBar) topFragment;
                    setupActionBar.setupActionBar();

                    if (topFragment instanceof MusicBrowserPhoneFragment) {
                        getActionBar().setIcon(R.drawable.ic_launcher);
                        getActionBar().setHomeButtonEnabled(false);
                    } else {
                        getActionBar().setIcon(R.drawable.ic_action_back);
                        getActionBar().setHomeButtonEnabled(true);
                    }
                }
            }
        });
    }

    public Fragment getTopFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.activity_base_content);
    }

    public void postRemoveFragment(final Fragment frag) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                getSupportFragmentManager().beginTransaction().remove(frag).commit();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        parseIntent(intent);
    }

    private void parseIntent(Intent intent) {
        if (intent.getAction() != null) {
            final String action = intent.getAction();
            Fragment targetFragment = null;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if (action.equals(ACTION_VIEW_SMART_PLAYLIST)) {
                int playlistType = intent.getExtras().getInt(Config.SMART_PLAYLIST_TYPE);
                switch (Config.SmartPlaylistType.values()[playlistType]) {
                    case LastAdded:
                        targetFragment = new LastAddedFragment();
                        break;
                    case RecentlyPlayed:
                        targetFragment = new RecentFragment();
                        break;
                    case TopTracks:
                        targetFragment = new TopTracksFragment();
                        break;
                }
            } else if (action.equals(ACTION_VIEW_PLAYLIST_DETAILS)) {
                targetFragment = new PlaylistDetailFragment();
            } else if (action.equals(ACTION_VIEW_ALBUM_DETAILS)) {
                targetFragment = new AlbumDetailFragment();
            } else if (action.equals(ACTION_VIEW_ARTIST_DETAILS)) {
                targetFragment = new ArtistDetailFragment();
            }

            if (targetFragment != null) {
                targetFragment.setArguments(intent.getExtras());
                transaction.setCustomAnimations(0, 0, 0, R.anim.fade_out);
                // If we ever come back to this because of memory concerns because
                // none of the fragments are being removed from memory, we can fix this
                // by using "replace" instead of "add".  The caveat is that the performance of
                // returning to previous fragments is a bit more sluggish because the fragment
                // view needs to be recreated. If we do remove that, we can remove the back stack
                // change listener code above
                transaction.add(R.id.activity_base_content, targetFragment);
                if (mLoadedBaseFragment) {
                    transaction.addToBackStack(null);
                    showPanel(Panel.Browse);
                } else {
                    // else mark the fragment as loaded so we don't load the music browse fragment.
                    // this happens when they launch search which is its own activity and then
                    // browse through that back to home activity
                    mLoadedBaseFragment = true;
                    getActionBar().setIcon(R.drawable.ic_action_back);
                    getActionBar().setHomeButtonEnabled(true);
                }
                transaction.commit();
            }
        }
    }
}