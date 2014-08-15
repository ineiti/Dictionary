package org.profeda.dictionary;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class DictionaryImporter extends AsyncTask<String, Long, Long[]> {
//public class DictionaryImporter extends ListActivity {

    private static final String TAG = "DictionaryImporter";

    ListView targetList;
    String dictFile;
    String errMes = null, errDetails = "";
    Context callerContext;
    ProgressDialog progdialog;

    public DictionaryImporter(Context c, ProgressDialog p){
        callerContext = c;
        progdialog = p;
    }

    @Override
    protected Long[] doInBackground(String... params) {

        long anfang = android.os.Process.getElapsedCpuTime();
        dictFile = params[0];

        long c = 0, ok = 0;
        BufferedWriter writeInfo = null, writeCurrent = null;

        try {
            BufferedReader read;
            File dictFileInfo = new File(dictFile);

            if (dictFile.substring(dictFile.length() - 4).toLowerCase().equals(".zip")) {
                Log.i(TAG, "using ZipFile");
                ZipFile zip = new ZipFile(dictFileInfo);
                ZipEntry item = zip.entries().nextElement();
                read = new BufferedReader(new InputStreamReader(zip.getInputStream(item)));
            } else {
                InputStream fis;
                try {
                    fis = new FileInputStream(dictFileInfo);
                } catch( FileNotFoundException e ){
                    fis = callerContext.getAssets().open(dictFile);
                }
                read = new BufferedReader(new InputStreamReader(fis));
            }

            String filePrefix = dictFileInfo.getName() + "_";

            Log.v(TAG, "Inserting new File: " + dictFile);

            //FileOutputStream fos1 = openFileOutput(filePrefix+"info",MODE_WORLD_READABLE);
            FileOutputStream fos1 = Dict.openForWrite(callerContext, filePrefix + "info", false);
            writeInfo = new BufferedWriter(new OutputStreamWriter(fos1));

            //values = new ContentValues();

            ArrayList<String[]> result = new ArrayList<String[]>();
            String[] r;
            String line;
            char lastChar0 = 0, ch0, lastChar1 = 0, ch1;
            boolean firstLine = true;
            while ((line = read.readLine()) != null) {
                if (line.length() == 0) continue;
                ch0 = line.charAt(0);
                if (ch0 == '#') {
                    if (line.contains("::")) {
                        errMes = callerContext.getString(R.string.errmes_dictionary_elcombri);
                        break;
                    }
                    if (firstLine && line.length() > 7) {
                        writeInfo.write(line.substring(2, 8) + "\n");
                        firstLine = false;
                    }
                    writeInfo.write(line + "\n");
                    continue;
                }
                int pos0, pos1 = line.indexOf('\t'), pos2 = line.lastIndexOf('\t');
                if (pos1 < 1 || pos2 == pos1) {
                    Log.v(TAG, "skipped invalid Line: >" + line + "<");
                    continue;
                }

                r = new String[8];
                r[Dict.WORD] = line.substring(0, pos1);
                r[Dict.DEF] = line.substring(pos1 + 1, pos2);
                pos1 = r[Dict.WORD].indexOf('{');
                if (pos1 != -1) {
                    pos0 = r[Dict.WORD].lastIndexOf('}');
                    if (pos0 == -1) pos0 = r[Dict.WORD].length() - 1;
                    r[Dict.WORD_GENDER] = r[Dict.WORD].substring(pos1, pos0 + 1); //WORD_GENDER
                    r[Dict.WORD] = r[Dict.WORD].substring(0, pos1) + r[Dict.WORD].substring(pos0 + 1);
                } else {
                    r[Dict.WORD_GENDER] = "";
                }
                pos1 = r[Dict.WORD].indexOf('[');
                if (pos1 != -1) {
                    pos0 = r[Dict.WORD].lastIndexOf(']');
                    if (pos0 == -1) pos0 = r[Dict.WORD].length() - 1;
                    r[Dict.WORD_EXTRA] = r[Dict.WORD].substring(pos1, pos0 + 1); //WORD_EXTRA
                    r[Dict.WORD] = r[Dict.WORD].substring(0, pos1) + r[Dict.WORD].substring(pos0 + 1);
                } else {
                    r[Dict.WORD_EXTRA] = "";
                }

                pos1 = r[Dict.DEF].indexOf('{');
                if (pos1 != -1) {
                    pos0 = r[Dict.DEF].lastIndexOf('}');
                    if (pos0 == -1) pos0 = r[Dict.DEF].length() - 1;
                    r[Dict.DEF_GENDER] = r[Dict.DEF].substring(pos1, pos0 + 1); //DEFINITION_GENDER
                    r[Dict.DEF] = r[Dict.DEF].substring(0, pos1) + r[Dict.DEF].substring(pos0 + 1);
                } else {
                    r[Dict.DEF_GENDER] = "";
                }
                pos1 = r[Dict.DEF].indexOf('[');
                if (pos1 != -1) {
                    pos0 = r[Dict.DEF].lastIndexOf(']');
                    if (pos0 == -1) pos0 = r[Dict.DEF].length() - 1;
                    r[Dict.DEF_EXTRA] = r[Dict.DEF].substring(pos1, pos0 + 1); //DEFINITION_EXTRA
                    r[Dict.DEF] = r[Dict.DEF].substring(0, pos1) + r[Dict.DEF].substring(pos0 + 1);
                } else {
                    r[Dict.DEF_EXTRA] = "";
                }

                r[Dict.WORD] = r[Dict.WORD].trim();
                r[Dict.TYPE] = line.substring(pos2 + 1);
                r[Dict.LCWORD] = Dict.deAccent(r[Dict.WORD].toLowerCase());
                //r[Dict.WORDLEN] = r[Dict.WORD].length();

                if (r[Dict.LCWORD].length() == 0) continue;
                ch0 = r[Dict.LCWORD].charAt(0);
                if (Character.isLetter(ch0)) ch0 = Character.toLowerCase(ch0);
                else ch0 = '$';
                if (r[Dict.LCWORD].length() > 1) {
                    ch1 = r[Dict.LCWORD].charAt(1);
                    if (Character.isLetter(ch1)) ch1 = Character.toLowerCase(ch1);
                    else ch1 = '$';
                } else ch1 = '$';
                if (ch0 != lastChar0 || ch1 != lastChar1) {
                    Log.i(TAG, "new ch0/1: '" + ch0 + ch1 + "'");
                    //if (writeCurrent != null)
                    if (lastChar0 != 0) {
                        writeToFile(lastChar0, lastChar1, filePrefix, result.toArray(new String[0][0]),
                                false);
                        //result = new ArrayList<String[]>();
                        result.clear();
                    }
                    lastChar0 = ch0;
                    lastChar1 = ch1;
                }
                result.add(r);

                c++;

                if ((c % 128) == 0) publishProgress(c);
            }

            if (lastChar0 != 0) {
                writeToFile(lastChar0, lastChar1, filePrefix, result.toArray(new String[0][0]),
                        false);
            }

            read.close();
            if (errMes == null) ok = 1;

        } catch (OutOfMemoryError e) {
            // TODO Auto-generated catch block
            if (errMes == null) errMes = "";
            errMes += "\nOUT OF MEMORY: You can't import this dictionary on this device!\n";
            e.printStackTrace();
            this.errDetails = e.toString() + "\nStack trace:\n" + MessageBox.getStackTrace(e);
            Log.e(TAG, e.toString());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            this.errDetails = e.toString() + "\nStack trace:\n" + MessageBox.getStackTrace(e);
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (writeInfo != null) writeInfo.close();
                if (writeCurrent != null) writeCurrent.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        long ende = android.os.Process.getElapsedCpuTime();
        return new Long[]{c, anfang, ende, ok};
    }

    void writeToFile(char ch0, char ch1, String filePrefix, String[][] ar,
                     Boolean append) throws IOException {
        BufferedWriter writeCurrent = new BufferedWriter(new OutputStreamWriter(
                Dict.openForWrite(callerContext, filePrefix + ch0 + ch1, append)));
        Arrays.sort(ar, new StringArrayByLengthComparator());
        Log.v(TAG, "writeToFile " + ch0 + ch1 + " \t ar.length=" + ar.length);
        for (int i = 0; i < ar.length; i++) {
            for (int j = 0; j <= Dict.TYPE; j++) {
                writeCurrent.write(ar[i][j]);
                writeCurrent.write('\t');
            }
            writeCurrent.write('\n');
        }
        writeCurrent.close();
    }

    class StringArrayByLengthComparator implements Comparator<String[]> {

        @Override
        public int compare(String[] object1, String[] object2) {
            Integer l1 = object1[Dict.LCWORD].length();
            return l1.compareTo(object2[Dict.LCWORD].length());
        }

    }

    @Override
    protected void onProgressUpdate(Long... values) {
        progdialog.setMessage(callerContext.getString(R.string.importing_dictionary_long) + dictFile + "\n" + String.format(callerContext.getString(R.string.count_words_imported), values[0]));
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Long[] result) {
        progdialog.dismiss();
        if (result[3] == 1) {
            new AlertDialog.Builder(callerContext)
                    .setMessage(String.format(callerContext.getString(R.string.message_dictionary_import_successful), dictFile, result[0], (result[2] - result[1])))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            //finish();
                        }
                    })
                    .setNeutralButton(R.string.import_another_dictionary, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        } else {
            Log.e(TAG, errDetails);
            new AlertDialog.Builder(callerContext)
                    .setTitle("Import failed!")
                    .setMessage((errMes == null ? "" : errMes + "\n\n") + String.format(callerContext.getString(R.string.errmes_dictionary_import_failed), result[0], result[2] - result[1]))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
                            if (arg2.getKeyCode() == KeyEvent.KEYCODE_MENU) {
                                if (errMes != null) {
                                    arg0.dismiss();
                                    MessageBox.alert(callerContext, errDetails);
                                }
                            }
                            return false;
                        }
                    })
                    .setPositiveButton(R.string.report_error, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        /* Create the Intent */
                            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                            String s = "";
                            s += "Error in DictionaryImportActivity:";
                            s += "\n\n----------\n\n" + (errMes == null ? "" : errMes);
                            s += "\n\n----------\n\n" + errDetails;
                            s += "\n\n----------\n\n Debug-infos:";
                            s += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
                            s += "\n OS API Level: " + android.os.Build.VERSION.SDK;
                            s += "\n Device: " + android.os.Build.DEVICE;
                            s += "\n Model (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")";
                            s += "\n Heap size: " + Debug.getNativeHeapSize();

						/* Fill it with Data */
                            emailIntent.setType("plain/text");
                            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"otfa-report@profeda.org"});
                            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "[Import Error Report]");
                            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, s);

						/* Send it off to the Activity-Chooser */
                            callerContext.startActivity(Intent.createChooser(emailIntent, "Send report via mail..."));

                        }
                    })
                    .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
        super.onPostExecute(result);
    }

}

