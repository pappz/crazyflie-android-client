package se.bitcraze.crazyfliecontrol.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import se.bitcraze.crazyflielib.CrazyradioLink;

import android.content.Context;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class PebbleController extends AbstractController{

    private float sensorRoll = 0;
    private float sensorPitch = 0;
    private float Yaw = 0;
    private float thrust = 0;

    private final float AMPLIFICATION = 1f;

    private final static int yawFactor = 40;

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
    private final UUID PEBBLE_UUID = UUID.fromString("30db0a7d-ebbd-4bf7-aaad-4bed53d54530");

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
            CrazyradioLink.getCrazyradioLink().getParam().setHoverMode(true);
            mControls.setHoverMode(true);
            thrust = 1;
            resetThurst();
            break;
        case BUTTON_UP:
            thrust = 1;
            resetThurst();
            break;
        case BUTTON_LONG_DOWN:
            Yaw = yawFactor;
            resetYaw();
            break;
        case BUTTON_LONG_SELECT:
            landing();
            break;
        case BUTTON_LONG_UP:
            Yaw = yawFactor*-1;
            resetYaw();
            break;
        }
    }

    private void landing() {
        thrust = -1;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                CrazyradioLink.getCrazyradioLink().getParam().setHoverMode(false);
                mControls.setHoverMode(false);
                thrust = 0;
            }
        }, 1000*10);
    }

    private void resetThurst() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                thrust = 0;
            }
        }, 1000*2);
    }

    private void resetYaw() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Yaw = 0;
            }
        }, 1000*1);
    }

    @Override
    public void enable() {
        super.enable();

        PebbleKit.startAppOnPebble(mContext, PEBBLE_UUID);

        dataHandler = new PebbleKit.PebbleDataReceiver(PEBBLE_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {

                if(data.contains(DATA_BUTTON)) {
                    int button = data.getUnsignedInteger(DATA_BUTTON).intValue();
                    if ( thrust == 0 && button >= BUTTON_DOWN && button <= BUTTON_UP) {
                        pushButton(button);
                    } else if (button != -1){
                        pushButton(button);
                    }
                }

                if(data.contains(DATA_X) && data.contains(DATA_Y) && data.contains(DATA_Z) ) {
                    double d = Math.sqrt(data.getInteger(DATA_X)*data.getInteger(DATA_X)+data.getInteger(DATA_Y)*data.getInteger(DATA_Y)+data.getInteger(DATA_Z)*data.getInteger(DATA_Z));
                    sensorRoll = (float) ((float) data.getInteger(DATA_X) / (float) d) * AMPLIFICATION;
                    sensorPitch = (float) (data.getInteger(DATA_Y) / (float) d) * AMPLIFICATION;
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
        float roll = sensorRoll;

        roll = (float) Math.min(1.0, Math.max(-1, roll+mControls.getRollTrim()));

        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    @Override
    public float getPitch() {
        float pitch = sensorPitch;

        pitch = (float) Math.min(1.0, Math.max(-1, pitch+mControls.getPitchTrim()));

        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }

    @Override
    public float getYaw() {
         return Yaw;
    }
}
