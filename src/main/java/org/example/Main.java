package org.example;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    final static String HOST_ME_UTM_MD = "me.utm.md";
    final static int PORT_ME_UTM_MD = 80;
    final static String HOST_UTM_MD = "utm.md";
    final static int PORT_UTM_MD = 443;
    public static final int NUM_OF_BITES = 1024;

    public static void main(String[] args) throws Exception {


        System.out.println("""
                Choose an option:\s
                Press 1 for: me.utm.md\s
                Press 2 for: utm.md\s
                You choice:\s""");

        Scanner scanner = new Scanner(System.in);
        int number = scanner.nextInt();
        if (number == 1)
            requestMeUTM_md();
        else if (number == 2)
            requestUTM_md();
    }

    public static void requestMeUTM_md() throws InterruptedException, IOException {
        String Response = getResponse(HOST_ME_UTM_MD, PORT_ME_UTM_MD, "/");
        List<String> listOfImg = getImages(Response);
        listOfImg.remove(listOfImg.size() - 1);
        System.out.println("List of images from me.utm.md:\n" + listOfImg +"\n");

        Semaphore semaphore = new Semaphore(2);
        ExecutorService exec = Executors.newFixedThreadPool(4);
//        ExecutorService exec = Executors.newCachedThreadPool();
        boolean status = true;
        while (status) {
            for (String image : listOfImg) {
                semaphore.acquire();
                exec.submit(() -> {
                    try {
                        getImg(getNameImages(image, "http://mib.utm.md/"));
                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                if (image.equals(listOfImg.get(listOfImg.size() - 1))) {
                    status = false;
                    break;
                }
            }
        }

        exec.shutdown();
    }
    public static void requestUTM_md() throws InterruptedException {
        String ResponseSecured = getResponseSecured(HOST_UTM_MD, PORT_UTM_MD, "/");
        List<String> listImageSecured= getImages(ResponseSecured);
        listImageSecured.remove(0);
        System.out.println("List of images from utm.md:" + listImageSecured);

//        Semaphore semaphore = new Semaphore(2);
//        ExecutorService exec = Executors.newFixedThreadPool(4);
        ExecutorService exec = Executors.newCachedThreadPool();
        boolean status = true;
        while (status) {
            for (String element : listImageSecured) {
//                semaphore.acquire();
                exec.submit(() -> {
                    try {
                        getImagesSecured(getNameImages(element, "https://utm.md"));
//                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                if (element.equals(listImageSecured.get(listImageSecured.size() - 1))) {
                    status = false;
                    break;
                }
            }
        }
        exec.shutdown();
    }

    public static List<String> getImages(String text) {
        String img;
        String regex = "<img.*src\\s*=\\s*(.*?)(jpg|png|gif)[^>]*?>";
        List<String> images = new ArrayList<>();

        Pattern pImage = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher mImage = pImage.matcher(text);

        while (mImage.find()) {
            img = mImage.group();
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
            while (m.find()) {
                images .add(m.group(1));
            }
        }
        return images ;
    }

    public static String getResponse(String hostName, int port, String getArgument) throws IOException {
        StringBuilder serverResponse = new StringBuilder();
        int c;

        Socket socket = new Socket(hostName, port);

        InputStream response = socket.getInputStream();
        OutputStream request = socket.getOutputStream();

        String dataRequest = "GET " + getArgument + " HTTP/1.1\r\n" +
                "Host: " + hostName + "\r\n" +
                "Content-Type: text/html;charset=utf-8 \r\n" +
                "User-Agent: Mozilla/5.0 (X11; Linux i686; rv:2.0.1) Gecko/20100101 Firefox/4.0.1 \r\n" +
                "Accept-Language: ro \r\n" +
                "Content-Language: en, ase, ru \r\n" +
                "Vary: Accept-Encoding \r\n" +
                "\r\n";

        byte[] data = (dataRequest).getBytes();
        request.write(data);

        while ((c = response.read()) != -1) {
            assert false;
            serverResponse.append((char) c);
        }
        socket.close();

        return serverResponse.toString();
    }

    public static String getResponseSecured(String hostName, int port, String getArgument) {
        StringBuilder serverResponse = new StringBuilder();
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(hostName, port);
            socket.startHandshake();

            PrintWriter outputWriter = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream())));

            String dataRequest = "GET " + getArgument + " HTTP/1.1\r\n" +
                    "Host: " + hostName + "\r\n" +
                    "Content-Type: text/html;charset=utf-8 \r\n" +
                    "User-Agent: Mozilla/5.0 (X11; Linux i686; rv:2.0.1) Gecko/20100101 Firefox/4.0.1 \r\n" +
                    "Accept-Language: ro \r\n" +
                    "Content-Language: en, ase, ru \r\n" +
                    "Vary: Accept-Encoding \r\n" +
                    "\r\n";

            outputWriter.println(dataRequest);
            outputWriter.flush();

            if (outputWriter.checkError())
                System.out.println(
                        "SSLSocketClient:java.io.PrintWriter error");

            BufferedReader InputReader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            String inputLine;
            while ((inputLine = InputReader.readLine()) != null)
                serverResponse.append(inputLine).append("\n");

            InputReader.close();
            outputWriter.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Secured connection successfully");
        return serverResponse.toString();
    }

    public static String getNameImages(String text, String hostName) {
        String result;
        if (text.contains(hostName)) {
            result = text.replace(hostName, "");
            result = result.replace("'", "");
            System.out.println(result);
        } else result = text;
        return result;
    }

    private static void getImg(String imgName) throws Exception {
        Socket socket = new Socket(HOST_ME_UTM_MD, PORT_ME_UTM_MD);
        DataOutputStream bw = new DataOutputStream(socket.getOutputStream());
        bw.writeBytes("GET /" + imgName + " HTTP/1.1\r\n");
        bw.writeBytes("Host: " + HOST_ME_UTM_MD + ":80\r\n");
        bw.writeBytes("Content-Type: text/html;charset=utf-8 \r\n");
        bw.writeBytes("User-Agent: Mozilla/5.0 (X11; Linux i686; rv:2.0.1) Gecko/20100101 Firefox/4.0.1 \r\n");
        bw.writeBytes("Accept-Language: ro \r\n");
        bw.writeBytes("Content-Language: en, ase, ru \r\n");
        bw.writeBytes("Vary: Accept-Encoding \r\n");
        bw.writeBytes("\r\n");

        bw.flush();

        String[] tokens = imgName.split("/");

        final DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        final OutputStream fileOutputStream = new FileOutputStream("images/" + tokens[tokens.length - 1]);

        unloadImg(socket, inputStream, fileOutputStream);
    }

    private static void getImagesSecured(String imgName) {
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(HOST_UTM_MD, PORT_UTM_MD);
            socket.startHandshake();

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                                    socket.getOutputStream())));

            out.println("GET " + imgName + " HTTP/1.1\r\nHost: " + HOST_UTM_MD + " \r\n\r\n");
            out.flush();

            if (out.checkError())
                System.out.println(
                        "SSLSocketClient:java.io.PrintWriter error");
            String[] tokens = imgName.split("/");

            OutputStream outputStream  = new FileOutputStream("images/" + tokens[tokens.length - 1]);
            unloadImg(socket, inputStream, outputStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void unloadImg(Socket socket, DataInputStream inputStream, OutputStream fileOutputStream) throws IOException {
        boolean headerEnded = false;

        byte[] bytes = new byte[NUM_OF_BITES];
        int length;
        while ((length = inputStream.read(bytes)) != -1) {
            if (headerEnded)
                fileOutputStream.write(bytes, 0, length);
            else {
                for (int i = 0; i < NUM_OF_BITES; i++) {
                    if (bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) {
                        headerEnded = true;
                        fileOutputStream.write(bytes, i+4, NUM_OF_BITES -i-4);
                        break;
                    }
                }
            }
        }
        inputStream.close();
        fileOutputStream.close();

        System.out.println("Status done");

        socket.close();
    }
}
