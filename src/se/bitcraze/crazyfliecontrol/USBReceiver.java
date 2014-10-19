package se.bitcraze.crazyfliecontrol;

import se.bitcraze.crazyflielib.CrazyradioLink;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;
import android.widget.Toast;

public class USBReceiver  extends BroadcastReceiver{   
    protected CrazyflieApp crazyflieApp;
    Context context;
    
    private SoundPool mSoundPool;
    private boolean mLoaded;
    private int mSoundConnect;
    private int mSoundDisconnect;

    
	@Override
    public void onReceive(Context context, Intent intent) {
		final String TAG = "Crazyflie.USBReceiver";
	
		this.context = context;
		crazyflieApp = (CrazyflieApp) context.getApplicationContext();
		initializeSounds();
		
		
		String action = intent.getAction();
        Log.d(TAG, "Intent action: " + action);
        if ((context.getPackageName()+".USB_PERMISSION").equals(action)) {
            //reached only when USB permission on physical connect was canceled and "Connect" or "Radio Scan" is clicked
            synchronized (this) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        Toast.makeText(context, "Crazyradio attached", Toast.LENGTH_SHORT).show();
                    }
                } else {
                	Log.d(TAG, "permission denied for device " + device);
                }
            }
        }            
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null && CrazyradioLink.isCrazyradio(device)) {
                Log.d(TAG, "Crazyradio detached");
                Toast.makeText(context, "Crazyradio detached", Toast.LENGTH_SHORT).show();
                playSound(mSoundDisconnect);
                crazyflieApp.crazyradioDetached();
            }
        }
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null && CrazyradioLink.isCrazyradio(device)) {
                Log.d(TAG, "Crazyradio attached");
                Toast.makeText(context, "Crazyradio attached", Toast.LENGTH_SHORT).show();
        		playSound(mSoundConnect);
                crazyflieApp.crazyradioAttached();
            }
        }
        
        mSoundPool.release();
        mSoundPool = null;
       
    }
	
	private void initializeSounds() {
		//TODO: check this!
    	//context.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
        // Load sounds
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mLoaded = true;
            }
        });        
        mSoundConnect = mSoundPool.load(context, R.raw.proxima, 1);
        mSoundDisconnect = mSoundPool.load(context, R.raw.tejat, 1);
    }
	private void playSound(int sound){
        if (mLoaded) {
            float volume = 1.0f;
            mSoundPool.play(sound, volume, volume, 1, 0, 1f);
        }
    }

}
