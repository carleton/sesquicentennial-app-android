package carleton150.edu.carleton.reunion.Interfaces;

import carleton150.edu.carleton.reunion.MainFragments.MainFragment;

/**
 * Created by haleyhinze on 4/27/16.
 */
public interface QuestStartedListener {

    public void questStarted(MainFragment newFragment);

    public void goBackToQuestScreen();

    public void questCompleted(MainFragment fragment);
}
