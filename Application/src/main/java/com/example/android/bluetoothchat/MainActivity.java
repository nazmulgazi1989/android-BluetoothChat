/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.example.android.bluetoothchat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewAnimator;
import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;
import com.example.android.common.logger.Person;
import com.example.android.util.ZipArchive;
import com.example.android.util.ZipManager;
import com.google.gson.Gson;
import com.snatik.storage.EncryptConfiguration;
import com.snatik.storage.Storage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "MainActivity";

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;

    private class MyHandlerThread extends HandlerThread {

        Handler handler;

        public MyHandlerThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    // process incoming messages here
                    // this will run in non-ui/background thread
                }
            };
        }
        
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Storage storage = new Storage(this);
        Storage storageEncrypted = new Storage(this);
//         set encryption
        String IVX = "abcdefghijklmnop"; // 16 lenght - not secret
        String SECRET_KEY = "secret1234567890"; // 16 lenght - secret
        byte[] SALT = "0000111100001111".getBytes(); // random 16 bytes array

// build configuratio
        EncryptConfiguration configuration = new EncryptConfiguration.Builder()
                .setEncryptContent(IVX, SECRET_KEY, SALT)
                .build();

// configure the simple storage
        storageEncrypted.setEncryptConfiguration(configuration);
        final String pathProject = Environment.getExternalStorageDirectory() + File.separator + ".privateFolder";
        if(!storage.isFileExist(pathProject))
         storageEncrypted.createDirectory(pathProject);
        if (getIntent() != null && getIntent().getAction().equalsIgnoreCase("android.intent.action.VIEW")) {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
            List<File> files = storage.getFiles(path, null);
            for (File file : files) {
                if (file.getName().endsWith(".txt.xyz")) {
                    if (storageEncrypted.isFileExist(pathProject + File.separator + file.getName().split("\\.")[0] + ".txt")) {
                        storageEncrypted.deleteFile(pathProject + File.separator + file.getName().split("\\.")[0] + ".txt");
                    }
                    String textFile = storage.readTextFile(file.getAbsolutePath());
                    storageEncrypted.createFile(pathProject + File.separator + file.getName().split("\\.")[0] + ".txt", textFile);

                } else if (file.getName().endsWith(".pdf.xyz")) {
                    if (storageEncrypted.isFileExist(pathProject + File.separator + file.getName().split("\\.")[0] + ".txt")) {
                        storageEncrypted.deleteFile(pathProject + File.separator + file.getName().split("\\.")[0] + ".txt");
                    }
                    String textFile = storage.readTextFile(file.getAbsolutePath());
                    storageEncrypted.createFile(pathProject + File.separator + file.getName().split("\\.")[0] + ".pdf", textFile);

                }
            }
//            RxFile.createFileFromUri(this, getIntent().getData().normalizeScheme())
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Subscriber<File>() {
//                        @Override
//                        public void onCompleted() {
//                            Log.e("Umar", "onCompleted() for File called");
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            Log.e("Umar,", "Error on file fetching:" + e.getMessage());
//                        }
//
//                        @Override
//                        public void onNext(File file) {
//                            String fileName = file.getName();
//                            String extension = file.getAbsolutePath();
//
//                        }
//                    });
//            // File file = new File(Constant.getRealPathFromURI_API19(MainActivity.this,getIntent().getData()));
        }

        // making json to file
//        List<Person> personList = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            Person person = new Person();
//            person.setId(UUID.randomUUID().toString());
//            person.setDOB("10.09.3400");
//            person.setEduLevel("School");
//            person.setEthnicity(UUID.randomUUID().toString());
//            person.setFname("fsadfas" + i);
//            person.setSurname("afsadf" + i);
//            personList.add(person);
//        }
//        if (storageEncrypted.isFileExist(pathProject + File.separator + "profile.txt")) {
//            storageEncrypted.deleteFile(pathProject + File.separator + "profile.txt");
//        }
//        storageEncrypted.createFile(pathProject + File.separator + "profile.txt", new Gson().toJson(personList).getBytes());

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothChatFragment fragment = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
        //making zip file
        List<File> files = storage.getFiles(pathProject + File.separator + "test", null);

//        int size = files.size();
//        final String [] filesPaths = new String[files.size()];
//
//        for(int i = 0; i< size ; i++){
//            filesPaths[i] = files.get(i).getAbsolutePath();
//        }

        new AsyncTask<Void, Void, Void>() {
            @SuppressLint("WrongThread")
            @Override
            protected Void doInBackground(Void... voids) {
                ZipArchive.unzip(pathProject + File.separator + "test3.zip",pathProject + File.separator + "test2","abcd");
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                //making emil send
                String filename = "contacts_sid.vcf";
                File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);

                Uri path = Uri.fromFile(filelocation);
                List<File> filesTarget = storage.getFiles(pathProject, null);
//                for(File file : filesTarget){
//                    storage.rename(file.getAbsolutePath(),file.getAbsolutePath()+".xyz");
//                }
                filesTarget = storage.getFiles(pathProject, null);
                ArrayList<Uri> uris = new ArrayList<Uri>();
//        uris.add(Uri.parse(pathProject + File.separator + "test1.zip.xyz"));
                for (File file : filesTarget) {
                    uris.add(Uri.fromFile(file));
                }
                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
// set the type to 'email'
                emailIntent.setType("vnd.android.cursor.dir/email");
                String to[] = {"asd@gmail.com"};
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
// the attachment
                emailIntent.putExtra(Intent.EXTRA_STREAM, path);
// the mail subject
                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        }.execute();


        //making upzip

//        try {
//            ZipManager.unzip(pathProject + File.separator + "test1.zip.xyz",pathProject + File.separator + "test2");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


//        //Zip using library
//
//        ZipArchive zipArchive = new ZipArchive();
//        zipArchive.zip(targetPath,destinationPath,password);


    }

    private String getRealPathFromURI(Uri contentURI) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            filePath = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            filePath = cursor.getString(idx);
            cursor.close();
        }
        return filePath;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Create a chain of targets that will receive log data
     */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }
}
