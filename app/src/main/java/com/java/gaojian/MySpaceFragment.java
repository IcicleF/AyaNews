package com.java.gaojian;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MySpaceFragment extends Fragment {

    private MainActivity mainAc;

    public MySpaceFragment() {
        // Required empty public constructor
    }

    private View view = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (view == null) {
            view = inflater.inflate(R.layout.my_space_list, container, false);

            TextView tvFavs = (TextView) view.findViewById(R.id.myspace_favorites);
            tvFavs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mainAc, FavoriteListActivity.class);
                    startActivity(intent);
                }
            });

            TextView tvChannels = (TextView) view.findViewById(R.id.myspace_channels);
            tvChannels.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mainAc, RSSListActivity.class);
                    startActivity(intent);
                }
            });

            TextView tvAbout = (TextView) view.findViewById(R.id.myspace_about);
            tvAbout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mainAc, AboutActivity.class);
                    startActivity(intent);
                }
            });
        }
        else {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null != parent) {
                parent.removeView(view);
            }
        }
        return view;
    }

    public void setMainActivity(MainActivity mainAc) {
        this.mainAc = mainAc;
    }
}
