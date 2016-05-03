package org.udoo.udoosettings;

import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import org.udoo.udoosettings.interfaces.OnResult;
import org.udoo.udoosettings.task.UtilUdoo;
import com.android.settings.R;
import java.util.ArrayList;

/**
 * Created by harlem88 on 17/03/16.
 */
public class UdooSettings extends PreferenceFragment {
    private String mDisplayTypeKey;
    private CheckBoxPreference mEnableExtOTG;
    private static final String ENABLE_EXTERNAL_OTG = "enable_external_otg";
    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
    private static final String ADK_PROP = "persist.udoo_enable_adk";
    private Handler mUIHandler;
    private final static String UDOO_QUAD = "UDOO-MX6DQ";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.udoo_settings);
        mUIHandler = new Handler();
        mDisplayTypeKey = getString(R.string.display_type_key);

        if (Build.MODEL.equals(UDOO_QUAD))
            mEnableExtOTG = findAndInitCheckboxPref(ENABLE_EXTERNAL_OTG);

        init();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean success = false;
        if (preference == mEnableExtOTG) {
            String stateAdk = (mEnableExtOTG.isChecked() ? "true" : "false");
            success = true;

            UtilUdoo.Set(ADK_PROP, stateAdk, new OnResult<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    if (!success && mUIHandler != null) {
                        mUIHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mEnableExtOTG.setChecked(!mEnableExtOTG.isChecked());
                            }
                        });
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                }
            });
        }
        return success;
    }

    private void init() {
        if (Build.MODEL.equals(UDOO_QUAD)) {
            UtilUdoo.Get(ADK_PROP, new OnResult<String>() {
                @Override
                public void onSuccess(final String state) {
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (state != null && state.equals("true"))
                                mEnableExtOTG.setChecked(true);
                            else
                                mEnableExtOTG.setChecked(false);
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                }
            });
        }

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
                                Toast.makeText(getActivity(), getString(R.string.apply_mod_ok), Toast.LENGTH_LONG).show();
                                preference.setSummary(value);
                                preference.getEditor().putString(mDisplayTypeKey, value).apply();
                            }
                        });
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), getString(R.string.apply_mod_ko), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.apply_mod_ko), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
            return true;
        }
    };

    private CheckBoxPreference findAndInitCheckboxPref(String key) {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);

        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        } else if (key.equals(ENABLE_EXTERNAL_OTG)) {
            pref.setChecked(false);
        }
        mAllPrefs.add(pref);
        return pref;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        mAllPrefs.remove(preference);
    }

}

