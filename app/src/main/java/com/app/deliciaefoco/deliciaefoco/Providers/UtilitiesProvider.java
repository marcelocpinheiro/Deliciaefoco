package com.app.deliciaefoco.deliciaefoco.Providers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.app.deliciaefoco.deliciaefoco.Activities.HomeActivity;
import com.app.deliciaefoco.deliciaefoco.Interfaces.Product;

import java.text.DecimalFormat;

import io.sentry.Sentry;
import io.sentry.event.UserBuilder;

public class UtilitiesProvider {
    public static String FILENAME = "DEFAULT_COMPANY";


    public static void backToHome(Context context){
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public static String pagSeguroValue(double ret, Context context) {
        SharedPreferences settings = context.getSharedPreferences(FILENAME, 0);
        if (settings.getString("env", "PROD").equals("HOM")) {
            double randomDouble = Math.random();
            randomDouble = randomDouble * 30 + 1;
            int randomInt = (int) randomDouble;
            return "0" + randomInt;
        } else {
            DecimalFormat df = new DecimalFormat("#.00");
            String formatted = df.format(ret);
            return formatted.replace(".", "").replace(",", "");
        }
    }

    public static void trackException(Exception e) {
        Sentry.init("https://97ff9129c969477c8c423bc67fa9375b@sentry.io/1797765");
        Sentry.getContext().setUser(
                new UserBuilder().setEmail("marcelompinheiro@outlook.com").build()
        );
        Sentry.getContext().addTag("bug", "bug");
        Sentry.capture(e);
    }


}
