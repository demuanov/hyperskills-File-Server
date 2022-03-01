package server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/*
0 - is no file
1 - file is add
2 - file sent
3 - file deleted
*/


public class Main implements Serializable {
    static String[] files = new String[11];
    public static boolean isFileTreeExist = false;

//    public static String testPath = File.separator + "File server" + File.separator + "task" + File.separator;
    public static String testPath = File.separator;


    public static String filePATH = System.getProperty("user.dir") + testPath + "src" + File.separator + "server" + File.separator + "data" + File.separator;

    public static String serverPATH = System.getProperty("user.dir") + testPath + "src" + File.separator + "server" + File.separator + "table.txt";

    public static String userPATH = System.getProperty("user.dir") + testPath + "src" + File.separator + "client" + File.separator + "data" + File.separator;

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


    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 23456;

    public static String deleteRequest(String request, TreeMap<Integer, String> fileTreeMap) {

        String[] pathFile = request.split(" ");
        String serverPath;
        String result;
        String fileName;
        Integer fileID;

        if (pathFile[1].equals("BY_ID")) {
            if (fileTreeMap.containsKey(Integer.valueOf(pathFile[2]))) {
                fileName = getFileFromID((pathFile[2]), fileTreeMap);
                fileID = Integer.valueOf(pathFile[2]);
            } else {
                return "404";
            }
        } else {
            if (fileTreeMap.containsValue(pathFile[2])) {
                fileName = pathFile[2];
                fileID = getKeysByValue(fileTreeMap, fileName);
            } else {
                return "404";
            }
        }

        serverPath = filePATH.concat(fileName);

        try {
            File f = new File(serverPath);
            if (f.delete()) {
                fileTreeMap.remove(fileID); // удалять по ключу
                result = "200";
            } else {
                result = "404";
            }

        } catch (
                Exception e) {
            e.printStackTrace();
            result = "404";
        }

        return result;
    }

