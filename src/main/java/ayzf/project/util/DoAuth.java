package ayzf.project.util;

import ayzf.project.Main;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DoAuth
{
    //limitT 0 -> limitV为注册时间不增加, 1 -> limitV为注册时间增加, 2 -> 当前授权时间满足limitV不增加, 3 -> 当前授权时间满足limitV增加
    //addLen 增加量
    public static String execute(String activityToken, long id)
    {
        AtomicReference<Byte> limitT = new AtomicReference<>((byte) -1);
        AtomicReference<String> limitV = new AtomicReference<>("");
        AtomicReference<String> addLen = new AtomicReference<>("");
        AtomicLong endTime = new AtomicLong(0L);
        Main.configLoader.getObject().forEach((key, value) -> {
            String keyStr = (String) key;
            if (keyStr.endsWith(".activityToken") && Main.configLoader.getProperty(keyStr).equals(activityToken))
            {
                String baseKey = keyStr.substring(0, keyStr.lastIndexOf(".activityToken"));
                limitT.set(Byte.parseByte(Main.configLoader.getProperty(baseKey + ".limitT")));
                limitV.set(Main.configLoader.getProperty(baseKey + ".limitV"));
                addLen.set(Main.configLoader.getProperty(baseKey + ".addLen"));
                endTime.set(Long.parseLong(Main.configLoader.getProperty(baseKey + ".endTime")));
            }
        });

        if (limitT.get() == -1)
            return "输入的令牌活动不存在";
        if (System.currentTimeMillis() / 1000 > endTime.get())
            return "当前活动已结束，请联系管理员";
        if (limitT.get() == 5)
            return limitV.get();
        if (JSONRecord.isAlreadyReceived(id, activityToken))
            return "当前用户已经参加指定的活动，不可重复参加";
        Connection conn = null;
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + Main.configLoader.getProperty("SQL_HOST") + ":" + Main.configLoader.getProperty("SQL_PORT") + "/" + Main.configLoader.getProperty("SQL_DB"), Main.configLoader.getProperty("SQL_ACCOUNT"), Main.configLoader.getProperty("SQL_PASSWORD"));
            PreparedStatement userStatement = conn.prepareStatement("SELECT * FROM " + ((limitT.get() == 4) ? limitV.get() : Main.configLoader.getProperty("SQL_FORM")) + " WHERE " + Main.configLoader.getProperty("SQL_ROW_ID") + " = ?");
            userStatement.setLong(1, id);

            ResultSet userResultSet = userStatement.executeQuery();
            if (userResultSet.next())
            {
                if (limitT.get() == 4)
                    return userResultSet.getString(addLen.get());
                else
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //时间统一秒为单位
                    long autExpireTime = userResultSet.getLong(Main.configLoader.getProperty("SQL_ROW_AUTH"));
                    if (autExpireTime > 1893427200L)//若授权时间大于2030-01-01 00:00:00视为永久，直接拒绝操作
                        return "尊贵的永久授权用户您无需参加本活动";

                    String authTime = sdf.format(new Date(autExpireTime * 1000));
                    if (limitT.get() == 0 || limitT.get() == 1)
                    {
                        long createTime = userResultSet.getTimestamp(Main.configLoader.getProperty("SQL_ROW_CREATE")).getTime() / 1000;
                        String createTimeS = sdf.format(new Date(createTime * 1000));
                        String allowTime = sdf.format(new Date(Long.parseLong(limitV.get()) * 1000));
                        if (limitT.get() == 0 && Long.parseLong(limitV.get()) >= createTime)
                            return "当前账号(" + id + ")不符合规则<br>注册时间：" + createTimeS + "<br>允许时间：" + allowTime + "<br>该活动要求注册时间要比允许时间延后";
                        else if (limitT.get() == 1 && Long.parseLong(limitV.get()) < createTime)
                            return "当前账号(" + id + ")不符合规则<br>注册时间：" + createTimeS + "<br>允许时间：" + allowTime + "<br>该活动要求注册时间要比允许时间提前";
                    }
                    else if (limitT.get() == 2 || limitT.get() == 3)
                    {
                        String allowTime = sdf.format(new Date(Long.parseLong(limitV.get()) * 1000));
                        if (limitT.get() == 2 && Long.parseLong(limitV.get()) >= autExpireTime)
                            return "当前账号(" + id + ")不符合规则<br>当前授权到期：" + authTime + "<br>允许时间：" + allowTime + "<br>该活动要求授权时间要比允许时间延后";
                        else if (limitT.get() == 3 && Long.parseLong(limitV.get()) < autExpireTime)
                            return "当前账号(" + id + ")不符合规则<br>当前授权到期：" + authTime + "<br>允许时间：" + allowTime + "<br>该活动要求授权时间要比允许时间提前";
                    }
                    else
                        return "配置活动出现问题，请联系管理员";

                    long newExpireTime = autExpireTime + Long.parseLong(addLen.get());
                    String updateQuery = "UPDATE " + Main.configLoader.getProperty("SQL_FORM") + " SET `" + Main.configLoader.getProperty("SQL_ROW_AUTH") + "` = ? WHERE " + Main.configLoader.getProperty("SQL_ROW_ID") + " = ?";
                    PreparedStatement updateStatement = conn.prepareStatement(updateQuery);
                    updateStatement.setLong(1, newExpireTime);
                    updateStatement.setLong(2, id);
                    if (updateStatement.executeUpdate() > 0)
                    {
                        JSONRecord.confirmReceived(id, activityToken);
                        return "授权已成功更新<br>旧授权时间：" + sdf.format(new Date(autExpireTime * 1000)) + "<br>新授权时间：" + sdf.format(new Date(newExpireTime * 1000));
                    }
                    else
                        return "授权更新失败，写入数据出现错误，请联系管理员";
                }
            }
            else
                return "请输入一个已经注册在本活动的用户";
        }
        catch (ClassNotFoundException | SQLException e)
        {
            return "操作异常";
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (SQLException ignored)
                {}
            }
        }
    }
}