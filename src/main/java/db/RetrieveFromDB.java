package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RetrieveFromDB {

    public static boolean URLExistsInDatabase(String url) {
        String newURL = preprocessURL(url);
        String owner = getRepoOwner(newURL);
        String repoName = getRepoName(newURL);
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT repo_name, owner FROM projects WHERE owner = '?' AND repo_name = '?'");
            st.setString(1, owner);
            st.setString(2, repoName);
            ResultSet resultSet = st.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (Exception ignored) {}
        return false;
    }

    private static String preprocessURL(String url) {
        String newURL = url;
        if (url.endsWith(".git/"))
            newURL = url.replace(".git/", "");
        if (newURL.endsWith(".git"))
            newURL = newURL.replace(".git", "");
        if (newURL.endsWith("/"))
            newURL = newURL.substring(0, url.length() - 1);
        return newURL;
    }

    private static String getRepoOwner(String url) {
        String[] urlSplit = url.split("/");
        return urlSplit[urlSplit.length - 2].replaceAll(".*@.*:", "");
    }

    private static String getRepoName(String url) {
        String[] urlSplit = url.split("/");
        return urlSplit[urlSplit.length - 1];
    }
}
