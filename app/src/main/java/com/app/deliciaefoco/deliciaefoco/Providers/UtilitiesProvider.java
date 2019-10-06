package com.app.deliciaefoco.deliciaefoco.Providers;

import android.content.Context;
import android.content.Intent;

import com.app.deliciaefoco.deliciaefoco.Activities.HomeActivity;

public class UtilitiesProvider {
    public static void backToHome(Context context){
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }
}
