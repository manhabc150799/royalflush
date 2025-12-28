package com.mygame.client.controller.action;

import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.stereotype.ViewActionContainer;
import com.github.czyzby.autumn.annotation.Initiate;
import com.github.czyzby.lml.annotation.LmlAction;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.mygame.client.controller.LoginController;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Action container for login view actions.
 * Separated from LoginController to ensure proper registration timing.
 * Similar pattern to Global action container.
 * NOTE: Global doesn't have @Component but still works - may need to test without it.
 */
@Component
@ViewActionContainer("login")
public class LoginActions implements ActionContainer {

    public LoginActions() {
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            Method[] methods = getClass().getDeclaredMethods();
            int actionCount = 0;
            StringBuilder names = new StringBuilder();
            for (Method m : methods) {
                if (m.isAnnotationPresent(LmlAction.class)) {
                    actionCount++;
                    if (names.length() > 0) {
                        names.append(",");
                    }
                    names.append(m.getName());
                }
            }
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"pre-fix\",\"hypothesisId\":\"H2\",\"timestamp\":" +
                    System.currentTimeMillis() +
                    ",\"location\":\"LoginActions.java:constructor\",\"message\":\"LoginActions constructed\",\"data\":{\"actionCount\":" +
                    actionCount + ",\"actionNames\":\"" + names + "\"}}\n");
        } catch (IOException ignored) {}
        // #endregion
    }
    
    @Inject private LoginController loginController;
    
    @Initiate
    private void init() {
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"H1\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoginActions.java:25\",\"message\":\"LoginActions init\",\"data\":{\"loginControllerNull\":" + (loginController == null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
    }
    
    /**
     * Action called when Login button is clicked
     */
    @LmlAction("attemptLogin")
    public void attemptLogin() {
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"H1\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoginActions.java:35\",\"message\":\"attemptLogin action invoked\",\"data\":{\"loginControllerNull\":" + (loginController == null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
        if (loginController != null) {
            loginController.attemptLogin();
        }
    }
    
    /**
     * Action called when Register button is clicked
     */
    @LmlAction("attemptRegister")
    public void attemptRegister() {
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"H1\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoginActions.java:48\",\"message\":\"attemptRegister action invoked\",\"data\":{\"loginControllerNull\":" + (loginController == null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
        if (loginController != null) {
            loginController.attemptRegister();
        }
    }
}
