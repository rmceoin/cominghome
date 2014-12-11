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

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import net.mceoin.cominghome.R;

/**
 * Displays the structures from Nest.
 */
public class StructuresListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

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

    OnStructureSelectedListener mCallback;

    // Container Activity must implement this interface

    /**
     * Called by {@link net.mceoin.cominghome.structures.StructuresListFragment} after
     * a user has selected a structure.
     */
    public interface OnStructureSelectedListener {
        public void onStructureSelected(long id);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.structureslist_item, null,
                new String[]{StructuresValues.Structures.NAME, StructuresValues.Structures.AWAY},
                new int[]{R.id.structure_name, R.id.structure_away}, 0);

        setListAdapter(mAdapter);

        LoaderManager.LoaderCallbacks<Cursor> mCallbacks = this;

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, mCallbacks);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnStructureSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnStructureSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Send the event to the host activity
        mCallback.onStructureSelected(id);


    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Uri baseUri = StructuresValues.Structures.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri,
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
