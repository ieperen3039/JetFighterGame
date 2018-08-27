package nl.NG.Jetfightergame.Launcher;

import nl.NG.Jetfightergame.ServerNetwork.BlockingListener;
import nl.NG.Jetfightergame.Settings.KeyBinding;
import nl.NG.Jetfightergame.Settings.ServerSettings;
import nl.NG.Jetfightergame.Tools.KeyNameMapper;
import nl.NG.Jetfightergame.Tools.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.Consumer;

import static java.awt.GridBagConstraints.*;
import static nl.NG.Jetfightergame.Tools.KeyNameMapper.*;

/**
 * @author Geert van Ieperen. Created on 23-8-2018.
 */
public final class SwingToolbox {
    private static final String BUTTON_TRUE_TEXT = "Enabled";
    private static final String BUTTON_FALSE_TEXT = "Disabled";
    public static final int BUTTON_BORDER = 8;
    public static final int MOUSE_DETECTION_MINIMUM = 50;

    public static final Dimension MAIN_BUTTON_DIM = new Dimension(250, 40);
    public static final Dimension SMALL_BUTTON_DIM = new Dimension(150, 20);
    public static final Dimension TEXTBOX_DIM = new Dimension(300, MAIN_BUTTON_DIM.height);
    public static final Dimension KEYFETCH_DIALOG_DIM = new Dimension(MOUSE_DETECTION_MINIMUM * 2 + 100, MOUSE_DETECTION_MINIMUM * 2 + 100);

    public static boolean ALLOW_FLYING_TEXT = true;

    private static final Cursor BLANK_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(
            new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
            new Point(0, 0), "blank cursor");

    public static Insets borderOf(int pixels) {
        return new Insets(pixels, pixels, pixels, pixels);
    }

