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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import net.mceoin.cominghome.history.HistoryListFragment;
import net.mceoin.cominghome.history.HistoryValues;

public class HistoryActivity extends ActionBarActivity {

    public static final int MENU_ITEM_DELETE_ALL = Menu.FIRST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.historylist);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationIcon(R.drawable.home);
        }

        getFragmentManager().beginTransaction().replace(R.id.list,
                new HistoryListFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(Menu.NONE, MENU_ITEM_DELETE_ALL, Menu.NONE, R.string.delete_all)
                .setIcon(android.R.drawable.ic_menu_delete);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE_ALL:
                Dialog about = new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_delete)
                        .setTitle(R.string.delete_all)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getContentResolver().delete(HistoryValues.History.CONTENT_URI, null, null);
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .setMessage(R.string.dialog_delete_history_msg)
                        .create();
                about.show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}