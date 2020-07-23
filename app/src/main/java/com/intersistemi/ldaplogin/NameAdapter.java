package com.intersistemi.ldaplogin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class NameAdapter extends ArrayAdapter<Name> {

    private final List<Name> names;
    private final Context context;

    public NameAdapter(Context context, int resource, List<Name> names) {
        super(context, resource, names);
        this.context = context;
        this.names = names;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        //getting the layoutinflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //getting listview itmes
        View listViewItem = inflater.inflate(R.layout.names, null, true);
        TextView textViewName = listViewItem.findViewById(R.id.textViewName);
        ImageView imageViewStatus = listViewItem.findViewById(R.id.imageViewStatus);
        RelativeLayout relativeLayout = listViewItem.findViewById(R.id.relativeLayout);


        //getting the current name
        Name name = names.get(position);

        //setting the name to textview
        textViewName.setText(name.getBarcode());

        if (position % 2 == 0) {
            relativeLayout.setBackgroundResource(R.drawable.reverse_background);

        } else {

            //textViewName.setBackgroundColor(Color.parseColor("#FFBF00"));
            relativeLayout.setBackgroundResource(R.drawable.background);

        }

        //if the synced status is 0 displaying
        //queued icon
        //else displaying synced icon
        if (name.getStatus() == 0) {
            imageViewStatus.setBackgroundResource(R.drawable.ic_av_timer_24px);
        } else {
            imageViewStatus.setBackgroundResource(R.drawable.ic_done_24px);
        }

        return listViewItem;
    }


}//end class
