package ayzf.project;

import ayzf.project.http.HttpClient;
import ayzf.project.http.HttpServer;
import ayzf.project.util.ConfigLoader;
import ayzf.project.util.DoAuth;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Main
{
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    public static ConfigLoader configLoader;

    public static void main(String[] args)
    {
        int port = 10000;
        System.out.println("欢迎使用ayzf产品--SEC授权补电\n\n现在准备启动必须内容，请稍等");
        System.out.println("一、设置服务器端口");
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("-port"))
            {
                try
                {
                    port = Integer.parseInt(args[i + 1]);
                    System.out.println("1.1设置端口：" + port);
                }
                catch (Exception e)
                {
                    System.out.println("1.1读取端口参数异常，使用默认参数10000");
                }
            }
        }
        if (port == 10000)
            System.out.println("1.1未设置端口参数，使用默认参数10000");
        System.out.println("二、加载配置文件");
        try
        {
            configLoader = new ConfigLoader("config.properties");
        }
        catch (IOException e)
        {
            System.out.println("2.1配置文件不存在，退出执行");
            System.exit(0);
        }
        catch (StringIndexOutOfBoundsException ignored)
        {
            System.out.println("2.1配置文件设置异常，退出执行");
            System.exit(0);
        }
        System.out.println("准备完成，启动服务器");
        new HttpServer().start(request -> {
            HashMap<String, String> response = new HashMap<>();
            String vaptcha_id = configLoader.getProperty("VAPTCHA_ID");
            String vaptcha_sence = configLoader.getProperty("VAPTCHA_SENCE");
            if (request.get(HttpServer.URL).get(0).equals("/doAuth"))
            {
                String userIp = HttpServer.findHeader(request.get(HttpServer.HEADER), "x-forwarded-for");//这里无需担心该请求头伪造问题，在Nginx中已经处理
                String body;
                System.out.println(request);
                ArrayList<String> postBody = request.get(HttpServer.BODY);
                if ((body = (postBody == null ? "" : postBody.get(0))).isEmpty())//GET请求
                {
                    response.put("403 Forbidden", "");
                }
                else
                {
                    ArrayList<HashMap<String, String>> param = HttpServer.getParam(URLDecoder.decode(body, StandardCharsets.UTF_8));
                    try
                    {
                        String s;
                        JSONObject vaptcha = new JSONObject(HttpServer.findParam(param, "vaptcha"));
                        vaptcha = new JSONObject(new HttpClient().postReqStr(Pattern.matches("http(s)?://.*\\.vaptcha\\.(net|com)/verify", s = vaptcha.getString("server")) ? s : "", "{\"id\": \"" + vaptcha_id + "\",\"secretkey\": \"" + configLoader.getProperty("VAPTCHA_KEY") + "\",\"scene\": " + vaptcha_sence + ",\"token\": \"" + vaptcha.getString("token") + "\",\"ip\": \"" + userIp + "\"}"));
                        if (vaptcha.getInt("success") == 1)
                            response.put("200 OK", DoAuth.execute(HttpServer.findParam(param, "activityToken"), Long.parseLong(HttpServer.findParam(param, "qqNumber"))));
                        else
                            response.put("200 OK", "验证不通过，错误如下：" + vaptcha.getString("msg"));
                    }
                    catch (NumberFormatException ignored)
                    {
                        response.put("200 OK", "用户账号输入不合法");
                    }
                    catch (JSONException ignored)
                    {
                        response.put("200 OK", "接收验证数据出现错误");
                    }
                    catch (HttpClient.HttpException.UnAuthorize | HttpClient.HttpException.Unknown | HttpClient.HttpException.ServerError | HttpClient.HttpException.Forbidden ignored)
                    {
                        response.put("200 OK", "验证数据服务器出现异常");
                    }
                    catch (Exception ignored)
                    {
                        response.put("200 OK", "未知错误");
                    }
                }
            }
            else if (request.get(HttpServer.URL).get(0).equals("/"))
            {
                response.put("200 OK", "<!DOCTYPE html>\n" +
                        "<html lang=\"zh-cn\">\n" +
                        "<head>\n" +
                        "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                        "    <title>NXA-电量补足</title>\n" +
                        "    <style>\n" +
                        "        body {\n" +
                        "            font-family: Arial, sans-serif;\n" +
                        "            margin: 20px;\n" +
                        "        }\n" +
                        "\n" +
                        "        h1 {\n" +
                        "            text-align: center;\n" +
                        "            color: #333;\n" +
                        "        }\n" +
                        "\n" +
                        "        .form-container {\n" +
                        "            max-width: 90%;\n" +
                        "            margin: 0 auto;\n" +
                        "            background-color: #f5f5f5;\n" +
                        "            padding: 20px;\n" +
                        "            border-radius: 5px;\n" +
                        "            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);\n" +
                        "        }\n" +
                        "\n" +
                        "        .form-container label {\n" +
                        "            display: block;\n" +
                        "            margin-bottom: 10px;\n" +
                        "            color: #333;\n" +
                        "        }\n" +
                        "\n" +
                        "        .form-container input[type=\"text\"],\n" +
                        "        .form-container input[type=\"captcha\"] {\n" +
                        "            width: 100%;\n" +
                        "            padding: 8px;\n" +
                        "            border: 1px solid #ccc;\n" +
                        "            border-radius: 4px;\n" +
                        "            box-sizing: border-box;\n" +
                        "        }\n" +
                        "\n" +
                        "        .form-container .captcha-image img {\n" +
                        "            max-width: 100%;\n" +
                        "            height: auto;\n" +
                        "            border-radius: 5px;\n" +
                        "        }\n" +
                        "\n" +
                        "        .form-container button {\n" +
                        "            width: 100%;\n" +
                        "            padding: 10px 16px;\n" +
                        "            background-color: #4CAF50;\n" +
                        "            color: white;\n" +
                        "            border: none;\n" +
                        "            border-radius: 4px;\n" +
                        "            cursor: pointer;\n" +
                        "        }\n" +
                        "\n" +
                        "        .ad-container img {\n" +
                        "            max-width: 100%;\n" +
                        "            height: auto;\n" +
                        "            border-radius: 5px;\n" +
                        "            margin-bottom: 10px;\n" +
                        "        }\n" +
                        "\n" +
                        "        .ad-container p {\n" +
                        "            color: #666;\n" +
                        "            font-size: 14px;\n" +
                        "        }\n" +
                        "\n" +
                        "        .VAPTCHA-init-main {\n" +
                        "            display: table;\n" +
                        "            width: 100%;\n" +
                        "            height: 100%;\n" +
                        "            background-color: #eeeeee;\n" +
                        "        }\n" +
                        "\n" +
                        "        .VAPTCHA-init-loading {\n" +
                        "            display: table-cell;\n" +
                        "            vertical-align: middle;\n" +
                        "            text-align: center;\n" +
                        "        }\n" +
                        "\n" +
                        "        .VAPTCHA-init-loading>a {\n" +
                        "            display: inline-block;\n" +
                        "            width: 18px;\n" +
                        "            height: 18px;\n" +
                        "            border: none;\n" +
                        "        }\n" +
                        "\n" +
                        "        .VAPTCHA-init-loading .VAPTCHA-text {\n" +
                        "            font-family: sans-serif;\n" +
                        "            font-size: 12px;\n" +
                        "            color: #cccccc;\n" +
                        "            vertical-align: middle;\n" +
                        "        }\n" +
                        "    </style>\n" +
                        "    <script src=\"https://cdn.jsdelivr.net/npm/sweetalert2@8\"></script>\n" +
                        "    <script src=\"https://cdn.bootcdn.net/ajax/libs/jquery/1.8.0/jquery-1.8.0.min.js\"></script>\n" +
                        "    <script src=\"https://v-cn.vaptcha.com/v3.js\"></script>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<h1>NXA-电量补足</h1>\n" +
                        "\n" +
                        "<div class=\"form-container\">\n" +
                        "    <form id=\"myForm\">\n" +
                        "        <label for=\"activityToken\">活动令牌：</label>\n" +
                        "        <input type=\"text\" id=\"activityToken\" required>\n" +
                        "        <br>\n" +
                        "        <label for=\"qqNumber\">QQ号码：</label>\n" +
                        "        <input type=\"text\" id=\"qqNumber\" required>\n" +
                        "        <br>\n" +
                        "        <label>验证码：</label>\n" +
                        "        <div id=\"VAPTCHAContainer\" style=\"width: 300px;height: 36px;\">\n" +
                        "            <div class=\"VAPTCHA-init-main\">\n" +
                        "                <div class=\"VAPTCHA-init-loading\">\n" +
                        "                    <a href=\"/\" target=\"_blank\">\n" +
                        "                        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"48px\" height=\"60px\" viewBox=\"0 0 24 30\" style=\"enable-background: new 0 0 50 50; width: 14px; height: 14px; vertical-align: middle\" xml:space=\"preserve\">\n" +
                        "                            <rect x=\"0\" y=\"9.22656\" width=\"4\" height=\"12.5469\" fill=\"#CCCCCC\">\n" +
                        "                                <animate attributeName=\"height\" attributeType=\"XML\" values=\"5;21;5\" begin=\"0s\" dur=\"0.6s\" repeatCount=\"indefinite\"></animate>\n" +
                        "                                <animate attributeName=\"y\" attributeType=\"XML\" values=\"13; 5; 13\" begin=\"0s\" dur=\"0.6s\" repeatCount=\"indefinite\"></animate>\n" +
                        "                            </rect>\n" +
                        "                                <rect x=\"10\" y=\"5.22656\" width=\"4\" height=\"20.5469\" fill=\"#CCCCCC\">\n" +
                        "                                <animate attributeName=\"height\" attributeType=\"XML\" values=\"5;21;5\" begin=\"0.15s\" dur=\"0.6s\" repeatCount=\"indefinite\"></animate>\n" +
                        "                                <animate attributeName=\"y\" attributeType=\"XML\" values=\"13; 5; 13\" begin=\"0.15s\" dur=\"0.6s\" repeatCount=\"indefinite\"></animate>\n" +
                        "                            </rect>\n" +
                        "                                <rect x=\"20\" y=\"8.77344\" width=\"4\" height=\"13.4531\" fill=\"#CCCCCC\">\n" +
                        "                                <animate attributeName=\"height\" attributeType=\"XML\" values=\"5;21;5\" begin=\"0.3s\" dur=\"0.6s\" repeatCount=\"indefinite\"></animate>\n" +
                        "                                <animate attributeName=\"y\" attributeType=\"XML\" values=\"13; 5; 13\" begin=\"0.3s\" dur=\"0.6s\" repeatCount=\"indefinite\"></animate>\n" +
                        "                            </rect>\n" +
                        "                        </svg>\n" +
                        "                    </a>\n" +
                        "                    <span class=\"VAPTCHA-text\">Vaptcha Initializing...</span>\n" +
                        "                </div>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "        <br>\n" +
                        "        <label>出现问题联系QQ：2070560848</label>\n" +
                        "        <button type=\"submit\" id=\"submitBtn\" disabled>提交</button>\n" +
                        "    </form>\n" +
                        "</div>\n" +
                        "\n" +
                        "<script>\n" +
                        "    Swal.fire({\n" +
                        "        title: '公告',\n" +
                        "        html: '<p>欢迎使用。目前活动正常运行</p>',\n" +
                        "        showCloseButton: true,\n" +
                        "        showConfirmButton: false,\n" +
                        "        allowOutsideClick: true,\n" +
                        "        allowEscapeKey: true,\n" +
                        "        allowEnterKey: true\n" +
                        "    });\n" +
                        "\n" +
                        "    vaptcha({\n" +
                        "        vid: '" + vaptcha_id +  "',\n" +
                        "        mode: 'click',\n" +
                        "        scene: " + vaptcha_sence +  ",\n" +
                        "        container: '#VAPTCHAContainer',\n" +
                        "        area: 'auto',\n" +
                        "    }).then(function (VAPTCHAObj) {\n" +
                        "        VAPTCHAObj.render();\n" +
                        "        VAPTCHAObj.listen('pass', function () {\n" +
                        "            let isRequesting = false;\n" +
                        "            const protocol = window.location.protocol\n" +
                        "            const host = window.location.host;\n" +
                        "            const serverToken = VAPTCHAObj.getServerToken();\n" +
                        "            const data = {\n" +
                        "                server: serverToken.server,\n" +
                        "                token: serverToken.token,\n" +
                        "            }\n" +
                        "            document.getElementById('submitBtn').disabled = false;\n" +
                        "\n" +
                        "            document.getElementById('myForm').addEventListener('submit', function (event) {\n" +
                        "                event.preventDefault();\n" +
                        "\n" +
                        "                if (isRequesting) {\n" +
                        "                    return;\n" +
                        "                }\n" +
                        "\n" +
                        "                const activityToken = document.getElementById('activityToken').value;\n" +
                        "                const qqNumber = document.getElementById('qqNumber').value;\n" +
                        "                const url = protocol + '//' + host + '/doAuth';\n" +
                        "                const params = new URLSearchParams();\n" +
                        "                params.append('activityToken', activityToken);\n" +
                        "                params.append('qqNumber', qqNumber);\n" +
                        "                params.append('vaptcha', JSON.stringify(data));\n" +
                        "\n" +
                        "                isRequesting = true;\n" +
                        "\n" +
                        "                fetch(url, {\n" +
                        "                    method: 'POST',\n" +
                        "                    body: params\n" +
                        "                })\n" +
                        "                    .then(response => {\n" +
                        "                        if (!response.ok) {\n" +
                        "                            throw new Error('请求失败');\n" +
                        "                        }\n" +
                        "                        return response.text();\n" +
                        "                    })\n" +
                        "                    .then(data => {\n" +
                        "                        Swal.fire({\n" +
                        "                            title: '服务器返回',\n" +
                        "                            html: data,\n" +
                        "                            icon: 'success'\n" +
                        "                        });\n" +
                        "                    })\n" +
                        "                    .catch(error => {\n" +
                        "                        console.error('发生错误:', error);\n" +
                        "                        Swal.fire({\n" +
                        "                            title: '发生错误',\n" +
                        "                            text: '请求失败',\n" +
                        "                            icon: 'error'\n" +
                        "                        });\n" +
                        "                    })\n" +
                        "                    .finally(() => {\n" +
                        "                        isRequesting = false;\n" +
                        "                        VAPTCHAObj.reset()\n" +
                        "                    });\n" +
                        "            });\n" +
                        "        })\n" +
                        "    })\n" +
                        "</script>\n" +
                        "</body>\n" +
                        "</html>");
            }
            else
            {
                response.put("403 Forbidden", "");
            }
            return response;
        }, port);
    }

    public static void thread(Runnable runnable)
    {
        threadPool.execute(runnable);
    }
}