package uk.ac.imperial.doc.mfldb.ui;

import javafx.scene.image.Image;

/**
 * Created by graham on 10/04/14.
 */
final class Const {

    static final String MAIN_WINDOW_FXML = "MainWindow.fxml";

    static final String MAIN_WINDOW_TITLE = "My First Little Debugger";
    static final int MAIN_WINDOW_WIDTH = 1000;
    static final int MAIN_WINDOW_HEIGHT = 700;

    static final String DEFAULT_PACKAGE_LABEL = "(default package)";

    static final String CLASS_IMAGE_PATH = "class.png";
    static final Image CLASS_IMAGE = new Image(Const.class.getResourceAsStream(CLASS_IMAGE_PATH));

    static final String PACKAGE_IMAGE_PATH = "package.png";
    static final Image PACKAGE_IMAGE = new Image(Const.class.getResourceAsStream(PACKAGE_IMAGE_PATH));

    static final String RUN_BUTTON_LABEL = "Run";
    static final String RUN_IMAGE_PATH = "run@2x.png";
    static final Image RUN_IMAGE = new Image(Const.class.getResourceAsStream(RUN_IMAGE_PATH));

    static final String RERUN_BUTTON_LABEL = "Rerun";
    static final String RERUN_IMAGE_PATH = "rerun@2x.png";
    static final Image RERUN_IMAGE = new Image(Const.class.getResourceAsStream(RERUN_IMAGE_PATH));

    static final String SUSPEND_BUTTON_LABEL = "Pause";
    static final String SUSPEND_IMAGE_PATH = "suspend@2x.png";
    static final Image SUSPEND_IMAGE = new Image(Const.class.getResourceAsStream(SUSPEND_IMAGE_PATH));

    static final String RESUME_BUTTON_LABEL = "Resume";
    static final String RESUME_IMAGE_PATH = "resume@2x.png";
    static final Image RESUME_IMAGE = new Image(Const.class.getResourceAsStream(RESUME_IMAGE_PATH));

    static final String CODEAREA_HTML = "CodeArea.html";

    private Const() {
        // No instances
    }
}
