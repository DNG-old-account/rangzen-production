package org.denovogroup.rangzen.ui;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.ExchangeHistoryTracker;
import org.denovogroup.rangzen.backend.Peer;
import org.denovogroup.rangzen.backend.PeerManager;
import org.denovogroup.rangzen.backend.RangzenService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liran on 1/19/2016.
 */
public class DebugFragment extends Fragment {

    Timer refreshTimer;

    ListView listView;

    List<String[]> peers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(BluetoothAdapter.getDefaultAdapter() != null) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(BluetoothAdapter.getDefaultAdapter().getAddress());
        }

        View view = inflater.inflate(R.layout.debug_fragment, container, false);

        listView = (ListView) view.findViewById(R.id.listView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(refreshTimer != null) refreshTimer.cancel();
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PeerManager manager = PeerManager.getInstance(getActivity().getApplicationContext());
                        peers.clear();
                        List<Peer> peersList = manager.getPeers();

                        for(Peer peer : peersList){
                            String[] peerStr = new String[3];
                            peerStr[0] = peer.toString();
                            try {
                                peerStr[1] = manager.thisDeviceSpeaksTo(peer) ? "true" : null;
                            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                            }

                            ExchangeHistoryTracker.ExchangeHistoryItem history = ExchangeHistoryTracker.getInstance().getHistoryItem(peer.address);


                            int backoff =  0;

                            if(history != null) {
                                Math.min(RangzenService.BACKOFF_MAX,
                                        (int) (Math.pow(2, history.getAttempts()) * RangzenService.BACKOFF_FOR_ATTEMPT_MILLIS));
                            }

                            String backoffString = null;
                            if(backoff > 0) {
                                backoffString = TimeUnit.MILLISECONDS.toMinutes(backoff) + ":" + (TimeUnit.MILLISECONDS.toSeconds(backoff) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(backoff)));
                            }

                            peerStr[2] = backoffString;
                            peers.add(peerStr);
                        }
                        listView.setAdapter(new DebugAdapter(getActivity(), peers));
                    }
                });
            }
        }, 1000, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(refreshTimer != null) refreshTimer.cancel();
        refreshTimer = null;
    }
}
