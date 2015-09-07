package me.hosiet.slowmotion;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.net.Socket;

import me.hosiet.slowmotion.Communicator;

/**
 * DebugActivity
 * Created by hosiet on 15-8-30.
 */
public class DebugActivity extends AppCompatActivity {

    /* socket used in the communication. Always started by intent and load it. */
    private Socket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug);

        ((Button) findViewById(R.id.debug_button_1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String line = Communicator.smSocketGetText(MainActivity.mainSocket);
                Toast.makeText(getApplicationContext(), "GOT:\n"+line, Toast.LENGTH_SHORT).show();
            }
        });
    }
}