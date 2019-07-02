package com.secure.vpnclient.demo;

import java.util.Properties;

import org.json.JSONObject;

import com.secure.comm.utils.SPFileUtil;
import com.secure.comm.utils.SPJSONUtil;
import com.secure.comm.view.SPPopupInputBox;
import com.secure.comm.view.SPPopupMsgBox;
import com.secure.comm.view.SPPopupWaiting;
import com.secure.libsportal.sdk.demo.R;
import com.secure.sportal.sdk.LibSecIDSDKLite;
import com.secure.sportal.sdk.SPVPNClient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * 假定如下<br>
 * VPN 服务器地址是 172.16.3.83 端口 443<br>
 * 登录 VPN 的帐号是 test 密码是 123456<br>
 * 受保护的应用服务器内网地址是 192.168.88.88 端口是 80<br>
 * <br>
 * 需要实现的步骤如下<br>
 * 1、登录VPN<br>
 * 2、用 Socket / HttpURLConnection / WebView 访问 192.168.88.88:80<br>
 * 
 * <code>
 *   +------------+             +------------------+         +------------------+
 *   |    APP     |  <==SSL==>  |    VPN 服务器    |  <====> |   APP 服务器     |
 *   | 移动客户端 |             | 172.16.1.233:443 |         | 192.168.88.88:80 |
 *   +------------+             +------------------+         +------------------+
 * </code>
 * 
 */
public class VPNClientDemoActivity extends Activity implements View.OnClickListener, SPVPNClient.OnVPNLoginCallback
{
    private int[]                  mBtnIDs;
    private View[]                 mDemoBtns;
    private SPPopupWaiting         mWaitingDlg;

    private VPNTunnelStateReceiver tunnelStateReceiver;
    private SessionStatusReceiver  sessionStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tunnelStateReceiver = new VPNTunnelStateReceiver();
        sessionStatusReceiver = new SessionStatusReceiver();

        setContentView(R.layout.activity_demo);
        findViewById(R.id.demo_btn_login).setOnClickListener(this);
        findViewById(R.id.demo_btn_logout).setOnClickListener(this);

        mBtnIDs = new int[] { R.id.demo_btn_http_proxy, R.id.demo_btn_http_hook, R.id.demo_btn_http_auto,
                R.id.demo_btn_webview, R.id.demo_btn_tcp_api, R.id.demo_btn_tcp_socksproxy, R.id.demo_btn_tcp_httpproxy,
                R.id.demo_btn_tcp_hook, R.id.demo_btn_tcp_maploop };
        mDemoBtns = new Button[mBtnIDs.length];
        for (int i = 0; i < mBtnIDs.length; i++)
        {
            mDemoBtns[i] = findViewById(mBtnIDs[i]);
            mDemoBtns[i].setOnClickListener(this);
        }
        enableButtons(false);
        mWaitingDlg = SPPopupWaiting.create(this, "", null, null, "取消", null);

