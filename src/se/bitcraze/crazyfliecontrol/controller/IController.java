package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.ui.FlyingDataEvent;

public interface IController {
	
    public void setOnFlyingDataListener(FlyingDataEvent flyingDataListener);
	
    public float getThrust();

    public float getRoll();

    public float getPitch();

    public float getYaw();

    public void enable();
    
    public void disable();
}
