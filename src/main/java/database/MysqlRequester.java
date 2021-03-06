package database;

import model.*;
import was.google_map_api.GoogleMapApi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by laurel on 03/11/2015.
 */
public class MysqlRequester {

    /**
     * Methode pour executer les requetes qui NE modifient PAS la base de donnees (select, etc)
     *
     * @param requete
     * @return
     */
    private static ResultSet executerRequete(String requete) {
        MysqlConnecter mysqlConnecter = MysqlConnecter.getInstance();
        return mysqlConnecter.executerRequeteInDatabase(requete);
    }

    /**
     * Methode pour executer les requetes qui MODIFIENT la base de donnees (select, etc)
     *
     * @param requete
     * @return
     */
    private static void executerRequeteInsertDeleteUpdate(String requete) {
        MysqlConnecter mysqlConnecter = MysqlConnecter.getInstance();
        mysqlConnecter.executerRequeteInsertDeleteUpdateInDatabase(requete);
    }

    /**
     * Insert les donnees statiques sur les stations (nom, adresse, position etc);
     *
     * @param stations
     */
    public static void insertStationDonneesStatiques(List<Station> stations) {
        Map<String, Integer> listeDesStations = getTousLesIdDesStationsParNom();
        String nom, adresse;
        for (Station s : stations) {
            nom = s.getNom().replace("'", "\\'");
            adresse = s.getAdresse().replace("'", "\\'");
            if (!nom.equals("")) {
                if (listeDesStations.get(nom) == null) {
                    String requete = "INSERT INTO VELO_MALIN.STATIONS (nom, adresse, latitude, longitude, places) " +
                            "values ( '" + nom + "', '" + adresse + "','" + s.getLatitude() + "', '" + s.getLongitude() + "', '" + s.getPlaces() + "')";
                    executerRequeteInsertDeleteUpdate(requete);
                    System.out.println("Creation de la station pour: " + nom);
                } else {
                    System.out.println("Pas de maj pour la station pour: " + nom);
                }
            }
        }
        System.out.println("Insertion des donnees statiques terminees a : " + new Date());
    }

    /**
     * Insert les donnees dynamiques sur les stations (nombre de velos utilises, date de maj etc);
     *
     * @param stations
     * @todo faire la gestion des jours, jours féries et vacance
     */
    public static void insertStationDonneesDynamiques(List<StationDisponibilites> stations) {
        Map<String, Integer> listeDesStations = getTousLesIdDesStationsParNom();
        Map<Integer, Timestamp> listeDerniereDateModifieeParStation = getToutesLesDatesDernieresModificationsParId();
        int idStation;
        Calendar dateMAJ = Calendar.getInstance();
        Calendar dateMajJCDecaux = Calendar.getInstance();
        Timestamp dateMajJCDecauxSql, dateMajSql;
        String nom, adresse;


        for (StationDisponibilites s : stations) {
            nom = s.getNom().replace("'", "\\'");
            if (!nom.equals("")) {
                idStation = listeDesStations.get(nom);
                dateMAJ.setTime(s.getDate_MAJ());
                dateMAJ.set(Calendar.MILLISECOND, 0);
                dateMajJCDecaux.setTime(s.getDateMajJCdecaux());
                dateMajJCDecaux.set(Calendar.MILLISECOND, 0);
                dateMajJCDecauxSql = new Timestamp(dateMajJCDecaux.getTimeInMillis());
                dateMajSql = new java.sql.Timestamp(dateMAJ.getTimeInMillis());

                if (!listeDerniereDateModifieeParStation.get(idStation).equals(dateMajJCDecauxSql)) {
                    String requete = "INSERT INTO VELO_MALIN.STATIONSDISPONIBILITES (id_station, date_MAJ," +
                            " places_occupees, places_disponibles, date_MAJ_JCDecaux, jour_special, vacances_scolaires, jour)" +
                            "values (' " + idStation + "','" + dateMajSql + "','" + s.getPlacesOccupees() + "','" + s.getPlacesDisponibles() + "','" + dateMajJCDecauxSql
                            + "','" + 0 + "','" + 0 + "','" + Jours.LUNDI + "')";
                    executerRequeteInsertDeleteUpdate(requete);
                    System.out.println("Insertion ok pour: " + nom + " a : " + new Date());
                } else {
                    System.out.println("Non Insertion pour: " + nom + " ; date de derniere maj a : " + dateMajJCDecauxSql);
                }
            }
        }
        System.out.println("Insertion des donnees dynamique terminees a : " + new Date());
    }

