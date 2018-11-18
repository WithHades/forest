package com.forestHook;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import de.robv.android.xposed.XposedBridge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author congyang.guo
 */
public class AliMobileAutoCollectEnergyUtils {
    public static Object curH5Fragment;
    public static Activity h5Activity;
    private static String TAG = "alilogforest";
    private static Object curH5PageImpl;
    private static ArrayList<String> friendsRankUseridList = new ArrayList<String>();
    private static Integer totalEnergy = 0;
    private static Integer pageCount = 0;
    private static Method rpcMethod = null;


    /**
     * 自动获取有能量的好友信息
     *
     * @param loader 加载器
     * @param response json
     */
    public static void autoGetCanCollectUserIdList(final ClassLoader loader) {
    	friendsRankUseridList.add("1");//这个是自己的
        // 开始解析好友信息，循环把所有有能量的好友信息都解析完
    	boolean isSucc = true;
    	String response;
    	while(isSucc){
    		response = rpcCall_FriendRankList(loader);
    		isSucc = parseFrienRankPageDataResponse(response);
    	}
        if (friendsRankUseridList.size() > 0) {
                showToast("开始获取每个好友能够偷取的能量信息...");
                for (String userId : friendsRankUseridList) {
                    // 开始收取每个用户的能量
                    response = rpcCall_CanCollectEnergy(loader, userId);
                    autoGetCanCollectBubbleIdList(loader,response);
                }
            // 执行完了调用刷新页面，看看总能量效果
            finishWork();   
            friendsRankUseridList.clear();
            pageCount = 0;
            totalEnergy = 0;
        }
        
    }

    /**
     * 自动获取能收取的能量ID
     *
     * @param loader 加载器
     * @param response json
     */

    public static void autoGetCanCollectBubbleIdList(ClassLoader loader, String response) {
        if (!TextUtils.isEmpty(response) && response.contains("collectStatus")) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.optJSONArray("bubbles");
                if (jsonArray != null && jsonArray.length() > 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        if ("AVAILABLE".equals(jsonObject1.optString("collectStatus"))) {
                            rpcCall_CollectEnergy(loader, jsonObject1.optString("userId"), jsonObject1.optInt("id"));
                        }
                        if (jsonObject1.optBoolean("canHelpCollect")) {
                            forFriendCollectEnergy(loader, jsonObject1.optString("userId"), jsonObject1.optInt("id"));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }


    public static boolean isUserDetail(String response) {
        return !TextUtils.isEmpty(response) && response.contains("userEnergy");
    }


    /**
     * 结束工作
     */
    private static void finishWork() {
        // 打印收取了多少能量
    	XposedBridge.log("一共收取了" + totalEnergy + "g能量");
    }

    /**
     * 解析好友信息
     *
     * @param response json
     * @return
     */
    private static boolean parseFrienRankPageDataResponse(String response) {
        try {
            JSONArray optJSONArray = new JSONObject(response).optJSONArray("friendRanking");
            if (optJSONArray == null || optJSONArray.length() == 0) {
                return false;
            } else {
                for (int i = 0; i < optJSONArray.length(); i++) {
                    JSONObject jsonObject = optJSONArray.getJSONObject(i);
                    boolean optBoolean = jsonObject.optBoolean("canCollectEnergy");
                    boolean optBooleans = jsonObject.optBoolean("canHelpCollect");
                    String userId = jsonObject.optString("userId");
                    if (optBoolean || optBooleans) {
                        if (!friendsRankUseridList.contains(userId)) {
                            friendsRankUseridList.add(userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

    /**
     * 获取分页好友信息命令
     *
     * @param loader
     */
    private static String rpcCall_FriendRankList(final ClassLoader loader) {
    	String response = null;
        try {
            JSONArray jsonArray = new JSONArray();
            JSONObject json = new JSONObject();
            json.put("av", "5");
            json.put("ct", "android");
            json.put("pageSize", 20);
            json.put("startPoint", "" + (pageCount * 20 + 1));
            pageCount++;
            jsonArray.put(json);
            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.queryEnergyRanking", jsonArray.toString(), "",
                    true, null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            response = (String) method.invoke(resp, new Object[]{});
            XposedBridge.log("FriendRankList response：" + response);
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_FriendRankList err: " + Log.getStackTraceString(e));
        }
        return response;
    }

    /**
     * 获取指定用户可以收取的能量信息
     *
     * @param loader
     * @param userId
     */
    private static String rpcCall_CanCollectEnergy(final ClassLoader loader, String userId) {
        String response = null;
    	try {
            JSONArray jsonArray = new JSONArray();
            JSONObject json = new JSONObject();
            if(userId!="1") json.put("userId", userId);
            json.put("version", "20180917");
            jsonArray.put(json);
            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.queryNextAction", jsonArray.toString(), "", true,
                    null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            response = (String) method.invoke(resp, new Object[]{});
            XposedBridge.log("queryNextAction_" + userId+":" + response);
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
        }
        return response;
    }

    /**
     * 收取能量命令
     *
     * @param loader
     * @param userId
     * @param bubbleId
     */
    public static void rpcCall_CollectEnergy(final ClassLoader loader, String userId, Integer bubbleId) {
        try {
            JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("userId", userId);
            jsonArray.put(json);

            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.collectEnergy", jsonArray.toString()
                    , "", true, null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            String response = (String) method.invoke(resp, new Object[]{});
            AliMobileAutoCollectEnergyUtils.parseCollectEnergyResponse(response);
            XposedBridge.log("collectEnergy_" + userId+":" + response);
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
        }
    }

    public static Method getRpcCallMethod(ClassLoader loader) {
    	Class<?> H5RpcUtilClazz = null, h5PageClazz=null, jsonClazz=null;
		try {
			H5RpcUtilClazz = loader.loadClass("com.alipay.mobile.nebulabiz.rpc.H5RpcUtil");
			h5PageClazz = loader.loadClass("com.alipay.mobile.h5container.api.H5Page");
			jsonClazz = loader.loadClass("com.alibaba.fastjson.JSONObject");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		
		try {
			rpcMethod = H5RpcUtilClazz.getMethod("rpcCall", String.class, String.class, String.class,
                    boolean.class, jsonClazz, String.class, boolean.class, h5PageClazz,
                    int.class, String.class, boolean.class, int.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
        return null;
    }

    public static boolean parseCollectEnergyResponse(String response) {
        if (!TextUtils.isEmpty(response) && response.contains("failedBubbleIds")) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.optJSONArray("bubbles");
                for (int i = 0; i < jsonArray.length(); i++) {
                    totalEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy");
                }
                if ("SUCCESS".equals(jsonObject.optString("resultCode"))) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    private static void showToast(final String str) {
        if (h5Activity != null) {
            try {
                h5Activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(h5Activity, str, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "showToast err: " + Log.getStackTraceString(e));
            }
        }
    }


    public static void forFriendCollectEnergy(ClassLoader loader, String userId, Integer bubbleId) {
        try {
            JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("targetUserId", userId);
            jsonArray.put(json);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("call cancollect energy params:");
            stringBuilder.append(jsonArray);
            rpcMethod.invoke(null, "alipay.antmember.forest.h5.forFriendCollectEnergy",
                    jsonArray.toString(), "", Boolean.TRUE, null, null, Boolean.FALSE, curH5PageImpl, 0, "",
                    Boolean.FALSE, -1);
        } catch (Exception e) {
            Log.i(TAG, "forFriendCollectEnergy err: " + e.getMessage());
        }
    }

}
