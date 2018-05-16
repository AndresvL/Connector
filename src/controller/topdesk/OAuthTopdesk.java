package controller.topdesk;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import object.Settings;
import object.Token;
import object.topdesk.OperatorGroup;
import object.topdesk.ProcessingStatus;;

public class OAuthTopdesk extends Authenticate {
	private Token tokenObject;
	TopdeskHandler handler = new TopdeskHandler();
	
	@Override
	public void authenticate(String softwareToken, HttpServletRequest req, HttpServletResponse resp)
			throws ClientProtocolException, IOException, ServletException, SQLException {
		RequestDispatcher rd = null;
		String softwareName = (String) req.getSession().getAttribute("softwareName");
		
		Token dbToken = null;
		
		// Get client token from db
		try {
			dbToken = TokenDAO.getToken(softwareToken, softwareName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (dbToken == null) {
			// If required parameters aren't set
			if (!req.getParameterMap().containsKey("clientToken") || !req.getParameterMap().containsKey("operatorName")
					|| !req.getParameterMap().containsKey("clientDomain")) {
				rd = req.getRequestDispatcher("topDesk.jsp");
				req.getSession().setAttribute("clientToken", null);
				req.getSession().setAttribute("operatorName", null);
				req.getSession().setAttribute("clientDomain", null);
			} else {
				// clientToken exists
				String operatorName = req.getParameter("operatorName");
				String clientToken = req.getParameter("clientToken");
				String clientDomain = (String) req.getParameter("clientDomain");
				boolean isValidToken = handler.checkCredentials(operatorName, clientToken, clientDomain);
				// Check if object is boolean; if true object is always true
				if (isValidToken) {
					tokenObject = new Token();
					tokenObject.setAccessToken(clientToken);
					tokenObject.setAccessSecret(operatorName);
					tokenObject.setSoftwareName(softwareName);
					tokenObject.setSoftwareToken(softwareToken);
					tokenObject.setConsumerSecret(clientDomain);
					
					// Save token to database
					try {
						TokenDAO.saveToken(tokenObject);
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					req = processingStatusToSession(req);
					req = operatorGroupsToSession(req);
					
					rd = req.getRequestDispatcher("topDesk.jsp");
					req.getSession().setAttribute("clientToken", clientToken);
					System.out.println("Session clientToken " + clientToken);
					req.getSession().setAttribute("errorMessage", "true");
				} else {
					// Login page
					rd = req.getRequestDispatcher("topDesk.jsp");
					req.getSession().setAttribute("errorMessage",
							"<br> <span style='color:red;'>Gegevens incorrect. </span><br> Open de documentatie voor info over het instellen van de koppeling");
					req.getSession().setAttribute("clientToken", null);
					req.getSession().setAttribute("operatorName", null);
					req.getSession().setAttribute("clientDomain", null);
				}
			}
		} else {
			tokenObject = dbToken;
			boolean isValidToken = handler.checkCredentials(tokenObject.getAccessSecret(), tokenObject.getAccessToken(),
					tokenObject.getConsumerSecret());
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (!allLogs.isEmpty() || allLogs != null) {
				req.getSession().setAttribute("logs", allLogs);
			}
			if (isValidToken) {
				req.getSession().setAttribute("errorMessage", "ok");
				Settings set = ObjectDAO.getSettings(softwareToken);
				if (set != null) {
					req.getSession().setAttribute("savedDate", set.getSyncDate());
					req.getSession().setAttribute("savedStatus", set.getImportOffice());
					req.getSession().setAttribute("savedOperators", set.getUser());
					
					ArrayList<String> importTypes = set.getImportObjects();
					req.getSession().setAttribute("importTypes", importTypes);
					
					String syncStatusSaved = set.getExportWerkbontype();
					req.getSession().setAttribute("syncStatusSaved", syncStatusSaved);
					
					String completeStatusSaved = set.getExportOffice();
					req.getSession().setAttribute("completeStatusSaved", completeStatusSaved);
					
				}
				req = processingStatusToSession(req);
				req = operatorGroupsToSession(req);
				if (set != null && set.getSyncDate() != null) {
					req.getSession().setAttribute("savedDate", set.getSyncDate());
				} else {
					req.getSession().setAttribute("currDate", getDate(null));
				}
				rd = req.getRequestDispatcher("topDesk.jsp");
				req.getSession().setAttribute("clientToken", tokenObject.getAccessToken());
			}
		}
		rd.forward(req, resp);
	}
	
	public HttpServletRequest operatorGroupsToSession(HttpServletRequest req) throws IOException {
		JSONArray operatorGroups = null;
		
		operatorGroups = handler.getOperatorGroups(tokenObject);
		ArrayList<OperatorGroup> operatorGroupList = new ArrayList<OperatorGroup>();
		
		if (operatorGroups != null && operatorGroups.length() > 0) {
			for (int i = 0; i < operatorGroups.length(); i++) {
				try {
					JSONObject operatorGroup = operatorGroups.getJSONObject(i);
					operatorGroupList.add(
							new OperatorGroup(operatorGroup.getString("id"), operatorGroup.getString("groupName")));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		if (operatorGroupList.size() > 0) {
			req.getSession().setAttribute("operatorGroups", operatorGroupList);
		}
		
		return req;
	}
	
	public HttpServletRequest processingStatusToSession(HttpServletRequest req) throws IOException {
		JSONArray statusList = null;
		try {
			statusList = handler.getProcessingStatus(tokenObject.getAccessSecret(), tokenObject.getAccessToken());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ArrayList<ProcessingStatus> processingStatus = new ArrayList<ProcessingStatus>();
		
		for (int i = 0; i < statusList.length(); i++) {
			try {
				JSONObject status = statusList.getJSONObject(i);
				processingStatus.add(new ProcessingStatus(status.getString("id"), status.getString("name")));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		req.getSession().setAttribute("processingStatus", processingStatus);
		return req;
	}
	
	public String getDate(String date) {
		String timestamp = null;
		ZonedDateTime za = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		if (date != null) {
			timestamp = date;
		} else {
			timestamp = za.format(formatter);
		}
		return timestamp;
	}
	
}