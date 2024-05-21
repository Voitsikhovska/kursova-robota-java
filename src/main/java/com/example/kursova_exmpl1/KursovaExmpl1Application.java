package com.example.kursova_exmpl1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


@SpringBootApplication
@Controller
public class KursovaExmpl1Application {

    private static final String URL = "jdbc:mysql://localhost:3306/kursova";
    private static final String USERNAME = "daryna";
    private static final String PASSWORD = "mandarYnka";

    public static void main(String[] args) {
        SpringApplication.run(KursovaExmpl1Application.class, args);
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/addTrain")
    public String addTrain(@RequestParam int directionCount,
                           @RequestParam List<String> wagonType,
                           @RequestParam List<Integer> wagonCount,
                           @RequestParam List<String> wagonNumbers,
                           @RequestParam List<String> time,
                           @RequestParam List<String> executor,
                           Model model) {

        List<Train> trains = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (int i = 0; i < directionCount; i++) {
            String[] wagonNumberArray = wagonNumbers.get(i).split(" ");
            List<Integer> wagonNumberList = new ArrayList<>();
            for (String number : wagonNumberArray) {
                wagonNumberList.add(Integer.parseInt(number));
            }
            trains.add(new Train(wagonType.get(i), wagonNumberList, time.get(i), executor.get(i)));
        }

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql;
            sql = "INSERT INTO train (wagon_type, wagons, time, executor) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Train train : trains) {
                    statement.setString(1, train.getWagonType());
                    String wagonsJson = objectMapper.writeValueAsString(train.getWagonNumbers());
                    statement.setString(2, wagonsJson);
                    statement.setString(3, train.getTime());
                    statement.setString(4, train.getExecutor());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }

        model.addAttribute("trains", trains);
        model.addAttribute("message", "Дані записано");
        return "report";
    }

    @GetMapping("/readTrains")
    public String readTrains(Model model) {
        List<Train> trains = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "SELECT * FROM train";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String wagonType = resultSet.getString("wagon_type");
                        String wagonNumbers = resultSet.getString("wagons");
                        String time = resultSet.getString("time");
                        String executor = resultSet.getString("executor");

                        List<Integer> wagonNumberList = objectMapper.readValue(wagonNumbers, List.class);
                        trains.add(new Train(wagonType, wagonNumberList, time, executor));
                    }
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }

        model.addAttribute("trains", trains);
        return "report";
    }
}

class Train {
    private String wagonType;
    private List<Integer> wagonNumbers;
    private String time;
    private String executor;

    public Train(String wagonType, List<Integer> wagonNumbers, String time, String executor) {
        this.wagonType = wagonType;
        this.wagonNumbers = wagonNumbers;
        this.time = time;
        this.executor = executor;
    }

    public String getWagonType() {
        return wagonType;
    }

    public List<Integer> getWagonNumbers() {
        return wagonNumbers;
    }

    public String getTime() {
        return time;
    }

    public String getExecutor() {
        return executor;
    }
}
