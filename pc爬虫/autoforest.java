package cn.banny.unidbg.android;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class autoforest {

    private static String miniwua;

    //the user cookie
    private static String cookie;
	
    //the version of product
    private static String x_mgs_productversion = "8f5";
	
    //Did is a deviceId
    private static String Did = "WSv";

    private static ArrayList<String> friendsRankUseridList = new ArrayList<String>();

    private static String name = "";

    private static int totalEnergy = 0;

    private static int totalForfriendEnergy = 0;

    private static int allTotalEnergy = 0;

    private static int allTotalForFriendEnergy = 0;

    private static String blackList = "";

    private final static TestSignso signso = new TestSignso();

    private static personal[] personals = new personal[2];

    private static SimpleDateFormat dateFormat= new SimpleDateFormat("MM-dd hh:mm:ss");;

    public static void main(String[] args) throws IOException {

        signso.initSign();
        personal p1 = new personal("","", "");
        personal p2 = new personal("","", "");
        personals[0] = p1;
        personals[1] = p2;
        work();

	}
    private static void work(){
        new Thread(){
            public void run(){
                while(true){
                    for(int i = 0; i < personals.length; i++){
                        if(personals[i] == null) continue;
                        personal per = personals[i];
                        name = per.name;
                        allTotalEnergy = per.allTotalEnergy;
                        allTotalForFriendEnergy = per.allTotalForFriendEnergy;
                        cookie = per.cookie;
                        miniwua = per.miniwua;

                        println("收取能量开始");

                        autoGetCanCollectUserIdList();

                        per.allTotalEnergy = allTotalEnergy;
                        per.allTotalForFriendEnergy = allTotalForFriendEnergy;

                        personals[i] = per;

                        int time = (int)(60 + Math.random() * (90 - 60 + 1));
                        println("sleep:" + time);
                        try {
                            Thread.sleep(time*1000);
                        } catch (InterruptedException e) {
                            println("work err:" + e.getMessage());
                        }
                    }
                }
            }
        }.start();
    }
    private static void autoGetCanCollectUserIdList() {

        //开始解析好友信息，循环把所有有能量的好友信息都解析完

        boolean isSucc = true;

        String response = "";

        int pageCount = 0;

        while(isSucc){

            response = getFriendRankList(pageCount);

            isSucc = parseFriendRankPageDataResponse(response);

            pageCount = pageCount + 1;

        }

        //获取自己的能量信息并收取
        response = canCollectEnergy(null);
        autoGetCanCollectBubbleIdList(response);

        if (friendsRankUseridList.size() > 0) {

            println("可操作好友共" + String.valueOf(friendsRankUseridList.size()) + "个");

            for (String userId : friendsRankUseridList) {

                response = canCollectEnergy(userId);

                autoGetCanCollectBubbleIdList(response);

            }

        }
        else{
            println("暂无可操作好友");
        }

        allTotalEnergy += totalEnergy;
        allTotalForFriendEnergy += totalForfriendEnergy;
        if(totalEnergy > 0) println("本次收取了" + totalEnergy + "g能量");
        if(allTotalEnergy > 0) println("一共收取了" + allTotalEnergy + "g能量");
        if(totalForfriendEnergy > 0) println("本次帮好友收取了" + totalForfriendEnergy + "g能量");
        if(allTotalForFriendEnergy > 0) println("一共帮好友收取了" + allTotalForFriendEnergy + "g能量");

        friendsRankUseridList.clear();
        totalEnergy = 0;
        totalForfriendEnergy = 0;
        println("工作完毕");
    }



    //获取好友列表
    private static String getFriendRankList(int pageCount) {

        try {

            JSONArray jsonArray = new JSONArray();
            JSONObject json = new JSONObject();
			json.put("av", "5");
	        json.put("ct", "android");
	        json.put("pageSize", 20);
	        json.put("startPoint", "" + (pageCount * 20 + 1));
	        jsonArray.put(json);
	        
	        String response = excuteHttp("alipay.antmember.forest.h5.queryEnergyRanking", jsonArray.toString());
	        
	        return response;
	        
		} catch (JSONException e) {
			println("FriendRankList_error:" + e.getMessage());
		}

		return "";
        
    }

    //解析好友列表信息
    private static boolean parseFriendRankPageDataResponse(String response) {

        try {
            if(response.equals("")) return false;

            JSONObject jsonObjects = new JSONObject(response);

            JSONArray optJSONArray = jsonObjects.optJSONArray("friendRanking");

            if (optJSONArray == null || optJSONArray.length() == 0) {

                return false;

            } else {

                for (int i = 0; i < optJSONArray.length(); i++) {

                    JSONObject jsonObject = optJSONArray.getJSONObject(i);

                    boolean optBoolean = jsonObject.optBoolean("canCollectEnergy");

                    boolean optBooleans = jsonObject.optBoolean("canHelpCollect");

                    String userId = jsonObject.optString("userId");

                    //屏蔽偷能量名单
                    if(blackList.contains(userId)) optBoolean = false;

                    if (optBoolean || optBooleans) {

                        if (!friendsRankUseridList.contains(userId)) {

                            friendsRankUseridList.add(userId);

                            println("Find userId：" + userId + (optBoolean ? "  canCollectEnergy" : "") + (optBooleans ? "  canHelpCollect" : ""));

                        }
                    }
                }
            }
            if(!jsonObjects.optBoolean("hasMore")) return false;

        } catch (Exception e) {
            println("parseFriendRankPageDataResponse error:" + e.getMessage());
        }

        return true;

    }

    //查询好友能量信息
    private static String canCollectEnergy(String userId) {
        try {
        	
            JSONArray jsonArray = new JSONArray();
            JSONObject json = new JSONObject();
            //json.put("canRobFlags", "T,T,T");
            if(userId != null) json.put("userId", userId);
            json.put("version", "20181220");
            jsonArray.put(json);
            
            String response = excuteHttp("alipay.antmember.forest.h5.queryNextAction", jsonArray.toString());

            return response;
            
        } catch (Exception e) {
        	println("canCollectEnergy_err:" + e.getMessage());
        }
        return "";
    }

    //解析好友能量信息
    public static void autoGetCanCollectBubbleIdList(String response) {

        if ((response != null) && response.contains("collectStatus")) {

            try {

                JSONObject jsonObject = new JSONObject(response);

                JSONArray usingUserProps = jsonObject.optJSONArray("usingUserProps");

                //说明有保护罩
                if(usingUserProps.length() != 0 && jsonObject.optString("nextAction").contains("Friend")) return;

                JSONArray jsonArray = jsonObject.optJSONArray("bubbles");

                if (jsonArray != null && jsonArray.length() > 0) {

                    for (int i = 0; i < jsonArray.length(); i++) {

                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);

                        if ("AVAILABLE".equals(jsonObject1.optString("collectStatus"))){
                            String res = collectEnergy(jsonObject1.optString("userId"), jsonObject1.optLong("id"));
                            parseCollectEnergyResponse(res);
                        }
                        if (jsonObject1.optBoolean("canHelpCollect")) {
                            String res = forFriendCollectEnergy(jsonObject1.optString("userId"), jsonObject1.optLong("id"));
                            parseForfriendEnergyResponse(res);
                        }
                    }
                }
            } catch (Exception e) {
                println("autoGetCanCollectBubbleIdList_err:" + e.getMessage());
            }
        }
    }

    //偷取好友能量
    private static String collectEnergy(String userId, Long bubbleId) {
        try {
        	
            JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("userId", userId);
            jsonArray.put(json);
            
            String response = excuteHttp("alipay.antmember.forest.h5.collectEnergy", jsonArray.toString());

            return response;
            
        } catch (Exception e) {
        	println("collectEnergy_err: " + e.getMessage());
        }
        return "";
    }

    //偷好友能量返回解析
    public static boolean parseCollectEnergyResponse(String response) {
        if ((response != null) && response.contains("failedBubbleIds")) {
            try {
                JSONObject jsonObject = new JSONObject(response);

                if (!"SUCCESS".equals(jsonObject.optString("resultCode"))) return true;

                JSONArray jsonArray = jsonObject.optJSONArray("bubbles");

                for (int i = 0; i < jsonArray.length(); i++) {
                    totalEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy");
                }

            } catch (Exception e) {
                println("parseCollectEnergyResponse_err: " + e.getMessage());
            }
        }
        return false;
    }

    //帮好友收能量
    private static String forFriendCollectEnergy(String userId, Long bubbleId) {
        try {

            JSONArray jsonArray = new JSONArray();
            JSONArray bubbleAry = new JSONArray();
            bubbleAry.put(bubbleId);
            JSONObject json = new JSONObject();
            json.put("bubbleIds", bubbleAry);
            json.put("targetUserId", userId);
            jsonArray.put(json);
            
            String response = excuteHttp("alipay.antmember.forest.h5.forFriendCollectEnergy", jsonArray.toString());

            return response;

        } catch (Exception e) {
        	println("forFriendCollectEnergy_err: " + e.getMessage());
        }
        return "";
    }

    //帮好友收能量返回解析
    public static boolean parseForfriendEnergyResponse(String response) {

        try {
            if ((response == null)) return false;
            JSONObject jsonObject = new JSONObject(response);

            if (!"SUCCESS".equals(jsonObject.optString("resultCode"))) return false;

            JSONArray jsonArray = jsonObject.optJSONArray("bubbles");

            for (int i = 0; i < jsonArray.length(); i++) {
                totalForfriendEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy");
            }

        } catch (Exception e) {
            println("parseForfriendEnergyResponse_err: " + e.getMessage());
        }

        return false;

    }
    
    //执行HTTP POST操作
    private static String excuteHttp(String operationType, String postData){
    	
    	String TAG = operationType.replaceAll("alipay.antmember.forest.h5.", "") ;
    	
    	println(TAG + "_send:" + postData);
    	
    	StringBuilder response = new StringBuilder();
    	
    	try {
    		
    		String ts = get64Time();
    		
    		String sign = getSign(operationType, postData, ts);

			URL url = new URL("https://mobilegw.alipay.com/mgw.htm");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");

			conn.setRequestProperty("visibleflag", "1");
			conn.setRequestProperty("AppId", "Android-container");
			conn.setRequestProperty("Version", "2");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("clientVersion", "10.1.75.7000");
			conn.setRequestProperty("Accept-Language", "zh-Hans");
			conn.setRequestProperty("Retryable2", "0*/");
			
			conn.setRequestProperty("miniwua", miniwua);
			conn.setRequestProperty("x-mgs-productversion", x_mgs_productversion);
			conn.setRequestProperty("Did", Did);
			conn.setRequestProperty("Operation-Type", operationType);
			conn.setRequestProperty("Ts", ts);
			conn.setRequestProperty("Sign", sign);
			conn.setRequestProperty("Cookie", cookie);
			
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			conn.connect();

            DataOutputStream dataout = new DataOutputStream(conn.getOutputStream());
            dataout.writeBytes(postData);
            dataout.flush();
            dataout.close();
            
            BufferedReader buffer = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            
            String line;

            while ((line = buffer.readLine()) != null) {
            	response.append(line);
            }

            if(response.toString().equals("")){
                Map<String, List<String>> Properties = conn.getHeaderFields();
                println("http error:" + URLDecoder.decode(Properties.get("Tips").toString()));
            }

            /*
            Map<String, List<String>> Properties = conn.getHeaderFields();
			for (Map.Entry<String, List<String>> entry : Properties.entrySet())
				System.out.println(URLDecoder.decode(entry.toString()));
            */
            buffer.close();
            conn.disconnect();
		
		} catch (Exception e) {
			println("http error:" + e.getMessage());
		}
    	
    	println(TAG + "_recv:" + response.toString());
    	
    	return response.toString();
    }
    
    //获取签名值
    private static String getSign(String operationType, String postData, String ts){
    	
    	Base64.Encoder encoder = Base64.getEncoder();
    	
    	StringBuffer signData = new StringBuffer("Operation-Type=");
    	signData.append(operationType);
    	
    	signData.append("&Request-Data=");
    	signData.append(encoder.encodeToString(postData.getBytes()));
    	
    	signData.append("&Ts=");
    	signData.append(ts);

    	String sign = signso.test(signData.toString());

        sign = sign.replaceAll("StringObject\\{value=","");
        sign = sign.replaceAll("}","");

    	return sign;
    
    }
    //获取64进制的时间戳
    private static String get64Time(){
    	long ts = System.currentTimeMillis();
    	return c10to64(ts);
    }
    
	//特殊的十进制到64进制
	private static final String c10to64(long j) {
		char[] a = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '+', '/'};
        int pow = (int) Math.pow(2.0d, 6.0d);
        char[] cArr = new char[pow];
        int i = pow;
        do {
            i--;
            cArr[i] = a[(int) (63 & j)];
            j >>>= 6;
        } while (j != 0);
        return new String(cArr, i, pow - i);
    }

    private static void println(String msg){
        Date date = new Date();
        String time = dateFormat.format(date);
        System.out.println(time + "  " + name + ": " + msg);
    }
}
