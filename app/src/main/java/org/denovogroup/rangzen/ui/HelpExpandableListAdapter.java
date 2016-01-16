package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.denovogroup.rangzen.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Liran on 1/11/2016.
 */
public class HelpExpandableListAdapter extends BaseExpandableListAdapter {

    Context context;
    List<String> headers;
    Map<String,List<String>> data;

    public HelpExpandableListAdapter(Context context, List<String> headers, Map<String, List<String>> data) {
        this.context = context;
        this.data = data;
        this.headers = headers;
    }

    @Override
    public int getGroupCount() {
        return data.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return data.get(headers.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return data.get(headers.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.help_title, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.help_title)).setText((String) getGroup(groupPosition));
        convertView.findViewById(R.id.shadow).setVisibility(isExpanded ? View.GONE : View.VISIBLE);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.help_body, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.help_body)).setText(Html.fromHtml((String)getChild(groupPosition, childPosition)));

        convertView.findViewById(R.id.shadow).setVisibility(isLastChild ? View.VISIBLE : View.GONE);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
