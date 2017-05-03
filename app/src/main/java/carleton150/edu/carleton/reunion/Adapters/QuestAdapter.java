package carleton150.edu.carleton.reunion.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import carleton150.edu.carleton.reunion.Interfaces.RecyclerViewClickListener;
import carleton150.edu.carleton.reunion.POJO.Quests.Quest;
import carleton150.edu.carleton.reunion.R;


/**
 * Adapter for the RecyclerView in the QuestFragment
 */
public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.QuestViewHolder> {

    private ArrayList<Quest> questList = new ArrayList<>();
    public static RecyclerViewClickListener clickListener;
    private Resources resources;
    private SharedPreferences sharedPreferences;

    public QuestAdapter(ArrayList<Quest> questList, RecyclerViewClickListener clickListener, Resources resources, SharedPreferences sharedPreferences) {
        this.questList = questList;
        this.clickListener = clickListener;
        this.resources = resources;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * inflates the itemView with a quest_card
     *
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public QuestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.quest_card, parent, false);
        return new QuestViewHolder(itemView);
    }

    /**
     * Binds a view holder after setting the necessary fields
     * @param holder the holder to be recycled
     * @param position the position of the holder being created
     */
    @Override
    public void onBindViewHolder(QuestViewHolder holder, int position) {
        Quest qi = questList.get(position);
        holder.setTitle(qi.getName());
        holder.setDescription(qi.getDesc());
        holder.setDifficulty(qi.getDifficulty(), resources);
        holder.setCreator(qi.getCreator());
        holder.setTargetAudience(qi.getAudience(), resources);

        Uri uri = Uri.parse(qi.getImage());
        Context imgContext = holder.getImageView().getContext();
        Picasso.with(imgContext).load(uri).into(holder.getImageView());

        double cluesCompleted = sharedPreferences.getInt(qi.getName(), 0);
        double percentCompleted = 0;
        if(cluesCompleted != 0){
            percentCompleted = cluesCompleted/(double)qi.getWaypoints().length * 100;
        }
        holder.setPercentCompleted((int) percentCompleted);
    }

    /**
     * gets the count of the list this is an adapter for
     *
     * @return the number of items in the questList
     */
    @Override
    public int getItemCount() {
        if(questList != null) {
            return questList.size();
        }else{
            return 0;
        }
    }

    public void updateQuestList(ArrayList<Quest> newQuests){
        questList = newQuests;
    }

    public ArrayList<Quest> getQuestList(){
        return this.questList;
    }


    /**
     * RecyclerView.ViewHolder for a quest view
     */
    public static class QuestViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView title;
        private ImageView image;
        private TextView creator;
        private TextView txtDifficulty;
        private TextView description;
        private LinearLayout layoutRatings;
        private Button btnBeginQuest;
        private TextView intendedAudience;


        public QuestViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.txtTitle);
            image = (ImageView) itemView.findViewById(R.id.img_quest);
            description = (TextView) itemView.findViewById(R.id.txt_quest_description);
            layoutRatings = (LinearLayout) itemView.findViewById(R.id.lin_layout_ratings);
            btnBeginQuest = (Button) itemView.findViewById(R.id.btn_start_quest);
            txtDifficulty = (TextView) itemView.findViewById(R.id.txt_difficulty);
            creator = (TextView) itemView.findViewById(R.id.txt_creator);
            intendedAudience = (TextView) itemView.findViewById(R.id.txt_intended_audience);
            btnBeginQuest.setOnClickListener(this);
        }

        public ImageView getImageView(){
            return this.image;
        }


        /**
         * Sets the button to display the percentage of the quest that was completed
         * @param percentCompleted
         */
        public void setPercentCompleted(int percentCompleted){
            if(percentCompleted == 0){
                btnBeginQuest.setText("Begin Quest");
            }else {
                btnBeginQuest.setText(percentCompleted + "% Completed");
            }
        }

        public void setDescription(String description) {
            this.description.setText(description);
        }

        /**
         * Fills in circles with the necessary colors to show the quest difficulty
         * and sets the quest difficulty string
         *
         * @param difficulty
         * @param resources
         */
        public void setDifficulty(String difficulty, Resources resources) {
            int difficultyInt = 0;
            difficultyInt = Integer.parseInt(difficulty);
            String difficultyString = resources.getString(R.string.no_rating);
            int colorInt = resources.getColor(R.color.windowBackground);
            if(difficultyInt == 0){
                difficultyString = resources.getString(R.string.easy);
                colorInt = resources.getColor(R.color.green);
            }if(difficultyInt == 1){
                difficultyString = resources.getString(R.string.medium);
                colorInt = resources.getColor(R.color.orange);
            }if(difficultyInt == 2){
                difficultyString = resources.getString(R.string.hard);
                colorInt = resources.getColor(R.color.red);
            }if(difficultyInt > 2){
                difficultyInt = 2;
            }
            for(int i = 0; i <= difficultyInt; i++){
                View view = this.layoutRatings.getChildAt(i);
                GradientDrawable background = (GradientDrawable) view.getBackground();
                background.setColor(colorInt);
            }for(int i = difficultyInt+1; i<3; i++){
                View view = this.layoutRatings.getChildAt(i);
                GradientDrawable background = (GradientDrawable) view.getBackground();
                background.setColor(resources.getColor(R.color.windowBackground));
            }
            txtDifficulty.setText(difficultyString);
        }

        /**
         * @return title
         */
        public String getTitle() {
            return (String) title.getText();
        }

        /**
         * @param title
         */
        public void setTitle(String title) {
            this.title.setText(title);
        }

        /**
         * @param width
         */
        public void setWidth(int width) {
            itemView.setLayoutParams(new RecyclerView.LayoutParams(width, RecyclerView.LayoutParams.MATCH_PARENT));
        }

        /**
         * @param creator
         */
        public void setCreator(String creator) {
            if(creator != null) {
                if(!creator.equals("")) {
                    this.creator.setVisibility(View.VISIBLE);
                    this.creator.setText(creator);
                }
                else{
                    this.creator.setVisibility(View.GONE);
                }
            }else{
                this.creator.setVisibility(View.GONE);
            }
        }

        /**
         * @param targetAudience
         */
        public void setTargetAudience(String targetAudience, Resources resources) {
            if(targetAudience != null) {
                if(!targetAudience.equals("")) {
                    this.intendedAudience.setVisibility(View.VISIBLE);
                    this.intendedAudience.setText(resources.getString(R.string.intended_for) + " " + targetAudience);
                }
                else{
                    this.intendedAudience.setVisibility(View.GONE);
                }
            }else{
                this.intendedAudience.setVisibility(View.GONE);
            }
        }

        /**
         * Tells clickListener the item that was clicked so it can begin
         * the specified quest
         * @param v
         */
        @Override
        public void onClick(View v) {
            clickListener.recyclerViewListClicked(v, getLayoutPosition());
        }
    }

}
