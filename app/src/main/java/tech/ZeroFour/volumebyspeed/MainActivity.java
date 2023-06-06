package tech.ZeroFour.volumebyspeed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SettingsPopUp.ExampleDialogListener {
    private final String TAG = "Main Activity";

    //UI element variables
    TextView tv_speed, tv_volume, tv_maxVolume, tv_percentIncrease, mphkph;
    Button onOff, infoPopUp, settingsPopUp, plusPercentIncrease, minusPercentIncrease;


    //Variables
    int maxVolume;
    Float percentIncrease;
    boolean running = false;
    boolean runOnStart = false;
    boolean mph = true;
    boolean notificationsRequested = false;
    boolean showWarning = false;
    SharedPreferences sp;

    AudioManager audioManager;

    AdView mAdView;
    InterstitialAd mInterstitialAd;
    Dialog myDialog;
    ReviewInfo reviewInfo;
    ReviewManager manager;
    int uses;


    @SuppressLint({"MissingPermission", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Connect UI elements to variables
        onOff = findViewById(R.id.onOff);
        tv_speed = findViewById(R.id.tv_speed);
        tv_volume = findViewById(R.id.tv_volume);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        tv_maxVolume = findViewById(R.id.tv_maxVolume);
        infoPopUp = findViewById(R.id.infoPopUp);
        settingsPopUp = findViewById(R.id.settingsPopUp);
        tv_percentIncrease = findViewById(R.id.tv_percentIncrease);
        plusPercentIncrease = findViewById(R.id.plusPercentIncrease);
        minusPercentIncrease = findViewById(R.id.minusPercentIncrease);
        mphkph = findViewById(R.id.mph_kph);

        //Connect buttons to variables with listeners
        onOff.setOnClickListener(this);
        infoPopUp.setOnClickListener(this);
        settingsPopUp.setOnClickListener(this);
        plusPercentIncrease.setOnClickListener(this);
        minusPercentIncrease.setOnClickListener(this);

        myDialog = new Dialog(this);

        //Banner Ad
        MobileAds.initialize(this, initializationStatus -> {});
        mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId("ca-app-pub-2701820106710934/8442406039");
        mAdView = findViewById(R.id.ad_view1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //??
        activateReviewInfo();

        //Get shared preferences (i think this is data stored in the cache)
        sp = getSharedPreferences("percent", Context.MODE_PRIVATE);
        SharedPreferences sp = getApplicationContext().getSharedPreferences("percent", Context.MODE_PRIVATE);

        //Connect shared preferences to variables
        percentIncrease = sp.getFloat("percent", 1);
        mph = sp.getBoolean("mph", true);
        runOnStart = sp.getBoolean("Run On Start", false);
        uses = sp.getInt("Uses", 0);

        //Log important values to console
        Log.i(TAG, "onCreate: " + percentIncrease);
        Log.i(TAG, "onCreate: " + mph);
        Log.i(TAG, "onCreate: " + runOnStart);

        //Set visible information
        tv_percentIncrease.setText(percentIncrease + "%");
        if (mph) {
            mphkph.setText("MPH");
        } else {
            mphkph.setText("KPH");
        }
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        tv_volume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
        tv_maxVolume.setText("/" + maxVolume);

        //Check if service is already running
        if (isMyServiceRunning()) {
            onOff.setBackground(ContextCompat.getDrawable(this, R.drawable.button_serviceon));
            running = true;
        } else if (runOnStart) {
            serviceToggle();
        }

        //Find warning volume
        if ((maxVolume * 0.8) < audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            infoPopUp.setBackground(ContextCompat.getDrawable(this, R.drawable.button_warningon));
            showWarning = true;
        } else {
            infoPopUp.setBackground(ContextCompat.getDrawable(this, R.drawable.button_warningoff));
            showWarning = false;
        }


        MobileAds.initialize(this, initializationStatus -> {});

        AdRequest adRequestInterstitial = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-2701820106710934/3990657149", adRequestInterstitial, new InterstitialAdLoadCallback() {

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.i(TAG, "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d(TAG, loadAdError.toString());
                mInterstitialAd = null;
            }
        });


    }


    //Checks if service is running
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SpeedService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    //Handles on volume key clicked events
    @SuppressLint("SetTextI18n")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keycode = event.getKeyCode();
        int action = event.getAction();

        int showVolume;

        if ((keycode == KeyEvent.KEYCODE_VOLUME_UP || keycode == KeyEvent.KEYCODE_VOLUME_DOWN) && action == KeyEvent.ACTION_UP) {
            if (keycode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) == audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                    showVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                } else {
                    showVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1;
                }
                tv_volume.setText(Integer.toString(showVolume));
            } else {
                if (0 == audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                    showVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                } else {
                    showVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1;
                }
            }
            tv_volume.setText(Integer.toString(showVolume));
            if ((maxVolume * 0.8) < showVolume) {
                infoPopUp.setBackground(ContextCompat.getDrawable(this, R.drawable.button_warningon));
                showWarning = true;
            } else {
                infoPopUp.setBackground(ContextCompat.getDrawable(this, R.drawable.button_warningoff));
                showWarning = false;
            }
        }
        return super.dispatchKeyEvent(event);
    }


    //Handles on click events
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onClick(View v) {
        if (v == onOff){
            serviceToggle();
        } else if (v == infoPopUp) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.addToBackStack(null);
            InfoPopUp infoPopUp = InfoPopUp.newInstance(showWarning);
            infoPopUp.show(fragmentTransaction, "Info Pop Up");
        } else if (v == settingsPopUp) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.addToBackStack(null);
            SettingsPopUp settingsPopUp = SettingsPopUp.newInstance(percentIncrease, mph, runOnStart);
            settingsPopUp.show(fragmentTransaction, "Settings Pop Up");
        } else if (v == plusPercentIncrease && !running) {
            percentIncrease += 1;
            tv_percentIncrease.setText(percentIncrease + "%");
        } else if (v == minusPercentIncrease && !running) {
            percentIncrease -= 1;
            tv_percentIncrease.setText(percentIncrease + "%");
        }
    }


    //Toggles the service on and off
    public void serviceToggle() {
        if (!running) {
            //Starts service
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
                notificationsRequested = true;
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            } else  {
                Log.i(TAG, "onClick: Service Starting");
                Log.i(TAG, "onClick: Percent Increase Number is " + percentIncrease);
                Log.i(TAG, "onClick: mph/kph toggle = " + mph);

                running = true;

                Intent intent = new Intent(this, SpeedService.class);
                intent.putExtra("percentIncrease", Float.parseFloat(percentIncrease.toString()));
                intent.putExtra("MPH_KPH", mph);

                onOff.setBackground(ContextCompat.getDrawable(this, R.drawable.button_serviceon));
                startForegroundService(intent);
            }
            uses++;
            Log.i(TAG, "serviceToggle: uses: " + uses);

        } else {
            //Ends service
            running = false;
            onOff.setBackground(ContextCompat.getDrawable(this, R.drawable.button_serviceoff));
            tv_speed.setText("-");
            stopService(new Intent(this, SpeedService.class));
            if(uses == 10){
                startReviewFlow();
                Log.i(TAG, "serviceToggle: SHOWING (OR SHOULD) POP UP REVIEW");
            }
            if (mInterstitialAd != null && uses%3 == 2 && uses > 3) {
                mInterstitialAd.show(MainActivity.this);
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet or was out of turn");
            }
        }
    }


    //Update UI
    @Override
    public void applyTexts(Float passedPercentIncrease, boolean passedMph, boolean passedRunOnStart) {

        percentIncrease = passedPercentIncrease;
        mph = passedMph;
        runOnStart = passedRunOnStart;

        tv_percentIncrease.setText(percentIncrease + "%");

        if (mph) {
            mphkph.setText("MPH");
        } else {
            mphkph.setText("KPH");
        }

    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        float speed = intent.getFloatExtra("speed", 0);
        int volume = intent.getIntExtra("volume", 0);
        boolean BRrunning = intent.getBooleanExtra("running", true);

        tv_speed.setText(String.valueOf((long) speed));
        tv_volume.setText(String.valueOf(volume));

        if (volume != audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
            infoPopUp.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.button_warningoff));
        } else {
            infoPopUp.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.button_warningon));
        }

        if (!BRrunning) {
            running = false;
            onOff.setBackgroundColor(getColor(R.color.teal_200));
            tv_speed.setText("-");
            tv_volume.setText(String.valueOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
        }
        }
    };



    void activateReviewInfo(){
        manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> managerInfoTask = manager.requestReviewFlow();
        managerInfoTask.addOnCompleteListener((task)->{
            if (task.isSuccessful()) {
                reviewInfo = task.getResult();
            }
        });
    }



    void startReviewFlow(){
        if (reviewInfo != null) {
            Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
            flow.addOnCompleteListener(task -> {
            });
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(SpeedService.VOLUME_BY_SPEED_SERVICE));
        Log.i(TAG, "onResume: Registered Broadcast Receiver");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        Log.i(TAG, "onPause: Unregistered Broadcast Receiver");
    }

    @Override
    protected void onStop() {
        SharedPreferences.Editor editor = sp.edit();

        editor.putFloat("percent", percentIncrease);
        editor.putBoolean("mph", mph);
        editor.putBoolean("Run On Start", runOnStart);
        editor.putInt("Uses", uses);
        editor.commit();

        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            Log.i(TAG, "ERROR CAUGHT");
        }
        super.onStop();
    }
}
