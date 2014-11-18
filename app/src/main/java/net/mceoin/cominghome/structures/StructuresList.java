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

package net.mceoin.cominghome.structures;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import net.mceoin.cominghome.R;

/**
 * Displays the structures of log entries. Will display notes from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link StructuresProvider}
 */
public class StructuresList extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = StructuresList.class.getSimpleName();

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[]{
            StructuresValues.Structures._ID, // 0
            StructuresValues.Structures.STRUCTURE_ID, // 1
            StructuresValues.Structures.NAME, // 2
            StructuresValues.Structures.AWAY, // 3
    };
    // The loader's unique id. Loader ids are specific to the Activity or
    // Fragment in which they reside.
    private static final int LOADER_ID = 1;
    // This is the Adapter being used to display the list's data.
    SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(StructuresValues.Structures.CONTENT_URI);
        }

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.structureslist_item, null,
                new String[]{StructuresValues.Structures.NAME, StructuresValues.Structures.AWAY},
                new int[]{R.id.structure_name, R.id.structure_away}, 0);

        setListAdapter(mAdapter);

        LoaderManager.LoaderCallbacks<Cursor> mCallbacks = this;

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, mCallbacks);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri data = getIntent().getData();
        if (data == null) {
            return;
        }
        Uri uri = ContentUris.withAppendedId(data, id);

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Uri baseUri = StructuresValues.Structures.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, baseUri,
                PROJECTION, null, null,
                StructuresValues.Structures.CREATED_DATE + " COLLATE LOCALIZED DESC");
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
