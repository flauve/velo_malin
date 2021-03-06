import database.MysqlRequester;
import model.Client;
import model.Jours;
import recherche.StatistiquesStation;
import was.google_map_api.GoogleMapApi;

import java.time.LocalTime;
import java.util.*;

/**
 * Created by florian on 2015-09-29.
 */
public class VeloMalinTest {

    public static void main(String[] arg) throws Exception {

//		boolean resultat_test;
//		
//		Client test_client;
//		test_client = new Client();
//		//Client test_client_actuel;	
//		Station test_station;
//		test_station = new Station();
//		Station station_depart = new Station();
//		Station station_arrivee = new Station();
//		
//		
//		test_station.setId_station(2);
//		test_client.setId_client(5);
//		station_depart.setId_station(10);
//		station_arrivee.setId_station(20);
//		
//		//test_client_actuel.getId_client();
//		
//		MysqlConnecter mysql = new MysqlConnecter();
//	
//		//test de base g�n�ral
//		resultat_test = mysql.insertStationFavorite(test_client,test_station);
//		resultat_test = mysql.insertItineraireFavorit(test_client,station_depart,station_arrivee);
//		
//		//test plus r�aliste : avec id r�cup�r� du client actuel
//		//resultat_test = mysql.insertStationFavorite(test_client_actuel,test_station);

        Calendar cal = Calendar.getInstance();
        cal.set(2015, 10 - 1, 28, 13, 00, 00);
        Date date = cal.getTime();

        List<Date> dateXY = StatistiquesStation.calculIntervalTemps(date, 5);
		int moyVelo = StatistiquesStation.getMoyenneNbVelosSurStation(2470, dateXY.get(0), dateXY.get(1), null, null);
		System.out.println(moyVelo +" velos le "+ date);

        int moyPlace = StatistiquesStation.getMoyenneNbPlacesSurStation(2470, dateXY.get(0), dateXY.get(1), null, null);
        System.out.println(moyPlace + " places vers " + date);
		
		int places = MysqlRequester.getNombreDePlacesSurStation(2903, date);
		System.out.println(places +" places le "+ date);

        System.out.println(MysqlRequester.getStation(2470));

		System.out.println(MysqlRequester.getStationsProximitees(45.783791, 4.868972, 3, 0.5));

//		System.out.println(MysqlRequester.getToutesLesStations().size());
//		for(Station object: MysqlRequester.getToutesLesStations()){
//			System.out.println(object);


//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        Map<Date, Integer> map = MysqlRequester.getVeloDisponiblePourUneStationSur24heure(2546, sdf.parse("2015-10-20"));
//        for (Map.Entry<Date, Integer> entry : map.entrySet()) {
//            System.out.println(entry.getKey() + "/" + entry.getValue());
//        }
//        System.out.println("NB de places " + MysqlRequester.getNombreDePlaceTotaleSurUneStation(2546));
        
        LocalTime heure = LocalTime.of(13, 00, 00);
        System.out.println(MysqlRequester.getNombreDePlacesSurStationAuMoment(2470, heure, Jours.MARDI, null));
        System.out.println(MysqlRequester.getNombreDeVelosSurStationAuMoment(2470, heure, Jours.MARDI, null));
        String adresse = "36 rue des antonins Résidence les tamaris villeurbanne";
        System.out.println("rechercher lat / long par adresse : " + GoogleMapApi.rechercherLatLongParAdresse(adresse).getLatitude() + " " + GoogleMapApi.rechercherLatLongParAdresse(adresse).getLongitude());
        System.out.println("rechercher adresse + lat / long: " + GoogleMapApi.rechercherAdresseParLatLong(45.7797430, 4.8790385));
        System.out.println("Temps de trajet pour 10 km : " + StatistiquesStation.getTempsDeTrajet(45.7797430, 4.8790385, 47.3590900, 3.3852100));

        // test itineraire favori

        System.out.println("itineraire fav exist : " + MysqlRequester.isItineraireFavoriExist("4.8047449", "45.780722", "4.4379189", "45.6917192"));
        System.out.println("itineraire fav exist pas: " + MysqlRequester.isItineraireFavoriExist("4.8047449", "45.780723", "4.4379189", "45.6917192"));

        MysqlRequester.insertItineraireFavorit(new Client(), "1.1", "2.2", "3.3", "4.4");
        int idItineraireFavori = MysqlRequester.getIdItineraireFavoriIdByLocalisation("1.1", "2.2", "3.3", "4.4");
        date = new Date();
        MysqlRequester.insertAlerte(date, idItineraireFavori);
        System.out.println("alerte existe : " + MysqlRequester.isAlerteExist(date, idItineraireFavori));
        System.out.println("alerte existe pas : " + MysqlRequester.isAlerteExist(new Date(), idItineraireFavori));

    }

}


