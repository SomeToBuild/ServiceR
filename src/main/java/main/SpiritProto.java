package main;


import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Main object for Server
 */
public class SpiritProto {
    /**
     * Default Constructor
	 */

	public SpiritProto(){

	}
	/**
	 * Get Token
	 * @param Login Login of user
	 * @param Password Password of user
	 * @return Generated Token
	 */
    //TODO Move to Player Class;
    public String GetToken(String Login,String Password){

        Connection con;
        PreparedStatement pstmt;
        String Token = "T" + UUID.randomUUID().toString();
        String result = "";
        try {
            con = DBUtils.ConnectDB();
            pstmt = con.prepareStatement("SELECT count(1) from gplayers WHERE PlayerName=? and Password=?");
            pstmt.setString(1, Login);
            pstmt.setString(2, Password);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
			if (rs.getInt(1)==0) {
                result = MyUtils.getJSONError("AccessDenied", "User not found.");
            } else {
                pstmt = con.prepareStatement("UPDATE gplayers SET USERTOKEN=? WHERE PlayerName=? and Password=?");
                pstmt.setString(1, Token);
                pstmt.setString(2, Login);
                pstmt.setString(3, Password);
                pstmt.execute();
                con.commit();
                con.close();
            }

        } catch (SQLException e) {
            result = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));

        } catch (NamingException e) {
            result = MyUtils.getJSONError("ResourceError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));

		}
        if (result.equals("")) return "{Token:" + '"' + Token + '"' + "}";
        else return result;

    }

	/**
	 * Get info of objects around coord
	 * @param token Secure Token
	 * @param Lat Latitude of point
	 * @param Lng Longtitude of point
	 * @return JSON String
	 */
	public String GetData(String token,int Lat,int Lng){
        String result = "";
        Connection con = null;
        try {
            con = DBUtils.ConnectDB();
            PlayerObj player = new PlayerObj();
            player.GetDBDataByToken(con, token);
            if (!player.isLogin()){
                con.close();
                result = MyUtils.getJSONError("AccessDenied", "We dont know you.");
            }
            else {
                player.setPos(Lat, Lng);
                player.SetDBData(con);
                PreparedStatement stmt = con.prepareStatement("select GUID,ObjectType from aobject where SQRT(POWER(?-Lat,2)+POWER(?-Lng,2))<10000");
                stmt.setInt(1, Lat);
                stmt.setInt(2, Lng);
                ResultSet rs = stmt.executeQuery();
                rs.beforeFirst();
                ArrayList<CityObj> Cities = new ArrayList<>();
                ArrayList<AmbushObj> Ambushes = new ArrayList<>();
                if (rs.isBeforeFirst()) {
                    while (rs.next()) {

                        String GUID = rs.getString(1);
                        String ObjType = rs.getString(2);
                        if (ObjType == null) {

                        } else if (ObjType.equalsIgnoreCase("CITY")) {
                            CityObj City = new CityObj();
                            City.GetDBData(con, GUID);
                            Cities.add(City);
                        } else if (ObjType.equalsIgnoreCase("AMBUSH")) {
                            AmbushObj ambush = new AmbushObj(con, GUID);
                            Ambushes.add(ambush);
                        }

                    }
                }
                result = "{Result:\"Succes\",Player:" + player.toString();
                String citiesinfo = null;
                for (CityObj city : Cities) {
                    if (citiesinfo == null) citiesinfo = city.toString();
                    else citiesinfo += "," + city.toString();

                }
                if (citiesinfo != null) result += "," + "Cities:[" + citiesinfo + "]";
                String ambushinfo = null;
                for (AmbushObj ambush : Ambushes) {
                    if (ambushinfo == null) ambushinfo = ambush.toString();
                    else ambushinfo += "," + ambush.toString();

                }
                if (ambushinfo != null) result += "," + "Ambushes:[" + ambushinfo + "]";

                result += "}";

                con.commit();
            }

        } catch (NamingException | SQLException e) {
            result= MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));

        } catch (Exception e) {
            result = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        } finally {
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback();
                    con.close();
                }
            } catch (SQLException el) {
                result= MyUtils.getJSONError("DBError", el.toString() + "\n" + Arrays.toString(el.getStackTrace()));
            }
        }

		return result;
	}


    /**
     * Create New Player
     *
     * @param Login Player Login
     * @param Password Player Password
     * @param email Player Email
     */
    public String NewPlayer(String Login,String Password,String email,String InviteCode){

		PreparedStatement stmt;
        String result;
        try {
            Connection con = DBUtils.ConnectDB();
            ResultSet rs;
			//Check inviteCode
            stmt = con.prepareStatement("select count(1) cnt from invites where inviteCode=? and Invited=''");
            stmt.setString(1, InviteCode);
            rs=stmt.executeQuery();
			rs.first();
			if (rs.getInt("cnt")==0){
				stmt.close();
				con.close();
                result = "NoInviteCode";
                return MyUtils.getJSONError(result, result);
            }
            //Check Name Available
            stmt = con.prepareStatement("select count(1) cnt from gplayers where PlayerName=? or email=?");
            stmt.setString(1,Login);
            stmt.setString(2,email);
			rs=stmt.executeQuery();
			rs.first();
			if (rs.getInt("cnt")>0){
				stmt.close();
				con.close();
                result = "UserExists";
                return MyUtils.getJSONError(result, result);
            }
            if (Password.length()<6){
				stmt.close();
				con.close();
                result = "EasyPassword";
                return MyUtils.getJSONError(result, result);
            }
            //Write InviteCode
			String GUID=UUID.randomUUID().toString();
			stmt=con.prepareStatement("update invites set Invited=? where inviteCode=?");
            stmt.setString(1, GUID);
            stmt.setString(2, InviteCode);
            stmt.execute();
            //Write Player Info
            stmt = con.prepareStatement("insert into gplayers(GUID,PlayerName,Password,email,HomeCity) VALUES(?,?,?,?,(select guid from cities order by RAND() limit 0,1))");
            stmt.setString(1, GUID);
            stmt.setString(2,Login);
            stmt.setString(3, Password);
            stmt.setString(4, email);
            stmt.execute();
            stmt = con.prepareStatement("insert into aobject(GUID,ObjectType) VALUES(?,\"PLAYER\")");
            stmt.setString(1, GUID);
            stmt.execute();
			stmt.close();
			con.commit();
			con.close();

		} catch (SQLException e) {
            return MyUtils.getJSONError("DBError", e.toString() + Arrays.toString(e.getStackTrace()));
        } catch (NamingException e) {
            return MyUtils.getJSONError("DBError", e.toString() + Arrays.toString(e.getStackTrace()));
        }

        return MyUtils.getJSONSuccess("User Creater.");
    }
    public String action(String Token, String PLat, String PLng, String TargetGUID, String Action){
        int Lat=Integer.parseInt(PLat);
        int Lng=Integer.parseInt(PLng);
        return action(Token,Lat,Lng,TargetGUID,Action);
    }
    public String action(String Token, int PLat, int PLng, String TargetGUID, String Action) {
        Connection con;
        String result;
        String GUID;
        String check;
        try {
            con = DBUtils.ConnectDB();
            PlayerObj player= new PlayerObj();
            player.GetDBDataByToken(con,Token);
            if (player.isLogin())  {
                RouteObj route = new RouteObj(player.GetGUID(), TargetGUID);
                AmbushObj ambush = new AmbushObj();
                switch (Action) {
                    case "createRoute":
                        check=route.checkCreateRoute(player.GetGUID()); //Так делать можно!
                        if (check.equalsIgnoreCase("Ok")) {
                            result= route.createRoute(player.GetGUID(), TargetGUID);
                        } else {result=check;}
                        break;
                    case "finishRoute":
                        GUID = route.getUnfinishedRoute(player.GetGUID());
                        if (GUID.length()>40) {
                            result = GUID;
                        }
                        else {
                            check = route.checkFinishRoute(player.GetGUID(), GUID, TargetGUID);
                            if (check.equalsIgnoreCase("Ok")) {
                                result = route.finishRoute(player.GetGUID(), GUID, TargetGUID);
                            } else {
                                result = check;
                            }
                        }
                        break;
                    case "createAmbush":
                        check=ambush.checkCreateAmbush(PLat, PLng);
                        if (check.equalsIgnoreCase("Ok")) {
                            result= ambush.createAmbush(player.GetGUID(), PLat, PLng);
                        } else {result=check;}
                        break;
                    case "removeAmbush":
                        check=ambush.checkRemoveAmbush(PLat, PLng, TargetGUID);
                        if (check.equalsIgnoreCase("Ok")) {
                            result= ambush.removeAmbush(player.GetGUID(), TargetGUID);
                        } else {result=check;}
                        break;
                    case "dropRoute":
                        //Zlodiak: вставить чек на возможность дропа
                        result=route.dropRoute(TargetGUID);
                        break;
                    case "stopRoute":
                        //Остановить создание маршрута
                        result=route.getUnfinishedRoute(player.GetGUID());
                        if (result.equalsIgnoreCase("Not found"))
                            result=MyUtils.getJSONError("RouteNotFound","Unfinished route not found.");
                            else result=route.dropRoute(TargetGUID);
                        break;
                    case "setHome":
                        result=player.setHome(player.GetGUID(),TargetGUID);
                        break;

                    default:
                        result = MyUtils.getJSONError("ActionNotFound", "Действие не определено");
                }
            } else
            {
                result = MyUtils.getJSONError("AccessDenied", "PlayerNotLoginIn " + Token);
            }
            con.close();
        } catch (SQLException | NamingException e) {
            result=MyUtils.getJSONError("DBError",e.toString());
        }
        return result;

    }
    public String getRouteList(String token,String city){
        Connection con = null;
        String result ;
        try {
             con=DBUtils.ConnectDB();
            PreparedStatement stmt;
            PlayerObj player=new PlayerObj();
            player.GetDBDataByToken(con,token);
            if (player.isLogin()) {
                if (city.equalsIgnoreCase("")) {
                    stmt = con.prepareStatement("select GUID from routes where owner=? and start!='' and finish!=''");
                    stmt.setString(1,player.GetGUID());
                } else {
                    stmt=con.prepareStatement("select GUID from routes where owner=? and (start=? or finish=?)");
                    stmt.setString(1,player.GetGUID());
                    stmt.setString(2,city);
                    stmt.setString(3,city);
                }
                ResultSet rs = stmt.executeQuery();
                rs.beforeFirst();
                result="{Routes:[";
                if (rs.isBeforeFirst()){

                    while (rs.next()){
                        RouteObj route=new RouteObj(con,rs.getString("GUID"));

                        if (rs.isFirst()) result+=route.toString();
                        else result+=','+route.toString();
                    }

                }
                result+="]}";
            } else
            {
                result=MyUtils.getJSONError("AccessDenied","PlayerNotLoginIn");
            }

        } catch (NamingException | SQLException e) {
            result=MyUtils.getJSONError("DBError",e.toString());
        } finally {
            try {
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                result=MyUtils.getJSONError("DBError",e.toString());
            }
        }
        return result;
    }
}
