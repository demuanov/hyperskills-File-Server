package client;


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;


public class Main implements Serializable {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 23456;


//        public static String testPath = File.separator + "File server" + File.separator + "task" + File.separator;
    public static String testPath = File.separator;
    public static String userPATH = System.getProperty("user.dir") + testPath + "src" + File.separator + "client" + File.separator + "data" + File.separator;

  /**
     * Serialize the given object to the file
     */
    public static void serialize(Object obj, String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.close();
    }

    /**
     * Deserialize to an object from the file
     */
    public static Object deserialize(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(fileName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    public static String byNameOrID() {
        Scanner scanner = new Scanner(System.in);
        String answer = null;
        if (scanner.next().equals("1")) {
            System.out.println("Enter name of the file: ");
            answer = "BY_NAME " + scanner.next();

        } else {
            System.out.println("Enter id: ");
            answer = "BY_ID " + scanner.next();
        }

        return answer;
    }


    public static void main(String[] args) throws ClassNotFoundException, InterruptedException {
    Thread.sleep(1000);
        System.out.println("Client started!");
        System.out.println("Enter action (1 - get a file, 2 - create a file, 3 - delete a file): ");

        try (
                Socket socket = new Socket(InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                Scanner sc = new Scanner(System.in)

        ) {socket.setSoTimeout(100000*10000);
//            socket.bind();
            String typeRequest = sc.nextLine();
            String answer = null;

            if (typeRequest.equals("1")) {
                System.out.println("Do you want to get the file by name or by id (1 - name, 2 - id): ");

                answer = "GET ".concat(byNameOrID());

                sentRequest(answer, output);
                String response = inputRead(input);

                if (response.contains("200")) {

                    System.out.println("The file was downloaded! Specify a name for it: ");
                    String newName = sc.nextLine();
                    String result = "";
                    try {
                        result = response.substring(4).trim();
                    } catch (IndexOutOfBoundsException e) {
                        result = "";
                    }
//                    File file = new File(userPATH + result );
                    Path rename = Paths.get(userPATH + result);
Files.move(rename, rename.resolveSibling(newName), StandardCopyOption.REPLACE_EXISTING);
//                    System.out.println(file.renameTo(rename));
                    //Написать перименовалку файла, в совтветсвии с пожеланием клиента


                } else {
                    System.out.println("The response says that this file is not found!");
                }
            }

            if (typeRequest.equals("2")) {

                String nameOnServer;

                answer = "PUT ";
                System.out.println("Enter name of the file: ");
                String fileName = sc.nextLine();
                System.out.println("Enter name of the file to be saved on server: ");

                nameOnServer = sc.nextLine();

                if ((nameOnServer.isEmpty())||(nameOnServer.equals("\n"))) {
                    nameOnServer = "NO_ID";
                }

                answer = answer.concat(fileName + " ");
                answer = answer.concat(nameOnServer + " ");
//                System.out.println("Answer= " + answer);
                sentRequest(answer, output);
                answer = null;

                String response = inputRead(input);
//                System.out.println(response);

                if (response.contains("200")) {
                    System.out.println("Response says that file is saved! ID ="+ response.substring(3));
                } else {
                    System.out.println("The response says that creating the file was forbidden!");
                }
            }

            if (typeRequest.equals("3")) {
                System.out.println("Do you want to delete the file by name or by id (1 - name, 2 - id): ");
                answer = "DELETE ".concat(byNameOrID());
                sentRequest(answer, output);
                answer = "";
                String response = inputRead(input);
                if (response.contains("200")) {
                    System.out.println("The response says that this file was deleted successfully!");
                } else {
                    System.out.println("The response says that the file was not found!");
                }
            }
            if (typeRequest.equals("exit")) {
                sentRequest("exit", output);
                System.out.println("The request was sent.");
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void sentRequest(String answer, DataOutputStream output) throws IOException {
        byte[] message = answer.getBytes(StandardCharsets.UTF_8);
        output.writeInt(message.length);
        output.write(message);

//        System.out.println("The request was sent. > "+ answer);
    }

    private static String inputRead(DataInputStream input) throws IOException {
        int length = input.readInt();
        byte[] message = new byte[length];
        input.readFully(message, 0, message.length);
        return new String(message, StandardCharsets.UTF_8);
    }


}
