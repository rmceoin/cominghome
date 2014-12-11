/*
 * Copyright 2014 Randy McEoin
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
package net.mceoin.cominghome;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import net.mceoin.cominghome.structures.StructuresListFragment;

public class StructuresActivity extends ActionBarActivity implements StructuresListFragment.OnStructureSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.structureslist);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationIcon(R.drawable.home);
        }

        getFragmentManager().beginTransaction().replace(R.id.list,
                new StructuresListFragment()).commit();
    }

    /**
     * Handle when a structure was selected in {@link net.mceoin.cominghome.structures.StructuresListFragment}
     *
     * @param id Id of structure
     */
    public void onStructureSelected(long id) {
        // The user selected a structure from the StructuresListFragment

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
}