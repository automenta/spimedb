//package spimedb.server;
//
//import com.googlecode.lanterna.TerminalPosition;
//import com.googlecode.lanterna.TerminalSize;
//import com.googlecode.lanterna.TextCharacter;
//import com.googlecode.lanterna.gui2.*;
//import com.googlecode.lanterna.screen.TerminalScreen;
//import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import spimedb.SpimeDB;
//
//import java.io.IOException;
//import java.io.Serializable;
//
///**
// * Created by me on 4/30/17.
// */
//public class ConsoleSession extends Session {
//
//    static final Logger logger = LoggerFactory.getLogger(ConsoleSession.class);
//
//    public final TerminalUI term;
//    private final int[] cursorPos = new int[2];
//
//    public ConsoleSession(SpimeDB db) {
//        super(db);
//
//        int cols = 40;
//        int rows = 25;
//        this.term =
//                //new DefaultVirtualTerminal(new TerminalSize(cols, rows));
//                new TerminalUI(cols, rows);
//
//        set("me", new API());
//
//    }
//
//    public static class ScreenShot implements Serializable {
//
//        public final String id;
//        public final int width;
//        public final byte[] data; //pairs of: (ASCII char,color)
//        public final int cursorX, cursorY;
//
//        ScreenShot(String id, int width, byte[] data, int[] cursorPos) {
//            this.id = id;
//            this.width = width;
//            this.data = data;
//            cursorX = cursorPos[0];
//            cursorY = cursorPos[1];
//        }
//    }
//
//    public class API {
//
//        public ScreenShot get() {
//            return term.screenshot();
//        }
//    }
//
//    class TerminalUI extends DefaultVirtualTerminal implements Runnable {
//
//        public MultiWindowTextGUI gui;
//
//        public TextBox textBox;
//
//        public TerminalUI(int c, int r) {
//            super(new TerminalSize(c, r));
//
//            new Thread(this).start();
//        }
//
//        public ScreenShot screenshot() {
//            int C = getTerminalSize().getColumns();
//            int R = getTerminalSize().getRows();
//            byte data[] = new byte[ R * C * 2 ];
//            int k = 0;
//            for (int j = 0; j < R; j++) {
//                for (int i = 0; i < C; i++) {
//                    TextCharacter ch = getCharacter(i, j);
//                    data[k++] = (byte) ch.getCharacter();
//                    data[k++] = 0;
//                }
//            }
//
//            return new ScreenShot("screen1", C, data, getCursorPos());
//
//        }
//
//        public void commit() {
//            try {
//                gui.updateScreen();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void run() {
//
//            logger.info("starting: {}", this);
//
//            try {
//                TerminalScreen screen = new TerminalScreen(this);
//                screen.startScreen();
//                gui = new MultiWindowTextGUI(
//                        new SameTextGUIThread.Factory(),
//                        screen);
//
//
//                setCursorVisible(true);
//
//                gui.setBlockingIO(false);
//                gui.setEOFWhenNoWindows(false);
//
//                TerminalSize size = getTerminalSize();
//
//                final BasicWindow window = new BasicWindow();
//                window.setPosition(new TerminalPosition(0, 0));
//                window.setSize(new TerminalSize(size.getColumns() - 2, size.getRows() - 2));
//
//
//                Panel panel = new Panel();
//                panel.setPreferredSize(new TerminalSize(32, 8));
//
//
//                textBox = new TextBox("", TextBox.Style.MULTI_LINE);
//                textBox.takeFocus();
//
//                panel.addComponent(textBox);
//                panel.addComponent(new Button("Button", () -> {
//                }));
//                panel.addComponent(new Button("XYZ", () -> {
//                }));
//
//
//                window.setComponent(panel);
//
//
//                gui.addWindow(window);
//                gui.setActiveWindow(window);
//
//                commit();
//
//                gui.waitForWindowToClose(window);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//
////    public Appendable append(CharSequence c) {
////        int l = c.length();
////        for (int i = 0; i < l; i++) {
////            append(c.charAt(i));
////        }
////        return this;
////    }
////
////
////    public Appendable append(char c) {
////        term.putCharacter(c);
////        return this;
////    }
////
////    @Override
////    public Appendable append(CharSequence charSequence, int i, int i1) {
////        throw new UnsupportedOperationException("TODO");
////    }
////    public OutputStream output() {
////        return new OutputStream() {
////
////            @Override
////            public void write(int i) {
////                append((char) i);
////            }
////
////            @Override
////            public void flush() {
////                term.flush();
////            }
////        };
////    }
//
//    public int[] getCursorPos() {
//        TerminalPosition p = term.getCursorPosition();
//        cursorPos[0] = p.getColumn();
//        cursorPos[1] = p.getRow();
//        return cursorPos;
//    }
//
//    public int cursorX() {
//        return term.getCursorPosition().getColumn();
//    }
//
//    public int cursorY() {
//        return term.getCursorPosition().getRow();
//    }
//
//    public TextCharacter charAt(int col, int row) {
//        return term.getCharacter(col, row);
//    }
//
//
////    public boolean onKey(KeyEvent e, boolean pressed) {
////
////        //return super.onKey(e, pressed);
////        DefaultVirtualTerminal eterm = this.term;
////
////        int cc = e.getKeyCode();
////        if (pressed && cc == 13) {
////            term.addInput(new KeyStroke(KeyType.Enter, e.isControlDown(), e.isAltDown()));
////        } else if (pressed && cc == 8) {
////            term.addInput(new KeyStroke(KeyType.Backspace, e.isControlDown(), e.isAltDown()));
////        } else if (pressed && cc == 27) {
////            term.addInput(new KeyStroke(KeyType.Escape, e.isControlDown(), e.isAltDown()));
////        } else if (e.isPrintableKey() && !e.isActionKey() && !e.isModifierKey()) {
////            char c = e.getKeyChar();
////            if (!TerminalTextUtils.isControlCharacter(c) && !pressed /* release */) {
////                //eterm.gui.getActiveWindow().handleInput(
////                term.addInput(
////                        //eterm.gui.handleInput(
////                        new KeyStroke(c, e.isControlDown(), e.isAltDown())
////                );
////
////            } else {
////                return false;
////            }
////        } else if (pressed) {
////            KeyType c = null;
////            //System.out.println(" keycode: " + e.getKeyCode());
////            switch (e.getKeyCode()) {
////                case KeyEvent.VK_BACK_SPACE:
////                    c = KeyType.Backspace;
////                    break;
////                case KeyEvent.VK_ENTER:
////                    c = KeyType.Enter;
////                    break;
////                case KeyEvent.VK_DELETE:
////                    c = KeyType.Delete;
////                    break;
////                case KeyEvent.VK_LEFT:
////                    c = KeyType.ArrowLeft;
////                    break;
////                case KeyEvent.VK_RIGHT:
////                    c = KeyType.ArrowRight;
////                    break;
////                case KeyEvent.VK_UP:
////                    c = KeyType.ArrowUp;
////                    break;
////                case KeyEvent.VK_DOWN:
////                    c = KeyType.ArrowDown;
////                    break;
////
////                default:
////                    System.err.println("character not handled: " + e);
////                    return false;
////            }
////
////
////            //eterm.gui.handleInput(
////
////            //eterm.gui.getActiveWindow().handleInput(
////            term.addInput(
////                    new KeyStroke(c, e.isControlDown(), e.isAltDown(), e.isShiftDown())
////            );
////            //                    KeyEvent.isModifierKey(KeyEvent.VK_CONTROL),
//////                    KeyEvent.isModifierKey(KeyEvent.VK_ALT),
//////                    KeyEvent.isModifierKey(KeyEvent.VK_SHIFT)
//////            ));
////        } else {
////            //...
////        }
////
////        //AtomicBoolean busy = new AtomicBoolean(false);
////        //if (busy.compareAndSet(false,true)) {
////
////        //this.term.flush();
////
////        if (eterm instanceof TerminalUI) {
////            TerminalUI ee = (TerminalUI) eterm;
////            ee.gui.getGUIThread().invokeLater(() -> {
////                try {
////                    ee.gui.processInput();
////                    ee.gui.updateScreen();
////                } catch (IOException e1) {
////                    e1.printStackTrace();
////                }
////            });
////        }
////        return true;
////    }
//
//}
