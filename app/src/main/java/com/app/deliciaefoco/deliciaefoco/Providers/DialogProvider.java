package com.app.deliciaefoco.deliciaefoco.Providers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

public class DialogProvider {
    private Context context;
    private AlertDialog window;
    private EditText input;

    public DialogProvider(Context context){
        this.context = context;
    }

    public void dialogShow(String title, String message, DialogInterface.OnClickListener okButtonListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", okButtonListener);
        this.window = builder.create();
        this.window.show();
    }

    public void promptPasswordDialogShow(String title, DialogInterface.OnClickListener okButtonListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        this.input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", okButtonListener);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        this.window = builder.create();
        this.window.show();
    }

    public String getInputValue(){
        return this.input.getText().toString();
    }
}
