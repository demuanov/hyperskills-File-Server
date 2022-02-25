package server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
/*
0 - is no file
1 - file is add
2 - file sent
3 - file deleted
*/


public class Main implements Serializable {
    static String[] files = new String[11];
    private static String value;
    public String file;

    public static boolean isFileTreeExist = false;

    public static String filePATH = System.getProperty("user.dir") + File.separator + "File server" + File.separator + "task" +
            File.separator + "src" + File.separator + "server" + File.separator + "data" + File.separator;

    public static String userPATH = System.getProperty("user.dir") + File.separator + "File server" + File.separator + "task" +
            File.separator + "src" + File.separator + "client" + File.separator + "data" + File.separator;

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


    private static void addFile(String[] next, String name) {
        int index = 15;

        try {
            index = Integer.parseInt(next[1]);
        } catch (NumberFormatException e) {
//            System.out.format("Cannot add the file %s\n", name);
        }

        if (index < files.length) {
            if (files[index] == ("add")) {
                System.out.format("Cannot add the file %s\n", name);
            } else {
                files[index] = "add";
                System.out.format("The file %s added successfully\n", name);
            }
        } else {
            System.out.format("Cannot add the file %s\n", name);
        }
    }

    private static void getFile(String[] gettingFile, String name) {
        int index = Integer.parseInt(gettingFile[1]);
        if (index < files.length) {
            if (files[index] == "add") {
                System.out.format("The file %s was sent\n", name);
            } else {
                System.out.format("The file %s not found\n", name);
            }

        } else {
            System.out.format("The file %s not found\n", name);
        }
    }

    private static void deleteFile(String[] delete, String name) {
        int index = Integer.parseInt(delete[1]);
        if (index <= files.length - 1) {
            if (files[index] == "add") {
                files[index] = null;
                System.out.format("The file %s was deleted\n", name);
            } else {
                System.out.format("The file %s not found\n", name);
            }
        } else {
            System.out.format("The file %s not found\n", name);
        }

    }

    public static String[] readFile(String method, String value) {
        String input = new Scanner(System.in).nextLine();
        String[] input2 = input.split(" ");

        return input2;

    }

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 23456;

