import java.util.Random;

public class Main {
	
	private static final int NUMBER_OF_USERS = 1000;
	private static final double MIN_LAT = 46.0, MIN_LNG = 6.0, MAX_LAT = 48.0, MAX_LNG = 10.5;
	
	private static String[] usersID = new String[NUMBER_OF_USERS];
	private static double[] usersLat = new double[NUMBER_OF_USERS];
	private static double[] usersLng = new double[NUMBER_OF_USERS];
	private static int latRange = (int) (MAX_LAT - MIN_LAT + 1), lngRange = (int) (MAX_LNG - MIN_LNG + 1);

	public static void main(String[] args) {
		initUsers();
	}
	
	public static void initUsers() {
		Random r = new Random();
		
		// generate user IDs
        StringBuilder s1 = null;
        for (int i = 0; i < usersID.length; i++) {
	        s1 = new StringBuilder(Integer.toHexString(r.nextInt()));
	        while (s1.length() < 8)
	        	s1.append('0');
	        s1.append(Integer.toHexString(r.nextInt()));
	        while (s1.length() < 16)
	        	s1.append('0');
	        usersID[i] = s1.toString();
        }
        
        // generate location
        for (int i = 0; i < usersLat.length; i++) {
        	double deg = r.nextDouble();
        	deg *= (double) r.nextInt(latRange);
        	deg += (double) MIN_LAT;
	        usersLat[i] = deg;
	        
	        deg = r.nextDouble();
        	deg *= (double) r.nextInt(lngRange);
        	deg += (double) MIN_LNG;
	        usersLng[i] = deg;
        }
        
        // debug output
        for (int i = 0; i < usersID.length; i++)
        	 System.out.println("user " + usersID[i] + "\tat " + usersLng[i] + "\t| " + usersLat[i]);
	}

}