    public static JButton getButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(MAIN_BUTTON_DIM);
        button.setMinimumSize(SMALL_BUTTON_DIM);
        return button;
    }

    public static JPanel getFiller() {
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        if (ServerSettings.DEBUG) {
            filler.setBorder(new BevelBorder(BevelBorder.LOWERED));
        }
        return filler;
    }

    public static GridBagConstraints getConstraints(int x, int y, int fill) {
        return new GridBagConstraints(x, y, 1, 1, 0, 0, CENTER, fill, borderOf(0), 0, 0);
    }

    public static GridBagConstraints getButtonConstraints(int index) {
        return new GridBagConstraints(1, index, 1, 1, 1, 0, CENTER, HORIZONTAL, borderOf(BUTTON_BORDER), 0, 5);
    }

    public static GridBagConstraints getButtonConstraints(int x, int y) {
        return new GridBagConstraints(x, y, 1, 1, 1, 0, CENTER, HORIZONTAL, borderOf(BUTTON_BORDER), 0, 5);
    }

    public static GridBagConstraints getFillConstraints(int x, int y) {
        return new GridBagConstraints(x, y, 1, 1, 1, 1, CENTER, BOTH, borderOf(0), 0, 0);
    }

    public static GridBagConstraints getFillConstraints(int x, int y, int width, int height) {
        return new GridBagConstraints(x, y, width, height, 1, 1, CENTER, BOTH, borderOf(0), 0, 0);
    }

    public static JPanel invisiblePanel() {
        return new JPanel() {
            @Override
            public void paint(Graphics g) {
                paintChildren(g);
            }
        };
    }

    static JScrollPane scrollable(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel) {
            @Override
            public void paint(Graphics g) {
                paintChildren(g);
            }
        };
        scrollPane.getVerticalScrollBar().addAdjustmentListener(a ->
                scrollPane.getRootPane().repaint()
        );
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(a ->
                scrollPane.getRootPane().repaint()
        );
        return scrollPane;
    }

    public static JPanel getImagePanel(File imageFile) {
        try {
            final BufferedImage sourceImage = ImageIO.read(imageFile);

            return new JPanel() {
                int width = sourceImage.getWidth();
                int height = sourceImage.getHeight();

                @Override
                public void paint(Graphics g) {
                    float scalar1 = (float) getHeight() / height;
                    float scalar2 = (float) getWidth() / width;
                    float scalar = Math.max(scalar1, scalar2);
                    Image image = sourceImage.getScaledInstance((int) (this.width * scalar), (int) (height * scalar), Image.SCALE_FAST);
                    g.drawImage(image, 0, 0, null);

                    paintChildren(g);
                }
            };

        } catch (Exception e) {
            Logger.ERROR.print("Could not load image " + imageFile);
            return new JPanel();
        }
    }

    public static ActionListener getActionWorker(String name, Runnable runnable) {
        return e -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            thread.start();
        };
    }

    public static JTextComponent flyingText() {
        JTextComponent jText = new JTextArea();
        if (ALLOW_FLYING_TEXT) {
            jText.addMouseListener(new RepaintUponMouseEvent(jText));
            jText.setOpaque(false);
        }
        jText.setEditable(false);
        return jText;
    }

    static Runnable getSliderSetting(JPanel parent, int col, String text, Consumer<Float> effect, float min, float max, float current) {
        JPanel panel = getSettingPanel(text);

        JPanel combo = new JPanel(new BorderLayout());
        JSlider userInput = new JSlider();
        userInput.setMinimumSize(MAIN_BUTTON_DIM);
        float diff = max - min;
        combo.add(userInput);
        JTextComponent valueDisplay = flyingText();
        userInput.addChangeListener(e -> valueDisplay.setText(
                String.format("%5.1f", (userInput.getValue() / 100f) * diff + min)
        ));
        userInput.setValue((int) (100 * (current - min) / diff));
        combo.add(valueDisplay, BorderLayout.EAST);
        panel.add(combo);

        parent.add(panel, getButtonConstraints(col, RELATIVE));

        return () -> effect.accept(((userInput.getValue() / 100f) * diff) + min);
    }

    static Runnable getTextboxSetting(JPanel parent, int col, String text, String current, Consumer<String> effect) {
        JPanel panel = getSettingPanel(text);

        JTextField userInput = new JTextField();
        userInput.setMinimumSize(TEXTBOX_DIM);
        userInput.setText(current);
        panel.add(userInput);

        parent.add(panel, getButtonConstraints(col, RELATIVE));

        return () -> effect.accept(userInput.getText());
    }

    public static Runnable getBooleanSetting(JPanel parent, int col, String text, boolean current, Consumer<Boolean> effect) {
        JPanel panel = getSettingPanel(text);

        JToggleButton button = new JToggleButton(current ? BUTTON_TRUE_TEXT : BUTTON_FALSE_TEXT);
        button.addActionListener(e -> button.setText(button.isSelected() ? BUTTON_TRUE_TEXT : BUTTON_FALSE_TEXT));
        button.setMinimumSize(SMALL_BUTTON_DIM);
        button.setSelected(current);
        panel.add(button);

        parent.add(panel, getButtonConstraints(col, RELATIVE));

        return () -> effect.accept(button.isSelected());
    }

    static <Type> Runnable getChoiceSetting(JPanel parent, int col, String text, Type[] values, Type current, Consumer<Type> effect) {
        JPanel panel = getSettingPanel(text);

        JComboBox<Type> userInput = new JComboBox<>(values);

        userInput.setSelectedItem(current);
        panel.add(userInput);

        parent.add(panel, getButtonConstraints(col, RELATIVE));

        return () -> effect.accept(userInput.getItemAt(userInput.getSelectedIndex()));
    }

    private static JPanel getSettingPanel(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        JTextComponent settingName = flyingText();
        settingName.setText(text);
        panel.add(settingName, BorderLayout.NORTH);
        return panel;
    }

    public static void appendToText(JTextPane console, String text, AttributeSet attributes) {
        try {
            int pos = console.getDocument().getLength();
            console.getStyledDocument().insertString(pos, text, attributes);
        } catch (BadLocationException ex) {
            console.setText(ex.toString());
        }
    }

    public static JPanel getKeyPanel(KeyBinding key, Frame parentFrame) {
        JPanel panel = getSettingPanel(key.name().replace("_", " ").toLowerCase());

        JButton userInput = getButton(key.name());
        userInput.setHorizontalAlignment(JTextField.CENTER);
        userInput.setText(key.keyName());
        userInput.addActionListener(a -> getGetKeyDialog(key, parentFrame, userInput));
        panel.add(userInput);

        return panel;
    }

    private static void getGetKeyDialog(KeyBinding key, Frame parentFrame, JButton userInput) {
        JDialog keyFetcherFrame = new JDialog(parentFrame, false);
        JTextArea keyFetcher = new JTextArea("Press any key");
        keyFetcher.setEditable(false);
        int xMid = parentFrame.getX() + (parentFrame.getWidth() / 2);
        int yMid = parentFrame.getY() + (parentFrame.getHeight() / 2);
        int x = xMid - (KEYFETCH_DIALOG_DIM.width / 2);
        int y = yMid - (KEYFETCH_DIALOG_DIM.height / 2);
        keyFetcherFrame.setLocation(x, y);
        keyFetcher.setPreferredSize(KEYFETCH_DIALOG_DIM);
        keyFetcher.setCursor(BLANK_CURSOR);
        keyFetcherFrame.add(keyFetcher);

        Runnable disposeSeq = () -> {
            parentFrame.setCursor(null);
            userInput.setText(key.keyName());
            keyFetcherFrame.dispose();
            Point p = userInput.getLocationOnScreen();
            moveMouseTo(
                    p.x + userInput.getWidth() / 2,
                    p.y + userInput.getHeight() / 2
            );
        };

        // move mouse and set/validate sizes before installing listeners
        moveMouseTo(xMid, yMid);
        keyFetcherFrame.pack();
        keyFetcherFrame.setVisible(true);
        keyFetcher.setFocusable(true);
        keyFetcher.requestFocusInWindow();

        // keyboard key
        keyFetcher.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int glfwKey = KeyNameMapper.swingKeyToGlfw(e.getKeyCode());
                key.installNew(glfwKey, false, false);
                disposeSeq.run();
            }
        });

        // mouse button
        keyFetcher.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                key.installNew(e.getButton(), false, false);
                disposeSeq.run();
            }
        });

        // mouse movement
        keyFetcher.addMouseMotionListener(new MouseMotionAdapter() {
            private final Point mousePos = keyFetcher.getMousePosition();
            private final double xMax = mousePos.getX() + MOUSE_DETECTION_MINIMUM;
            private final double xMin = mousePos.getX() - MOUSE_DETECTION_MINIMUM;
            private final double yMax = mousePos.getY() + MOUSE_DETECTION_MINIMUM;
            private final double yMin = mousePos.getY() - MOUSE_DETECTION_MINIMUM;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (e.getX() > xMax) {
                    choose(MOUSE_RIGHT);
                } else if (e.getX() < xMin) {
                    choose(MOUSE_LEFT);
                } else if (e.getY() > yMax) { // screen coordinates
                    choose(MOUSE_DOWN);
                } else if (e.getY() < yMin) {
                    choose(MOUSE_UP);
                }
            }

            private void choose(int value) {
                key.installNew(value, true, false);
                disposeSeq.run();
            }
        });
    }

    /** try moving the mouse to the screen coordinates, but this may fail (silently) */
    private static void moveMouseTo(int x, int y) {
        try {
            new Robot().mouseMove(x, y);
        } catch (AWTException ignored) {
        }
    }

    static void bindOutputToLogger(Process proc) throws IOException {
        InputStream in = proc.getInputStream();
        InputStream err = proc.getErrorStream();
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(in));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(err));

        //noinspection Convert2Lambda
        new BlockingListener() {
            @Override
            public boolean handleMessage() throws IOException {
                String line = stdIn.readLine();
                if (line == null) return false;
                Logger.printRaw(line);
                return true;
            }
        }.listenInThread(false);

        //noinspection Convert2Lambda
        new BlockingListener() {
            @Override
            public boolean handleMessage() throws IOException {
                String line = stdErr.readLine();
                if (line == null) return false;
                Logger.errorRaw(line);
                return true;
            }
        }.listenInThread(false);
    }

    private static class RepaintUponMouseEvent implements MouseListener {
        private final JComponent component;

        public RepaintUponMouseEvent(JTextComponent component) {
            this.component = component;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            SwingUtilities.invokeLater(this::repaint);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
            repaint();
        }

        private void repaint() {
            component.getRootPane().repaint(
                    component.getX(), component.getY(), component.getWidth(), component.getHeight()
            );
        }
    }
}
