package ayzf.project;

import ayzf.project.util.JSONRecord;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DoAuth
{
    //limitT 0 -> limitV为注册时间不增加, 1 -> limitV为注册时间增加, 2 -> 当前授权时间满足limitV不增加, 3 -> 当前授权时间满足limitV增加
    //addLen 增加量
    public static String execute(String activityToken, long id)
    {
        byte limitT = 0;
        long limitV = 0L;
        long addLen = 0L;

        Connection conn = null;
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + Main.configLoader.getProperty("SQL_HOST") + ":" + Main.configLoader.getProperty("SQL_PORT") + "/" + Main.configLoader.getProperty("SQL_DB"), Main.configLoader.getProperty("SQL_ACCOUNT"), Main.configLoader.getProperty("SQL_PASSWORD"));
            PreparedStatement userStatement = conn.prepareStatement("SELECT * FROM " + Main.configLoader.getProperty("FORM") + " WHERE " + Main.configLoader.getProperty("SQL_ROW_ID") + " = ?");
            userStatement.setLong(1, id);

            ResultSet userResultSet = userStatement.executeQuery();
            if (userResultSet.next())
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                //时间统一秒为单位
                long autExpireTime = userResultSet.getLong(Main.configLoader.getProperty("SQL_ROW_AUTH"));
                if (autExpireTime > 1893427200L)//若授权时间大于2030-01-01 00:00:00视为永久，直接拒绝操作
                    return "尊贵的永久授权用户您无需参加本活动";
                String authTime = sdf.format(new Date(autExpireTime * 1000));
                if (limitT == 0 || limitT == 1)
                {
                    long createTime = userResultSet.getTimestamp(Main.configLoader.getProperty("SQL_ROW_CREATE")).getTime() / 1000;
                    String createTimeS = sdf.format(new Date(createTime * 1000));
                    String allowTime = sdf.format(new Date(limitV * 1000));
                    if (limitT == 0 && limitV >= createTime)
                        return "当前账号(" + id + ")不符合规则\n注册时间：" + createTimeS + "\n允许时间：" + allowTime + "\n该活动要求注册时间要比允许时间延后";
                    else if (limitT == 1 && limitV < createTime)
                        return "当前账号(" + id + ")不符合规则\n注册时间：" + createTimeS + "\n允许时间：" + allowTime + "\n该活动要求注册时间要比允许时间提前";
                }
                else if (limitT == 2 || limitT == 3)
                {
                    String allowTime = sdf.format(new Date(limitV * 1000));
                    if (limitT == 2 && limitV >= autExpireTime)
                        return "当前账号(" + id + ")不符合规则\n当前授权到期：" + authTime + "\n允许时间：" + allowTime + "\n该活动要求授权时间要比允许时间延后";
                    else if (limitT == 3 && limitV < autExpireTime)
                        return "当前账号(" + id + ")不符合规则\n当前授权到期：" + authTime + "\n允许时间：" + allowTime + "\n该活动要求授权时间要比允许时间提前";
                }
                else
                {
                    return "配置活动出现问题，请联系管理员";
                }

                if (JSONRecord.isAlreadyReceived(id, activityToken))
                    return "当前用户已经参加指定的活动，不可重复参加";
                else
                    JSONRecord.confirmReceived(id, activityToken);

                long newExpireTime = autExpireTime + addLen;
                String updateQuery = "UPDATE " + Main.configLoader.getProperty("SQL_DB") + " SET `" + Main.configLoader.getProperty("SQL_ROW_AUTH") + "` = ? WHERE " + Main.configLoader.getProperty("SQL_ROW_ID") + " = ?";
                PreparedStatement updateStatement = conn.prepareStatement(updateQuery);
                updateStatement.setLong(1, newExpireTime);
                updateStatement.setLong(2, id);
                if (updateStatement.executeUpdate() > 0)
                    return "授权已成功更新\n旧授权时间：" + sdf.format(new Date(autExpireTime * 1000)) + "\n新授权时间：" + sdf.format(new Date(newExpireTime * 1000));
                else
                    return "授权更新失败，写入数据出现错误，请联系管理员";
            }
            else
                return "请输入一个已经注册在本平台的用户";
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