package servlet;

import controller.WorkOrderHandler;
import controller.bouwsoft.OAuthBouwsoft;
import controller.drivefx.OAuthDriveFx;
import controller.eaccouting.OAuthEAccounting;
import controller.moloni.OAuthMoloni;
import controller.offective.OAuthOffective;
import controller.sageone.OAuthSageOne;
import controller.snelstart.OAuthSnelStart;
import controller.teamleader.OAuthTeamleader;
import controller.topdesk.OAuthTopdesk;
import controller.trackjack.OAuthTrackJack;
import controller.twinfield.OAuthTwinfield;
import controller.visma.OAuthVisma;
import controller.wefact.OAuthWeFact;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OAuthServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String softwareToken = req.getParameter("token");
		String softwareName = req.getParameter("softwareName");
		// Get checkSaved from session every time oAuth.do is called
		String checkSaved = (String) req.getSession().getAttribute("checkSaved");
		if (checkSaved != null) {
			req.getSession().setAttribute("saved", checkSaved);
			req.getSession().setAttribute("checkSaved", null);
		} else {
			req.getSession().setAttribute("saved", "");
		}
		
		RequestDispatcher rd = null;
		// Set session with softwareName and token
		int code = WorkOrderHandler.checkWorkOrderToken(softwareToken, softwareName);
		// WOA returns 200 if request is successfull
		System.out.println("WOACode " + code);
		if (code == 200) {
			req.getSession().setAttribute("softwareToken", softwareToken);
			req.getSession().setAttribute("softwareName", softwareName);
			req.getSession().setAttribute("checkboxes", null);
			req.getSession().setAttribute("exportCheckboxes", null);
			req.getSession().setAttribute("logs", null);
			req.getSession().setAttribute("error", null);
			
			switch (softwareName) {
			case "Twinfield":
				OAuthTwinfield oauth = new OAuthTwinfield();
				try {
					oauth.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				req.getSession().setAttribute("offices", null);
				req.getSession().setAttribute("users", null);
				break;
			case "WeFact":
				OAuthWeFact oauth2 = new OAuthWeFact();
				try {
					oauth2.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "eAccounting":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthEAccounting oauth3 = new OAuthEAccounting();
				try {
					oauth3.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "Moloni":
				req.getSession().setAttribute("offices", null);
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthMoloni oauth4 = new OAuthMoloni();
				try {
					oauth4.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "DriveFx":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthDriveFx oauth5 = new OAuthDriveFx();
				try {
					oauth5.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "SageOne":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthSageOne oauth6 = new OAuthSageOne();
				try {
					oauth6.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "SnelStart_Online":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthSnelStart oauth7 = new OAuthSnelStart();
				try {
					oauth7.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "Bouwsoft":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				req.getSession().setAttribute("materialGroups", null);
				OAuthBouwsoft oauth8 = new OAuthBouwsoft();
				try {
					oauth8.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "Offective":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthOffective oauth9 = new OAuthOffective();
				try {
					oauth9.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "Teamleader":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				OAuthTeamleader oauth10 = new OAuthTeamleader();
				try {
					oauth10.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "TrackJack":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				req.getSession().setAttribute("materialGroups", null);
				req.getSession().setAttribute("errorMessage", null);
				OAuthTrackJack oauth11 = new OAuthTrackJack();
				try {
					oauth11.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				break;
			case "Visma":
				// typeofwork
				req.getSession().setAttribute("types", null);
				// paymentmethod
				req.getSession().setAttribute("paymentmethod", null);
				req.getSession().setAttribute("materialGroups", null);
				OAuthVisma oauth12 = new OAuthVisma();
				try {
					oauth12.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "TopDesk":
				OAuthTopdesk oauth13 = new OAuthTopdesk();
				try {
					oauth13.authenticate(softwareToken, req, resp);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				break;
			default:
				req.getSession().setAttribute("errorMessage",
						"Error: softwareName " + softwareName + " can not be found");
				break;
			}
			
		} else {
			switch (softwareName) {
			case "Twinfield":
				rd = req.getRequestDispatcher("twinfield.jsp");
				req.getSession().setAttribute("session", null);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("error", "Token is invalid");
				break;
			case "WeFact":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("weFact.jsp");
				break;
			case "eAccounting":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("eAccounting.jsp");
				break;
			case "Moloni":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("moloni.jsp");
				break;
			case "DriveFx":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("driveFx.jsp");
				break;
			case "SageOne":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("sageOne.jsp");
				break;
			case "SnelStart_Online":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("errorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("snelStart.jsp");
				break;
			case "Bouwsoft":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("errorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("bouwsoft.jsp");
				break;
			case "Offective":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("offective.jsp");
				break;
			case "Teamleader":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("ErrorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("teamleader.jsp");
				break;
			case "TrackJack":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("errorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("trackjack.jsp");
				break;
			case "Visma":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("errorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("visma.jsp");
				break;
			case "TopDesk":
				req.getSession().setAttribute("softwareToken", softwareToken);
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("errorMessage", "Error " + code + ": Token is invalid");
				rd = req.getRequestDispatcher("topDesk.jsp");
				break;
			
			default:
				break;
			}
			rd.forward(req, resp);
		}
		
	}
}
