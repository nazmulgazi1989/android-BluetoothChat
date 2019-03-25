/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.bluetoothchat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.Person;
import com.example.android.util.Constant;
import com.example.android.util.PermissionHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;
    private PermissionHelper permissionHelper;
    private String[] appPermissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
    private String sCameraPhotoPath;
    int size = 0, index = 0, maxSize =0;
    private byte[] byteMessage,byteTargetArray;
    ImageView ivImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        permissionHelper = new PermissionHelper(getActivity(), getActivity());

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        ivImageView = view.findViewById(R.id.ivImageView);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
//                View view = getView();
//                if (null != view) {
//                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
//                    String message = textView.getText().toString();
//                    sendMessage(message);
//                }

//                if (permissionHelper.isPermissionsGranted(appPermissions)) {
//                    sCameraPhotoPath = permissionHelper.browseForImage();
//                }

//                //image sending
//                File  file = new File(
//                        Environment.getExternalStoragePublicDirectory(
//                                Environment.DIRECTORY_PICTURES), "me.jpg");
//
//                // If there is not data, then we may have taken a photo
//                try {
//                    Glide.with(getActivity()).load(readFile(file)).into(ivImageView);
//                    sendMessage(readFile(file));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

     //text sending successfully handled
                List<Person> personList = new ArrayList<>();
                for(int i =0 ; i< 10; i++){
                    Person person = new Person();
                    person.setId(UUID.randomUUID().toString());
                    person.setDOB("10.09.3400");
                    person.setEduLevel("School");
                    person.setEthnicity(UUID.randomUUID().toString());
                    person.setFname("fsadfas" + i);
                    person.setSurname("afsadf" + i);
                    personList.add(person);
                }
                sendMessage(new Gson().toJson(personList).getBytes());


//                InputStream inputStream = null;//You can get an inputStream using any IO API
//                try {
//                    inputStream = new FileInputStream(file.getAbsolutePath());
//                    byte[] buffer = new byte[8192];
//                    int bytesRead;
//                    ByteArrayOutputStream output = new ByteArrayOutputStream();
//
//                    Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
//                    try {
//                        while ((bytesRead = inputStream.read(buffer)) != -1) {
//                            output64.write(buffer, 0, bytesRead);
//                        }
//                        output64.close();
//
//                        sendMessage(output.toString());
//                        Log.d("ttt",output.toString());
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }



        }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);

            return Base64.encode(data,Base64.DEFAULT);
        } finally {
            f.close();
        }
    }
