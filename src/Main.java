import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class Main {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {

        DBUtils.connect("dbTerentev.s3db");
        DBUtils.getQuery1();
        DBUtils.getQuery2();
        DBUtils.getQuery3();
        Chart chart = new Chart();
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }
}
class Chart extends ApplicationFrame {

    public Chart() throws SQLException {
        super("");
        JFreeChart barChart = ChartFactory.createBarChart(
                "Соотношение количества домов с разным количеством этажей", "Количество этажей", "Количество зданий",
                createDataset(),
                PlotOrientation.VERTICAL,
                true, true, false);
        ChartPanel chartPanel = new ChartPanel( barChart );
        chartPanel.setPreferredSize(new java.awt.Dimension(800 , 600) );
        setContentPane(chartPanel);
    }
    private CategoryDataset createDataset() throws SQLException {
        Map<String, String> chartData = DBUtils.getQuery1();
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset( );

        for (String a: chartData.keySet())
            dataset.addValue( Float.parseFloat(chartData.get(a)) , a, "Количество этажей" );

        return dataset;
    }
}
class DBUtils {
    public static Connection connection;
    public static Statement statement;
    public static ResultSet resultSet;
    public static Map<String, String> getQuery1() throws SQLException {
        System.out.println("\nДома с кол-вом этажей и их кол-во:");
        Map<String, String> chartData = new HashMap<String, String>();
        resultSet = statement.executeQuery("select\n" + "    buildingTypeFloors as 'Кол-во этажей',\n" + "    count(buildingTypeFloors) as 'Кол-во таких зданий'\n" +
                "from Buildings\n" + "where buildingTypeFloors > 0\n" + "group by buildingTypeFloors;");
        while (resultSet.next()) {
            System.out.println(resultSet.getString(1) +"  -  "+resultSet.getString(2));
            chartData.put(resultSet.getString(1), resultSet.getString(2));
        }
        return chartData;
    }
    public static void getQuery2() throws SQLException {
        System.out.println("\nЗарегистрированные участки, по улице шлиссельбургское шоссе с префиксом 9881:");
        resultSet = statement.executeQuery("select\n" + "    description,\n" + "    address\n" +
                "from Buildings inner join Prefixes\n" + "on Buildings.number = Prefixes.number\n" + "where\n" + "    address like '%лиссель%шоссе%'\n" +
                "    and prefixCode = 9881\n" + "    and description like '%арегистри%часто%';");
        while (resultSet.next()) {
            System.out.println(resultSet.getString(1) +"  -  "+resultSet.getString(2));
        }
    }
    public static void getQuery3() throws SQLException {
        System.out.println("\nУниверситеты выше 5 этажа с известным годом постройки и вычислите средний prefix_code:");
        resultSet = statement.executeQuery("select\n" + "    avg(prefixCode) as 'Средний префикс'\n" + "from Buildings inner join Prefixes\n" +
                "on Buildings.number = Prefixes.number\n" + "where\n" + "    yearConstruction <> ''\n" + "    and buildingTypeFloors > 5\n" + "    and description like '%ниверсит%';");
        while (resultSet.next()) {
            System.out.println(resultSet.getString(1));
        }
    }
    public static void connect(String dbName) throws ClassNotFoundException, SQLException {
        System.out.println("Подключение к базе данных \""+dbName+"\"...");
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:"+dbName);
        statement = connection.createStatement();
        System.out.println("База \""+dbName+"\" подключена!");
    }
    public static void fillDataToDB(List<Building> parsedBuildings, List<Prefix> parsedPrefixes) throws SQLException {
        System.out.println("Заполнение базы данных...");
        int querySize = 800;
        int sizePrefixes = parsedPrefixes.size();
        int sizeBuildings = parsedBuildings.size();

        fillPrefixesTable(parsedPrefixes, querySize, sizePrefixes);
        fillBuildingsTable(parsedBuildings, querySize, sizeBuildings);

        System.out.println("Заполнение базы данных прошло успешно!");
    }
    private static void fillBuildingsTable(List<Building> parsedBuildings, int querySize, int sizeBuildings) {
        for (int i = 0; i < sizeBuildings; i += querySize)
            try {
                System.out.print("Процесс заполнения таблицы зданий:"+String.format("%.2f", (float)i/(float) sizeBuildings *100.0f)+"%\r");

                String query = "BEGIN TRANSACTION;\n";
                for (int j = 0; j < querySize; j++) {
                    Building building = parsedBuildings.get(i+j);
                    query += "INSERT INTO 'Buildings' ('id', 'number', 'address', 'buildingTypeMaterial', 'buildingTypeHabited'," +
                            " 'yearConstruction', 'buildingTypeFloors', 'description')" + " VALUES ("+building.id_+", '"+building.number+"', '"+building.address+"', '"+
                            building.buildingTypeMaterial+"', "+(building.buildingTypeHabited?1:0)+", '"+ building.yearConstruction+"', "+building.buildingTypeFloors+", '"+building.description+"');\n";
                }
                statement.executeUpdate(query+"COMMIT;");
            } catch (Exception e) {}
    }
    private static void fillPrefixesTable(List<Prefix> parsedPrefixes, int querySize, int sizePrefixes) {
        for (int i = 0; i < sizePrefixes; i += querySize)
            try {
                System.out.print("Процесс заполнения таблицы префиксов:"+String.format("%.2f", (float)i/(float) sizePrefixes *100.0f)+"%\r");

                String query = "BEGIN TRANSACTION;\n";
                for (int j = 0; j < querySize; j++) {
                    Prefix prefix = parsedPrefixes.get(i+j);
                    query += "INSERT INTO 'Prefixes' ('prefixCode', 'id_', 'number')" +
                            " VALUES (" + prefix.prefix_code + ", " + prefix.id_ + ", '" + prefix.number + "');\n";
                }
                statement.executeUpdate(query+"COMMIT;");
            } catch (Exception e) {}
    }
    public static void createStructure() throws ClassNotFoundException, SQLException {
        System.out.println("Настройка базы данных...");
        statement.executeUpdate("PRAGMA foreign_keys=on;\n"+ "create table if not exists [Buildings] (\n" + "[id] INTEGER  NULL,\n" +
                "[number] VARCHAR(20)  NULL PRIMARY KEY,\n" + "[address] TEXT  NULL,\n" + "[buildingTypeMaterial] TEXT  NULL,\n" + "[buildingTypeHabited] BOOLEAN  NULL,\n" +
                "[yearConstruction] TEXT  NULL,\n" + "[buildingTypeFloors] INTEGER  NULL,\n" + "[description] TEXT  NULL,\n" +
                "FOREIGN KEY (number) REFERENCES Prefixes(number)\n"+ ");\n" + "create table if not exists [Prefixes] (\n" + "[prefixCode] INTEGER  NULL,\n" +
                "[id_] INTEGER  NULL,\n" + "[number] VARCHAR(20) PRIMARY KEY UNIQUE NOT NULL\n" + ");");
        System.out.println("Процесс настройки структуры базы данных завершен");
    }
}
class Parser {
    private static List<Building> parsedBuildings = new ArrayList<Building>();
    private static List<Prefix> parsedPrefixes = new ArrayList<Prefix>();
    public static void parseData(String path) throws IOException {
        System.out.println("Запущен процесс парсинга");
        List<String> fileLines = Files.readAllLines(Paths.get(path));
        for (String line : fileLines.subList(2,fileLines.size()-1)) {
            processParsedLine(line);
        }
        System.out.println("Парсинг окончен");
    }
    public static List<Building> getParsedBuildings(){ return parsedBuildings; }
    public static List<Prefix> getParsedPrefixes(){ return parsedPrefixes; }
    private static void processParsedLine(String line) {
        try {
            String[] data = line.replace("\"", "").replace("\'", "").split(";");
            String number = data[0];
            String address = data[1];
            String snapshot = data[2];
            String description = data[3];
            String numberOfFloor = data[4];
            int prefixCode = -1;
            try {prefixCode = Integer.parseInt(data[5]);} catch (Exception e){}
            String buildingType = data[6];
            int id_ = -1;
            try {id_ = Integer.parseInt(data[7]);} catch (Exception e){}
            String year = "";
            if (data.length == 9)
                year = data[8];

            parsedBuildings.add(new Building(
                    id_, number, address, getMaterial(snapshot),
                    year, getFloorsCount(snapshot, numberOfFloor),
                    description, isHabited(snapshot, buildingType)));

            parsedPrefixes.add(new Prefix(id_, number, prefixCode));
        }
        catch (Exception e){
            System.out.println("Возникла ошибка: "+line);
        }
    }
    private static String getMaterial(String snapshot) {
        if (snapshot.contains("К"))
            return "Каменнный";
        if (snapshot.contains("Д"))
            return "Деревяный";
        return "";
    }
    private static boolean isHabited(String snapshot, String buildingType) {
        if (snapshot.contains("Н"))
            return false;
        if (snapshot.contains("Ж"))
            return true;
        if (buildingType.contains("Нежи"))
            return false;
        if (buildingType.contains("Жи"))
            return true;
        return false;
    }
    private static int getFloorsCount(String snapshot, String buildingType) {
        if (snapshot.length() > 0 && Character.isDigit(snapshot.charAt(0)))
                return Integer.parseInt(Character.toString(snapshot.charAt(0)));
        if (buildingType.length() > 0 && Character.isDigit(buildingType.charAt(0)))
                return Integer.parseInt(Character.toString(buildingType.charAt(0)));
        return -1;
    }
}
class Building {
    public int id_;
    public String number;
    public String address;
    public String buildingTypeMaterial;
    public boolean buildingTypeHabited;
    public String yearConstruction;
    public int buildingTypeFloors;
    public String description;

    public Building(int id_,
                    String number,
                    String address,
                    String buildingTypeMaterial,
                    String yearConstruction,
                    int buildingTypeFloors,
                    String description,
                    boolean buildingTypeHabited) {
        this.id_ = id_;
        this.number = number;
        this.address = address;
        this.buildingTypeMaterial = buildingTypeMaterial;
        this.yearConstruction = yearConstruction;
        this.buildingTypeFloors = buildingTypeFloors;
        this.description = description;
        this.buildingTypeHabited = buildingTypeHabited;
    }

    @Override
    public String toString() {
        return "\nОбъект Building:" + "\nid_=" + id_ + "\nnumber=" + number + "\naddress=" + address + "\nbuildingTypeMaterial=" + buildingTypeMaterial +
                "\nyearConstruction=" + yearConstruction + "\nbuildingTypeFloors=" + buildingTypeFloors + "\ndescription=" + description + "\nbuildingTypeHabited=" + buildingTypeHabited;
    }
}
class Prefix {
    public int prefix_code;
    public int id_;
    public String number;

    public Prefix(int id_, String number, int prefix_code) {
        this.id_ = id_;
        this.number = number;
        this.prefix_code = prefix_code;
    }
}