package carleton150.edu.carleton.carleton150.LayoutManagers;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Created by haleyhinze on 5/4/16.
 */
public class SnapToCenterRecyclerView extends RecyclerView{
    private int screenWidth = 0;
    private float x1 = 0;
    private float x2 = 0;
    static final int MIN_DISTANCE = 500;

    public SnapToCenterRecyclerView(Context context) {
        super(context);
    }

    public SnapToCenterRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnapToCenterRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setScreenWidth(int screenWidth){
        this.screenWidth = screenWidth;
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        int scrollDistanceLeft = calculateScrollDistanceLeft();
        int scrollDistanceRight = calculateScrollDistanceRight();

        //if(user swipes to the left)
        if(velocityX > 0){
            smoothScrollBy(scrollDistanceLeft, 0);
        }
        else {
            smoothScrollBy(-scrollDistanceRight, 0);
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // We want the parent to handle all touch events--there's a lot going on there,
        // and there is no reason to overwrite that functionality--bad things will happen.
        final boolean ret = super.onTouchEvent(e);
        final LayoutManager lm = getLayoutManager();

        if((e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_DOWN)
                && getScrollState() == SCROLL_STATE_IDLE) {
            // The layout manager is a SnappyLayoutManager, which means that the
            // children should be snapped to a grid at the end of a drag or
            // fling. The motion event is either a user lifting their finger or
            // the cancellation of a motion events, so this is the time to take
            // over the scrolling to perform our own functionality.
            // Finally, the scroll state is idle--meaning that the resultant
            // velocity after the user's gesture was below the threshold, and
            // no fling was performed, so the view may be in an unaligned state
            // and will not be flung to a proper state.
            int scrollDistanceLeft = calculateScrollDistanceLeft();
            int scrollDistanceRight = calculateScrollDistanceRight();

            if(e.getAction() == MotionEvent.ACTION_DOWN){
                x1 = e.getX();
                Log.i("SCROLLING", "set x1: " + x1);
            }if(e.getAction() == MotionEvent.ACTION_UP){
                x2 = e.getX();
                Log.i("SCROLLING", " x2: " + x2);
            }

            Log.i("SCROLLING", "x1: " + x1 + " x2: " + x2);


            float deltaX = x2 - x1;
                    if (Math.abs(deltaX) > MIN_DISTANCE) {
                        // Left to Right swipe action
                        if (x2 > x1) {
                            Log.i("SCROLLING", "ScrollDistanceRight: " + scrollDistanceRight);
                            smoothScrollBy(-scrollDistanceRight, 0);
                        }
                        // Right to left swipe action
                        else {
                            Log.i("SCROLLING", "ScrollDistanceLeft: " + scrollDistanceLeft);
                            smoothScrollBy(scrollDistanceLeft, 0);
                        }
                    }
            }


        return ret;
    }

    private int calculateScrollDistanceRight(){
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();

        //these variables identify the views you see on screen.
        int firstVisibleView = linearLayoutManager.findFirstVisibleItemPosition();
        View firstView = linearLayoutManager.findViewByPosition(firstVisibleView);

    //these variables get the distance you need to scroll in order to center your views.
    //my views have variable sizes, so I need to calculate side margins separately.
    // note the subtle difference in how right and left margins are calculated, as well as
    //the resulting scroll distances.
        int rightMargin = (screenWidth - firstView.getWidth()) / 2 + firstView.getWidth();
        int rightEdge = firstView.getRight();
        return rightMargin - rightEdge;
    }

    private int calculateScrollDistanceLeft(){
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();

        //these variables identify the views you see on screen.
        int lastVisibleView = linearLayoutManager.findLastVisibleItemPosition();
        View lastView = linearLayoutManager.findViewByPosition(lastVisibleView);

        //these variables get the distance you need to scroll in order to center your views.
        //my views have variable sizes, so I need to calculate side margins separately.
        //note the subtle difference in how right and left margins are calculated, as well as
        //the resulting scroll distances.
        int leftMargin = (screenWidth - lastView.getWidth()) / 2;
        int leftEdge = lastView.getLeft();
        return leftEdge - leftMargin;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if(e.getAction() == MotionEvent.ACTION_DOWN){
            return true;
        }else{
            return super.onInterceptTouchEvent(e);
        }
    }
}
