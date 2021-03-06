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

public class Sampledapter extends ArrayAdapter<Sample> {

    private final List<Sample> samples;
    private final Context context;

    public Sampledapter(Context context, int resource, List<Sample> samples) {
        super(context, resource, samples);
        this.context = context;
        this.samples = samples;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        //getting the layoutinflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //getting listview itmes
        View listViewItem = inflater.inflate(R.layout.samples, null, true);
        TextView textViewSample = listViewItem.findViewById(R.id.textViewSample);
        ImageView imageViewStatus = listViewItem.findViewById(R.id.imageViewStatus);
        RelativeLayout relativeLayout = listViewItem.findViewById(R.id.relativeLayout);
        TextView textViewSampleStatus = listViewItem.findViewById(R.id.textViewSampleStatus);

        //getting the current sample
        Sample sample = samples.get(position);

        //setting the sample to textview
        textViewSample.setText(sample.getBarcode());

        if (position % 2 == 0) {
            relativeLayout.setBackgroundResource(R.drawable.reverse_background);
        } else {
            relativeLayout.setBackgroundResource(R.drawable.background);
        }

        switch (sample.getStatus()) {
            case 0:
                imageViewStatus.setBackgroundResource(R.drawable.ic_av_timer_24px);
                textViewSampleStatus.setText(R.string.status_unsent);
                break;
            case 1:
                imageViewStatus.setBackgroundResource(R.drawable.ic_done_24px);
                textViewSampleStatus.setText(R.string.status_ok);
                break;
            case 2:
                imageViewStatus.setBackgroundResource(R.drawable.ic_error_white_24dp);
                textViewSampleStatus.setText(sample.getStatus_text());
                break;
        }

        return listViewItem;
    }

}//end class