    private static Map<String, Integer> getTousLesIdDesStationsParNom() {
        String requete = "SELECT * FROM VELO_MALIN.STATIONS";
        ResultSet resultat = executerRequete(requete);
        Map<String, Integer> listeDesStations = new HashMap<String, Integer>();
        Station s;
        String nom;
        try {
            while (resultat.next()) {
                s = new Station();
                int idStation = resultat.getInt("id_station");
                s.setId_station(idStation);
                nom = resultat.getString("nom").replace("'", "\\'");
                s.setNom(nom);
                listeDesStations.put(nom, idStation);
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return listeDesStations;
    }

    private static Map<Integer, Timestamp> getToutesLesDatesDernieresModificationsParId() {
        String requete = "SELECT id_station, max(date_MAJ_JCDecaux) date_MAJ_JCDecaux FROM velo_malin.stationsdisponibilites group by id_station;";
        ResultSet resultat = executerRequete(requete);
        Map<Integer, Timestamp> listeDesStations = new HashMap<Integer, Timestamp>();
        try {
            int idStation;
            Timestamp date;
            while (resultat.next()) {
                idStation = resultat.getInt("id_station");
                date = Timestamp.valueOf(resultat.getString("date_MAJ_JCDecaux"));
                listeDesStations.put(idStation, date);
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return listeDesStations;
    }

    public static List<Integer> getNombreDeVelosSurStationDansInterval(int id_station, Date dateX, Date dateY, List<Jours> jours, String jour_special) {

        StringBuilder sb = new StringBuilder();

        if (jours != null) {
            sb.append(" AND jour in (");
            String delim = "";
            for (Jours jour : jours) {
                sb.append(delim).append("'" + jour + "'");
                delim = ",";
            }
            sb.append(")");
        } else {
            sb.append("");
        }

        if (jour_special != null) {
            sb.append(" AND jour_special=" + jour_special);
        } else {
            sb.append("");
        }

        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String sqlQuery = "SELECT places_occupees FROM velo_malin.stationsdisponibilites WHERE id_station='" + id_station +
                "' AND date_MAJ_JCDecaux BETWEEN '" + datetime.format(dateX) + "' AND '" + datetime.format(dateY) + "'" + sb.toString() + ";";

        ResultSet rs = executerRequete(sqlQuery);
        List<Integer> nbVeloList = new ArrayList<Integer>();

        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
                while (rs.next()) {
                    nbVeloList.add(rs.getInt("places_occupees"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nbVeloList;
    }

    public static List<Integer> getNombreDePlacesSurStationDansInterval(int id_station, Date dateX, Date dateY, List<Jours> jours, String jour_special) {

        StringBuilder sb = new StringBuilder();

        if (jours != null) {
            sb.append(" AND jour in (");
            String delim = "";
            for (Jours jour : jours) {
                sb.append(delim).append("'" + jour + "'");
                delim = ",";
            }
            sb.append(")");
        } else {
            sb.append("");
        }

        if (jour_special != null) {
            sb.append(" AND jour_special=" + jour_special);
        } else {
            sb.append("");
        }

        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String sqlQuery = "SELECT places_disponibles FROM velo_malin.stationsdisponibilites WHERE id_station='" + id_station + "' AND date_MAJ_JCDecaux BETWEEN '" +
                datetime.format(dateX) + "' AND '" + datetime.format(dateY) + "'" + sb.toString() + ";";

        ResultSet rs = executerRequete(sqlQuery);
        List<Integer> nbPlaceList = new ArrayList<Integer>();
        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
                while (rs.next()) {
                    nbPlaceList.add(rs.getInt("places_disponibles"));
                }
            }
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        }

        return nbPlaceList;
    }

    public static int getNombreDePlacesSurStation(int id_station, Date date) {

        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String sqlQuery = "SELECT places_disponibles FROM velo_malin.stationsdisponibilites WHERE id_station='" + id_station + "' AND date_MAJ_JCDecaux <= '" + datetime.format(date) + "' ORDER BY date_MAJ_JCDecaux desc LIMIT 1;";

        ResultSet rs = executerRequete(sqlQuery);
        int nbPlaces = -1;
        try {
            if (!rs.next()) {
                return nbPlaces;
            } else {
                nbPlaces = rs.getInt("places_disponibles");
            }
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        }
        return nbPlaces;
    }

    public static int getNombreDeVelosSurStation(int id_station, Date date) {

        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String sqlQuery = "SELECT places_occupees FROM velo_malin.stationsdisponibilites WHERE id_station='" + id_station + "' AND date_MAJ_JCDecaux <= '" + datetime.format(date) + "' ORDER BY date_MAJ_JCDecaux desc LIMIT 1;";

        ResultSet rs = executerRequete(sqlQuery);
        int nbVelos = -1;
        try {
            if (!rs.next()) {
                return nbVelos;
            } else {
                nbVelos = rs.getInt("places_occupees");
            }
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        }
        return nbVelos;
    }

    public static List<Integer> getNombreDePlacesSurStationAuMoment(int id_station, LocalTime heure, Jours jour, String jour_special) {

        StringBuilder sb = new StringBuilder();
        if (jour_special != null) {
            sb.append(" AND jour_special=" + jour_special);
        } else {
            sb.append("");
        }

        String sqlQuery = "select distinct(DATE_FORMAT(date_MAJ_JCDecaux,'%Y-%m-%d')) as date from velo_malin.stationsdisponibilites where jour='" + jour + "'" + sb.toString() + ";";
        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date;
        ResultSet rs = executerRequete(sqlQuery);

        List<Integer> nbPlaces = new ArrayList<Integer>();
        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
                while (rs.next()) {
                    date = datetime.parse(rs.getString("date") + " " + heure.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    nbPlaces.add(getNombreDePlacesSurStation(id_station, date));
                }
            }
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Bloc catch g�n�r� automatiquement
            e.printStackTrace();
        }

        return nbPlaces;

    }

    public static List<Integer> getNombreDeVelosSurStationAuMoment(int id_station, LocalTime heure, Jours jour, String jour_special) {

        StringBuilder sb = new StringBuilder();
        if (jour_special != null) {
            sb.append(" AND jour_special=" + jour_special);
        } else {
            sb.append("");
        }

        String sqlQuery = "select distinct(DATE_FORMAT(date_MAJ_JCDecaux,'%Y-%m-%d')) as date from velo_malin.stationsdisponibilites where jour='" + jour + "'" + sb.toString() + ";";
        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date;
        ResultSet rs = executerRequete(sqlQuery);

        List<Integer> nbVelos = new ArrayList<Integer>();
        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
                while (rs.next()) {
                    date = datetime.parse(rs.getString("date") + " " + heure.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    nbVelos.add(getNombreDeVelosSurStation(id_station, date));
                }
            }
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Bloc catch g�n�r� automatiquement
            e.printStackTrace();
        }

        return nbVelos;

    }

    public static Station getStation(int id_station) {
        Station station = new Station();

        String sqlQuery = "SELECT * FROM velo_malin.stations WHERE id_station='" + id_station + "';";

        ResultSet rs = executerRequete(sqlQuery);

        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
            }
            if (rs.next()) {
                station.setId_station(rs.getInt("id_station"));
                station.setNom(rs.getString("nom"));
                station.setAdresse(rs.getString("adresse"));
                station.setLatitude(rs.getString("latitude"));
                station.setLongitude(rs.getString("longitude"));
                station.setPlaces(rs.getInt("places"));
            }
        } catch (NumberFormatException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        }

        return station;
    }

    public static List<Station> getToutesLesStations() {
        String sqlQuery = "SELECT * FROM velo_malin.stations;";

        ResultSet rs = executerRequete(sqlQuery);
        List<Station> stations = new ArrayList<>();
        Station station;
        String adresse = "", latitude, longitude;
        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
            }
            while (rs.next()) {
                station = new Station();
                station.setId_station(rs.getInt("id_station"));
                station.setNom(rs.getString("nom"));
                adresse = rs.getString("adresse");
                longitude = rs.getString("longitude");
                latitude = rs.getString("latitude");
                station.setLatitude(latitude);
                station.setLongitude(longitude);
                if (adresse.equals("")) {
                    // adresse = GoogleMapApi.rechercherAdresseParLatLong(new Double(station.getLatitude()), new Double(station.getLongitude()));
                }
                station.setAdresse(adresse);
                station.setPlaces(rs.getInt("places"));
                stations.add(station);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stations;
    }

    public static List<String> getAdressesToutesLesStations() {
        List<Station> stations = MysqlRequester.getToutesLesStations();
        List<String> stationsAdresse = new ArrayList<>();
        String adresse;
        for (Station s : stations) {
            adresse = s.getAdresse();
            if (adresse.equals("")) {
                adresse = GoogleMapApi.rechercherAdresseParLatLong(new Double(s.getLatitude()), new Double(s.getLongitude()));
            }
            stationsAdresse.add(adresse);

        }
        return stationsAdresse;
    }

    public static Map<Station, Double> getStationsProximitees(double latitude, double longitude, int nb_stations, double distance) {

        Map<Station, Double> mapStations = new HashMap<Station, Double>();

        // Merci ? Oleksiy Kovyrin
        String sqlQuery = "SELECT *,3956 * 2 * ASIN(SQRT( POWER(SIN(('" + latitude + "' - abs(latitude)) * pi()/180 / 2),2) + COS('" + latitude + "' * pi()/180 ) * COS( abs(latitude) *  pi()/180) * POWER(SIN(('" + longitude + "' - longitude) *  pi()/180 / 2), 2) )) as distance FROM velo_malin.stations having distance < '" + distance + "' ORDER BY distance limit " + nb_stations + ";";

        ResultSet rs = executerRequete(sqlQuery);
        Station station;

        try {
            if (!rs.next()) {
                return null;
                /*
            	station = new Station();
            	station.setId_station(0);
                station.setNom("");
                station.setAdresse("");
                station.setLatitude("");
                station.setLongitude("");
                station.setPlaces(0);
                mapStations.put(station, 0.0); 
                */
            } else {
                rs.beforeFirst();
                while (rs.next()) {
                    station = new Station();
                    station.setId_station(rs.getInt("id_station"));
                    station.setNom(rs.getString("nom"));
                    station.setAdresse(rs.getString("adresse"));
                    station.setLatitude(rs.getString("latitude"));
                    station.setLongitude(rs.getString("longitude"));
                    station.setPlaces(rs.getInt("places"));
                    mapStations.put(station, rs.getDouble("distance"));
                }
            }
        } catch (SQLException e) {
            // TODO Bloc catch g?n?r? automatiquement
            e.printStackTrace();
        }

        return mapStations;
    }

    public static boolean insertItineraireFavorit(Client client, String lat_depart, String long_dep, String lat_arrivee, String long_arrivee) {

        if (! isItineraireFavoriExist(lat_depart, long_dep, lat_arrivee, long_arrivee))
        {
            String sqlQuery = "INSERT IGNORE INTO velo_malin.itinerairesfavoris (id_client, depart_longitude, depart_latitude, arrive_longitude, arrive_latitude) VALUES('" + 1 + "','"
                    + long_dep + "','" + lat_depart + "','" + long_arrivee + "','" + lat_arrivee + "')";

            executerRequeteInsertDeleteUpdate(sqlQuery);
            return true;
        }
        else
        {
            return false;
        }

    }

    public static Map<Integer, List<Double>> getListeItinerairesFavoris() {

        String sqlQuery = "SELECT depart_longitude,depart_latitude,arrive_longitude,arrive_latitude FROM velo_malin.itinerairesfavoris";
        int cpt = 0;

        ResultSet rs = executerRequete(sqlQuery);
        Map<Integer, List<Double>> liste_itineraires_favoris = new HashMap<Integer, List<Double>>();

        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
            }
            while (rs.next()) {
                List<Double> liste_valeurs = new ArrayList<Double>();
                liste_valeurs.add(rs.getDouble("depart_longitude"));
                liste_valeurs.add(rs.getDouble("depart_latitude"));
                liste_valeurs.add(rs.getDouble("arrive_longitude"));
                liste_valeurs.add(rs.getDouble("arrive_latitude"));
                liste_itineraires_favoris.put(cpt, liste_valeurs);
                cpt++;
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return liste_itineraires_favoris;
    }

    public static Map<Integer, List<Double>> getItineraireFavoriPlusRecent() {

        String sqlQuery = "SELECT depart_longitude,depart_latitude,arrive_longitude,arrive_latitude FROM velo_malin.itinerairesfavoris WHERE id_itinerairefavori=(SELECT MAX(id_itinerairefavori) FROM velo_malin.itinerairesfavoris)";
        int cpt = 0;

        ResultSet rs = executerRequete(sqlQuery);
        Map<Integer, List<Double>> liste_itineraires_favoris = new HashMap<Integer, List<Double>>();

        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
            }
            while (rs.next()) {
                List<Double> liste_valeurs = new ArrayList<Double>();
                liste_valeurs.add(rs.getDouble("depart_longitude"));
                liste_valeurs.add(rs.getDouble("depart_latitude"));
                liste_valeurs.add(rs.getDouble("arrive_longitude"));
                liste_valeurs.add(rs.getDouble("arrive_latitude"));
                liste_itineraires_favoris.put(cpt, liste_valeurs);
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return liste_itineraires_favoris;
    }


    public static int getIdStationparNom(String nom_station) {
        String sqlQuery = "SELECT id_station FROM velo_malin.stations WHERE nom='" + nom_station + "';";

        ResultSet rs = executerRequete(sqlQuery);
        List<Integer> id_station = new ArrayList<Integer>();

        try {
            while (rs.next()) {
                id_station.add(rs.getInt("id_station"));
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }

        int id = id_station.get(0);
        return id;
    }


    public static int getIdStationparCoord(String latitude, String longitude) {
        String sqlQuery = "SELECT id_station FROM velo_malin.stations WHERE latitude='" + latitude + "' AND longitude='" + longitude + "'  ;";

        ResultSet rs = executerRequete(sqlQuery);
        List<Integer> id_station = new ArrayList<Integer>();

        try {
            while (rs.next()) {
                id_station.add(rs.getInt("id_station"));
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }

        int id = id_station.get(0);
        return id;
    }

    /**
     * @param idStation id de la station
     * @param jour      date du jour
     * @return
     */
    public static Map getVeloDisponiblePourUneStationSur24heure(int idStation, Date jour) {
        Calendar date = Calendar.getInstance();
        date.setTime(jour);
        int day = date.get(Calendar.DAY_OF_MONTH);
        int month = date.get(Calendar.MONTH) + 1;
        int year = date.get(Calendar.YEAR);

        String sqlQuery = "SELECT * FROM velo_malin.stationsdisponibilites WHERE id_station='" + idStation + "' AND date_MAJ_JCDecaux LIKE '" + year + "-" + month + "-" + day + "%';";
        ResultSet rs = executerRequete(sqlQuery);

        HashMap<Date, Integer> map = new LinkedHashMap<>();
        try {
            while (rs.next()) {
                map.put(new java.util.Date(rs.getTimestamp("date_MAJ_JCDecaux").getTime()), rs.getInt("places_occupees"));
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return map;
    }

    /**
     * @param idStation id de la station
     * @return
     */
    public static int getNombreDePlaceTotaleSurUneStation(int idStation) {
        String sqlQuery = "SELECT * FROM velo_malin.stations WHERE id_station='" + idStation + "';";
        ResultSet rs = executerRequete(sqlQuery);

        int placesTotales = 0;
        try {
            if (rs.next()) {
                placesTotales = rs.getInt("places");
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return placesTotales;
    }

    public static Map<Integer, Alerte> getListeAlerte() {
        String sqlQuery = "SELECT id_itinerairefavori,heure, id_alerte FROM velo_malin.alertes ;";
        ResultSet rs = executerRequete(sqlQuery);

        Map<Integer, Alerte> liste = new LinkedHashMap<>();
        Alerte alerte;
        try {
            while (rs.next()) {
                int id = rs.getInt("id_alerte");
                alerte = new Alerte();
                alerte.setId(rs.getInt("id_alerte"));
                alerte.setTime(rs.getTimestamp("heure"));
                alerte.setIdItineraireFavori(rs.getInt("id_itinerairefavori"));
                liste.put(id, alerte);
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return liste;
    }

    /**
     * Recupère le temps en miliseconde de la prochaine alerte dans le temps, ainsi que son id
     *
     * @return liste_alerte_mili
     * @auth Pec
     */
    public static List<Integer> getTimeNextAlert() {
        String sqlQuery = "SELECT id_itinerairefavori, heure, NOW() as 'now',DATEDIFF(heure,NOW())*86400000+((hour(heure)*3600000+minute(heure)*60000)-( hour(now())*3600000+minute(now())*60000)) AS 'milirestant'FROM velo_malin.alertes WHERE heure > now() ORDER BY heure  ASC;";
        ResultSet rs = executerRequete(sqlQuery);

        List<Integer> liste_alerte_id_mili = new ArrayList<Integer>();

        try {
            /*
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
            }
            while (rs.next()) {
                liste_alerte_id_mili.add(rs.getInt("id_itinerairefavori"));
                liste_alerte_id_mili.add(rs.getInt("milirestant"));
            }
            */
        } catch (Exception ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return liste_alerte_id_mili;
    }

    /**
     * Renvoie les coordonnées de départ et d'arrivée d'un trajet favori en fonction de son id_favoris
     *
     * @param id_itinerairefavori int
     * @return liste_coord liste(long_dep, lat_dep, long_arr, lat_arr)
     * @auth Pec
     */
    public static List<Double> getCoordDunItineraireFavori(int id_itinerairefavori) {

        String sqlQuery = "SELECT depart_longitude,depart_latitude,arrive_longitude,arrive_latitude FROM velo_malin.itinerairesfavoris" +
                "WHERE id_itinerairefavori = " + id_itinerairefavori + ";";

        ResultSet rs = executerRequete(sqlQuery);
        List<Double> liste_coord = new ArrayList<Double>();

        try {
            if (!rs.next()) {
                return null;
            } else {
                rs.beforeFirst();
            }
            while (rs.next()) {
                liste_coord.add(rs.getDouble("depart_longitude"));
                liste_coord.add(rs.getDouble("depart_latitude"));
                liste_coord.add(rs.getDouble("arrive_longitude"));
                liste_coord.add(rs.getDouble("arrive_latitude"));
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return liste_coord;
    }

    public static int getIdItineraireFavoriIdByLocalisation(String lat_depart, String long_dep, String lat_arrivee, String long_arrivee) {
        String sqlQuery = "SELECT id_itinerairefavori FROM velo_malin.itinerairesfavoris WHERE depart_longitude='" + long_dep + "'AND depart_latitude='" + lat_depart + "'AND arrive_longitude='" + long_arrivee + "'AND arrive_latitude='" + lat_arrivee + "';";
        ResultSet rs = executerRequete(sqlQuery);

        int id_itineraire_favori = 0;
        try {
            if (rs.next()) {
                id_itineraire_favori = rs.getInt("id_itinerairefavori");
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return id_itineraire_favori;
    }


    public static List<Double> getItineraireFavorisLocalisationViaId(int id_favori) {
        String sqlQuery = "SELECT depart_longitude,depart_latitude,arrive_longitude,arrive_latitude FROM velo_malin.itinerairesfavoris WHERE id_itinerairefavori='" + id_favori + "';";
        ResultSet rs = executerRequete(sqlQuery);
        List<Double> liste_valeurs = new ArrayList<Double>();
        try {
            while (rs.next()) {
                liste_valeurs.add(0, rs.getDouble("depart_longitude"));
                liste_valeurs.add(1, rs.getDouble("depart_latitude"));
                liste_valeurs.add(2, rs.getDouble("arrive_longitude"));
                liste_valeurs.add(3, rs.getDouble("arrive_latitude"));
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(MysqlConnecter.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            System.out.println("Erreur: " + ex);
        }
        return liste_valeurs;
    }


    public static void supprimerItinerairesFavoris(String lat_station_depart, String long_station_depart, String lat_station_arrivee, String long_station_arrivee) {
        // supression des favoris
        String sqlQuery = "DELETE FROM velo_malin.itinerairesfavoris WHERE depart_latitude='" + lat_station_depart + "' AND depart_longitude='" + long_station_depart + "' AND arrive_longitude='" + long_station_arrivee + "' AND arrive_latitude='" + lat_station_arrivee + "' ;";
        executerRequeteInsertDeleteUpdate(sqlQuery);

        // suppression des alertes liés à ce favoris
        int idItineraireFavori = MysqlRequester.getIdItineraireFavoriIdByLocalisation(lat_station_depart, long_station_depart, lat_station_arrivee, long_station_arrivee);
        supprimerAlerteByItiFavo(idItineraireFavori);
    }

    public static boolean isItineraireFavoriExist(String lat_station_depart, String long_station_depart, String lat_station_arrivee, String long_station_arrivee) {
        String sqlQuery = "Select id_itinerairefavori FROM velo_malin.itinerairesfavoris WHERE depart_latitude='" + lat_station_depart + "' AND depart_longitude='" + long_station_depart + "' AND arrive_longitude='" + long_station_arrivee + "' AND arrive_latitude='" + lat_station_arrivee + "' ;";
        ResultSet rs = executerRequete(sqlQuery);
        try {
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {

        }
        return false;
    }

    public static void supprimerAlerteByItiFavoEtHeure(int idItineraireFavoris, Date heure) {
        String sqlQuery = "DELETE FROM velo_malin.alertes WHERE id_itineraireFavori = '" + idItineraireFavoris + "' and heure = '" + heure + "' ;";
        executerRequeteInsertDeleteUpdate(sqlQuery);
    }

    public static void supprimerAlerteByItiFavo(int idItineraireFavoris) {
        String sqlQuery = "DELETE FROM velo_malin.alertes WHERE id_itineraireFavori = '" + idItineraireFavoris + ";";
        executerRequeteInsertDeleteUpdate(sqlQuery);
    }

    public static boolean isAlerteExist(Date Heure, int id_itineraire_favori)
    {
        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sqlQuery = "Select * from  velo_malin.alertes where id_itineraireFavori = '" + id_itineraire_favori + "' and heure = '" + datetime.format(Heure) + "'";
        ResultSet rs = executerRequete(sqlQuery);
        try {
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {

        }
        return false;
    }

    public static boolean insertAlerte(Date Heure, int id_itineraire_favori) {
        SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (isAlerteExist( Heure, id_itineraire_favori))
        {
            return false;
        }
        else
        {
            String sqlQuery = "INSERT INTO velo_malin.alertes(id_client,id_itinerairefavori, heure) VALUES ( 1,'" + id_itineraire_favori + "','" + datetime.format(Heure) + "')";
            executerRequeteInsertDeleteUpdate(sqlQuery);
            return true;
        }
    }
}
