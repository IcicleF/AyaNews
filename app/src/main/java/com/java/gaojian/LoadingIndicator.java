package com.java.gaojian;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;

import com.wang.avi.AVLoadingIndicatorView;

import java.util.List;

public class LoadingIndicator extends AlertDialog {

    private AVLoadingIndicatorView view;
    private boolean isShown;

    private LoadingIndicator(Context context, int themeResId) {
        super(context, themeResId);
    }

    public static LoadingIndicator buildInst(Context context) {
        LoadingIndicator inst = new LoadingIndicator(context, R.style.TransparentDialog);
        inst.setCancelable(true);
        inst.setCanceledOnTouchOutside(false);
        return inst;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.loading_indicator);
        view = (AVLoadingIndicatorView) findViewById(R.id.av_loading_indicator);
    }

    @Override
    public void show() {
        if (!isShown) {
            super.show();
            try {
                if (this.getWindow() == null)
                    throw new NullPointerException("Unexpected null window");
                this.getWindow().setDimAmount(0.2f);
                isShown = true;
                view.show();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void dismiss() {
        if (isShown) {
            isShown = false;
            super.dismiss();
            view.hide();
        }
    }
}
