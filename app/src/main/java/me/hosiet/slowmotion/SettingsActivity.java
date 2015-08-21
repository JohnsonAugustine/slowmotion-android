package me.hosiet.slowmotion;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

/**
 * Settings Activity.
 *
 * Created by hosiet on 15-8-21.
 */
public class SettingsActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    /**
     * Static SettingsFragment class
     *
     * Used to display pref from pre-defined XML.
     */
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences to UI from XML resource
            this.addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the content of the activity
        this.getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Listen to settings change
     */
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
/* // HAVE BUGS.
        if (key.equals(getString(R.string.key_pref_remote_addr))) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    EditText editText = (EditText) findViewById(R.id.input_hostname);//@TODO NULL POINTER
                    String myStr = sharedPreferences.getString(key, "");
                    editText.setText(myStr);
                }
            });
        }
    */
    }
}
