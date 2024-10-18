import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/Admin?useSSL=false&serverTimezone=Europe/Moscow"; // URL к вашей БД
    private static final String DB_USER = "root"; // ваш пользователь
    private static final String DB_PASSWORD = "12345678"; // ваш пароль

    public static void main(String[] args) {
        Main app = new Main();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Меню:");
            System.out.println("1. Регистрация");
            System.out.println("2. Авторизация");
            System.out.println("3. Выход");
            System.out.print("Выберите опцию: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // освобождаем буфер

            switch (choice) {
                case 1:
                    app.registerUser(scanner);
                    break;
                case 2:
                    app.loginUser(scanner);
                    break;
                case 3:
                    System.out.println("Выход из программы.");
                    return;
                default:
                    System.out.println("Неверный выбор! Попробуйте снова.");
            }
        }
    }

    private void createAdmin() {
        // Пытаемся создать администратора, если его еще нет
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Проверяем, существует ли уже администратор
            String checkAdminSql = "SELECT * FROM users WHERE username = 'admin'";
            ResultSet resultSet = connection.prepareStatement(checkAdminSql).executeQuery();
            if (!resultSet.next()) {
                // Администратор не найден, создаем нового
                String hashedPassword = hashPassword("admin_password"); // Замените admin_password на другой пароль, если необходимо
                String insertAdminSql = "INSERT INTO users (username, password, email, phone, role) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(insertAdminSql);
                statement.setString(1, "admin");
                statement.setString(2, hashedPassword);
                statement.setString(3, "admin@example.com");
                statement.setString(4, "1234567890");
                statement.setString(5, "admin");
                statement.executeUpdate();
                System.out.println("Администратор создан с именем 'admin' и паролем 'admin_password'");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void registerUser(Scanner scanner) {
        System.out.print("Введите имя пользователя: ");
        String username = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        System.out.print("Введите email: ");
        String email = scanner.nextLine();
        System.out.print("Введите номер телефона: ");
        String phone = scanner.nextLine();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String hashedPassword = hashPassword(password);
            String sql = "INSERT INTO users (username, password, email, phone, role) VALUES (?, ?, ?, ?, 'user')";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.setString(3, email);
            statement.setString(4, phone);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Регистрация прошла успешно!");
            } else {
                System.out.println("Ошибка регистрации.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void loginUser(Scanner scanner) {
        System.out.println("Выберите способ авторизации:");
        System.out.println("1. Имя пользователя");
        System.out.println("2. Email");
        System.out.println("3. Номер телефона");
        System.out.print("Ваш выбор: ");
        int loginChoice = scanner.nextInt();
        scanner.nextLine(); // освобождаем буфер

        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        String hashedPassword = hashPassword(password);

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql;
            PreparedStatement statement;
            ResultSet resultSet;

            switch (loginChoice) {
                case 1: // авторизация по имени пользователя
                    System.out.print("Введите имя пользователя: ");
                    String username = scanner.nextLine();
                    sql = "SELECT * FROM users WHERE username = ? AND password = ?";
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, username);
                    statement.setString(2, hashedPassword);
                    break;
                case 2: // авторизация по email
                    System.out.print("Введите email: ");
                    String email = scanner.nextLine();
                    sql = "SELECT * FROM users WHERE email = ? AND password = ?";
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, email);
                    statement.setString(2, hashedPassword);
                    break;
                case 3: // авторизация по номеру телефона
                    System.out.print("Введите номер телефона: ");
                    String phone = scanner.nextLine();
                    sql = "SELECT * FROM users WHERE phone = ? AND password = ?";
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, phone);
                    statement.setString(2, hashedPassword);
                    break;
                default:
                    System.out.println("Неверный выбор!");
                    return;
            }

            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                handleUserRole(resultSet, scanner);
            } else {
                System.out.println("Неверное имя пользователя, email или пароль.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleUserRole(ResultSet resultSet, Scanner scanner) throws SQLException {
        String role = resultSet.getString("role");
        String username = resultSet.getString("username");

        if (role.equals("admin")) {
            System.out.println("Добро пожаловать, администратор " + username);
            adminMenu(scanner);
        } else {
            System.out.println("Авторизация успешна! Добро пожаловать, " + username);
        }
    }

    private void adminMenu(Scanner scanner) {
        while (true) {
            System.out.println("Администрирование:");
            System.out.println("1. Изменить данные пользователя");
            System.out.println("2. Выход");
            System.out.print("Ваш выбор: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // освобождаем буфер

            switch (choice) {
                case 1:
                    changeUserData(scanner);
                    break;
                case 2:
                    System.out.println("Выход из администрирования.");
                    return;
                default:
                    System.out.println("Неверный выбор! Попробуйте снова.");
            }
        }
    }

    private void changeUserData(Scanner scanner) {
        System.out.print("Введите имя пользователя для изменения данных: ");
        String username = scanner.nextLine();
        System.out.print("Введите новый email: ");
        String newEmail = scanner.nextLine();
        System.out.print("Введите новый номер телефона: ");
        String newPhone = scanner.nextLine();
        System.out.print("Введите новый пароль: ");
        String newPassword = scanner.nextLine();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String hashedPassword = hashPassword(newPassword);
            String sql = "UPDATE users SET email = ?, phone = ?, password = ? WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, newEmail);
            statement.setString(2, newPhone);
            statement.setString(3, hashedPassword);
            statement.setString(4, username);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Данные пользователя успешно обновлены!");
            } else {
                System.out.println("Ошибка обновления данных. Пользователь не найден.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хеширования пароля", e);
        }
    }
}