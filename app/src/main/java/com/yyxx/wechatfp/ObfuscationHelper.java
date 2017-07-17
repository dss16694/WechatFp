package com.yyxx.wechatfp;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ObfuscationHelper {
    public static int versionint=0;
    public static int version_code=0;

    public static class MM_Classes {
        public static Class<?> Payview;
        public static Class<?> PayUI;

        private static void init(int idx, LoadPackageParam lpparam) throws Throwable {
            PayUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pay.ui." + new String[]{"WalletPayUI","WalletPayUI"}[idx], lpparam.classLoader);
            Payview = XposedHelpers.findClass("com.tencent.mm.plugin.wallet_core.ui." + new String[]{"l","l"}[idx], lpparam.classLoader);
        }
    }

    public static class MM_Fields {
        public static String PaypwdEditText;
        public static String PaypwdView;
        public static String PayInputView;
        public static String PayTitle;

        private static void init(int idx) throws Throwable {
            PaypwdView = new String[]{"qVO","ryk"}[idx];
            PaypwdEditText = new String[]{"vyO","wjm"}[idx];
            PayInputView = new String[]{"mOL","nnG"}[idx];
            PayTitle = new String[]{"qVK","ryg"}[idx];
        }
    }
    public static class MM_Res {
        public static int Finger_icon;

        private static void init(int idx) throws Throwable {
            Finger_icon=new int[]{2130838280,2130838289}[idx];
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
        }if(versionName.contains("6.5.10")){
            versionint=1;
            return 1;
        }
        return -1;
    }
}
