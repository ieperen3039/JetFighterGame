package nl.NG;

import nl.NG.Tools.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import static java.awt.GridBagConstraints.*;
import static nl.NG.SwingToolbox.*;

/**
 * @author Geert van Ieperen. Created on 23-8-2018.
 */
public class LauncherMain {
    private static final String[] BACKDROP_IMAGE = {"Backdrop.png", "Passage1.png", "Passage2.png", "Seekers.png", "Seekers2.png", "Seekers3.png"};
    private static final Path jarName = Directory.gameJar.getPath("GameRunnable.jar");

    private static final Dimension MINIMUM_LAUNCHER_SIZE = new Dimension(900, 600);
    private static final Dimension DEFAULT_LAUNCHER_SIZE = new Dimension(1200, 700);
    private static final int COLUMNS_OF_SETTINGS = 3;
    public static final Color ERROR_TEXT_COLOR = new Color(1f, 0.3f, 0.3f);
    public static final Color REGULAR_TEXT_COLOR = Color.WHITE;
    private static final float BASE_FONT_SIZE = 14f;
    private static final float LARGE_FONT_SIZE = 24f;

    private static final File fontFileLucidaConsole = Directory.fonts.getFile("LucidaConsole", "lucon.ttf");
    private static final Random RANDOM = new Random();
    private final Tables names;

    private final Font largeFont;

    /** settings */
    private String map;
    private boolean hideLauncherWhenRunning = false;
    private String playerName = null;

    /** main frame */
    private JFrame frame;

    /** panels */
    private JComponent mainMenuPanel;
    private JComponent imagePanel;
    private JComponent ipSearchPanel;
    private JComponent loadoutPanel;
    private JComponent settingsPanel;
    private JComponent keyBindingPanel;
    private JComponent replaySelectPanel;
    private JComponent debugOutputPanel;
    private JComponent updatePanel;
    private JComponent defaultPanel;
    private JComponent[] mutexPanels;

    public LauncherMain() throws IOException, FontFormatException {
        Font lucidaConsole = Font.createFont(Font.TRUETYPE_FONT, fontFileLucidaConsole);
        largeFont = lucidaConsole.deriveFont(LARGE_FONT_SIZE);
        names = new Tables(Directory.tables.getFile("tables.tb"));
        map = names.findWorld("ISLAND");
    }

    public void init() {
        frame = new JFrame();
        frame.setTitle(LauncherSettings.GAME_NAME + " Launcher");
        frame.setMinimumSize(MINIMUM_LAUNCHER_SIZE);

        imagePanel = SwingToolbox.getImagePanel(randomBackdrop());
        imagePanel.setLayout(new GridBagLayout());

        mainMenuPanel = getMainMenuPanel();
        imagePanel.add(mainMenuPanel, SwingToolbox.getConstraints(0, 0, BOTH));

        ipSearchPanel = getIPSearchPanel();
        loadoutPanel = getLoadoutPanel();
        settingsPanel = getSettingsPanel();
        keyBindingPanel = getKeyBindingPanel();
        replaySelectPanel = getReplaySelectPanel(Directory.recordings);
        debugOutputPanel = getConsolePanel();
        updatePanel = getUpdatePanel(Directory.gameJar);
        defaultPanel = getDefaultPanel();

        mutexPanels = new JComponent[]{
                defaultPanel, ipSearchPanel, loadoutPanel, settingsPanel,
                keyBindingPanel, replaySelectPanel, debugOutputPanel, updatePanel
        };
        for (JComponent panel : mutexPanels) {
            panel.setVisible(false);
            imagePanel.add(panel, SwingToolbox.getFillConstraints(1, 0));
        }
        select(null);

        frame.add(imagePanel);

        frame.setSize(DEFAULT_LAUNCHER_SIZE);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point centerPoint = ge.getCenterPoint();

        int dx = centerPoint.x - (frame.getWidth() / 2);
        int dy = centerPoint.y - (frame.getHeight() / 2);

        frame.setLocation(dx, dy);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(DEFAULT_LAUNCHER_SIZE);
    }

    private File randomBackdrop() {
        File file;
        while (true) {
            int index = RANDOM.nextInt(BACKDROP_IMAGE.length);
            String image = BACKDROP_IMAGE[index];
            file = Directory.pictures.getFile(image);
            if (!file.exists()) {
                Logger.WARN.print("Could not load " + file);
            } else break;
        }

        return file;
    }

    private void reboot() {
        boolean wasVisible = frame.isVisible();
        close();
        frame.dispose();
        init();
        if (wasVisible) show();
    }

