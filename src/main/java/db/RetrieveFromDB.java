package db;

import data.Globals;
import infrastructure.interest.JavaFile;
import infrastructure.interest.QualityMetrics;

import java.sql.*;
import java.util.*;

import static data.Globals.*;

public class RetrieveFromDB {

    public static boolean ProjectExistsInDatabase() {
        String owner = Globals.getProjectOwner();
        String repoName = getProjectRepo();
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT owner, repo FROM projects WHERE owner = ? AND repo = ?");
            st.setString(1, owner);
            st.setString(2, repoName);
            ResultSet resultSet = st.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (Exception ignored) { System.out.println(ignored); }
        return false;
    }

    public static List<String> getExistingCommitIds() {
        String owner = Globals.getProjectOwner();
        String repoName = getProjectRepo();
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT DISTINCT sha FROM metrics WHERE pid = (SELECT pid FROM projects WHERE owner = ? AND repo = ?)");
            st.setString(1, owner);
            st.setString(2, repoName);
            ResultSet resultSet = st.executeQuery();
            List<String> shaList = new ArrayList<>();
            while (resultSet.next())
                shaList.add(resultSet.getString("sha"));
            return shaList;
        } catch (SQLException e) {
            System.out.println(e);
        }
        return null;
    }

    public static Integer getLastVersionNum() {
        String owner = Globals.getProjectOwner();
        String repoName = getProjectRepo();
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT revision_count FROM metrics WHERE pid = (SELECT pid FROM projects WHERE owner = ? AND repo = ?) ORDER BY revision_count DESC LIMIT 1");
            st.setString(1, owner);
            st.setString(2, repoName);
            ResultSet resultSet = st.executeQuery();
            int revisionCount = 0;
            while (resultSet.next())
                revisionCount = resultSet.getInt("revision_count");
            return revisionCount;
        } catch (SQLException e) {
            System.out.println(e);
        }
        return 0;
    }

    public static String getFilePathById(Integer fid) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT file_path FROM files WHERE fid = ?");
            st.setInt(1, fid);
            ResultSet resultSet = st.executeQuery();
            String filePath = "";
            while (resultSet.next())
                filePath = resultSet.getString("file_path");
            return filePath;
        } catch (SQLException e) {
            System.out.println(e);
        }
        return null;
    }

    public static Set<String> getClassNamesByFileId(Integer fid) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT class_names FROM files WHERE fid = ?");
            st.setInt(1, fid);
            ResultSet resultSet = st.executeQuery();
            Array classesArr = null;
            while (resultSet.next())
                classesArr = resultSet.getArray("class_names");
            return Set.of((String[]) (classesArr != null ? classesArr.getArray() : new HashSet<>()));
        } catch (SQLException e) {
            System.out.println(e);
        }
        return null;
    }

    public static void retrieveJavaFiles() {
        String owner = Globals.getProjectOwner();
        String repoName = getProjectRepo();
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT revision_count, m.sha AS sha, classes_num, complexity, dac, dit, interest_eu, interest_in_hours, avg_interest_per_loc, interest_in_avg_loc, sum_interest_per_loc, lcom, mpc, nocc, old_size1, rfc, size1, size2, wmc, nom, kappa, cbo, file_path, class_names FROM metrics m JOIN files f ON m.fid = f.fid WHERE m.pid = (SELECT pid FROM projects WHERE owner = ? AND repo = ?) AND revision_count = (SELECT MAX(revision_count) FROM metrics)");
            st.setString(1, owner);
            st.setString(2, repoName);
            ResultSet resultSet = st.executeQuery();
            while (resultSet.next()) {
                String sha = resultSet.getString("sha");
                String filePath = resultSet.getString("file_path");
                Integer classesNum = resultSet.getInt("classes_num");
                Double complexity = resultSet.getDouble("complexity");
                Integer dac = resultSet.getInt("dac");
                Integer dit = resultSet.getInt("dit");
                Double interestInEuros = resultSet.getDouble("interest_eu");
                Double interestInHours = resultSet.getDouble("interest_in_hours");
                Double avgInterestPerLoc = resultSet.getDouble("avg_interest_per_loc");
                Double interestInAvgLoc = resultSet.getDouble("interest_in_avg_loc");
                Double sumInterestPerLoc = resultSet.getDouble("sum_interest_per_loc");
                Double lcom = resultSet.getDouble("lcom");
                Double mpc = resultSet.getDouble("mpc");
                Integer nocc = resultSet.getInt("nocc");
                Integer oldSize1 = resultSet.getInt("old_size1");
                Double rfc = resultSet.getDouble("rfc");
                Integer size1 = resultSet.getInt("size1");
                Integer size2 = resultSet.getInt("size2");
                Double wmc = resultSet.getDouble("wmc");
                Double nom = resultSet.getDouble("nom");
                Double cbo = resultSet.getDouble("cbo");
                Double kappa = resultSet.getDouble("kappa");
                Array classesArr = resultSet.getArray("class_names");
                Set<String> classes = Set.of((String[]) (classesArr != null ? classesArr.getArray() : new HashSet<>()));
                Globals.getJavaFiles().add(new JavaFile(filePath, new QualityMetrics(sha, classesNum, complexity, dit, nocc, rfc, lcom, wmc, nom, mpc, dac, oldSize1, cbo, size1, size2), interestInEuros, interestInHours, avgInterestPerLoc, interestInAvgLoc, sumInterestPerLoc, kappa, classes));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
}
