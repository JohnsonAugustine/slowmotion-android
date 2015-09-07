package me.hosiet.slowmotion;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;

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

        /****************** Drawer Setup **************************/

        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem item1 = new PrimaryDrawerItem()
                .withName(R.string.drawer_play_note)
                .withIdentifier(1);
        PrimaryDrawerItem item2 = new PrimaryDrawerItem()
                .withName(R.string.drawer_play_music)
                .withIdentifier(2);
        PrimaryDrawerItem item3 = new PrimaryDrawerItem()
                .withName(R.string.drawer_debug)
                .withIdentifier(3)
                .withBadge("333");

        final Drawer drawer = new DrawerBuilder()
                .withActivity(this)
                //.withTranslucentStatusBar(false) // on Android 5+, should always be false
                //.withActionBarDrawerToggle(true) // if set, no matter true or not, drawer will show partly
                .addDrawerItems(
                        item1,
                        item2,
                        new DividerDrawerItem(),
                        item3,
                        new DividerDrawerItem()
                )
                .build();

        drawer.setSelection(3);
        drawer.addStickyFooterItem(new PrimaryDrawerItem().withName("Settings").withIdentifier(4));

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

    }

}