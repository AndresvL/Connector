$(document).ready(function() {
		$('[data-toggle="tooltip"]').tooltip(); 
			<!-- reload page every 15 - 20 min -->
			setTimeout(function(){
				  location.reload();
			  },880000)
				var modal = $('#checkbox').val();
				if ($('#session').val() === "" || $('#session').val() === null) {
					if ($('#error').val() !== null) {
						swal({
							title : 'Warning',
							text : $('#error').val(),
							type : 'error'
						})
					} else {
						swal({
							title : 'Waarchuwing',
							text : 'Je bent niet verbonden',
							type : 'error'
						})
					}
	
				} else {
					$('.settings').show();
					if ($('#saved').val() !== "") {
						swal({
							title : 'Success',
							text : $('#saved').val(),
							type : 'success'
						})
					}
					$('#saved').val("false");
				}
			$(".showDetails").click(function() {
				var errorDetails = $(this).data("href");
				if(errorDetails !== null && errorDetails !== true && errorDetails !== false && errorDetails !== ""){
					var nice = vkbeautify.xml(errorDetails);
					alert(nice);
				} 
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
				if (event.target === modal) {
					modal.style.display = "none";
				}
			}
		});
		$("#save").submit(
				function(event) {
					if ($('#officeExportList').val() === null
							|| $('#officelist').val() === null) {
						event.preventDefault();
						swal({
							title : 'Warning',
							text : 'Geen administratie geselecteerd',
							type : 'error'
						})
					}
				});
		$("#syncbutton").click(
				function(event) {
					if ($('#officeExportList').val() === null
							|| $('#officelist').val() === null) {
						event.preventDefault();
						swal({
							title : 'Warning',
							text : 'Geen administration geselecteerd',
							type : 'error'
						})
					} else {
						event.preventDefault();
						swal({
							  title: 'Synchroniseren',
							  text: 'Op de achtergrond zal de synchronisatie plaatsvinden. Kom over een paar minuten terug',
							  imageUrl: 'WBA.png',
							  imageWidth: 250,
							  imageHeight: 220,
							  showConfirmButton: true
							}).then(
							  function () {
								  $( "#sync" ).submit();
							  })
					}
				});
		$("#officelist").change(function() {
			$("#officeExportList").val($("#officelist").val());
			$("#exportOfficeValue").val($("#officelist").val());
		});
		$(".exportRelation").on('change', function() {
			if ($("input[name='exportRelations']:checked").val() == "yes") {
				$('#relations').attr('checked', true);
			}
		});
		