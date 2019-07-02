package com.secure.vpnclient.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.Locale;

import com.secure.sportal.sdk.SPVPNClient;

public class DemoTcpClient
{
    private static final String REQ_STR = "GET / HTTP/1.1\r\n" + "Host: %s\r\n"
            + "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0\r\n"
            + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n"
            + "Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3\r\n" + "Accept-Encoding: gzip, deflate\r\n"
            + "Connection: keep-alive\r\n\r\n";

    public static void connect(final String host, final int port, final String mode)
    {
        new Thread()
        {
            public void run()
            {
                try
                {
                    if ("api".equals(mode))
                    {
                        connectByAPI(host, port);
                    }
                    else if ("hook".equals(mode))
                    {
                        connectByHook(host, port);
                    }
                    else if ("socks_proxy".equals(mode))
                    {
                        connectBySocksProxy(host, port);
                    }
                    else if ("http_proxy".equals(mode))
                    {
                        connectByHttpProxy(host, port);
                    }
                    else if ("map_loopback".equals(mode))
                    {
                        connectByMapLoop(host, port);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }.start();
    }

    private static void connectByAPI(String host, int port) throws Exception
    {
        Socket sock = SPVPNClient.sconnect(host, port, 30000);
        readAndWrite(sock, host);
    }

    private static void connectByHook(String host, int port) throws Exception
    {
        Socket sock = new Socket();
        // 直接连接
        sock.connect(new InetSocketAddress(host, port), 30000);
        readAndWrite(sock, host);
    }

    private static void connectBySocksProxy(String host, int port) throws Exception
    {
        java.net.Proxy proxy = new Proxy(java.net.Proxy.Type.SOCKS,
                new InetSocketAddress("127.0.0.1", SPVPNClient.getProxyPort()));
        Socket sock = new Socket(proxy);
        sock.connect(new InetSocketAddress(host, port), 30000);
        readAndWrite(sock, host);
    }

    // Android的Socket只支持SOCKS代理服务器，因此HTTP代理服务器只能自己实现
    private static void connectByHttpProxy(String host, int port) throws Exception
    {
        Socket sock = new Socket();
        sock.connect(SPVPNClient.getHttpProxy().address(), 30000);

        // 发送代理连接请求
        String request = "CONNECT " + host + ":" + port + " HTTP/1.1\r\n\r\n";
        sock.getOutputStream().write(request.getBytes());

        // HTTP/1.1 200 OK\r\n\r\n
        byte[] bytes = new byte[128];
        InputStream input = sock.getInputStream();
        int len = 0;
        while (len < 128)
        {
            bytes[len++] = (byte) input.read();
            if (len > 4 && bytes[len - 4] == '\r' && bytes[len - 3] == '\n' && bytes[len - 2] == '\r'
                    && bytes[len - 1] == '\n')
            {
                break;
            }
        }
        String response = new String(bytes, 0, Math.min(len, bytes.length), "UTF-8");
        if (!response.startsWith("HTTP/1.1 200 "))
        {
            sock.close();
            throw new IOException("Connect failed");
        }
        readAndWrite(sock, host);
    }

    private static void connectByMapLoop(String host, int port) throws Exception
    {
        int loop_port = SPVPNClient.mapToLoopback(host, port);
        Socket sock = new Socket();
        // 连接到环回地址
        sock.connect(new InetSocketAddress("127.0.0.1", loop_port), 30000);
        readAndWrite(sock, host);
    }

    private static void readAndWrite(Socket sock, String host) throws Exception
    {
        String updata = String.format(Locale.ENGLISH, REQ_STR, host);
        sock.getOutputStream().write(updata.getBytes());

        byte[] bytes = new byte[1024];
        int len = sock.getInputStream().read(bytes);
        System.out.println(new String(bytes, 0, Math.min(len, bytes.length), "UTF-8"));

        try
        {
            sock.getInputStream().close();
        }
        catch (Exception ex)
        {
        }
        try
        {
            sock.getOutputStream().close();
        }
        catch (Exception ex)
        {
        }
        sock.close();
    }
}
