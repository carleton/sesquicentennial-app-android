package carleton150.edu.carleton.carleton150.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import carleton150.edu.carleton.carleton150.R;


/**
 * Adapter for the RecyclerView that shows images in the bottom of the HistoryFragment
 */
public class EventDateCardAdapter extends RecyclerView.Adapter<EventDateCardAdapter.EventDateCardViewHolder> {

    private int screenWidth;
    private ArrayList<String> dateInfo;


    public EventDateCardAdapter(ArrayList<String> dateInfo,
                                int screenWidth) {

        this.screenWidth = screenWidth;
        this.dateInfo = dateInfo;
    }



    @Override
    public EventDateCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.event_date_card, parent, false);
        return new EventDateCardViewHolder(itemView);
    }


    // Setting view for each card
    @Override
    public void onBindViewHolder(EventDateCardViewHolder holder, int position) {
        holder.setDate(dateInfo.get(position));
    }

    @Override
    public int getItemCount() {
        if(dateInfo != null) {
            return dateInfo.size();
        }else{
            return 0;
        }
    }

    public static class EventDateCardViewHolder extends RecyclerView.ViewHolder {

        public EventDateCardViewHolder(View itemView) {
            super(itemView);

        }

        // Set date in event calendar date tabs
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


            dateTitle.setText(dfCorrect.format(curDate));
            dateTitle.setTag(dateInfo);
        }

    }
}