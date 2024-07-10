package ClientServerWebcam;

import java.awt.image.BufferedImage;
import java.io.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.Graphics2D;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerWebcam {

    private static InetAddress ip;
    private static int port; // 6699
    private static int AUDIO_PORT;  //5001
    private static TargetDataLine line = null;
    private static final int target_DataRate = 5000;
    private static final int frameWidth = 320;
    private static final int frameHeight = 240;
    private static final int frameRate = 30;

    /*private static final IAudioSamples.Format soundFormat = IAudioSamples.Format.FMT_S16;
    private static final int BYTE_DEPTH = soundFormat.swigValue() + 1;
    private static final int BIT_DEPTH = 8 * BYTE_DEPTH;
    public static final int CHANNEL_COUNT = 1;
    public static final int SAMPLE_RATE = 44100;*/
    public static void main(String[] args) throws IOException {

        socketConnections();
    }

    public static void socketConnections() throws IOException {
        Webcam cam = null;
        DatagramSocket dataSocket = null;

        try {
            dataSocket = new DatagramSocket();
            ip = InetAddress.getByName("127.0.0.1");//getIpAddressFromUser();//InetAddress.getByName("localhost");//192.9.200.131,localhost
            port = 3306;//getPortFromUser();
            AUDIO_PORT = 3307;//getAudioPortFromUser();
            sendVideoAudioUDP(dataSocket);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e) {

            System.out.println(e.getMessage());
        }
//      } finally {
//            if (cam != null) {
//                cam.close();
//                line.stop();
//                line.close();
//                
//            }
//            Webcam.shutdown();
//            if (dataSocket != null) {
//                dataSocket.close();
//                
//            }
//            fileOutputStream.close();//testing
//            System.out.println("finished");
//        }
    }

    public static void sendVideoAudioUDP(DatagramSocket dataSocket) throws IOException, LineUnavailableException {
        final Webcam cam = Webcam.getDefault();
        cam.setViewSize(WebcamResolution.VGA.getSize());
        cam.open();
        WebcamPanel webcamPanel = new WebcamPanel(cam);//step3
//        webcamPanel.setImageSizeDisplayed(true);
//        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setMirrored(true);

        JFrame frame1 = new JFrame();//Step2 jframeoperations
        frame1.setTitle("ServerWebcam");
        frame1.add(webcamPanel);
        frame1.setLocationRelativeTo(null);
        frame1.pack();
        frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame1.setVisible(true);

        System.out.println("sending webcam images to client");

        AudioFormat audioFormat = new AudioFormat(8000, 16, 1, true, false);
        // SAMPLE_RATE, BIT_DEPTH, CHANNEL_COUNT, true, false
        /*DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);//Testing
        line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);//Testing*/
        line = AudioSystem.getTargetDataLine(audioFormat);
        line.open(audioFormat);
        line.start();

        //Create and Start the audio thread
        Thread audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] audioBuffer = new byte[1024];
                    while (true) {
                        int bytesRead = line.read(audioBuffer, 0, audioBuffer.length);
                        DatagramPacket audioPacket = new DatagramPacket(audioBuffer, bytesRead, ip, AUDIO_PORT);
                        dataSocket.send(audioPacket);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        audioThread.start();

        // Create and start the video thread
        Thread videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long startTime = System.currentTimeMillis();
                    long totalBytesSent = 0;
                    int frameCount = 0;
                    int desiredFPS = 5;//10

                    while (true) {
                        BufferedImage frame = cam.getImage();// Get image from webcam
                        //System.out.println("frame:"+frame.getWidth()+"x"+frame.getHeight());
                        // Resize the image
//                        BufferedImage resizedImage = new BufferedImage(frame.getWidth(), frame.getHeight(), frame.TYPE_INT_RGB);
//                        Graphics2D g = resizedImage.createGraphics();
//                        g.drawImage(frame, 0, 0, frameWidth, frameHeight, null);
//                        g.dispose();
                        //Compress the image and get the compressed Image as byteArray
                        byte[] compressedBytes = compressImageToH264(frame);

                        // Creates a UDP packet and sends it to the recipient
                        DatagramPacket videoPacket = new DatagramPacket(compressedBytes, compressedBytes.length, ip, port);
                        dataSocket.send(videoPacket);
                        //Update the totalBytes sent
                        totalBytesSent += compressedBytes.length;
                        frameCount++;
                        //Caluculating the elapsed time
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        //Check if one second is passed
                        if (elapsedTime >= 1000 || frameCount >= desiredFPS) {
                            //Caluculate the bytes per second
                            double bytesPerSecond = totalBytesSent / (elapsedTime / 1000);
                            System.out.println("Bytes transferred per second: " + bytesPerSecond);
                            //Reset the counters
                            startTime = System.currentTimeMillis();
                            totalBytesSent = 0;
                            frameCount = 0;

                        }
                        // Delay to achieve the desired FPS
//                        long sleepTime = 1000 / desiredFPS;
//                        Thread.sleep(sleepTime);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        videoThread.start();

        /* while (true) {
            BufferedImage frame = cam.getImage(); // Get image from webcam

            // Converts the image to JPEG and passes it to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(frame, "jpeg", baos);
            byte[] bytes = baos.toByteArray();


           // Creates a UDP packet and sends it to the recipient
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip, port);
            dataSocket.send(packet);

            byte[] audioData = new byte[1024];
            int bytesRead = line.read(audioData, 0, audioData.length);

            DatagramPacket audioPacket = new DatagramPacket(audioData, bytesRead, ip, AUDIO_PORT);
            dataSocket.send(audioPacket);
            
        }*/
    }

    private static byte[] compressImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //This line retrieves an ImageWriter object for the JPEG format 
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        //Returns an ImageOutputStream will send its output to given object
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        //Returns new ImageWriteParam object of the appropriate type for this file format containing
        //defualt values.
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.11f);

        writer.write(null, new IIOImage(image, null, null), param);
        ios.flush();
        writer.dispose();
        return baos.toByteArray();
    }

    private static BufferedImage compressImageJPEG(BufferedImage image) throws IOException {
        //Create a new BufferedImage with JPEG compression
        BufferedImage compressedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        compressedImage.createGraphics().drawImage(image, 0, 0, null);
        //Write the compressed Image to ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(compressedImage, "jpeg", baos);
        // Read the compressed image back as a BufferedImage
        return ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
    }

    public static byte[] compressImage1(BufferedImage image) throws IOException {
        float quality = 1.0f;
        byte[] compressedBytes;
        do {
            compressedBytes = compressWithquality(image, quality);
            int dataSizeKB = compressedBytes.length / 1024;
            if (dataSizeKB <= target_DataRate) {
                break;
            } else {
                quality -= 0.05f;//Reduce the Quality and Try again
            }

        } while (quality >= 0.1f);
        return compressedBytes;
    }

    private static byte[] compressWithquality(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //This line retrieves an ImageWriter object for the JPEG format 
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        //Returns an ImageOutputStream will send its output to given object
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        //Returns new ImageWriteParam object of the appropriate type for this file format containing
        //defualt values.
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.write(null, new IIOImage(image, null, null), param);
        ios.flush();
        writer.dispose();
        return baos.toByteArray();
    }

    public static InetAddress getIpAddressFromUser() throws UnknownHostException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP Address: ");
        String ipAddress = scanner.nextLine();
        return InetAddress.getByName(ipAddress);

    }

    public static int getPortFromUser() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Port Number: ");
        int portNumber = scanner.nextInt();
        return portNumber;
    }

    public static int getAudioPortFromUser() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter AudioPort Number: ");
        int audioPortNumber = scanner.nextInt();
        return audioPortNumber;
    }

    private static byte[] compressImageToH264(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Assuming you have FFmpeg in your system's PATH
        String ffmpegCommand = "ffmpeg -f image2pipe -vcodec png -i - -c:v libx264 -preset medium -tune zerolatency -b:v 32k -minrate 32k -maxrate 32k -bufsize 32k -vf scale=370:240 -r 10 -f h264 -";
//        String ffmpegCommand = "ffmpeg -f image2pipe -vcodec png -i - -c:v libx264 -preset medium -tune film -b:v 32k -bufsize 32k -maxrate 32k -minrate 32k -vf scale=320:240 -r 10 -crf 30 -f h264 -";
        /*String[] ffmpegCommand = {
            "ffmpeg",
            "-f", "image2pipe",
            "-vcodec", "png",
            "-i", "-",
            "-c:v", "libx264",
            "-preset", "medium",
            "-tune", "film",
            "-b:v", "32k",
            "-bufsize", "32k",
            "-maxrate", "32k",
            "-minrate", "32k",
            "-vf", "scale=370:240",
            "-r", "10",
            "-pix_fmt", "bgr24",
            "-crf", "30",
            "-f", "h264",
            "-"
        };*/
 /*String[] ffmpegCommand = {
            "ffmpeg",
            "-f", "image2pipe",
            "-vcodec", "png",
            "-i", "-",
            "-c:v", "libx265", // Change to libx265 for H.265
            "-preset", "medium",
            "-tune", "film",
            "-b:v", "32k",
            "-bufsize", "32k",
            "-maxrate", "32k",
            "-minrate", "32k",
            "-vf", "scale=370:240",
            "-r", "10",
            "-pix_fmt", "yuv420p", // Change to yuv420p for H.265
            "-crf", "30",
            "-f", "h265", // Change to h265 for H.265
            "-"
        };*/

        Process process = Runtime.getRuntime().exec(ffmpegCommand);

        try (OutputStream os = process.getOutputStream()) {
            ImageIO.write(image, "png", os);
        }

        InputStream is = process.getInputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        process.destroy();

        return baos.toByteArray();
    }
// for testing

    private static void saveImagesForDebugging(BufferedImage originalImage, byte[] compressedData) throws IOException {
        // Save the original image
        File originalImageFile = new File("original_image.png");
        ImageIO.write(originalImage, "png", originalImageFile);

        // Save the compressed image
        File compressedImageFile = new File("compressed_image.h264");
        try (FileOutputStream fos = new FileOutputStream(compressedImageFile)) {
            fos.write(compressedData);
        }
    }

    private static void saveVideoForDebugging(byte[] videoData) throws IOException {
        // Save the compressed video to a file
        try (FileOutputStream fos = new FileOutputStream("debug_video.h264")) {
            fos.write(videoData);
        }
    }

}
