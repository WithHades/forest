package cn.banny.unidbg.android;

import cn.banny.unidbg.LibraryResolver;
import cn.banny.unidbg.arm.ARMEmulator;
import cn.banny.unidbg.file.FileIO;
import cn.banny.unidbg.file.IOResolver;
import cn.banny.unidbg.linux.android.AndroidARMEmulator;
import cn.banny.unidbg.linux.android.AndroidResolver;
import cn.banny.unidbg.linux.android.dvm.*;
import cn.banny.unidbg.linux.android.dvm.array.ArrayObject;
import cn.banny.unidbg.linux.android.dvm.wrapper.DvmInteger;
import cn.banny.unidbg.linux.file.ByteArrayFileIO;
import cn.banny.unidbg.linux.file.SimpleFileIO;
import cn.banny.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Signso extends AbstractJni implements IOResolver {

    private static LibraryResolver createLibraryResolver() {
        return new AndroidResolver(19);
    }

    private static ARMEmulator createARMEmulator() {
        return new AndroidARMEmulator();
    }

    private ARMEmulator emulator;
    private VM vm;
    private DvmClass Native;
    private static final String APK_INSTALL_PATH = "/data/app/test.apk";
    private static final File APK_FILE = new File("src/test/resources/app/zhifubao.apk");

    public void initSign() throws IOException{
        emulator = createARMEmulator();
        emulator.getSyscallHandler().addIOResolver(this);
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(createLibraryResolver());
        memory.setCallInitFunction();
        vm = emulator.createDalvikVM(APK_FILE);
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary("sgmainso-6.4.11174435", false);
        dm.callJNI_OnLoad(emulator);
        Native = vm.resolveClass("com.taobao.wireless.security.adapter.JNICLibrary".replace(".", "/"));

        DvmObject context = vm.resolveClass("android/content/Context").newObject(null);
        Native.callStaticJniMethod(emulator, "doCommandNative(I[Ljava/lang/Object;)Ljava/lang/Object;",
                10101,
                new ArrayObject(context, DvmInteger.valueOf(vm, 3), new StringObject(vm, ""), new StringObject(vm, new File("target/app_SGLib").getAbsolutePath()), new StringObject(vm, ""))
        );
        vm.deleteLocalRefs();

        Native.callStaticJniMethod(emulator, "doCommandNative(I[Ljava/lang/Object;)Ljava/lang/Object;",
                10102,
                new ArrayObject(new StringObject(vm, "main"), new StringObject(vm, "6.4.11174435"), new StringObject(vm, "D:\\workplace\\unidbg\\src\\test\\resources\\example_binaries\\libsgmainso-6.4.11174435.so")));
        vm.deleteLocalRefs();
    }

    private void destroy() throws IOException {
        emulator.close();
        System.out.println("destroy");
    }

    public String doCommandNative(String str) {
        Number ret = Native.callStaticJniMethod(emulator, "doCommandNative(I[Ljava/lang/Object;)Ljava/lang/Object;",
                10401,
                new ArrayObject(new ArrayObject(new StringObject(vm, str)),
                        new StringObject(vm, "rpc-sdk-online"), DvmInteger.valueOf(vm, 0), new StringObject(vm, "")));
        long hash = ret.intValue() & 0xffffffffL;
        DvmObject dvmObject = vm.getObject(hash);
        vm.deleteLocalRefs();
        return dvmObject.toString();
    }

    @Override
    public DvmObject newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/alibaba/wireless/security/open/SecException-><init>(Ljava/lang/String;I)V": {
                StringObject msg = varArg.getObject(0);
                int value = varArg.getInt(1);
                return dvmClass.newObject(msg.getValue() + "[" + value + "]");
            }
            case "java/lang/Integer-><init>(I)V":
                int value = varArg.getInt(0);
                return DvmInteger.valueOf(vm, value);
        }

        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject callObjectMethod(BaseVM vm, DvmObject dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/HashMap->keySet()Ljava/util/Set;": {
                HashMap map = (HashMap) dvmObject.getValue();
                return vm.resolveClass("java/util/Set").newObject(map.keySet());
            }
            case "java/util/Set->toArray()[Ljava/lang/Object;":
                Set set = (Set) dvmObject.getValue();
                Object[] array = set.toArray();
                DvmObject[] objects = new DvmObject[array.length];
                for (int i = 0; i < array.length; i++) {
                    if(array[i] instanceof String) {
                        objects[i] = new StringObject(vm, (String) array[i]);
                    } else {
                        throw new IllegalStateException("array=" + array[i]);
                    }
                }
                return new ArrayObject(objects);
            case "java/util/HashMap->get(Ljava/lang/Object;)Ljava/lang/Object;": {
                HashMap map = (HashMap) dvmObject.getValue();
                Object key = varArg.getObject(0).getValue();
                Object obj = map.get(key);
                if(obj instanceof String) {
                    return new StringObject(vm, (String) obj);
                } else {
                    throw new IllegalStateException("array=" + obj);
                }
            }
            case "android/content/Context->getPackageCodePath()Ljava/lang/String;":
                return new StringObject(vm, APK_INSTALL_PATH);
            case "android/content/Context->getFilesDir()Ljava/io/File;":
                return vm.resolveClass("java/io/File").newObject(new File("target"));
            case "java/io/File->getAbsolutePath()Ljava/lang/String;":
                File file = (File) dvmObject.getValue();
                return new StringObject(vm, file.getAbsolutePath());
        }

        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public void callStaticVoidMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/taobao/dp/util/CallbackHelper->onCallBack(ILjava/lang/String;I)V":
                int i1 = varArg.getInt(0);
                StringObject str = varArg.getObject(1);
                int i2 = varArg.getInt(2);
                System.out.println("com/taobao/dp/util/CallbackHelper->onCallBack i1=" + i1 + ", str=" + str + ", i2=" + i2);
                return;
            case "com/alibaba/wireless/security/open/edgecomputing/ECMiscInfo->registerAppLifeCyCleCallBack()V":
                System.out.println("registerAppLifeCyCleCallBack");
                return;
        }

        super.callStaticVoidMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject getObjectField(BaseVM vm, DvmObject dvmObject, String signature) {
        switch (signature) {
            case "android/content/pm/ApplicationInfo->nativeLibraryDir:Ljava/lang/String;":
                return new StringObject(vm, new File("target").getAbsolutePath());
        }

        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public int callStaticIntMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/alibaba/wireless/security/framework/utils/UserTrackMethodJniBridge->utAvaiable()I":
                return 1;
            case "com/taobao/wireless/security/adapter/common/SPUtility2->saveToFileUnifiedForNative(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)I":
                StringObject a1 = varArg.getObject(0);
                StringObject a2 = varArg.getObject(1);
                StringObject a3 = varArg.getObject(2);
                boolean b4 = varArg.getInt(3) != 0;
                System.out.println("saveToFileUnifiedForNative a1=" + a1 + ", a2=" + a2 + ", a3=" + a3 + ", b4=" + b4);
        }

        return super.callStaticIntMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public FileIO resolve(File workDir, String pathname, int oflags) {
        if (pathname.equals(APK_INSTALL_PATH)) {
            return new SimpleFileIO(oflags, APK_FILE, pathname);
        }
        if (("/proc/self/status").equals(pathname)) {
            return new ByteArrayFileIO(oflags, pathname, "TracerPid:\t0\nState:\tr\n".getBytes());
        }
        if (("/proc/" + emulator.getPid() + "/stat").equals(pathname)) {
            return new ByteArrayFileIO(oflags, pathname, (emulator.getPid() + " (a.out) R 6723 6873 6723 34819 6873 8388608 77 0 0 0 41958 31 0 0 25 0 3 0 5882654 1409024 56 4294967295 134512640 134513720 3215579040 0 2097798 0 0 0 0 0 0 0 17 0 0 0\n").getBytes());
        }
        if (("/proc/" + emulator.getPid() + "/wchan").equals(pathname)) {
            return new ByteArrayFileIO(oflags, pathname, "sys_epoll".getBytes());
        }
        return null;
    }
}
