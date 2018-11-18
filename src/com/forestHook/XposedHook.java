package com.forestHook;

import java.util.Map;

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
            hookH5(lpparam);
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
						sleep(5*60*1000);
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
    
    //检测版本，由于直接调用，并未加载H5页面，H5Page的变量处理起来比较麻烦，所以采用直接hook的办法
    private void hookH5(XC_LoadPackage.LoadPackageParam lpparam) {
    	try {
    		Class<?> loadClass = lpparam.classLoader.loadClass("com.alipay.mobile.nebulabiz.rpc.H5AppRpcUpdate");
    		Class<?> h5PageClazz = lpparam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5Page");
    		XposedHelpers.findAndHookMethod(loadClass, "matchVersion", h5PageClazz, Map.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult(false);
                }
            });
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
    	
    }
}