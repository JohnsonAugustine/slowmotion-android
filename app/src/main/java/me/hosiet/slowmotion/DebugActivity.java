package me.hosiet.slowmotion;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.iflytek.cloud.SpeechConstant;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;

import me.hosiet.slowmotion.Communicator;
import me.hosiet.slowmotion.WelcomeFragment;
import com.iflytek.cloud.SpeechUtility;

/**
 * DebugActivity
 *
 * Created by hosiet on 15-8-30.
 */
public class DebugActivity extends AppCompatActivity implements Handler.Callback {

    /* socket used in the communication. Use it and load it. */
    public static Socket socket = null;
    public static String status = null; // Status of robot
    public static String received_string = null; // String received from Socket
    /* background thread message Handler */
    public static Handler mHandler = null;

    /* integer used to identify Message sent to background thread */
    public static final int COMMAND_CONNECT = 1;
    public static final int COMMAND_DISCONNECT = 2;
    public static final int COMMAND_RECONNECT = 3;
    public static final int COMMAND_SEND = 4;
    public static final int COMMAND_RECV = 5;
    public static final int COMMAND_RESET_ALL = 6; // wrapper for COMMAND_SEND

    public static final int REQUEST_RETURN_WELCOME = 101;
    public static final int REQUEST_REC_NOTE_PLAY = 102;
    public static final int REQUEST_REC_NOTE_CLEAN = 103;
    public static final int REQUEST_REC_NOTE_REC = 104;
    /* NOTE REC list variables */
    public static ArrayList<String> al_recNoteName = new ArrayList<>();
    public static ArrayList<Long> al_recNoteTime = new ArrayList<>();
    public static long al_recNoteTimeBegin = 0;


    /* various strings used in Logs */
    public static final String NAME_BG_THREAD = "Background Thread";
    public static final String NAME_ON_CREATE = "onCreate()";
    public static final String NAME_FG_HANDLER = "UI Thread Handler";

    /* the drawer */
    public static Drawer drawer;
    /* NOTE those are both for ID and position. */
    public static final int DRAWER_ID_NOTE = 1;
    public static final int DRAWER_ID_MUSIC = 2;
    public static final int DRAWER_ID_DEBUG = 4;
    public static final int DRAWER_ID_DISCONNECT = 6;
    public static final int DRAWER_ID_BEGIN_RECORD = 7;
    public static final int DRAWER_ID_STOP_RECORD = 8;
    public static final int DRAWER_ID_PLAY_RECORD = 9;
    public static final int DRAWER_ID_PLAY_TOGETHER = 10;
    public static final int DRAWER_ID_REBOOT = 12;
    public static final int DRAWER_ID_POWEROFF = 13;
    public static final int DRAWER_ID_SETTINGS = -1;

    /* UI Thread Handler */
    protected static class MainHandler extends Handler {
        private final WeakReference<DebugActivity> mActivity;

