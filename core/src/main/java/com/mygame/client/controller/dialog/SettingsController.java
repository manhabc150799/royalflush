package com.mygame.client.controller.dialog;

    import com.badlogic.gdx.scenes.scene2d.Actor;
    import com.github.czyzby.autumn.annotation.Inject;
    import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
    import com.github.czyzby.autumn.mvc.stereotype.ViewDialog;
    import com.github.czyzby.lml.annotation.LmlAction;
    import com.github.czyzby.lml.parser.action.ActionContainer;
    import com.kotcrab.vis.ui.VisUI.SkinScale;
    import com.mygame.client.service.NetworkService;
    import com.mygame.client.service.SessionManager;
    import com.mygame.client.service.ScaleService;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    /** This is a settings dialog, which can be shown in any view by using "show:settings" LML action or - in Java code -
     * through InterfaceService.showDialog(Class) method. Thanks to the fact that it implements ActionContainer, its methods
     * will be available in the LML template. */
    @ViewDialog(id = "settings", value = "ui/templates/dialogs/settings.lml")
    public class SettingsController implements ActionContainer {
        private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
        
        // @Inject-annotated fields will be automatically filled with values from the context.
        @Inject private ScaleService scaleService;
        @Inject private SessionManager sessionManager;
        @Inject private NetworkService networkService;
        @Inject private InterfaceService interfaceService;

        /** @return array of available GUI scales. */
        @LmlAction("scales")
        public SkinScale[] getGuiScales() {
            return scaleService.getScales();
        }

        /** @param actor requested scale change. Its ID represents a GUI scale. */
        @LmlAction("changeScale")
        public void changeGuiScale(final Actor actor) {
            final SkinScale scale = scaleService.getPreference().extractFromActor(actor);
            scaleService.changeScale(scale);
        }
        
        /**
         * Action: Thoát game (logout và quay về login)
         */
        @LmlAction("outGame")
        public void outGame() {
            logger.info("Người chơi đã chọn thoát game");
            
            // Logout
            sessionManager.logout();
            
            // Ngắt kết nối network
            networkService.disconnect();
            
            // Chuyển về login screen
            interfaceService.show(com.mygame.client.controller.LoginController.class);
        }
    }