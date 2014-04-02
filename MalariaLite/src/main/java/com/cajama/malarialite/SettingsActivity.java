package com.cajama.malarialite;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Created by Jasper on 11/13/13.
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 11)
            getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        else
            addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setResult(1);
    }
}
