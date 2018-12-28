package com.app.deliciaefoco.deliciaefoco;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.NumberPicker;

public class NumberPickerDialog extends DialogFragment {
    private NumberPicker.OnValueChangeListener valueChangeListener;
    private String title = "Escolha o valor", subtitle = "Escolha um número", positiveButton = "Ok", negativeButton = "Cancelar";
    private int minValue = 1, maxValue = 5;

    @SuppressLint("ValidFragment")
    public NumberPickerDialog(String title, String subtitle, String positiveButton, String negativeButton, int minValue, int maxValue){
        this.title = title;
        this.subtitle = subtitle;
        this.positiveButton = positiveButton;
        this.negativeButton = negativeButton;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public NumberPickerDialog(){

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final NumberPicker numberPicker = new NumberPicker(getActivity());

        numberPicker.setMinValue(this.minValue);
        numberPicker.setMaxValue(this.maxValue);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); //evitar a exibição do teclado


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(subtitle);

        builder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                valueChangeListener.onValueChange(numberPicker,
                        numberPicker.getValue(), numberPicker.getValue());
            }
        });

        builder.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("Cancelado", "Cancelado");
            }
        });

        builder.setView(numberPicker);
        return builder.create();
    }

    public NumberPicker.OnValueChangeListener getValueChangeListener() {
        return valueChangeListener;
    }

    public void setValueChangeListener(NumberPicker.OnValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
    }
}
