package se.bitcraze.crazyfliecontrol.controller;

import android.widget.Toast;
import se.bitcraze.crazyfliecontrol.ui.FlyingDataEvent;
import se.bitcraze.crazyfliecontrol.ui.MainActivity;


/**
 * The AbstractController implements the basic methods of IController class
 *
 */
public abstract class AbstractController implements IController {

	private FlyingDataEvent flyingDataEvent;
	protected Controls mControls;
	protected boolean mIsDisabled;
	protected MainActivity mActivity;

	public AbstractController(Controls controls, MainActivity activity) {
		mControls = controls;
		mActivity = activity;
	}
	
	public void setOnFlyingDataListener(FlyingDataEvent flyingDataListener) {
		flyingDataEvent = flyingDataListener;
	}

	public void enable(){
		mIsDisabled = false;
        Toast.makeText(mActivity, "Using " + getControllerName(), Toast.LENGTH_SHORT).show();
	}
	
    public void disable() {
        mIsDisabled = true;
    }

    public boolean isDisabled() {
        return mIsDisabled;
    }

    public String getControllerName(){
    	return "unknown controller";
    }
    
    public void updateFlightData() {
    	flyingDataEvent.flyingDataEvent(getPitch(), getRoll(), getThrust(), getYaw());
	}
    
    public float getThrust() {
        float thrust = ((mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getRightAnalog_Y() : mControls.getLeftAnalog_Y());
        if (thrust > mControls.getDeadzone()) {
            return mControls.getMinThrust() + (thrust * mControls.getThrustFactor());
        }
        return 0;
    }

    public float getRoll() {
        float roll = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getRightAnalog_X() : mControls.getLeftAnalog_X();
        return (roll + mControls.getRollTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(roll);
    }

    public float getPitch() {
        float pitch = (mControls.getMode() == 1 || mControls.getMode() == 3) ? mControls.getLeftAnalog_Y() : mControls.getRightAnalog_Y();
        return (pitch + mControls.getPitchTrim()) * mControls.getRollPitchFactor() * mControls.getDeadzone(pitch);
    }

    public float getYaw() {
        float yaw = 0;
        yaw = (mControls.getMode() == 1 || mControls.getMode() == 2) ? mControls.getLeftAnalog_X() : mControls.getRightAnalog_X();
        return yaw * mControls.getYawFactor() * mControls.getDeadzone(yaw);
    }

}
