package me.hosiet.slowmotion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "me.hosiet.slowmotion.MESSAGE";
    private Button [] mButton = new Button[9]; /* only declaration, not defination; not using 0 */

    private static boolean connected = false; /* for whether piThread has started or not */
    PiLoopedThread piThread = null;
    private static final int REQUEST_EX = 1;

    Uri uriData = null; /* for the file */
    String content = ""; //文件内容字符串

    private MainHandler mainHandler = null;

    /* For application bar and left drawer */
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mPlanetTitles;
    private ListView mDrawerList;

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            Log.e("ITEMCLICK", "Not implemented");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* load initial settings preferences */

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        /* set main UI View */
        setContentView(R.layout.activity_main);
        final Activity mActivity = this;


        /* set Handler for UI thread */
        mainHandler = new MainHandler();

        /* the OnClickListener() used for giving click info */
        OnClickListener play_listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.edit_message);
                editText.setText(v.getTag().toString());
                /* Call sendMessageToPi() when ready */
                sendMessageToPi(v);
            }
        };

        /* initialzations of my 8 buttons */
        for (int i = 1; i <= 8; i++) {
            /* use special way instead of R.id.* */
            mButton[i] = (Button)findViewById(getResources().getIdentifier("button_0" + Integer.toString(i), "id", this.getPackageName()));
            mButton[i].setOnClickListener(play_listener);
        }

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    mActivity.startActivityForResult(Intent.createChooser(intent, "Select"), REQUEST_EX);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /* the initialization for left-side drawer */
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mPlanetTitles = getResources().getStringArray(R.array.drawer_list_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        mDrawerToggle = new ActionBarDrawerToggle(
                this,                        /* host activity */
                mDrawerLayout,               /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
                ){
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                ActionBar mActionBar = getSupportActionBar();
                if (mActionBar == null) {
                    Log.e("MAIN_onCreate","ACTION BAR IS NULL!");
                } else {
                    mActionBar.setTitle(getString(R.string.title_activity_main));
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ActionBar mActionBar = getSupportActionBar();
                if (mActionBar == null) {
                    Log.e("MAIN_onCreate","ACTION BAR IS NULL!");
                } else {
                    mActionBar.setTitle(getString(R.string.title_drawer));
                }
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_EX) {
                uriData = intent.getData();
                Log.e("onActivityResult","Now uriData is"+uriData.toString());
                TextView text = (TextView) findViewById(R.id.text);
                text.setText("Received:: " + uriData);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EditText editText = (EditText) findViewById(R.id.input_hostname);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String myStr = prefs.getString(getString(R.string.key_pref_remote_addr), "");
        editText.setText(myStr);
        if (uriData != null) {
            Log.e("OnResume","last");
            ((TextView) findViewById(R.id.text)).setText(readPhoneNumber(uriData.toString(), uriData));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }*/

        switch(id) {
            case R.id.action_settings:
                /* start a settings activity */
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_reset_pref:
                /* call resetPreferences() */
                resetPreferences(null);
                break;
            case R.id.action_about:
                new AlertDialog.Builder(this)
                        .setTitle(getText(R.string.menu_main_about_string))
                                //.setIcon(R.drawable.abc_tab_indicator_material)
                        .setMessage(getText(R.string.dialog_about_info))
                        .setPositiveButton("Confirm", null)
                        .show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Reset preferences to default */
    public void resetPreferences(View view) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);//@TODO WHY INVALID?
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        getApplicationContext(),
                        "Reset done [INVALID]",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Handler to solve the disconnect problem
     *
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i("MAINHANDLER", "Now reconnecting socket.");
            triggerPiConnect(null);
            triggerPiConnect(null);
            /* Commented out due to bugs
            if (msg.obj instanceof Message && msg.obj != null) {
                //send msg.obj to piThread again
                while (!connected || piThread == null) {
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.e("MAINHANDLER", "Not connected yet, sleeping...");
                }
                if (piThread != null) {
                    Log.e("MAINHANDLER", "PiThread is not null!!!!!");
                }
                if (connected) {
                    Log.e("MAINHANDLER", "connected flag is true!!!!!!");
                }
                Log.i("MAINHANDLER", "Resending msg.obj..."+(String)((Message)msg.obj).what);
                Message msg_resend = piThread.mChildHandler.obtainMessage(((Message)msg.obj).what);
                Log.i("MAINHANDLER", "msg_resend obtained.");
                msg_resend.obj = ((Message)msg.obj).obj;
                piThread.mChildHandler.sendMessage(msg_resend);
                */
            Log.i("MAINHANDLER","Resent msg.obj.");
        }
    }
    /**
     * Customized Thread, with background information sending
     *
     * 写得并不好，好孩子不要学我
     * 因为 Looper 线程本来就不应该这样子
     */
    class PiLoopedThread extends Thread {
        public Socket socket = null;

        public final String LOG_STR = "PiThread";

        /** Looper Message Types */
        private static final int MSG_STOP = 3;
        private static final int MSG_PLAY = 1;
        private static final int IS_TEXT = 2;

        /* Message Handler */
        private Handler mChildHandler = null;

        /**
         * Quit the Loop state. Always called when thread is exiting.
         */
        public void myQuitLoopState() {
            Log.i(this.LOG_STR, "Recv quit request, exiting...");
            // Close socket here
            try {
                if (this.socket == null) {
                    Log.i(this.LOG_STR, "socket is null, ignoring");
                } else if (this.socket.isClosed()) {
                    Log.i(this.LOG_STR, "socket already closed, passing");
                } else {
                    socket.close();
                    connected = false;
                    Log.i(this.LOG_STR, "socket closed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "socket disconnected", Toast.LENGTH_SHORT).show();
                /* SHOULD DESTROY THIS THREAD NOW */
            Looper mylooper = Looper.myLooper();
            if (mylooper != null) {
                mylooper.quit();
            }
            connected = false;//Need a better place
        }

        /**
         * Extended Handler Class, to solve Handler problem
         *
         * Since static Class cannot solve the problem, I will keep the memory leak.
         */
        class MHandler extends Handler {
            /*private final WeakReference<PiLoopedThread> piLoopedThread;*/

            @Override
            public void handleMessage(Message msg) {
                Log.e("Mhandler", "msg.what ="+String.valueOf(msg.what));
                /* should handle msg here */
                switch (msg.what) {

                    case PiLoopedThread.MSG_STOP:
                        myQuitLoopState();
                        break;
                    case PiLoopedThread.IS_TEXT:
                        Log.e("Mhandler", "receivetext is "+msg.obj);
                        try {
                            Log.e("MHandler", "inside the first try");
                            try {
                                Log.e("MHandler", "now socket is");
                                if (socket == null){
                                    Log.e("MHandler", "Now socket is nul;");
                                }
                                OutputStream sendtext = socket.getOutputStream();
                                sendtext.write(("1\n").getBytes());
                                sendtext.flush();
                            } catch (SocketException e) {
                                Log.e("Mhandler:", "Exception happened when writing 1, exiting current handler");
                                Message msg_restart = mainHandler.obtainMessage();
                                msg_restart.obj = msg;
                                mainHandler.sendMessage(msg_restart);
                                // Now stop current thread
                                return;
                            } catch(Exception f) {
                                Log.e("MHandler", "inside common exception");
                                Log.e("MHandler", f.toString());
                            }
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println((String) msg.obj);
                            Log.i("Mhandler:", "Output will be " + msg.obj);
                            out.flush();
                            if (out.checkError()) {
                                /* Often connection is closed by peer, now re-connect and resend */
                                Log.e("MHANDLER", "Error in writing, now re-send msg");
                                // TODO ReSEND, let main thread do the job, REMOVE
                                Message msgReConnect = mainHandler.obtainMessage();
                                mainHandler.sendMessage(msgReConnect);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.e("MHandler", "END OF IS_TEXT");
                        break;
                       case PiLoopedThread.MSG_PLAY:
                        /* check connectivity */

                        /* send through the socket */
                        Log.e("MHANDLER:", "Will play the music.");
                        try {
                            /* first of all, write through a '0' to check connectivity */
                            try {
                                OutputStream o = socket.getOutputStream();
                                o.write("0".getBytes());
                                o.flush();
                                Log.e("MHANDLER:", "0 written and flushed");
                            } catch(SocketException e) {
                                /* connection closed by peer. Now re-connect!
                                   Solution:
                                           send a Message msg to UI thread,
                                           and msg.obj should be current msg.
                                           UI Handler should re-start the working thread
                                           and re-send the message.
                                 */
                                Log.e("MHANDLER:", "Exception happened when writing 0, exiting current handler");
                                Message msg_restart = mainHandler.obtainMessage();
                                msg_restart.obj = msg;
                                mainHandler.sendMessage(msg_restart);
                                // Now stop current thread
                                return;
                            }
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(piThread.socket.getOutputStream())),
                                    true);
                            out.println((String) msg.obj);
                            Log.e("MHANDLER:", "Output will be " + msg.obj);
                            out.flush();
                            if (out.checkError()) {
                                /* Often connection is closed by peer, now re-connect and resend */
                                Log.e("MHANDLER", "Error in writing, now re-send msg");
                                // TODO ReSEND, let main thread do the job, REMOVE
                                Message msgReConnect = mainHandler.obtainMessage();
                                mainHandler.sendMessage(msgReConnect);
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        break;
                }
                Log.e("MHANDLER", "MHandler has finished and reached end.");
            }
        }

        @Override
        public void run() {
            /* always run super-class codes */
            super.run();
            /* establish event loop */
            Looper.prepare();
            /* set handler */
            this.mChildHandler = new MHandler();
                /* do initialization of local variables */
            try {
                InetAddress serverAddr = InetAddress.getByName(((EditText) findViewById(R.id.input_hostname)).getText().toString());
                this.socket = new Socket(serverAddr, 9999);
            } catch(Exception e) {
                e.printStackTrace();
            }

            Log.i(this.LOG_STR, "socket connected");
            Toast.makeText(getApplicationContext(), "socket connected", Toast.LENGTH_SHORT).show();
                /* start looping */
            Looper.loop();
        }
    }

    /**
     * Initialization to new thread for connection.
     *
     * Thread exists together with the thread. If the connection was lost,
     * the thread should be destroyed together.
     * @param view View that triggered the function
     */
    public void triggerPiConnect(View view) {
        Button button_connect = (Button) findViewById(R.id.button_connect);
        if (!connected) {
            piThread = new PiLoopedThread();
            piThread.start();
            connected = true;
            button_connect.setText(getResources().getString(R.string.button_pi_to_disconnect));
            ((EditText) findViewById(R.id.edit_message)).setHint("Connected");
        } else {
            Message msgStopThread = piThread.mChildHandler.obtainMessage(PiLoopedThread.MSG_STOP);
            msgStopThread.obj = null;
            piThread.mChildHandler.sendMessage(msgStopThread);
            piThread = null;
            button_connect.setText(getResources().getString(R.string.button_pi_to_connect));
            ((EditText) findViewById(R.id.edit_message)).setHint("Disonnected");
            ((EditText) findViewById(R.id.edit_message)).setText("");
        }


    } /* triggerPiConnect() */


    /**
     * Send string to Raspberry Pi */
    public void sendMessageToPi(View view) {

        if (view == null) {
            /* view must not be null */
            Log.e("ERROR1", "View is NULL!");
            return;
        }

        String toServer = view.getTag().toString();

        if (!connected) {
            Log.e("MAIN", "Not connected, don't do anything");
            return;
        }
        Message msgPlayNote = piThread.mChildHandler.obtainMessage(PiLoopedThread.MSG_PLAY);
        msgPlayNote.obj = toServer;
        piThread.mChildHandler.sendMessage(msgPlayNote);
        Log.i("MAIN", "msgPlayNote sent");
    }

    public String readPhoneNumber(String strFilePath, Uri myuri)
    {
        //如果path是传递过来的参数，可以做一个非目录的判断
        try {
            InputStream instream = getContentResolver().openInputStream(myuri);
            if (instream != null) {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //分行读取
                while ((line = buffreader.readLine()) != null) {
                    content += line + "\n";
                    if (!connected) {
                        Log.e("readphonenumber", "Not connected, don't do anything");
                    }
                    if (piThread.mChildHandler!=null) {
                        Message mestext = piThread.mChildHandler.obtainMessage();
                        mestext.obj = line;
                        mestext.what = PiLoopedThread.IS_TEXT;
                        Log.e("mainHandler", "Now msg.what is"+String.valueOf(mestext.what)+" and msg.obj is"+mestext.obj);
                        piThread.mChildHandler.sendMessage(mestext);
                        Log.e("handler","done");
                    }
                }
                instream.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("My Content:", content);
        return content;
    }


}
