<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="">
<title>TrackJack Connection</title>
<!-- datepicker CSS -->
<link
	href="//cdn.rawgit.com/Eonasdan/bootstrap-datetimepicker/e8bddc60e73c1ec2475f827be36e1957af72e2ea/build/css/bootstrap-datetimepicker.css"
	rel="stylesheet">
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
<link href="css/custom2.css" rel="stylesheet">
<script type="text/javascript"
	src="//code.jquery.com/jquery-2.1.1.min.js"></script>
<script
	src="//cdnjs.cloudflare.com/ajax/libs/moment.js/2.9.0/moment-with-locales.js"></script>
<!-- Bootstrap Core CSS -->
<link href="vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">
<!-- Theme CSS -->
<link href="css/freelancer.min.css" rel="stylesheet">
<!-- Custom Fonts -->
<link href="vendor/font-awesome/css/font-awesome.min.css"
	rel="stylesheet" type="text/css">
<link href="https://fonts.googleapis.com/css?family=Montserrat:400,700"
	rel="stylesheet" type="text/css">
<link
	href="https://fonts.googleapis.com/css?family=Lato:400,700,400italic,700italic"
	rel="stylesheet" type="text/css">
<!-- Sweet Alert -->
<script src="sweetalert2/sweetalert2.min.js" type="text/javascript"></script>
<link rel="stylesheet" href="sweetalert2/sweetalert2.min.css">
<!-- Latest compiled and minified CSS -->
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.12.4/css/bootstrap-select.min.css">

<!-- Latest compiled and minified JavaScript -->
<script
	src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.12.4/js/bootstrap-select.min.js"></script>
<body>
	<div class="affixImg" data-spy="affix">
		<img src="./img/loading.gif" height="60" width="60" id="loading"
			style="display: none;" />
		<h6 id="loadingLabel" style="display: none;">synchronizeren...</h6>
	</div>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" id="WBA_logo" />
		<img src="./img/logo_TrackJack.png" height="70" width="100"
			id="boekhoud_logo" />
	</div>
	<div class="settings">
		<div class="panel-group">
			<div id="loginModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content" id="loginContent">
					<form action="OAuth.do">
						<input type="hidden" value="${softwareToken}" name="token"
							id="softwareToken" />
						<input type="hidden" value="TrackJack" name="softwareName"
							id="softwareName" />
						<input type="hidden" value="${validLogin}" id="validLogin" />
						<table>
							<tr>
								<th>
									<h2>Login</h2>
								</th>
							</tr>
							<tr>
								<td><label id="errorMessage">${errorMessage}</label></td>
							</tr>
							<tr>
								<td><label>Gebruikersnaam</label></td>
							</tr>
							<tr>
								<td><input type="text" class="form-control" id="username"
										name="username" required /></td>
							</tr>
							<tr>
								<td><label>Wachtwoord</label></td>
							</tr>
							<tr>
								<td><input type="password" class="form-control"
										id="password" name="password" required /></td>
							</tr>
						</table>
						<br>
						<input type="submit" id="loginButton" value="Login"
							class="btn btn-success btn-lg" />
					</form>
				</div>
			</div>
			<!-- The Help Modal -->
			<div id="myModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content">
					<div class="modal-header">
						<span class="close">&times;</span>
						<h2>
							Welkom <small>bij de TrackJack koppeling</small>
						</h2>
					</div>
					<div class="modal-body">
						<h4>Data mapping & Filters</h4>
						<table class="table table-hover">
							<thead>
								<tr>
									<th>WerkbonApp</th>
									<th>TrackJack</th>
									<th>Filter</th>
								</tr>
							</thead>
							<tbody>
								<tr>
									<th colspan="2">Import</th>
								</tr>
								<tr>
									<td>Medewerkers</td>
									<td>Werknemers</td>
									<td>lastupdate groter dan synchroniseerdatum</td>
								</tr>
							</tbody>
						</table>
						<br>
					</div>
					<div class="modal-footer">
						<p>WorkOrderApp B.V.</p>
					</div>
				</div>
			</div>
			<input type="hidden" value="${saved}" id="saved" name="saved" />
			<form action="settings.do" id="saveTrackJack">
				<div class="panel panel-success">
					<div class="panel-heading" id="import-panel">Import
						instellingen</div>

					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type="hidden" value="${softwareToken}"
									name="softwareToken" />
								<div class='col-sm-7'>
									<label>Synchroniseer datum</label>
									<img src="./img/vraagteken.jpg" height="13" width="13"
										data-toggle="tooltip"
										title="Alle gegevens vanaf deze datum worden gesynchroniseerd" />
									<br>
									<div class="row">
										<div class="form-group">
											<div class="input-group">
												<input type='text' class="form-control" id='datetimepicker1'
													name="syncDate" value="${savedDate}" />
												<span class="input-group-addon"> <i
													class="glyphicon glyphicon-calendar"></i>
												</span>
											</div>
											<hr />
										</div>
									</div>
								</div>
								<div class="[ col-xs-20 col-sm-10 ]">
									<label>Selecteer objecten om te importeren</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Selecteer de objecten die je wilt importeren van TrackJack naar WerkbonApp"
										height="13" width="13" />
									<br> <br>
									<div class="[ form-group ]">
										<input type="checkbox" name="importType"
											id="fancy-checkbox-default" value="locations"
											${"selected" == checkboxes.locations  ? 'checked' : ''}
											autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-default"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span>
												</span>
											</label>
											<label for="fancy-checkbox-default"
												class="[ btn btn-default active ]" data-toggle="tooltip"
												title="De meest recente locaties worden opgehaald">
												Locaties </label>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</form>
		</div>

	</div>
	<!-- this form will be validated after syncbutton is pressed  -->
	<form action="fastSync.do" id="sync">
		<input type="hidden" value="${softwareToken}" name="token" />
		<input type="hidden" value="${softwareName}" name="softwareName" />
	</form>
	<div class="settings">
		<div class="panel panel-success">
			<div class="panel-heading">
				Log
				<img src="./img/refresh_button.png" data-toggle="tooltip"
					title="Click this button to reload page" height="30" width="30"
					id="refresh_knop" />
			</div>
			<div class="panel-body">
				<div class="row control-group">
					<div class="form-group col-xs-12 floating-label controls">
						<table class="table table-hover">
							<thead>
								<tr>
									<th>Tijd</th>
									<th>Bericht</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${logs}" var="log">
									<tr class="showDetails" data-href='${log.details}'>
										<td>${log.timestamp}</td>
										<td>${log.message}</td>
									</tr>
								</c:forEach>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- jQuery -->
	<script src="vendor/jquery/jquery.min.js" type="text/javascript"></script>
	<!--    <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script> -->
	<!-- Bootstrap Core JavaScript -->
	<script src="vendor/bootstrap/js/bootstrap.min.js"
		type="text/javascript"></script>
	<!-- Plugin JavaScript -->
	<script
		src="https://cdnjs.cloudflare.com/ajax/libs/jquery-easing/1.3/jquery.easing.min.js"
		type="text/javascript"></script>
	<!-- Contact Form JavaScript -->
	<script src="js/jqBootstrapValidation.js" type="text/javascript"></script>
	<script src="js/contact_me.js" type="text/javascript"></script>
	<!-- Theme JavaScript -->

	<script src="js/freelancer.min.js" type="text/javascript"></script>
	<script type="text/javascript" src="js/vkbeautify.js"></script>
	<script type="text/javascript" src="js/bootstrap-datetimepicker.js"></script>
	<script type="text/javascript" src="js/trackjack.js"></script>
</body>
</html>
