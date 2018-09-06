package com.java.gaojian;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlaceholderFragment extends Fragment {

    View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (mView == null)
            mView = inflater.inflate(R.layout.place_holder, container, false);
        ((TextView) mView.findViewById(R.id.place_hold_text)).setText(getResources().getString(R.string.placeholder));
        return mView;
    }
}
