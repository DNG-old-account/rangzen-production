package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.Peer;
import org.denovogroup.rangzen.backend.PeerManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Liran on 12/24/2015.
 */
public class BetaNearbyFragment extends Fragment {

    ListView listView;
    PeerManager peerManager;

    Timer timer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.beta_nearby_fragment, container, false);

        peerManager = PeerManager.getInstance(getActivity());

        listView = (ListView) v.findViewById(R.id.list_view);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(peerManager != null && listView != null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int p = Math.max(0,listView.getFirstVisiblePosition());
                            float offset = (listView.getChildCount() > 0) ? listView.getChildAt(0).getTop() : 0f;

                            List<Peer> peers = peerManager.getPeers();

                            listView.setAdapter(new NearbyAdapter(getActivity(), peers));
                            listView.smoothScrollToPositionFromTop(Math.min(p, peers.size()-1), (int)offset);
                        }
                    });
                }
            }
        },0 ,1000);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(timer !=null){
            timer.cancel();
            timer = null;
        }
    }

    private static class NearbyAdapter extends ArrayAdapter<Peer>{

        public NearbyAdapter(Context context, List<Peer> peers) {
            super(context, android.R.layout.simple_list_item_1, peers);
        }


    }

}
