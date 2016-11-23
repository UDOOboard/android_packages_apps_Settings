package org.udoo.udoosettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
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
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by harlem88 on 17/03/16.
 */
public class UdooSettings extends PreferenceFragment {
    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
    private Handler mUIHandler;
    
    private CheckBoxPreference mEnableExtOTG;
    private static final String ADK_PROP = "persist.udoo_enable_adk";
    
    private final static String UDOO_QUAD = "UDOO-MX6DQ";
    private final static String A62 = "A62-MX6DQ";
    
    private static final String GOVERNORS_LIST = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    private static final String GOVERNOR = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    private static final String NO_GOVERNORS_AVAILABLE = "No governors found";

    private ListPreference mAudioDevicePref;
    private String mAudioDevice;
    private static final String AUDIO_DEVICE_PROPERTY = "persist.audio.device";
    private static final String AUDIO_DEVICE_DEFAULT = "imx-hdmi-soc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.udoo_settings);
        mUIHandler = new Handler();

        if (Build.MODEL.equals(UDOO_QUAD)) {
            mEnableExtOTG = findAndInitCheckboxPref("udoo_enable_external_otg_key");
        }

        if (Build.MODEL.equals(A62)) {
            removePreference(getPreferenceManager().findPreference("udoo_enable_external_otg_key"));
            removePreference(getPreferenceManager().findPreference("udoo_expand_data_partition_key"));
        }

        mAudioDevicePref = (ListPreference) findPreference("udoo_audio_device_key");

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

        mAudioDevicePref.setOnPreferenceChangeListener(preferenceChangeListener);
        UtilUdoo.Get(AUDIO_DEVICE_PROPERTY, new OnResult<String>() {
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

        UtilUdoo.ReadParameter(new OnResult<String>() {
            @Override
            public void onSuccess(final String o) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.udoo_video_output_key));
                preference.setSummary(o);
                preference.getEditor().putString(getString(R.string.udoo_video_output_key), o).apply();
                preference.setOnPreferenceChangeListener(preferenceChangeListener);
            }

            @Override
            public void onError(Throwable throwable) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.udoo_video_output_key));
                preference.setSummary("-");
            }
        });

        UtilUdoo.ReadOneLine(GOVERNOR, new OnResult<String>() {
            @Override
            public void onSuccess(String o) {
                ListPreference preference = (ListPreference) getPreferenceManager().findPreference(getString(R.string.udoo_governor_key));
                preference.setValue(o);
                preference.setSummary(o);
            }

            @Override
            public void onError(Throwable throwable) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.udoo_governor_key));
                preference.setSummary("-");
            }
        });

        UtilUdoo.ReadOneLine(GOVERNORS_LIST, new OnResult<String>() {
            @Override
            public void onSuccess(String governors) {
                ListPreference listPreference = (ListPreference) getPreferenceManager().findPreference(getString(R.string.udoo_governor_key));
                if (governors.length() > 0) {
                    String[] av_govs = governors.split(" ");
                    listPreference.setEntries(av_govs);
                    listPreference.setEntryValues(av_govs);

                    listPreference.setOnPreferenceChangeListener(preferenceChangeListener);
                } else {
                    CharSequence seq[] = new CharSequence[1];
                    seq[0] = NO_GOVERNORS_AVAILABLE;
                    listPreference.setEntries(seq);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Preference preference = getPreferenceManager().findPreference(getString(R.string.udoo_governor_key));
                preference.setSummary("-");
            }
        });

        Preference button = (Preference)findPreference(getString(R.string.udoo_reboot_recovery_key));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              showDialog(getString(R.string.udoo_recovery_title), getString(R.string.udoo_recovery_mod_ok),
                new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                     PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
                     pm.reboot("recovery");
                   }
               });

            return true;
            }
        });

        if (!Build.MODEL.equals(A62)) {
            Preference dataPartition = (Preference)findPreference(getString(R.string.udoo_expand_data_partition_key));
            dataPartition.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDialog(getString(R.string.udoo_expand_data_partition_dialog), getString(R.string.udoo_expand_data_partition_mod_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                expandDataPartition();
                            }
                        }
                    );
                    return true;
                }
            });
        }
    }
    
    private void expandDataPartition() {
        boolean success = false;
        if (UtilUdoo.ExecuteCommandLine("su -c expand-data-partition")) {
            try {
                Process p = Runtime.getRuntime().exec("sh");
                OutputStream os = p.getOutputStream();
                os.write("cp /system/etc/expand-data-fs.zip /cache/expand-data-fs.zip\n".getBytes());
                os.write("mkdir -p /cache/recovery/\n".getBytes());
                os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());
                os.write("echo '--update_package=/cache/expand-data-fs.zip' >> /cache/recovery/command\n".getBytes());
                os.flush();
                success = true;
            } catch (IOException ex) {
                return;
            }
            
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            pm.reboot("recovery");
        }
        
        if (!success) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), getString(R.string.udoo_governor_mod_ko), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final String value = (String) newValue;
            final String pref_key = preference.getKey();

            if (pref_key.equals(getString(R.string.udoo_video_output_key))) {
                UtilUdoo.WriteParameter(value, new OnResult<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isWritePreference) {
                        if (isWritePreference) {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showRebootDialog(getString(R.string.udoo_video_output_title), getString(R.string.udoo_video_output_mod_ok));
                                    preference.setSummary(value);
                                    preference.getEditor().putString(getString(R.string.udoo_video_output_key), value).apply();
                                }
                            });
                        } else {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), getString(R.string.udoo_video_output_mod_ko), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), getString(R.string.udoo_video_output_mod_ko), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            } else if (pref_key.equals(getString(R.string.udoo_governor_key))) {

                if (UtilUdoo.ExecuteCommandLine("su -c echo " + value + " > " + GOVERNOR)) {
                    preference.setSummary(value);
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.udoo_governor_mod_ok), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), getString(R.string.udoo_governor_mod_ko), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else if (pref_key.equals(getString(R.string.udoo_audio_device_key))) {
                if (!mAudioDevice.equals(value)) {
                    UtilUdoo.Set(AUDIO_DEVICE_PROPERTY, value, new OnResult<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            if (result) {
                                mAudioDevice = value;
                                mUIHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showRebootDialog(getString(R.string.udoo_audio_device_title), getString(R.string.udoo_audio_device_mod_ok));
                                        preference.setSummary(mAudioDevice);
                                    }
                                });

                            } else {
                                mUIHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), R.string.udoo_audio_device_mod_ko, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.udoo_audio_device_mod_ko, Toast.LENGTH_SHORT).show();
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
        } else if (key.equals("udoo_enable_external_otg_key")) {
            pref.setChecked(false);
        }
        mAllPrefs.add(pref);
        return pref;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        mAllPrefs.remove(preference);
    }

    private void showRebootDialog(String title, String message){
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
                        pm.reboot("");
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showDialog(String title, String message, DialogInterface.OnClickListener listener){
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
