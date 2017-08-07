package com.yyxx.wechatfp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
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

import com.yyxx.wechatfp.ObfuscationHelper.MM_Classes;
import com.yyxx.wechatfp.ObfuscationHelper.MM_Fields;
import com.yyxx.wechatfp.ObfuscationHelper.MM_Res;
import com.yyxx.wechatfp.Utils.AESHelper;

import rx.Subscriber;
import rx.Subscription;
import zwh.com.lib.FPerException;
import zwh.com.lib.RxFingerPrinter;
import static zwh.com.lib.CodeException.FINGERPRINTERS_FAILED_ERROR;
import static zwh.com.lib.CodeException.HARDWARE_MISSIING_ERROR;
import static zwh.com.lib.CodeException.KEYGUARDSECURE_MISSIING_ERROR;
import static zwh.com.lib.CodeException.NO_FINGERPRINTERS_ENROOLED_ERROR;
import static zwh.com.lib.CodeException.PERMISSION_DENIED_ERROE;
import static zwh.com.lib.CodeException.SYSTEM_API_ERROR;

public class WalletBaseUI implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static Activity WalletPayUI_Activity=null;
    private static EditText mInputEditText=null;
    private static XSharedPreferences XMOD_PREFS;
    private RelativeLayout Passwd;
    private ImageView fingerprint;
    private RelativeLayout fp_linear;
    private TextView inputpwd,passwdtv;
    private static RxFingerPrinter rxFingerPrinter;
    private static boolean needfp;
    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XMOD_PREFS = new XSharedPreferences("com.yyxx.wechatfp", "fp_settings");
        XMOD_PREFS.makeWorldReadable();
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
                            if(mEnable && needfp){
                                initFingerPrintLock();
                                Passwd = (RelativeLayout)XposedHelpers.getObjectField(param.thisObject, MM_Fields.PaypwdView);
                                mInputEditText= (EditText) XposedHelpers.getObjectField(Passwd, MM_Fields.PaypwdEditText);
                                mInputEditText.setVisibility(View.GONE);
                                inputpwd = (TextView) XposedHelpers.getObjectField(param.thisObject, MM_Fields.PayTitle);
                                inputpwd.setText(MM_Res.Finger_title);
                                final View mKeyboard = (View) XposedHelpers.getObjectField(param.thisObject, MM_Fields.PayInputView);
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
                                        rxFingerPrinter.unSubscribe(WalletPayUI_Activity);
                                        inputpwd.setText(MM_Res.passwd_title);
                                    }
                                });
                                /*
                                passwdtv = (TextView) XposedHelpers.getObjectField(param.thisObject, MM_Fields.Passwd_Text);
                                passwdtv.setVisibility(View.VISIBLE);
                                passwdtv.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Passwd.removeView(fp_linear);
                                        mInputEditText.setVisibility(View.VISIBLE);
                                        mKeyboard.setVisibility(View.VISIBLE);
                                        rxFingerPrinter.unSubscribe(WalletPayUI_Activity);
                                        inputpwd.setText(MM_Res.passwd_title);
                                    }
                                });
                                */
                            }else{
                                rxFingerPrinter.unSubscribe(WalletPayUI_Activity);
                            }
                        }
                    });
                    XposedHelpers.findAndHookMethod(MM_Classes.Payview, "dismiss", new XC_MethodHook() {
                        @TargetApi(21)
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if(WalletPayUI_Activity != null){
                                rxFingerPrinter.unSubscribe(WalletPayUI_Activity);
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
        if (Build.VERSION.SDK_INT >= 23 && WalletPayUI_Activity.getSystemService(WalletPayUI_Activity.FINGERPRINT_SERVICE) != null) {
            try {
                rxFingerPrinter = new RxFingerPrinter(WalletPayUI_Activity);
                Subscription subscription =
                        rxFingerPrinter
                                .begin()
                                .subscribe(new Subscriber<Boolean>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        if (e instanceof FPerException) {
                                            switch (((FPerException) e).getCode()) {
                                                case SYSTEM_API_ERROR:
                                                    Toast.makeText(WalletPayUI_Activity, "系统API小于23，请关闭WechaFp模块", Toast.LENGTH_SHORT).show();
                                                    needfp = false;
                                                case PERMISSION_DENIED_ERROE:
                                                    Toast.makeText(WalletPayUI_Activity, "未开启微信使用指纹的权限", Toast.LENGTH_SHORT).show();
                                                    needfp = false;
                                                case HARDWARE_MISSIING_ERROR:
                                                    Toast.makeText(WalletPayUI_Activity, "未找到可用指纹传感器，请关闭WechaFp模块", Toast.LENGTH_SHORT).show();
                                                    needfp = false;
                                                case KEYGUARDSECURE_MISSIING_ERROR:
                                                    Toast.makeText(WalletPayUI_Activity, "未开启锁屏密码保护", Toast.LENGTH_SHORT).show();
                                                    needfp = false;
                                                case NO_FINGERPRINTERS_ENROOLED_ERROR:
                                                    Toast.makeText(WalletPayUI_Activity, "未录入指纹，请在系统中录入指纹后再使用", Toast.LENGTH_SHORT).show();
                                                case FINGERPRINTERS_FAILED_ERROR:
                                                    Toast.makeText(WalletPayUI_Activity, "指纹认证失败", Toast.LENGTH_SHORT).show();
                                                default:
                                                    Log.e("test1",((FPerException) e).getDisplayMessage());
                                            }
                                        }
                                    }

                                    @Override
                                    public void onNext(Boolean aBoolean) {
                                        if (aBoolean) {
                                            Toast.makeText(WalletPayUI_Activity, "指纹识别成功", Toast.LENGTH_SHORT).show();
                                            onSuccessUnlock();
                                        } else {
                                            Toast.makeText(WalletPayUI_Activity, "指纹识别失败", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                rxFingerPrinter.addSubscription(WalletPayUI_Activity, subscription);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static void onSuccessUnlock() {
        XMOD_PREFS.reload();
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
