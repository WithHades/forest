package com.forestHook;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import de.robv.android.xposed.XposedBridge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class AliMobileAutoCollectEnergyUtils {
    public static Object curH5Fragment;
    public static Activity h5Activity;
    private static String TAG = "alilogforest";
    private static Object curH5PageImpl;
    private static ArrayList<String> friendsRankUseridList = new ArrayList<String>();
    private static Integer totalEnergy = 0;
    private static Integer totalForfriendEnergy = 0;
    private static Integer pageCount = 0;
    private static Method rpcMethod = null;


    /**
     * 自动获取有能量的好友信息
     *
     * @param loader 加载器
     * @param response json
     */
    public static void autoGetCanCollectUserIdList(final ClassLoader loader) {
        //开始解析好友信息，循环把所有有能量的好友信息都解析完
    	boolean isSucc = true;
    	String response;
    	while(isSucc){
    		response = rpcCall_FriendRankList(loader);
    		isSucc = parseFrienRankPageDataResponse(response);
    	}
        if (friendsRankUseridList.size() > 0) {
        	XposedBridge.log("可操作好友共" + String.valueOf(friendsRankUseridList.size()) + "个");
        	for (String userId : friendsRankUseridList) {
        		response = rpcCall_CanCollectEnergy(loader, userId);
        		autoGetCanCollectBubbleIdList(loader,response);
        	}
        	if(totalEnergy > 0) XposedBridge.log("一共收取了" + totalEnergy + "g能量");
        	if(totalForfriendEnergy > 0) XposedBridge.log("一共帮好友收取了" + totalForfriendEnergy + "g能量");
        	friendsRankUseridList.clear();
            pageCount = 0;
            totalEnergy = 0;
            totalForfriendEnergy = 0;
        }
        else{
        	XposedBridge.log("暂无可操作好友");
        }
        XposedBridge.log("工作完毕");
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
                        	rpcCall_forFriendCollectEnergy(loader, jsonObject1.optString("userId"), jsonObject1.optInt("id"));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
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
                            XposedBridge.log("Find userId：" + userId + (optBoolean ? "  canCollectEnergy" : "") + (optBooleans ? "  canHelpCollect" : ""));
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
            //XposedBridge.log("FriendRankList_send:" + jsonArray.toString());
            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.queryEnergyRanking", jsonArray.toString(), "",
                    true, null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            response = (String) method.invoke(resp, new Object[]{});
            //XposedBridge.log("FriendRankList_recv:" + response);
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_FriendRankList err: " + Log.getStackTraceString(e));
            XposedBridge.log("rpcCall_FriendRankList_err: " + Log.getStackTraceString(e));
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
            json.put("userId", userId);
            json.put("version", "20180917");
            jsonArray.put(json);
            //XposedBridge.log("rpcCall_CanCollectEnergy_send:" + jsonArray.toString());
            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.queryNextAction", jsonArray.toString(),
                    "", true, null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            response = (String) method.invoke(resp, new Object[]{});
            //XposedBridge.log("rpcCall_CanCollectEnergy_recv:" + response);
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
            XposedBridge.log("rpcCall_CanCollectEnergy_err:" + Log.getStackTraceString(e));
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
            //XposedBridge.log("collectEnergy_send:" + jsonArray.toString());
            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.collectEnergy", jsonArray.toString()
                    , "", true, null, null, false, curH5PageImpl, 0, "", false, -1);
            Method method = resp.getClass().getMethod("getResponse");
            String response = (String) method.invoke(resp, new Object[]{});
            //XposedBridge.log("collectEnergy_recv:" + response);
            AliMobileAutoCollectEnergyUtils.parseCollectEnergyResponse(response);
        } catch (Exception e) {
            Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
            XposedBridge.log("collectEnergy_err: " + Log.getStackTraceString(e));
        }
    }

    /**
     * 收取即将消失的能量命令
     *
     * @param loader
     * @param userId
     * @param bubbleId
     */
    public static void rpcCall_forFriendCollectEnergy(ClassLoader loader, String userId, Integer bubbleId) {
        try {
        	JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("targetUserId", userId);
            jsonArray.put(json);
            //XposedBridge.log("forFriendCollectEnergy_send:" + jsonArray.toString());
            Object resp = rpcMethod.invoke(null, "alipay.antmember.forest.h5.forFriendCollectEnergy",
                    jsonArray.toString(), "", Boolean.TRUE, null, null, Boolean.FALSE, curH5PageImpl, 0, "",
                    Boolean.FALSE, -1);
            Method method = resp.getClass().getMethod("getResponse");
            String response = (String) method.invoke(resp, new Object[]{});
            AliMobileAutoCollectEnergyUtils.parseForfriendEnergyResponse(response);
            //XposedBridge.log("forFriendCollectEnergy_recv:" + response);
        } catch (Exception e) {
            Log.i(TAG, "forFriendCollectEnergy err: " + e.getMessage());
            XposedBridge.log("forFriendCollectEnergy_err: " + e.getMessage());
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
    
    public static boolean parseForfriendEnergyResponse(String response) {
    	try {
    		JSONObject jsonObject = new JSONObject(response);
    		JSONArray jsonArray = jsonObject.optJSONArray("bubbles");
    		for (int i = 0; i < jsonArray.length(); i++) {
    			totalForfriendEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy");
    		}
    		if ("SUCCESS".equals(jsonObject.optString("resultCode"))) {
    			return true;
    		}
    	} catch (Exception e) {
    	}
    	return false;
	}
}
