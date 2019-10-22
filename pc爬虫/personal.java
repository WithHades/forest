package cn.banny.unidbg.android;

public class personal {
     
    //标志
    String name;
    //授权信息
    String cookie;
    //设备信息
    String miniwua;
    //运行期间总共偷取的能量
    int allTotalEnergy;
    //运行期间总共帮好友收取的能量
    int allTotalForFriendEnergy;

    personal(String name, String cookie,String miniwua){
        this.name = name;
        this.cookie = cookie;
        this.miniwua = miniwua;
        this.allTotalEnergy = 0;
        this.allTotalForFriendEnergy = 0;
    }

}
