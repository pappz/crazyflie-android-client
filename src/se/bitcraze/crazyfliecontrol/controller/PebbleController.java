package se.bitcraze.crazyfliecontrol.controller;

import java.util.UUID;

import android.content.Context;
import android.util.Log;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class PebbleController extends TouchController  {

    protected int mResolution = 1000;
   
    private float sensorRoll = 0;
    private float sensorPitch = 0;
    
    private PebbleKit.PebbleDataReceiver dataHandler = null;    
    private final UUID pebbleUUID = UUID.fromString("30db0a7d-ebbd-4bf7-aaad-4bed53d54530");
    
    public PebbleController(Context context, DualJoystickView dualJoystickView) {
    	super(context, dualJoystickView);
    }

    @Override
    public void enable() {
        super.enable();
        mControls.setHoverMode(true);

        dataHandler = new PebbleKit.PebbleDataReceiver(pebbleUUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
            	if(data.contains(0)) {            	
            		Long button = data.getUnsignedInteger(0);
            	}
            	
            	Long x = (long) 0,y = (long) 0 ,z = (long) 0;
            	
            	if(data.contains(1)) {            	
            		x = data.getInteger(1);          		
            	}
            	if(data.contains(2)) {            	
            		y = data.getInteger(2);            		
            	}
            	
            	if(data.contains(3)) {            	
            		z = data.getInteger(3);
            	}            	
            	            	
                PebbleKit.sendAckToPebble(context, transactionId); 

                sensorRoll = (float) x;
                sensorPitch = (float) y;
                updateFlightData();
            }
        };
        Log.d("Crazyflie.Pebble: ","Ready for receiving");
        PebbleKit.registerReceivedDataHandler(mContext, dataHandler);
    }

    @Override
    public void disable() {
        super.disable();
        try{
        	mContext.unregisterReceiver(dataHandler);
        }catch(Exception e){
        	
        }
    }

    public String getControllerName() {
        return "Pebble controller";
    }

    public float getRoll() {
        float roll = sensorRoll / (float) 1000;

        if(roll + mControls.getRollTrim() > 1 ) {
        	roll = 1;
        } else if(roll + mControls.getRollTrim() < 1) {
        	roll = -1;
        }
        
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = sensorPitch / (float) 1000;

        if(pitch + mControls.getPitchTrim() > 1 ) {
        	pitch = 1;
        } else if(pitch + mControls.getPitchTrim() < 1) {
        	pitch = -1;
        }
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }
}
