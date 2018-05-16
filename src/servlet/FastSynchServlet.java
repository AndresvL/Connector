package servlet;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import DBUtil.DBConnection;
import controller.WorkOrderHandler;
import controller.bouwsoft.BouwsoftHandler;
import controller.drivefx.DriveFxHandler;
import controller.eaccouting.EAccountingHandler;
import controller.eaccouting.OAuthEAccounting;
import controller.moloni.MoloniHandler;
import controller.moloni.OAuthMoloni;
import controller.sageone.OAuthSageOne;
import controller.sageone.SageOneHandler;
import controller.snelstart.SnelStartHandler;
import controller.teamleader.TeamleaderHandler;
import controller.teamleader.util.Dates;
import controller.trackjack.TrackJackHandler;
import controller.twinfield.SoapHandler;
import controller.twinfield.TwinfieldHandler;
import controller.wefact.WeFactHandler;
import object.Settings;
import object.Token;
import org.apache.http.client.HttpResponseException;

public class FastSynchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String redirect = System.getenv("REDIRECT");
	private String[] messageArray = null;
	
	/**
	 * This method is called after the user or the system triggers the sync
	 * method, if softwareToken parameter is null the credentials from the
	 * database will be used for sychronisation
	 * 
	 * @param req
	 *            HttpServletRequest with request properties
	 * @param resp
	 *            HttpServletResponse with response properties
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		String softwareToken = req.getParameter("token");
		String softwareName = req.getParameter("softwareName");
		ArrayList<Token> allTokens = null;
		// SoftwareToken != null if user is online
		if (softwareToken != null) {
			try {
				Token t = TokenDAO.getToken(softwareToken, softwareName);
				if (t != null && !t.getAccessSecret().equals("invalid")) {
					softwareName = t.getSoftwareName();
					// setSyncMethods(true) because user is online
					this.setSyncMethods(t, req, resp, true);
					if (redirect != null) {
						resp.sendRedirect(
								redirect + "OAuth.do?token=" + softwareToken + "&softwareName=" + softwareName);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Get all users from database to sync their data all at once
		} else {
			try {
				allTokens = TokenDAO.getSoftwareTokens();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			String token = null;
			for (Token t : allTokens) {
				softwareName = t.getSoftwareName();
				token = t.getSoftwareToken();
				// Check if token is valid
				if (WorkOrderHandler.checkWorkOrderToken(token, softwareName) == 200) {
					try {
						setSyncMethods(t, null, null, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			// Print total Users fetched from database
			System.out.println("A total of " + allTokens.size() + " users are found in database");
			int trackjackCount = 0;
			
			for (Token t : allTokens) {
				switch (t.getSoftwareName()) {
				case "TrackJack":
					trackjackCount++;
					break;
				}
			}
			System.out.println(trackjackCount + " TrackJack users");
		}
	}
	
	/**
	 * This method is used to get the current Date
	 * 
	 * @param date
	 *            a Date(yyyy-MM-dd HH:mm:ss) String
	 * @return date with format yyyy-MM-dd HH:mm:ss
	 */
	public String getDate(String date) {
		String timestamp = null;
		ZonedDateTime za = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		if (date != null) {
			timestamp = date;
		} else {
			timestamp = za.format(formatter);
		}
		return timestamp;
	}
	
	/**
	 * This method calls different threads to start the synchronisation process.
	 * For every softwareName a different thread will be started
	 * 
	 * @param t
	 *            Token object of the current user
	 * @param req
	 *            HttpServletRequest
	 * @param loggedIn
	 *            true if user is logged in
	 * @throws Exception
	 *             createDatabaseConnection exception
	 */
	public void setSyncMethods(Token t, HttpServletRequest req, HttpServletResponse resp, boolean loggedIn)
			throws Exception {
		String date = null;
		// Get the date from the last synchronisation if user is not logged in
		if (!loggedIn) {
			date = TokenDAO.getModifiedDate(t.getSoftwareToken());
		}
		switch (t.getSoftwareName()) {
		case "TrackJack":
			new TrackJackThread(t, date).start();
			DBConnection.createDatabaseConnection(false, "TrackJackThread");
			break;
		
		default:
			break;
		}
		
	}
	
	public class TrackJackThread extends Thread {
		String date;
		Token t;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		TrackJackThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			try {
				System.out.println("TrackJack Thread Running");
				ObjectDAO.saveProgress(1, t.getSoftwareToken());
				TrackJackHandler trackjack = new TrackJackHandler();
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					if (date == null) {
						date = set.getSyncDate();
						DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
						Date newDate = null;
						try {
							// String to date
							newDate = format.parse(date);
							Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							date = formatter.format(newDate);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						case "locations":
							// Get all lastLocations from TrackJack and add them
							// to WOA
							// Return an array with response message for log
							messageArray = trackjack.getLocations(t);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					if (checkUpdate.equals("true")) {
						TokenDAO.saveModifiedDate(getDate(null), t.getSoftwareToken());
					}
					if (!errorMessage.equals("")) {
						ObjectDAO.saveLog(errorMessage, errorDetails, t.getSoftwareToken());
					} else {
						ObjectDAO.saveLog("Niks te importeren", errorDetails, t.getSoftwareToken());
					}
					// Stop syncing
					ObjectDAO.saveProgress(2, t.getSoftwareToken());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Bouwsoft Thread finished");
		}
	};
}