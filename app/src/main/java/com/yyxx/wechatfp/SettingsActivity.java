package com.yyxx.wechatfp;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.yyxx.wechatfp.Utils.AESHelper;
import com.yyxx.wechatfp.Utils.SecuritySharedPreference;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private SharedPreferences prefs,defaultprefs;
    private EditTextPreference mPaypwd;
    private CheckBoxPreference mEnable;
    private static final String MOD_PREFS = "fp_settings";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs=getSharedPreferences(MOD_PREFS, 1);
        defaultprefs= PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.preference);
        mEnable=(CheckBoxPreference)findPreference("enable_fp");
        mPaypwd=(EditTextPreference)findPreference("paypwd");
        mPaypwd.setText(prefs.getString("paypwd",""));
        mEnable.setChecked(prefs.getBoolean("enable_fp",false));
        mPaypwd.setOnPreferenceChangeListener(this);
        mEnable.setOnPreferenceChangeListener(this);
        mEnable.setOnPreferenceClickListener(this);
        mPaypwd.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference==mPaypwd){
            SharedPreferences.Editor mEditor=prefs.edit();
            SharedPreferences.Editor dmEditor=defaultprefs.edit();

            String ANDROID_ID = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
            Log.e("deviceid",ANDROID_ID);

            String pwd=(String)newValue;
            Log.e("deviceid",AESHelper.encrypt(pwd,ANDROID_ID));
            if(pwd.length()>10){
                mEditor.putString("paypwd",pwd);
                dmEditor.putString("paypwd",pwd);
            }else{
                mEditor.putString("paypwd", AESHelper.encrypt(pwd,ANDROID_ID));
                dmEditor.putString("paypwd", AESHelper.encrypt(pwd,ANDROID_ID));
            }
            dmEditor.commit();
            return mEditor.commit();
        }
        if(preference==mEnable){
            SharedPreferences.Editor mEditor=prefs.edit();
            SharedPreferences.Editor dmEditor=defaultprefs.edit();
            mEditor.putBoolean("enable_fp",(boolean)newValue);
            mEditor.putBoolean("enable_fp",(boolean)newValue);
            dmEditor.commit();
            return mEditor.commit();
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference==mPaypwd){
            mPaypwd.setText(prefs.getString("paypwd",""));
        }
        return false;
    }
}

