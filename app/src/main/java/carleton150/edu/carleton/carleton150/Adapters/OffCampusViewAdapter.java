package carleton150.edu.carleton.carleton150.Adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.Interfaces.RecyclerViewClickListener;
import carleton150.edu.carleton.carleton150.Models.BitmapWorkerTask;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoObject;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;
import carleton150.edu.carleton.carleton150.R;

/**
 * Created by haleyhinze on 4/4/16.
 */
public class OffCampusViewAdapter extends RecyclerView.Adapter<OffCampusViewAdapter.PoiViewHolder> {

    ArrayList<GeofenceInfoContent> geofenceInfoContents = null;
    public int screenWidth;
    public int screenHeight;
    private static Constants constants = new Constants();
    public static RecyclerViewClickListener clickListener;

    public OffCampusViewAdapter(ArrayList<GeofenceInfoContent> geofenceInfoContents, int screenWidth, int screenHeight, RecyclerViewClickListener clickListener){
        this.geofenceInfoContents = geofenceInfoContents;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.clickListener = clickListener;
    }

    /**
     * inflates the itemView with a quest_card
     *
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public PoiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.list_view_poi, parent, false);
        return new PoiViewHolder(itemView);
    }



    /**
     * Binds a view holder after setting the necessary fields
     * @param holder the holder to be recycled
     * @param position the position of the holder being created
     */
    @Override
    public void onBindViewHolder(PoiViewHolder holder, int position) {
        GeofenceInfoContent geofence = geofenceInfoContents.get(position);
        holder.setTitle(geofence.getName());
        holder.setWidth(screenWidth);
        if(geofence.getType().equals(geofence.TYPE_IMAGE)) {
            holder.setImage(position, geofence.getData(), screenWidth, screenHeight);
        }
    }

    /**
     * gets the count of the list this is an adapter for
     *
     * @return the number of items in the questList
     */
    @Override
    public int getItemCount() {
        if(geofenceInfoContents != null) {
            return geofenceInfoContents.size();
        }else{
            return 0;
        }
    }



    /**
     * RecyclerView.ViewHolder for a quest view
     */
    public static class PoiViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView title;
        private ImageView image;


        public PoiViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.txt_poi);
            image = (ImageView) itemView.findViewById(R.id.img_poi);
            itemView.setOnClickListener(this);
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
            if(title != null) {
                this.title.setText(title);
            }
        }

        /**
         * @param width
         */
        public void setWidth(int width) {
            itemView.setLayoutParams(new RecyclerView.LayoutParams(width, RecyclerView.LayoutParams.MATCH_PARENT));
        }

        /**
         * Sets the image by downsizing and decoding the image string, then putting the image
         * into the recyclerView at the specified position in the image ImageView
         *
         * @param resId position of image in RecyclerView
         * @param encodedImage 64-bit encoded image
         * @param screenWidth width of phone screen
         * @param screenHeight height of phone screen
         */
        public void setImage(int resId, String encodedImage, int screenWidth, int screenHeight) {
            System.gc();
            if(encodedImage == null) {
                image.setImageResource(R.drawable.test_image1);
                image.setColorFilter(R.color.blackSemiTransparent);
            }else {
                int w = constants.PLACEHOLDER_IMAGE_DIMENSIONS, h = constants.PLACEHOLDER_IMAGE_DIMENSIONS;
                Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                Bitmap mPlaceHolderBitmap = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
                if (cancelPotentialWork(resId, image)) {
                    final BitmapWorkerTask task = new BitmapWorkerTask(image, encodedImage
                            , screenWidth / 5, screenHeight / 5);
                    final BitmapWorkerTask.AsyncDrawable asyncDrawable =
                            new BitmapWorkerTask.AsyncDrawable(mPlaceHolderBitmap, task);
                    image.setImageDrawable(asyncDrawable);
                    task.execute(resId);
                }
            }
        }

        /**
         * Cancels the previous task if a view is recycled so it can use the correct image
         *
         * @param data
         * @param imageView
         * @return
         */
        public static boolean cancelPotentialWork(int data, ImageView imageView) {
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (bitmapWorkerTask != null) {
                final int bitmapData = bitmapWorkerTask.data;
                // If bitmapData is not yet set or it differs from the new data
                if (bitmapData == 0 || bitmapData != data) {
                    // Cancel previous task
                    bitmapWorkerTask.cancel(true);
                } else {
                    // The same work is already in progress
                    return false;
                }
            }
            // No task associated with the ImageView, or an existing task was cancelled
            return true;
        }

        /**
         * Gets the worker task that is trying to decode an image for the imageView
         * @param imageView
         * @return the new BitmapWorkerTask for the imageView
         */
        private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
            if (imageView != null) {
                final Drawable drawable = imageView.getDrawable();
                if (drawable instanceof BitmapWorkerTask.AsyncDrawable) {
                    final BitmapWorkerTask.AsyncDrawable asyncDrawable = (BitmapWorkerTask.AsyncDrawable) drawable;
                    return asyncDrawable.getBitmapWorkerTask();
                }
            }
            return null;
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
