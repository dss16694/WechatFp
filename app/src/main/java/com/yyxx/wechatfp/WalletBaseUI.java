package com.yyxx.wechatfp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.yyxx.wechatfp.ObfuscationHelper.MM_Classes;
import com.yyxx.wechatfp.ObfuscationHelper.MM_Fields;
import com.yyxx.wechatfp.ObfuscationHelper.MM_Res;
import com.yyxx.wechatfp.Utils.AESHelper;



public class WalletBaseUI implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static Activity WalletPayUI_Activity=null;
    private static EditText mInputEditText=null;
    private static XSharedPreferences XMOD_PREFS = null;
    private RelativeLayout Passwd;
    private ImageView fingerprint;
    private RelativeLayout fp_linear;
    private TextView inputpwd,passwdtv;
    private static FingerprintIdentify mFingerprintIdentify;
    private static boolean needfp;
    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    public void initZygote(StartupParam startupParam) throws Throwable {
        XMOD_PREFS = new XSharedPreferences("com.yyxx.wechatfp", "fp_settings");
        XMOD_PREFS.makeWorldReadable();
        XposedBridge.log("设置数量"+String.valueOf(XMOD_PREFS.getAll().size()));
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("loaded: " + lpparam.packageName);
        if (lpparam.packageName.equals(WECHAT_PACKAGENAME)) {
            try {
                Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
                if (ObfuscationHelper.init(context.getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0).versionCode, context.getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0).versionName, lpparam)) {
                    XposedHelpers.findAndHookMethod(MM_Classes.PayUI, "onCreate",Bundle.class, new XC_MethodHook() {
                        @TargetApi(21)
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            WalletPayUI_Activity = (Activity)param.thisObject;
                            needfp = true;
                        }
                    });
                    XposedHelpers.findAndHookMethod(MM_Classes.FetchUI, "onCreate",Bundle.class, new XC_MethodHook() {
                        @TargetApi(21)
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            WalletPayUI_Activity = (Activity)param.thisObject;
                            needfp = true;
                        }
                    });
                    XposedHelpers.findAndHookConstructor(MM_Classes.Payview ,Context.class , new XC_MethodHook() {
                        @TargetApi(21)
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean mEnable;
                            XMOD_PREFS.reload();
                            if(XMOD_PREFS.getAll().size()>0){
                                mEnable = XMOD_PREFS.getBoolean("enable_fp",false);
                            }else{
                                mEnable = false;
                            }
                            if(mEnable && needfp && WalletPayUI_Activity!= null){
                                initFingerPrintLock();
                                Passwd = (RelativeLayout)XposedHelpers.getObjectField(param.thisObject, MM_Fields.PaypwdView);
                                mInputEditText= (EditText) XposedHelpers.getObjectField(Passwd, MM_Fields.PaypwdEditText);
                                XposedBridge.log("密码输入框:" + mInputEditText.getClass().getName());
                                mInputEditText.setVisibility(View.GONE);
                                inputpwd = (TextView) XposedHelpers.getObjectField(param.thisObject, MM_Fields.PayTitle);
                                inputpwd.setText(MM_Res.Finger_title);
                                final View mKeyboard = (View) XposedHelpers.getObjectField(param.thisObject, MM_Fields.PayInputView);
                                XposedBridge.log("密码键盘:" + mKeyboard.getClass().getName());
                                mKeyboard.setVisibility(View.GONE);
                                fp_linear = new RelativeLayout(WalletPayUI_Activity);
                                RelativeLayout.LayoutParams layoutParams= new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                                fp_linear.setLayoutParams(layoutParams);
                                fingerprint = new ImageView(WalletPayUI_Activity);
                                fingerprint.setImageResource(MM_Res.Finger_icon);
                                fp_linear.addView(fingerprint);
                                Passwd.addView(fp_linear);
                                fingerprint.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Passwd.removeView(fp_linear);
                                        mInputEditText.setVisibility(View.VISIBLE);
                                        mKeyboard.setVisibility(View.VISIBLE);
                                        mFingerprintIdentify.cancelIdentify();
                                        inputpwd.setText(MM_Res.passwd_title);
                                    }
                                });
                                passwdtv = (TextView) XposedHelpers.getObjectField(param.thisObject, MM_Fields.Passwd_Text);
                                passwdtv.setVisibility(View.VISIBLE);
                                passwdtv.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Passwd.removeView(fp_linear);
                                        mInputEditText.setVisibility(View.VISIBLE);
                                        mKeyboard.setVisibility(View.VISIBLE);
                                        mFingerprintIdentify.cancelIdentify();
                                        inputpwd.setText(MM_Res.passwd_title);
                                    }
                                });
                            }else{

                            }
                        }
                    });
                    XposedHelpers.findAndHookMethod(MM_Classes.Payview, "dismiss", new XC_MethodHook() {
                        @TargetApi(21)
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if(WalletPayUI_Activity != null){
                                mFingerprintIdentify.cancelIdentify();
                                WalletPayUI_Activity = null;
                                needfp = false;
                            }
                        }
                    });
                }
            } catch (Throwable l) {
                XposedBridge.log(l);
            }
        }
    }

    public static void initFingerPrintLock() {
        mFingerprintIdentify = new FingerprintIdentify(WalletPayUI_Activity);
        if (mFingerprintIdentify.isFingerprintEnable()) {
            mFingerprintIdentify.startIdentify(3, new BaseFingerprint.FingerprintIdentifyListener() {
                @Override
                public void onSucceed() {
                    // 验证成功，自动结束指纹识别
                    Toast.makeText(WalletPayUI_Activity, "指纹识别成功", Toast.LENGTH_SHORT).show();
                    onSuccessUnlock();
                }

                @Override
                public void onNotMatch(int availableTimes) {
                    // 指纹不匹配，并返回可用剩余次数并自动继续验证
                    Toast.makeText(WalletPayUI_Activity, "指纹识别失败，还可尝试"+String.valueOf(availableTimes)+"次", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(boolean isDeviceLocked) {
                    // 错误次数达到上限或者API报错停止了验证，自动结束指纹识别
                    // isDeviceLocked 表示指纹硬件是否被暂时锁定
                    Toast.makeText(WalletPayUI_Activity, "多次尝试错误，请确认指纹", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartFailedByDeviceLocked() {
                    // 第一次调用startIdentify失败，因为设备被暂时锁定
                    Toast.makeText(WalletPayUI_Activity, "系统限制，重启后必须验证密码后才能使用指纹验证", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private static void onSuccessUnlock() {
        String pwd;
        String ANDROID_ID = Settings.System.getString(WalletPayUI_Activity.getContentResolver(), Settings.System.ANDROID_ID);
        if(XMOD_PREFS.getAll().size() > 0){
            pwd= XMOD_PREFS.getString("paypwd", "");
        }else{
            pwd="";
        }
        if(pwd.length()>0){
            mInputEditText.setText(AESHelper.decrypt(pwd,ANDROID_ID));
        }else{
            Toast.makeText(WalletPayUI_Activity, "未设定支付密码，请在WechatFp中设定微信的支付密码", Toast.LENGTH_SHORT).show();
        }

    }




}
