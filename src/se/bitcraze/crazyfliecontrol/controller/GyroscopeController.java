package se.bitcraze.crazyfliecontrol.controller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

/**
 * The GyroscopeController extends the TouchController and uses the gyroscope sensors 
 * of the device to control the roll and pitch values.
 * The yaw and thrust values are still controlled by the touch controls according
 * to the chosen "mode" setting.
 * 
 */
public class GyroscopeController extends TouchController {

    private SensorManager mSensorManager;
    private Sensor sensor = null;
    private SensorEventListener seListener = null;    

    private float mSensorRoll = 0;
    private float mSensorPitch = 0;

    public GyroscopeController(Context context, DualJoystickView dualJoystickView, SensorManager sensorManager) {
        super(context, dualJoystickView);
        mSensorManager = sensorManager;
        
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!=null) {
        	sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        	seListener = new RotationVectorListener();
        	
        } else {
        	sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        	seListener = new AccelerometerListener();
        }
    }
        
    @Override
    public void enable() {
        super.enable();
        mSensorManager.registerListener(seListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void disable() {
        mSensorManager.unregisterListener(seListener);
        super.disable();
    }
    
    public String getControllerName() {
        return "gyroscope controller";
    }
    
    class AccelerometerListener implements SensorEventListener {

    	@Override
    	public void onAccuracyChanged(Sensor arg0, int arg1) {		
    	}

    	@Override
    	public void onSensorChanged(SensorEvent event) {
    		// TODO Auto-generated method stub
    		mSensorPitch = (event.values[0] / 10 ) * -1;
    		mSensorRoll = event.values[1] / 10;       
            updateFlightData();
    	}    	
    }
    
    class RotationVectorListener implements SensorEventListener {
    	private int AMPLIFICATION = 2;
    	
    	@Override
    	public void onAccuracyChanged(Sensor arg0, int arg1) { }

    	@Override
    	public void onSensorChanged(SensorEvent event) {
    		// TODO Auto-generated method stub
            // amplifying the sensitivity.
        	Log.d("Crazyflie: ","Sensor: "+Float.toString(event.values[0])+" - "+Float.toString(event.values[1]));
            mSensorRoll = event.values[0] * AMPLIFICATION;
            mSensorPitch = event.values[1] * AMPLIFICATION;
            updateFlightData();
    	}    	
    }      

    // overwrite getRoll() and getPitch() to only use values from gyro sensors
    public float getRoll() {
        float roll = mSensorRoll;
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = mSensorPitch;
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }    
}
