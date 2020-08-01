package vip.mimiya.helper;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyService extends Service {
    private static final String TAG = "MyService";
    private CustomHanlder ch;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: MyService");
        HandlerThread ht = new HandlerThread("handler.thread.name");
        ht.start();
        ch = new CustomHanlder(ht.getLooper());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("service started:", "flag");
        Log.w("main thread id:", "" + Thread.currentThread().getName() + Thread.currentThread().getId());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return stub;// 在客户端连接服务端时，Stub通过ServiceConnection传递到客户端
    }

    // 实现接口中暴露给客户端的Stub--Stub继承自Binder，它实现了IBinder接口
    private IMyAidlInterface.Stub stub = new IMyAidlInterface.Stub() {

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public String postMessage(String context) throws RemoteException {
            Message msg = ch.obtainMessage();
            msg.what = 100;
            msg.obj = context;
            ch.sendMessage(msg);
            return "ok";
        }
    };

    private class CustomHanlder extends Handler {
        public CustomHanlder(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.w("handler thread id:", "" + Thread.currentThread().getName() + Thread.currentThread().getId());
            switch (msg.what) {
                case 100:
                    doTask((String) msg.obj);
                    break;
                case 102:
                    Toast.makeText(getApplicationContext(), "后台处理-提交完成!", Toast.LENGTH_SHORT).show();
                    break;
                case 103:
                    Toast.makeText(getApplicationContext(), "后台处理-视频原始地址处理出错!", Toast.LENGTH_SHORT).show();
                    break;
                case 104:
                    Toast.makeText(getApplicationContext(), "后台处理-提交出错!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }


    void doTask(String context) {
        String video = getVideoRawUrl(context);
        if (video != null) {
            postMyVideoRecord(video);
        }
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private void postMyVideoRecord(String video) {
        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)//设置连接超时时间
                .readTimeout(30, TimeUnit.SECONDS)//设置读取超时时间
                .build();

        RequestBody body = RequestBody.create(video, JSON);
        Request request = new Request.Builder()
                .url("http://47.98.199.11:5008/do/video_upload")
                .header("token", Utils.token)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String result = response.body().string();
            ch.sendEmptyMessage(102);
        } catch (IOException e) {
            ch.sendEmptyMessage(104);
        }
    }

    private String getVideoRawUrl(String url) {

        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)//设置连接超时时间
                .readTimeout(30, TimeUnit.SECONDS)//设置读取超时时间
                .build();
        String json = String.format("{\"share_url\":\"%s\"}", url);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url("http://47.98.199.11:5008/do/video_raw_url")
                .header("token", Utils.token)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String result = response.body().string();
            JSONObject resultObj = new JSONObject(result);
            JSONObject resultData = resultObj.getJSONObject("data");
            resultData.put("phone", Utils.phone);
            System.out.println(result);
            return resultData.toString();
        } catch (IOException | JSONException e) {
            ch.sendEmptyMessage(103);
        }
        return null;

    }

    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //逻辑代码
        }
    }
}