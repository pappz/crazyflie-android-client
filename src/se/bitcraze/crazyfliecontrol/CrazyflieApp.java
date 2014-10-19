package se.bitcraze.crazyfliecontrol;

import java.io.IOException;

import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.IController;
import se.bitcraze.crazyfliecontrol.ui.MainActivity;
import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.crtp.CommanderPacket;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class CrazyflieApp extends Application {
	private static final String TAG = "Crazyflie.APP";
	
	Context context;
	private CrazyradioLink crazyradioLink;
	private IController mController = null;
	private Controls controls = null;

	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
		controls = Controls.getControlsInstance(context);
		crazyradioLink = CrazyradioLink.getCrazyradioLink();
	}
	
	@Override
	public void onTerminate() {
		crazyradioLink.disconnect();
	}
	
	public void crazyradioDetached() {
		linkDisconnect();
	}
	
	public void crazyradioAttached() {
		Log.d(TAG,"Attached the usb");
	}

	public void addConnectionListener(MainActivity mainActivity) {
		crazyradioLink.addConnectionListener(mainActivity);
	}
	
	public void removeConnectionListener(MainActivity mainActivity) {
		crazyradioLink.removeConnectionListener(mainActivity);
	}

	public void linkConnect() {
        // ensure previous link is disconnected
        linkDisconnect();

        int radioChannel = controls.getRadioChannel();
        int radioDatarate = controls.getDataRate();
    	
        try {
            // create link
        	crazyradioLink.connect(context, new CrazyradioLink.ConnectionData(radioChannel, radioDatarate));

            // connect and start thread to periodically send commands containing
            // the user input
        	new Thread(new Runnable() {
                @Override
                public void run() {
                    while (crazyradioLink.isConnected()) {                       
                    	crazyradioLink.send(new CommanderPacket(mController.getRoll(), mController.getPitch(), mController.getYaw(), (char) mController.getThrust(), controls.isXmode()));
                        try {
                            Thread.sleep(20, 0);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }).start();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(this, "Crazyradio not attached", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
	
	public void linkDisconnect() {
		if (crazyradioLink != null) {
            crazyradioLink.disconnect();
        }
	}
	
	public void setController(IController controller){
		if(this.mController != null ) {
			this.mController.disable();
		}
		
		this.mController = controller;
		this.mController.enable();
	}
	
	public void disableController() {
		if(mController != null) {
			mController.disable();
		}
	}
}