    public static String putRequest(String request, String information, TreeMap<Integer, String> fileTreeMap) {
        //"PUT BY_ID 23" GET BY_NAME filename.txt
        String[] pathFile = request.split(" ");
        String f_path = null;
        String serverPath = null;
        if (pathFile[2].equals("NO_ID")) {
            f_path = userPATH.concat(pathFile[1]);
            serverPath = filePATH.concat(pathFile[1]);
        }
        if (pathFile[2].contains(".")) {

            f_path = userPATH.concat(pathFile[1]);
            serverPath = filePATH.concat(pathFile[2]);
        }

        String result = "403";

        try {
            if (copyFileUsingStream(new File(f_path), new File(serverPath))) {

                Integer hashCODE = null;
                if (pathFile[2].equals("NO_ID")) {
                    fileTreeMap.put(hCode((pathFile[1])), pathFile[1]);
                    hashCODE = hCode(pathFile[1]);
                }
                if (pathFile[2].contains(".")) {
                    fileTreeMap.put(hCode(pathFile[2]), pathFile[2]);
                    hashCODE = hCode(pathFile[2]);
                }


                result = "200 " + hashCODE;
            } else {
                result = "403";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            saveFile(fileTreeMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String getRequest(String request, TreeMap<Integer, String> fileTreeMap) {
//"GET BY_ID 23" GET BY_NAME filename.txt

        String[] pathFile = request.split(" ");
        String f_path = null;
        String serverPath = null;
        String buf = null;

        if (pathFile[1].equals("BY_ID")) {
            f_path = userPATH.concat(getFileFromID((pathFile[2]), fileTreeMap));
            serverPath = filePATH.concat(getFileFromID((pathFile[2]), fileTreeMap));
            buf = getFileFromID((pathFile[2]), fileTreeMap);
        } else {
            f_path = userPATH.concat(pathFile[2]);
            serverPath = filePATH.concat(pathFile[2]);
            buf = pathFile[2];
        }
        String result = "404";

        try {
            if (copyFileUsingStream(new File(serverPath), new File(f_path))) {
                result = "200";
            }
        } catch (Exception e) {
//            e.printStackTrace();
            result = "404";
        }
//добавить нуль поинтер эксепт
        String output = result + " " + buf;
        return output.trim();
    }

    private static String getFileFromID(String s, TreeMap<Integer, String> fileTreeMap) {
if (fileTreeMap.containsKey(Integer.valueOf(s))) {
    return (String) fileTreeMap.get(Integer.valueOf(s));
}
return "NONE";
    }

    public static Integer getKeysByValue(TreeMap<Integer, String> map, String value) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static byte[] inputRead(DataInputStream input) throws IOException {
        int length = input.readInt();
        byte[] message = new byte[length];
        input.readFully(message, 0, message.length);
        return message;

    }

    private static boolean copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        boolean result = false;

        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
                result = true;
            }
        } catch (IOException e) {
//            e.printStackTrace();
            result = false;
        }
        finally {
            is.close();
            os.close();
        }
        return result;
    }

    private static void sentRequest(String answer, DataOutputStream output) throws IOException {
        byte[] message = answer.getBytes(StandardCharsets.UTF_8);
        output.writeInt(message.length);
        output.write(message);
//        System.out.println("Answer= "+ answer + "   request= "+ message);
        System.out.println("The request was sent.");
    }

    public static int hCode(String files) {
        return Math.abs(files.hashCode() % 100);
    }

    public static void saveFile(TreeMap<Integer, String> members)
            throws IOException {
        try (ObjectOutputStream os = new ObjectOutputStream(
                new FileOutputStream(serverPATH))) {
            os.writeObject(members);
        }
    }

    public static TreeMap<Integer, String> readFile()
            throws ClassNotFoundException, IOException {
        try (ObjectInputStream is = new ObjectInputStream(
                new FileInputStream(serverPATH))) {
            return (TreeMap<Integer, String>) is.readObject();
        }
    }

    public static void print(TreeMap<Integer, String> fileTreeMap) {
        fileTreeMap.forEach((x, y) -> System.out.println(x + "=" + y));
    }

    public static void updateMap(TreeMap<Integer, String> fileTreeMap) throws IOException, ClassNotFoundException {
        File file = new File(serverPATH);

        isFileTreeExist = file.exists();

        if (isFileTreeExist) {
            TreeMap<Integer, String> lol = readFile();
            fileTreeMap.putAll(lol);
        } else {
            saveFile(fileTreeMap);
        }
        Set<Map.Entry<Integer, String> > entries
                = fileTreeMap.entrySet();
        entries.forEach(entry ->{
            String value = entry.getValue();
            Path valuePath = Paths.get(filePATH+value);

            if (!Files.exists(valuePath)){
                try {
                    copyFileUsingStream(new File(userPATH + value), new File(String.valueOf(valuePath.toFile())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });


    }

    public static String serverLogic(TreeMap<Integer, String> fileTreeMap,
                                     ServerSocket server, String inputMessage) throws IOException {
        String information = null;
        if (inputMessage.contains("GET")) {
//            print(fileTreeMap);
            information = getRequest(inputMessage, fileTreeMap);
        }

        if (inputMessage.contains("PUT")) {
            information = putRequest(inputMessage, inputMessage, fileTreeMap);
//            System.out.println(information);
            saveFile(fileTreeMap);
        }

        if (inputMessage.contains("DELETE")) {
            information = deleteRequest(inputMessage, fileTreeMap);
//            print(fileTreeMap);
            saveFile(fileTreeMap);
        }

        if (inputMessage.contains("exit")) {
            server.close();
        }

        return information;
    }

    public static void main(String[] args) {
        System.out.println("Server started!");
        TreeMap<Integer, String> fileTreeMap = new TreeMap<>();
//        generateID(fileTreeMap);


        try (ServerSocket server = new ServerSocket(SERVER_PORT, 50, InetAddress.getByName(SERVER_ADDRESS))
        ) {

            while (true) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try (
                        Socket socket = server.accept(); // accepting a new client
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream())

                ) {
                    updateMap(fileTreeMap);

                    byte[] taskCode = inputRead(input);
                    String inputMessage = new String(taskCode, StandardCharsets.UTF_8);
                    Future<String> outputMessage;

                    Callable callable = () -> {
//                        Thread.sleep(2000);
                        return serverLogic(fileTreeMap, server, inputMessage);
                    };

                    outputMessage = executor.submit(callable);
                    sentRequest(outputMessage.get(), output);
//                    sentRequest(serverLogic(input, output, fileTreeMap, server, inputMessage), output);


                } catch (IOException | ClassNotFoundException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
//                catch (ClassNotFoundException e) {e.printStackTrace();}

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

