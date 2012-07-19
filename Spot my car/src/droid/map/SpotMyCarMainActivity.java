package droid.map;

import java.util.List;

import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/*
 * The original idea was to show the route from your position to your car. However, Google blocked that option from their mapView
 * for some reason. So, we just show the user position and the car position.
 */

public class SpotMyCarMainActivity extends MapActivity{

	// For debugging purposes
	private static final String TAG = "Spot my car";
	
	// UI components
	private LinearLayout linearLayout;
	private MapView mapView;
	private List<Overlay> mapOverlays;
	private Drawable carFlag;
	private ItemizedOverlay itimizedOverlay;
	private MyLocationOverlay locationOverlay;
	private ImageButton carButton;
	private ImageButton rulerButton;
	private ImageButton blueButton;

	// Map and GPS components
	public GPSLocation Locator;
	private LocationManager locationManager;
	private MapController MC;
	private GeoPoint geoPoint;
	public Location tempLocation;
	private Location carLocation;
	private Location dummy;
	int REQUEST_CODE = 0;
	int latitude;
	int longitude;
	float MIN_ACCURACY = 10f;
	long MAX_TIME = 30000;
	
	//Message types sent from the BluetoothConnectService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothConnectService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the connection services
	private BluetoothConnectService mConnectService = null;

	// Status of the app.
	boolean devicePaired = false;
	boolean isConnectionLost = false;
	boolean isCarSet = false;


	private Degrees degrees;
	// In onCreate we take care of initiating all the variables, enabling the GPS and TODO the Bluetooth and 
	// centering the map in America. Kind of cliche, I know
	@Override
	public void onCreate(Bundle savedInstanceState) {

		degrees = new Degrees();
		// Set up the base UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mapView = (MapView) findViewById(R.id.mapview);
		carButton = (ImageButton) findViewById(R.id.carButton);
		rulerButton = (ImageButton) findViewById(R.id.rulerButton);
		blueButton = (ImageButton) findViewById(R.id.blueButton);
		carFlag = this.getResources().getDrawable(R.drawable.pin_location);	

		// Getting the GPS ready
		locationManager =(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Locator = new GPSLocation(this, locationManager);

		// Getting the Bluetooth ready
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// TODO Sets the use of the ruler button, that measures distance
		// between the user and the car
		OnClickListener rulerListener = new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				/*if(isCarSet && dummy!=null){
					float[] results = new float[3];
					Location.distanceBetween(	dummy.getLatitude(), dummy.getLongitude(), 
							carLocation.getLatitude(), carLocation.getLongitude(), results);
					Double distance = new Double(results[0]);
					String distanceBetween = distance.toString();
					showMessage("La distancia es " + distance, Toast.LENGTH_LONG);
				}
				else{
					showMessage("No se pudo calcular la distancia", Toast.LENGTH_SHORT);
				}*/
				String sLatitude = degrees.convertLat(120.607);
				String sLongitude = degrees.convertLong(113.4168);
				
				Log.d(TAG, "Enviando coordenadas");
				
				mConnectService.sendMessage(sLatitude);
				mConnectService.sendMessage(sLongitude);

			}

		};
		rulerButton.setOnClickListener(rulerListener);

