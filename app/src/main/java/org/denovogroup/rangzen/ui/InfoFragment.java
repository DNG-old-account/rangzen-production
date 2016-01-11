package org.denovogroup.rangzen.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.denovogroup.rangzen.R;

/**
 * Created by Liran on 1/11/2016.
 */
public class InfoFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.info_fragment, container, false);

        String version = "0.0";
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),0).versionName +" ("+getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),0).versionCode+")";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ((TextView) v.findViewById(R.id.version)).setText("v"+version);

        return v;
    }
}
