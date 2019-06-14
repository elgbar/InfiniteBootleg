package no.elg.infiniteBootleg;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import org.junit.BeforeClass;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TestGraphic {

    private static ConsoleLogger logger = new ConsoleTestLogger();

    @BeforeClass
    public static void init() {
        Main.renderGraphic = false;
        Gdx.gl20 = mock(GL20.class);
        Gdx.gl = Gdx.gl20;

        //run postRunnable at once
        Gdx.app = mock(Application.class);
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, Runnable.class).run();
            return null;
        }).when(Gdx.app).postRunnable(any(Runnable.class));


        Gdx.graphics = mock(Graphics.class);
        when(Gdx.graphics.getWidth()).thenReturn(1);
        when(Gdx.graphics.getHeight()).thenReturn(1);
        Main.inst = mock(Main.class);
        when(Main.inst.getConsoleLogger()).thenReturn(logger);
    }

    private static class ConsoleTestLogger implements ConsoleLogger {

        @Override
        public void logf(String msg, Object... objs) {
            logf(LogLevel.DEFAULT, msg, objs);
        }

        @Override
        public void logf(LogLevel level, String msg, Object... objs) {
            if (level == LogLevel.ERROR) {
                System.err.printf(msg + "%n", objs);
            }
            else {
                System.out.printf(msg + "%n", objs);
            }
        }

        @Override
        public void log(LogLevel level, String msg) {
            if (level == LogLevel.ERROR) {
                System.err.println(msg);
            }
            else {
                System.out.println(msg);
            }
        }

        @Override
        public void log(String msg) {
            log(LogLevel.DEFAULT, msg);
        }
    }
}