    public static String deleteRequest(String request, TreeMap<Integer, String> fileTreeMap) {

        String[] pathFile = request.split(" ");
        String f_path = null;
        String serverPath = null;
        String result = "404";
        String fileName = null;
        Integer fileID = null;

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

        f_path = userPATH.concat(fileName);
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
        String f_path = "";
        String serverPath;
// TODO: починить таблицу, для храненния ид и только имени файла.
// User path of file
        if (pathFile[2].equals("BY_ID")) {
            f_path = userPATH.concat(getFileFromID((pathFile[1]), fileTreeMap));
            serverPath = filePATH.concat(getFileFromID((pathFile[1]), fileTreeMap));
        } else {
            f_path = userPATH.concat(pathFile[1]);
            serverPath = filePATH.concat(pathFile[1]);
        }

        String result = "403";

        try {
            if (copyFileUsingStream(new File(f_path), new File(serverPath))) {

                if (pathFile[2].equals("NO_ID")) {
                    fileTreeMap.put(hCode((pathFile[1])), pathFile[1]);
                } else {
                    fileTreeMap.put(Integer.valueOf(pathFile[2]), pathFile[1]);
                }

                result = "200";
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


       /* try {
            FileReader reader = new FileReader(file);
            Scanner scan = new Scanner(reader);

            while (scan.hasNextLine()) {
                buf = buf + scan.nextLine();
            }
            result = "200";
            scan.close();
            reader.close();
        } catch (IOException e) {
//            e.printStackTrace();
            result = "404";
        }
*/
        try {
            copyFileUsingStream(new File(serverPath), new File(f_path));
            result = "200";
        } catch (Exception e) {
//            e.printStackTrace();
            result = "404";
        }
//добавить нуль поинтер эксепт
        String output = result + " " + buf;
        return output.trim();
    }

    private static String getFileFromID(String s, TreeMap<Integer, String> fileTreeMap) {

        return (String) fileTreeMap.get(Integer.valueOf(s));

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
        } finally {
            is.close();
            os.close();
        }
        return result;
    }

    private static void sentRequest(String answer, DataOutputStream output) throws IOException {
        byte[] message = answer.getBytes(StandardCharsets.UTF_8);
        output.writeInt(message.length);
        output.write(message);

        System.out.println("The request was sent.");
    }

    public static int hCode(String files) {
          /* Date date = new Date();
            //This method returns the time in millis
            long timeMilli = date.getTime();

            return (int) timeMilli %100;*/
        return Math.abs(files.hashCode() % 100);
    }

    private static void generateID(TreeMap fileTreeMap) {
        File dir = new File(userPATH); //path указывает на директорию
        File[] arrFiles = dir.listFiles();
        String[] strFiles = (String[]) Arrays.stream(arrFiles).toArray();

        for (var files : strFiles) {
            fileTreeMap.put(hCode(files), files);

        }
        fileTreeMap.forEach((x, y) -> {
            System.out.println(x + "=" + y);
        });

        try {
            saveFile(fileTreeMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveFile(TreeMap<Integer, String> members)
            throws IOException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(filePATH + File.separator + "table.txt"))) {
            os.writeObject(members);
        }
    }

    public static TreeMap<Integer, String> readFile()
            throws ClassNotFoundException, IOException {
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(filePATH + File.separator + "table.txt"))) {
            return (TreeMap<Integer, String>) is.readObject();
        }
    }

    public static void print(TreeMap<Integer, String> fileTreeMap) {
        fileTreeMap.entrySet().forEach(entry -> {
            Integer x = entry.getKey();
            String y = entry.getValue();
            System.out.println(x + "=" + y);
        });
    }

    public static void updateMap(TreeMap<Integer, String> fileTreeMap) throws IOException, ClassNotFoundException {
        File file = new File(filePATH + File.separator + "table.txt");

        isFileTreeExist = file.exists();

        if (isFileTreeExist) {
            TreeMap<Integer, String> lol = readFile();
            fileTreeMap.putAll(lol);
        } else {
            saveFile(fileTreeMap);
        }
    }

    public static void serverLogic(DataInputStream input,
                                   DataOutputStream output,
                                   TreeMap<Integer, String> fileTreeMap,
                                   ServerSocket server) throws IOException {
        byte[] taskCode = inputRead(input);

        String inputMessage = new String(taskCode, StandardCharsets.UTF_8);
        if (inputMessage.contains("GET")) {
            print(fileTreeMap);
            sentRequest(getRequest(inputMessage, fileTreeMap), output);
        }

        if (inputMessage.contains("PUT")) {
            print(fileTreeMap);
            sentRequest(putRequest(inputMessage, inputMessage, fileTreeMap), output);
            saveFile(fileTreeMap);
        }

        if (inputMessage.contains("DELETE")) {
            sentRequest(deleteRequest(inputMessage, fileTreeMap), output);
            print(fileTreeMap);
            saveFile(fileTreeMap);
        }

        if (inputMessage.contains("exit")) {
            server.close();
        }
    }

    public static void main(String[] args) {
        System.out.println("Server started!");
        TreeMap<Integer, String> fileTreeMap = new TreeMap<>();
//        generateID(fileTreeMap);

        ExecutorService executor = Executors.newFixedThreadPool(5);

        try (ServerSocket server = new ServerSocket(SERVER_PORT, 50, InetAddress.getByName(SERVER_ADDRESS))
        ) {


            while (true) {
                try (
                        Socket socket = server.accept(); // accepting a new client
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                ) {
                    updateMap(fileTreeMap);
                    executor.submit(() -> {
                                try {
                                    serverLogic(input, output, fileTreeMap, server);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    executor.shutdown();
                                }
                            }
                    );

                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


}

