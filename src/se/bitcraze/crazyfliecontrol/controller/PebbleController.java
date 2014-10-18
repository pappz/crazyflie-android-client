package se.bitcraze.crazyfliecontrol.controller;

import static com.getpebble.android.kit.Constants.APP_UUID;
import static com.getpebble.android.kit.Constants.MSG_DATA;
import static com.getpebble.android.kit.Constants.TRANSACTION_ID;

import java.util.UUID;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.util.PebbleDictionary;


/**
 * The TouchController uses the on-screen joysticks to control the roll, pitch, yaw and thrust values.
 * The mapping of the axes can be changed with the "mode" setting in the preferences.
 * 
 * For example, mode 3 (default) maps roll to the left X-Axis, pitch to the left Y-Axis,
 * yaw to the right X-Axis and thrust to the right Y-Axis.
 * 
 */
public class PebbleController extends TouchController  {

    protected int mResolution = 1000;
   
    private float sensorRoll = 0;
    private float sensorPitch = 0;
    double g = 0;
    
    private PebbleKit.PebbleDataReceiver dataHandler = null;    
    private final UUID pebbleUUID = UUID.fromString("30db0a7d-ebbd-4bf7-aaad-4bed53d54530");
    
    public PebbleController(Context context, DualJoystickView dualJoystickView) {
    	super(context, dualJoystickView);
    }

    @Override
    public void enable() {
        super.enable();
        g = 0;
        dataHandler = new PebbleKit.PebbleDataReceiver(pebbleUUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
            	if(data.contains(0)) {            	
            		Long button = data.getUnsignedInteger(0);
            		Log.d("Crazyflie.Pebble: ","Received data: "+Long.toString(button));
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
            	//Log.d("Crazyflie.Pebble: ","Received data+: "+Float.toString(converToSI(x)));
            	//Log.d("Crazyflie.Pebble: ","Received data+: "+Double.toString(exludeGravity(x)));
            	//", "+Double.toString(exludeGravity(y))+" "+Double.toString(exludeGravity(z)));
            	/*
            	
            	long y = data.getInteger(1);
            	long z = data.getInteger(2);
            	sensorRoll = (float) x;
            	sensorPitch = (float) y;
            	*/
            	//Log.d("Crazyflie.Pebble: ","Received data: "+Long.toString(x)+" "+Long.toString(y));            	
                PebbleKit.sendAckToPebble(context, transactionId);
                //updateUi(); 
                
                sensorRoll = x / 1000;
                sensorPitch = y / 1000;       
                updateFlightData();
            }
        };
        Log.d("Crazyflie.Pebble: ","Ready for receiving");
        PebbleKit.registerReceivedDataHandler(mContext, dataHandler);
    }
    
    private float converToSI(float value) {
    	return (float) (value/1000*9.80665);    
    }
    
    public double exludeGravity(float d) {
    	//convert G to m/s^2
    	double v = d/1000*9.80665;
    	
         // alpha is calculated as t / (t + dT)
         // with t, the low-pass filter's time-constant
         // and dT, the event delivery rate    	

         final float alpha = (float) 0.8;

         g = alpha * g + (1 - alpha) * v;

         return v - g;
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
        float roll = sensorRoll;
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = sensorPitch;
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }
}
