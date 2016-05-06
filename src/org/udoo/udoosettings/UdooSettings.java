package org.udoo.udoosettings;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
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
    private static final String GOVERNORS_LIST = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    private static final String GOVERNOR = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    private static final String NO_GOVERNOR_AVAILABLE = "no governor founds";
    private static final String SETTINGS_AUDIO_DEVICE = "persist.audio.device";
    private ListPreference mAudioDevicePref;
    private static final String KEY_AUDIO_DEVICE = "audio_device";
    private String mAudioDevice;
    private static final String AUDIO_DEVICE_DEFAULT = "imx-hdmi-soc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.udoo_settings);
        mUIHandler = new Handler();
        mDisplayTypeKey = getString(R.string.display_type_key);


        if (Build.MODEL.equals(UDOO_QUAD)) {
            mEnableExtOTG = findAndInitCheckboxPref(ENABLE_EXTERNAL_OTG);
            mAudioDevicePref = (ListPreference) findPreference(KEY_AUDIO_DEVICE);
        }

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
            mAudioDevicePref.setOnPreferenceChangeListener(preferenceChangeListener);

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

            UtilUdoo.Get(SETTINGS_AUDIO_DEVICE, new OnResult<String>() {
                @Override
                public void onSuccess(String value) {
                    mAudioDevice = value.length() > 0 ? value : AUDIO_DEVICE_DEFAULT;

                    mAudioDevicePref.setValue(mAudioDevice);
                    mAudioDevicePref.setSummary(mAudioDevice);
                }

                @Override
                public void onError(Throwable throwable) {
                    mAudioDevice = AUDIO_DEVICE_DEFAULT;
                    mAudioDevicePref.setValue(mAudioDevice);
                    mAudioDevicePref.setSummary(mAudioDevice);
                }
            });

        }

        UtilUdoo.ReadParameter(new OnResult<String>() {
            @Override
            public void onSuccess(final String o) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.display_type_key));
                preference.setSummary(o);
                preference.getEditor().putString(mDisplayTypeKey, o).apply();
                preference.setOnPreferenceChangeListener(preferenceChangeListener);
            }

            @Override
            public void onError(Throwable throwable) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.display_type_key));
                preference.setSummary("-");
            }
        });

        UtilUdoo.ReadOneLine(GOVERNOR, new OnResult<String>() {
            @Override
            public void onSuccess(String o) {
                ListPreference preference = (ListPreference) getPreferenceManager().findPreference(getString(R.string.governor_type_key));
                preference.setValue(o);
                preference.setSummary(o);
            }

            @Override
            public void onError(Throwable throwable) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.governor_type_key));
                preference.setSummary("-");
            }
        });

        UtilUdoo.ReadOneLine(GOVERNORS_LIST, new OnResult<String>() {
            @Override
            public void onSuccess(String governors) {
                ListPreference listPreference = (ListPreference) getPreferenceManager().findPreference(getString(R.string.governor_type_key));
                if (governors.length() > 0) {
                    String[] av_govs = governors.split(" ");
                    listPreference.setEntries(av_govs);
                    listPreference.setEntryValues(av_govs);

                    listPreference.setOnPreferenceChangeListener(preferenceChangeListener);
                } else {
                    CharSequence seq[] = new CharSequence[1];
                    seq[0] = NO_GOVERNOR_AVAILABLE;
                    listPreference.setEntries(seq);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.governor_type_key));
                preference.setSummary("-");
            }
        });
    }

    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final String value = (String) newValue;
            final String pref_key = preference.getKey();

            if (pref_key.equals(getString(R.string.display_type_key))) {
                UtilUdoo.WriteParameter(value, new OnResult<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isWritePreference) {
                        if (isWritePreference) {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), getString(R.string.apply_video_mod_ok), Toast.LENGTH_LONG).show();
                                    preference.setSummary(value);
                                    preference.getEditor().putString(mDisplayTypeKey, value).apply();
                                }
                            });
                        } else {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), getString(R.string.apply_governor_mod_ko), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), getString(R.string.apply_video_mod_ko), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            } else if (pref_key.equals(getString(R.string.governor_type_key))) {

                if (UtilUdoo.ExecuteCommandLine("su -c echo " + value + " > " + GOVERNOR)) {
                    preference.setSummary(value);
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.apply_governor_mod_ok), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.apply_governor_mod_ko), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else if (KEY_AUDIO_DEVICE.equals(pref_key)) {
                if (!mAudioDevice.equals(value)) {
                    UtilUdoo.Set(SETTINGS_AUDIO_DEVICE, value, new OnResult<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            if (result) {
                                mAudioDevice = value;
                                preference.setSummary(mAudioDevice);
                                mUIHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), R.string.audio_device_reboot_message, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else {
                                mUIHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), R.string.audio_device_set_error, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.audio_device_set_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });

                }
            }

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

