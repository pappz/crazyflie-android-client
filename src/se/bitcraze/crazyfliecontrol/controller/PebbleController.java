package se.bitcraze.crazyfliecontrol.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import se.bitcraze.crazyfliecontrol.CrazyflieApp;

import android.content.Context;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class PebbleController extends AbstractController{

    protected int mResolution = 1000;
   
    private long sensorRoll = 0;
    private long sensorPitch = 0;
    private float thrust = 0;
    
    
    //Possible receiver keys
    private final static int DATA_BUTTON = 0;
    private final static int DATA_X = 1;
    private final static int DATA_Y = 2;
    private final static int DATA_Z = 3;

    //Buttons
    private final static int BUTTON_DOWN = 0;
    private final static int BUTTON_SELECT = 1;
    private final static int BUTTON_UP = 2;
    private final static int BUTTON_LONG_DOWN = 3;
    private final static int BUTTON_LONG_SELECT = 4;
    private final static int BUTTON_LONG_UP = 5;
    
    Timer timer;
    
    private PebbleKit.PebbleDataReceiver dataHandler = null;    
    private final UUID pebbleUUID = UUID.fromString("30db0a7d-ebbd-4bf7-aaad-4bed53d54530");
    
    public PebbleController(Context context, DualJoystickView dualJoystickView) {
    	super(context);
    	
    	timer = new Timer();
    }

    private void pushButton(int button) {
    	switch (button) {
    		case BUTTON_DOWN:
    			thrust = -1;
    			resetThurst();
    			break;
    		case BUTTON_SELECT:
    			//Felszallas
				CrazyflieApp crazyflieApp = (CrazyflieApp) mContext.getApplicationContext();
				crazyflieApp.getRadioLink().getParam().setHoverMode(true);
				mControls.setHoverMode(true);
				thrust = 1;
				resetThurst();
    			break;
    		case BUTTON_UP:
    			thrust = 1;
    			resetThurst();
    			break;
    	}
    }
    
    private void resetThurst() {
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
				  thrust = 0;
			  }
		}, 1000*2);
    }
    @Override
    public void enable() {
        super.enable();
        
        dataHandler = new PebbleKit.PebbleDataReceiver(pebbleUUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
            	
            	if(data.contains(DATA_BUTTON)) {
            		int button = data.getUnsignedInteger(DATA_BUTTON).intValue();
            		if ( thrust == 0 && button != -1 ) {
            			pushButton(button);
            		}
            	}
            	
            	if(data.contains(DATA_X) && data.contains(DATA_Y) && data.contains(DATA_Z) ) {
            		sensorRoll = data.getInteger(DATA_X);
            		sensorPitch = data.getInteger(DATA_Y);
            	} else {
            		sensorRoll = 0;
            		sensorPitch = 0;
            	}
            	            	
                PebbleKit.sendAckToPebble(context, transactionId);
                updateFlightData();
            }
        };
        PebbleKit.registerReceivedDataHandler(mContext, dataHandler);
    }
    
    @Override
    public void disable() {
        super.disable();
        mControls.setHoverMode(false);
        try{
        	mContext.unregisterReceiver(dataHandler);
        }catch(Exception e){
        	
        }
    }

    public String getControllerName() {
        return "Pebble controller";
    }

    @Override
    public float getThrust() {
    	if(!mControls.getHoverMode()) {
    		return 0;
    	}
    	
        if(thrust == 1) {
        	return 65535;
        } else if( thrust == -1) {
        	return 0;
        } else {
        	return 32767;
        }

    }
    
    @Override
    public float getRoll() {
        float roll = (float) sensorRoll / (float) 1000;

        if(roll + mControls.getRollTrim() > 1 ) {
        	roll = 1;
        } else if(roll + mControls.getRollTrim() < -1) {
        	roll = -1;
        }
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }
    
    @Override
    public float getPitch() {
        float pitch = (float) sensorPitch / (float) 1000;

        if(pitch + mControls.getPitchTrim() > 1 ) {
        	pitch = 1;
        } else if(pitch + mControls.getPitchTrim() < -1) {
        	pitch = -1;
        }
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }
}
