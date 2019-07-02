package com.secure.vpnclient.demo;

import java.net.HttpURLConnection;
import java.net.URL;

import com.secure.sportal.sdk.SPVPNClient;

/**
 * 测试 HttpURLConnection
 */
public class DemoHttpClient
{
    public static void getPage(final String url, final String mode)
    {
        System.out.println("Request URL " + url + " by " + mode);
        new Thread()
        {
            public void run()
            {
                try
                {
                    if ("hook".equals(mode))
                    {
                        getPageByHook(url);
                    }
                    else if ("proxy".equals(mode))
                    {
                        getPageByProxy(url);
                    }
                    else if ("auto".equals(mode))
                    {
                        getPageByAuto(url);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }.start();
    }

    // SDK底层自动Hook连接
    private static void getPageByHook(String url) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        request(conn);
    }

    // 通过SDK的代理服务器
    private static void getPageByProxy(String url) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(SPVPNClient.getHttpProxy());
        request(conn);
    }

    // 在某些机型或系统下Hook可能无法工作，因此可采用先判断然后再自动适配方式
    private static void getPageByAuto(String url) throws Exception
    {
        if (SPVPNClient.isNetHooking())
        {
            getPageByHook(url);
        }
        else
        {
            getPageByProxy(url);
        }
    }

    private static void request(HttpURLConnection conn) throws Exception
    {
        conn.setConnectTimeout(5000); // set timeout to 5 seconds
        conn.connect();
        byte[] bytes = new byte[1024];
        int len = conn.getInputStream().read(bytes);
        System.out.println(new String(bytes, 0, Math.min(len, bytes.length), "UTF-8"));
        conn.getInputStream().close();
        conn.disconnect();
    }
}