//    public static byte[] compress(String text) throws Exception{
//        byte[] output = new byte;
//        Deflater compresser = new Deflater();
//        compresser.setInput(text.getBytes("UTF-8"));
//        compresser.finish();
//        int compressedDataLength = compresser.deflate(output);
//        byte[] dest = new byte[compressedDataLength];
//        System.arraycopy(output, 0, dest, 0, compressedDataLength);
//        return dest;
//    }
//
//    public static String decompress(byte[] bytes) throws Exception{
//        Inflater decompresser = new Inflater();
//        decompresser.setInput(bytes, 0, bytes.length);
//        byte[] result = new byte[bytes.length *10];
//        int resultLength = decompresser.inflate(result);
//        decompresser.end();
//
//        // Decode the bytes into a String
//        String outputString = new String(result, 0, resultLength, "UTF-8");
//        return outputString;
//    }
@TargetApi(Build.VERSION_CODES.KITKAT)
private static String ungzip(byte[] bytes) throws Exception{
    InputStreamReader isr = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8);
    StringWriter sw = new StringWriter();
    char[] chars = new char[1024];
    for (int len; (len = isr.read(chars)) > 0; ) {
        sw.write(chars, 0, len);
    }
    return sw.toString();
}

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static byte[] gzip(String s) throws Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        OutputStreamWriter osw = new OutputStreamWriter(gzip, StandardCharsets.UTF_8);
        osw.write(s);
        osw.close();
        return bos.toByteArray();
    }
    public static byte[] compress(byte [] bytarray) throws Exception{
        Deflater compresser = new Deflater();
        compresser.setInput(bytarray);
        compresser.finish();
        int compressedDataLength = compresser.deflate(bytarray);
        byte[] dest = new byte[compressedDataLength];
        System.arraycopy(bytarray, 0, dest, 0, compressedDataLength);
        return dest;
    }

    public static String decompress(byte[] bytes) throws Exception{
        Inflater decompresser = new Inflater();
        decompresser.setInput(bytes, 0, bytes.length);
        byte[] result = new byte[bytes.length *10];
        int resultLength = decompresser.inflate(result);
        decompresser.end();

        // Decode the bytes into a String
        String outputString = new String(result, 0, resultLength, "UTF-8");
        return outputString;
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            maxSize = message.length;
            size = message.length/980;
            byteMessage = message;
            byte [] chank = new byte[980];
            System.arraycopy(Constant.longToBytes(maxSize),0,chank,1,Constant.longToBytes(maxSize).length);
            chank[0] = (byte)3;
            mChatService.write(chank);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }


    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    Log.d("Umar",String.valueOf(readBuf.length));
                    if(readBuf[0] == 1){
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        Log.d("Umar",readMessage);
                        if(index <maxSize){
                            byte [] chank = new byte[980];
                            System.arraycopy(byteMessage,index,chank,1,Math.min(maxSize-index,979));
                            chank[0] = (byte)0;
                            mChatService.write(chank);
                            index+=979;
                        }
                    }else if(readBuf[0] == 0){
                        byte [] chank = new byte[980];
                        chank[0] = (byte)1;
                        System.arraycopy("Success".getBytes(),0,chank,1,"Success".getBytes().length);

                        if(index < maxSize){
                         System.arraycopy(readBuf,1,byteTargetArray,index,Math.min(979,maxSize-index));
                         index+=979;
                         mChatService.write(chank);
                        }

                        if(index > maxSize) {
//                            String readMessage = new String(readBuf, 0, msg.arg1);
//                            mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
//                            Log.d("Umar",String.valueOf(byteTargetArray.length) + " inside glide");
//                            mConversationView.setVisibility(View.GONE);
//                            Log.d("Umar",String.valueOf(byteTargetArray.length) + " inside glide");
//                            Glide.with(getActivity()).load(Base64.decode(byteTargetArray,Base64.DEFAULT)).apply(new RequestOptions().override(600, 200)).into(ivImageView);

                            //text successfully handled
                            Gson gson = new Gson();
                            List<Person> people = gson.fromJson(new String(byteTargetArray, 0, byteTargetArray.length), new TypeToken<List<Person>>(){}.getType());
                            for(Person person : people){
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + gson.toJson(person));
                            }
                        }

                                // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    }else if(readBuf[0] == 3){
                        byte[] array = new byte[4];
                        System.arraycopy(readBuf,1,array,0,array.length);
                        Log.d("Umar", Arrays.toString(array));
                        maxSize = Constant.bytesToLong(array);
                        Log.d("Umar",String.valueOf(maxSize));
                        byteTargetArray = new byte[maxSize];
                        byte [] chank = new byte[512];
                        chank[0] = (byte)1;
                        System.arraycopy("Success".getBytes(),0,chank,1,"Success".getBytes().length);
                        mChatService.write(chank);
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constant.PERMISSION_REQUEST_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            // Gather permission grant results
            for (int i = 0; i < grantResults.length; i++) {
                // Add only permissions which are denied
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            // Check if all permissions are granted
            if (deniedCount == 0) {
                // Proceed ahead with the app
                firstCameraPermission();
            }
            // Atleast one or all permissions are denied
            else {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                    String permName = entry.getKey();

                    // permission is denied (this is the first time, when "never ask again" is not checked)
                    // so ask again explaining the usage of permission
                    // shouldShowRequestPermissionRationale will return true
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permName)) {
                        // Show dialog of explanation
                        permissionHelper.showDialog(getResources().getString(R.string.permission_title),
                                getResources().getString(R.string.permission_reason_text),
                                getResources().getString(R.string.permission_grant_text),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        permissionHelper.isPermissionsGranted(appPermissions);
                                    }
                                },
                                getResources().getString(R.string.exit_app_text),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        getActivity().finish();
                                    }
                                });
                    }
                    //permission is denied (and never ask again is  checked)
                    //shouldShowRequestPermissionRationale will return false
                    else {
                        // Ask user to go to settings and manually allow permissions
                        permissionHelper.showDialog(getResources().getString(R.string.permission_title),
                                getResources().getString(R.string.permission_deny_text),
                                getResources().getString(R.string.go_settings_text),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        // Go to app settings
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getActivity().getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        getActivity().finish();
                                    }
                                },
                                getResources().getString(R.string.exit_app_text), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        getActivity().finish();
                                    }
                                });
                        break;
                    }
                }
            }
        }
    }
    //camera and profile pic functions
    private void firstCameraPermission() {
        sCameraPhotoPath = permissionHelper.browseForImage();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
//            if (data == null) {
//                File  file = new File(
//                        Environment.getExternalStoragePublicDirectory(
//                                Environment.DIRECTORY_PICTURES)+"/Screenshots", "Test.png");
//
//                // If there is not data, then we may have taken a photo
//
//
//                    InputStream inputStream = null;//You can get an inputStream using any IO API
//                    try {
//                        inputStream = new FileInputStream(file.getAbsolutePath());
//                        byte[] buffer = new byte[8192];
//                        int bytesRead;
//                        ByteArrayOutputStream output = new ByteArrayOutputStream();
//                        Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
//                        try {
//                            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                                output64.write(buffer, 0, bytesRead);
//                            }
//                            output64.close();
//
//                            sendMessage(output.toString());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            } else {
//                String dataString = data.getDataString();
//                if (dataString != null) {
//                    permissionHelper.pickedFromGallery(resultCode, data, 81, 81);
//                }
           }

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}
