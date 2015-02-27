/*
 * 
 * Copyright (C) 2014 Randy McEoin
 * 
 * Original version came from Notepad sample.
 * 
 * Copyright (C) 2007 The Android Open Source Project
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

package net.mceoin.cominghome.history;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import net.mceoin.cominghome.R;

/**
 * Displays the history of log entries. Will display notes from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link HistoryProvider}
 */
public class HistoryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

//    private static final String TAG = HistoryListFragment.class.getSimpleName();

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[]{
            HistoryValues.History._ID, // 0
            HistoryValues.History.ENTRY, // 1
            HistoryValues.History.BITS, // 2
            HistoryValues.History.CREATED_DATE, // 3
            HistoryValues.History.CREATED_DATE_STR, // 4
    };

    // This is the Adapter being used to display the list's data.
    SimpleCursorAdapter mAdapter;

    // The loader's unique id. Loader ids are specific to the Activity or
    // Fragment in which they reside.
    private static final int LOADER_ID = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.historylist_item, null,
                new String[]{HistoryValues.History.ENTRY, HistoryValues.History.CREATED_DATE_STR},
                new int[]{R.id.history_entry, R.id.history_date}, 0);

        setListAdapter(mAdapter);

        LoaderManager.LoaderCallbacks<Cursor> mCallbacks = this;

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, mCallbacks);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Uri baseUri = HistoryValues.History.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri,
                PROJECTION, null, null,
                HistoryValues.History.CREATED_DATE + " COLLATE LOCALIZED DESC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        switch (loader.getId()) {
            case LOADER_ID:
                // The asynchronous load is complete and the data
                // is now available for use. Only now can we associate
                // the queried Cursor with the SimpleCursorAdapter.
                mAdapter.swapCursor(cursor);
                break;
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
}
