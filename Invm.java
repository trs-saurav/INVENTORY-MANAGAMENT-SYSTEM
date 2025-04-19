import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Invm {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "admin123";
    
    private JFrame mainFrame;
    private JPanel currentPanel;
    private String currentUser;
    private boolean isAdmin = false;

    public static void main(String[] args) {
        new Invm().showLoginPage();
    }

    // Database initialization
    private void initializeDatabase() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            
            // Create users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    is_admin BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create products table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    quantity INT NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    category VARCHAR(50),
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """);
            
            // Create customers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    phone VARCHAR(20),
                    address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create sales table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    product_id INT,
                    customer_id INT,
                    quantity INT NOT NULL,
                    total_price DECIMAL(10,2) NOT NULL,
                    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (product_id) REFERENCES products(id),
                    FOREIGN KEY (customer_id) REFERENCES customers(id)
                )
            """);
            
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database initialization failed: " + e.getMessage());
        }
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Login Page
    void showLoginPage() {
        mainFrame = new JFrame("Inventory Management System - Login");
        mainFrame.setSize(400, 300);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register New User");
        
        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(passwordField, gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        loginPanel.add(loginButton, gbc);
        
        gbc.gridy = 3;
        loginPanel.add(registerButton, gbc);
        
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            if (validateLogin(username, password)) {
                currentUser = username;
                showMainMenu();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Invalid credentials!");
            }
        });
        
        registerButton.addActionListener(e -> showRegistrationPage());
        
        mainFrame.add(loginPanel);
        mainFrame.setVisible(true);
        initializeDatabase();
    }

    // Registration Page
    private void showRegistrationPage() {
        JFrame regFrame = new JFrame("Register New User");
        regFrame.setSize(400, 300);
        
        JPanel regPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);
        JCheckBox adminCheckBox = new JCheckBox("Register as Admin");
        JButton registerButton = new JButton("Register");
        
        gbc.gridx = 0; gbc.gridy = 0;
        regPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        regPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        regPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        regPanel.add(passwordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        regPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        regPanel.add(confirmPasswordField, gbc);
        
        gbc.gridx = 1; gbc.gridy = 3;
        regPanel.add(adminCheckBox, gbc);
        
        gbc.gridy = 4;
        regPanel.add(registerButton, gbc);
        
        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(regFrame, "Passwords don't match!");
                return;
            }
            
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, username);
                pstmt.setString(2, hashPassword(password));
                pstmt.setBoolean(3, adminCheckBox.isSelected());
                pstmt.executeUpdate();
                conn.close();
                
                JOptionPane.showMessageDialog(regFrame, "Registration successful!");
                regFrame.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(regFrame, "Registration failed: " + ex.getMessage());
            }
        });
        
        regFrame.add(regPanel);
        regFrame.setVisible(true);
    }

    // Validate Login
    private boolean validateLogin(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "SELECT password, is_admin FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                isAdmin = rs.getBoolean("is_admin");
                conn.close();
                return storedPassword.equals(hashPassword(password));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Main Menu
    private void showMainMenu() {
        mainFrame.getContentPane().removeAll();
        mainFrame.setTitle("Inventory Management System - Main Menu");
        
        JPanel menuPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton productsButton = new JButton("Manage Products");
        JButton customersButton = new JButton("Manage Customers");
        JButton salesButton = new JButton("Sales Dashboard");
        JButton reportsButton = new JButton("Generate Reports");
        JButton logoutButton = new JButton("Logout");
        
        productsButton.addActionListener(e -> showProductsPage());
        customersButton.addActionListener(e -> showCustomersPage());
        salesButton.addActionListener(e -> showSalesDashboard());
        reportsButton.addActionListener(e -> showReportsPage());
        logoutButton.addActionListener(e -> {
            currentUser = null;
            isAdmin = false;
            showLoginPage();
        });
        
        menuPanel.add(productsButton);
        menuPanel.add(customersButton);
        menuPanel.add(salesButton);
        menuPanel.add(reportsButton);
        menuPanel.add(logoutButton);
        
        mainFrame.add(menuPanel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    // Products Page
    private void showProductsPage() {
        JPanel productsPanel = new JPanel(new BorderLayout());
        
        // Table Model for Products
        Vector<String> columnNames = new Vector<>();
        columnNames.add("ID");
        columnNames.add("Name");
        columnNames.add("Quantity");
        columnNames.add("Price");
        columnNames.add("Category");
        
        Vector<Vector<Object>> data = new Vector<>();
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM products");
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("quantity"));
                row.add(rs.getDouble("price"));
                row.add(rs.getString("category"));
                data.add(row);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        JTable productsTable = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(productsTable);
        
        // Buttons Panel
        JPanel buttonsPanel = new JPanel();
        JButton addButton = new JButton("Add Product");
        JButton editButton = new JButton("Edit Product");
        JButton deleteButton = new JButton("Delete Product");
        JButton backButton = new JButton("Back to Menu");
        
        addButton.addActionListener(e -> showAddProductDialog());
        editButton.addActionListener(e -> {
            int selectedRow = productsTable.getSelectedRow();
            if (selectedRow >= 0) {
                showEditProductDialog((Integer)productsTable.getValueAt(selectedRow, 0));
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a product to edit.");
            }
        });
        
        deleteButton.addActionListener(e -> {
            int selectedRow = productsTable.getSelectedRow();
            if (selectedRow >= 0) {
                int productId = (Integer)productsTable.getValueAt(selectedRow, 0);
                deleteProduct(productId);
                showProductsPage(); // Refresh the page
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a product to delete.");
            }
        });
        
        backButton.addActionListener(e -> showMainMenu());
        
        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(backButton);
        
        productsPanel.add(scrollPane, BorderLayout.CENTER);
        productsPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        updateMainPanel(productsPanel);
    }

    // Add Product Dialog
    private void showAddProductDialog() {
        JDialog dialog = new JDialog(mainFrame, "Add New Product", true);
        dialog.setSize(400, 300);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField nameField = new JTextField(20);
        JTextField quantityField = new JTextField(20);
        JTextField priceField = new JTextField(20);
        JTextField categoryField = new JTextField(20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        panel.add(quantityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        panel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        panel.add(categoryField, gbc);
        
        JButton saveButton = new JButton("Save");
        gbc.gridx = 1; gbc.gridy = 5;
        panel.add(saveButton, gbc);
        
        saveButton.addActionListener(e -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "INSERT INTO products (name, quantity, price, category) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, nameField.getText());
                pstmt.setInt(2, Integer.parseInt(quantityField.getText()));
                pstmt.setDouble(3, Double.parseDouble(priceField.getText()));
                pstmt.setString(4, categoryField.getText());
                pstmt.executeUpdate();
                conn.close();
                
                dialog.dispose();
                showProductsPage(); // Refresh the products page
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding product: " + ex.getMessage());
            }
        });
        
        dialog.add(panel);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    // Edit Product Dialog
    private void showEditProductDialog(int productId) {
        JDialog dialog = new JDialog(mainFrame, "Edit Product", true);
        dialog.setSize(400, 300);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField nameField = new JTextField(20);
        JTextField quantityField = new JTextField(20);
        JTextField priceField = new JTextField(20);
        JTextField categoryField = new JTextField(20);
        
        // Load current product data
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "SELECT * FROM products WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                quantityField.setText(String.valueOf(rs.getInt("quantity")));
                priceField.setText(String.valueOf(rs.getDouble("price")));
                categoryField.setText(rs.getString("category"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        panel.add(quantityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        panel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        panel.add(categoryField, gbc);
        
        JButton updateButton = new JButton("Update");
        gbc.gridx = 1; gbc.gridy = 5;
        panel.add(updateButton, gbc);
        
        updateButton.addActionListener(e -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "UPDATE products SET name=?, quantity=?, price=?, category=? WHERE id=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, nameField.getText());
                pstmt.setInt(2, Integer.parseInt(quantityField.getText()));
                pstmt.setDouble(3, Double.parseDouble(priceField.getText()));
                pstmt.setString(4, categoryField.getText());
                pstmt.setInt(5, productId);
                pstmt.executeUpdate();
                conn.close();
                
                dialog.dispose();
                showProductsPage(); // Refresh the products page
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating product: " + ex.getMessage());
            }
        });
        
        dialog.add(panel);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    // Delete Product
    private void deleteProduct(int productId) {
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Are you sure you want to delete this product?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "DELETE FROM products WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
                conn.close();
                
                JOptionPane.showMessageDialog(mainFrame, "Product deleted successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(mainFrame, "Error deleting product: " + e.getMessage());
            }
        }
    }

    // Customers Page
    private void showCustomersPage() {
        JPanel customersPanel = new JPanel(new BorderLayout());
        
        // Table Model for Customers
        Vector<String> columnNames = new Vector<>();
        columnNames.add("ID");
        columnNames.add("Name");
        columnNames.add("Email");
        columnNames.add("Phone");
        columnNames.add("Address");
        
        Vector<Vector<Object>> data = new Vector<>();
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM customers");
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getString("email"));
                row.add(rs.getString("phone"));
                row.add(rs.getString("address"));
                data.add(row);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        JTable customersTable = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(customersTable);
        
        // Buttons Panel
        JPanel buttonsPanel = new JPanel();
        JButton addButton = new JButton("Add Customer");
        JButton editButton = new JButton("Edit Customer");
        JButton deleteButton = new JButton("Delete Customer");
        JButton backButton = new JButton("Back to Menu");
        
        addButton.addActionListener(e -> showAddCustomerDialog());
        editButton.addActionListener(e -> {
            int selectedRow = customersTable.getSelectedRow();
            if (selectedRow >= 0) {
                showEditCustomerDialog((Integer)customersTable.getValueAt(selectedRow, 0));
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a customer to edit.");
            }
        });
        
        deleteButton.addActionListener(e -> {
            int selectedRow = customersTable.getSelectedRow();
            if (selectedRow >= 0) {
                deleteCustomer((Integer)customersTable.getValueAt(selectedRow, 0));
                showCustomersPage(); // Refresh the page
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a customer to delete.");
            }
        });
        
        backButton.addActionListener(e -> showMainMenu());
        
        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(backButton);
        
        customersPanel.add(scrollPane, BorderLayout.CENTER);
        customersPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        updateMainPanel(customersPanel);
    }

    // Add Customer Dialog
    private void showAddCustomerDialog() {
        JDialog dialog = new JDialog(mainFrame, "Add New Customer", true);
        dialog.setSize(400, 300);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextArea addressArea = new JTextArea(3, 20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(addressArea), gbc);
        
        JButton saveButton = new JButton("Save");
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(saveButton, gbc);
        
        saveButton.addActionListener(e -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "INSERT INTO customers (name, email, phone, address) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, emailField.getText());
                pstmt.setString(3, phoneField.getText());
                pstmt.setString(4, addressArea.getText());
                pstmt.executeUpdate();
                conn.close();
                
                dialog.dispose();
                showCustomersPage(); // Refresh the customers page
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding customer: " + ex.getMessage());
            }
        });
        
        dialog.add(panel);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    // Sales Dashboard
 // Add this method after showSalesDashboard() in the inv2 class

    private void showSalesDashboard() {
        JPanel salesPanel = new JPanel(new BorderLayout(10, 10));
        salesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create top panel for buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newSaleButton = new JButton("Record New Sale");
        JButton backButton = new JButton("Back to Menu");
        topPanel.add(newSaleButton);
        topPanel.add(backButton);

        // Create center panel with chart and statistics
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Create sales chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = """
                SELECT 
                    DATE_FORMAT(sale_date, '%Y-%m') as month,
                    SUM(total_price) as total,
                    COUNT(*) as transaction_count
                FROM sales 
                GROUP BY month 
                ORDER BY month DESC 
                LIMIT 6
            """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                dataset.addValue(rs.getDouble("total"), "Monthly Sales", rs.getString("month"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Monthly Sales Overview",
            "Month",
            "Sales Amount ($)",
            dataset,
            org.jfree.chart.plot.PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Customize chart appearance
        chart.setBackgroundPaint(Color.WHITE);
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.WHITE);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));

        // Create statistics panel
        JPanel statsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Sales Statistics"));

        // Add statistics
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();

            // Today's sales
            ResultSet rs = stmt.executeQuery("""
                SELECT 
                    COUNT(*) as count,
                    SUM(total_price) as total,
                    AVG(total_price) as average
                FROM sales 
                WHERE DATE(sale_date) = CURDATE()
            """);
            
            if (rs.next()) {
                addStatistic(statsPanel, "Today's Sales Count", String.valueOf(rs.getInt("count")));
                addStatistic(statsPanel, "Today's Total Revenue", String.format("$%.2f", rs.getDouble("total")));
                addStatistic(statsPanel, "Today's Average Sale", String.format("$%.2f", rs.getDouble("average")));
            }

            // This month's sales
            rs = stmt.executeQuery("""
                SELECT 
                    COUNT(*) as count,
                    SUM(total_price) as total,
                    AVG(total_price) as average
                FROM sales 
                WHERE MONTH(sale_date) = MONTH(CURDATE())
                AND YEAR(sale_date) = YEAR(CURDATE())
            """);
            
            if (rs.next()) {
                addStatistic(statsPanel, "This Month's Sales Count", String.valueOf(rs.getInt("count")));
                addStatistic(statsPanel, "This Month's Revenue", String.format("$%.2f", rs.getDouble("total")));
                addStatistic(statsPanel, "Monthly Average Sale", String.format("$%.2f", rs.getDouble("average")));
            }

            // Top selling products
            rs = stmt.executeQuery("""
                SELECT 
                    p.name,
                    SUM(s.quantity) as total_qty,
                    SUM(s.total_price) as total_revenue
                FROM sales s
                JOIN products p ON s.product_id = p.id
                GROUP BY p.id
                ORDER BY total_qty DESC
                LIMIT 3
            """);
            
            JPanel topProductsPanel = new JPanel(new GridLayout(0, 1));
            topProductsPanel.setBorder(BorderFactory.createTitledBorder("Top Selling Products"));
            
            while (rs.next()) {
                String productInfo = String.format("%s (Qty: %d, Revenue: $%.2f)",
                    rs.getString("name"),
                    rs.getInt("total_qty"),
                    rs.getDouble("total_revenue"));
                topProductsPanel.add(new JLabel(productInfo));
            }
            
            statsPanel.add(topProductsPanel);
            
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        centerPanel.add(chartPanel);
        centerPanel.add(statsPanel);

        // Add all panels to main sales panel
        salesPanel.add(topPanel, BorderLayout.NORTH);
        salesPanel.add(centerPanel, BorderLayout.CENTER);

        // Add button listeners
        newSaleButton.addActionListener(e -> showNewSaleDialog());
        backButton.addActionListener(e -> showMainMenu());

        updateMainPanel(salesPanel);
    }

    private void showNewSaleDialog() {
        JDialog dialog = new JDialog(mainFrame, "Record New Sale", true);
        dialog.setSize(500, 400);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Product selection
        JComboBox<String> productCombo = new JComboBox<>();
        Map<String, Integer> productIds = new HashMap<>();
        Map<String, Double> productPrices = new HashMap<>();
        
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, price FROM products WHERE quantity > 0");
            
            while (rs.next()) {
                String productName = rs.getString("name");
                productCombo.addItem(productName);
                productIds.put(productName, rs.getInt("id"));
                productPrices.put(productName, rs.getDouble("price"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Customer selection
        JComboBox<String> customerCombo = new JComboBox<>();
        Map<String, Integer> customerIds = new HashMap<>();
        
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name FROM customers");
            
            while (rs.next()) {
                String customerName = rs.getString("name");
                customerCombo.addItem(customerName);
                customerIds.put(customerName, rs.getInt("id"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JLabel totalLabel = new JLabel("Total: $0.00");
        
        // Calculate total when quantity changes
        quantitySpinner.addChangeListener(e -> {
            String selectedProduct = (String) productCombo.getSelectedItem();
            int quantity = (Integer) quantitySpinner.getValue();
            double price = productPrices.getOrDefault(selectedProduct, 0.0);
            totalLabel.setText(String.format("Total: $%.2f", quantity * price));
        });
        
        // Add components to panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Product:"), gbc);
        gbc.gridx = 1;
        panel.add(productCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Customer:"), gbc);
        gbc.gridx = 1;
        panel.add(customerCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        panel.add(quantitySpinner, gbc);
        
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(totalLabel, gbc);
        
        JButton saveButton = new JButton("Record Sale");
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(saveButton, gbc);
        
        saveButton.addActionListener(e -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                
                // Start transaction
                conn.setAutoCommit(false);
                
                try {
                    String selectedProduct = (String) productCombo.getSelectedItem();
                    String selectedCustomer = (String) customerCombo.getSelectedItem();
                    int quantity = (Integer) quantitySpinner.getValue();
                    double price = productPrices.get(selectedProduct);
                    double total = quantity * price;
                    
                    // Insert sale record
                    String saleSql = "INSERT INTO sales (product_id, customer_id, quantity, total_price) VALUES (?, ?, ?, ?)";
                    PreparedStatement salePstmt = conn.prepareStatement(saleSql);
                    salePstmt.setInt(1, productIds.get(selectedProduct));
                    salePstmt.setInt(2, customerIds.get(selectedCustomer));
                    salePstmt.setInt(3, quantity);
                    salePstmt.setDouble(4, total);
                    salePstmt.executeUpdate();
                    
                    // Update product quantity
                    String updateSql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setInt(1, quantity);
                    updatePstmt.setInt(2, productIds.get(selectedProduct));
                    updatePstmt.executeUpdate();
                    
                    conn.commit();
                    dialog.dispose();
                    showSalesDashboard(); // Refresh dashboard
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
                
                conn.close();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error recording sale: " + ex.getMessage());
            }
        });
        
        dialog.add(panel);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private void addStatistic(JPanel panel, String label, String value) {
        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statPanel.add(new JLabel(label + ": "));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD));
        statPanel.add(valueLabel);
        panel.add(statPanel);
    }

    // Reports Page
    private void showReportsPage() {
    	 JPanel reportsPanel = new JPanel(new BorderLayout());
         reportsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
         
         mainFrame.setTitle("GENERATE REPORT");
         
         JLabel label = new JLabel("Coming Soon!!!");
         label.setHorizontalAlignment(JLabel.CENTER);
         label.setVerticalAlignment(JLabel.CENTER);
         
         reportsPanel.add(label);
         
         updateMainPanel(reportsPanel);
        
        JPanel buttonPanel = new JPanel(new GridLayout());

        JButton backButton = new JButton("Back to Menu");

        backButton.addActionListener(e -> showMainMenu());

        buttonPanel.add(backButton);
        
        reportsPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        updateMainPanel(reportsPanel);
    }

    // Utility method to update main panel
    private void updateMainPanel(JPanel newPanel) {
        mainFrame.getContentPane().removeAll();
        mainFrame.add(newPanel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    // Edit Customer Dialog
    private void showEditCustomerDialog(int customerId) {
        JDialog dialog = new JDialog(mainFrame, "Edit Customer", true);
        dialog.setSize(400, 300);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextArea addressArea = new JTextArea(3, 20);
        
        // Load current customer data
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "SELECT * FROM customers WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                emailField.setText(rs.getString("email"));
                phoneField.setText(rs.getString("phone"));
                addressArea.setText(rs.getString("address"));
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(addressArea), gbc);
        
        JButton updateButton = new JButton("Update");
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(updateButton, gbc);
        
        updateButton.addActionListener(e -> {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "UPDATE customers SET name=?, email=?, phone=?, address=? WHERE id=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, emailField.getText());
                pstmt.setString(3, phoneField.getText());
                pstmt.setString(4, addressArea.getText());
                pstmt.setInt(5, customerId);
                pstmt.executeUpdate();
                conn.close();
                
                dialog.dispose();
                showCustomersPage(); // Refresh the customers page
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating customer: " + ex.getMessage());
            }
        });
        
        dialog.add(panel);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    // Delete Customer
    private void deleteCustomer(int customerId) {
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Are you sure you want to delete this customer?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String sql = "DELETE FROM customers WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, customerId);
                pstmt.executeUpdate();
                conn.close();
                
                JOptionPane.showMessageDialog(mainFrame, "Customer deleted successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(mainFrame, "Error deleting customer: " + e.getMessage());
            }
        }
    }
}