package com.lewisk.javafx_learn;

import com.lewisk.emulation.Assemble6502;
import com.lewisk.emulation.CPU6502;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application
{
    public static CPU6502 cpu;
    public static PixelWriter display;
    public Thread cpuThread;

    public static void main(String[] args)
    {
        cpu = new CPU6502();

        /*
        cpu.importRAM(
                (short) 0x0600,
                0xa0, 0x00, 0xa2, 0x00, 0xa9, 0x00, 0x85, 0x10, 0xa9, 0x02, 0x85, 0x11, 0x8a, 0x91, 0x10, 0xe8,
                0xc8 , 0xd0, 0xf9, 0xa0, 0x00, 0xe6, 0x11, 0xa5, 0x11, 0xc9, 0x06, 0x90, 0xef
        );
        */

       // cpu.importRAM("snake.6502");
        String[] data = Assemble6502.assemble("test.asm");
        cpu.importRAM(data);

        launch(args);
    }

    @Override
    public void start(Stage win) throws Exception {
        win.setTitle("JavaFX Window (Stage)");

        WritableImage vramCanvas = new WritableImage(32, 32);
        PixelWriter vramEdit = vramCanvas.getPixelWriter();
        ImageView vramView = new ImageView(vramCanvas);
        Canvas display = new Canvas(640, 640);

        var graphics = display.getGraphicsContext2D();
        graphics.setImageSmoothing(false);
        graphics.setFill(Color.BLACK);
        graphics.fillRect(0, 0, 640, 640);
        graphics.drawImage(vramView.getImage(), 0, 0, 640, 640);

        Group layout = new Group(display);
        Scene mainScene = new Scene(layout, 640, 640, Color.GRAY);

        win.setScene(mainScene);
        win.show();

        // Process input
        // WASD = 57,41,53,44
        mainScene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            byte code = (byte) key.getCode().getCode(); // Nice syntax JavaFX...
            cpu.writeRAM((short) 0x00FF, code);
        });

        cpu.halt = false;
        cpu.display = vramEdit;

        // Setup processor threads.
        cpuThread = new Thread(cpu);
        cpuThread.start();

        // Update display
        new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                graphics.drawImage(vramView.getImage(), 0, 0, 640, 640);
            }
        }.start();
    }

    @Override
    public void stop() throws Exception
    {
        // Stop CPU processing.
        cpuThread.interrupt();
    }
}
