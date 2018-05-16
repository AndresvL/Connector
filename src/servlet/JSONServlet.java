package servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import DAO.ObjectDAO;

public class JSONServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static int progress;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String softwareToken = (String) request.getSession().getAttribute("softwareToken");
		int synchronizing = 0;
		try {
			synchronizing = ObjectDAO.getProgress(softwareToken);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(String.valueOf(synchronizing));
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String softwareToken = (String) request.getSession().getAttribute("softwareToken");
		int status = Integer.parseInt(request.getParameter("status"));
		try {
			ObjectDAO.saveProgress(status, softwareToken);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(String.valueOf(status));
		
	}
}
