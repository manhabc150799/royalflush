package com.mygame.client.controller.dialog;

import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.stereotype.ViewDialog;
import com.github.czyzby.lml.annotation.LmlAction;
import com.github.czyzby.lml.annotation.LmlActor;
import com.github.czyzby.lml.annotation.LmlAfter;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.MatchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog chọn chế độ chơi (Multiplayer hoặc Singleplayer)
 */
@ViewDialog(id = "gameMode", value = "ui/templates/dialogs/game_mode_selection.lml")
public class GameModeSelectionDialog implements ActionContainer {
    private static final Logger logger = LoggerFactory.getLogger(GameModeSelectionDialog.class);
    
    @Inject private InterfaceService interfaceService;
    
    @LmlActor("gameTypeLabel") private VisLabel gameTypeLabel;
    
    private GameType selectedGameType;
    private GameModeCallback callback;
    
    /**
     * Interface để callback khi chọn mode
     */
    public interface GameModeCallback {
        void onModeSelected(GameType gameType, MatchMode matchMode);
    }
    
    @LmlAfter
    public void initialize() {
        if (gameTypeLabel != null && selectedGameType != null) {
            gameTypeLabel.setText(selectedGameType.name());
        }
    }
    
    /**
     * Set game type và callback
     */
    public void setup(GameType gameType, GameModeCallback callback) {
        this.selectedGameType = gameType;
        this.callback = callback;
        
        if (gameTypeLabel != null) {
            gameTypeLabel.setText(gameType.name());
        }
    }
    
    /**
     * Action: Chọn Multiplayer
     */
    @LmlAction("selectMultiplayer")
    public void selectMultiplayer() {
        logger.info("Đã chọn Multiplayer cho game: {}", selectedGameType);
        
        if (callback != null && selectedGameType != null) {
            callback.onModeSelected(selectedGameType, MatchMode.MULTIPLAYER);
        }
        
        // Close dialog - Autumn MVC sẽ tự đóng khi show view mới
    }
    
    /**
     * Action: Chọn Singleplayer
     */
    @LmlAction("selectSingleplayer")
    public void selectSingleplayer() {
        logger.info("Đã chọn Singleplayer cho game: {}", selectedGameType);
        
        if (callback != null && selectedGameType != null) {
            callback.onModeSelected(selectedGameType, MatchMode.SINGLEPLAYER);
        }
        
        // Close dialog - Autumn MVC sẽ tự đóng khi show view mới
    }
}

