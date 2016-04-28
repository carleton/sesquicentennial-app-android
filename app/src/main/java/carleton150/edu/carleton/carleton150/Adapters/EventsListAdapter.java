/*
package carleton150.edu.carleton.carleton150.Adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.R;

*//**
 * Created by nayelymartinez on 2/4/16.
 *
 */

package carleton150.edu.carleton.carleton150.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.MainActivity;
import carleton150.edu.carleton.carleton150.POJO.Event;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.R;

// Adapter for the events list view
public class EventsListAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<EventContent> events;
    private MainActivity context;

    public EventsListAdapter(MainActivity context, List<EventContent> events){
        this.events = events;
        this.context = context;
    }

    // Format date tabs to display day of the week
    private String formatStartTime(EventContent event) {
        String startTime = event.getStartTime();
        String[] dateArray = startTime.split("(-)|(T)|(:)");


        // Check if hours/minutes/seconds included
        SimpleDateFormat df;
        Calendar newStartTime;
        if (dateArray.length >= 6) {
            df = new SimpleDateFormat("MMM dd hh:mm a", Locale.US);

            newStartTime = Calendar.getInstance();
            newStartTime.set(Integer.parseInt(dateArray[0]),
                    Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]),
                    Integer.parseInt(dateArray[3]), Integer.parseInt(dateArray[4]),
                    Integer.parseInt(dateArray[5]));
        } else {
            df = new SimpleDateFormat("MMM dd", Locale.US);
            newStartTime = Calendar.getInstance();
            newStartTime.set(Integer.parseInt(dateArray[0]),
                    Integer.parseInt(dateArray[1])-1, Integer.parseInt(dateArray[2]));
        }
        // Set display to new formatted startTime
        return df.format(newStartTime.getTime());
    }

    public void setEvents(List<EventContent> events){
        this.events = events;
        notifyDataSetChanged();
    }

    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.event_card, parent, false);
        return new EventViewHolder(itemView, context);
    }

    @Override
    public int getItemCount() {
        if(events != null) {
            return events.size();
        }else{
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final EventViewHolder myHolder = (EventViewHolder) holder;
        myHolder.setDate(formatStartTime(events.get(position)));
        myHolder.setDescription(events.get(position).getDescription());
        myHolder.setTitle(events.get(position).getTitle());
        myHolder.setLocation(events.get(position).getLocation());
        myHolder.setExpanded(events.get(position).isExpanded());

        if(events.get(position).getUrl() != null){
            myHolder.setBtnUrl(events.get(position).getUrl());
        }else{
            myHolder.hasNoUrl();
        }

        View view = myHolder.getItemView();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myHolder.swapExpanded();
                events.get(position).setIsExpanded(!events.get(position).isExpanded());
                myHolder.setIconExpand(context);
            }
        });

    }

    public static class EventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private boolean expanded = false;
        private ImageView iconExpand;
        TextView txtDescription;
        TextView dateTitle;
        TextView txtTitle;
        TextView txtLocation;
        LinearLayout linLayoutUrl;
        Button btnUrl;
        private MainActivity context;
        private boolean hasUrl = false;


        public EventViewHolder(View itemView, MainActivity context) {
            super(itemView);
            itemView.setOnClickListener(this);
            dateTitle = (TextView) itemView.findViewById(R.id.txt_event_date);
            txtTitle = (TextView) itemView.findViewById(R.id.txt_event_title);
            txtLocation = (TextView) itemView.findViewById(R.id.txt_event_location);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_event_description);
            iconExpand = (ImageView) itemView.findViewById(R.id.img_icon_expand);
            linLayoutUrl = (LinearLayout) itemView.findViewById(R.id.lin_layout_url);
            btnUrl = (Button) itemView.findViewById(R.id.btn_url);
            this.context = context;
        }

        private View getItemView(){
            return this.itemView;
        }

        /**
         * @param width
         */
        public void setWidth(int width) {
            itemView.setLayoutParams(new RecyclerView.LayoutParams(width, RecyclerView.LayoutParams.MATCH_PARENT));
        }

        @Override
        public void onClick(View v) {

           swapExpanded();
            setIconExpand(context);

        }

        public void setBtnUrl(final String url) {
            hasUrl = true;
            btnUrl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.showEventInfoInBrowser(url);
                }
            });
        }

        public void hasNoUrl(){
            hasUrl = false;
        }

        // Set date in event calendar date tabs
        public void setDate(String dateInfo) {
            dateTitle.setText(dateInfo);
        }

        public void setTitle(String title) {
            txtTitle.setText(title);
        }

        public void setLocation(String location) {
            txtLocation.setText(location);
        }

        public void setDescription(String description) {
            txtDescription.setText(description);
        }

        /**
         * If the item should be expanded, sets the icon to the expand less icon and shows the description
         * If it should be shrunk, sets the icon to the expand more icon and hides the description
         * @param context
         */
        public void setIconExpand(Context context){
            if(expanded){
                txtDescription.setVisibility(View.VISIBLE);
                if(hasUrl){
                    linLayoutUrl.setVisibility(View.VISIBLE);
                }
                iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_less));
            }else{
                iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_more));
                txtDescription.setVisibility(View.GONE);
                linLayoutUrl.setVisibility(View.GONE);
            }
        }

        public void setExpanded(boolean expanded){
            this.expanded = expanded;
        }

        public void swapExpanded(){
            this.expanded = !expanded;
        }


    }



}
