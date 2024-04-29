package ayzf.project;

import asqnw.project.interfaces.MapString;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * @Author 暗影之风
 * @CreateTime 2024-03-31 00:03:25
 * @Description HTTP客户端
 */
@SuppressWarnings("JavadocDeclaration")
public class HttpClient
{
    private final NetCookie<String, String> cookie;
    private final MapString requestProperty;

    public HttpClient()
    {
        this.cookie = new NetCookie<>();
        this.requestProperty = new MapString() {{
            put("Connection", "keep-alive");
        }};
    }

    public String getReqStr(String url) throws HttpException.UnAuthorize, HttpException.Forbidden, HttpException.Unknown, HttpException.ServerError
    {
        return new String(getReqBytes(url), StandardCharsets.UTF_8);
    }

    public byte[] getReqBytes(String url) throws HttpException.UnAuthorize, HttpException.Forbidden, HttpException.Unknown, HttpException.ServerError
    {
        try
        {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);//设置连接主机服务器超时时间：15000毫秒
//            connection.setReadTimeout(60000);//设置读取远程返回的数据时间：60000毫秒
            connection.setDoInput(true);//设置输入流采用字节流
            connection.setDoOutput(false);//设置成true会自动变成POST请求
            //添加请求头
            String[] keys = this.requestProperty.keySet();
            for (String k : keys)
                connection.setRequestProperty(k, this.requestProperty.get(k));
            if (!this.cookie.isEmpty())
                connection.setRequestProperty("Cookie", this.cookie.toString());

            final int responseCode = connection.getResponseCode();//状态码 getResponseCode()隐形调用了connect()
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                setCookie(connection);
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                InputStream stream = getInputStream(connection);
                byte[] buf = new byte[8192];
                int len;
                while ((len = stream.read(buf)) != -1)
                    o.write(buf,0,len);
                return o.toByteArray();
            }
            else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
            {
                connection.disconnect();
                throw new HttpException.UnAuthorize();
            }
            else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN)
            {
                connection.disconnect();
                throw new HttpException.Forbidden();
            }
            else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
            {
                connection.disconnect();
                throw new HttpException.ServerError();
            }
            else
            {
                connection.disconnect();
                throw new HttpException.Unknown(String.valueOf(responseCode));
            }
        }
        catch (HttpException.UnAuthorize | HttpException.Forbidden | HttpException.Unknown | HttpException.ServerError httpException)
        {
            throw httpException;
        }
        catch (Exception ignored)
        {}
        return new byte[0];
    }

    public String postReqStr(String url, String body) throws HttpException.UnAuthorize, HttpException.Forbidden, HttpException.Unknown, HttpException.ServerError
    {
        return new String(postReqBytes(url, body), StandardCharsets.UTF_8);
    }

    public byte[] postReqBytes(String url, String body) throws HttpException.UnAuthorize, HttpException.Forbidden, HttpException.Unknown, HttpException.ServerError
    {
        try
        {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);//设置连接主机服务器超时时间：15000毫秒
//            connection.setReadTimeout(60000);//设置读取远程返回的数据时间：60000毫秒
            connection.setDoInput(true);//设置输入流采用字节流
            connection.setDoOutput(true);//设置输出流采用字节流
            if (!this.cookie.isEmpty())
                connection.setRequestProperty("Cookie", this.cookie.toString());
            //添加请求头
            String[] keys = this.requestProperty.keySet();
            for (String k : keys)
                connection.setRequestProperty(k, this.requestProperty.get(k));

            final OutputStream output = connection.getOutputStream();
            output.write(body.getBytes());
            final OutputStreamWriter osw = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            osw.flush();
            osw.close();

            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                InputStream stream = getInputStream(connection);
                setCookie(connection);
                byte[] buf = new byte[8192];
                int len;
                while ((len = stream.read(buf)) != -1)
                    o.write(buf,0, len);
                connection.disconnect();
                return o.toByteArray();
            }
            else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
            {
                connection.disconnect();
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
                    throw new HttpException.UnAuthorize();
                else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN)
                    throw new HttpException.Forbidden();
                else
                    throw new HttpException.ServerError();
            }
            else
            {
                connection.disconnect();
                throw new HttpException.Unknown(String.valueOf(responseCode));
            }
        }
        catch (HttpException.UnAuthorize | HttpException.Forbidden | HttpException.Unknown | HttpException.ServerError httpException)
        {
            throw httpException;
        }
        catch (Exception ignored)
        {}
        return new byte[0];
    }

    private void setCookie(String cookie)
    {
        String[] itemCookie = cookie.split(";");
        for (String s : itemCookie)//key=value
        {
            final String cookieTag = "=";
            String key;
            String value;
            if (!s.contains(cookieTag))//特殊的不符合键值类型的cookie，通常将该内容存为键，值是true
            {
                key = s;
                value = String.valueOf(true);
            }
            else
            {
                key = s.substring(0, s.indexOf(cookieTag));
                value = s.substring(s.indexOf(cookieTag) + cookieTag.length());
            }
            this.cookie.put(key, value);
        }
    }

    private void setCookie(HttpURLConnection connection)
    {
        final Map<String, List<String>> map = connection.getHeaderFields();//取全部响应头，List是多个相同键组合一起
        final Set<String> set = map.keySet();//取全部响应头键
        for (String k : set)
        {
            if (k != null && k.equals("Set-Cookie"))
            {
                final List<String> list = map.get(k);

                for (String str : Objects.requireNonNull(list))//key1=value1; key2=value2; ...
                    setCookie(str.replaceAll("\\s*", ""));
            }
        }
    }

    private InputStream getInputStream(HttpURLConnection connection) throws IOException
    {
        InputStream is;
        final String encode = connection.getContentEncoding() == null ? "" : connection.getContentEncoding();
        if (encode.equals("deflate"))
            is = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
        else if (encode.equals("gzip"))
            is = new GZIPInputStream(connection.getInputStream());
        else
            is = connection.getInputStream();
        return is;
    }

    public static final class NetCookie<K, V> extends HashMap<K, V>
    {
        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            for (K k : this.keySet())
                sb.append(k).append("=").append(this.get(k)).append(";");
            return sb.substring(0, sb.toString().length() - 1);
        }
    }

    public static class HttpException
    {
        public static class UnAuthorize extends Exception
        {
            UnAuthorize()
            {
                super();
            }
        }

        public static class Forbidden extends Exception
        {
            Forbidden()
            {
                super();
            }
        }

        public static class Unknown extends Exception
        {
            Unknown(String str)
            {
                super(str);
            }
        }

        public static class ServerError extends Exception
        {
            ServerError()
            {
                super();
            }
        }
    }
}