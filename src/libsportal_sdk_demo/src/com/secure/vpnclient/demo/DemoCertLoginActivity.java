package com.secure.vpnclient.demo;

import java.util.Properties;

import com.secure.comm.utils.SPFileUtil;
import com.secure.comm.view.SPPopupInputBox;
import com.secure.comm.view.SPPopupMsgBox;
import com.secure.libsportal.sdk.demo.R;
import com.secure.sportal.sdk.SPVPNClient;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;

public class DemoCertLoginActivity extends Activity implements View.OnClickListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smxcert_test);

        findViewById(R.id.btn_login_cert_soft).setOnClickListener(this);
        findViewById(R.id.btn_login_cert_hard).setOnClickListener(this);
        findViewById(R.id.btn_logout).setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btn_login_cert_soft)
        {
            TestLoginCert(null);
        }
        else if (v.getId() == R.id.btn_login_cert_hard)
        {
            SPPopupInputBox.inputBox(this, "请输入国密TF卡PIN码",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, null, null, null,
                    new SPPopupInputBox.SPInputBoxCallback()
                    {
                        @Override
                        public void OnInputBoxText(String text, boolean remember)
                        {
                            if (!TextUtils.isEmpty(text))
                            {
                                TestLoginCert(text);
                            }
                        }
                    });
        }
        else if (v.getId() == R.id.btn_logout)
        {
            SPVPNClient.logout();
        }
    }

    private void TestLoginCert(String sdcardPin)
    {
        Properties params = new Properties();
        // VPN服务器地址
        params.setProperty(SPVPNClient.PARAM_VPN_HOST, "172.16.1.233");
        params.setProperty(SPVPNClient.PARAM_VPN_PORT, "443");
        // 认证服务器名称
        params.setProperty(SPVPNClient.PARAM_AUTH_SERVER, "a");
        if (TextUtils.isEmpty(sdcardPin))
        {
            // 没有pin码，表示是软证书方式
            // pfx文件是 证书文件+私钥
            String filename = SPFileUtil.getSdCardPath() + "/client.pfx";
            String pfx = Base64.encodeToString(SPFileUtil.readFile(filename), Base64.NO_WRAP);
            // pfx证书文件内容base64编码
            params.setProperty(SPVPNClient.PARAM_AUTH_USERNAME, pfx);
            // 私钥密码
            params.setProperty(SPVPNClient.PARAM_AUTH_PASSWORD, "aaaaaa");
        }
        else
        {
            // 硬件证书不用传递证书内容，SDK自动从TF卡获取
            params.setProperty(SPVPNClient.PARAM_AUTH_USERNAME, "");
            params.setProperty(SPVPNClient.PARAM_AUTH_PASSWORD, "");
            // 国密TF卡pin码
            params.setProperty(SPVPNClient.PARAM_SDCARD_PIN, sdcardPin);
        }

        SPVPNClient.login(this, params, new SPVPNClient.OnVPNLoginCallback()
        {
            @Override
            public void onVPNLoginMessage(int msgid, String msg)
            {
                if (SPVPNClient.MSGID_LOGIN_SUCC == msgid)
                {
                    // 启动VPN隧道，SDK内部会自行根据是否有NC服务决定是否启动VPN隧道
                    startVpnTunnel();
                }
                else
                {
                    SPPopupMsgBox.popup(DemoCertLoginActivity.this, "登录失败", msg);
                }
            }
        });
    }

    private void startVpnTunnel()
    {
        Intent intent = VpnService.prepare(getApplicationContext());
        if (intent != null)
        {
            startActivityForResult(intent, SPVPNClient.REQUEST_CODE_VPN_TUNNEL);
        }
        else
        {
            onActivityResult(SPVPNClient.REQUEST_CODE_VPN_TUNNEL, RESULT_OK, null);
        }
    }

    // 因为第一次启动时需要用户确认，因此需要重载onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SPVPNClient.REQUEST_CODE_VPN_TUNNEL)
        {
            if (resultCode == RESULT_OK)
            {
                // 启动VPN隧道，SDK内部会自行根据是否有NC服务决定是否启动VPN隧道
                SPVPNClient.startVPNTunnel(this, true, 1);
            }
        }
    }
}
