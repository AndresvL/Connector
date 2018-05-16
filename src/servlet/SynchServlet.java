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
import java.util.Calendar;
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
import controller.topdesk.TopdeskHandler;
import controller.twinfield.SoapHandler;
import controller.twinfield.TwinfieldHandler;
import controller.visma.VismaHandler;
import controller.wefact.WeFactHandler;
import object.Settings;
import object.Token;
import object.workorder.WorkOrder;

import org.apache.http.client.HttpResponseException;

public class SynchServlet extends HttpServlet {
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
			int twinfieldCount = 0, wefactCount = 0, eaccountingCount = 0, moloniCount = 0, drivefxCount = 0,
					snelstartCount = 0, sageoneCount = 0, bouwsoftCount = 0, offectiveCount = 0, teamleaederCount = 0,
					trackjackCount = 0, topdeskCount = 0, vismaCount = 0;
			
			for (Token t : allTokens) {
				switch (t.getSoftwareName()) {
				case "Twinfield":
					twinfieldCount++;
					break;
				case "WeFact":
					wefactCount++;
					break;
				case "eAccounting":
					eaccountingCount++;
					break;
				case "Moloni":
					moloniCount++;
					break;
				case "DriveFx":
					drivefxCount++;
					break;
				case "SageOne":
					sageoneCount++;
					break;
				case "SnelStart_Online":
					snelstartCount++;
					break;
				case "Bouwsoft":
					bouwsoftCount++;
					break;
				case "Teamleader":
					teamleaederCount++;
					break;
				case "TrackJack":
					trackjackCount++;
					break;
				case "TopDesk":
					topdeskCount++;
					break;
				case "Visma":
					vismaCount++;
					break;
				}
			}
			System.out.println(twinfieldCount + " Twinfield users");
			System.out.println(wefactCount + " WeFact users");
			System.out.println(eaccountingCount + " eAccounting users");
			System.out.println(moloniCount + " Moloni users");
			System.out.println(drivefxCount + " DriveFx users");
			System.out.println(sageoneCount + " SageOne users");
			System.out.println(snelstartCount + " SnelStart_Online users");
			System.out.println(bouwsoftCount + " Bouwsoft users");
			System.out.println(teamleaederCount + " Teamleader users");
			System.out.println(trackjackCount + " TrackJack users");
			System.out.println(topdeskCount + " TopDesk users");
			System.out.println(vismaCount + " Visma users");
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
		case "Twinfield":
			String session = null;
			String cluster = null;
			String tempSession = null;
			String tempCluster = null;
			if (req != null) {
				tempSession = (String) req.getSession().getAttribute("session");
				tempCluster = (String) req.getSession().getAttribute("cluster");
			}
			String[] array = SoapHandler.getSession(t, tempSession, tempCluster);
			session = array[0];
			cluster = array[1];
			
