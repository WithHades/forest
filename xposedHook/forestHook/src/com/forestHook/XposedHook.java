package com.forestHook;

import java.lang.reflect.Method;
import java.util.Map;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedHook implements IXposedHookLoadPackage {

    private static String TAG = "XposedHook";

    private static boolean isHook = false;
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException {
        
        if(!(lpparam.packageName.contains("com.eg.android.AlipayGphone"))) return;

        if(isHook) return;
        
        isHook = true;
        
        XposedBridge.log("load: " + lpparam.packageName);
        
        //过掉支付宝检测xposed
        hookSecurity(lpparam);
        
        //过掉H5检测
        hookH5(lpparam);
        
        //开始工作
        hookForest(lpparam);
        
        //测试代码开关
        if(TAG == "XposedHook") return;

        hookTest(lpparam);
        
    }

    private void hookForest(final XC_LoadPackage.LoadPackageParam lpparam) {
    	new Thread(){
    		public void run(){
    			while(true){
        			try {
        				sleep(5*1000);
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
        			XposedBridge.log("收取能量开始");
            		AliMobileAutoCollectEnergyUtils.getRpcCallMethod(lpparam.classLoader);
            		AliMobileAutoCollectEnergyUtils.autoGetCanCollectUserIdList(lpparam.classLoader);
    			}
    		}
    	}.start();
    }
    
    private void hookSecurity(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> loadClass = lpparam.classLoader.loadClass("com.alipay.mobile.base.security.CI");
            if (loadClass != null) {
                XposedHelpers.findAndHookMethod(loadClass, "a", String.class, String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        param.setResult(null);
                    }
                });
            }
        } catch (Throwable e) {
        	XposedBridge.log("hookSecurity_err: " + e.getMessage());
        }
    }
    
    //检测版本，rpcCall会调用该函数，由于我们并未加载H5页面直接调用rpcCall，因此会引发异常，所以直接hook掉该函数
    private void hookH5(XC_LoadPackage.LoadPackageParam lpparam) {
    	try {
    		Class<?> loadClass = lpparam.classLoader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5AppRpcUpdate");
    		Class<?> h5PageClazz = lpparam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5Page");
    		XposedHelpers.findAndHookMethod(loadClass, "matchVersion", h5PageClazz, Map.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult(false);
                }
    		});
		} catch (ClassNotFoundException e) {
			XposedBridge.log("hook_h5_err: " + e.getMessage());
		}
    	
    }
    
    //test
    private void hookTest(XC_LoadPackage.LoadPackageParam lpparam) {
    	try {
    		//hook发送函数查看数据
            Class<?> H5RpcUtilClazz = lpparam.classLoader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil");
            Class<?> h5PageClazz = lpparam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5Page");
            final Class<?> jsonClazz = lpparam.classLoader.loadClass("com.alibaba.fastjson.JSONObject");
            
            XposedHelpers.findAndHookMethod(H5RpcUtilClazz, "rpcCall", String.class, String.class, String.class,
                    boolean.class, jsonClazz, String.class, boolean.class, h5PageClazz,
                    int.class, String.class, boolean.class, int.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	if(param.args[0]!=null) XposedBridge.log("rpc_0: " + param.args[0].toString());
                	if(param.args[1]!=null) XposedBridge.log("rpc_1: " + param.args[1].toString());
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                	Object resp = param.getResult();
                	Method getResponse = resp.getClass().getMethod("getResponse");
                	String response = (String) getResponse.invoke(resp);
                	XposedBridge.log("rpc_return: " + response);
                }
    		});
            
            //hook日志
            Class<?> LogCatUtil = lpparam.classLoader.loadClass("com.alipay.mobile.common.transport.utils.LogCatUtil");
            XposedHelpers.findAndHookMethod(LogCatUtil, "debug", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(LogCatUtil, "debugOrLose", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(LogCatUtil, "info", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(LogCatUtil, "printInfo", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(LogCatUtil, "verbose", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(LogCatUtil, "warn", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            
            Class<?> H5Log = lpparam.classLoader.loadClass("com.alipay.mobile.nebula.util.H5Log");
            XposedHelpers.findAndHookMethod(H5Log, "d", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(H5Log, "e", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(H5Log, "w", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(H5Log, "d", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	if(param.args[0]!=null) XposedBridge.log("d_0: " + param.args[0].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(H5Log, "e", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	if(param.args[0]!=null) XposedBridge.log("e_0: " + param.args[0].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(H5Log, "w", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	if(param.args[0]!=null) XposedBridge.log("w_0: " + param.args[0].toString());
                }
    		});
            XposedHelpers.findAndHookMethod(H5Log, "debug", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                	XposedBridge.log(param.args[0].toString() + ":" + param.args[1].toString());
                }
    		});
		} catch (ClassNotFoundException e) {
			XposedBridge.log("hook_test_err: " + Log.getStackTraceString(e));
		}
    	
    }
}