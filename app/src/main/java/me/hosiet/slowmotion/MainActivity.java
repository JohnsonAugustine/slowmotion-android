package me.hosiet.slowmotion;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "me.hosiet.slowmotion.MESSAGE";
    private Button [] mButton = new Button[9]; /* only declaration, not defination; not using 0 */

    private static boolean connected = false; /* for whether piThread has started or not */
    PiLoopedThread piThread = null;

    private MainHandler mainHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            case R.id.action_settings:
                break;
            case R.id.action_about:
                new AlertDialog.Builder(this)
                        .setTitle("About...")
                        //.setIcon(R.drawable.abc_tab_indicator_material)
                        .setMessage("Slowmotion robot\n\nFor USTC Robogame 2015")
                        .setPositiveButton("Confirm", null)
                        .show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Will reset to proper positon */
    public void sendMessage(View view) {
        // Let's try.
        /*
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);*/
        if (!connected) {
            /* do nothing */
            return;
        } else {
            Message msgResetPosition = piThread.mChildHandler.obtainMessage(PiLoopedThread.MSG_PLAY);
            msgResetPosition.obj = "0\n";
            piThread.mChildHandler.sendMessage(msgResetPosition);
        }
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
            Log.i("MAINHANDLER", "Reconnect finished.");
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
        private static final int MSG_STOP = 0;
        private static final int MSG_PLAY = 1;

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
                    Log.i(this.LOG_STR, "socket closed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
                /* SHOULD DESTROY THIS THREAD NOW */
            Looper mylooper = Looper.myLooper();
            if (mylooper != null) {
                mylooper.quit();
            }
            connected = false;
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
                /* should handle msg here */
                switch(msg.what) {
                    case PiLoopedThread.MSG_STOP:
                        myQuitLoopState();
                        break;
                    case PiLoopedThread.MSG_PLAY:
                        /* check connectivity */

                        /* send through the socket */
                        Log.i("HANDLER:", "Will play the music.");
                        try {

                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(piThread.socket.getOutputStream())),
                                    true);
                            out.println((String) msg.obj);
                            Log.i("HANDLER:", "Output will be " + msg.obj);
                            out.flush();
                            if (out.checkError()) {
                                /* Often connection is closed by peer, now re-connect and resend */
                                Log.e("HANDLER", "Error in writing, now re-send msg");
                                // TODO ReSEND, let main thread do the job
                                Message msgReConnect = mainHandler.obtainMessage();
                                mainHandler.sendMessage(msgReConnect);
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        break;
                }
                Log.i("HANDLER", "Handler has finished and reached end.");
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
}
