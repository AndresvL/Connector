var interval = null;
$(document).ready(
		function() {
			$('[data-toggle="tooltip"]').tooltip();
			// Date
			$('#datetimepicker1').datetimepicker({
				format : 'DD-MM-YYYY HH:mm:ss'
			});

			var modal = document.getElementById('loginModal');
			if ($('#validLogin').val() === ""
					|| $('#validLogin').val() === null) {
				modal.style.display = "block";
			} else {
				modal.style.display = "hide";
				if ($('#validLogin').val() === "true") {
					swal({
						title : 'Gelukt',
						text : "Je bent ingelogd!",
						type : 'success',
						showConfirmButton : true
					}).then(function() {
						$("#help").click();
					});
					$('#validLogin').val("");
				}
				if ($('#saved').val() !== "") {
					swal({
						title : 'Gelukt',
						text : $('#saved').val(),
						type : 'success'
					})
				}
				$('#saved').val("false");
			}
			$(".showDetails").click(
					function() {
						var errorDetails = $(this).data("href");
						if (errorDetails !== null && errorDetails !== true
								&& errorDetails !== false
								&& errorDetails !== "") {
							alert(errorDetails);
						}
					});
			$("#refresh_knop").click(function() {
				window.location.reload();
			});
		});

$("#show").click(function() {
	$("#mappingTable").toggle();
});
$("#help").click(function() {
	var modal = document.getElementById('myModal');
	modal.style.display = "block";
	// Get the <span> element that closes the modal
	var span = document.getElementsByClassName("close")[0];
	// When the user clicks on <span> (x), close the modal
	span.onclick = function() {
		modal.style.display = "none";
	}

	// When the user clicks anywhere outside of the modal, close it
	window.onclick = function(event) {
		if (event.target == modal) {
			modal.style.display = "none";
		}
	}
});
$("#syncbutton")
		.click(
				function(event) {
					event.preventDefault();
					// function startSync() {
					if ($("#status").val() === "error") {
						swal(
								{
									title : 'Let op',
									text : 'Werkbonstatus staat op Error. Hierdoor zullen geen werkbonnen gesynchroniseerd worden',
									imageUrl : 'WBA.png',
									imageWidth : 250,
									imageHeight : 220,
									showConfirmButton : true,
									showCancelButton : true,
									cancelButtonText : 'Verander status'
								}).then(function() {
							$("#sync").submit();
						}, function(dismiss) {
							// dismiss can be 'cancel', 'overlay',
							// 'close', and 'timer'
							// if (dismiss === 'cancel') {
							// $.ajax({
							// type : "POST",
							// url : "ProgressBarServlet",
							// data : "status=3",
							// success : function(data) {
							// 
							// }
							// });
							// $('#status').val("compleet");
							// $("#saveBouwsoft").submit();
							// } else {
							// $.ajax({
							// type : "POST",
							// url : "ProgressBarServlet",
							// data : "status=3",
							// success : function(data) {
							// 
							// }
							// });
							// }
						})
					} else {
						swal(
								{
									title : 'Synchroniseren',
									text : 'Op de achtergrond zal de synchronisatie plaatsvinden. Kom over een paar minuten terug',
									imageUrl : 'WBA.png',
									imageWidth : 250,
									imageHeight : 220,
									showConfirmButton : true
								}).then(function() {
							$("#sync").submit();
						}, function(dismiss) {
							// dismiss can be 'cancel', 'overlay',
							// 'close', and 'timer'
							if (dismiss === 'overlay') {
								$.ajax({
									type : "POST",
									url : "ProgressBarServlet",
									data : "status=3",
									success : function(data) {

									}
								});
							}
						})
						// }
					}
				});