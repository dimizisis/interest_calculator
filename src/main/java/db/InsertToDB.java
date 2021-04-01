package db;

import data.Globals;
import infrastructure.interest.JavaFile;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class InsertToDB {

    public static boolean insertProjectToDatabase() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO projects (url) VALUES (?) ON CONFLICT (url) DO UPDATE SET url = ?;");
            st.setString(1, Globals.getProjectURL());
            st.setString(2, Globals.getProjectURL());
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

//            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
//                    "complexity, dac, dit, file_path, interest_eu, lcom, mpc, nocc, old_size1, pid, " +
//                    "rfc, sha, size1, size2, wmc, nom, kappa) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE url = '" + Globals.getProjectURL() + "' LIMIT 1), ?, ?, ?, ?, ?, ?, ?)"
//            + " ON CONFLICT (file_path) DO UPDATE SET classes_num = EXCLUDED.classes_num, " +
//                    "complexity = EXCLUDED.complexity, dac = EXCLUDED.dac, dit = EXCLUDED.dit, file_path = EXCLUDED.file_path, interest_eu = EXCLUDED.interest_eu, lcom = EXCLUDED.lcom, mpc = EXCLUDED.mpc, nocc = EXCLUDED.nocc, old_size1 = EXCLUDED.old_size1, pid = (SELECT pid FROM projects WHERE url = '" + Globals.getProjectURL() + "' LIMIT 1), " +
//                    "rfc = EXCLUDED.rfc, size1 = EXCLUDED.size1, size2 = EXCLUDED.size2, wmc = EXCLUDED.wmc, nom = EXCLUDED.nom, kappa = EXCLUDED.kappa;");

            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
                    "complexity, dac, dit, file_path, interest_eu, lcom, mpc, nocc, old_size1, pid, " +
                    "rfc, sha, size1, size2, wmc, nom, kappa, revision_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE url = '" + Globals.getProjectURL() + "' LIMIT 1), ?, ?, ?, ?, ?, ?, ?, ?);");

            st.setInt(1, jf.getQualityMetrics().getClassesNum());
            st.setDouble(2, jf.getQualityMetrics().getComplexity());
            st.setInt(3, jf.getQualityMetrics().getDAC());
            st.setInt(4, jf.getQualityMetrics().getDIT());
            st.setString(5, jf.getPath());
            st.setDouble(6, jf.getInterestInEuros());
            st.setDouble(7, jf.getQualityMetrics().getLCOM());
            st.setDouble(8, jf.getQualityMetrics().getMPC());
            st.setInt(9, jf.getQualityMetrics().getNOCC());
            st.setInt(10, jf.getQualityMetrics().getOldSIZE1());
            st.setDouble(11, jf.getQualityMetrics().getRFC());
            st.setString(12, Globals.getCurrentSha());
            st.setInt(13, jf.getQualityMetrics().getSIZE1());
            st.setInt(14, jf.getQualityMetrics().getSIZE2());
            st.setDouble(15, jf.getQualityMetrics().getWMC());
            st.setDouble(16, jf.getQualityMetrics().getNOM());
            st.setDouble(17, jf.getKappaValue());
            st.setDouble(18, Globals.getRevisionCount());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
