package edu.carleton.app.reunion.Interfaces;

import edu.carleton.app.reunion.MainFragments.MainFragment;

/**
 * Created by haleyhinze on 4/27/16.
 */
public interface QuestStartedListener {

    public void questStarted(MainFragment newFragment);

    public void goBackToQuestScreen();

    public void questCompleted(MainFragment fragment);
}
