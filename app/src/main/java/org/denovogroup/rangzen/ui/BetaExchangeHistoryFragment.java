package org.denovogroup.rangzen.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.ExchangeHistoryTracker;
import org.denovogroup.rangzen.backend.RangzenService;
import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.objects.BetaExchangeHistory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 12/24/2015.
 */
public class BetaExchangeHistoryFragment extends Fragment {

    public static final String HISTORY_FILE = "exchange_history_file_name";
    public static final String HISTORY_UPDATED_INTENT = "com.rangzen.historyupdated";

    private List<BetaExchangeHistory> exchangeHistoryList;
    private BroadcastReceiver receiver;
    private IntentFilter filter = new IntentFilter(HISTORY_UPDATED_INTENT);

    ListView listView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.beta_exchange_history, container, false);

        listView = (ListView) v.findViewById(R.id.list_view);

        v.findViewById(R.id.clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RangzenService.clearHistory(getActivity());
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(listView == null && getView() != null) listView = (ListView) getView().findViewById(R.id.list_view);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadHistory();
            }
        };

        getActivity().registerReceiver(receiver, filter);

        loadHistory();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(receiver != null) {
            getActivity().unregisterReceiver(receiver);
            receiver = null;
        }
    }

    private void loadHistory(){
        try {

            File dir = getActivity().getFilesDir();
            File file = new File(dir, BetaExchangeHistoryFragment.HISTORY_FILE);

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            exchangeHistoryList = (List<BetaExchangeHistory>) ois.readObject();
            ois.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            exchangeHistoryList = new ArrayList<BetaExchangeHistory>();
        }

        if(listView != null){
            listView.setAdapter(new HistoryAdapter(getActivity(), exchangeHistoryList));
        }
    }

    private static class HistoryAdapter extends ArrayAdapter<BetaExchangeHistory>{

        public HistoryAdapter(Context context, List<BetaExchangeHistory> exchangeHistoryList) {
            super(context, R.layout.ex_history_list_item, exchangeHistoryList);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(v == null) v = inflater.inflate(R.layout.ex_history_list_item, parent, false);

            ((TextView)v.findViewById(R.id.timestamp_tv)).setText(
                    Utils.convertTimestampToDateString(false, getItem(position).timestamp)
            );

            ((TextView)v.findViewById(R.id.count_tv)).setText(
                    getItem(position).receivedMessages +" ("+ getItem(position).newMessages+" new)"
            );

            return v;
        }
    }
}
