package db;

import infrastructure.Project;
import infrastructure.Revision;
import infrastructure.interest.JavaFile;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class InsertToDB {

    public static boolean insertProjectToDatabase(Project project) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO projects (owner, repo, url) VALUES (?, ?, ?) ON CONFLICT (owner, repo) DO UPDATE SET url = ?;");
            st.setString(1, project.getOwner());
            st.setString(2, project.getRepo());
            st.setString(3, project.getUrl());
            st.setString(4, project.getUrl());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    public static boolean insertFileToDatabase(Project project, JavaFile jf, Revision currentRevision) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO files (pid, file_path, class_names, sha) VALUES ((SELECT pid FROM projects WHERE repo = '" + project.getRepo() + "' AND owner = '" + project.getOwner() + "'), ?, ?, ?) ON CONFLICT (pid, file_path, sha) DO NOTHING");
            st.setString(1, jf.getPath());
            Array classes = conn.createArrayOf("VARCHAR", jf.getClasses().toArray());
            st.setArray(2, classes);
            st.setString(3, currentRevision.getSha());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    public static boolean insertMetricsToDatabase(Project project, JavaFile jf, Revision currentRevision) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
                    "complexity, dac, dit, fid, interest_eu, interest_in_hours, avg_interest_per_loc, interest_in_avg_loc, sum_interest_per_loc, lcom, mpc, nocc, pid, " +
                    "rfc, sha, size1, size2, wmc, nom, kappa, revision_count, cbo) VALUES (?, ?, ?, ?, (SELECT fid FROM files AS f WHERE file_path = '" + jf.getPath() + "' AND f.sha = '" + currentRevision.getSha() + "'), ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE repo = '" + project.getRepo() + "' AND owner = '" + project.getOwner() + "'), ?, ?, ?, ?, ?, ?, ?, ?, ?)");

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
            st.setDouble(13, jf.getQualityMetrics().getRFC());
            st.setString(14, currentRevision.getSha());
            st.setInt(15, jf.getQualityMetrics().getSIZE1());
            st.setInt(16, jf.getQualityMetrics().getSIZE2());
            st.setDouble(17, jf.getQualityMetrics().getWMC());
            st.setDouble(18, jf.getQualityMetrics().getNOM());
            st.setDouble(19, jf.getKappaValue());
            st.setDouble(20, currentRevision.getRevisionCount());
            st.setDouble(21, jf.getQualityMetrics().getCBO());
            st.executeUpdate();
            st.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    public static boolean insertEmpty(Project project, Revision currentRevision) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement st = conn.prepareStatement("INSERT INTO metrics (classes_num, " +
                    "complexity, dac, dit, fid, interest_eu, interest_in_hours, avg_interest_per_loc, interest_in_avg_loc, sum_interest_per_loc, lcom, mpc, nocc, pid, " +
                    "rfc, sha, size1, size2, wmc, nom, kappa, revision_count, cbo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (SELECT pid FROM projects WHERE repo = '" + project.getRepo() + "' AND owner = '" + project.getOwner() + "'), ?, ?, ?, ?, ?, ?, ?, ?, ?)");

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
            st.setDouble(14, 0.0);
            st.setString(15, currentRevision.getSha());
            st.setInt(16, 0);
            st.setInt(17, 0);
            st.setDouble(18, 0.0);
            st.setDouble(19, 0.0);
            st.setDouble(20, 0.0);
            st.setDouble(21, currentRevision.getRevisionCount());
            st.setDouble(22, 0.0);
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
