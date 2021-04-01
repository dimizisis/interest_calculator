package db;

import infrastructure.interest.JavaFile;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DeleteFromDB {

//    public static boolean deleteFileFromDatabase(JavaFile jf) {
//
//        try {
//
//            Connection conn = DatabaseConnection.getConnection();
//
//            PreparedStatement st = conn.prepareStatement("DELETE FROM metrics WHERE file_path = '?'");
//
//            st.setString(1,  jf.getPath().replace("\\", "/"));
//            st.executeUpdate();
//            st.close();
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    public static boolean deleteProjectFromDatabase(String url) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("DELETE FROM projects WHERE url LIKE '%?%'");

            st.setString(1, url.replace(".git", ""));
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

