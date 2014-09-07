package se.bitcraze.crazyfliecontrol;

import android.app.Application;
import android.content.Context;

public class CrazyflieApp extends Application {
	Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
	}
}