			new TwinfieldThread(t, date, session, cluster).start();
			DBConnection.createDatabaseConnection(false, "TwinfieldThread");
			break;
		case "WeFact":
			new WeFactThread(t.getSoftwareToken(), t.getAccessToken(), date).start();
			DBConnection.createDatabaseConnection(false, "WeFactThread");
			break;
		case "eAccounting":
			new eAccountingThread(t, date).start();
			DBConnection.createDatabaseConnection(false, "eAccountingThread");
			break;
		case "Moloni":
			new MoloniThread(t, date).start();
			DBConnection.createDatabaseConnection(false, "MoloniThread");
			break;
		case "DriveFx":
			new DriveFxThread(t, date).start();
			System.out.println("DATE " + date);
			DBConnection.createDatabaseConnection(false, "DriveFxThread");
			break;
		case "SageOne":
			new SageOneThread(t, date).start();
			DBConnection.createDatabaseConnection(false, "SageOneThread");
			break;
		case "SnelStart_Online":
			new SnelStartThread(t, date).start();
			DBConnection.createDatabaseConnection(false, "SnelStartThread");
			break;
		case "Bouwsoft":
			new BouwsoftThread(t, date, req, resp).start();
			DBConnection.createDatabaseConnection(false, "BouwsoftThread");
			break;
		case "Teamleader":
			new TeamleaderThread(t, date, req, resp).start();
			DBConnection.createDatabaseConnection(false, "TeamleaderThread");
			break;
		case "Visma":
			new VismaThread(t, date).start();
			DBConnection.createDatabaseConnection(false, "TeamleaderThread");
			break;
		case "TopDesk":
			new TopDeskThread(t, date, req, resp).start();
			DBConnection.createDatabaseConnection(false, "TopDeskThread");
		default:
			break;
		}
		
	}
	
	public class TwinfieldThread extends Thread {
		private Token t;
		private String date;
		private String session = null, cluster = null;
		private String errorMessage = "", errorDetails = "";
		private String checkUpdate = "false";
		
		TwinfieldThread(Token t, String date, String session, String cluster) {
			this.t = t;
			this.date = date;
			this.session = session;
			this.cluster = cluster;
		}
		
		public void run() {
			try {
				System.out.println("Twinfield Thread Running");
				TwinfieldHandler twinfield = new TwinfieldHandler();
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						// Switch for different import objects
						switch (type) {
						case "employees":
							// Get all Employees from Twinfield and add them
							// to WOA
							// Returns an array with response message for
							// log
							messageArray = twinfield.getEmployees(set.getImportOffice(), session, cluster,
									t.getSoftwareToken(), t.getSoftwareName(), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "projects":
							// Get all Projects from Twinfield and add them
							// to WOA
							// Returns an array with response message for log
							messageArray = twinfield.getProjects(set.getImportOffice(), session, cluster,
									t.getSoftwareToken(), t.getSoftwareName(), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "materials":
							// Get all Materials from Twinfield and add them
							// to WOA
							// Returns an array with response message for log
							messageArray = twinfield.getMaterials(set.getImportOffice(), session, cluster,
									t.getSoftwareToken(), t.getSoftwareName(), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Relations from Twinfield and add them
							// to WOA
							// Returns an array with response message for log
							messageArray = twinfield.getRelations(set.getImportOffice(), session, cluster,
									t.getSoftwareToken(), t.getSoftwareName(), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "hourtypes":
							// Get all Projects from Twinfield and add them
							// to WOA
							// Returns an array with response message for log
							messageArray = twinfield.getHourTypes(set.getImportOffice(), session, cluster,
									t.getSoftwareToken(), t.getSoftwareName(), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						
						}
						
					}
					// Export section
					String[] exportMessageArray = twinfield.setWorkOrders(session, cluster, t.getSoftwareToken(),
							t.getSoftwareName(), set);
					errorMessage += exportMessageArray[0];
					if (exportMessageArray[1] != null) {
						errorDetails = exportMessageArray[1];
					}
					if (checkUpdate.equals("true")) {
						TokenDAO.saveModifiedDate(getDate(null), t.getSoftwareToken());
					}
					if (!errorMessage.equals("")) {
						ObjectDAO.saveLog(errorMessage, errorDetails, t.getSoftwareToken());
					} else {
						ObjectDAO.saveLog("Niks te importeren", errorDetails, t.getSoftwareToken());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Twinfield Thread finished");
		}
	};
	
	public class WeFactThread extends Thread {
		String token, clientToken, date;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		WeFactThread(String token, String clientToken, String date) {
			this.token = token;
			this.clientToken = clientToken;
			this.date = date;
		}
		
		public void run() {
			try {
				System.out.println("WeFact Thread Running");
				WeFactHandler wefact = new WeFactHandler();
				Settings set = ObjectDAO.getSettings(token);
				if (set != null) {
					ObjectDAO.saveProgress(1, token);
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
					Object obj = wefact.checkClientToken(clientToken);
					// Check if object is boolean; if true object is always true
					if (!(obj instanceof Boolean)) {
						errorMessage = obj.toString();
						if (obj.toString().startsWith("De API sleutel")) {
							try {
								TokenDAO.deleteToken(token);
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
						return;
					}
					
					for (String type : importTypes) {
						switch (type) {
						case "materials":
							// Get all Materials from WeFact and add them to WOA
							// Return an array with response message for log
							messageArray = wefact.getMaterials(clientToken, token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Relations from WeFact and add them to WOA
							// Return an array with response message for log
							messageArray = wefact.getRelations(clientToken, token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "hourtypes":
							// Get all Hourtypes from WeFact and add them to WOA
							// Return an array with response message for log
							messageArray = wefact.getHourTypes(clientToken, token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "offertes":
							// Get all Offertes from WeFact and add them as
							// WorkOrder to WOA
							// Return an array with response message for log
							messageArray = wefact.getOffertes(clientToken, token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					if (set.getFactuurType().equals("compleet")) {
						// Export section
						String[] exportMessageArray = null;
						// Type is factuur
						if (set.getExportWerkbontype().equals("factuur")) {
							exportMessageArray = wefact.setFactuur(clientToken, token, set);
							// Type is offerte
						} else {
							exportMessageArray = wefact.setOfferte(clientToken, token, set.getFactuurType(),
									set.getRoundedHours());
						}
						
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
						}
					}
					if (checkUpdate.equals("true")) {
						TokenDAO.saveModifiedDate(getDate(null), token);
					}
					if (!errorMessage.equals("")) {
						ObjectDAO.saveLog(errorMessage, errorDetails, token);
					} else {
						ObjectDAO.saveLog("Niks te importeren", errorDetails, token);
					}
					// Stop syncing
					ObjectDAO.saveProgress(2, token);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					ObjectDAO.saveProgress(2, token);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			System.out.println("WeFact Thread finished");
		}
	};
	
	public class eAccountingThread extends Thread {
		Token t;
		String date;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		eAccountingThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			System.out.println("eAccounting Thread Running");
			EAccountingHandler eaccounting = new EAccountingHandler();
			// Check if accessToken is still valid
			try {
				if (t.getAccessSecret() != null && !eaccounting.checkAccessToken(t.getAccessToken())) {
					// Get accessToken with refreshToken
					t = OAuthEAccounting.getAccessToken(null, t.getAccessSecret(), t.getSoftwareName(),
							t.getSoftwareToken());
				}
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					if (date == null) {
						date = set.getSyncDate();
						if (!date.equals("")) {
							DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
							Date newDate = null;
							try {
								// String to date
								newDate = format.parse(date);
								Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
								date = formatter.format(newDate);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							date = null;
						}
					}
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						case "materials":
							// Get all Materials from eAccounting and add them
							// to WOA
							// Return an array with response message for log
							messageArray = eaccounting.getMaterials(t, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Relations from eAccounting and add them
							// to WOA
							// Return an array with response message for log
							messageArray = eaccounting.getRelations(t, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "projects":
							// Get all Projects from eAccounting and add them to
							// WOA
							// Return an array with response message for log
							messageArray = eaccounting.getProjects(t, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "verkooporders":
							// Get all Verkooporders from eAccounting and add
							// them as WorkOrder to WOA
							// Return an array with response message for log
							messageArray = eaccounting.getOrders(t, date, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					// Export section
					String[] exportMessageArray = null;
					// Type is always factuur
					if (set.getExportWerkbontype().equals("factuur")) {
						exportMessageArray = eaccounting.setFactuur(t, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
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
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("eAccounting Thread Finished");
		}
	};
	
	public class MoloniThread extends Thread {
		Token t;
		String date;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		MoloniThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			System.out.println("Moloni Thread Running");
			MoloniHandler moloni = new MoloniHandler();
			// Check if accessToken is still valid
			try {
				if (t.getAccessSecret() != null && !MoloniHandler.checkAccessToken(t.getAccessToken())) {
					// Get accessToken with refreshToken
					t = OAuthMoloni.getAccessToken(null, t.getAccessSecret(), t.getSoftwareName(),
							t.getSoftwareToken());
				}
				
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					if (date == null) {
						date = set.getSyncDate();
						if (!date.equals("")) {
							DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
							Date newDate = null;
							try {
								// String to date
								newDate = format.parse(date);
								Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
								date = formatter.format(newDate);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							date = null;
						}
					}
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						case "materials":
							messageArray = moloni.getMaterials(t, date, set.getImportOffice(), 0, 0, null);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "hourtypes":
							messageArray = moloni.getHourtypes(t, date, set.getImportOffice(), 0, 0, null);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							messageArray = moloni.getRelations(t, date, set.getImportOffice());
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "employees":
							messageArray = moloni.getEmployees(t, date, set.getImportOffice());
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					// Export section
					String[] exportMessageArray = null;
					// Type is factuur
					if (set.getExportWerkbontype().equals("factuur")) {
						exportMessageArray = moloni.setFactuur(t, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
						}
					}
					if (checkUpdate.equals("true")) {
						TokenDAO.saveModifiedDate(getDate(null), t.getSoftwareToken());
					}
					if (!errorMessage.equals("")) {
						ObjectDAO.saveLog(errorMessage, errorDetails, t.getSoftwareToken());
					} else {
						ObjectDAO.saveLog("Nothing to import", errorDetails, t.getSoftwareToken());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Moloni Thread Finished");
		}
	};
	
	public class DriveFxThread extends Thread {
		Token t;
		String date;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		DriveFxThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			System.out.println("DriveFx Thread Running");
			DriveFxHandler driveFx = new DriveFxHandler();
			// check if accessToken is still valid
			try {
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					if (date == null) {
						date = set.getSyncDate();
						if (!date.equals("")) {
							DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
							Date newDate = null;
							try {
								// String to date
								newDate = format.parse(date);
								Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
								date = formatter.format(newDate);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							date = null;
						}
					}
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						case "materials":
							messageArray = driveFx.getMaterials(t, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							messageArray = driveFx.getRelations(t, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "employees":
							messageArray = driveFx.getEmployees(t, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					// Export section
					String[] exportMessageArray = null;
					// Type is factuur
					if (set.getExportWerkbontype().equals("factuur")) {
						exportMessageArray = driveFx.setFactuur(t, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
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
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("DriveFx Thread Finished");
		}
	};
	
	public class SageOneThread extends Thread {
		Token t;
		String date;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		SageOneThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			System.out.println("SageOne Thread Running");
			SageOneHandler sageone = new SageOneHandler();
			// Check if accessToken is still valid
			Token token = null;
			try {
				if (t.getAccessSecret() != null && !sageone.checkAccessToken(t)) {
					// Get accessToken with refreshToken
					token = OAuthSageOne.getAccessToken(null, t.getAccessSecret(), t.getSoftwareName(),
							t.getSoftwareToken());
				} else {
					token = t;
				}
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					if (date == null) {
						date = set.getSyncDate();
						if (!date.equals("")) {
							DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
							Date newDate = null;
							try {
								// String to date
								newDate = format.parse(date);
								Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
								date = formatter.format(newDate);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							date = null;
						}
					}
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						
						case "materials":
							// Get all Materials from SageOne
							// Return an array with response message for log
							messageArray = sageone.getMaterials(token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "hourtypes":
							// Get all Hourtypes from SageOne
							// Return an array with response message for log
							messageArray = sageone.getHourtypes(token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Hourtypes from SageOne
							// Return an array with response message for log
							messageArray = sageone.getRelations(token, date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						// case "quotes":
						// // Get all Hourtypes from SageOne
						// // Return an array with response message for log
						// messageArray = sageone.getOrders(token, date, set);
						// ErrorMessage += messageArray[0];
						// if (messageArray[1].equals("true")) {
						// checkUpdate = "true";
						// }
						// break;
						}
					}
					// Export section
					String[] exportMessageArray = null;
					// Type is factuur
					if (set.getExportWerkbontype().equals("factuur")) {
						exportMessageArray = sageone.setInvoice(token, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
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
				}
			} catch (
			
			Exception e) {
				e.printStackTrace();
			}
			System.out.println("SageOne Thread Finished");
		}
	};
	
	public class SnelStartThread extends Thread {
		Token t;
		String date;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		SnelStartThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			try {
				System.out.println("SnelStart_Online Thread Running");
				
				SnelStartHandler snelstart = new SnelStartHandler();
				// Check if accessToken is still valid
				
				if (!snelstart.checkAccessToken(t.getAccessToken(), t.getSoftwareToken())) {
					// Get accessToken with koppelingssleutel
					String access = snelstart.getAccessToken(t.getAccessSecret());
					if (access != null) {
						t.setAccessToken(access);
						TokenDAO.saveToken(t);
					} else {
						System.out.println("ERROR INVALID LOGIN");
						// TokenDAO.deleteToken(t.getSoftwareToken());
						return;
					}
					
				}
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					ObjectDAO.saveProgress(1, t.getSoftwareToken());
					if (date == null) {
						date = set.getSyncDate();
						if (!date.equals("")) {
							DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
							Date newDate = null;
							try {
								// String to date
								newDate = format.parse(date);
								Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
								date = formatter.format(newDate);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							date = null;
						}
					}
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						
						case "materials":
							// Get all Materials from SageOne
							// Return an array with response message for log
							messageArray = snelstart.getMaterials(t, date, 0, 0, null);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Hourtypes from SageOne
							// Return an array with response message for log
							messageArray = snelstart.getRelations(t, date, 0, 0, null);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					// Export section
					String[] exportMessageArray = null;
					// Type is factuur
					if (set.getExportWerkbontype().equals("factuur")) {
						exportMessageArray = snelstart.setInvoice(t, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
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
					ObjectDAO.saveProgress(2, t.getSoftwareToken());
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					ObjectDAO.saveProgress(2, t.getSoftwareToken());
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			System.out.println("SnelStart_Online Thread Finished");
		}
	};
	
	public class BouwsoftThread extends Thread {
		String date;
		HttpServletRequest req;
		HttpServletResponse resp;
		Token t;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		BouwsoftThread(Token t, String date, HttpServletRequest req, HttpServletResponse resp) {
			this.t = t;
			this.date = date;
			this.req = req;
			this.resp = resp;
		}
		
		public void run() {
			try {
				System.out.println("Bouwsoft Thread Running");
				BouwsoftHandler bouwsoft = new BouwsoftHandler();
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					ObjectDAO.saveProgress(1, t.getSoftwareToken());
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
						case "employees":
							// Get all Materials from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getEmployees(BouwsoftHandler.checkAccessToken(t), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "categories":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getCategories(BouwsoftHandler.checkAccessToken(t), date, false);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "materials":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getMaterials(BouwsoftHandler.checkAccessToken(t), date, 0, 0, null,
									set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "hourtypes":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getHourtypes(BouwsoftHandler.checkAccessToken(t), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getRelations(BouwsoftHandler.checkAccessToken(t), date, 0, 0, null,
									set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "contacts":
							// Get all ContactPerons from Bouwsoft and add
							// them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getContacts(BouwsoftHandler.checkAccessToken(t), date, 0, 0, 0,
									null);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "projects":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getProjects(BouwsoftHandler.checkAccessToken(t), date, set, 0, 0,
									null);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "assignment":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = bouwsoft.getAssignment(BouwsoftHandler.checkAccessToken(t), date);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						// case "hourtypes":
						// // Get all Hourtypes from WeFact and add them to
						// WOA
						// // Return an array with response message for log
						// messageArray = wefact.getHourTypes(clientToken,
						// token, date);
						// ErrorMessage += messageArray[0];
						// if (messageArray[1].equals("true")) {
						// checkUpdate = "true";
						// }
						// break;
						// case "offertes":
						// // Get all Offertes from WeFact and add them as
						// // WorkOrder to WOA
						// // Return an array with response message for log
						// messageArray = wefact.getOffertes(clientToken,
						// token,
						// date);
						// ErrorMessage += messageArray[0];
						// if (messageArray[1].equals("true")) {
						// checkUpdate = "true";
						// }
						// break;
						}
					}
					// if (set.getFactuurType().equals("compleet")) {
					// // Export section
					// String[] exportMessageArray = null;
					// // Type is factuur
					// if (set.getExportWerkbontype().equals("factuur")) {
					// exportMessageArray = wefact.setFactuur(clientToken,
					// token, set);
					// // Type is offerte
					// } else {
					// exportMessageArray = wefact.setOfferte(clientToken,
					// token, set.getFactuurType(),
					// set.getRoundedHours());
					// }
					//
					// ErrorMessage += exportMessageArray[0];
					// if (exportMessageArray[1] != null) {
					// logDetails = exportMessageArray[1];
					// }
					// }
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
				e.printStackTrace();
				try {
					ObjectDAO.saveProgress(2, t.getSoftwareToken());
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			System.out.println("Bouwsoft Thread finished");
		}
	};
	
	public class TeamleaderThread extends Thread {
		String date;
		HttpServletRequest req;
		HttpServletResponse resp;
		Token t;
		StringBuilder logMessage = new StringBuilder();
		String logDetails = "";
		
		TeamleaderThread(Token t, String date, HttpServletRequest req, HttpServletResponse resp) {
			this.t = t;
			this.date = date;
			this.req = req;
			this.resp = resp;
		}
		
		public void run() {
			try {
				System.out.println("Teamleader Thread Running");
				
				ObjectDAO.saveProgress(1, t.getSoftwareToken());
				
				// Check token
				TeamleaderHandler.handleRequest("helloWorld.php", t, null, String.class);
				
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				
				if (set != null) {
					date = date != null ? Dates.convert(date, Dates.DATE_TIME_INV, Dates.DATE_TIME) : set.getSyncDate();
					
					TeamleaderHandler teamleader = new TeamleaderHandler(t, set, date);
					
					// Cache
					teamleader.cache();
					
					// Import section
					for (String type : set.getImportObjects()) {
						switch (type) {
						case "employees":
							// Get all Materials from Teamleader and add them to
							// WOA
							// Return an array with response message for log
							messageArray = teamleader.getEmployees();
							
							break;
						case "materials":
							// Get all Relations from Teamleader and add them to
							// WOA
							// Return an array with response message for log
							messageArray = teamleader.getMaterials();
							
							break;
						case "hourtypes":
							// Get all Relations from Teamleader and add them to
							// WOA
							// Return an array with response message for log
							messageArray = teamleader.getHourtypes();
							
							break;
						case "relations":
							// Get all Relations from Teamleader and add them to
							// WOA
							// Return an array with response message for log
							messageArray = teamleader.getRelations();
							
							break;
						case "projects":
							// Get all Relations from Teamleader and add them to
							// WOA
							// Return an array with response message for log
							messageArray = teamleader.getProjects();
							
							break;
						case "assignments":
							// Get all Relations from Teamleader and add them to
							// WOA
							// Return an array with response message for log
							messageArray = teamleader.getWorkOrders();
							
							break;
						}
						logMessage.append(messageArray[0]);
					}
					
					// Export section
					if (!"geen".equals(set.getFactuurType())) {
						String[] result = teamleader.setWorkOrders();
						logMessage.append("<br>").append(result[0]);
						if (result[1] != null) {
							logDetails = result[1];
						}
					}
					
					if (messageArray[1].equals("true")) {
						TokenDAO.saveModifiedDate(getDate(null), t.getSoftwareToken());
					}
					
					if (logMessage.length() > 0) {
						ObjectDAO.saveLog(logMessage.toString(), logDetails, t.getSoftwareToken());
					} else {
						ObjectDAO.saveLog("Niks te importeren", logDetails, t.getSoftwareToken());
					}
					
				}
			} catch (HttpResponseException e1) {
				System.out.println((e1.getStatusCode() + " - " + e1.getMessage()));
				try {
					ObjectDAO.saveLog("Something went wrong while synchronising. Click for details<br>",
							e1.getMessage(), t.getSoftwareToken());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				ObjectDAO.saveProgress(2, t.getSoftwareToken());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			System.out.println("Teamleader Thread finished");
		}
	};
	
	public class TopDeskThread extends Thread {
		String date;
		HttpServletRequest req;
		HttpServletResponse resp;
		Token t;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		TopDeskThread(Token t, String date, HttpServletRequest req, HttpServletResponse resp) {
			this.t = t;
			this.date = date;
			this.req = req;
			this.resp = resp;
		}
		
		public void run() {
			try {
				System.out.println("TopDesk Thread Running");
				
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				TopdeskHandler topdesk = new TopdeskHandler(t.getAccessSecret(), t.getAccessToken());
				if (set != null) {
					ObjectDAO.saveProgress(1, t.getSoftwareToken());
					
					ArrayList<String> importTypes = set.getImportObjects();
					// Import section
					for (String type : importTypes) {
						switch (type) {
						case "operators":
							// Get all Operators from TopDesk and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = topdesk.getOperators(t);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						
						case "branches":
							// Get all Persons from TopDesk and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = topdesk.getBranches(t);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "persons":
							// Get all ContactPersons from TopDesk and add
							// them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = topdesk.getPersons(t, 1);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						
						case "calls":
							// Get all Calls from TopDesk and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = topdesk.getIncidents(t);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					
					// output update status in TopDesk
					ArrayList<WorkOrder> orders = WorkOrderHandler.getData(t.getSoftwareToken(), "GetWorkorders",
							"Compleet", true, t.getSoftwareName());
					if (orders != null && orders.size() > 0) {
						
						for (WorkOrder order : orders) {
							topdesk.updateStatus(t, order, "Compleet");
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
					
					String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(Calendar.getInstance().getTime());
					set.setSyncDate(timeStamp.toString());
					ObjectDAO.saveSettings(set, t.getSoftwareToken());
				}
			} catch (Exception e) {
				e.printStackTrace();
				e.printStackTrace();
				try {
					ObjectDAO.saveProgress(2, t.getSoftwareToken());
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			System.out.println("TopDesk Thread finished");
		}
	};
	
	public class VismaThread extends Thread {
		String date;
		HttpServletRequest req;
		HttpServletResponse resp;
		Token t;
		String errorMessage = "", errorDetails = "";
		String checkUpdate = "false";
		
		VismaThread(Token t, String date) {
			this.t = t;
			this.date = date;
		}
		
		public void run() {
			try {
				System.out.println("Visma Thread Running");
				VismaHandler visma = new VismaHandler();
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				if (set != null) {
					ObjectDAO.saveProgress(1, t.getSoftwareToken());
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
						case "employees":
							// Get all Materials from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = visma.getEmployees(t, date, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "materials":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = visma.getMaterials(t, date, 0, 0, null, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "relations":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = visma.getRelations(t, date, 0, 0, null, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "hourtypes":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = visma.getHourtypes(t, date, 0, 0, null, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "projects":
							// Get all Relations from Bouwsoft and add them
							// to
							// WOA
							// Return an array with response message for log
							messageArray = visma.getProjects(t, date, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						case "workorder":
							// Get all Offertes from WeFact and add them as
							// WorkOrder to WOA
							// Return an array with response message for log
							messageArray = visma.getOrders(t, date, set);
							errorMessage += messageArray[0];
							if (messageArray[1].equals("true")) {
								checkUpdate = "true";
							}
							break;
						}
					}
					// Export section
					String[] exportMessageArray = null;
					if (set.getExportWerkbontype().equals("factuur")) {
						exportMessageArray = visma.setFactuur(t, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
						}
					} else if (set.getExportWerkbontype().equals("salesorder")) {
						exportMessageArray = visma.setSalesOrder(t, set, date);
						errorMessage += exportMessageArray[0];
						if (exportMessageArray[1] != null) {
							errorDetails = exportMessageArray[1];
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
				e.printStackTrace();
				try {
					ObjectDAO.saveProgress(2, t.getSoftwareToken());
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			System.out.println("Visma Thread finished");
		}
	};
	
}
