package com.forestHook;

import android.app.Activity;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedHook implements IXposedHookLoadPackage {

    private static String TAG = "XposedHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        Log.i(TAG, lpparam.packageName);
        if ("com.eg.android.AlipayGphone".equals(lpparam.packageName)) {
            hookSecurity(lpparam);
            hookForest(lpparam);
        }
    }

    private void hookForest(final XC_LoadPackage.LoadPackageParam lpparam) {
    	new Thread(){
    		public void run(){
    			while(true){
    				XposedBridge.log("收取能量开始");
        			AliMobileAutoCollectEnergyUtils.getRpcCallMethod(lpparam.classLoader);
        			AliMobileAutoCollectEnergyUtils.autoGetCanCollectUserIdList(lpparam.classLoader);
        			try {
						sleep(10*60*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
    			}
    		}
    	}.start();
    }
    
    private void hookSecurity(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> loadClass = lpparam.classLoader.loadClass("android.util.Base64");
            if (loadClass != null) {
                XposedHelpers.findAndHookMethod(loadClass, "decode", String.class, Integer.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                    }
                });
            }
            loadClass = lpparam.classLoader.loadClass("android.app.Dialog");
            if (loadClass != null) {
                XposedHelpers.findAndHookMethod(loadClass, "show", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        try {
                            throw new NullPointerException();
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
            loadClass = lpparam.classLoader.loadClass("com.alipay.mobile.base.security.CI");
            if (loadClass != null) {
                XposedHelpers.findAndHookMethod(loadClass, "a", loadClass, Activity.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return null;
                    }
                });
                XposedHelpers.findAndHookMethod(loadClass, "a", String.class, String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        param.setResult(null);
                    }
                });
            }
        } catch (Throwable e) {
            Log.i(TAG, "hookSecurity err:" + Log.getStackTraceString(e));
        }
    }
}