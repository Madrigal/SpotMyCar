package droid.map;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

public class GPSLocation implements LocationListener{

	Context context;
	LocationManager locationManager;
	LocationListener locationListener;
	Location location = null;
	private static final String TAG = "Where's my car?";
	
	public GPSLocation(Context HelloMapViewContext, LocationManager locationManager){
		context = HelloMapViewContext;
		this.locationManager = locationManager;
		location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}	
	
	
	public void startGPS(){
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				0, 
				0, 
				this);
	}
	
	public void stopGPS(){
		locationManager.removeUpdates(this);
	}
	
	public boolean isGpsEnabled(){
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			return true;
		else
			return false;		
	}
	public Location getLocation(){
		return location;
	}
	
	
	@Override
	public void onLocationChanged(Location arg0) {
		// do nothing
	}

	@Override
	public void onProviderDisabled(String arg0) {
		Log.i(TAG, "Provider disabled");
	}

	@Override
	public void onProviderEnabled(String arg0) {
		Log.i(TAG, "Provider enabled");

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		Log.i(TAG, "Status changed");
	}

}
