var interval = null;
$(document).ready(
		function() {
			var modal = document.getElementById('loginModal');
			if ($('#client').val() === "" || $('#client').val() === null) {
				modal.style.display = "block";
			}else {
				if($('#error').val() === "true"){
					swal({
						title : 'Gelukt',
						text : "Je bent ingelogd!",
						type : 'success',
						showConfirmButton: true
						}).then(function () {
							$( "#help" ).click();
						});				
					$('#error').val("");
				} else if($('#error').val() == "syncStatusError"){
					swal({
						title : 'Oops!',
						text : 'Sync status & Compleet status mogen niet hetzelfde zijn als een Processing status',
						type : 'error',
						showConfirmButton: true
						}).then(function () {
							$( "#help" ).click();
						});				
					$('#error').val("");
				}
				/*
				if ($('#saved').val() !== "") {
					swal({
						title : 'Gelukt',
						text : $('#saved').val(),
						type : 'success'
					})
				}
				$('#saved').val("false");
				*/
			}
			$(".showDetails").click(function() {
				var errorDetails = $(this).data("href");
				if(errorDetails !== null && errorDetails !== true && errorDetails !== false && errorDetails !== ""){
					alert(errorDetails);
				} 
			});
			
			$.ajax({
				type : "GET",
				url : "ProgressBarServlet",
				success : function(data) {
					if (data === 1) {
						$('#loading').show();
						$('#loadingLabel').show();
						sync();
					}
				}
			});
			function sync() {
				interval = setInterval(function() {
					$.ajax({
						type : "GET",
						url : "ProgressBarServlet",
						success : function(data) {
							if (data === 1) {
								$('#loading').show();
								$('#loadingLabel').show();
							} else if (data === 2) {
								$('#loading').hide();
								$('#loadingLabel').hide();
								location.reload();
							} else {
								clearInterval(interval);
								$('#loading').hide();
								$('#loadingLabel').hide();
							}
						}
					});
				}, 4000)
			}
			$('[data-toggle="tooltip"]').tooltip();
			// Date
			$('#datetimepicker1').datetimepicker({
				format : 'DD-MM-YYYY HH:mm:ss'
			});
			// Project checkbox 
			if (!$('#fancy-checkbox-success').attr('checked')) {
				$('#projectActive').hide();
			}
			// Project checkbox 
			$('#fancy-checkbox-success').on('change', function() {
				if ($('#fancy-checkbox-success')[0].checked === false) {
					// projectfilter checkboxes 
					$('#fancy-checkbox-active')[0].checked = false
				}
				$('#projectActive').toggle(this.checked);
			})
			
			if (!$('#fancy-checkbox-primary').attr('checked')) {
				$('#orderOptionsParent').hide();
			}
			// Werkbonnen checkbox
			$('#fancy-checkbox-primary').on('change', function() {
				if ($('#fancy-checkbox-primary')[0].checked === false) {
					// category checkbox
					$("#orderOptionsParent").hide(300);
				} else {
					$("#orderOptionsParent").show(300);
				}
				
			})
			
			var selectedOpGroups =$('#hidOpGroup').value;
			$.each(selectedOpGroups.split(";"), function(i,e){
				$("#opGroupPicker option[value='" + e + "']").prop("selected", true);
				$('#opGroupPicker').trigger('change');
			});
			
			var selectedStatus = $('#hidStatusGroup').value;
			$.each(selectedStatus.split(";"), function(i,e){
				$("#statusGroupPicker option[value='" + e + "']").prop("selected", true);
				$('#statusGroupPicker').trigger('change');
			});

			// relations checkbox 
			if (!$('#fancy-checkbox-warning').attr('checked')) {
				$('#contacts').hide();
			}
			// relations checkbox 
			$('#fancy-checkbox-warning').on('change', function() {
				if ($('#fancy-checkbox-warning')[0].checked === false) {
					// contacts checkbox 
					$('#fancy-checkbox-contacts')[0].checked = false
				}
				$('#contacts').toggle(this.checked);
			})

			// Werkbonnen checkbox
			
			if ($('#error').val() === "true") {
				swal({
					title : 'Gelukt',
					text : "Je bent ingelogd!",
					type : 'success',
					showConfirmButton : true
				}).then(function() {
					$("#help").click();
				});
				$('#error').val("");
			} else if($('#error').val() !== ""){
				swal({
					title : 'Oops!',
					text : $('#error').val(),
					type : 'error',
					showConfirmButton: true
					}).then(function () {
						$( "#help" ).click();
					});				
				$('#error').val("");
			}
			
			/*
			if ($('#saved').val() !== "") {
				swal({
					title : 'Gelukt',
					text : $('#saved').val(),
					type : 'success'
				})
			}
			$('#saved').val("false");
			*/

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
	var modal = document.getElementById('helpModal');
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
					$.ajax({
						type : "POST",
						url : "ProgressBarServlet",
						data : "status=1",
						success : function(data) {
							startSync();
						}
					});
					event.preventDefault();
					function startSync() {
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
								if (dismiss === 'cancel') {
									$.ajax({
										type : "POST",
										url : "ProgressBarServlet",
										data : "status=3",
										success : function(data) {

										}
									});
									$('#status').val("compleet");
									$("#saveBouwsoft").submit();
								} else {
									$.ajax({
										type : "POST",
										url : "ProgressBarServlet",
										data : "status=3",
										success : function(data) {

										}
									});
								}
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
						}
					}
				});