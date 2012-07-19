package droid.map;

public class Degrees {
	private boolean isFirstRunLat = true;
	private boolean isFirstRunLong = true;

	public String convertLat(double Lat){
		String result = "";

		int degrees = (int)Lat;
		Lat -= degrees;
		Lat = Lat * 60;
		int minutes = (int)Lat;
		Lat -= minutes;
		Lat = Lat * 60;
		int seconds = (int)Lat;

		if(isFirstRunLat){
			result+="Lat: ";
			isFirstRunLat = false;
		}
		else{
			// The character ´%´ is a command that clears the screen. Howerer,
			// it has a bug
			result+=" Lat: ";
		}
		int length = Integer.toString(degrees).length();
		System.out.println("Degree length " + length);
		while(length != 3){
			result += " ";
			length++;
		}

		result += degrees;
		result += "X";		// The symbol of grades in the microprocessor

		length = Integer.toString(minutes).length();
		if (length == 1)
			result +="0";

		result += minutes;
		result += "'";

		length = Integer.toString(seconds).length();
		if (length == 1)
			result +="0";

		result += seconds;
		result += "\"";

		if(degrees > 0)
			result += "N";
		else
			result += "S";

		return result;
	}

	public String convertLong(double Long){
		String result = "";

		int degrees = (int)Long;
		Long -= degrees;
		Long = Long * 60;
		int minutes = (int)Long;
		Long -= minutes;
		Long = Long * 60;
		int seconds = (int)Long;

		result+=" Lon: ";

		int length = Integer.toString(degrees).length();
		while(length != 3){
			result += " ";
			length++;
		}

		result += degrees;
		result += "X";		// The symbol of grades in the microprocessor

		length = Integer.toString(minutes).length();
		if (length == 1)
			result +="0";

		result += minutes;
		result += "'";

		length = Integer.toString(seconds).length();
		if (length == 1)
			result +="0";

		result += seconds;
		result += "\"";

		if(degrees > 0)
			result += "E";
		else
			result += "W";

		return result;
	}

}
