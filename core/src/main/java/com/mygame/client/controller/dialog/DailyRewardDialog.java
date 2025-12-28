package com.mygame.client.controller.dialog;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.stereotype.ViewDialog;
import com.github.czyzby.lml.annotation.LmlActor;
import com.github.czyzby.lml.annotation.LmlAfter;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.kotcrab.vis.ui.widget.VisLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dialog hiển thị daily reward với animation
 */
@ViewDialog(id = "dailyReward", value = "ui/templates/dialogs/daily_reward.lml")
public class DailyRewardDialog implements ActionContainer {
    private static final Logger logger = LoggerFactory.getLogger(DailyRewardDialog.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    
    @Inject private InterfaceService interfaceService;
    
    @LmlActor("creditsLabel") private VisLabel creditsLabel;
    @LmlActor("nextRewardLabel") private VisLabel nextRewardLabel;
    @LmlActor("rewardIcon") private Actor rewardIcon;
    
    private long creditsReceived;
    private LocalDateTime nextRewardTime;
    
    @LmlAfter
    public void initialize() {
        // Animation cho icon
        if (rewardIcon != null) {
            rewardIcon.addAction(Actions.repeat(-1, Actions.sequence(
                Actions.scaleTo(1.1f, 1.1f, 0.5f),
                Actions.scaleTo(1f, 1f, 0.5f)
            )));
        }
        
        // Animation cho credits label
        if (creditsLabel != null) {
            creditsLabel.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.5f),
                Actions.scaleTo(1.2f, 1.2f, 0.3f),
                Actions.scaleTo(1f, 1f, 0.3f)
            ));
        }
    }
    
    /**
     * Setup dialog với credits và next reward time
     */
    public void setup(long credits, LocalDateTime nextTime) {
        this.creditsReceived = credits;
        this.nextRewardTime = nextTime;
        
        if (creditsLabel != null) {
            creditsLabel.setText(String.format("%,d", credits));
        }
        
        if (nextRewardLabel != null && nextTime != null) {
            nextRewardLabel.setText("Lần nhận tiếp theo: " + nextTime.format(TIME_FORMAT));
        }
    }
}

