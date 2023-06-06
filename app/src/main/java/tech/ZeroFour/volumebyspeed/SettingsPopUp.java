package tech.ZeroFour.volumebyspeed;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SettingsPopUp extends DialogFragment {
    private final String TAG = "Settings DF";

    TextView tv_percentIncrease, tv_percentPrompt;
    Button mphkph;
    CheckBox cb_autoStart;
    ExampleDialogListener listener;


    Float percentIncrease;
    boolean mph;
    boolean runOnStart;

    static SettingsPopUp newInstance(Float percentIncrease, Boolean mph, Boolean runOnStart) {
        SettingsPopUp f = new SettingsPopUp();

        Bundle args = new Bundle();

        args.putFloat("Percent Increase", percentIncrease);
        args.putBoolean("mph/kph", mph);
        args.putBoolean("Run On Start", runOnStart);

        f.setArguments(args);

        return f;
    }



    @SuppressLint("MissingInflatedId")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.settings_pop_up, null);

        tv_percentIncrease = view.findViewById(R.id.tv_percentIncrease);
        tv_percentPrompt = view.findViewById(R.id.percentPrompt);
        mphkph = view.findViewById(R.id.mph_kph);
        cb_autoStart = view.findViewById(R.id.autoStartCheckBox);

        assert getArguments() != null;
        percentIncrease = getArguments().getFloat("Percent Increase");
        mph = getArguments().getBoolean("mph/kph");
        runOnStart = getArguments().getBoolean("Run On Start");

        tv_percentIncrease.setText(String.valueOf(percentIncrease));
        if (mph) {
            mphkph.setText("MPH");
            tv_percentPrompt.setText(R.string.increaseMPH);
        } else {
            mphkph.setText("KPH");
            tv_percentPrompt.setText(R.string.increaseKPH);
        }
        if (runOnStart) {
            cb_autoStart.setChecked(true);
        }


        mphkph.setOnClickListener(v -> {
            mph = !mph;
            if (mph) {
                mphkph.setText("MPH");
                tv_percentPrompt.setText(R.string.increaseMPH);
            } else {
                mphkph.setText("KPH");
                tv_percentPrompt.setText(R.string.increaseKPH);
            }
        });

        cb_autoStart.setOnClickListener(view1 -> runOnStart = !runOnStart);



        builder.setView(view)
                .setOnCancelListener(dialogInterface -> {
                    if (!tv_percentIncrease.getText().toString().equals("")){
                        percentIncrease = Float.parseFloat(tv_percentIncrease.getText().toString()); //TODO: check if tv_percentIncrease.getText() is not null
                    }
                    listener.applyTexts(percentIncrease, mph, runOnStart);
                    Log.i(TAG, "onCreateDialog: DISMISSED");
                })
                .setNegativeButton("save", (dialogInterface, i) -> {
                    if (!tv_percentIncrease.getText().toString().equals("")){
                        percentIncrease = Float.parseFloat(tv_percentIncrease.getText().toString()); //TODO: check if tv_percentIncrease.getText() is not null
                    }
                    listener.applyTexts(percentIncrease, mph, runOnStart);
                });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        listener = (ExampleDialogListener) context;
    }

    public interface ExampleDialogListener{
        void applyTexts(Float percentIncrease, boolean mph, boolean runOnStart);
    }
}
