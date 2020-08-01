package vip.mimiya.helper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.PermissionListener;
import com.yhao.floatwindow.Screen;
import com.yhao.floatwindow.ViewStateListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ClipboardManager clipboardManager;
    private String lastPasteString;
    private EditText postEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initClipboard();
        bindRemoteService();
        startTarget();
    }

    private void startTarget() {
        ComponentName componetName = new ComponentName("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.main.MainActivity");

        try {
            Intent intent = new Intent();
            intent.setComponent(componetName);
            startActivity(intent);
        } catch (Exception e) {
        }
    }

    private void initClipboard() {
        clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        View floatLayout = inflater.inflate(R.layout.input_layout, null);

        postEditText = floatLayout.findViewById(R.id.post_editText);
        Button postButton = floatLayout.findViewById(R.id.post_button);

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FloatWindow.get().addFocus();
                postEditText.setEnabled(true);
                postEditText.setFocusable(true);
                postEditText.setFocusableInTouchMode(true);
                postEditText.requestFocus();
                postEditText.findFocus();
                postEditText.setText("");
                handler.sendEmptyMessage(101);
            }
        });
        postButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                lastPasteString = "";
                return false;
            }
        });

        FloatWindow.with(getApplicationContext()).setView(floatLayout)
                //.setWidth(150)                               //设置控件宽高
                .setHeight(Screen.width, 0.2f)
                .setX(0)                                   //设置控件初始位置
                .setY(Screen.height, 0.1f)
                .setDesktopShow(true)                        //桌面显示
                .setViewStateListener(mViewStateListener)    //监听悬浮控件状态改变
                .setPermissionListener(mPermissionListener)  //监听权限申请结果
                .build();
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    String result = (String) msg.obj;
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                    break;
                case 101:
                    getClipData();
                    break;
                default:
                    break;
            }
        }
    };

    private void getClipData() {
        try {
            ClipData clipData = clipboardManager.getPrimaryClip();

            String pasteString = "";
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).getText();
                pasteString = text.toString();
            }
            // if (!status) return;
            if (TextUtils.isEmpty(pasteString)) return;

            clipboardManager.setPrimaryClip(ClipData.newPlainText("", "" + System.currentTimeMillis()));

            if (!TextUtils.isEmpty(lastPasteString) && lastPasteString.equals(pasteString))
                return;


            lastPasteString = pasteString;

            String pattern = "(https://v.douyin.com).*/";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(pasteString);
            if (m.find()) {
                String shareUrl = m.group(0);
                try {
                    String result = iMyAidlInterface.postMessage(shareUrl);
                    if (result.equals("ok"))
                        Toast.makeText(MainActivity.this, "收到任务:[" + pasteString + "]", Toast.LENGTH_LONG).show();
                    else {
                        Toast.makeText(MainActivity.this, "POST ERROR!", Toast.LENGTH_SHORT).show();
                    }
                } catch (RemoteException e) {
                    Toast.makeText(MainActivity.this, "SERVICE ERROR!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "NO MATCH!", Toast.LENGTH_SHORT).show();
            }
        } finally {
            postEditText.setText("");
            postEditText.clearFocus();
            postEditText.setEnabled(false);
            FloatWindow.get().clearFocus();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private PermissionListener mPermissionListener = new PermissionListener() {
        @Override
        public void onSuccess() {
            Log.d(TAG, "mPermissionListener onSuccess");
        }

        @Override
        public void onFail() {
            Log.d(TAG, "mPermissionListener onFail");
        }
    };


    private ViewStateListener mViewStateListener = new ViewStateListener() {
        @Override
        public void onPositionUpdate(int x, int y) {
            Log.d(TAG, "onPositionUpdate: x=" + x + " y=" + y);
        }

        @Override
        public void onShow() {
            Log.d(TAG, "onShow");
        }

        @Override
        public void onHide() {
            Log.d(TAG, "onHide");
        }

        @Override
        public void onDismiss() {
            Log.d(TAG, "onDismiss");
        }

        @Override
        public void onMoveAnimStart() {
            Log.d(TAG, "onMoveAnimStart");
        }

        @Override
        public void onMoveAnimEnd() {
            Log.d(TAG, "onMoveAnimEnd");
        }

        @Override
        public void onBackToDesktop() {
            Log.d(TAG, "onBackToDesktop");
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private IMyAidlInterface iMyAidlInterface;// 定义接口变量
    private ServiceConnection connection;

    private void bindRemoteService() {
        Intent intentService = new Intent();
        intentService.setClassName(this, "vip.mimiya.helper.MyService");

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                iMyAidlInterface = IMyAidlInterface.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                // 断开连接
                iMyAidlInterface = null;
            }
        };

        bindService(intentService, connection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null)
            unbindService(connection);
    }
}