        public MainHandler(DebugActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REQUEST_RETURN_WELCOME:
                    // Reset UI to welcome fragment
                    // need msg.obj to be activity
                    Activity activity = (Activity) msg.obj;
                    FragmentManager fragmentManager = activity.getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.detach(fragmentManager.findFragmentById(R.id.debug_content_frame)); //detach first TODO See if crash
                    WelcomeFragment welcomeFragment = new WelcomeFragment();
                    fragmentTransaction.add(R.id.debug_content_frame, welcomeFragment);
                    fragmentTransaction.commit();
                    Toast.makeText(
                            activity.getApplicationContext(),
                            activity.getString(R.string.str_return_to_welcome),
                            Toast.LENGTH_SHORT
                    ).show();
                    // let drawer return to welcomeFragment
                    drawer.setSelection(DRAWER_ID_DEBUG);
                    break;
                case REQUEST_REC_NOTE_PLAY:
                    // run in foreground and open a dialog to take the space
                    // First, update time for further playing
                    // TODO FIXME
                    // NOTE!!! HAVE TO PASS A Activity with it!
                    // Stop playing by setting al_recNoteTimeBegin to 0
                    Activity myActivity = (Activity) msg.obj;
                    new AlertDialog.Builder(myActivity)
                            .setTitle("Playing Music...")
                                    //.setIcon(R.drawable.abc_tab_indicator_material)
                            .setMessage("Click to cancel the play.")
                            .setPositiveButton("STOP", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // stop playback
                                    al_recNoteTimeBegin = 0;
                                }
                            })
                            .show();
                    // start playback
                    Long beforehand = al_recNoteTimeBegin;
                    while (al_recNoteTimeBegin != 0 && ! al_recNoteTime.isEmpty() && !al_recNoteName.isEmpty()) {
                        Long afterhand = al_recNoteTime.get(0);
                        String now_noteName = al_recNoteName.get(0);
                        Log.i("REQUEST_REC_NOTE_PLAY", "Playing note "+now_noteName+" ...");
                        SystemClock.sleep(afterhand - beforehand);
                        Message msg3 = new Message();
                        msg3.what = COMMAND_SEND;
                        msg3.obj = "<play note=\""+now_noteName+"\"/>";
                        mHandler.sendMessage(msg3);
                        beforehand = afterhand;
                        al_recNoteTime.remove(0);
                        al_recNoteName.remove(0);
                    }
                    Log.i("REQUEST_REC_NOTE_PLAY", "Will now stop REC play.");
                    break;
                case REQUEST_REC_NOTE_CLEAN:
                    Log.i("mainHandler", "Will now CLEAN REC play.");
                    // clean all rec vars
                    al_recNoteTimeBegin = 0;
                    al_recNoteName = new ArrayList<>();
                    al_recNoteTime = new ArrayList<>();
                    break;
                case REQUEST_REC_NOTE_REC:
                    Log.i("mainHandler", "Will now BEGIN REC play.");
                    al_recNoteTimeBegin = System.currentTimeMillis();
                    al_recNoteName = new ArrayList<>();
                    al_recNoteTime = new ArrayList<>();
                    break;
            }
        }
    }
    public MainHandler mainHandler = new MainHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /* initialize iflytek libraries */
        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID +"="+getString(R.string.str_iflytek_cloud_appid));
        setTitle(getString(R.string.app_name));
        setContentView(R.layout.activity_debug);

        /****************** Drawer Setup **************************/

        // Create Account Header
        AccountHeader accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(android.R.drawable.screen_background_dark)
                .build();

        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem item1 = new PrimaryDrawerItem()
                .withName(R.string.drawer_play_note)
                .withIdentifier(DRAWER_ID_NOTE)
                .withBadge(getString(R.string.drawer_play_note_explanation));
        PrimaryDrawerItem item2 = new PrimaryDrawerItem()
                .withName(R.string.drawer_play_music)
                .withIdentifier(DRAWER_ID_MUSIC)
                .withBadge(getString(R.string.drawer_play_music_explanation));
        PrimaryDrawerItem item3 = new PrimaryDrawerItem()
                .withName(R.string.title_fragment_welcome_default)
                .withIdentifier(DRAWER_ID_DEBUG);
        SecondaryDrawerItem item4 = new SecondaryDrawerItem()
                .withName(R.string.str_robot_disconnect)
                .withIdentifier(DRAWER_ID_DISCONNECT);
        SecondaryDrawerItem item5 = new SecondaryDrawerItem()
                .withName(R.string.str_begin_note_record)
                .withIdentifier(DRAWER_ID_BEGIN_RECORD);
        SecondaryDrawerItem item6 = new SecondaryDrawerItem()
                .withName(R.string.str_stop_note_record)
                .withIdentifier(DRAWER_ID_STOP_RECORD);
        SecondaryDrawerItem item7 = new SecondaryDrawerItem()
                .withName(R.string.str_play_note_record)
                .withIdentifier(DRAWER_ID_PLAY_RECORD);
        // 根据选定音乐同时播放与敲击
        SecondaryDrawerItem item8 = new SecondaryDrawerItem()
                .withName(R.string.str_play_note_music_together)
                .withIdentifier(DRAWER_ID_PLAY_TOGETHER);
        PrimaryDrawerItem item9 = new PrimaryDrawerItem()
                .withName(R.string.str_reboot)
                .withIdentifier(DRAWER_ID_REBOOT);
        PrimaryDrawerItem item10 = new PrimaryDrawerItem()
                .withName(R.string.str_poweroff)
                .withIdentifier(DRAWER_ID_POWEROFF);

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(accountHeader)
                //.withTranslucentStatusBar(false) // on Android 5+, should always be false
                //.withActionBarDrawerToggle(true) // if set, no matter true or not, drawer will show partly
                .withSelectedItem(-1)
                .addDrawerItems(
                        item1,
                        item2,
                        new DividerDrawerItem(),
                        item3,
                        new DividerDrawerItem(),
                        item4,
                        item5,
                        item6,
                        item7,
                        item8,
                        new DividerDrawerItem(),
                        item9,
                        item10
                )
                .build();

        //drawer.setSelection(3);
        drawer.addStickyFooterItem(new PrimaryDrawerItem().withName("Settings").withIdentifier(4));
        drawer.setOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                /*Toast.makeText(
                        getApplicationContext(),
                        String.valueOf(position),
                        Toast.LENGTH_SHORT
                ).show();*/
                switch (position) {
                    case DRAWER_ID_NOTE:
                        /* load fragment */
                        NoteFragment noteFragment = new NoteFragment();
                        smLoadFragment(noteFragment);
                        break;
                    case DRAWER_ID_MUSIC:
                        break;
                    case DRAWER_ID_DEBUG:
                        break;
                    case DRAWER_ID_DISCONNECT:
                        Message msg = new Message();
                        msg.what = COMMAND_DISCONNECT;
                        msg.obj = DebugActivity.this;
                        mHandler.sendMessage(msg);
                        break;
                    case DRAWER_ID_BEGIN_RECORD:
                        // TODO
                        Message msg4 = new Message();
                        msg4.what = REQUEST_REC_NOTE_REC;
                        mainHandler.sendMessage(msg4);
                        break;
                    case DRAWER_ID_STOP_RECORD:
                        // TODO
                        Message msg2 = new Message();
                        msg2.what = REQUEST_REC_NOTE_CLEAN;
                        mainHandler.sendMessage(msg2);
                        break;
                    case DRAWER_ID_PLAY_RECORD:
                        // Make out a persistent dialog until user cancel
                        Message msg3 = new Message();
                        msg3.what = REQUEST_REC_NOTE_PLAY;
                        msg3.obj = DebugActivity.this;
                        mainHandler.sendMessage(msg3);
                        break;
                    case DRAWER_ID_PLAY_TOGETHER:
                        // start playback of RECORD and music together
                        // NOTE!!!!! should set music loop to TRUE!!!!!!!
                        //
                        // First, begin to play the music
                        try {
                            DebugActivity.this.findViewById(R.id.fragment_music_button_play).performClick();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(
                                    getApplicationContext(),
                                    "ERR: View Not load!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        // Second, call to start note playback. Won't stop music afterwards.
                        // THE SAME AS DRAWER_ID_PLAY_RECORD
                        // Make out a persistent dialog until user cancel
                        Message msg5 = new Message();
                        msg5.what = REQUEST_REC_NOTE_PLAY;
                        msg5.obj = DebugActivity.this;
                        mainHandler.sendMessage(msg5);
                        break;
                    case DRAWER_ID_SETTINGS:
                        /* start SettingsActivity */
                        startActivity(new Intent(getApplication(), SettingsActivity.class));
                        break;
                    case DRAWER_ID_REBOOT:
                        // reboot the robot
                        Message msg6 = new Message();
                        msg6.what = COMMAND_SEND;
                        msg6.obj = "<command action=\"reboot\"/>";
                        mHandler.sendMessage(msg6);
                        break;
                    case DRAWER_ID_POWEROFF:
                        // poweroff the robot
                        Message msg7 = new Message();
                        msg7.what = COMMAND_SEND;
                        msg7.obj = "<command action=\"poweroff\"/>";
                        mHandler.sendMessage(msg7);
                        break;
                }
                drawer.closeDrawer();
                return true;
            }
        });

        /******************* EOF Drawer Setup *********************/


        /****************** ACTION BAR SETUP **********************/

        Toolbar toolbar = (Toolbar) findViewById(R.id.debug_activity_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.title_activity_debug_default));
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerOpen()) {
                    drawer.closeDrawer();
                } else {
                    drawer.openDrawer();
                }
            }
        });


        ActionBar mActionBar = getSupportActionBar();

        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(false);
            mActionBar.setHomeButtonEnabled(true);
            mActionBar.setDisplayShowHomeEnabled(true);
        }
        /******************** EOF Action Bar Setup ******************/


        /* _______________  Welcome Fragment Setup ________________ */
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        WelcomeFragment welcomeFragment = new WelcomeFragment();
        fragmentTransaction.add(R.id.debug_content_frame, welcomeFragment);
        fragmentTransaction.commit();
        /* _______________ EOF Fragment Setup _____________________  */


        /* ^^^^^^^^^^^^^^^^ Handler Setup ^^^^^^^^^^^^^^^^^^^^^^^^^^^ */
        HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), this);
        /* ^^^^^^^^^^^^^^^^^^ EOF Handler Setup ^^^^^^^^^^^^^^^^^^^^^^ */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_debug, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

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
            case R.id.debug_action_settings:
                /* start a settings activity */
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.debug_action_reset_pref:
                /* call resetPreferences() */
                //TODO resetPreferences(null);
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.str_not_implemented),
                        Toast.LENGTH_SHORT
                ).show();
                break;
            case R.id.debug_action_reconnect:
                /* do reconnect to the robot */
                // TODO RECONNECT_FUNCTION_MENU
                Message msg = new Message();
                msg.what = COMMAND_RECONNECT;
                mHandler.sendMessage(msg);
                break;
            case R.id.debug_action_about:
                new AlertDialog.Builder(this)
                        .setTitle(getText(R.string.menu_main_about_string))
                        //.setIcon(R.drawable.abc_tab_indicator_material)
                        .setMessage(getText(R.string.dialog_about_info))
                        .setPositiveButton(getString(R.string.str_confirm), null)
                        .show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.getLooper().quit();              // 关闭 Looper 线程
    }

    /**
     * handle Message sent to the Activity.
     *
     * Note: THIS FUNCTION RUN IN BACKGROUND THREAD!!!
     * 本函数在后台线程运行！
     *
     * @param message message to be sent
     * @return as required by Handler.Callback
     */
    @Override
    public boolean handleMessage(Message message) {
        Log.i(NAME_BG_THREAD, "Now Begin handleMessage()");
        switch (message.what) {
            case COMMAND_CONNECT:
                Log.v(NAME_BG_THREAD, "Received COMMAND_CONNECT");
                socket = Communicator.smSocketConnect(getApplicationContext());
                if (socket != null) {
                    status = "STANDBY";
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_conn_established),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Log.e(NAME_BG_THREAD, "error occurred when connecting, no valid socket now.");
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_error_when_connecting),
                            Toast.LENGTH_SHORT
                    ).show();
                }
                break;
            case COMMAND_DISCONNECT:
                Log.v(NAME_BG_THREAD, "Received COMMAND_DISCONNECT");
                if (socket == null || !socket.isConnected()) {
                    Log.i(NAME_BG_THREAD, "socket is invalid, not doing anything.");
                } else if (! Communicator.smSocketDisconnect(getApplicationContext(), socket)) {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_error_when_disconnecting),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_conn_disconnected),
                            Toast.LENGTH_SHORT
                    ).show();
                    // Reset to WelcomeFragment
                    Message msg2 = new Message();
                    msg2.what = REQUEST_RETURN_WELCOME;
                    msg2.obj = message.obj;
                    mainHandler.sendMessage(msg2);
                }
                socket = null;
                status = null;
                break;
            case COMMAND_RECONNECT:
                Log.v(NAME_BG_THREAD, "Received COMMAND_RECONNECT");
                status = null;
                socket = Communicator.smSocketReconnect(getApplicationContext(), socket);
                if (socket == null) {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_error_when_reconnecting),
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    status = "STANDBY";
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_conn_reconnected),
                            Toast.LENGTH_SHORT
                    ).show();
                }
                break;
            case COMMAND_RESET_ALL:
                Log.v(NAME_BG_THREAD, "Received COMMAND_RESET_ALL");
                if (socket == null) {
                    Log.w(NAME_BG_THREAD, "in COMMAND_RESET_ALL: socket is null, not doing anything.");
                    break;
                }
                Communicator.smSendCmd_ResetAll(socket);
                if (socket != null && socket.isConnected()) {
                    status = "STANDBY";
                    break;
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.str_error_when_connecting),
                            Toast.LENGTH_SHORT
                    ).show();
                }
                break;
            case COMMAND_RECV:
                Log.v(NAME_BG_THREAD, "Received COMMAND_RECV");
                String temp_str = Communicator.smSocketGetText(socket);
                if (temp_str == null || temp_str.equals("")) {
                    Log.e(NAME_BG_THREAD, "COMMAND_RECV: nothing received");
                } else {
                    Log.i(NAME_BG_THREAD, "COMMAND_RECV: received str:"+temp_str);
                }
                received_string = temp_str;
                Log.v(NAME_BG_THREAD, "End OF COMMAND_RECV procedure");
                break;
            case COMMAND_SEND:
                Log.v(NAME_BG_THREAD, "Received COMMAND_SEND, str is "+(String)message.obj);
                try {
                    Communicator.smSocketSendText(socket, (String) message.obj);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(
                            getApplicationContext(),
                            "ERR_BAD_COMMAND_SEND",
                            Toast.LENGTH_SHORT
                    ).show();
                }
                break;
        }
        return true;
    }

    /**
     * load the given fragment into DebugActivity.
     *
     * @param fragment fragment to be inserted
     */
    public void smLoadFragment(Fragment fragment) {
        Log.i("smLoadFragment", "Now trying to load new fragment.");
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.detach(fragmentManager.findFragmentById(R.id.debug_content_frame)); //detach first
        fragmentTransaction.commit();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.debug_content_frame, fragment); //then attach it
        fragmentTransaction.commit();
    }

}