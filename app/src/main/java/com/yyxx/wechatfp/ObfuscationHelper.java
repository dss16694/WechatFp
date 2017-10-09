package com.yyxx.wechatfp;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ObfuscationHelper {
    public static int versionint=0;
    public static int version_code=0;

    public static class MM_Classes {
        public static Class<?> PayUI,FetchUI,Payview,WalletBaseUI;

        private static void init(int idx, LoadPackageParam lpparam) throws Throwable {
            switch(idx){
                case 0:case 1:case 2:case 3:case 4:
                    PayUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pay.ui.WalletPayUI", lpparam.classLoader);
                    Payview = XposedHelpers.findClass("com.tencent.mm.plugin.wallet_core.ui.l", lpparam.classLoader);
                    FetchUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.balance.ui.WalletBalanceFetchPwdInputUI", lpparam.classLoader);
                    WalletBaseUI = XposedHelpers.findClass("com.tencent.mm.wallet_core.ui.WalletBaseUI", lpparam.classLoader);
            }
        }
    }

    public static class MM_Fields {
        public static String PaypwdEditText;
        public static String PaypwdView;
        public static String PayInputView;
        public static String PayTitle;
        public static String Passwd_Text;

        private static void init(int idx) throws Throwable {
            switch(idx){
                case 0:
                    PaypwdView = "qVO";
                    PaypwdEditText ="vyO" ;
                    PayInputView = "mOL";
                    PayTitle = "qVK";
                    Passwd_Text = "qVK";
                case 1:
                    PaypwdView = "ryk";
                    PaypwdEditText ="wjm" ;
                    PayInputView = "nnG";
                    PayTitle = "ryg";
                    Passwd_Text = "ryz";
                case 2:
                    PaypwdView = "ryM";
                    PaypwdEditText ="wjX" ;
                    PayInputView = "nnZ";
                    PayTitle = "ryI";
                    Passwd_Text = "rzb";
                case 3:
                    PaypwdView = "rLB";
                    PaypwdEditText ="wDJ" ;
                    PayInputView = "nol";
                    PayTitle = "rLw";
                    Passwd_Text = "rLQ";
                case 4:
                    PaypwdView = "rWo";
                    PaypwdEditText ="xhU" ;
                    PayInputView = "nzg";
                    PayTitle = "rWj";
                    Passwd_Text = "rWD";
            }
        }
    }
    public static class MM_Res {
        public static int Finger_icon;
        public static int Finger_title;
        public static int passwd_title;

        private static void init(int idx) throws Throwable {
            switch (idx){
                case 0:
                    Finger_icon = 2130838280;
                    Finger_title = 2131236833;
                    passwd_title = 2131236838;
                case 1:
                    Finger_icon = 2130838289;
                    Finger_title = 2131236918;
                    passwd_title = 2131236923;
                case 2:
                    Finger_icon = 2130838289;
                    Finger_title = 2131236918;
                    passwd_title = 2131236923;
                case 3:
                    Finger_icon = 2130838298;
                    Finger_title = 2131236964;
                    passwd_title = 2131236969;
                case 4:
                    Finger_icon = 2130838248;
                    Finger_title = 2131237043;
                    passwd_title = 2131237048;
            }
        }
    }


    public static boolean init(int versioncode, String versionName, LoadPackageParam lpparam) throws Throwable {
        version_code = versioncode;
        int versionIndex = isSupportedVersion(versioncode, versionName);
        if (versionIndex < 0) {
            return false;
        }
        MM_Classes.init(versionIndex, lpparam);
        MM_Fields.init(versionIndex);
        MM_Res.init(versionIndex);
        return true;
    }


    public static int isSupportedVersion(int versioncode, String versionName) {
        if(versionName.contains("6.5.8")){
            versionint=0;
            return 0;
        }
        if(versionName.contains("6.5.10") && versioncode == 1080){
            versionint=1;
            return 1;
        }
        if(versionName.contains("6.5.10") && versioncode == 1061){
            versionint=2;
            return 2;
        }
        if(versionName.contains("6.5.13") && versioncode == 1100){
            versionint=3;
            return 3;
        }
        if(versionName.contains("6.5.16") && versioncode == 1120){
            versionint=4;
            return 4;
        }
        return -1;
    }
}
