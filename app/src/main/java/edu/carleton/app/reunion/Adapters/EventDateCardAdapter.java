package edu.carleton.app.reunion.Adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import edu.carleton.app.reunion.R;


/**
 * Adapter for the Dates in the Events section of the app
 */
public class EventDateCardAdapter extends RecyclerView.Adapter<EventDateCardAdapter.EventDateCardViewHolder> {

    private ArrayList<String> dateInfo;
    public EventDateCardAdapter(ArrayList<String> dateInfo) {

        this.dateInfo = dateInfo;
    }

    @Override
    public EventDateCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.event_date_card, parent, false);
        return new EventDateCardViewHolder(itemView);
    }


    /**
     * Sets view for each card
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(EventDateCardViewHolder holder, int position) {
        holder.setDate(dateInfo.get(position));
    }

    /**
     * returns number of items in dateInfo
     * @return
     */
    @Override
    public int getItemCount() {
        if(dateInfo != null) {
            return dateInfo.size();
        }else{
            return 0;
        }
    }

    /**
     * ViewHolder for Event dates
     */
    public static class EventDateCardViewHolder extends RecyclerView.ViewHolder {

        public EventDateCardViewHolder(View itemView) {
            super(itemView);

        }

        /**
         * Sets holder to display formatted date
         * @param dateInfo
         */
        public void setDate(String dateInfo) {

            TextView dateTitle = (TextView) itemView.findViewById(R.id.event_date_title);

            if(dateInfo.equals("")){
                dateTitle.setText("");
                dateTitle.setTag("");
                return;
            }

            DateFormat dfCorrect = new SimpleDateFormat("EEEE'\r' MMM dd',' yyyy", Locale.US);
            String[] dateArray = dateInfo.split("-");
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]));
            Date curDate = calendar.getTime();
            Log.d("date", "curDate: "+curDate);
            dateTitle.setText(dfCorrect.format(curDate));
            dateTitle.setTag(dateInfo);
        }

    }
}