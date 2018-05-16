package DBUtil;

import javax.servlet.ServletContextListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
	private static String password, user, url, host, port, database;
	
	public static Connection createDatabaseConnection(Boolean b, String location) {
		Connection con = null;
		// check if app runs locally or online
		if (System.getenv("MYSQL_SERVICE_HOST") == null) {
			localCon();
		} else {
			// System.out.println("online");
			onlineCon();
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			if (b) {
				// System.out.println("new connection " + location);
				con = DriverManager.getConnection(url, user, password);
			}
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return con;
	}
	
	public static void localCon() {
		url = "jdbc:mysql://localhost/Twinfield";
		user = "root";
		password = "root";
	}
	
	public static void onlineCon() {
		host = System.getenv("MYSQL_SERVICE_HOST");
		port = System.getenv("MYSQL_SERVICE_PORT");
		user = System.getenv("MYSQL_USER");
		password = System.getenv("MYSQL_PASSWORD");
		database = System.getenv("MYSQL_DATABASE");
		url = "jdbc:mysql://" + host + ":" + port + "/" + database;
	}
}
