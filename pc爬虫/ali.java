package com.ali;

import android.app.Activity;
import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ali {
	private volatile static boolean hooked = false;

    public static void aliHook(final XC_LoadPackage.LoadPackageParam loadPackageParam){
    	
    	if (!loadPackageParam.packageName.contains("com.eg.android.AlipayGphone")) {
    		return;
    	}
    	
    	if(hooked) return;
    	hooked = true;
    	
		XposedBridge.log("Loaded app: " + loadPackageParam.packageName);
		//hook支付宝对xposed的检查
		hookSecurity(loadPackageParam);
		//hook日志函数
		hooklog(loadPackageParam);
		//hook调用so库的参数
		hookTh(loadPackageParam);
		
	}

    private static void hookTh(final XC_LoadPackage.LoadPackageParam lpparam) {
    	new Thread(){
    		public void run(){
    			XposedHelpers.findAndHookMethod(ClassLoader.class, 
    					"loadClass",
    					String.class, 
    					new XC_MethodHook() {
    				@Override
    				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    					super.afterHookedMethod(param);
    					
    					if (param.hasThrowable()) return;
    					
    					if(param.args[0].toString().contains("java")) return;
    					
    					Class<?> cl = (Class<?>)param.getResult();
    					String clsname = cl.getName();
    					
    					if(clsname.contains("com.taobao.wireless.security.adapter.JNICLibrary")) doCommandNative(cl);
    					
    					
    				}
    			});
    		}
    	}.start();

    }
    public static void doCommandNative(Class<?> cls) {
		XposedHelpers.findAndHookMethod(cls, 
				"doCommandNative", 
				int.class, 
				Object[].class, 
				new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("signature_0:" + param.args[0].toString());
				
				if(param.args[0].toString().contains("10101")) {
					Object[] objs = (Object[])param.args[1];
					for(int i = 0; i < objs.length; i++) {
						Object obj = objs[i];
						if(obj != null) XposedBridge.log("10101_" + String.valueOf(i) + ":" + obj.toString());
					}
				}
				
				if(param.args[0].toString().contains("10102")) {
					Object[] objs = (Object[])param.args[1];
					for(int i = 0; i < objs.length; i++) {
						Object obj = objs[i];
						if(obj != null) XposedBridge.log("10102_" + String.valueOf(i) + ":" + obj.toString());
					}
				}
				
				if(!param.args[0].toString().contains("10401")) return;
				Object[] obj = (Object[])param.args[1];
				
				String[] strarr = (String[])obj[0];
				for(String str : strarr) {
					XposedBridge.log("signature_1[0]:" + str);
				}
				
				if(obj[1] != null) XposedBridge.log("signature_1[1]:" + obj[1].toString());
				if(obj[2] != null) XposedBridge.log("signature_1[2]:" + obj[2].toString());
				if(obj[3] != null) XposedBridge.log("signature_1[3]:" + obj[3].toString());
				if(obj[0] == null) return;

			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				
				if(param.args[0].toString().contains("10101")) XposedBridge.log("10101:" + param.getResult().toString());
				
				if(!param.args[0].toString().contains("10401")) return;
				
				XposedBridge.log("signature:" + param.getResult().toString());
				
			}
		});
    }
    
    private static void hooklog(XC_LoadPackage.LoadPackageParam lpparam) {

        try {
            Class<?> LogCatUtil = lpparam.classLoader.loadClass("com.alipay.mobile.common.transport.utils.LogCatUtil");
            if (LogCatUtil != null) {
                XposedHelpers.findAndHookMethod(LogCatUtil, "info", String.class, String.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                    	if(param.args[1].toString().contains("printHeaderLog")) XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                        return null;
                    }
                });
            }
        } catch (Throwable e) {
        }
    }
    
    private static void hookSecurity(XC_LoadPackage.LoadPackageParam lpparam) {


        try {
            Class<?> loadClass = lpparam.classLoader.loadClass("com.alipay.mobile.base.security.CI");
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
        }
    }

}
    	