package se.bitcraze.crazyflielib;

import se.bitcraze.crazyflielib.crtp.HoverParamPacket;

public class Param {
	CrazyradioLink mCrazyradioLink;

	public Param(CrazyradioLink crazyradioLink) {
		mCrazyradioLink = crazyradioLink;
	}
	
	public void setHoverMode(boolean enable) {
		if (mCrazyradioLink.isConnected())
			mCrazyradioLink.send(new HoverParamPacket(enable));	
	}
}
