/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author praveen
 */
/*public class PlayVideoFfplay {

    public static void main(String[] args) {
        try {
            // Replace "your-video-file.mp4" with the actual path to your video file
            String videoFilePath = "/home/praveen/NetBeansProjects/LiveStream/received_video.h265";

            // Construct the ffplay command
            String ffplayCommand = "ffplay -autoexit " + videoFilePath;

            // Execute the ffplay command
            Process process = Runtime.getRuntime().exec(ffplayCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to complete (when the video player is closed)
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class PlayVideoFfplay {

    public static void main(String[] args) {
        try {
            // Specify the ffplay command
            String senderIPAddress = getIpAddressFromUser(); // Replace with the actual sender's IP address
            String ffplayCommand = "ffplay -flags low_delay -vf setpts=0 udp://" + senderIPAddress + ":3306";

//            String ffplayCommand = "ffplay -flags low_delay -vf setpts=0 udp://localhost:3306";
            // Start the ffplay process
            Process ffplayProcess = Runtime.getRuntime().exec(ffplayCommand);

            // Redirect the standard error stream to see any error messages
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(ffplayProcess.getErrorStream()));
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }

            // You can also wait for the process to finish (optional)
            int exitCode = ffplayProcess.waitFor();
            System.out.println("ffplay exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getIpAddressFromUser() throws UnknownHostException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP Address: ");
        String ipAddress = scanner.nextLine();
        return ipAddress;

    }
}
/*import java.awt.Canvas;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class PlayVideoFfplay {

    public static void main(String[] args) throws IOException, UnknownHostException, InterruptedException {
        // Create a new JFrame object.
        Frame frame = new Frame("Display Video from ffplay");
        frame.setSize(320, 240);

        // Create a new Canvas object and add it to the JFrame.
        Canvas canvas = new Canvas();
        frame.add(canvas);

        // Create a new ffplay process and redirect its output to the Canvas.
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
        Process ffplayProcess = Runtime.getRuntime().exec("ffplay -flags low_delay -vf setpts=0 udp://" + getIpAddressFromUser() + ":3306");
        ffplayProcess.getErrorStream().close();
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = ffplayProcess.getInputStream().read(buffer)) != -1) {
                    pipedOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Display the video.
        canvas.setIgnoreRepaint(true);
        new Thread(() -> {
            try {
                while (true) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = pipedInputStream.read(buffer)) != -1) {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
                        BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
                        canvas.getGraphics().drawImage(bufferedImage, 0, 0, null);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Set the visible property of the JFrame to true.
        frame.setVisible(true);

        // Wait for the ffplay process to finish.
        ffplayProcess.waitFor();
    }

    private static String getIpAddressFromUser() throws UnknownHostException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP Address: ");
        String ipAddress = scanner.nextLine();
        return ipAddress;
    }
}*/
