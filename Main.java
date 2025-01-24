import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    static LibC.TermiosClass originalAttributes;
    static int columns = 10;
    static int rows = 10;


    public static void main(String[] args) throws IOException {

        enableRawMode();

        initEditor();


        while (true) {
            refreshScreen();
            int key = getRead();
            handleKey(key);
        }
    }

    private static void initEditor() {
        LibC.WinSize winSize = getWindowSize();
        columns = winSize.ws_col;
        rows = winSize.ws_row;
    }

    private static void refreshScreen() {
        StringBuilder builder = new StringBuilder();

        builder.append("\033[2J");
        builder.append("\033[H");

        builder.append("~\r\n".repeat(Math.max(0, rows - 1)));

        String statusMessage = "Marco Code's Editor - v0.0.1";
        builder.append("\033[7m")
                .append(statusMessage)
                .append(" ".repeat(Math.max(0, columns - statusMessage.length())))
                .append("\033[0m");

        builder.append("\033[H");
        System.out.print(builder);

    }

    private static void handleKey(int key) {
        if (key == 'q') {
            LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);
            System.exit(0);
        }
        System.out.print((char) key + " (" + key + ")\r\n");
    }

    private static int getRead() throws IOException {
        return System.in.read();
    }

    private static void enableRawMode() {
        LibC.TermiosClass termios = new LibC.TermiosClass();
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);

        if (rc != 0) {
            System.err.println("There was a problem calling tcgetattr");
            System.exit(rc);
        }

        originalAttributes = LibC.TermiosClass.copyMethod(termios);

        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);

        termios.c_cc[LibC.VMIN] = 0;
        termios.c_cc[LibC.VTIME] = 1;

        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
    }

    private static LibC.WinSize getWindowSize() {
        LibC.WinSize winSize = new LibC.WinSize();
        int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winSize);

        if (rc != 0) {
            System.err.println("There was a problem calling ioctl");
            System.exit(rc);
        }

        return winSize;
    }
}

interface LibC extends Library
{
    int SYSTEM_OUT_FD = 0;
    int ISIG = 1, ICANON = 2, ECHO = 10, TCSAFLUSH = 2,
            IXON = 2000, ICRNL = 400, IEXTEN = 100000, OPOST = 1, VMIN = 6, VTIME = 5, TIOCGWINSZ = 0x5413;

    // we're loading the C standard library for POSIX systems
    LibC INSTANCE = Native.load("c", LibC.class);

    @Structure.FieldOrder(value={"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class WinSize extends Structure
    {
        public short ws_row, ws_col, ws_xpixel, ws_ypixel;
    }

    @Structure.FieldOrder(value={"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class TermiosClass extends Structure
    {
        public int c_iflag, c_oflag, c_cflag, c_lflag;
        public byte[] c_cc = new byte[19];

        public TermiosClass()
        {

        }

        public static TermiosClass copyMethod(TermiosClass other)
        {
            TermiosClass copy = new TermiosClass();
            copy.c_iflag = other.c_iflag;
            copy.c_oflag = other.c_oflag;
            copy.c_cflag = other.c_cflag;
            copy.c_lflag = other.c_lflag;
            copy.c_cc = other.c_cc.clone();
            return copy;
        }

        @Override
        public String toString() {
            return "Termios{" +
                    "c_iflag=" + c_iflag +
                    ", c_oflag=" + c_oflag +
                    ", c_cflag=" + c_cflag +
                    ", c_lflag=" + c_lflag +
                    ", c_cc=" + Arrays.toString(c_cc) +
                    '}';
        }

    }


    int tcgetattr(int fd, TermiosClass termios);
    int tcsetattr(int fd, int optional_actions,
                     TermiosClass termios);
    int ioctl(int fd, int op, WinSize winSize);

}
