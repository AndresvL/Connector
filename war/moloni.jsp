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
<title>Moloni Connection</title>
<!-- datepicker CSS -->
<link
	href="//cdn.rawgit.com/Eonasdan/bootstrap-datetimepicker/e8bddc60e73c1ec2475f827be36e1957af72e2ea/build/css/bootstrap-datetimepicker.css"
	rel="stylesheet">
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
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
<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
		        <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
		        <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
		    <![endif]-->

</head>
<body>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" />
		<img src="./img/moloni.png" height=80 width="120" id="boekhoud_logo" />
	</div>
	<div class="settings">
		<div class="panel-group">
			<!-- The Help Modal -->
			<div id="myModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content">
					<div class="modal-header">
						<span class="close">&times;</span>
						<h2>
							Welcome <small>at the Moloni integration</small>
						</h2>
					</div>
					<div class="modal-body">
						<h4>Important</h4>
						<h5>Pay attention!</h5>
						<ul>
							<li>After purchasing the integration, it is wise to put the
								status of <mark>all existing workorders</mark> in WorkOrderApp
								on <mark>handled</mark>.
							</li>
							<li>The <mark>import settings</mark> may only be editted in
								Moloni.
							</li>
							<li>Click on an <mark>error message</mark> in the log to see
								more details.
							</li>
						</ul>
						<br>

						<h4>Information</h4>
						<ul>
							<li>On this page it is possible to set the settings for <mark>importing</mark>
								and <mark>exporting</mark> data between WorkOrderApp and Moloni.
							</li>
							<li>An <mark>automatic synchronisation</mark> will take
								place every 15 minutes.
							</li>
							<li>It is possible to <mark>synchronize manually</mark>, by
								pressing the sync button at the bottom of this page.
							</li>
						</ul>
						<br>

						<h4>Possibilities</h4>
						<ul>
							<li><mark>Employees, products, customers and products
									with unit hour</mark> can be imported from Moloni.</li>
						</ul>
						<br>
						<button type="button" id="show" class="btn btn-info">Show</button>
						<h4>Data Mapping</h4>
						<div id="mappingTable" style="display: none;">
							<table class="table table-hover">
								<thead>
									<tr>
										<th>WorkOrderApp</th>
										<th>Moloni</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<th colspan="2">Import</th>
									</tr>
									<tr>
										<td>Employees</td>
										<td>Users</td>
									</tr>
									<tr>
										<td>Materials</td>
										<td>Products</td>
									</tr>
									<tr>
										<td>Relations</td>
										<td>Customers</td>
									</tr>
									<tr>
										<td>Hourtypes</td>
										<td>Products with unit hour</td>
									</tr>
									<tr>
										<th colspan="2">Export</th>
									</tr>
									<tr>
										<td>WorkOrder</td>
										<td>Invoice</td>
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
			<form action="settings.do" id="saveMoloni">
				<div class="panel panel-success">
					<div class="panel-heading">Import Settings</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type="hidden" value="${softwareToken}"
									name="softwareToken" />
								<label>Administration</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Choose an administration" height="13" width="13" />
								<select name="offices" class="form-control" id="officelist"
									required>
									<option disabled selected value>-- Selecteer een
										administratie --</option>
									<c:forEach items="${offices}" var="office">
										<option value="${office.code}"
											${office.code == importOffice ? 'selected="selected"' : ''}>
											${office.name}</option>
									</c:forEach>
								</select><br>
								<label>Select objects for import</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Select the objects you want to import from Moloni into WorkOrderApp"
									height="13" width="13" />
								<div class="checkbox">
									<label>
										<input type="checkbox" value="employees"
											${"selected" == checkboxes.employees  ? 'checked' : ''}
											name="importType">
										Employees
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="materials"
											${"selected" == checkboxes.materials  ? 'checked' : ''}
											name="importType">
										Products
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="hourtypes"
											${"selected" == checkboxes.hourtypes  ? 'checked' : ''}
											name="importType">
										Hourtypes
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="relations"
											${"selected" == checkboxes.relations  ? 'checked' : ''}
											name="importType">
										Relations
									</label>
								</div>

								<%-- <div class="checkbox">
									<label>
										<input type="checkbox" value="verkooporders"
											${"selected" == checkboxes.verkooporders  ? 'checked' : ''}
											name="importType" id="verkooporders">
										Orders
									</label>
								</div> --%>
								<div id="verkooporders_extra">
									<label>Select a worktype</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="This worktype will be added to every order" height="13"
										width="13" />
									<select name="typeofwork" class="form-control" id="typeofwork"
										required>
										<c:forEach items="${types}" var="type">
											<option value="${type.key}"
												${type.value == 'selected' ? 'selected="selected"' : ''}>
												${type.key}</option>
										</c:forEach>

									</select> <br>
									<label>Select a payment method</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="This payment method will be added to every order"
										height="13" width="13" />
									<select name="paymentmethod" class="form-control"
										id="paymentmethod" required>
										<c:forEach items="${paymentmethods}" var="payment">
											<option value="${payment.key}"
												${payment.value == 'selected' ? 'selected="selected"' : ''}>
												${payment.key}</option>
										</c:forEach>
									</select>
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
												name="syncDate" value="${savedDate}" />

										</div>
									</div>
								</div>

								<button type="button" id="help" class="btn btn-info btn-lg">Help</button>
								<input type="submit" class="btn btn-success btn-lg" value="Save"
									name="category" id="savebutton" />
							</div>
						</div>
					</div>
				</div>
				<div class="panel panel-success">
					<div class="panel-heading">Export Settings</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<label>Workorderstatus</label>
								<img src="./img/vraagteken.jpg" height="13" width="13"
									data-toggle="tooltip"
									title="WorkOrders with status complete will be exported" />
								<input class="form-control" type="text" disabled
									value="Complete" />
								<input class="form-control" type="hidden" name="factuurType"
									value="Compleet" />
								<br>
								<label>Export workorder as</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="A workorder will be exporterd as an invoice" height="13"
									width="13" />
								<br>
								<input type="radio" name="exportWerkbon" value="factuur" checked>
								Invoice<br> <br>
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
								</select>
							</div>
						</div>
					</div>
					<div class="row">
						<div class="form-group col-xs-12">
							<input type="submit" class="btn btn-success btn-lg" value="Sync"
								id="syncbutton" />
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
	<script type="text/javascript" src="js/moloni.js"></script>
</body>
</html>
