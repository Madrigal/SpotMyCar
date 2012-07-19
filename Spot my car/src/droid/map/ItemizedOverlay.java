package droid.map;

import java.util.ArrayList;

import com.google.android.maps.OverlayItem;

import android.graphics.drawable.Drawable;
/**
 * It holds and takes care of displaying things in front of a MapView
 *
 */
public class ItemizedOverlay extends com.google.android.maps.ItemizedOverlay {
	private ArrayList<OverlayItem> ItemsOverlayed = new ArrayList<OverlayItem>();
	
	public ItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		// TODO Auto-generated constructor stub
	}
	
	public void addOverlay (OverlayItem Item){
		ItemsOverlayed.add(Item);
		populate();
	}

	@Override
	public int size() {
		return ItemsOverlayed.size();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return ItemsOverlayed.get(i);
	}
}
