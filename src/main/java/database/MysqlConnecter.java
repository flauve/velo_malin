package database;


import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by flauve on 2015-10-14.
 */
public class MysqlConnecter {

    private static MysqlConnecter instance = null;

    private static Connection con = null;
    private static Statement st = null;
    private static ResultSet rs = null;
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    

    private String url = "jdbc:mysql://master.granetlucas.fr:80";
    private String user = "velo_malin";
    private String password = "velo_mal1n";

    /* Local Database
    private String url = "jdbc:mysql://localhost";
    private String user = "root";
    private String password = "root";
	*/
    
    private MysqlConnecter() {
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(url, user, password);
            st = con.createStatement();
            rs = st.executeQuery("SELECT VERSION()");

            if (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur");
        }
        catch( Exception e)
        {

        }
    }

    protected static MysqlConnecter getInstance() {
        if (instance == null) {
            instance = new MysqlConnecter();
        }
        return instance;
    }


    /**
     * Methode pour executer les requetes qui NE modifient PAS la base de donnees (select, etc)
     *
     * @param requete
     * @return
     */
    protected ResultSet executerRequeteInDatabase(String requete) {
        try {
            st = con.createStatement();
            rs = st.executeQuery(requete);

        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return rs;
    }

    /**
     * Methode pour executer les requetes qui MODIFIENT la base de donnees (select, etc)
     *
     * @param requete
     * @return
     */
    protected void executerRequeteInsertDeleteUpdateInDatabase(String requete) {
        try {
            st = con.createStatement();
            st.executeUpdate(requete);

        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
    }
}
