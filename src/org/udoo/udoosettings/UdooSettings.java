package org.udoo.udoosettings;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.udoo.udoosettings.interfaces.OnResult;
import org.udoo.udoosettings.task.UtilUdoo;
import com.android.settings.R;

/**
 * Created by harlem88 on 17/03/16.
 */
public class UdooSettings extends PreferenceFragment {
    private String mDisplayTypeKey;

@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisplayTypeKey = getString(R.string.display_type_key);
        UtilUdoo.ReadParameter(new OnResult<String>() {
            @Override
            public void onSuccess(final String o) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addPreferencesFromResource(R.xml.udoo_settings);
                        Preference preference = getPreferenceManager().findPreference(getString(R.string.display_type_key));
                        preference.setSummary(o);
                        preference.getEditor().putString(mDisplayTypeKey, o).apply();
                        preference.setOnPreferenceChangeListener(preferenceChangeListener);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });

    }

    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final String value = (String) newValue;
            UtilUdoo.WriteParameter(value, new OnResult<Boolean>() {
                @Override
                public void onSuccess(Boolean isWritePreference) {
                    if (isWritePreference) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                preference.setSummary(value);
                                preference.getEditor().putString(mDisplayTypeKey, value).apply();
                            }
                        });
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
            return true;
        }
    };
}
