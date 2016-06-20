package com.example.youni.testapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.youni.testapp.R;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by youni on 16/6/20.
 */
public class GroupListActivity extends Activity {
    private ListView lvGroup;
    private LinearLayout llHeaderView;
    private GroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_list);

        findView();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        refresh();
    }

    private void initView() {
        adapter = new GroupAdapter(this);

        lvGroup.setAdapter(adapter);

        llHeaderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // try to create a new group

                startActivity(new Intent(GroupListActivity.this,NewGroupActivity.class));
            }
        });

        refresh();
    }

    private void findView() {
        lvGroup = (ListView) findViewById(R.id.lv_group_list);

        llHeaderView = (LinearLayout) View.inflate(this, R.layout.activity_group_list_header_view,null);

        lvGroup.addHeaderView(llHeaderView);

    }

    private void refresh(){
        adapter.refresh(EMClient.getInstance().groupManager().getAllGroups());
    }
}

class GroupAdapter extends BaseAdapter{

    private List<EMGroup> groupList = new ArrayList<>();
    private Context context;

    public GroupAdapter(Context context){
        this.context = context;
    }

    public void refresh(List<EMGroup> groups){
        groupList.clear();
        groupList.addAll(groups);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if(groupList == null){
            return 0;
        }

        return groupList.size();
    }

    @Override
    public Object getItem(int position) {
        if(groupList == null){
            return null;
        }

        return groupList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        EMGroup group = (EMGroup) getItem(position);

        if(convertView == null){
            viewHolder = new ViewHolder();

            convertView = LayoutInflater.from(context).inflate(R.layout.row_group_item,null);
            viewHolder.tvGroupName = (TextView) convertView.findViewById(R.id.tv_group_name);
            convertView.setTag(viewHolder);

        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tvGroupName.setText(group.getGroupName());

        return convertView;
    }

    static class ViewHolder{
        ImageView ivGroupAvatar;
        TextView tvGroupName;
    }
}
