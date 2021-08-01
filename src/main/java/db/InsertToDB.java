package db;

import data.Globals;
import infrastructure.interest.JavaFile;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class InsertToDB {

    public static boolean insertProjectToDatabase() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO projects (owner, repo, url) VALUES (?, ?, ?) ON CONFLICT (owner, repo) DO UPDATE SET url = ?;");
            st.setString(1, Globals.getProjectOwner());
            st.setString(2, Globals.getProjectRepo());
            st.setString(3, Globals.getProjectURL());
            st.setString(4, Globals.getProjectURL());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    public static boolean insertFileToDatabase(JavaFile jf) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO files (pid, file_path, class_names, sha) VALUES ((SELECT pid FROM projects WHERE repo = '" + Globals.getProjectRepo() + "' AND owner = '" + Globals.getProjectOwner() + "'), ?, ?, ?) ON CONFLICT (pid, file_path, sha) DO NOTHING");
            st.setString(1, jf.getPath());
            Array classes = conn.createArrayOf("VARCHAR", jf.getClasses().toArray());
            st.setArray(2, classes);
            st.setString(3, Globals.getCurrentSha());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    public static boolean insertMetricsToDatabase(JavaFile jf) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
                    "complexity, dac, dit, fid, interest_eu, interest_in_hours, avg_interest_per_loc, interest_in_avg_loc, sum_interest_per_loc, lcom, mpc, nocc, old_size1, pid, " +
                    "rfc, sha, size1, size2, wmc, nom, kappa, revision_count, cbo) VALUES (?, ?, ?, ?, (SELECT fid FROM files AS f WHERE file_path = '" + jf.getPath() + "' AND f.sha = '" + Globals.getCurrentSha() + "'), ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE repo = '" + Globals.getProjectRepo() + "' AND owner = '" + Globals.getProjectOwner() + "'), ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            st.setInt(1, jf.getQualityMetrics().getClassesNum());
            st.setDouble(2, jf.getQualityMetrics().getComplexity());
            st.setInt(3, jf.getQualityMetrics().getDAC());
            st.setInt(4, jf.getQualityMetrics().getDIT());
            st.setDouble(5, jf.getInterestInEuros());
            st.setDouble(6, jf.getInterestInHours());
            st.setDouble(7, jf.getAvgInterestPerLoc());
            st.setDouble(8, jf.getInterestInAvgLoc());
            st.setDouble(9, jf.getSumInterestPerLoc());
            st.setDouble(10, jf.getQualityMetrics().getLCOM());
            st.setDouble(11, jf.getQualityMetrics().getMPC());
            st.setInt(12, jf.getQualityMetrics().getNOCC());
            st.setInt(13, jf.getQualityMetrics().getOldSIZE1());
            st.setDouble(14, jf.getQualityMetrics().getRFC());
            st.setString(15, Globals.getCurrentSha());
            st.setInt(16, jf.getQualityMetrics().getSIZE1());
            st.setInt(17, jf.getQualityMetrics().getSIZE2());
            st.setDouble(18, jf.getQualityMetrics().getWMC());
            st.setDouble(19, jf.getQualityMetrics().getNOM());
            st.setDouble(20, jf.getKappaValue());
            st.setDouble(21, Globals.getRevisionCount());
            st.setDouble(22, jf.getQualityMetrics().getCBO());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    public static boolean insertEmpty() {

        try {

            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
                    "complexity, dac, dit, fid, interest_eu, interest_in_hours, avg_interest_per_loc, interest_in_avg_loc, sum_interest_per_loc, lcom, mpc, nocc, old_size1, pid, " +
                    "rfc, sha, size1, size2, wmc, nom, kappa, revision_count, cbo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE repo = '" + Globals.getProjectRepo() + "' AND owner = '" + Globals.getProjectOwner() + "'), ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            st.setInt(1, 0);
            st.setDouble(2, 0.0);
            st.setInt(3, 0);
            st.setInt(4, 0);
            st.setInt(5, -1);
            st.setDouble(6, 0.0);
            st.setDouble(7, 0.0);
            st.setDouble(8, 0.0);
            st.setDouble(9, 0.0);
            st.setDouble(10, 0.0);
            st.setDouble(11, 0.0);
            st.setDouble(12, 0.0);
            st.setInt(13, 0);
            st.setInt(14, 0);
            st.setDouble(15, 0.0);
            st.setString(16, Globals.getCurrentSha());
            st.setInt(17, 0);
            st.setInt(18, 0);
            st.setDouble(19, 0.0);
            st.setDouble(20, 0.0);
            st.setDouble(21, 0.0);
            st.setDouble(22, Globals.getRevisionCount());
            st.setDouble(23, 0.0);
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }
}