    private JComponent getDefaultPanel() {
        return LauncherSettings.DEBUG ? debugOutputPanel : SwingToolbox.invisiblePanel();
    }

    private JComponent getSettingsPanel() {
        JPanel panel = SwingToolbox.invisiblePanel();
        panel.setLayout(new GridBagLayout());

        final int cols = (COLUMNS_OF_SETTINGS * 2) - 1;
        ArrayList<Setting> settings = new ArrayList<>();
        boolean[] doReboot = new boolean[1];

        // for the title
        GridBagConstraints titlePosition = new GridBagConstraints(0, 1, cols, 1, 1, 1, CENTER, HORIZONTAL, borderOf(20), 0, 0);

        JTextComponent title = SwingToolbox.flyingText();
        title.setFont(largeFont);
        title.setText("Settings");
        panel.add(title, titlePosition);

        settings.add(getBooleanSetting(panel, column(1), "Debug mode",
                LauncherSettings.DEBUG, (result) -> {
                    doReboot[0] = true;
                    LauncherSettings.DEBUG = result;
                }));

        settings.add(getBooleanSetting(panel, column(1), "Menu Transparency",
                LauncherSettings.ALLOW_FLYING_TEXT, (result) -> {
                    doReboot[0] = true;
                    LauncherSettings.ALLOW_FLYING_TEXT = result;
                }));

        settings.add(getBooleanSetting(panel, column(1), "Hide launcher while playing",
                hideLauncherWhenRunning, (result) -> hideLauncherWhenRunning = result));

        settings.add(getChoiceSetting(panel, column(1), "Logger output level",
                Logger.values(), Logger.getLoggingLevel(), Logger::setLoggingLevel));

        panel.add(getFiller(), getButtonConstraints(column(1), RELATIVE));

        settings.add(getTextboxSetting(panel, column(2), "Target FPS",
                LauncherSettings.TARGET_FPS, (s) -> LauncherSettings.TARGET_FPS = Integer.valueOf(s)));

        settings.add(getTextboxSetting(panel, column(2), "Render Delay (sec)",
                LauncherSettings.RENDER_DELAY, (s) -> LauncherSettings.RENDER_DELAY = Float.valueOf(s)));

        settings.add(getSliderSetting(panel, column(2), "Field of View (deg)",
                (f) -> LauncherSettings.FOV = f, 0.35f, 2.10f, LauncherSettings.FOV));

        settings.add(getSliderSetting(panel, column(2), "Particle Modifier (exp scale)",
                (f) -> LauncherSettings.PARTICLE_MODIFIER = (float) Math.exp(f), -2, 3, (float) Math.log(LauncherSettings.PARTICLE_MODIFIER)));

        settings.add(getTextboxSetting(panel, column(2), "Player Name",
                getPlayerName(), (s) -> playerName = s));

        panel.add(getFiller(), getButtonConstraints(column(2), RELATIVE));

        settings.add(getTextboxSetting(panel, column(3), "Server Port",
                LauncherSettings.SERVER_PORT, (s) -> LauncherSettings.SERVER_PORT = Integer.valueOf(s)));

        settings.add(getBooleanSetting(panel, column(3), "Save Replays",
                LauncherSettings.MAKE_REPLAY, (result) -> LauncherSettings.MAKE_REPLAY = result));

        settings.add(getTextboxSetting(panel, column(3), "Server Ticks per second",
                LauncherSettings.TARGET_TPS, (s) -> LauncherSettings.TARGET_TPS = Integer.valueOf(s)));

        settings.add(getChoiceSetting(panel, column(3), "Race map",
                names.getWorlds(), map, (m) -> map = m));

        panel.add(getFiller(), getButtonConstraints(column(3), RELATIVE));

        for (int i = 0; i < COLUMNS_OF_SETTINGS - 1; i++) {
            panel.add(getFiller(), getFillConstraints((i * 2) + 1, 1, 1, 1));
        }
        panel.add(getFiller(), getFillConstraints(0, RELATIVE, cols, 1));

        JButton applyButton = SwingToolbox.getButton("Apply and close");
        panel.add(applyButton, SwingToolbox.getButtonConstraints(cols - 1, RELATIVE));
        applyButton.addActionListener(e -> {
            settings.forEach(Setting::apply);
            select(null);
            if (doReboot[0]) reboot();
            doReboot[0] = false;
        });

        JButton cancelButton = SwingToolbox.getButton("Cancel");
        panel.add(cancelButton, SwingToolbox.getButtonConstraints(0, RELATIVE));
        cancelButton.addActionListener(e -> {
            settings.forEach(Setting::reset);
            select(null);
        });

        JScrollPane scrollable = scrollable(panel);
        scrollable.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                settings.forEach(Setting::apply); // reset is more logic, but in practise this is preferred
            }
        });
        return scrollable;
    }

    private String getPlayerName() {
        return playerName != null ? playerName : "TheLegend" + RANDOM.nextInt(1000);
    }

    private JComponent getKeyBindingPanel() {
        JPanel panel = SwingToolbox.invisiblePanel();
        panel.setLayout(new GridBagLayout());

        final int cols = (COLUMNS_OF_SETTINGS * 2) - 1;

        // for the title
        GridBagConstraints titlePosition = new GridBagConstraints(0, 1, cols, 1, 1, 1, CENTER, HORIZONTAL, borderOf(20), 0, 0);

        JTextComponent title = SwingToolbox.flyingText();
        title.setFont(largeFont);
        title.setText("Keybindings");
        panel.add(title, titlePosition);

        for (KeyBinding key : KeyBinding.getActionButtons()) {
            panel.add(getKeyPanel(key, frame), getButtonConstraints(column(1), RELATIVE));
        }
        for (KeyBinding key : KeyBinding.getControlButtons()) {
            panel.add(getKeyPanel(key, frame), getButtonConstraints(column(2), RELATIVE));
        }

        for (int i = 0; i < COLUMNS_OF_SETTINGS - 1; i++) {
            panel.add(getFiller(), getFillConstraints((i * 2) + 1, 1, 1, 1));
        }
        panel.add(getFiller(), getFillConstraints(0, RELATIVE, cols, 1));

        return scrollable(panel);
    }

    /** @return the column number for a setting of type i, where i > 0 */
    private static int column(int i) {
        return ((i - 1) % COLUMNS_OF_SETTINGS) * 2;
    }

    private JPanel getLoadoutPanel() {
        JPanel panel = SwingToolbox.invisiblePanel();
        panel.setLayout(new GridBagLayout());

        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(1, 0));
        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(0, 0, 1, REMAINDER));

        JTextComponent text = SwingToolbox.flyingText();
        text.setText("Choose a jet:");
        panel.add(text, SwingToolbox.getButtonConstraints(RELATIVE));

        for (String jet : names.getJets()) {
            JButton chooseJet = SwingToolbox.getButton(jet);
            panel.add(chooseJet, SwingToolbox.getButtonConstraints(RELATIVE));
            chooseJet.addActionListener(e -> {
                LauncherSettings.JET_TYPE = jet;
                select(null);
            });
        }

        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(1, RELATIVE));
        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(2, 0, 1, REMAINDER));

        return panel;
    }

    private JPanel getIPSearchPanel() {
        JPanel panel = SwingToolbox.invisiblePanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints cons;

        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(1, 0, 2, 1));
        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(0, 0, 1, REMAINDER));

        JTextComponent messageDisplay = SwingToolbox.flyingText();
        messageDisplay.setText("Enter the target IP address");
        messageDisplay.setMinimumSize(MAIN_BUTTON_DIM);
        cons = new GridBagConstraints(1, 1, 2, 1, 0, 0, LINE_END, HORIZONTAL, borderOf(BUTTON_BORDER), 0, 0);
        panel.add(messageDisplay, cons);

        JTextField ipField = new JTextField();
        ipField.setFont(largeFont);
        ipField.setPreferredSize(SwingToolbox.TEXTBOX_DIM);
        cons = new GridBagConstraints(1, 2, 2, 1, 1, 0, LINE_END, HORIZONTAL, borderOf(BUTTON_BORDER), 0, 0);
        panel.add(ipField, cons);

        JButton searchLocal = SwingToolbox.getButton("Search Local");
        cons = new GridBagConstraints(1, 3, 1, 1, 1, 0, LINE_START, HORIZONTAL, borderOf(BUTTON_BORDER), 0, 0);
        panel.add(searchLocal, cons);

        JButton connectButton = SwingToolbox.getButton("Connect");
        cons = new GridBagConstraints(2, 3, 1, 1, 1, 0, LINE_START, HORIZONTAL, borderOf(BUTTON_BORDER), 0, 0);
        panel.add(connectButton, cons);

        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(1, 4, 2, 1));
        panel.add(SwingToolbox.getFiller(), SwingToolbox.getFillConstraints(4, 0, 1, REMAINDER));

        // implementation
        ipField.addActionListener(e -> connectButton.doClick()); // enter -> [connect]

        searchLocal.addActionListener(e -> launchGame(InetAddress.getLoopbackAddress(), null));

        connectButton.addActionListener(SwingToolbox.getActionWorker("Searching IP and connect", () -> {
            messageDisplay.setText("Searching...");
            InetAddress address = getIP(ipField.getText());
            if (address == null) {
                messageDisplay.setText("That is not a valid address");
            } else {
                messageDisplay.setText("Starting game with " + address);
                launchGame(address, null);
                select(null);
            }
        }));

        return panel;
    }

    private JPanel getMainMenuPanel() {
        JPanel panel = SwingToolbox.invisiblePanel();
        panel.setLayout(new GridBagLayout());

        JButton startLocal = SwingToolbox.getButton("Start Local Server");
        startLocal.addActionListener(e -> launchGame(null, null));
        panel.add(startLocal, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton searchInternet = SwingToolbox.getButton("Search Server");
        searchInternet.addActionListener(e -> select(ipSearchPanel));
        panel.add(searchInternet, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton searchLocal = SwingToolbox.getButton("Replay file");
        searchLocal.addActionListener(e -> select(replaySelectPanel));
        panel.add(searchLocal, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton changeLoadout = SwingToolbox.getButton("Change Loadout");
        changeLoadout.addActionListener(e -> select(loadoutPanel));
        panel.add(changeLoadout, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton showDebug = SwingToolbox.getButton("Show Console");
        showDebug.addActionListener(e -> select(debugOutputPanel));
        panel.add(showDebug, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton checkUpdates = SwingToolbox.getButton("Check for Updates");
        checkUpdates.addActionListener(e -> select(updatePanel));
        panel.add(checkUpdates, SwingToolbox.getButtonConstraints(RELATIVE));

        JPanel filler = SwingToolbox.getFiller();
        filler.setMinimumSize(new Dimension(MAIN_BUTTON_DIM.width + 2 * BUTTON_BORDER, 1));
        panel.add(filler, SwingToolbox.getFillConstraints(1, RELATIVE));

        JButton keyBindingsButton = SwingToolbox.getButton("Key Bindings");
        keyBindingsButton.addActionListener(e -> select(keyBindingPanel));
        panel.add(keyBindingsButton, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton settingsButton = SwingToolbox.getButton("Settings");
        settingsButton.addActionListener(e -> select(settingsPanel));
        panel.add(settingsButton, SwingToolbox.getButtonConstraints(RELATIVE));

        JButton exitGame = SwingToolbox.getButton("Exit Launcher");
        exitGame.addActionListener(e -> this.close());
        panel.add(exitGame, SwingToolbox.getButtonConstraints(RELATIVE));

        return panel;
    }

    private JPanel getReplaySelectPanel(Directory dir) {
        JPanel panel = new JPanel(new GridBagLayout());

        JFileChooser dialog = new JFileChooser(dir.getFile(""));
        dialog.addActionListener(a -> {
            File file = dialog.getSelectedFile();
            select(null);
            if (file != null) launchGame(null, file);
        });

        panel.add(dialog, SwingToolbox.getFillConstraints(RELATIVE, RELATIVE));
        return panel;
    }

    private JPanel getUpdatePanel(Directory dir) {
        JPanel panel = new JPanel(new GridBagLayout());

        // for the title
        GridBagConstraints titlePosition = SwingToolbox.getConstraints(0, 0, HORIZONTAL);
        JTextComponent title = SwingToolbox.flyingText();
        title.setFont(largeFont);
        title.setText("Only manual updating is enabled at the moment.\nSelect the newest GameRunnable.jar");
        panel.add(title, titlePosition);

        JFileChooser dialog = new JFileChooser(dir.getFile(""));
        dialog.addActionListener(a -> {
            try {
                File file = dialog.getSelectedFile();
                File original = jarName.toFile();
                if (file != null && file != original) {
                    Files.deleteIfExists(jarName);
                    Files.move(file.toPath(), jarName);
                }
                select(null);

            } catch (IOException e) {
                Logger.ERROR.print(e);
                select(debugOutputPanel);
            }
        });

        panel.add(dialog, SwingToolbox.getFillConstraints(0, RELATIVE));
        return panel;
    }

    private JPanel getConsolePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JTextPane console = new JTextPane();
        console.setEditable(false);
        console.setBackground(Color.BLACK);
        JScrollPane consoleView = new JScrollPane(console);
        JScrollBar scrollBar = consoleView.getVerticalScrollBar();

        MutableAttributeSet REGULAR = new SimpleAttributeSet(console.getInputAttributes());
        StyleConstants.setForeground(REGULAR, REGULAR_TEXT_COLOR);
        MutableAttributeSet ERROR = new SimpleAttributeSet(console.getInputAttributes());
        StyleConstants.setForeground(ERROR, ERROR_TEXT_COLOR);

        Logger.setOutputReceiver(
                str -> {
                    SwingToolbox.appendToText(console, str + "\n", REGULAR);
                    scrollBar.setValue(scrollBar.getMaximum());
                },
                str -> {
                    SwingToolbox.appendToText(console, str + "\n", ERROR);
                    scrollBar.setValue(scrollBar.getMaximum());
                }
        );
        Logger.ERROR.addOutputReceiver(System.err::println);

        panel.add(consoleView, SwingToolbox.getFillConstraints(0, 0));
        panel.setBorder(new BevelBorder(BevelBorder.LOWERED));

        return panel;
    }

    /** @return the InetAddress represented by text, or null if it is invalid */
    private static InetAddress getIP(String text) {
        try {
            return InetAddress.getByName(text);

        } catch (UnknownHostException e) {
            Logger.ERROR.print(e);
            return null;
        }
    }

    private void select(JComponent panelToShow) {
        if (panelToShow == null || panelToShow.isVisible()) panelToShow = defaultPanel;

        for (JComponent panel : mutexPanels) {
            panel.setVisible(panel == panelToShow);
        }

    }

    /** Starts the game in another thread and returns almost immediately */
    private void launchGame(InetAddress address, File replayFile) {
        if (!hideLauncherWhenRunning) {
            if (!debugOutputPanel.isVisible()) {
                select(debugOutputPanel);
            }
        } else {
            frame.setVisible(false);
        }

        new Thread(() -> {
            try {
                StringBuilder args = new StringBuilder();
                boolean doReplay = replayFile != null && replayFile.exists();

                File str = LauncherSettings.writeSettingsToFile("settings.json");
                args.append("-json \"").append(str.getPath()).append("\"");
                if (!names.loadedSuccessful()) args.append(" -rebuild");
                if (!doReplay && address == null) args.append(" -local");
                if (LauncherSettings.DEBUG) args.append(" -debug");
                if (doReplay) args.append(" -replay ").append(replayFile.getName());
                if (!doReplay && LauncherSettings.MAKE_REPLAY) args.append(" -store");
                if (!doReplay) args.append(" -map ").append(map);
                if (!doReplay) args.append(" -name ").append(getPlayerName());

                String command = "java -jar \"" + jarName + "\" " + args;
                Logger.DEBUG.print("Calling Command:\n" + command);

                Process proc = Runtime.getRuntime().exec(command);
                bindOutputToLogger(proc);

                int exitCode = proc.waitFor();
                Logger.INFO.print("Game finished with exit code " + exitCode + ".");

            } catch (Exception e) {
                StringBuilder stacktrace = new StringBuilder();
                stacktrace.append(e);
                if (LauncherSettings.DEBUG) {
                    for (StackTraceElement elt : e.getStackTrace()) {
                        stacktrace.append("\n\t").append(elt);
                    }
                }
                Logger.ERROR.print(stacktrace.toString());

            } finally {
                show();
            }
        }, "Main Game Thread").start();
    }

    private void close() {
        frame.dispose();
    }

    private void show() {
        frame.invalidate();
        frame.setVisible(true);
        Logger.INFO.print("Opened Launcher");
    }

    public static void main(String[] args) throws IOException, FontFormatException {
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontFileLucidaConsole);
        setLookAndFeel(font.deriveFont(LauncherMain.BASE_FONT_SIZE));

        LauncherMain launcher = new LauncherMain();
        launcher.init();
        launcher.show();
    }

    private static void setLookAndFeel(Font font) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            FontUIResource fontResource = new FontUIResource(font);

            UIDefaults uiDefaults = UIManager.getDefaults();
            for (Map.Entry<Object, Object> e : uiDefaults.entrySet()) {
                Object item = e.getKey();
                if (item instanceof String) {
                    String property = (String) item;
                    if (property.contains(".font")) {
                        uiDefaults.put(item, fontResource);
                    }
                }
            }

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Logger.ERROR.print("Error while loading Look&Feel : " + e);
            if (LauncherSettings.DEBUG) e.printStackTrace();
            Logger.WARN.print("Falling back on default L&F, this may look weird");
        } catch (UnsupportedLookAndFeelException e) {
            Logger.ERROR.print("Invalid Look&Feel : " + e.getMessage());
            if (LauncherSettings.DEBUG) e.printStackTrace();
            Logger.WARN.print("Falling back on default L&F, this may look weird");
        }
    }
}
