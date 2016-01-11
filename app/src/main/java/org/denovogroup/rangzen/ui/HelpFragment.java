package org.denovogroup.rangzen.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import org.denovogroup.rangzen.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Liran on 1/11/2016.
 */
public class HelpFragment extends Fragment{

    private ExpandableListView listView;

    List<String> headers;
    Map<String, List<String>> data;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.help_fragment, container, false);

        listView = (ExpandableListView) view.findViewById(R.id.listView);

        prepareHelpContent();

        listView.setAdapter(new HelpExpandableListAdapter(getActivity(), headers, data));

        return view;
    }

    private void prepareHelpContent() {

        headers = new ArrayList<>();
        data = new HashMap<>();

        String header1 = getString(R.string.help_title1);
        List<String> body1 = new ArrayList<>();
        body1.add(getString(R.string.help_content1));
        headers.add(header1);
        data.put(header1, body1);

        String header2 = getString(R.string.help_title2);
        List<String> body2 = new ArrayList<>();
        body2.add(getString(R.string.help_content2));
        headers.add(header2);
        data.put(header2, body2);

        String header3 = getString(R.string.help_title3);
        List<String> body3 = new ArrayList<>();
        body3.add(getString(R.string.help_content3));
        headers.add(header3);
        data.put(header3, body3);

    }
}