		// Moves the map to the user location so he can see the car.
		// TODO as now it only centers the map on the car
		OnClickListener carListener = new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (Locator.isGpsEnabled() && carLocation != null){
					showMessage("Centrando", Toast.LENGTH_SHORT);
					locationOverlay.enableMyLocation();

					int carLatitude = (int) (carLocation.getLatitude()*1e6);
					int carLongitude = (int) (carLocation.getLongitude()*1e6);

					GeoPoint carPoint = new GeoPoint(carLatitude, carLongitude);
					MC.setCenter(carPoint);
				}
				else{
					showMessage("No hay ninguna dirección guardada.", Toast.LENGTH_LONG);
				}
			}
		};
		carButton.setOnClickListener(carListener);

		// This button simulates the Bluetooth pairing and disconnection of devices
		OnClickListener blueListener = new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (!devicePaired && !isConnectionLost){
					showMessage("Dispositivo emparejado", Toast.LENGTH_LONG);
					devicePaired = true;
				}

				else{

					if(devicePaired && !isConnectionLost){
						isConnectionLost = true;
					}

					else{

						if(devicePaired && isConnectionLost){
							showMessage("Desparejando dispositivos", Toast.LENGTH_SHORT);
							devicePaired = false;
							isConnectionLost = false;
						}
					}
				}
			}
		};
		blueButton.setOnClickListener(blueListener);

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			showMessage("Bluetooth no disponible", Toast.LENGTH_LONG);
			finish();
			return;
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			askToEnableBluetooth();
		}
 
		if (!Locator.isGpsEnabled()){
			askToEnableGPS();
		}

		setMap();
	}	

	public void onStart(){
		super.onStart();

		// If we could got a last location, zoom the map to it
		if(latitude!=0 && longitude!=0){
			Log.i(TAG, "There was a last known position. Zooming to it");
			GeoPoint last_location = new GeoPoint(latitude,longitude);
			MC.setZoom(13);
			MC.animateTo(last_location);
		}	

		if(!isCarSet)
			new carLocationUpdate().execute(null);

		if (!mBluetoothAdapter.isEnabled()) {
			askToEnableBluetooth();
		} else {
			if (mConnectService == null) 
				setupChat();
		}

	}

	public void onResume(){
		super.onResume();
		if (isCarSet)
			locationOverlay.enableMyLocation();
		if (mConnectService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't started already
			if (mConnectService.getState() == BluetoothConnectService.STATE_NONE) {
				// Start the Bluetooth chat services
				mConnectService.start();
			}
		}
	}

	public void onPause(){
		super.onPause();
		if (isCarSet)
			locationOverlay.disableMyLocation();
	}
	// Required by MapView.
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	 @Override
	    public void onDestroy() {
	        super.onDestroy();
	        // Stop the Bluetooth and GPS services
	        if (mConnectService != null) mConnectService.stop();
	        if (Locator.isGpsEnabled()) Locator.stopGPS();
	    }

	// This method checks if the user decided to activate GPS. If not, continues with a warning, and telling
	// the user he can activate the GPS at any time he wants
	private void askToEnableGPS() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Esta aplicación requiere de GPS. ¿Deseas activarlo?")
		.setCancelable(false)
		.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivityForResult(intent, REQUEST_CODE);
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				showMessage("Puedes activar el GPS en el menú más adelante", Toast.LENGTH_LONG);
			}
		}); 

		final AlertDialog NoGPSAlert = builder.create();
		NoGPSAlert.show();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
             setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }

		if (!Locator.isGpsEnabled()){
			showMessage("Puedes activar el GPS en el menú más adelante", Toast.LENGTH_LONG);
		}
	}



	// Stuff for the menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.connect_device:
			 serverIntent = new Intent(this, DeviceListActivity.class);
	         startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.GPS:
			if (!Locator.isGpsEnabled()){
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivityForResult(intent, REQUEST_CODE);
			}
			else{
				showMessage("Tu GPS ya esta activado", Toast.LENGTH_SHORT);
			}

			return true;
		case R.id.discoverable:
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	public void setCarLocation(Location location){

		if (location != null){
			carLocation = location;
			isCarSet = true;
			int carLatitude = (int) (carLocation.getLatitude()*1e6);
			int carLongitude = (int) (carLocation.getLongitude()*1e6);
			String sLatitude = degrees.convertLat((carLocation.getLatitude()*1e6));
			String sLongitude = degrees.convertLong((carLocation.getLongitude()*1e6));
			
			Log.d(TAG, "Enviando coordenadas");
			
			mConnectService.sendMessage(sLatitude);
			mConnectService.sendMessage(sLongitude);
			
			GeoPoint carGeoPoint = new GeoPoint(carLatitude, carLongitude);
			OverlayItem overlayitem = new OverlayItem(carGeoPoint, "", "");
			itimizedOverlay = new ItemizedOverlay(carFlag);
			itimizedOverlay.addOverlay(overlayitem);
			mapOverlays.add(itimizedOverlay);

			Log.d(TAG, "Accuracy: " + location.getAccuracy() + " Latitude " + location.getLatitude());

			showMessage("Dirección guardada", Toast.LENGTH_SHORT);
		}
		else{
			Log.d(TAG, "The location was null");
			
			// This thread is causing troubles
			mConnectService.sendMessage("NoLocation");
			isCarSet = false;
			//showMessage("La dirección no pudo ser conseguida", Toast.LENGTH_LONG);
		}
	}

	/*
	 * A little explication:
	 * AsyncTask is Android's approach to threads. The class handles threas
	 * automatically just by calling the execute method. The gerenics are the
	 * Parameters, Progress and Results, in that order
	 * 
	 * This AsyncTask keeps listening for when the Blueetooth device is paired
	 * and the connection is Lost. At debugg level this is simulated with the
	 * Bluetooth button, but this works when the paired device lost connection
	 */
	private class carLocationUpdate extends AsyncTask<Void, Void, Location>{
		private Location location;
		private Location firstLocation;
		boolean activeGPS;

		// It's not clear why, but I can't call Locator without a handler
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(final Message msgs) {
				showMessage("Conexión perdida", Toast.LENGTH_SHORT);
				Locator.startGPS();
			}
		};

		@Override
		protected void onPreExecute(){
			// We provide a quasi-null location so we don't get any errors ahead
			location = new Location(LocationManager.GPS_PROVIDER);
			location.setAccuracy(200);
			activeGPS = Locator.isGpsEnabled();
		}

		@Override
		protected Location doInBackground(Void... params) {

			Log.i(TAG, "doing in Background");

			while(!devicePaired || !isConnectionLost || !activeGPS)
			{
				// Keep waiting until all conditions are met
			}	

			new Thread(new Runnable() {
				@Override
				public void run() {
					handler.sendEmptyMessage(1);
				}
			}).start();

			Log.d(TAG, "fetching location");
			Time actualTime = new Time("UTC");
			Time runTime = new Time("UTC");
			actualTime.setToNow();
			runTime.setToNow();
			long startTime = actualTime.toMillis(false);
			firstLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			do{
				tempLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

				if (tempLocation !=null){

					if (tempLocation.getAccuracy() < location.getAccuracy()){
						System.out.println(tempLocation.toString());
						location = tempLocation;
					}
				}

				runTime.setToNow();				
			}
			while ((location.getAccuracy()>=MIN_ACCURACY || 
					location.getAccuracy() == 0.0 || 
					tempLocation != firstLocation)
					&&
					(runTime.toMillis(false)-startTime)<MAX_TIME);

			if(tempLocation == firstLocation){
				location = null;
				return location;
			}

			return location;

		}


		// onPostExecute recives as parameter the result given by the method
		// doInBackground
		@Override
		protected void onPostExecute(Location result) {
			Locator.stopGPS();
			if(location == null)
				System.out.println("Dirección nula");
			setCarLocation(location);
		}

	}

	private void showMessage(String message, int duration){
		Toast.makeText(this, message, duration).show();
	}

	private void askToEnableBluetooth(){
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// Initialize the BluetoothConnectService to perform bluetooth connections
		mConnectService = new BluetoothConnectService(this, mHandler);
	}
	
	 private void ensureDiscoverable() {
	        Log.d(TAG, "ensure discoverable");
	        if (mBluetoothAdapter.getScanMode() !=
	            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 400);
	            startActivity(discoverableIntent);
	        }
	    }
	 
	 private final void setStatus(int resId) {
	    	
		    System.out.println("IMPRIME: "+ resId);
	 }
	 
	  private final void setStatus(CharSequence subTitle) {
          
	    	 System.out.println("IMPRIME: "+subTitle);	
	    }
	  
	  // The Handler that gets information back from the BluetoothConnectService
	    private final Handler mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	            	Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothConnectService.STATE_CONNECTED:
	                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
	                    System.out.println("IMPRIME: "+getString(R.string.title_connected_to, mConnectedDeviceName));
	                    break;
	                case BluetoothConnectService.STATE_CONNECTING:
	                    setStatus(R.string.title_connecting);
	                    break;
	                case BluetoothConnectService.STATE_LISTEN:
	                case BluetoothConnectService.STATE_NONE:
	                    setStatus(R.string.title_not_connected);
	                    break;
	                }
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "Conectado a "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                break;
	            }
	        }
	    };
	    
	    private void connectDevice(Intent data, boolean secure) {
	        // Get the device MAC address
	        String address = data.getExtras()
	            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
	        // Get the BluetoothDevice object
	        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	        // Attempt to connect to the device
	      mConnectService.connect(device, secure);
	    }
	    
	    private void setMap(){
	    	//This point centers the map at apox. Mexico City
			GeoPoint centerPoint = new GeoPoint(19240000,-99420000);
			
			// We get the users last known location so we can animate to that location each time we start the app. Of course,
			// if there is one last known location
			dummy = Locator.getLocation();
			if (dummy != null){
				latitude = (int) (dummy.getLatitude()*1e6);
				longitude = (int) (dummy.getLongitude()*1e6);
			}

			MC = mapView.getController();
			MC.setCenter(centerPoint);
			
			// With this zoom you can see Mexico centered on the screen
			MC.setZoom(4);

			// Sets the items thas float over the map.
			mapOverlays = mapView.getOverlays();

			locationOverlay = new MyLocationOverlay(this, mapView);
			mapOverlays.add(locationOverlay);
			
	    }

}