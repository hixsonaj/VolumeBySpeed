package tech.ZeroFour.volumebyspeed;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class InfoPopUp extends DialogFragment {

    TextView volumeTooHighWarning;

    static InfoPopUp newInstance(boolean showWarning) {
        InfoPopUp f = new InfoPopUp();

        Bundle args = new Bundle();

        args.putBoolean("Show Warning", showWarning);

        f.setArguments(args);

        return f;
    }

    @SuppressLint("MissingInflatedId")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.info_pop_up, null);

        volumeTooHighWarning = view.findViewById(R.id.volumeTooHighWarning);

        assert getArguments() != null;
        boolean showWarning = getArguments().getBoolean("Show Warning");

        if (showWarning){
            volumeTooHighWarning.setText(R.string.too_high_volume_warning);
        } else {
            volumeTooHighWarning.setText("");
        }

        builder.setView(view);

        return builder.create();
    }
}
