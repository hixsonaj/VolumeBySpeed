package tech.ZeroFour.volumebyspeed;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static tech.ZeroFour.volumebyspeed.AppNotification.CHANNEL_ID;


public class SpeedService extends Service implements LocationListener {

    //TAG
    private final String TAG = "SpeedService";

    //SpeedService Intent
    public static final String VOLUME_BY_SPEED_SERVICE = "tech.zerofour.volumebyspeed";
    Intent intent = new Intent(VOLUME_BY_SPEED_SERVICE);


    //Variables
    AudioManager audioManager;
    LocationManager locationManager;
    float percentIncrease;
    long pageStateVolume;
    boolean mph;


    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        intent = null;
        if (intent == null) {
            startForeground(1, getMyActivityNotification("VOLUME BY SPEED", "ERROR!! UNABLE TO ACTIVATE SERVICE"));
            Log.i(TAG, "onStartCommand: ABOUT TO DESTROY");
            onDestroy();
        } else {


            Bundle extras = intent.getExtras(); //TODO: check for null intent // working on rn (line 47)
            if (extras != null) {
                if (intent.getBooleanExtra("STOP_SERVICE", false)) {
                    Intent broadcastIntent = new Intent(VOLUME_BY_SPEED_SERVICE);

                    broadcastIntent.putExtra("running", false);
                    broadcastIntent.putExtra("speed", (float) 0.0);
                    broadcastIntent.putExtra("volume", 0);
                    sendBroadcast(broadcastIntent);

                    stopSelf();
                }
            }


            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            pageStateVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            Log.i(TAG, "onStartCommand: Page State Volume is " + pageStateVolume);

            percentIncrease = intent.getFloatExtra("percentIncrease", 0);
            Log.i(TAG, "onStartCommand: percent increase is " + intent.getFloatExtra("percentIncrease", 0));

            mph = intent.getBooleanExtra("MPH_KPH", true);

            //Creates notification
            startForeground(1, getMyActivityNotification("VOLUME BY SPEED", "Ready! The volume will change when you move"));

            //Starts Location Updates
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }

        }
        return START_STICKY;
    }



    @SuppressLint("UnspecifiedImmutableFlag")
    private Notification getMyActivityNotification(String title, String text){

        PendingIntent contentIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            contentIntent = PendingIntent.getActivity(this,
                    0, new Intent(this, MainActivity.class), PendingIntent.FLAG_MUTABLE);
        } else {
            contentIntent = PendingIntent.getActivity(this,
                    0, new Intent(this, MainActivity.class), 0);
        }

        Intent stopServiceIntent = new Intent(this, SpeedService.class);
        stopServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        stopServiceIntent.putExtra("STOP_SERVICE", true);
        PendingIntent stopServicePendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            stopServicePendingIntent = PendingIntent.getService(this,0,stopServiceIntent, PendingIntent.FLAG_MUTABLE);
        } else {
            stopServicePendingIntent = PendingIntent.getService(this,0,stopServiceIntent, 0);
        }


        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(contentIntent)
                .addAction(R.mipmap.ic_launcher_round, "Stop", stopServicePendingIntent)
                .setOnlyAlertOnce(true)
                .build();
    }



    private void updateVolume(CLocation location) {

        float nCurrentSpeed = 0;
        if (location != null) {
            location.setUseMetricUnits(!mph);
            nCurrentSpeed = location.getSpeed();
        }
        Log.i(TAG, "updateSpeed: Speed is " + nCurrentSpeed);

        int updateVolume = (int) (pageStateVolume * (1 + 0.01 * (percentIncrease * nCurrentSpeed)));
        Log.i(TAG, "updateVolume: New Volume is " + updateVolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, updateVolume, 0);

        Notification notification = getMyActivityNotification("Current volume is: " + updateVolume, "Current speed is: " + (int) nCurrentSpeed);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notification);

        intent.putExtra("running", true);
        intent.putExtra("speed", nCurrentSpeed);
        intent.putExtra("volume", updateVolume);
        sendBroadcast(intent);
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        CLocation myLocation = new CLocation(location, !mph);
        this.updateVolume(myLocation);
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Terminate Service");
        if (locationManager != null) {
            locationManager.removeUpdates(this); //TODO: Look out for reports of this line      ORIGINAL: //TODO: Check if locationManager is null
        }
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}