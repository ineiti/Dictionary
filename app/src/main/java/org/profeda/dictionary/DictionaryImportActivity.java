/**
 *  This file is part of DICTiCC.
 *
 *  DICTiCC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DICTiCC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with DICTiCC.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright (c) 2011 by Max Weller <dictionary@max-weller.org>
 */

package org.profeda.dictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class DictionaryImportActivity extends ListActivity {

    private static final String TAG = "DictionaryImportActivity";

    String[] file_list;
    Stack<String> currentFolder = new Stack<String>();

    ProgressDialog progdialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(DictionaryImportActivity.this);
        setFolder(pref.getString("importDialogCurrentFolder", Environment.getExternalStorageDirectory().getAbsolutePath()));

        fillList();

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor pref = PreferenceManager.getDefaultSharedPreferences(DictionaryImportActivity.this).edit();
        pref.putString("importDialogCurrentFolder", getFolder());
        pref.commit();
    }


    String getFolder() {
        return "/" + TextUtils.join("/", currentFolder.toArray());
    }

    void setFolder(String folder) {
        String[] s = folder.split("/");
        currentFolder.clear();
        for (String d : s) if (!d.equals("")) currentFolder.push(d);
        updateTitleBar();
    }

    void updateTitleBar() {
        setTitle(getString(R.string.import_dictionary) + " - " + getFolder());
    }

    void fillList() {
        try {
            File[] subfiles = new File(getFolder()).listFiles();
            ArrayList<String> tmplist = new ArrayList<String>();
            if (!getFolder().equals("/")) tmplist.add("..");
            for (int i = 0; i < subfiles.length; i++) {
                if (subfiles[i].getName().startsWith(".") == false)
                    tmplist.add((subfiles[i].isDirectory() ? "/" : "") + subfiles[i].getName());
            }

            file_list = tmplist.toArray(new String[0]);
            Arrays.sort(file_list, String.CASE_INSENSITIVE_ORDER);

            setListAdapter(new GenericStringAdapter(DictionaryImportActivity.this, R.layout.listitem_plaintext, R.id.text, getLayoutInflater(), file_list, false));
        } catch (Exception ex) {
            MessageBox.alert(DictionaryImportActivity.this, String.format(getString(R.string.errmes_cant_show_file_list), getFolder(), ex.getMessage()));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dictimport_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.import_all_dictionaries:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        //super.onListItemClick(l, v, position, id);
        Log.i(TAG, "onListItemClick: " + position);
        final String fileName = ((TextView) v.findViewById(R.id.text)).getText().toString();
        Log.i(TAG, "fileName: " + fileName);
        if (fileName.charAt(0) == '/') {
            currentFolder.push(fileName.substring(1));
            fillList();
            updateTitleBar();
        } else if (fileName.equals("..")) {
            currentFolder.pop();
            fillList();
            updateTitleBar();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.question_import_this_file_as_dictionary)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            importDictionary(getFolder() + "/" + fileName);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

        }

    }

    public void importDictionary(String fileSpec) {
        progdialog = ProgressDialog.show(DictionaryImportActivity.this, null, getString(R.string.importing_dictionary_long) +
                "Initializing");

        Log.i(TAG, "going to call DictionaryImporter for fileSpec=" + fileSpec);
        DictionaryImporter imp = new DictionaryImporter(this, progdialog);
        imp.execute(fileSpec);
    }
}
