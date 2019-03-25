package com.example.android.util;

import android.app.Activity;
import android.content.Context;

/**
 * Created by user on 3/20/2017.
 */

public abstract class BaseModelClass {

    public Context context;
    public Activity activity;

    public BaseModelClass(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }
}
