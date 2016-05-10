package carleton150.edu.carleton.carleton150.Adapters;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import carleton150.edu.carleton.carleton150.LogMessages;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfo.Event;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfo.Text_;
import carleton150.edu.carleton.carleton150.POJO.Quests.Waypoint;
import carleton150.edu.carleton.carleton150.R;

/**
 * Adapter for the RecyclerView in the HistoryInfoPopup
 * Also used for showing progress through quest
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private Event[] historyListNew = null;

    private Waypoint[] waypointList = null;
    public int screenWidth;
    public int screenHeight;
    public boolean isQuestProgress;
    public Context context;


    public HistoryAdapter(Context context, Event[] historyListNew, Waypoint[] waypoints, int screenWidth, int screenHeight, boolean isQuestProgress) {
        this.historyListNew = historyListNew;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        this.waypointList = waypoints;
        this.isQuestProgress = isQuestProgress;
        this.context = context;
    }


    public void closeAdapter(){
        this.context = null;
    }

    /**
     * Returns 0 if the object contains an image, 1 if it contains text, 2
     * if it contains a quest waypoint
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
       if(isQuestProgress){
            return 2;
        }
        if(historyListNew != null){
            if(historyListNew[position].getMedia() != null && historyListNew[position].getMedia().getUrl() != null &&
                     !historyListNew[position].getMedia().getUrl().equals("")){
                return 0;
            }else{
                return 1;
            }
        }
        else {
            return -1;
        }
    }

    /**
     * Depending on the type, creates a ViewHolder from either the
     * history_info_card_image, the history_info_card_text,
     * or the info_card_quest_in_progress
     *
     *
     * @param parent
     * @param viewType
     * @return RecyclerView.ViewHolder for the view
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0:
                View itemView = LayoutInflater.
                        from(parent.getContext()).
                        inflate(R.layout.history_info_card_image, parent, false);
                return new HistoryViewHolderImage(itemView);
            case 1:
                View view = LayoutInflater.
                        from(parent.getContext()).
                        inflate(R.layout.history_info_card_text, parent, false);
                return new HistoryViewHolderText(view);
            case 2:
                View questInProgressView = LayoutInflater.
                        from(parent.getContext()).
                        inflate(R.layout.info_card_quest_in_progress, parent, false);
                return new ViewHolderQuestInProgress(questInProgressView);
        }
        return null;
    }

    /**
     * When the holder is bound, sets the necessary fields depending on the type
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        //Sets fields of a HistoryViewHolderText and sets an OnClickListener to expand the view
        if(holder instanceof HistoryViewHolderText){
            if(historyListNew != null){
                final Event geofenceInfoContent = historyListNew[position];
                if(geofenceInfoContent.getText() != null) {
                    ((HistoryViewHolderText) holder).setTxtSummary(geofenceInfoContent.getText().getHeadline());
                    ((HistoryViewHolderText) holder).setTxtDescription(geofenceInfoContent.getText().getText());
                }
                ((HistoryViewHolderText) holder).setTxtDate(geofenceInfoContent.getStartDate().getYear());
                ((HistoryViewHolderText) holder).setExpanded(geofenceInfoContent.isExpanded());
                ImageView imgExpanded = ((HistoryViewHolderText) holder).getIconExpand();

                ((HistoryViewHolderText) holder).setExpanded(geofenceInfoContent.isExpanded());
                ((HistoryViewHolderText) holder).setIconExpand(context);

                imgExpanded.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        geofenceInfoContent.setIsExpanded(!geofenceInfoContent.isExpanded());
                        ((HistoryViewHolderText) holder).setExpanded(geofenceInfoContent.isExpanded());
                        ((HistoryViewHolderText) holder).setIconExpand(context);
                    }
                });
            }


        }else if(holder instanceof HistoryViewHolderImage){
            //Sets fields of a HistoryViewHolderImage and sets an OnClickListener to expand the view
            if(!isQuestProgress) {
                if(historyListNew != null){
                    final Event geofenceInfoContent = historyListNew[position];
                    if(geofenceInfoContent.getMedia() != null) {
                        Uri uri = Uri.parse(geofenceInfoContent.getMedia().getUrl());
                        Context imgContext = ((HistoryViewHolderImage) holder).getImgMedia().getContext();
                        Picasso.with(imgContext).load(uri).into(((HistoryViewHolderImage) holder).getImgMedia());
                        ((HistoryViewHolderImage) holder).setTxtCaption(geofenceInfoContent.getMedia().getCaption());
                        ((HistoryViewHolderImage) holder).setTxtDescription(geofenceInfoContent.getMedia().getCredit());
                        ((HistoryViewHolderImage) holder).setImgCredit(geofenceInfoContent.getMedia().getCredit());
                    }
                    ((HistoryViewHolderImage) holder).setTxtDate(geofenceInfoContent.getStartDate().getYear());
                    ((HistoryViewHolderImage) holder).setText(geofenceInfoContent.getText());
                    ((HistoryViewHolderImage) holder).setExpanded(geofenceInfoContent.isExpanded());
                    ImageView imgExpanded = ((HistoryViewHolderImage) holder).getIconExpand();
                    ((HistoryViewHolderImage) holder).setExpanded(geofenceInfoContent.isExpanded());
                    ((HistoryViewHolderImage) holder).setIconExpand(context);
                    imgExpanded.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            geofenceInfoContent.setIsExpanded(!geofenceInfoContent.isExpanded());
                            ((HistoryViewHolderImage) holder).setExpanded(geofenceInfoContent.isExpanded());
                            ((HistoryViewHolderImage) holder).setIconExpand(context);
                        }
                    });
                }
            }

        }else if(holder instanceof ViewHolderQuestInProgress) {
            //Sets fields of a ViewHolderQuestInProgress and sets an OnClickListener to expand the view
            final Waypoint waypoint = waypointList[position];
            ((ViewHolderQuestInProgress) holder).setVisibilities(waypoint);
            if(waypoint.getClue().getImage() != null) {
                ((ViewHolderQuestInProgress) holder).setImageClue(waypoint.getClue().getImage().getImage());
            }
            if(waypoint.getHint().getImage() != null) {
                ((ViewHolderQuestInProgress) holder).setImageHint(waypoint.getHint().getImage().getImage());
            }
            if(waypoint.getCompletion().getImage() != null){
                ((ViewHolderQuestInProgress) holder).setImageComp(waypoint.getClue().getImage().getImage());
            }
            ((ViewHolderQuestInProgress) holder).setTxtCompMessage(waypoint.getCompletion().getText());
            ((ViewHolderQuestInProgress) holder).setTxtClue(waypoint.getClue().getText());
            ((ViewHolderQuestInProgress) holder).setTxtHint(waypoint.getHint().getText());
            ((ViewHolderQuestInProgress) holder).setExpanded(false);
            ImageView imgExpanded = ((ViewHolderQuestInProgress) holder).getIconExpand();
            ((ViewHolderQuestInProgress) holder).setIconExpand(context);
            imgExpanded.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ViewHolderQuestInProgress) holder).swapExpanded();
                    ((ViewHolderQuestInProgress) holder).setIconExpand(context);
                }
            });
        }

    }

    /**
     * returns the number of items in the historyList or, if this
     * is being used to display the progress through quest, returns the number
     * of items in the waypoint list
     * @return number of items in list
     */
    @Override
    public int getItemCount() {
        if(historyListNew != null) {
            return historyListNew.length;
        } else if (waypointList != null){
            return waypointList.length;
        }else{
            return 0;
        }
    }

    /**
     * A ViewHolder for views that contain an image, a date, a caption, and a description
     */
    public static class HistoryViewHolderImage extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView txtDate;
        private ImageView imgMedia;
        private TextView txtCaption;
        private ImageView iconExpand;
        private TextView txtDescription;
        private TextView txtText;
        private TextView txtImgCredit;
        private boolean expanded = false;

        public HistoryViewHolderImage(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            txtDate = (TextView) itemView.findViewById(R.id.txt_date);
            imgMedia = (ImageView) itemView.findViewById(R.id.img_history_info_image);
            txtCaption = (TextView) itemView.findViewById(R.id.txt_caption);
            iconExpand = (ImageView) itemView.findViewById(R.id.img_expand);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_image_description);
            txtText = (TextView) itemView.findViewById(R.id.txt_text);
            txtImgCredit = (TextView) itemView.findViewById(R.id.txt_img_credit);
        }

        public void setTxtDescription(String txtDescription) {
            if(txtDescription != null) {
                this.txtDescription.setText(txtDescription);
            }
        }

        public void setText(Text_ text){
            if(text != null){
                if(text.getText() != null){
                    if(!text.getText().equals("")){
                        txtText.setText(text.getText());
                    }
                }
            }
        }

        public void setTxtDate(String txtDate) {

            if(txtDate != null) {
                this.txtDate.setText(txtDate);
            }
        }

        public void setTxtCaption(String caption){

            if(caption != null){
                this.txtCaption.setText(caption);
            }
        }

        public void setImgCredit(String credit){
            if(credit != null){
                if (!credit.equals("")){
                    txtImgCredit.setText(credit);
                    txtImgCredit.setVisibility(View.VISIBLE);
                    return;
                }
            }
            txtImgCredit.setVisibility(View.GONE);
        }

        public void setExpanded(boolean expanded){
            this.expanded = expanded;
        }

        public void swapExpanded(){
            this.expanded = !this.expanded;
        }

        public ImageView getIconExpand(){
            return this.iconExpand;
        }

        /**
         * If the item should be expanded, sets the icon to the expand less icon and shows the description
         * If it should be shrunk, sets the icon to the expand more icon and hides the description
         * @param context
         */
        public void setIconExpand(Context context){
            if(context != null) {
                if (expanded) {
                    iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_less));
                    if(!txtDescription.getText().equals("")) {
                        txtDescription.setVisibility(View.VISIBLE);
                    }

                    if(!txtText.getText().equals("")) {
                        txtText.setVisibility(View.VISIBLE);
                    }
                } else {
                    iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_more));
                    txtDescription.setVisibility(View.GONE);
                    txtText.setVisibility(View.GONE);
                }
            }
        }




        public ImageView getImgMedia(){
            return this.imgMedia;
        }

        /**
         * Expands or shrinks the view depending on whether it is expanded or not
         * @param v
         */
        @Override
        public void onClick(View v) {
            swapExpanded();
        }
    }


    /**
     * A ViewHolder for views that contain only a text description, date, and summary
     */
    public static class HistoryViewHolderText extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView txtSummary;
        private TextView txtDate;
        private ImageView iconExpand;
        private TextView txtDescription;
        private boolean expanded = false;

        public HistoryViewHolderText(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            txtSummary = (TextView) itemView.findViewById(R.id.txt_txt_summary);
            txtDate = (TextView) itemView.findViewById(R.id.txt_date);
            iconExpand = (ImageView) itemView.findViewById(R.id.img_expand);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_txt_description);
        }

        public ImageView getIconExpand(){
            return this.iconExpand;
        }

        public void setTxtDescription(String txtDescription) {
            if(txtDescription != null) {
                this.txtDescription.setText(txtDescription);
            }
        }

        public void setTxtSummary(String txtSummary) {
            if(txtSummary != null) {
                this.txtSummary.setText(txtSummary);
            }
        }

        public void setTxtDate(String txtDate) {

            if(txtDate != null){
                this.txtDate.setText(txtDate);
            }
        }

        /**
         * If the item should be expanded, sets the icon to the expand less icon and shows the description
         * If it should be shrunk, sets the icon to the expand more icon and hides the description
         * @param context
         */
        public void setIconExpand(Context context){
            if(expanded){
                txtDescription.setVisibility(View.VISIBLE);
                iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_less));
            }else{
                iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_more));
                txtDescription.setVisibility(View.GONE);
            }
        }

        public void setExpanded(boolean expanded){
            this.expanded = expanded;
        }

        public void swapExpanded(){
            this.expanded = !expanded;
        }

        @Override
        public void onClick(View v) {
            swapExpanded();
        }
    }



    /**
     * A ViewHolder for views that contain only an image and date
     */
    public static class ViewHolderQuestInProgress extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView txtCompMessage;
        private ImageView imgClue;
        private ImageView imgHint;
        private TextView txtClue;
        private TextView txtHint;
        private CardView cardClue;
        private CardView cardHint;
        private ImageView iconExpand;
        private ImageView imgCompImage;
        private CardView cardCompImage;
        private TextView txtHintTitle;
        private TextView txtClueTitle;
        private boolean expanded = false;
        private boolean hasClueImage = false;
        private boolean hasHintImage = false;
        private boolean hasCompImage = false;

        public ViewHolderQuestInProgress(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            txtCompMessage = (TextView) itemView.findViewById(R.id.txt_completion_message);
            imgClue = (ImageView) itemView.findViewById(R.id.img_clue_image);
            imgHint = (ImageView) itemView.findViewById(R.id.img_hint_image);
            txtClue = (TextView) itemView.findViewById(R.id.txt_clue);
            txtHint = (TextView) itemView.findViewById(R.id.txt_hint);
            cardClue = (CardView) itemView.findViewById(R.id.card_clue_image);
            cardHint = (CardView) itemView.findViewById(R.id.card_hint_image);
            iconExpand = (ImageView) itemView.findViewById(R.id.img_expand);
            txtClueTitle = (TextView) itemView.findViewById(R.id.txt_clue_title);
            txtHintTitle = (TextView) itemView.findViewById(R.id.txt_hint_title);
            imgCompImage = (ImageView) itemView.findViewById(R.id.img_comp_image);
            cardCompImage = (CardView) itemView.findViewById(R.id.card_comp_image);
        }

        public void setTxtHint(String hint){
            if(hint != null) {
                txtHint.setText(hint);
            }else{
                txtHint.setVisibility(View.GONE);
            }
        }

        public void setTxtClue(String clue){
            if(clue != null) {
                txtClue.setText(clue);
            }else{
                txtClue.setVisibility(View.GONE);
            }
        }

        public void setTxtCompMessage(String compMessage){
            if(compMessage != null) {
                txtCompMessage.setText(compMessage);
            }else{
                txtCompMessage.setVisibility(View.GONE);
            }
        }

        public void setExpanded(boolean expanded){
            this.expanded = expanded;
        }

        public void swapExpanded(){
            this.expanded = !this.expanded;
        }

        /**
         * If the item should be expanded, sets the icon to the expand less icon and shows the
         * information about the clue and the hint
         * If it should be shrunk, sets the icon to the expand more icon and hides the
         * information about the clue and the hint
         * @param context
         */
        public void setIconExpand(Context context){
            if(context != null) {
                if (expanded) {
                    iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_less));
                    txtHint.setVisibility(View.VISIBLE);
                    txtHintTitle.setVisibility(View.VISIBLE);
                    txtClueTitle.setVisibility(View.VISIBLE);
                    txtClue.setVisibility(View.VISIBLE);
                    if(hasHintImage) {
                        cardHint.setVisibility(View.VISIBLE);
                    }if(hasClueImage){
                        cardClue.setVisibility(View.VISIBLE);
                    }
                } else {
                    iconExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_navigation_expand_more));
                    txtHint.setVisibility(View.GONE);
                    txtHintTitle.setVisibility(View.GONE);
                    txtClueTitle.setVisibility(View.GONE);
                    txtClue.setVisibility(View.GONE);
                    cardHint.setVisibility(View.GONE);
                    cardClue.setVisibility(View.GONE);
                }
            }
        }
        public ImageView getIconExpand(){
            return this.iconExpand;
        }

        /**
         * Sets booleans for images that the waypoint contains since a waypoint
         * doesn't have to contain images. Also sets the completion image to visible
         * if the waypoint has a completion image
         * @param waypoint
         */
        public void setVisibilities(Waypoint waypoint){
            if(waypoint.getClue().getImage() != null){
                if(!waypoint.getClue().getImage().equals("")){
                    hasClueImage = true;
                }else{
                    hasClueImage = false;
                }
            }else {
                hasClueImage = false;
                cardClue.setVisibility(View.GONE);
            }if(waypoint.getHint().getImage() != null){
                if(!waypoint.getHint().getImage().equals("")) {
                    hasHintImage = true;
                }else{
                    hasHintImage = false;
                }
            }else{
                hasHintImage = false;
            }
            if(waypoint.getCompletion().getImage() != null){
                if(!waypoint.getClue().getImage().equals("")){
                    hasCompImage = true;
                    cardCompImage.setVisibility(View.VISIBLE);
                }else{
                    hasCompImage = false;
                    cardCompImage.setVisibility(View.GONE);
                }
            }else {
                hasCompImage = false;
                cardCompImage.setVisibility(View.GONE);
            }
        }


        /**
         * Sets the image by downsizing and decoding the image string, then putting the image
         * into the recyclerView at the specified position in the imgClue space
         *
         * @param url
         */
        public void setImageClue(String url) {
            if(hasClueImage) {
                Uri uri = Uri.parse(url);
                Context imgContext = imgHint.getContext();
                Picasso.with(imgContext).load(uri).into(imgHint);
            }

        }

        /**
         * Sets the image by downsizing and decoding the image string, then putting the image
         * into the recyclerView at the specified position in the imgHint space
         *
         * @param url
         */
        public void setImageHint(String url) {
            if(hasHintImage) {
                Uri uri = Uri.parse(url);
                Context imgContext = imgHint.getContext();
                Picasso.with(imgContext).load(uri).into(imgHint);
            }
        }

        /**
         * Sets the image by downsizing and decoding the image string, then putting the image
         * into the recyclerView at the specified position in the imgComp space
         *
         * @param url
         */
        public void setImageComp(String url) {
            if(hasCompImage) {
                Uri uri = Uri.parse(url);
                Context imgContext = imgCompImage.getContext();
                Picasso.with(imgContext).load(uri).into(imgCompImage);
            }
        }

        @Override
        public void onClick(View v) {
            swapExpanded();
        }
    }
}
