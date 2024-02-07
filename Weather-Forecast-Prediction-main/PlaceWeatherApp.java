import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class PlaceWeatherApp {
    private static DefaultTableModel tableModel;
    private static JTextPane weatherTextArea;
    private static TimeSeries temperatureSeries;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(new JFrame());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Place Weather App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel panel = new JPanel();
        frame.add(panel);
        panel.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        JLabel locationLabel = new JLabel("Enter Location: ");
        JTextField locationField = new JTextField(20);
        JButton getWeatherButton = createStyledButton("Get Weather", new Color(33, 150, 243));

        inputPanel.add(locationLabel);
        inputPanel.add(locationField);
        inputPanel.add(getWeatherButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Category");
        tableModel.addColumn("Value");
        JTable dataTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                component.setFont(new Font("Arial", Font.PLAIN, 16)); // Adjust the font size here
                return component;
            }
        };
        JScrollPane scrollPane = new JScrollPane(dataTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        weatherTextArea = new JTextPane();
        weatherTextArea.setEditable(false);
        panel.add(new JScrollPane(weatherTextArea), BorderLayout.SOUTH);

        JButton clearButton = createStyledButton("Clear", new Color(244, 67, 54));

        JToggleButton themeToggle = new JToggleButton("Toggle Theme");
        themeToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleTheme(frame, themeToggle);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        buttonPanel.add(themeToggle);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Create and add the temperature chart panel
        JPanel chartPanel = createTemperatureChartPanel();
        panel.add(chartPanel, BorderLayout.EAST);

        frame.setVisible(true);

        getWeatherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String location = locationField.getText().trim();
                if (!location.isEmpty()) {
                    getWeatherData(location);
                }
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                locationField.setText("");
                clearTableModel(tableModel);
                weatherTextArea.setText("");
                clearTemperatureChart();
            }
        });
    }

    private static JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(backgroundColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.PLAIN, 20));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            }
        });

        return button;
    }

    private static void toggleTheme(JFrame frame, JToggleButton themeToggle) {
        try {
            if (themeToggle.isSelected()) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }

            SwingUtilities.updateComponentTreeUI(frame);
            frame.pack();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private static void getWeatherData(String location) {
        String apiKey = "a301a9da9dec12f7b394e5599cf244ff";
        String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + location + "&appid=" + apiKey;

        SwingWorker<Void, Void> weatherWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    Scanner scanner = new Scanner(connection.getInputStream());

                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        response.append(line).append("\n");
                    }

                    System.out.println("API Response: " + response.toString()); // Debug print

                    JSONObject jsonResponse = new JSONObject(response.toString());

                    // Clear table before updating
                    SwingUtilities.invokeLater(() -> clearTableModel(tableModel));

                    // Update the table
                    if (jsonResponse.has("main")) {
                        JSONObject mainData = jsonResponse.getJSONObject("main");
                        double temperature = mainData.getDouble("temp") - 273.15;
                        int humidity = mainData.getInt("humidity");

                        SwingUtilities.invokeLater(() -> {
                            updateTableModel(tableModel, "Temperature", String.format("%.2f °C", temperature));
                            updateTableModel(tableModel, "Humidity", humidity + "%");
                        });

                        if (jsonResponse.has("weather")) {
                            JSONObject weatherData = jsonResponse.getJSONArray("weather").getJSONObject(0);
                            String description = weatherData.getString("description");
                            SwingUtilities.invokeLater(() -> updateTableModel(tableModel, "Weather", description));
                        }

                        if (jsonResponse.has("wind")) {
                            JSONObject windData = jsonResponse.getJSONObject("wind");
                            double windSpeed = windData.getDouble("speed");
                            SwingUtilities.invokeLater(() -> updateTableModel(tableModel, "Wind Speed", windSpeed + " m/s"));
                        }

                        if (jsonResponse.has("main")) {
                            int pressure = mainData.getInt("pressure");
                            SwingUtilities.invokeLater(() -> updateTableModel(tableModel, "Pressure", pressure + " hPa"));
                        }

                        // Update the temperature chart
                        updateTemperatureChart(temperature);
                    } else {
                        SwingUtilities.invokeLater(() -> clearTableModel(tableModel));
                    }

                    // Update the JTextPane
                    SwingUtilities.invokeLater(() -> appendToPane(weatherTextArea, jsonResponse.toString() + "\n", Color.BLACK));

                    scanner.close();
                    connection.disconnect();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> clearTableModel(tableModel));
                }
                return null;
            }

            @Override
            protected void done() {
                // This method is called on the EDT after doInBackground is finished
                super.done();
            }
        };

        weatherWorker.execute();
    }

    private static void updateTableModel(DefaultTableModel tableModel, String category, String value) {
        tableModel.addRow(new String[]{category, value});
    }

    private static void clearTableModel(DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
    }

    private static void updateTemperatureChart(double temperature) {
        Second currentSecond = new Second();
        temperatureSeries.addOrUpdate(currentSecond, temperature);
    }

    private static void clearTemperatureChart() {
        temperatureSeries.clear();
    }

    private static JPanel createTemperatureChartPanel() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        temperatureSeries = new TimeSeries("Temperature");
        dataset.addSeries(temperatureSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Temperature Chart",
                "Time",
                "Temperature (°C)",
                dataset,
                false,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        return new ChartPanel(chart);
    }

    private static void appendToPane(JTextPane tp, String msg, Color c) {
        StyledDocument doc = tp.getStyledDocument();
        AttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground((MutableAttributeSet) set, c);

        try {
            doc.insertString(doc.getLength(), msg, set);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