        // Android 6.0 以后的平台需要增加权限检查功能
        SPFileUtil.verifyStoragePermissions(this);
    }

    private void loginFirstStep()
    {
        Properties param = new Properties();
        // SSLVPN 服务器地址，支持主机名和IP地址
        param.setProperty(SPVPNClient.PARAM_VPN_HOST, "172.16.3.83");

        // SSLVPN 服务器端口号
        param.setProperty(SPVPNClient.PARAM_VPN_PORT, "443");

        // SSLVPN 认证服务器名称（如果不填，则SDK自动使用第一个作为默认值）
        param.setProperty(SPVPNClient.PARAM_AUTH_SERVER, "本地认证");

        // SSLVPN 登录用户名
        param.setProperty(SPVPNClient.PARAM_AUTH_USERNAME, "kp");

        // SSLVPN 登录密码
        param.setProperty(SPVPNClient.PARAM_AUTH_PASSWORD, "kkkkkk");

        SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
        mWaitingDlg.setMessage("第一步认证中");
        mWaitingDlg.dialog().show();
    }

    // 用360ID口令进行第二步验证
    private void loginSecondaryWith360IDToken(String username, String token)
    {
        Properties param = new Properties();
        param.setProperty(SPVPNClient.PARAM_AUTH_USERNAME, username);
        param.setProperty(SPVPNClient.PARAM_VERIFY_CODE, token);
        SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
        mWaitingDlg.setMessage("第二步认证中");
        mWaitingDlg.dialog().show();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.demo_btn_login:
                loginFirstStep();
                break;
            case R.id.demo_btn_logout:
                SPVPNClient.logout();
                enableButtons(false);
                break;
            case R.id.demo_btn_webview:
                Intent intent = new Intent(this, DemoWebViewActivity.class);
                intent.putExtra("service_url", "http://192.168.88.88");
                startActivity(intent);
                break;
            case R.id.demo_btn_http_proxy:
                DemoHttpClient.getPage("http://192.168.88.88/", "proxy");
                break;
            case R.id.demo_btn_http_hook:
                DemoHttpClient.getPage("http://192.168.88.88/", "hook");
                break;
            case R.id.demo_btn_http_auto:
                DemoHttpClient.getPage("http://192.168.88.88/", "auto");
                break;
            case R.id.demo_btn_tcp_api:
                DemoTcpClient.connect("192.168.88.88", 80, "api");
                break;
            case R.id.demo_btn_tcp_socksproxy:
                DemoTcpClient.connect("192.168.88.88", 80, "socks_proxy");
                break;
            case R.id.demo_btn_tcp_httpproxy:
                DemoTcpClient.connect("192.168.88.88", 80, "http_proxy");
                break;
            case R.id.demo_btn_tcp_hook:
                DemoTcpClient.connect("192.168.88.88", 80, "hook");
                break;
            case R.id.demo_btn_tcp_maploop:
                DemoTcpClient.connect("192.168.88.88", 80, "map_loopback");
                break;
            default:
                break;
        }
    }

    @Override
    public void onVPNLoginMessage(int msgid, String msg)
    {
        mWaitingDlg.close(0);
        switch (msgid)
        {
            case SPVPNClient.MSGID_LOGIN_SUCC:
                establishVPNTunnel();
                break;
            case SPVPNClient.MSGID_LOGIN_NEED_SECID_TOKEN:
                // 需要动态口令，可以用户直接手动输入，如果是 360ID 口令，还可以调用 SDK 直接获取
                // 方式一，提示用户输入动态口令
                SPPopupInputBox.inputBox(this, "请输入动态口令", "", new SPPopupInputBox.SPInputBoxCallback()
                {
                    @Override
                    public void OnInputBoxText(String text, boolean remember)
                    {
                        if (!TextUtils.isEmpty(text))
                        {
                            // 用接收到的验证码进行第二步验证，这里还是调用 SPVPNClient.login() 方法，
                            // 参数只需要一个 SPVPNClient.PARAM_VERIFY_CODE
                            Properties param = new Properties();
                            param.setProperty(SPVPNClient.PARAM_VERIFY_CODE, text);
                            SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
                        }
                    }
                });
                // 方式二，调用360ID获取动态口令，通过onActivityResult()获取结果
                LibSecIDSDKLite.generateToken(this, "", 443, "");
                break;
            case SPVPNClient.MSGID_LOGIN_NEED_PHONE_NUM:
                // 需要接收短信验证码的手机号码
                SPPopupInputBox.inputBox(this, "请输入接收短信验证码的手机号码", "", new SPPopupInputBox.SPInputBoxCallback()
                {
                    @Override
                    public void OnInputBoxText(String text, boolean remember)
                    {
                        if (!TextUtils.isEmpty(text))
                        {
                            // 将接收短信验证码的手机号码通知VPN，这里还是调用 SPVPNClient.login() 方法，
                            // 参数只需要一个 SPVPNClient.PARAM_VCODE_TARGET
                            Properties param = new Properties();
                            param.setProperty(SPVPNClient.PARAM_VCODE_TARGET, text);
                            SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
                        }
                    }
                });
                break;
            case SPVPNClient.MSGID_LOGIN_NEED_EMAIL_ADDR:
                // 需要接收验证码的电子邮件地址
                SPPopupInputBox.inputBox(this, "请输入接收验证码的电子邮件地址", "", new SPPopupInputBox.SPInputBoxCallback()
                {
                    @Override
                    public void OnInputBoxText(String text, boolean remember)
                    {
                        if (!TextUtils.isEmpty(text))
                        {
                            // 将接收验证码的电子邮件地址通知VPN，这里还是调用 SPVPNClient.login() 方法，
                            // 参数只需要一个 SPVPNClient.PARAM_VCODE_TARGET
                            Properties param = new Properties();
                            param.setProperty(SPVPNClient.PARAM_VCODE_TARGET, text);
                            SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
                        }
                    }
                });
                break;
            case SPVPNClient.MSGID_LOGIN_NEED_VCCODE_SMS:
                // 需要短信验证码，这时 msg 是接收短信的手机号
                SPPopupInputBox.inputBox(this, "请输入[" + msg + "]接收到的短信验证码", "", new SPPopupInputBox.SPInputBoxCallback()
                {
                    @Override
                    public void OnInputBoxText(String text, boolean remember)
                    {
                        if (!TextUtils.isEmpty(text))
                        {
                            // 用接收到的验证码进行第二步验证，这里还是调用 SPVPNClient.login() 方法，
                            // 参数只需要一个 SPVPNClient.PARAM_VERIFY_CODE
                            Properties param = new Properties();
                            param.setProperty(SPVPNClient.PARAM_VERIFY_CODE, text);
                            SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
                        }
                    }
                });
                break;
            case SPVPNClient.MSGID_LOGIN_NEED_VCCODE_EMAIL:
                // 需要 EMail 验证码，这时 msg 是接收验证码的邮件地址
                SPPopupInputBox.inputBox(this, "请输入[" + msg + "]接收到的 EMail 验证码", "",
                        new SPPopupInputBox.SPInputBoxCallback()
                        {
                            @Override
                            public void OnInputBoxText(String text, boolean remember)
                            {
                                if (!TextUtils.isEmpty(text))
                                {
                                    // 用接收到的验证码进行第二步验证，这里还是调用 SPVPNClient.login() 方法，
                                    // 参数只需要一个 SPVPNClient.PARAM_VERIFY_CODE
                                    Properties param = new Properties();
                                    param.setProperty(SPVPNClient.PARAM_VERIFY_CODE, text);
                                    SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
                                }
                            }
                        });
                break;
            case SPVPNClient.MSGID_LOGIN_NEED_VCCODE_TOKEN:
                // 需要动态口令（第三方）
                SPPopupInputBox.inputBox(this, "请输入动态口令", "", new SPPopupInputBox.SPInputBoxCallback()
                {
                    @Override
                    public void OnInputBoxText(String text, boolean remember)
                    {
                        if (!TextUtils.isEmpty(text))
                        {
                            // 用接收到的验证码进行第二步验证，这里还是调用 SPVPNClient.login() 方法，
                            // 参数只需要一个 SPVPNClient.PARAM_VERIFY_CODE
                            Properties param = new Properties();
                            param.setProperty(SPVPNClient.PARAM_VERIFY_CODE, text);
                            SPVPNClient.login(VPNClientDemoActivity.this, param, VPNClientDemoActivity.this);
                        }
                    }
                });
                break;
            case SPVPNClient.MSGID_LOGIN_FAIL:
            default:
                SPPopupMsgBox.popup(this, "提示", "登录VPN失败: " + msg);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SPVPNClient.REQUEST_CODE_VPN_TUNNEL)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                SPVPNClient.startVPNTunnel(this, true, 10);
                accomplishVPNLogin();
            }
        }
        else if (requestCode == LibSecIDSDKLite.REQUEST_CODE_GEN_TOKEN)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                int errcode = data.getIntExtra(LibSecIDSDKLite.KEY_SECID_ERRCODE, 0);
                if (0 == errcode)
                {
                    // 获取360ID口令成功，用口令进行第二步验证
                    String token = data.getStringExtra(LibSecIDSDKLite.KEY_TOTP_TOKEN);
                    String username = data.getStringExtra(LibSecIDSDKLite.KEY_SECID_USERNAME);
                    loginSecondaryWith360IDToken(TextUtils.isEmpty(username) ? "test1" : username, token);
                }
                else
                {
                    SPPopupMsgBox.popup(this, "提示",
                            "获取360ID安全口令失败: " + data.getStringExtra(LibSecIDSDKLite.KEY_SECID_ERRMSG));
                }
            }
            else
            {
                SPPopupMsgBox.popup(this, "提示", "获取360ID安全口令失败");
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // 按需启动全局VPN隧道
    private void establishVPNTunnel()
    {
        if (SPVPNClient.needsNCVpnTunnel())
        {
            Intent intent = VpnService.prepare(getApplicationContext());
            if (intent != null)
            {
                startActivityForResult(intent, SPVPNClient.REQUEST_CODE_VPN_TUNNEL);
            }
            else
            {
                onActivityResult(SPVPNClient.REQUEST_CODE_VPN_TUNNEL, RESULT_OK, intent);
            }
        }
        else
        {
            accomplishVPNLogin();
        }
    }

    private void accomplishVPNLogin()
    {
        enableButtons(true);
        // SSLVPN 登录完成
        SPPopupMsgBox.popup(this, "提示", "SSLVPN登录成功");
    }

    private void enableButtons(boolean enabled)
    {
        findViewById(R.id.demo_btn_login).setEnabled(!enabled);
        for (View btn : mDemoBtns)
        {
            btn.setEnabled(enabled);
        }
    }

    @Override
    protected void onResume()
    {
        SPVPNClient.registVPNTunnelStateReceiver(this, tunnelStateReceiver);
        SPVPNClient.resitSessionStateReceiver(this, sessionStatusReceiver);
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        SPVPNClient.unregistBroadcastReceiver(this, tunnelStateReceiver);
        SPVPNClient.unregistBroadcastReceiver(this, sessionStatusReceiver);
        super.onPause();
    }

    private class VPNTunnelStateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            StringBuilder msg = new StringBuilder("VPN隧道状态变化 ");
            int state = intent.getIntExtra(SPVPNClient.EXTRA_KEY_NC_TUNNEL_STATE, 0);
            if (state == SPVPNClient.NC_TUNNEL_STATE_CONNECTING)
            {
                msg.append("连接中");
            }
            else if (state == SPVPNClient.NC_TUNNEL_STATE_CONNECTED)
            {
                msg.append("已连接");
            }
            else if (state == SPVPNClient.NC_TUNNEL_STATE_ERROR)
            {
                msg.append("出错了");
            }
            else
            {
                msg.append("已停止");
            }
            Toast.makeText(context, msg.toString(), Toast.LENGTH_LONG).show();
            Log.d("demo", msg.toString());
        }
    }

    private class SessionStatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String topic = intent.getStringExtra("topic");
            String content = intent.getStringExtra("content");
            Log.d("demo", "SSLVPN-Session-State topic: " + topic);
            Log.d("demo", "SSLVPN-Session-State content: " + content);
            if ("session_status".equals(topic))
            {
                JSONObject json = SPJSONUtil.parseObject(content);
                if ("offline".equals(json.optString("session_state")))
                {
                    SPVPNClient.logout();
                    enableButtons(false);
                    SPPopupMsgBox.popup(VPNClientDemoActivity.this, "提示", "SSLVPN下线了");
                }
            }
        }
    }
}
