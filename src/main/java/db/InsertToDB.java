package db;

import data.Globals;
import infrastructure.interest.JavaFile;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class InsertToDB {

    public static boolean insertProjectToDatabase() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO projects (owner, repo, url) VALUES (?, ?, ?) ON CONFLICT (url) DO UPDATE SET url = ?;");
            st.setString(1, Globals.getProjectOwner());
            st.setString(2, Globals.getProjectRepo());
            st.setString(3, Globals.getProjectURL());
            st.setString(4, Globals.getProjectURL());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean insertMetricsToDatabase(JavaFile jf) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
                    "complexity, dac, dit, file_path, interest_eu, interest_in_hours, avg_interest_per_loc, interest_in_avg_loc, sum_interest_per_loc, lcom, mpc, nocc, old_size1, pid, " +
                    "rfc, sha, size1, size2, wmc, nom, kappa, revision_count, cbo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE owner = '" + Globals.getProjectOwner() + "' AND  repo = '" + Globals.getProjectRepo() + "'), ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            st.setInt(1, jf.getQualityMetrics().getClassesNum());
            st.setDouble(2, jf.getQualityMetrics().getComplexity());
            st.setInt(3, jf.getQualityMetrics().getDAC());
            st.setInt(4, jf.getQualityMetrics().getDIT());
            st.setString(5, jf.getPath());
            st.setDouble(6, jf.getInterestInEuros());
            st.setDouble(7, jf.getInterestInHours());
            st.setDouble(8, jf.getAvgInterestPerLoc());
            st.setDouble(9, jf.getInterestInAvgLoc());
            st.setDouble(10, jf.getSumInterestPerLoc());
            st.setDouble(11, jf.getQualityMetrics().getLCOM());
            st.setDouble(12, jf.getQualityMetrics().getMPC());
            st.setInt(13, jf.getQualityMetrics().getNOCC());
            st.setInt(14, jf.getQualityMetrics().getOldSIZE1());
            st.setDouble(15, jf.getQualityMetrics().getRFC());
            st.setString(16, Globals.getCurrentSha());
            st.setInt(17, jf.getQualityMetrics().getSIZE1());
            st.setInt(18, jf.getQualityMetrics().getSIZE2());
            st.setDouble(19, jf.getQualityMetrics().getWMC());
            st.setDouble(20, jf.getQualityMetrics().getNOM());
            st.setDouble(21, jf.getKappaValue());
            st.setDouble(22, Globals.getRevisionCount());
            st.setDouble(23, jf.getQualityMetrics().getCBO());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
