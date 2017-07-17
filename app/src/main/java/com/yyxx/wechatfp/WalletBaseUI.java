package com.yyxx.wechatfp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private TextView inputpwd;
    private static RxFingerPrinter rxFingerPrinter;
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XMOD_PREFS = new XSharedPreferences("com.yyxx.wechatfp", "fp_settings");
        XMOD_PREFS.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("loaded: " + lpparam.packageName);
        if (lpparam.packageName.equals("com.tencent.mm")) {
            try {
                Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
                if (ObfuscationHelper.init(context.getPackageManager().getPackageInfo("com.tencent.mm", 0).versionCode, context.getPackageManager().getPackageInfo("com.tencent.mm", 0).versionName, lpparam)) {
                    XposedHelpers.findAndHookMethod(MM_Classes.PayUI, "onCreate",Bundle.class, new XC_MethodHook() {
                        @TargetApi(21)
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            WalletPayUI_Activity = (Activity)param.thisObject;
                            initFingerPrintLock();
                        }
                    });
                    XposedHelpers.findAndHookMethod(MM_Classes.PayUI, "onDestroy", new XC_MethodHook() {
                        @TargetApi(21)
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            rxFingerPrinter.unSubscribe(WalletPayUI_Activity);
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
                            if(mEnable){
                                //Log.e("test1","hook in l");
                                Passwd = (RelativeLayout)XposedHelpers.getObjectField(param.thisObject, MM_Fields.PaypwdView);
                                //Log.e("test1",Passwd.getClass().getName());
                                mInputEditText= (EditText) XposedHelpers.getObjectField(Passwd, MM_Fields.PaypwdEditText);
                                mInputEditText.setVisibility(View.GONE);
                                //Log.e("test1",mInputEditText.getClass().getName());
                                inputpwd = (TextView) XposedHelpers.getObjectField(param.thisObject, MM_Fields.PayTitle);
                                inputpwd.setText("请验证指纹或点击指纹图标后输入密码");
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
                                        inputpwd.setText("请输入支付密码");
                                    }
                                });
                            }else{
                                rxFingerPrinter.unSubscribe(WalletPayUI_Activity);
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
                                                case PERMISSION_DENIED_ERROE:
                                                case HARDWARE_MISSIING_ERROR:
                                                case KEYGUARDSECURE_MISSIING_ERROR:
                                                case NO_FINGERPRINTERS_ENROOLED_ERROR:
                                                    break;
                                                case FINGERPRINTERS_FAILED_ERROR:
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
        //Log.e("test1","好开心，指纹验证成功");
        XMOD_PREFS.reload();
        String pwd;
        String ANDROID_ID = Settings.System.getString(WalletPayUI_Activity.getContentResolver(), Settings.System.ANDROID_ID);
        if(XMOD_PREFS.getAll().size() > 0){
            pwd= XMOD_PREFS.getString("paypwd", "");
        }else{
            pwd="";
        }
        //Log.e("test1",pwd);
        if(pwd.length()>0){
            mInputEditText.setText(AESHelper.decrypt(pwd,ANDROID_ID));
        }else{
            Toast.makeText(WalletPayUI_Activity, "未设定支付密码，请在WechatFp中设定微信的支付密码", Toast.LENGTH_SHORT).show();
        }

    }




}
