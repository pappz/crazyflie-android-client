/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2013 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyfliecontrol;

import java.io.IOException;
import java.util.Locale;

import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.GamepadController;
import se.bitcraze.crazyfliecontrol.controller.GyroscopeController;
import se.bitcraze.crazyfliecontrol.controller.IController;
import se.bitcraze.crazyfliecontrol.controller.TouchController;
import se.bitcraze.crazyflielib.ConnectionAdapter;
import se.bitcraze.crazyflielib.ConnectionListener;
import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.Link;
import se.bitcraze.crazyflielib.crtp.CommanderPacket;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

public class MainActivity extends Activity implements ConnectionListener {

	private static final String TAG = "CrazyflieControl";

	private DualJoystickView mDualJoystickView;
	private FlightDataView mFlightDataView;

	private SharedPreferences mPreferences;

	private IController mController;
	private GamepadController mGamepadController;

	private boolean mDoubleBackToExitPressedOnce = false;

	private Controls mControls;

	private CrazyflieApp crazyflieApp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		crazyflieApp = (CrazyflieApp) getApplication();

		setDefaultPreferenceValues();

		mControls = new Controls(this, mPreferences);
		mControls.setDefaultPreferenceValues(getResources());

		// Default controller
		mDualJoystickView = (DualJoystickView) findViewById(R.id.joysticks);
		mController = new TouchController(mControls, this, mDualJoystickView);

		// initialize gamepad controller
		mGamepadController = new GamepadController(mControls, this,
				mPreferences);
		mGamepadController.setDefaultPreferenceValues(getResources());

		mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);
	}

	private void setDefaultPreferenceValues() {
		// Set default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		// Initialize preferences
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	private void checkScreenLock() {
		boolean isScreenLock = mPreferences.getBoolean(
				PreferencesActivity.KEY_PREF_SCREEN_ROTATION_LOCK_BOOL, false);
		if (isScreenLock || mController instanceof GyroscopeController) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			try {
				crazyflieApp.linkConnect();
			} catch (IllegalStateException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.preferences:
			Intent intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		crazyflieApp.addConnectionListener(this);
		// TODO: improve
		mControls.setControlConfig();
		mGamepadController.setControlConfig();
		resetInputMethod();
		checkScreenLock();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mControls.resetAxisValues();
		crazyflieApp.linkDisconnect();
		crazyflieApp.removeConnectionListener(this);
		mController.disable();
	}

	@Override
	public void onBackPressed() {
		if (mDoubleBackToExitPressedOnce) {
			super.onBackPressed();
			return;
		}
		this.mDoubleBackToExitPressedOnce = true;
		Toast.makeText(this, "Please click BACK again to exit",
				Toast.LENGTH_SHORT).show();
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				mDoubleBackToExitPressedOnce = false;

			}
		}, 2000);
	}

	// TODO: fix indirection
	public void updateFlightData() {
		mFlightDataView.updateFlightData(mController.getPitch(),
				mController.getRoll(), mController.getThrust(),
				mController.getYaw());
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		// Check that the event came from a joystick since a generic motion
		// event could be almost anything.
		if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
				&& event.getAction() == MotionEvent.ACTION_MOVE) {
			if (!(mController instanceof GamepadController)) {
				changeToGamepadController();
			}
			mGamepadController.dealWithMotionEvent(event);
			updateFlightData();
			return true;
		} else {
			return super.dispatchGenericMotionEvent(event);
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// TODO: works for PS3 controller, but does it also work for other
		// controllers?
		// do not call super if key event comes from a gamepad, otherwise the
		// buttons can quit the app
		if (event.getSource() == 1281) {
			if (!(mController instanceof GamepadController)) {
				changeToGamepadController();
			}
			mGamepadController.dealWithKeyEvent(event);
			// exception for OUYA controllers
			if (!Build.MODEL.toUpperCase(Locale.getDefault()).contains("OUYA")) {
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	// TODO: improve
	private void changeToGamepadController() {
		if (!((TouchController) getController()).isDisabled()) {
			((TouchController) getController()).disable();
		}
		mController = mGamepadController;
		mController.enable();
	}

	private void resetInputMethod() {
		// TODO: reuse existing touch controller?

		// Use GyroscopeController if activated in the preferences
		if (mControls.isUseGyro()) {
			mController = new GyroscopeController(mControls, this,
					mDualJoystickView,
					(SensorManager) getSystemService(Context.SENSOR_SERVICE));
		} else {
			mController = new TouchController(mControls, this,
					mDualJoystickView);
		}
		mController.enable();
	}

	public IController getController() {
		return mController;
	}

	// Connection listener implementations
	@Override
	public void connectionInitiated(Link l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionSetupFinished(Link l) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(),
						"Connection Setup finished", Toast.LENGTH_SHORT).show();
			}
		});
		mController.enable();
	}

	@Override
	public void disconnected(Link l) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // link quality is not available when there is no active connection
                mFlightDataView.setLinkQualityText("n/a");
            }
        });
        mController.disable();
	}

	@Override
	public void connectionLost(Link l) {
		runOnUiThread(new Runnable() {
			@Override
	        public void run() {
				Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
			}
		});
	    mController.disable();
	}

	@Override
	public void connectionFailed(Link l) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
        mController.disable();
	}

	@Override
	public void linkQualityUpdate(Link l, final int quality) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFlightDataView.setLinkQualityText(quality + "%");
            }
        });
	}

}
