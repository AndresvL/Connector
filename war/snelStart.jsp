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
<title>SnelStart Online Connection</title>
<!-- datepicker CSS -->
<link
	href="//cdn.rawgit.com/Eonasdan/bootstrap-datetimepicker/e8bddc60e73c1ec2475f827be36e1957af72e2ea/build/css/bootstrap-datetimepicker.css"
	rel="stylesheet">
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
<link href="css/loading.css" rel="stylesheet">
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
<body>
	<div class="affixImg" data-spy="affix">
		<img src="./img/loading.gif" height="60" width="60" id="loading" style="display: none;" />
		<h6 id="loadingLabel" style="display: none;">synchronizeren...</h6>
	</div>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" />
		<img src="./img/logo_SnelStart.png" height=33 width="200"
			id="boekhoud_logo" />
	</div>
	<div class="settings">
		<div class="panel-group">
			<!-- The login Modal -->
			<div id="loginModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content" id="loginContent">
					<form action="OAuth.do">
						<input type="hidden" value="${softwareToken}" name="token"
							id="softwareToken" />
						<input type="hidden" value="SnelStart_Online" name="softwareName"
							id="softwareName" />
						<input type="hidden" value="${clientToken}" id="client" />
						<table>
							<tr>
								<th>
									<h2>Authentication</h2>
								</th>
							</tr>
							<tr>
								<td><label>${errorMessage}</label></td>
							</tr>
							<tr>
								<td><label>Koppeling Key SnelStart Online</label> <img
										src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Login bij SnelStart Online en navigeer naar Koppelingen - WerkbonApp en genereer een sleutel"
										height="13" width="13" /></td>
							</tr>
							<tr>
								<td><input type="text" class="form-control"
										id="clientToken"
										placeholder="5a5a5fbbcecdd585aa62812119d0721e"
										name="clientToken" pattern=".{3,}" required
										title="De koppelingssleutel kan niet zo kort zijn" /></td>
							</tr>
						</table>
						<br>
						<input type="submit" id="loginButton" value="Submit"
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
							Welcome <small>at the SnelStart Online integration</small>
						</h2>
					</div>
					<div class="modal-body">

						<button type="button" id="show" class="btn btn-info">Show</button>
						<h4>Data Mapping</h4>
						<div id="mappingTable" style="display: none;">
							<table class="table table-hover">
								<thead>
									<tr>
										<th>WerkbonApp</th>
										<th>SnelStart Online</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<th colspan="2">Import</th>
									</tr>
									<tr>
										<td>Materialen</td>
										<td>Artikelen</td>
									</tr>
									<tr>
										<td>Relaties</td>
										<td>Relaties</td>
									</tr>
									<tr>
										<th colspan="2">Export</th>
									</tr>
									<tr>
										<td>Werkbonnen met status naar SnelStart</td>
										<td>Concepten(orders)</td>
									</tr>
								</tbody>
							</table>
						</div>
						<br>
					</div>
					<div class="modal-footer">
						<p>WorkOrderApp B.V.</p>
					</div>
				</div>
			</div>
			<input type="hidden" value="${errorMessage}" id="error" />
			<input type="hidden" value="${saved}" id="saved" name="saved" />
			<form action="settings.do" id="saveSnelStart">
				<div class="panel panel-success">
					<div class="panel-heading">Import Settings</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type="hidden" value="${softwareToken}"
									name="softwareToken" />
								<label>Select objects for import</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Select the objects you want to import from SnelStart Online into WorkOrderApp"
									height="13" width="13" />
								<div class="checkbox">
									<label>
										<input type="checkbox" value="materials"
											${"selected" == checkboxes.materials  ? 'checked' : ''}
											name="importType">
										Materials
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" id="relations" value="relations"
											${"selected" == checkboxes.relations  ? 'checked' : ''}
											name="importType">
										Relations
									</label>
								</div>
								<br>
								<div class="row">
									<div class='col-sm-7'>
										<div class="form-group">
											<label>Synchronisation date</label>
											<img src="./img/vraagteken.jpg" height="13" width="13"
												data-toggle="tooltip"
												title="All data will be imported starting from this date" />
											<input type='text' class="form-control" id='datetimepicker1'
												name="syncDate" value="${savedDate}" required />

										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="panel panel-success">
					<div class="panel-heading">Export Settings</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<label>Werkbonstatus</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="De werkbonnen met status compleet worden opgehaald"
									height="13" width="13" />
								<select name="factuurType" class="form-control" id="status"
									required>
									<option selected value="compleet"
										${"compleet" == factuur ? 'selected="selected"' : ''}>Compleet</option>
									<option value="error"
										${"error" == factuur ? 'selected="selected"' : ''}>Error</option>
									<option value="geen"
										${"geen" == factuur ? 'selected="selected"' : ''}>Geen</option>
								</select> <br>

								<label>Workorder options</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Select the objects you want to create in SnelStart Online"
									height="13" width="13" />
								<div class="checkbox">
									<label>
										<input type="checkbox" value="materials"
											${"selected" == exportCheckboxes.materials  ? 'checked' : ''}
											name="exportType">
										New materials
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" class="exportRelation"
											value="relations"
											${"selected" == exportCheckboxes.relations  ? 'checked' : ''}
											name="exportType">
										New relations
									</label>
								</div>
								<br>
								<label>Export workorder as</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="A workorder will be exporterd as an invoice" height="13"
									width="13" />
								<br>
								<input type="radio" name="exportWerkbon" value="factuur" checked>
								Concepten (orders)<br> <br>
								<label>Rounded hours</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="All worked hours will be rounded to the selected amount of minutes"
									height="13" width="13" />
								<br> <select name="roundedHours" class="form-control"
									id="uren" required>
									<option selected value="1"
										${"1" == roundedHours ? 'selected="selected"' : ''}>No
										rounding</option>
									<option value="5"
										${"5" == roundedHours ? 'selected="selected"' : ''}>5
										minutes</option>
									<option value="15"
										${"15" == roundedHours ? 'selected="selected"' : ''}>15
										minutes</option>
									<option value="30"
										${"30" == roundedHours ? 'selected="selected"' : ''}>30
										minutes</option>
								</select> <br>
								<label>Universal hourtype</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="This articlenumber will be used to book unknown hourtpes from WorkorderApp"
									height="13" width="13" />
								<br>
								<input type="number" class="form-control" name="materialCode"
									value="${materialCode}" placeholder="1000" required>
								<br>
							</div>
						</div>
					</div>
					<div class="row">
						<div class="form-group col-xs-12">
							<input type="submit" class="btn btn-success btn-lg" value="Sync"
								id="syncbutton" />
							<button type="button" id="help" class="btn btn-info btn-lg">Help</button>
							<input type="submit" class="btn btn-success btn-lg" value="Save"
								name="category" id="savebutton" />
						</div>
					</div>
				</div>
			</form>
		</div>
	</div>
	<!-- this form will be validated after syncbutton is pressed  -->
	<form action="sync.do" id="sync">
		<input type="hidden" value="${softwareToken}" name="token" />
		<input type="hidden" value="${softwareName}" name="softwareName" />
	</form>
	<div class="settings">
		<div class="panel panel-success">
			<div class="panel-heading">Log</div>
			<div class="panel-body">
				<div class="row control-group">
					<div class="form-group col-xs-12 floating-label controls">
						<table class="table table-hover">
							<thead>
								<tr>
									<th>Time</th>
									<th>Message</th>
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
	<script type="text/javascript" src="js/bootstrap-datetimepicker.js"></script>
	<script src="js/freelancer.min.js" type="text/javascript"></script>
	<script type="text/javascript" src="js/vkbeautify.js"></script>
	<script type="text/javascript" src="js/snelstart.js"></script>
</body>
</html>
