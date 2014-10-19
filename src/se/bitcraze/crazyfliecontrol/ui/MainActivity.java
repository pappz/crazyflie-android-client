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

package se.bitcraze.crazyfliecontrol.ui;

import java.util.Locale;

import se.bitcraze.crazyfliecontrol.CrazyflieApp;
import se.bitcraze.crazyfliecontrol.R;
import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.GamepadController;
import se.bitcraze.crazyfliecontrol.controller.GyroscopeController;
import se.bitcraze.crazyfliecontrol.controller.IController;
import se.bitcraze.crazyfliecontrol.controller.PebbleController;
import se.bitcraze.crazyfliecontrol.controller.TouchController;
import se.bitcraze.crazyfliecontrol.prefs.PreferencesActivity;
import se.bitcraze.crazyflielib.ConnectionListener;
import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.Link;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

public class MainActivity extends Activity implements FlyingDataEvent, ConnectionListener, OnCheckedChangeListener{

	private DualJoystickView mDualJoystickView;
	private FlightDataView mFlightDataView;

	private GamepadController gamepadController;

	private boolean mDoubleBackToExitPressedOnce = false;

	private Controls controls;

	private CrazyflieApp crazyflieApp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		crazyflieApp = (CrazyflieApp) getApplication();

		controls = Controls.getControlsInstance(this);

		// Default controller
		mDualJoystickView = (DualJoystickView) findViewById(R.id.joysticks);

		// initialize gamepad controller
		gamepadController = new GamepadController(getApplicationContext(), crazyflieApp);

		mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);
		
		((ToggleButton) findViewById(R.id.hovermode)).setOnCheckedChangeListener(this);
	}
	
	private void checkScreenLock() {
		if (controls.isScreenLock() || controls.isUseGyro()) {
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
	public boolean onPrepareOptionsMenu(final Menu menu) {
		if (CrazyradioLink.getCrazyradioLink().isConnected()) {
			menu.findItem(R.id.menu_connect).setTitle(R.string.menu_disconnect);
		} else {
			menu.findItem(R.id.menu_connect).setTitle(R.string.menu_connect);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			if (CrazyradioLink.getCrazyradioLink().isConnected()) {
				crazyflieApp.linkDisconnect();
			} else {
				crazyflieApp.linkConnect();
				//Have to call because it reset the hover mode.
				resetInputMethod();
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
		controls.setControlConfig();
		gamepadController.setControlConfig();
		resetInputMethod();
		checkScreenLock();
		
		switchHoverMode(false);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		controls.resetAxisValues();
		crazyflieApp.linkDisconnect();
		crazyflieApp.removeConnectionListener(this);		
		crazyflieApp.disableController();
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switchHoverMode(isChecked);
	}
	
	private void switchHoverMode(boolean hover){
		//Send the info
		CrazyradioLink.getCrazyradioLink().getParam().setHoverMode(hover);
		//store the state
		controls.setHoverMode(hover);
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

	@Override
	public void flyingDataEvent(float pitch, float roll, float thrust, float yaw) {
		if(!controls.getHoverMode())
			mFlightDataView.updateFlightData(pitch, roll, thrust/(65535/100), yaw);
		else
			mFlightDataView.updateFlightData(pitch, roll, (int) thrust, yaw);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		// Check that the event came from a joystick since a generic motion
		// event could be almost anything.
		if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
				&& event.getAction() == MotionEvent.ACTION_MOVE) {
			if (gamepadController.isDisabled()) {
				changeToGamepadController();
			}
			
			gamepadController.dealWithMotionEvent(event);
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
			if (gamepadController.isDisabled()) {
				changeToGamepadController();
			}
			
			gamepadController.dealWithKeyEvent(event);
			// exception for OUYA controllers
			if (!Build.MODEL.toUpperCase(Locale.getDefault()).contains("OUYA")) {
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	private void changeToGamepadController() {
		crazyflieApp.setController(gamepadController);
	}

	private void resetInputMethod() {
		IController controller;
				
		if (controls.isUseGyro()) {
			controller = new GyroscopeController(getApplicationContext(),mDualJoystickView, (SensorManager) getSystemService(Context.SENSOR_SERVICE));
		} else if(controls.isUsePebble()) {
			controller = new PebbleController(getApplicationContext(), mDualJoystickView);
		} else {
			controller = new TouchController(getApplicationContext(), mDualJoystickView);
		}
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(controls.isUsePebble()) {
					((ToggleButton) findViewById(R.id.hovermode)).setEnabled(false);
				} else {
					((ToggleButton) findViewById(R.id.hovermode)).setEnabled(true);
				}
			}
		});
		
		controller.setOnFlyingDataListener(this);
		crazyflieApp.setController(controller);
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
		resetInputMethod();
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
		crazyflieApp.disableController();
	}

	@Override
	public void connectionLost(Link l) {
		runOnUiThread(new Runnable() {
			@Override
	        public void run() {
				Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
			}
		});
		crazyflieApp.disableController();
	}

	@Override
	public void connectionFailed(Link l) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
		crazyflieApp.disableController();
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
